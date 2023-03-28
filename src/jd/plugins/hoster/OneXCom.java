package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "1x.com" }, urls = { "https?://(?:www\\.)?1x\\.com/photo/\\d+" })
public class OneXCom extends PluginForHost {
    public OneXCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://1x.com/about";
    }

    private String imageURL = null;

    @Override
    public String getLinkID(DownloadLink link) {
        final String imageID = getImageID(link);
        if (imageID != null) {
            return getHost() + "/" + imageID;
        } else {
            return super.getLinkID(link);
        }
    }

    private final String getImageID(DownloadLink link) {
        final String imageID = link != null ? new Regex(link.getPluginPatternMatcher(), "/photo/(\\d+)").getMatch(0) : null;
        return imageID;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(parameter.getPluginPatternMatcher());
        final String imageID = getImageID(parameter);
        final String title = br.getRegex("<title>\\s*(.*?)\\s*</title>").getMatch(0);
        imageURL = br.getRegex("id\\s*=\\s*\"img-" + imageID + "\"[^>]*src\\s*=\\s*\"(https?://[^/]+/images[^\"]+)\"").getMatch(0);
        if (imageURL == null) {
            if (br.containsHTML(">\\s*We cannot find that") || br.containsHTML(">\\s*404\\s*</div")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        parameter.setFinalFileName(title + getFileNameExtensionFromURL(imageURL, ".jpg"));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (imageURL == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, imageURL, false, 1);
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
