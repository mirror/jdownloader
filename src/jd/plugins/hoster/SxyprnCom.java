package jd.plugins.hoster;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yourporn.sexy" }, urls = { "https?://(www\\.)?(yourporn\\.sexy|sxyprn\\.com)/post/[a-fA-F0-9]{13}\\.html" })
public class SxyprnCom extends antiDDoSForHost {
    public SxyprnCom(PluginWrapper wrapper) {
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

    private String  json         = null;
    private String  dllink       = null;
    private boolean server_issue = false;
    private String  authorid     = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        json = null;
        dllink = null;
        br.setFollowRedirects(true);
        getPage(link.getPluginPatternMatcher());
        final String title = br.getRegex("name\" content=\"(.*?)\"").getMatch(0);
        /** 2019-07-08: yourporn.sexy now redirects to youporn.com but sxyprn.com still exists. */
        if (title == null || br.getHost().equals("youporn.com")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (link.getFinalFileName() == null) {
            link.setFinalFileName(title.trim() + ".mp4");
        }
        String fid = new Regex(link.getLinkID(), "//([a-z0-9]+)").getMatch(0);
        authorid = br.getRegex("data-authorid='([^']+)'").getMatch(0);
        json = br.getRegex("data-vnfo=\\'([^\\']+)\\'").getMatch(0);
        String vnfo = PluginJSonUtils.getJsonValue(json, fid);
        if (vnfo == null && json != null) {
            final String ids[] = new Regex(json, "\"([a-z0-9]*?)\"").getColumn(0);
            for (final String id : ids) {
                vnfo = PluginJSonUtils.getJsonValue(json, id);
                dllink = getDllink(link, vnfo);
                if (dllink != null) {
                    break;
                }
            }
        } else {
            dllink = getDllink(link, vnfo);
        }
        return AvailableStatus.TRUE;
    }

    private long ssut51(final String input) {
        final String num = input.replaceAll("[^0-9]", "");
        long ret = 0;
        for (int i = 0; i < num.length(); i++) {
            ret += Long.parseLong(String.valueOf(num.charAt(i)), 10);
        }
        return ret;
    }

    private String getDllink(final DownloadLink link, final String vnfo) throws Exception {
        final String tmp[] = vnfo.split("/");
        tmp[1] += "8";
        tmp[5] = String.valueOf((Long.parseLong(tmp[5]) - (ssut51(tmp[6]) + ssut51(tmp[7]))));
        final String url = "/" + StringUtils.join(tmp, "/");
        Browser brc = br.cloneBrowser();
        brc.setFollowRedirects(false);
        brc.getPage(url);
        final String redirect = brc.getRedirectLocation();
        if (redirect != null && link.getVerifiedFileSize() == -1) {
            brc = br.cloneBrowser();
            final URLConnectionAdapter con = openAntiDDoSRequestConnection(brc, brc.createHeadRequest(redirect));
            try {
                if (con.isOK() && StringUtils.containsIgnoreCase(con.getContentType(), "video")) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                    return redirect;
                }
            } finally {
                con.disconnect();
            }
        }
        if (redirect == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return redirect;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 10;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (dllink == null) {
            if (json != null && json.length() <= 10) {
                /* Rare case: E.g. empty json object '[]' */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: No video source available");
            } else if (server_issue) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: All video sources are broken");
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -2);
        if (dl.getConnection().getContentType().contains("text")) {
            br.followConnection();
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                /*
                 * 2019-09-24: E.g. serverside broken content, videos will not even play via browser. This may also happen when a user opens
                 * up a lot of connections to this host!
                 */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
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
