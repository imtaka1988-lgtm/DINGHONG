# -*- coding: utf-8 -*-
"""
爬取 https://www.gswyl.com/ 全站文章，生成 HTML + CSV 干货汇总报告。

功能区段：
  1. 网络请求
  2. HTML 解析 & 噪音过滤
  3. 文章列表收集
  4. 报告生成（HTML / CSV）
  5. main() 编排
"""

import re
import ssl
import time
import sys
import os

ssl._create_default_https_context = ssl._create_unverified_context
import urllib.request, urllib.error
from concurrent.futures import ThreadPoolExecutor, as_completed

# ============================================================
# 1. 网络请求
# ============================================================

BASE = "https://www.gswyl.com"
HDR = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"}


def get(url, retries=2):
    """GET 请求，返回 HTML 文本或 None。"""
    for _ in range(retries):
        try:
            req = urllib.request.Request(url, headers=HDR)
            with urllib.request.urlopen(req, timeout=20) as r:
                raw = r.read()
                try:
                    return raw.decode("utf-8")
                except UnicodeDecodeError:
                    return raw.decode("utf-8", errors="replace")
        except Exception:
            time.sleep(1)
    return None

# ============================================================
# 2. HTML 解析 & 噪音过滤
# ============================================================

NOISE_KEYWORDS = [
    "关注公众号", "点菜单找古圣领取", "古圣微说", "扫码进入", "添加微信",
    "复制微信", "免费领取全套引流", "联系古圣", "联系QQ", "微信号已复制",
    "复制微信号", "备注：网站添加", "GSWLPP", "GSWLYY", "GSWL69",
    "本文链接：", "版权声明", "点击查看更多", "关注公众号\"古圣微说\"",
]


def strip_tags(html):
    """去除 HTML 标签，返回纯文本。"""
    t = re.sub(r"<script[^>]*>.*?</script>", "", html, flags=re.DOTALL | re.I)
    t = re.sub(r"<style[^>]*>.*?</style>", "", t, flags=re.DOTALL | re.I)
    t = re.sub(r"<[^>]+>", " ", t)
    t = re.sub(r"&[a-z]+;", " ", t)
    t = re.sub(r"&#?\w+;", " ", t)
    t = re.sub(r"[ \t]+", " ", t)
    t = re.sub(r"\n\s*\n+", "\n", t)
    return t.strip()


def filter_noise(text):
    """过滤推广话术，返回干净文本。"""
    lines = text.split("\n")
    clean = []
    for line in lines:
        line = line.strip()
        if len(line) < 15:
            continue
        if any(n in line for n in NOISE_KEYWORDS):
            continue
        clean.append(line)
    return "\n".join(clean)


def extract_from_list(html):
    """从列表页提取文章信息（标题、摘要、分类、日期）。"""
    articles = []
    blocks = re.findall(r"<article class=\"card item\">(.*?)</article>", html, re.DOTALL)
    for block in blocks:
        link_m = re.search(r"href=\"(/post/\d+\.html)\"\s+title=\"([^\"]*)\"", block)
        if not link_m:
            link_m = re.search(
                r"href=\"(https?://[^\"]+/post/\d+\.html)\"\s+title=\"([^\"]*)\"",
                block,
            )
        if not link_m:
            continue
        url = link_m.group(1)
        if url.startswith("/"):
            url = BASE + url
        title = link_m.group(2).strip()

        summary_m = re.search(r"<p class=\"content-ellipsis\">(.*?)</p>", block, re.DOTALL)
        summary = strip_tags(summary_m.group(1)) if summary_m else ""

        cat_m = re.search(
            r"<a href=\"[^\"]*category-\d+\.html\"[^>]*title=\"([^\"]*)\"", block
        )
        category = cat_m.group(1).strip() if cat_m else "其他"

        date_m = re.search(
            r"<span class=\"time\">[^<]*<svg[^>]*>.*?</svg>\s*(.*?)\s*</span>",
            block,
            re.DOTALL,
        )
        if date_m:
            date = date_m.group(1).strip()
        else:
            date_m = re.search(r"(\d{4}-\d{2}-\d{2})", block)
            date = date_m.group(1) if date_m else ""

        articles.append({
            "url": url,
            "title": title,
            "category": category,
            "date": date,
            "summary": summary,
        })
    return articles


def summarize(text, max_len=500):
    """截断文本到 max_len 字符，尽量在句号处断句。"""
    text = text.strip()
    if len(text) <= max_len:
        return text
    for sep in ["。", "！", "？", "\n", "；"]:
        pos = text.rfind(sep, 0, max_len + 80)
        if pos > max_len - 80:
            text = text[: pos + 1]
            break
    if len(text) > max_len + 50:
        text = text[:max_len] + "..."
    return text.strip()

# ============================================================
# 3. 文章列表收集
# ============================================================

MAX_PAGES = 75


def fetch_page(page_num):
    """抓取单页文章列表。"""
    url = BASE + "/" if page_num == 1 else f"{BASE}/page_{page_num}.html"
    html = get(url)
    if not html:
        return []
    return extract_from_list(html)


def collect_all_article_links():
    """并发抓取所有页面，返回去重文章列表。"""
    print("=" * 60, file=sys.stderr)
    print("古圣引流网 - 全站文章爬取与干货提炼", file=sys.stderr)
    print("=" * 60, file=sys.stderr)

    all_articles = []
    seen = set()

    print(f"\n[1] 爬取文章列表 (共{MAX_PAGES}页)...", file=sys.stderr)
    with ThreadPoolExecutor(max_workers=5) as executor:
        futures = {executor.submit(fetch_page, p): p for p in range(1, MAX_PAGES + 1)}
        for future in as_completed(futures):
            page = futures[future]
            try:
                articles = future.result()
                new = 0
                for a in articles:
                    if a["url"] not in seen:
                        seen.add(a["url"])
                        all_articles.append(a)
                        new += 1
                print(
                    f"  第{page}页: +{new}篇 (累计{len(all_articles)})",
                    file=sys.stderr,
                )
            except Exception as e:
                print(f"  第{page}页出错: {e}", file=sys.stderr)

    return all_articles

# ============================================================
# 4. 报告生成
# ============================================================

CSS_STYLE = """\
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: "Microsoft YaHei", "PingFang SC", sans-serif; background: #f5f5f5; color: #333; line-height: 1.7; }
.header { background: linear-gradient(135deg, #f1404b, #c0392b); color: #fff; padding: 30px 40px; text-align: center; }
.header h1 { font-size: 26px; margin-bottom: 6px; }
.header p { font-size: 14px; opacity: 0.85; }
.stats { display: flex; justify-content: center; gap: 30px; padding: 20px; background: #fff;
         border-bottom: 1px solid #e0e0e0; margin-bottom: 20px; flex-wrap: wrap; }
.stat-item { text-align: center; }
.stat-num { font-size: 32px; font-weight: bold; color: #f1404b; }
.stat-label { font-size: 12px; color: #888; }
.container { max-width: 1200px; margin: 0 auto; padding: 0 20px 40px; }
.toc { background: #fff; border-radius: 8px; padding: 20px 25px; margin-bottom: 20px; border-left: 4px solid #f1404b; }
.toc h3 { margin-bottom: 12px; font-size: 18px; }
.toc-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 6px; }
.toc a { display: block; padding: 6px 12px; background: #fff5f5; border-radius: 4px;
         text-decoration: none; color: #c0392b; font-size: 13px; transition: .2s; }
.toc a:hover { background: #f1404b; color: #fff; }
.filter-bar { margin-bottom: 20px; display: flex; gap: 8px; flex-wrap: wrap; }
.filter-btn { padding: 6px 16px; border: 1px solid #ddd; border-radius: 20px;
              background: #fff; cursor: pointer; font-size: 13px; transition: .2s; }
.filter-btn:hover, .filter-btn.active { background: #f1404b; color: #fff; border-color: #f1404b; }
table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 8px; overflow: hidden;
        box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
th { background: #f1404b; color: #fff; padding: 12px 10px; font-size: 13px; text-align: left; position: sticky; top: 0; }
td { padding: 10px; border-bottom: 1px solid #f0f0f0; font-size: 13px; vertical-align: top; }
tr:hover { background: #fff5f5; }
td:nth-child(1) { width: 50px; text-align: center; color: #999; }
td:nth-child(2) { width: 80px; }
.cat-badge { display: inline-block; padding: 2px 8px; border-radius: 10px; font-size: 11px;
             background: #ffeaea; color: #c0392b; white-space: nowrap; }
td:nth-child(3) { min-width: 180px; font-weight: 600; }
td:nth-child(3) a { color: #333; text-decoration: none; }
td:nth-child(3) a:hover { color: #f1404b; }
td:nth-child(5) { max-width: 500px; }
td:nth-child(4) { width: 90px; white-space: nowrap; color: #888; font-size: 12px; }
.hidden-tr { display: none; }
.footer { text-align: center; padding: 20px; color: #999; font-size: 12px; margin-top: 30px; }
.search-box { padding: 8px 16px; border: 1px solid #ddd; border-radius: 20px; width: 250px;
              font-size: 13px; outline: none; }
.search-box:focus { border-color: #f1404b; }"""


def _cat_hash(cat):
    """与前端 JavaScript 一致的分类哈希。"""
    return hash(cat) & 0x7FFFFFFF


def generate_html_report(articles, output_path):
    """生成单文件 HTML 报告（带分类导航+搜索过滤）。"""
    cat_counts = {}
    for a in articles:
        c = a["category"]
        cat_counts[c] = cat_counts.get(c, 0) + 1

    def _id(a):
        m = re.search(r"/post/(\d+)", a["url"])
        return int(m.group(1)) if m else 0

    parts = []
    parts.append(f"""\
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<title>古圣引流网 - 干货汇总报告</title>
<style>
{CSS_STYLE}
</style>
</head>
<body>
<div class="header">
<h1>古圣引流网 - 全站干货汇总报告</h1>
<p>数据来源: https://www.gswyl.com/ | 报告生成于 {time.strftime('%Y-%m-%d')} | 已过滤推广和广告内容，仅保留核心干货</p>
</div>

<div class="stats">
<div class="stat-item"><div class="stat-num">{len(articles)}</div><div class="stat-label">总文章数</div></div>
""")

    for cat, cnt in sorted(cat_counts.items(), key=lambda x: -x[1]):
        parts.append(f'<div class="stat-item"><div class="stat-num">{cnt}</div><div class="stat-label">{cat}</div></div>\n')

    parts.append('</div>\n<div class="container">\n<div class="toc">\n<h3>分类导航</h3>\n<div class="toc-grid">\n')
    for cat in sorted(cat_counts.keys()):
        parts.append(f'<a href="#cat-{_cat_hash(cat)}">{cat} ({cat_counts[cat]}篇)</a>\n')
    parts.append('</div></div>\n')

    parts.append('<div class="filter-bar">\n'
                 '<input class="search-box" placeholder="搜索文章..." oninput="filterArticles()">\n'
                 '<button class="filter-btn active" onclick="filterCat(\'all\', this)">全部</button>\n')
    for cat in sorted(cat_counts.keys()):
        parts.append(f'<button class="filter-btn" onclick="filterCat(\'{cat}\', this)">{cat}</button>\n')
    parts.append('</div>\n')

    parts.append('<table id="articleTable">\n<thead><tr>\n'
                 '<th>#</th><th>分类</th><th>标题</th><th>发布日期</th><th>干货摘要</th>\n'
                 '</tr></thead>\n<tbody>\n')

    for a in articles:
        summary = a.get("summary_clean") or a["summary"]
        display = summarize(summary, 400)
        if not display.strip():
            continue
        num = _id(a)
        parts.append(
            f'<tr class="cat-{_cat_hash(a["category"])}">'
            f'<td>{num}</td>'
            f'<td><span class="cat-badge">{a["category"]}</span></td>'
            f'<td><a href="{a["url"]}" target="_blank" title="{a["title"]}">{a["title"]}</a></td>'
            f'<td>{a["date"]}</td>'
            f'<td>{display}</td>'
            f'</tr>\n'
        )

    parts.append('</tbody></table>\n<div class="footer">\n<p>报告由程序自动生成 | 数据来源: gswyl.com | 已去除推广和广告内容，仅保留核心干货</p>\n</div>\n</div>\n')

    # 分类筛选 JS
    parts.append('<script>\n')
    parts.append("window._catMap = {\n")
    for cat in cat_counts:
        parts.append(f"    '{cat}': 'cat-{_cat_hash(cat)}',\n")
    parts.append("};\n")
    parts.append("""\
window.currentCat = 'all';
window._catHash = '';

window.filterCat = function(cat, btn) {
    document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    window.currentCat = cat;
    window._catHash = window._catMap[cat] || '';
    filterArticles();
};

window.filterArticles = function() {
    const search = (document.querySelector('.search-box')?.value || '').toLowerCase();
    const cat = window.currentCat || 'all';
    document.querySelectorAll('tbody tr').forEach(r => {
        let show = true;
        if (search) show = r.textContent.toLowerCase().includes(search);
        if (cat !== 'all' && window._catHash) show = show && r.classList.contains(window._catHash);
        r.classList.toggle('hidden-tr', !show);
    });
};
</script>
</body>
</html>""")

    with open(output_path, "w", encoding="utf-8") as f:
        f.write("".join(parts))

    print(f"\n报告已生成到: {output_path}", file=sys.stderr)


def generate_csv(articles, output_path):
    """生成 CSV 文件。"""
    def _id(a):
        m = re.search(r"/post/(\d+)", a["url"])
        return int(m.group(1)) if m else 0

    with open(output_path, "w", encoding="utf-8-sig") as f:
        f.write("序号,分类,标题,发布日期,干货摘要,原文链接\n")
        for a in articles:
            title = a["title"].replace('"', '""')
            summary = (a.get("summary_clean") or a["summary"]).replace('"', '""').replace('\n', '。')
            cat = a["category"].replace('"', '""')
            f.write(f'{_id(a)},{cat},"{title}",{a["date"]},"{summarize(summary, 800)}",{a["url"]}\n')

    print(f"CSV已生成到: {output_path}", file=sys.stderr)

# ============================================================
# 5. main() 编排
# ============================================================

def main():
    articles = collect_all_article_links()
    if not articles:
        print("失败: 未获取到任何文章", file=sys.stderr)
        return

    # 按文章 ID 排序
    def _id(a):
        m = re.search(r"/post/(\d+)", a["url"])
        return int(m.group(1)) if m else 0
    articles.sort(key=_id)

    print(f"\n[2] 列表页爬取完成，共 {len(articles)} 篇文章", file=sys.stderr)

    # 对摘要做噪音过滤
    for a in articles:
        a["summary_clean"] = filter_noise(a["summary"])

    desktop = os.path.join(os.path.expanduser("~"), "Desktop")

    # 生成报告
    generate_html_report(
        articles,
        os.path.join(desktop, "古圣引流网_引流干货汇总报告.html"),
    )
    generate_csv(articles, os.path.join(desktop, "古圣引流网_干货汇总.csv"))

    # 控制台预览
    print("\n" + "=" * 60)
    print("干货预览（前15篇）：")
    print("=" * 60)
    for a in articles[:15]:
        print(f"\n[{a['category']}] {a['title']} ({a['date']})")
        print(f"  URL: {a['url']}")
        s = a["summary_clean"] or a["summary"]
        print(f"  摘要: {summarize(s, 200)}")

    print(f"\n完成！共 {len(articles)} 篇文章。")


if __name__ == "__main__":
    main()
