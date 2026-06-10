# -*- coding: utf-8 -*-
"""
爬取 https://www.gswyl.com/ 全部文章，提炼干货，生成汇总报告
"""
import urllib.request
import urllib.error
import re
import os
import sys
import ssl

# 忽略SSL验证
ssl._create_default_https_context = ssl._create_unverified_context

BASE_URL = "https://www.gswyl.com"
HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
}

def fetch_url(url, timeout=20):
    """获取URL内容"""
    try:
        req = urllib.request.Request(url, headers=HEADERS)
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read()
            # Try different encodings
            content = None
            for enc in ['utf-8', 'gb2312', 'gbk', 'latin-1']:
                try:
                    content = raw.decode(enc)
                    break
                except:
                    continue
            if content is None:
                content = raw.decode('utf-8', errors='replace')
            return content
    except Exception as e:
        print(f"[ERROR] 获取 {url} 失败: {e}", file=sys.stderr)
        return None

def clean_html(html):
    """去除HTML标签，提取纯文本"""
    # Remove scripts and styles
    text = re.sub(r'<script[^>]*>.*?</script>', '', html, flags=re.DOTALL | re.IGNORECASE)
    text = re.sub(r'<style[^>]*>.*?</style>', '', text, flags=re.DOTALL | re.IGNORECASE)
    text = re.sub(r'<noscript[^>]*>.*?</noscript>', '', text, flags=re.DOTALL | re.IGNORECASE)
    # Remove HTML tags
    text = re.sub(r'<[^>]+>', ' ', text)
    # Remove HTML entities
    text = re.sub(r'&[a-zA-Z]+;', ' ', text)
    text = re.sub(r'&#\d+;', ' ', text)
    text = re.sub(r'&#x[0-9a-fA-F]+;', ' ', text)
    # Collapse whitespace
    text = re.sub(r'[ \t]+', ' ', text)
    text = re.sub(r'\n\s*\n', '\n\n', text)
    text = text.strip()
    return text

def find_article_links(html):
    """从HTML中提取文章链接"""
    links = set()
    # Find all href links
    hrefs = re.findall(r'href=["\']([^"\']+\.html)["\']', html)
    for h in hrefs:
        if h.startswith('http'):
            links.add(h)
        elif h.startswith('/'):
            links.add(BASE_URL + h)
        else:
            links.add(BASE_URL + '/' + h)

    # Also find navigation menu links
    hrefs2 = re.findall(r'href=["\'](/[^"\']+)["\']', html)
    for h in hrefs2:
        if not h.startswith('http'):
            # Filter out non-article links
            if any(x in h for x in ['.html', 'article', 'post', 'blog', 'detail', 'info', 'content', 'news']):
                links.add(BASE_URL + h)

    # General pattern for any path with .html or numeric IDs
    hrefs3 = re.findall(r'href=["\'](https?://[^"\']+)["\']', html)
    for h in hrefs3:
        if 'gswyl.com' in h:
            links.add(h)

    return list(links)

def extract_article_content(html):
    """从文章页面提取正文内容"""
    # Try to find main content area
    # Common patterns for article content
    content_patterns = [
        r'<article[^>]*>(.*?)</article>',
        r'<div[^>]*class="[^"]*content[^"]*"[^>]*>(.*?)</div>',
        r'<div[^>]*class="[^"]*article[^"]*"[^>]*>(.*?)</div>',
        r'<div[^>]*class="[^"]*post[^"]*"[^>]*>(.*?)</div>',
        r'<div[^>]*class="[^"]*entry[^"]*"[^>]*>(.*?)</div>',
        r'<div[^>]*id="content"[^>]*>(.*?)</div>',
    ]

    for pattern in content_patterns:
        match = re.search(pattern, html, re.DOTALL | re.IGNORECASE)
        if match:
            return clean_html(match.group(1))

    # Fall back to body content
    body = re.search(r'<body[^>]*>(.*?)</body>', html, re.DOTALL | re.IGNORECASE)
    if body:
        return clean_html(body.group(1))
    return clean_html(html)

def extract_title(html):
    """提取文章标题"""
    title_match = re.search(r'<title[^>]*>(.*?)</title>', html, re.IGNORECASE)
    if title_match:
        title = clean_html(title_match.group(1))
        # Remove site name suffix
        title = re.sub(r'\s*[-–—|]\s*.*$', '', title).strip()
        return title
    # Try h1
    h1_match = re.search(r'<h1[^>]*>(.*?)</h1>', html, re.IGNORECASE)
    if h1_match:
        return clean_html(h1_match.group(1))
    return "无标题"

def extract_key_points(text):
    """从文本中提取关键干货"""
    points = []
    lines = text.split('\n')

    for line in lines:
        line = line.strip()
        if len(line) < 20:
            continue
        # Skip navigation/breadcrumb type lines
        if line in ['首页', '文章', '关于', '联系', '更多']:
            continue
        points.append(line)

    return points

def main():
    print("=" * 80, file=sys.stderr)
    print("开始爬取 https://www.gswyl.com/", file=sys.stderr)
    print("=" * 80, file=sys.stderr)

    # Step 1: Fetch homepage
    print("\n[1/3] 获取首页...", file=sys.stderr)
    homepage_html = fetch_url(BASE_URL)
    if not homepage_html:
        print("无法获取首页内容!", file=sys.stderr)
        sys.exit(1)

    # Save homepage for debugging
    with open('homepage_debug.html', 'w', encoding='utf-8') as f:
        f.write(homepage_html)
    print("首页已保存到 homepage_debug.html", file=sys.stderr)

    # Print some structure for analysis
    # Find all unique href patterns
    all_links = re.findall(r'href=["\']([^"\']+)["\']', homepage_html)
    print(f"\n首页共发现 {len(all_links)} 个链接", file=sys.stderr)

    # Print all .html links
    html_links = [l for l in all_links if '.html' in l or l.endswith('/')]
    print("\nHTML链接:", file=sys.stderr)
    for l in sorted(set(html_links)):
        if 'gswyl.com' in l or l.startswith('/'):
            print(f"  {l}", file=sys.stderr)

    # Step 2: Find article links
    print("\n\n[2/3] 提取文章链接...", file=sys.stderr)
    article_links = find_article_links(homepage_html)

    # Also look for WordPress-style URLs or directory URLs
    # Try common WordPress patterns
    wp_patterns = [
        '/index.php/archives/',
        '/archives/',
        '/category/',
        '/tag/',
        '/p/',
        '/post/',
        '/article/',
    ]

    # Print all unique links for analysis
    unique_links = sorted(set([l for l in all_links if 'gswyl.com' in l or (l.startswith('/') and l != '/')]))
    print(f"\n站内唯一链接 ({len(unique_links)} 个):", file=sys.stderr)
    for l in unique_links:
        print(f"  {l}", file=sys.stderr)

    # Let's also check if there's a sitemap or categories
    sitemap_url = BASE_URL + '/sitemap.xml'
    sitemap_html = fetch_url(sitemap_url)
    if sitemap_html:
        print("\n发现sitemap.xml!", file=sys.stderr)
        with open('sitemap_debug.xml', 'w', encoding='utf-8') as f:
            f.write(sitemap_html)

    # Check common WordPress API endpoints
    api_urls = [
        '/wp-json/wp/v2/posts',
        '/wp-json/wp/v2/pages',
        '/api/posts',
        '/feed',
        '/rss',
    ]

    for api_url in api_urls:
        print(f"\n尝试 {api_url}...", file=sys.stderr)
        content = fetch_url(BASE_URL + api_url)
        if content and len(content) > 50:
            print(f"  成功! 长度: {len(content)}", file=sys.stderr)
            with open(f'api_debug{api_url.replace("/","_")}.txt', 'w', encoding='utf-8') as f:
                f.write(content)

    # Step 3: Visit each article
    print(f"\n\n[3/3] 共找到 {len(article_links)} 个文章链接", file=sys.stderr)

    articles = []
    for i, link in enumerate(article_links[:50]):  # Limit to 50 first
        print(f"\n[{i+1}/{min(len(article_links), 50)}] 获取: {link}", file=sys.stderr)
        article_html = fetch_url(link)
        if not article_html:
            continue

        title = extract_title(article_html)
        content = extract_article_content(article_html)

        # Truncate content
        if len(content) > 5000:
            content = content[:5000] + "..."

        articles.append({
            'title': title,
            'url': link,
            'content': content,
        })
        print(f"  标题: {title}", file=sys.stderr)
        print(f"  内容长度: {len(content)}", file=sys.stderr)

    # Print final summary
    print("\n\n" + "=" * 80, file=sys.stderr)
    print(f"总计爬取 {len(articles)} 篇文章", file=sys.stderr)
    for a in articles:
        print(f"\n--- {a['title']} ---", file=sys.stderr)
        print(f"URL: {a['url']}", file=sys.stderr)
        print(a['content'][:500], file=sys.stderr)

if __name__ == '__main__':
    main()
