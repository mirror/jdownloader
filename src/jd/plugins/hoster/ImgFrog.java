package jd.plugins.hoster;

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

@HostPlugin(revision = "$Revision: 36569 $", interfaceVersion = 2, names = { "imgfrog.cf" }, urls = { "https?://imgfrog\\.cf/i/[a-zA-Z0-9\\-_]+\\.[a-zA-Z0-9]{4,}" })
public class ImgFrog extends antiDDoSForHost {
    public ImgFrog(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://imgfrog.cf/page/tos";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        br.setFollowRedirects(true);
        getPage(parameter.getPluginPatternMatcher());
        if (!canHandle(br.getURL())) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String title = br.getRegex("<meta property\\s*=\\s*\"og:title\"\\s*content\\s*=\\s*\"\\s*(.*?)\\s*\"").getMatch(0);
        final String url = br.getRegex("<meta property\\s*=\\s*\"og:image\"\\s*content\\s*=\\s*\"(https?://.*?)\"").getMatch(0);
        if (url == null || title == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!parameter.isNameSet()) {
            parameter.setFinalFileName(title + getFileNameExtensionFromURL(url, ".jpg"));
        }
        if (!(Thread.currentThread() instanceof SingleDownloadController)) {
            final Browser brc = br.cloneBrowser();
            brc.getHeaders().put("Accept-Encoding", "identity");
            final URLConnectionAdapter con = brc.openHeadConnection(url);
            try {
                if (con.isOK() && StringUtils.containsIgnoreCase(con.getContentType(), "image")) {
                    if (con.getLongContentLength() > 0) {
                        parameter.setDownloadSize(con.getCompleteContentLength());
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                con.disconnect();
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        final String url = br.getRegex("<meta property\\s*=\\s*\"og:image\"\\s*content\\s*=\\s*\"(https?://.*?)\"").getMatch(0);
        if (url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            br.getHeaders().put("Accept-Encoding", "identity");
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, false, 1);
            if (!dl.getConnection().isOK() || dl.getConnection().getContentType().contains("html")) {
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
