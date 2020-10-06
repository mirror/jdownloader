package jd.controlling.linkcrawler;

import java.util.List;

import jd.controlling.linkcrawler.LinkCrawler.LinkCrawlerGeneration;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

public abstract class LinkCrawlerDeepInspector {
    /**
     * https://www.iana.org/assignments/media-types/media-types.xhtml
     *
     * @param urlConnection
     * @return
     */
    public boolean looksLikeDownloadableContent(final URLConnectionAdapter urlConnection) {
        if (urlConnection.getResponseCode() == 200 || urlConnection.getResponseCode() == 206) {
            final boolean hasContentType = StringUtils.isNotEmpty(urlConnection.getHeaderField(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE));
            final boolean hasContentLength = StringUtils.isNotEmpty(urlConnection.getHeaderField(HTTPConstants.HEADER_RESPONSE_CONTENT_LENGTH));
            final String filePathName = new Regex(urlConnection.getURL().getPath(), ".*?/([^/]+)$").getMatch(0);
            final long completeContentLength = urlConnection.getCompleteContentLength();
            final long sizeDownloadableContent;
            if (StringUtils.endsWithCaseInsensitive(filePathName, ".epub")) {
                sizeDownloadableContent = 1 * 1024 * 1024l;
            } else {
                sizeDownloadableContent = 2 * 1024 * 1024l;
            }
            if (urlConnection.isContentDisposition()) {
                return true;
            } else if (hasContentType && StringUtils.containsIgnoreCase(urlConnection.getContentType(), "application/force-download")) {
                return true;
            } else if (hasContentType && StringUtils.containsIgnoreCase(urlConnection.getContentType(), "application/octet-stream")) {
                return true;
            } else if (hasContentType && StringUtils.containsIgnoreCase(urlConnection.getContentType(), "application/x-rar-compressed")) {
                return true;
            } else if (hasContentType && StringUtils.containsIgnoreCase(urlConnection.getContentType(), "application/zip")) {
                return true;
            } else if (hasContentType && StringUtils.containsIgnoreCase(urlConnection.getContentType(), "audio/")) {
                return true;
            } else if (hasContentType && StringUtils.containsIgnoreCase(urlConnection.getContentType(), "video/")) {
                return true;
            } else if (hasContentType && StringUtils.containsIgnoreCase(urlConnection.getContentType(), "image/")) {
                return true;
            } else if (completeContentLength > sizeDownloadableContent && (!hasContentType || !isTextContent(urlConnection))) {
                return true;
            } else if (completeContentLength > sizeDownloadableContent && (StringUtils.contains(urlConnection.getHeaderField(HTTPConstants.HEADER_RESPONSE_ACCEPT_RANGES), "bytes") || (urlConnection.getResponseCode() == 206 && urlConnection.getRange() != null))) {
                return true;
            } else if (StringUtils.endsWithCaseInsensitive(filePathName, ".epub") && isPlainTextContent(urlConnection) && (!hasContentLength || completeContentLength > sizeDownloadableContent)) {
                return true;
            }
        }
        return false;
    }

    public boolean isTextContent(final URLConnectionAdapter urlConnection) {
        return isOtherTextContent(urlConnection) || isPlainTextContent(urlConnection) || isHtmlContent(urlConnection);
    }

    public boolean isOtherTextContent(final URLConnectionAdapter urlConnection) {
        final String contentType = urlConnection.getContentType();
        return StringUtils.containsIgnoreCase(contentType, "application/json") || StringUtils.containsIgnoreCase(contentType, "application/xml");
    }

    public boolean isPlainTextContent(final URLConnectionAdapter urlConnection) {
        final String contentType = urlConnection.getContentType();
        return StringUtils.containsIgnoreCase(contentType, "text/plain");
    }

    public boolean isHtmlContent(final URLConnectionAdapter urlConnection) {
        final String contentType = urlConnection.getContentType();
        return StringUtils.containsIgnoreCase(contentType, "text/html") || StringUtils.containsIgnoreCase(contentType, "application/xhtml+xml");
    }

    public abstract List<CrawledLink> deepInspect(LinkCrawler lc, final LinkCrawlerGeneration generation, Browser br, URLConnectionAdapter urlConnection, final CrawledLink link) throws Exception;
}
