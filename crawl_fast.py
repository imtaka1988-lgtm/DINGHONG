# -*- coding: utf-8 -*-
"""
爬取 https://www.gswyl.com/ 全站文章（仅从列表页提取摘要，速度快）
"""
import re, ssl, time, sys, os, json

ssl._create_default_https_context = ssl._create_unverified_context
import urllib.request, urllib.error
from concurrent.futures import ThreadPoolExecutor, as_completed

BASE = "https://www.gswyl.com"
HDR = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'}

def get(url, retries=2):
    for _ in range(retries):
        try:
            req = urllib.request.Request(url, headers=HDR)
            with urllib.request.urlopen(req, timeout=20) as r:
                raw = r.read()
                try:
                    return raw.decode('utf-8')
                except:
                    return raw.decode('utf-8', errors='replace')
        except Exception as e:
            time.sleep(1)
    return None

def strip_tags(html):
    t = re.sub(r'<script[^>]*>.*?</script>', '', html, flags=re.DOTALL | re.I)
    t = re.sub(r'<style[^>]*>.*?</style>', '', t, flags=re.DOTALL | re.I)
    t = re.sub(r'<[^>]+>', ' ', t)
    t = re.sub(r'&[a-z]+;', ' ', t)
    t = re.sub(r'&#?\w+;', ' ', t)
    t = re.sub(r'[ \t]+', ' ', t)
    t = re.sub(r'\n\s*\n+', '\n', t)
    return t.strip()

def extract_from_list(html):
    """从列表页一次性提取所有文章信息"""
    articles = []
    blocks = re.findall(r'<article class="card item">(.*?)</article>', html, re.DOTALL)
    for block in blocks:
        # 链接和标题
        link_m = re.search(r'href="(/post/\d+\.html)"\s+title="([^"]*)"', block)
        if not link_m:
            link_m = re.search(r'href="(https?://[^"]+/post/\d+\.html)"\s+title="([^"]*)"', block)
        if not link_m:
            continue
        url = link_m.group(1)
        if url.startswith('/'):
            url = BASE + url
        title = link_m.group(2).strip()

        # 摘要
        summary_m = re.search(r'<p class="content-ellipsis">(.*?)</p>', block, re.DOTALL)
        summary = strip_tags(summary_m.group(1)) if summary_m else ''

        # 分类
        cat_m = re.search(r'<a href="[^"]*category-\d+\.html"[^>]*title="([^"]*)"', block)
        category = cat_m.group(1).strip() if cat_m else '其他'

        # 日期
        date_m = re.search(r'<span class="time">[^<]*<svg[^>]*>.*?</svg>\s*(.*?)\s*</span>', block, re.DOTALL)
        if date_m:
            date = date_m.group(1).strip()
        else:
            date_m = re.search(r'(\d{4}-\d{2}-\d{2})', block)
            date = date_m.group(1) if date_m else ''

        articles.append({
            'url': url,
            'title': title,
            'category': category,
            'date': date,
            'summary': summary,
        })
    return articles

def filter_noise(text):
    """过滤推广话术"""
    noise = [
        '关注公众号', '点菜单找古圣领取', '古圣微说', '扫码进入', '添加微信',
        '复制微信', '免费领取全套引流', '联系古圣', '联系QQ', '微信号已复制',
        '复制微信号', '备注：网站添加', 'GSWLPP', 'GSWLYY', 'GSWL69',
        '本文链接：', '版权声明', '点击查看更多', '关注公众号"古圣微说"',
    ]
    lines = text.split('\n')
    clean = []
    for line in lines:
        line = line.strip()
        if len(line) < 15:
            continue
        skip = False
        for n in noise:
            if n in line:
                skip = True
                break
        if not skip:
            clean.append(line)
    return '\n'.join(clean)

def fetch_page(page_num):
    """获取单页文章列表"""
    if page_num == 1:
        url = BASE + '/'
    else:
        url = f'{BASE}/page_{page_num}.html'
    html = get(url)
    if not html:
        return []
    return extract_from_list(html)

def collect_all_article_links():
    """获取所有文章列表"""
    print("=" * 60, file=sys.stderr)
    print("古圣引流网 - 全站文章爬取与干货提炼", file=sys.stderr)
    print("=" * 60, file=sys.stderr)

    # 先获取首页确认总页数
    html = get(BASE + '/')
    if not html:
        print("无法访问首页!", file=sys.stderr)
        return []
    max_page = 75  # 从首页分页已知75页

    all_articles = []
    seen = set()

    print(f"\n[1] 爬取文章列表 (共{max_page}页)...", file=sys.stderr)

    # 并发获取
    with ThreadPoolExecutor(max_workers=5) as executor:
        futures = {executor.submit(fetch_page, p): p for p in range(1, max_page + 1)}
        for future in as_completed(futures):
            page = futures[future]
            try:
                articles = future.result()
                new = 0
                for a in articles:
                    if a['url'] not in seen:
                        seen.add(a['url'])
                        all_articles.append(a)
                        new += 1
                print(f"  第{page}页: +{new}篇 (累计{len(all_articles)})", file=sys.stderr)
            except Exception as e:
                print(f"  第{page}页出错: {e}", file=sys.stderr)

    return all_articles

def fetch_article_details(article):
    """获取单篇文章完整正文（用于更详细的摘要）"""
    html = get(article['url'])
    if not html:
        return None
    # 提取正文
    m = re.search(r'<div[^>]*class="[^"]*post-body[^"]*"[^>]*>(.*?)</div>\s*<(?:div|footer)', html, re.DOTALL | re.I)
    if not m:
        m = re.search(r'<div[^>]*class="[^"]*post-content[^"]*"[^>]*>(.*?)</div>', html, re.DOTALL | re.I)
    if not m:
        m = re.search(r'<article[^>]*>(.*?)</article>', html, re.DOTALL | re.I)
    body = strip_tags(m.group(1)) if m else ''
    body = filter_noise(body)
    return body

def summarize(text, max_len=500):
    text = text.strip()
    if len(text) <= max_len:
        return text
    for sep in ['。', '！', '？', '\n', '；']:
        pos = text.rfind(sep, 0, max_len + 80)
        if pos > max_len - 80:
            text = text[:pos + 1]
            break
    if len(text) > max_len + 50:
        text = text[:max_len] + '...'
    return text.strip()

def main():
    # 1) 获取所有文章列表（带摘要）
    articles = collect_all_article_links()
    if not articles:
        print("失败: 未获取到任何文章", file=sys.stderr)
        return

    # 按ID排序
    def get_id(a):
        m = re.search(r'/post/(\d+)', a['url'])
        return int(m.group(1)) if m else 0
    articles.sort(key=get_id)

    print(f"\n[2] 列表页爬取完成，共 {len(articles)} 篇文章", file=sys.stderr)

    # 2) 用摘要作为基础（直接来自列表页的content-ellipsis）
    #    对摘要也进行噪音过滤
    for a in articles:
        a['summary_clean'] = filter_noise(a['summary'])

    # 3) 保存到桌面
    desktop = os.path.join(os.path.expanduser('~'), 'Desktop')
    report_path = os.path.join(desktop, '古圣引流网_引流干货汇总报告.html')

    # 生成精美HTML报告
    cat_counts = {}
    for a in articles:
        c = a['category']
        cat_counts[c] = cat_counts.get(c, 0) + 1

    html_report = f'''<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<title>古圣引流网 - 干货汇总报告</title>
<style>
* {{ margin: 0; padding: 0; box-sizing: border-box; }}
body {{ font-family: "Microsoft YaHei", "PingFang SC", sans-serif; background: #f5f5f5; color: #333; line-height: 1.7; }}
.header {{ background: linear-gradient(135deg, #f1404b, #c0392b); color: #fff; padding: 30px 40px; text-align: center; }}
.header h1 {{ font-size: 26px; margin-bottom: 6px; }}
.header p {{ font-size: 14px; opacity: 0.85; }}
.stats {{ display: flex; justify-content: center; gap: 30px; padding: 20px; background: #fff; border-bottom: 1px solid #e0e0e0; margin-bottom: 20px; flex-wrap: wrap; }}
.stat-item {{ text-align: center; }}
.stat-num {{ font-size: 32px; font-weight: bold; color: #f1404b; }}
.stat-label {{ font-size: 12px; color: #888; }}
.container {{ max-width: 1200px; margin: 0 auto; padding: 0 20px 40px; }}
.toc {{ background: #fff; border-radius: 8px; padding: 20px 25px; margin-bottom: 20px; border-left: 4px solid #f1404b; }}
.toc h3 {{ margin-bottom: 12px; font-size: 18px; }}
.toc-grid {{ display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 6px; }}
.toc a {{ display: block; padding: 6px 12px; background: #fff5f5; border-radius: 4px; text-decoration: none; color: #c0392b; font-size: 13px; transition: .2s; }}
.toc a:hover {{ background: #f1404b; color: #fff; }}
.filter-bar {{ margin-bottom: 20px; display: flex; gap: 8px; flex-wrap: wrap; }}
.filter-btn {{ padding: 6px 16px; border: 1px solid #ddd; border-radius: 20px; background: #fff; cursor: pointer; font-size: 13px; transition: .2s; }}
.filter-btn:hover, .filter-btn.active {{ background: #f1404b; color: #fff; border-color: #f1404b; }}
table {{ width: 100%; border-collapse: collapse; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }}
th {{ background: #f1404b; color: #fff; padding: 12px 10px; font-size: 13px; text-align: left; position: sticky; top: 0; }}
td {{ padding: 10px; border-bottom: 1px solid #f0f0f0; font-size: 13px; vertical-align: top; }}
tr:hover {{ background: #fff5f5; }}
td:nth-child(1) {{ width: 50px; text-align: center; color: #999; }}
td:nth-child(2) {{ width: 80px; }}
.cat-badge {{ display: inline-block; padding: 2px 8px; border-radius: 10px; font-size: 11px; background: #ffeaea; color: #c0392b; white-space: nowrap; }}
td:nth-child(3) {{ min-width: 180px; font-weight: 600; }}
td:nth-child(3) a {{ color: #333; text-decoration: none; }}
td:nth-child(3) a:hover {{ color: #f1404b; }}
td:nth-child(5) {{ max-width: 500px; }}
td:nth-child(4) {{ width: 90px; white-space: nowrap; color: #888; font-size: 12px; }}
.hidden-tr {{ display: none; }}
.footer {{ text-align: center; padding: 20px; color: #999; font-size: 12px; margin-top: 30px; }}
.search-box {{ padding: 8px 16px; border: 1px solid #ddd; border-radius: 20px; width: 250px; font-size: 13px; outline: none; }}
.search-box:focus {{ border-color: #f1404b; }}
</style>
</head>
<body>
<div class="header">
<h1>古圣引流网 - 全站干货汇总报告</h1>
<p>数据来源: https://www.gswyl.com/ | 报告生成于 {time.strftime('%Y-%m-%d')} | 已过滤推广和广告内容，仅保留核心干货</p>
</div>

<div class="stats">
<div class="stat-item"><div class="stat-num">{len(articles)}</div><div class="stat-label">总文章数</div></div>
'''

    for cat, cnt in sorted(cat_counts.items(), key=lambda x: -x[1]):
        html_report += f'<div class="stat-item"><div class="stat-num">{cnt}</div><div class="stat-label">{cat}</div></div>\n'

    html_report += '''</div>

<div class="container">

<div class="toc">
<h3>分类导航</h3>
<div class="toc-grid">
'''

    for cat in sorted(cat_counts.keys()):
        html_report += f'<a href="#cat-{hash(cat) & 0x7fffffff}">{cat} ({cat_counts[cat]}篇)</a>\n'

    html_report += '''</div></div>

<div class="filter-bar">
<input class="search-box" placeholder="搜索文章..." oninput="filterArticles()">
<button class="filter-btn active" onclick="filterCat('all', this)">全部</button>
'''

    for cat in sorted(cat_counts.keys()):
        html_report += f'<button class="filter-btn" onclick="filterCat(\'{cat}\', this)">{cat}</button>\n'

    html_report += '''</div>

<table id="articleTable">
<thead><tr>
<th>#</th><th>分类</th><th>标题</th><th>发布日期</th><th>干货摘要</th>
</tr></thead>
<tbody>
'''

    for i, a in enumerate(articles):
        cat = a['category']
        title = a['title']
        summary = a['summary_clean'] or a['summary']
        date = a['date']
        url = a['url']
        # 截断过长摘要
        summary_display = summarize(summary, 400)
        num = get_id(a)

        if not summary_display.strip():
            continue  # 跳过完全无内容的

        html_report += f'''<tr class="cat-{hash(cat) & 0x7fffffff}">
<td>{num}</td>
<td><span class="cat-badge">{cat}</span></td>
<td><a href="{url}" target="_blank" title="{title}">{title}</a></td>
<td>{date}</td>
<td>{summary_display}</td>
</tr>
'''

    html_report += '''</tbody></table>

<div class="footer">
<p>报告由程序自动生成 | 数据来源: gswyl.com | 已去除推广和广告内容，仅保留核心干货</p>
</div>

</div>

<script>
function filterCat(cat, btn) {
    document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    const rows = document.querySelectorAll('tbody tr');
    rows.forEach(r => {
        if (cat === 'all' || r.classList.contains('cat-' + (() => { let h = 0; for (let i=0; i<cat.length; i++) { h = ((h<<5)-h) + cat.charCodeAt(i); h |= 0; } return h & 0x7fffffff; })())) {
            r.classList.remove('hidden-tr');
        } else {
            r.classList.add('hidden-tr');
        }
    });
    // Simplified: use data attribute approach
    window.currentCat = cat;
    filterArticles();
}

function filterArticles() {
    const search = (document.querySelector('.search-box')?.value || '').toLowerCase();
    const cat = window.currentCat || 'all';
    const rows = document.querySelectorAll('tbody tr');
    rows.forEach(r => {
        let show = true;
        if (search) {
            const text = r.textContent.toLowerCase();
            show = text.includes(search);
        }
        if (cat !== 'all') {
            show = show && r.className.includes('cat-' + window._catHash);
        }
        r.classList.toggle('hidden-tr', !show);
    });
}

// Fix: use simpler category filtering
(function() {
    const map = {};
'''
    for cat in cat_counts:
        html_report += f"    map['{cat}'] = 'cat-{hash(cat) & 0x7fffffff}';\n"

    html_report += '''
    window._catMap = map;
    window.filterCatReal = function(cat, btn) {
        document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        window.currentCat = cat;
        window._catHash = map[cat] || '';
        filterArticlesReal();
    };
    window.filterArticlesReal = function() {
        const search = (document.querySelector('.search-box')?.value || '').toLowerCase();
        const cat = window.currentCat || 'all';
        const rows = document.querySelectorAll('tbody tr');
        rows.forEach(r => {
            let show = true;
            if (search) {
                show = r.textContent.toLowerCase().includes(search);
            }
            if (cat !== 'all' && window._catHash) {
                show = show && r.classList.contains(window._catHash);
            }
            r.classList.toggle('hidden-tr', !show);
        });
    };
    // Override
    window.filterCat = window.filterCatReal;
    window.filterArticles = window.filterArticlesReal;
})();
</script>
</body>
</html>'''

    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(html_report)

    print(f"\n报告已生成到: {report_path}", file=sys.stderr)
    print(f"总计 {len(articles)} 篇文章（已过滤推广内容）", file=sys.stderr)

    # 同时生成CSV
    csv_path = os.path.join(desktop, '古圣引流网_干货汇总.csv')
    with open(csv_path, 'w', encoding='utf-8-sig') as f:
        f.write('序号,分类,标题,发布日期,干货摘要,原文链接\n')
        for i, a in enumerate(articles):
            title = a['title'].replace('"', '""')
            summary = (a['summary_clean'] or a['summary']).replace('"', '""').replace('\n', '。')
            cat = a['category'].replace('"', '""')
            f.write(f'{get_id(a)},{cat},"{title}",{a["date"]},"{summarize(summary, 800)}",{a["url"]}\n')

    print(f"CSV已生成到: {csv_path}", file=sys.stderr)

    # 打印预览
    print("\n" + "=" * 60)
    print("干货预览（前15篇）：")
    print("=" * 60)
    for a in articles[:15]:
        print(f"\n[{a['category']}] {a['title']} ({a['date']})")
        print(f"  URL: {a['url']}")
        s = a['summary_clean'] or a['summary']
        print(f"  摘要: {summarize(s, 200)}")

    print(f"\n完成！共 {len(articles)} 篇文章。")

if __name__ == '__main__':
    main()
