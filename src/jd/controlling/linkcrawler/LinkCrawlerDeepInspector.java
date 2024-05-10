package jd.controlling.linkcrawler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jd.controlling.linkcrawler.LinkCrawler.LinkCrawlerGeneration;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadConnectionVerifier;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.PluginClassLoader;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.plugins.controller.host.PluginFinder;

public abstract class LinkCrawlerDeepInspector {
    /**
     * https://www.iana.org/assignments/media-types/media-types.xhtml
     *
     * @param urlConnection
     * @return
     */
    public boolean looksLikeDownloadableContent(final URLConnectionAdapter urlConnection) {
        Boolean verified = null;
        boolean looksLike = false;
        if (urlConnection.getResponseCode() == 200 || urlConnection.getResponseCode() == 206) {
            final long completeContentLength = urlConnection.getCompleteContentLength();
            final String contentType = urlConnection.getHeaderField(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE);
            final boolean hasContentType = StringUtils.isNotEmpty(contentType);
            final boolean hasContentLength = StringUtils.isNotEmpty(urlConnection.getHeaderField(HTTPConstants.HEADER_RESPONSE_CONTENT_LENGTH));
            final boolean allowsByteRanges = StringUtils.contains(urlConnection.getHeaderField(HTTPConstants.HEADER_RESPONSE_ACCEPT_RANGES), "bytes");
            final String eTag = urlConnection.getHeaderField(HTTPConstants.HEADER_ETAG);
            final boolean hasEtag = StringUtils.isNotEmpty(eTag);
            final boolean hasStrongEtag = hasEtag && !eTag.matches("(?i)^\\s*W/.*");
            final String filePathName = new Regex(urlConnection.getURL().getPath(), ".*?/([^/]+)$").getMatch(0);
            final long sizeDownloadableContent;
            if (StringUtils.endsWithCaseInsensitive(filePathName, ".epub")) {
                sizeDownloadableContent = 1 * 1024 * 1024l;
            } else {
                sizeDownloadableContent = 2 * 1024 * 1024l;
            }
            if (urlConnection.isContentDisposition()) {
                final String contentDispositionHeader = urlConnection.getHeaderField(HTTPConstants.HEADER_RESPONSE_CONTENT_DISPOSITION);
                final String contentDispositionFileName = HTTPConnectionUtils.getFileNameFromDispositionHeader(contentDispositionHeader);
                final boolean inlineFlag = contentDispositionHeader.matches("(?i)^\\s*inline\\s*;?.*");
                if (inlineFlag && (contentDispositionFileName != null && contentDispositionFileName.matches("(?i)^.*\\.html?$") || (hasContentType && isHtmlContent(urlConnection)))) {
                    // HTTP/1.1 200 OK
                    // Content-Type: text/html;
                    // Content-Disposition: inline; filename=error.html
                    looksLike = false;
                } else {
                    looksLike = true;
                }
            } else if (completeContentLength == 0) {
                return false;
            } else if (hasContentType && (!isTextContent(urlConnection) && contentType.matches("(?i)^(application|audio|video|image)/.+"))) {
                if (isOtherTextContent(urlConnection)) {
                    looksLike = false;
                } else if (looksLikeMpegURL(urlConnection)) {
                    looksLike = false;
                } else {
                    looksLike = true;
                }
            } else if (hasContentType && (!isTextContent(urlConnection) && contentType.matches("(?i)^binary/octet-stream"))) {
                looksLike = true;
            } else if (hasContentType && !isTextContent(urlConnection) && completeContentLength > 0) {
                looksLike = true;
            } else if (!hasContentType && completeContentLength > 0 && hasStrongEtag) {
                looksLike = true;
            } else if (!hasContentType && completeContentLength > 0 && allowsByteRanges) {
                // HTTP/1.1 200 OK
                // Content-Length: 1156000
                // Accept-Ranges: bytes
                looksLike = true;
            } else if (completeContentLength > sizeDownloadableContent && (!hasContentType || !isTextContent(urlConnection))) {
                looksLike = true;
            } else if (completeContentLength > sizeDownloadableContent && (allowsByteRanges || (urlConnection.getResponseCode() == 206 && urlConnection.getRange() != null))) {
                looksLike = true;
            } else if (filePathName != null && filePathName.matches("(?i).+\\.epub") && isPlainTextContent(urlConnection) && (!hasContentLength || completeContentLength > sizeDownloadableContent)) {
                looksLike = true;
            } else if (filePathName != null && filePathName.matches("(?i).+\\.srt") && isPlainTextContent(urlConnection) && (!hasContentLength || completeContentLength > 512 || hasEtag)) {
                /*
                 * Accept-Ranges: bytes
                 * 
                 * Content-Type: text/plain
                 * 
                 * ETag: "11968........"
                 * 
                 * Content-Length: 28...
                 */
                looksLike = true;
            }
        }
        if (looksLike) {
            /*  */
            try {
                final Set<LazyHostPlugin> plugins = new HashSet<LazyHostPlugin>();
                final Plugin currentActivePlugin = Plugin.getCurrentActivePlugin();
                if (currentActivePlugin instanceof DownloadConnectionVerifier) {
                    verified = ((DownloadConnectionVerifier) currentActivePlugin).verifyDownloadableContent(plugins, urlConnection);
                }
                if (verified == null) {
                    final String host = Browser.getHost(urlConnection.getURL());
                    final LazyHostPlugin lazyHostPlugin = new PluginFinder()._assignHost(host);
                    if (lazyHostPlugin != null && plugins.add(lazyHostPlugin)) {
                        final PluginClassLoaderChild pluginClassLoaderChild = PluginClassLoader.getThreadPluginClassLoaderChild();
                        final PluginForHost plugin = Plugin.getNewPluginInstance(currentActivePlugin, lazyHostPlugin, pluginClassLoaderChild);
                        if (plugin instanceof DownloadConnectionVerifier) {
                            verified = ((DownloadConnectionVerifier) plugin).verifyDownloadableContent(plugins, urlConnection);
                        }
                    }
                }
            } catch (Exception e) {
                LogController.CL().log(e);
            }
        }
        if (verified != null) {
            return verified.booleanValue();
        } else {
            return looksLike;
        }
    }

    /** Use this to check for HLS/.m3u8 content. */
    public static boolean looksLikeMpegURL(final URLConnectionAdapter urlConnection) {
        final String contentType = urlConnection != null ? urlConnection.getContentType() : null;
        return StringUtils.isNotEmpty(contentType) && (StringUtils.equalsIgnoreCase(contentType, "application/vnd.apple.mpegurl") || StringUtils.equalsIgnoreCase(contentType, "application/x-mpegurl"));
    }

    /** Use this to check for DASH/.mpd content. */
    public static boolean looksLikeDashURL(final URLConnectionAdapter urlConnection) {
        final String contentType = urlConnection != null ? urlConnection.getContentType() : null;
        return StringUtils.isNotEmpty(contentType) && (StringUtils.equalsIgnoreCase(contentType, "application/dash+xml"));
    }

    public boolean isTextContent(final URLConnectionAdapter urlConnection) {
        if (isOtherTextContent(urlConnection)) {
            return true;
        } else if (isPlainTextContent(urlConnection)) {
            return true;
        } else if (isHtmlContent(urlConnection)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isOtherTextContent(final URLConnectionAdapter urlConnection) {
        if (isHtmlContent(urlConnection)) {
            return false;
        } else {
            final String contentType = urlConnection.getContentType();
            return StringUtils.isNotEmpty(contentType) && (contentType.matches("(?i)application/(?!vnd)[^ ;]*(json|xml).*") || contentType.matches("(?i)text/xml.*"));
        }
    }

    public boolean isPlainTextContent(final URLConnectionAdapter urlConnection) {
        final String contentType = urlConnection != null ? urlConnection.getContentType() : null;
        return StringUtils.isNotEmpty(contentType) && StringUtils.containsIgnoreCase(contentType, "text/plain");
    }

    public boolean isHtmlContent(final URLConnectionAdapter urlConnection) {
        final String contentType = urlConnection != null ? urlConnection.getContentType() : null;
        return StringUtils.isNotEmpty(contentType) && (StringUtils.containsIgnoreCase(contentType, "text/html") || StringUtils.containsIgnoreCase(contentType, "application/xhtml+xml"));
    }

    public abstract List<CrawledLink> deepInspect(LinkCrawler lc, final LinkCrawlerGeneration generation, Browser br, URLConnectionAdapter urlConnection, final CrawledLink link) throws Exception;
}
