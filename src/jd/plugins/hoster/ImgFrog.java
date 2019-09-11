package jd.plugins.hoster;

import java.net.URL;

import jd.PluginWrapper;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision: 36569 $", interfaceVersion = 2, names = { "imgfrog.pw" }, urls = { "https?://(cdn\\.)?imgfrog\\.(?:cf|pw)/(i/([a-zA-Z0-9\\-_]+\\.)?[a-zA-Z0-9]{4,}|f/[0-9a-zA-Z\\._\\-%,]+)" })
public class ImgFrog extends antiDDoSForHost {
    public ImgFrog(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://imgfrog.pw/page/tos";
    }

    public static boolean isImageLink(DownloadLink link) {
        final String url = link.getPluginPatternMatcher();
        return url.matches("https?://imgfrog\\.(?:cf|pw)/i/([a-zA-Z0-9\\-_]+\\.)?[a-zA-Z0-9]{4,}");
    }

    public static boolean isFileLink(DownloadLink link) {
        final String url = link.getPluginPatternMatcher();
        return url.matches("https?://cdn\\.imgfrog\\.(?:cf|pw)/f/[0-9a-zA-Z\\._\\-%,]+");
    }

    @Override
    public String rewriteHost(String host) {
        if (host == null) {
            return getHost();
        } else if ("imgfrog.cf".equals(host) || "imgfrog.pw".equals(host)) {
            return getHost();
        } else {
            return super.rewriteHost(host);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        br.setFollowRedirects(true);
        if (isFileLink(link)) {
            if (!(Thread.currentThread() instanceof SingleDownloadController)) {
                final Browser brc = br.cloneBrowser();
                brc.getHeaders().put("Accept-Encoding", "identity");
                final URLConnectionAdapter con = openAntiDDoSRequestConnection(brc, brc.createHeadRequest(link.getPluginPatternMatcher()));
                try {
                    if (con.isOK() && !StringUtils.containsIgnoreCase(con.getContentType(), "text")) {
                        if (con.getLongContentLength() > 0) {
                            link.setDownloadSize(con.getCompleteContentLength());
                        }
                        if (!link.isNameSet()) {
                            link.setFinalFileName(getFileNameFromHeader(con));
                        }
                        return AvailableStatus.TRUE;
                    } else {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                } finally {
                    con.disconnect();
                }
            }
            if (!link.isNameSet()) {
                link.setFinalFileName(getFileNameFromURL(new URL(link.getPluginPatternMatcher())));
            }
            return AvailableStatus.UNCHECKED;
        } else if (isImageLink(link)) {
            getPage(link.getPluginPatternMatcher());
            if (!canHandle(br.getURL())) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String title = br.getRegex("<meta property\\s*=\\s*\"og:title\"\\s*content\\s*=\\s*\"\\s*(.*?)\\s*\"").getMatch(0);
            final String url = br.getRegex("<meta property\\s*=\\s*\"og:image\"\\s*content\\s*=\\s*\"(https?://.*?)\"").getMatch(0);
            if (url == null || title == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!link.isNameSet()) {
                link.setFinalFileName(title + getFileNameExtensionFromURL(url, ".jpg"));
            }
            if (!(Thread.currentThread() instanceof SingleDownloadController)) {
                final Browser brc = br.cloneBrowser();
                brc.getHeaders().put("Accept-Encoding", "identity");
                final URLConnectionAdapter con = openAntiDDoSRequestConnection(brc, brc.createHeadRequest(url));
                try {
                    if (con.isOK() && StringUtils.containsIgnoreCase(con.getContentType(), "image")) {
                        if (con.getLongContentLength() > 0) {
                            link.setDownloadSize(con.getCompleteContentLength());
                        }
                    } else {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                } finally {
                    con.disconnect();
                }
            }
            return AvailableStatus.TRUE;
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        final String url;
        if (isFileLink(link)) {
            url = link.getPluginPatternMatcher();
        } else if (isImageLink(link)) {
            url = br.getRegex("<meta property\\s*=\\s*\"og:image\"\\s*content\\s*=\\s*\"(https?://.*?)\"").getMatch(0);
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            br.getHeaders().put("Accept-Encoding", "identity");
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, false, 1);
            if (!dl.getConnection().isOK() || dl.getConnection().getContentType().contains("text")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
