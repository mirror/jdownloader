package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "chirb.it" }, urls = { "https?://(?:www\\.)?chirb\\.it/[a-zA-Z0-9]+$" })
public class ChirbIt extends PluginForHost {
    public ChirbIt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.chirbit.com/help/tos";
    }

    private String url = null;

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(parameter.getPluginPatternMatcher());
        if (br.containsHTML("Page Not Found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String title = br.getRegex("\"chirbit-title\"\\s*>\\s*(.*?)\\s*<").getMatch(0);
        final String user = br.getRegex("\"chirbit-username\"\\s*>\\s*(.*?)\\s*<").getMatch(0);
        if (title == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        url = br.getRegex("data-fd\\s*=\\s*\"([^\"]+c0RHa)").getMatch(0);
        if (url != null) {
            url = Base64.decodeToString(new StringBuilder(url).reverse().toString());
        }
        final String fileTitle = title + " by " + user;
        parameter.setName(fileTitle);
        if (StringUtils.isNotEmpty(url) && parameter.getFinalFileName() == null) {
            parameter.setFinalFileName(fileTitle + getFileNameExtensionFromURL(url));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (StringUtils.isEmpty(url)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, true, 1);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
