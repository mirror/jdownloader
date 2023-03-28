package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "soundgasm.net" }, urls = { "https?://(?:www\\.)?soundgasm\\.net/u/[^/]+/[^/]+" })
public class SoundGasmNet extends PluginForHost {
    public SoundGasmNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return null;
    }

    private String url = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)Page Not Found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String title = br.getRegex("\"title\"\\s*>\\s*(.*?)\\s*<").getMatch(0);
        final String user = br.getRegex("soundgasm\\.net/u/.*?\"\\s*>\\s*(.*?)\\s*<").getMatch(0);
        if (title == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        url = br.getRegex("(https?://media\\.soundgasm\\.net/sounds/[a-f0-9]+\\.(ogg|m4a|mp3))").getMatch(0);
        final String fileTitle = title + " by " + user;
        link.setName(fileTitle);
        if (StringUtils.isNotEmpty(url) && link.getFinalFileName() == null) {
            link.setFinalFileName(fileTitle + getFileNameExtensionFromURL(url));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (StringUtils.isEmpty(url)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, true, 1);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
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
