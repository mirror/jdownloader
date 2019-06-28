package jd.controlling.linkcrawler;

import java.util.List;

import jd.controlling.linkcrawler.LinkCrawler.LinkCrawlerGeneration;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.utils.StringUtils;

public abstract class LinkCrawlerDeepInspector {
    public boolean looksLikeDownloadableContent(final URLConnectionAdapter urlConnection) {
        final boolean hasContentType = urlConnection.getHeaderField(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE) != null;
        if (urlConnection.getResponseCode() == 200) {
            final long sizeDownloadableContent = 2 * 1024 * 1024l;
            if (urlConnection.isContentDisposition()) {
                return true;
            } else if (hasContentType && StringUtils.contains(urlConnection.getContentType(), "octet-stream")) {
                return true;
            } else if (hasContentType && StringUtils.contains(urlConnection.getContentType(), "audio")) {
                return true;
            } else if (hasContentType && StringUtils.contains(urlConnection.getContentType(), "video")) {
                return true;
            } else if (hasContentType && StringUtils.contains(urlConnection.getContentType(), "image")) {
                return true;
            } else if (urlConnection.getLongContentLength() > sizeDownloadableContent && (!hasContentType || (!StringUtils.contains(urlConnection.getContentType(), "text") && !StringUtils.containsIgnoreCase(urlConnection.getContentType(), "application/json")))) {
                return true;
            } else if (urlConnection.getLongContentLength() > sizeDownloadableContent && StringUtils.contains(urlConnection.getHeaderField(HTTPConstants.HEADER_RESPONSE_ACCEPT_RANGES), "bytes")) {
                return true;
            }
        }
        return false;
    }

    public abstract List<CrawledLink> deepInspect(LinkCrawler lc, final LinkCrawlerGeneration generation, Browser br, URLConnectionAdapter urlConnection, final CrawledLink link) throws Exception;
}
