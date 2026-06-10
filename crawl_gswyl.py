# -*- coding: utf-8 -*-
"""
爬取 https://www.gswyl.com/ 全部文章，提炼干货，生成Excel汇总报告
"""
import re
import ssl
import time
import sys
import os

ssl._create_default_https_context = ssl._create_unverified_context

try:
    import urllib.request
    import urllib.error
except:
    pass

BASE = "https://www.gswyl.com"
HDR = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Accept': 'text/html,application/xhtml+xml',
}

def get(url, enc='utf-8', retries=2):
    for _ in range(retries):
        try:
            req = urllib.request.Request(url, headers=HDR)
            with urllib.request.urlopen(req, timeout=25) as r:
                raw = r.read()
                try:
                    return raw.decode(enc)
                except:
                    return raw.decode('utf-8', errors='replace')
        except Exception as e:
            time.sleep(1)
    print(f"  [FAIL] {url}", file=sys.stderr)
    return None

def strip_tags(html):
    t = re.sub(r'<script[^>]*>.*?</script>', '', html, flags=re.DOTALL|re.I)
    t = re.sub(r'<style[^>]*>.*?</style>', '', t, flags=re.DOTALL|re.I)
    t = re.sub(r'<[^>]+>', ' ', t)
    t = re.sub(r'&[a-z]+;', ' ', t)
    t = re.sub(r'&#?\w+;', ' ', t)
    t = re.sub(r'[ \t]+', ' ', t)
    t = re.sub(r'\n\s*\n+', '\n', t)
    return t.strip()

def extract_articles_from_list(html):
    """从列表页提取文章链接和标题"""
    articles = []
    # 匹配 article 块
    items = re.findall(r'<article[^>]*>.*?href="([^"]+)"[^>]*title="([^"]*)"', html, re.DOTALL|re.I)
    for url, title in items:
        if '/post/' in url and 'gswyl.com' not in url:
            url = BASE + url
        if '/post/' in url:
            articles.append({'url': url, 'title': title.strip()})

    # 如果上面没匹配到，用备用方法
    if not articles:
        links = re.findall(r'href="(/post/\d+\.html)"', html)
        titles = re.findall(r'<a[^>]*href="[^"]*post/\d+\.html"[^>]*title="([^"]*)"', html)
        for i, link in enumerate(links):
            title = titles[i] if i < len(titles) else '无标题'
            articles.append({'url': BASE + link, 'title': title.strip()})
    return articles

def get_article_body(html):
    """提取文章正文"""
    # 尝试匹配 post-body 或 article-content
    m = re.search(r'<div[^>]*class="[^"]*post-body[^"]*"[^>]*>(.*?)</div>\s*<(?:div|footer)', html, re.DOTALL|re.I)
    if not m:
        m = re.search(r'<div[^>]*class="[^"]*article-content[^"]*"[^>]*>(.*?)</div>', html, re.DOTALL|re.I)
    if not m:
        m = re.search(r'<div[^>]*class="[^"]*post-content[^"]*"[^>]*>(.*?)</div>', html, re.DOTALL|re.I)
    if not m:
        m = re.search(r'<article[^>]*>(.*?)</article>', html, re.DOTALL|re.I)
    if m:
        return strip_tags(m.group(1))
    return strip_tags(html)

def get_title(html):
    m = re.search(r'<title[^>]*>(.*?)</title>', html, re.I)
    if m:
        t = strip_tags(m.group(1))
        t = re.sub(r'\s*[-–—|]\s*.*$', '', t).strip()
        return t
    m = re.search(r'<h1[^>]*>(.*?)</h1>', html, re.I)
    if m:
        return strip_tags(m.group(1))
    return '无标题'

def get_category(html):
    m = re.search(r'class="[^"]*category[^"]*"[^>]*>\s*<a[^>]*>([^<]+)</a>', html, re.I)
    if m:
        return m.group(1).strip()
    return '其他'

def get_date(html):
    m = re.search(r'(\d{4}-\d{2}-\d{2}\s*\d{2}:\d{2}:\d{2})', html)
    if m:
        return m.group(1)
    m = re.search(r'(\d{4}-\d{2}-\d{2})', html)
    if m:
        return m.group(1)
    return ''

def filter_noise(text):
    """过滤无营养的推广内容"""
    # 去掉常见推广话术
    noise = [
        '关注公众号',
        '点菜单找古圣领取',
        '古圣微说',
        '扫码进入',
        '添加微信',
        '复制微信',
        '免费领取全套引流',
        '联系古圣',
        '联系QQ',
        '微信号已复制',
        '复制微信号',
        '关注公众号',
        '备注：网站添加',
        'GSWLPP',
        'GSWLYY',
        'GSWL69',
        '本文链接：',
        '版权声明',
    ]
    lines = text.split('\n')
    clean_lines = []
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
            clean_lines.append(line)
    return '\n'.join(clean_lines)

def summarize_content(text, max_len=800):
    """摘要提取：取前max_len字符，但要完整句子"""
    text = text.strip()
    if len(text) <= max_len:
        return text
    # 在max_len附近找到句号、换行等断点
    cutoff = max_len
    for sep in ['。', '！', '？', '\n', '；']:
        pos = text.rfind(sep, 0, max_len + 100)
        if pos > max_len - 100:
            cutoff = pos + 1
            break
    result = text[:cutoff].strip()
    if len(result) < len(text):
        result += '...'
    return result

def main():
    print("=" * 60)
    print("古圣引流网 - 全站文章爬取与干货提炼")
    print("=" * 60)

    all_articles = []
    seen_urls = set()

    # 1) 获取首页文章列表
    print("\n[1] 爬取文章列表 (75页)...")
    for page in range(1, 76):
        if page == 1:
            url = BASE + '/'
        else:
            url = f'{BASE}/page_{page}.html'

        html = get(url)
        if not html:
            print(f"  第{page}页获取失败，跳过", file=sys.stderr)
            continue

        articles = extract_articles_from_list(html)
        new_count = 0
        for a in articles:
            if a['url'] not in seen_urls:
                seen_urls.add(a['url'])
                all_articles.append(a)
                new_count += 1

        print(f"  第{page}页: 新发现 {new_count} 篇文章 (累计 {len(all_articles)})", file=sys.stderr)
        time.sleep(0.3)

    print(f"\n共发现 {len(all_articles)} 篇独立文章")

    # 2) 逐篇爬取正文
    print("\n[2] 逐篇获取正文...")
    results = []
    for i, a in enumerate(all_articles):
        print(f"  [{i+1}/{len(all_articles)}] {a['title'][:50]}...", file=sys.stderr)
        html = get(a['url'])
        if not html:
            results.append({
                'title': a['title'],
                'url': a['url'],
                'category': '',
                'date': '',
                'summary': '',
                'body': '',
            })
            continue

        title = get_title(html)
        body = get_article_body(html)
        body_clean = filter_noise(body)
        summary = summarize_content(body_clean)
        category = get_category(html)
        date = get_date(html)

        results.append({
            'title': title,
            'url': a['url'],
            'category': category,
            'date': date,
            'summary': summary,
            'body': body_clean,
        })
        time.sleep(0.3)

    # 3) 保存中间结果
    print("\n[3] 保存中...")

    # 保存完整数据 (JSON)
    try:
        import json
        with open('gswyl_full_data.json', 'w', encoding='utf-8') as f:
            json.dump(results, f, ensure_ascii=False, indent=2)
        print("  完整数据已保存到 gswyl_full_data.json")
    except:
        pass

    # 保存CSV到桌面
    desktop = os.path.join(os.path.expanduser('~'), 'Desktop')
    csv_path = os.path.join(desktop, '古圣引流网_干货汇总.csv')

    with open(csv_path, 'w', encoding='utf-8-sig') as f:
        f.write('序号,分类,标题,发布日期,干货摘要,原文链接\n')
        for i, r in enumerate(results):
            # Excel安全处理：替换换行、引号
            title = r['title'].replace('"', '""')
            summary = r['summary'].replace('"', '""').replace('\n', '。')
            category = r['category'].replace('"', '""')
            date = r['date']
            url = r['url']
            f.write(f'{i+1},{category},"{title}",{date},"{summary}",{url}\n')

    print(f"\n  汇总CSV已生成到: {csv_path}")
    print(f"  共 {len(results)} 篇文章")

    # 4) 输出到控制台
    print("\n" + "=" * 60)
    print("干货汇总预览（前20篇）：")
    print("=" * 60)
    for i, r in enumerate(results[:20]):
        print(f"\n--- [{r['category']}] {r['title']} ({r['date']}) ---")
        print(f"URL: {r['url']}")
        print(r['summary'][:300])

    print(f"\n总计 {len(results)} 篇文章已汇总。")

if __name__ == '__main__':
    main()
