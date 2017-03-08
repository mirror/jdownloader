package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yourporn.sexy" }, urls = { "https?://(www\\.)?yourporn\\.sexy/post/[a-fA-F0-9]{13}\\.html" })
public class YourPornSexy extends PluginForHost {

    public YourPornSexy(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return null;
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        if (link.getSetLinkID() == null) {
            final String id = new Regex(link.getPluginPatternMatcher(), "/post/([a-fA-F0-9]{13})\\.html").getMatch(0);
            link.setLinkID(getHost() + "://" + id);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        final String title = br.getRegex("name\" content=\"(.*?)\"").getMatch(0);
        if (title == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String mp4 = br.getRegex("<video id='.*?'\\s*src=(\"|')((?:https?:)?//.*?\\.mp4)(\"|')").getMatch(1);
        if (mp4 == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        link.setFinalFileName(title.trim() + ".mp4");
        if (link.getVerifiedFileSize() == -1) {
            final URLConnectionAdapter con = br.cloneBrowser().openHeadConnection(mp4);
            try {
                if (con.isOK() && StringUtils.containsIgnoreCase(con.getContentType(), "video")) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
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
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        final String mp4 = br.getRegex("<video id='.*?'\\s*src=(\"|')((?:https?:)?//.*?\\.mp4)(\"|')").getMatch(1);
        if (mp4 == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, mp4, true, -2);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
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
