package com.apexai.mobile;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

final class WebResearcher {
    static final class ResultItem {
        final String title, snippet, url;
        ResultItem(String t, String s, String u) { title=t; snippet=s; url=u; }
    }

    static List<ResultItem> search(String query, int limit) throws Exception {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) return new ArrayList<>();
        Document doc = Jsoup.connect("https://html.duckduckgo.com/html/?q=" + URLEncoder.encode(q, "UTF-8"))
                .userAgent("Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/131 Mobile Safari/537.36")
                .referrer("https://duckduckgo.com/")
                .timeout(18000)
                .maxBodySize(2 * 1024 * 1024)
                .get();
        Elements entries = doc.select(".result");
        List<ResultItem> found = new ArrayList<>();
        for (Element entry : entries) {
            Element a = entry.selectFirst("a.result__a");
            Element s = entry.selectFirst(".result__snippet");
            if (a == null) continue;
            String title = a.text().trim();
            String url = a.absUrl("href");
            String snippet = s == null ? "" : s.text().trim();
            if (!title.isEmpty() && !url.isEmpty()) found.add(new ResultItem(title, snippet, url));
            if (found.size() >= Math.max(1, Math.min(8, limit))) break;
        }
        return found;
    }

    static String format(List<ResultItem> items) {
        StringBuilder b = new StringBuilder();
        for (int i=0;i<items.size();i++) {
            ResultItem r = items.get(i);
            b.append(i+1).append(". ").append(r.title).append("\\n")
                    .append(r.snippet).append("\\nالمصدر: ").append(r.url).append("\\n\\n");
        }
        return b.toString();
    }
    private WebResearcher() {}
}
