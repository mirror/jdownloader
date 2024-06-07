//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class MixUploadOrg extends PluginForHost {
    public MixUploadOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.AUDIO_STREAMING, LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    @Override
    public String getAGBLink() {
        return "http://" + getHost() + "/about/agreement";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "mixupload.org", "mixupload.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([a-z]{2}/)?track/([\\w\\-]+)-(\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(2);
    }

    private String dllink = null;

    /**
     * TODO: Add account support & show filesizes based on accounts e.g. show full size (available via '/player/getTrackInfo/') for premium
     * users and stream size for free users.
     */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        final String extDefault = ".mp3";
        if (!link.isNameSet()) {
            final String titleSlug = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
            link.setName(titleSlug.replace("-", " ").trim() + extDefault);
        }
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String trackID = link.getStringProperty("trackid");
        if (trackID == null) {
            br.setAllowedResponseCodes(new int[] { 451 });
            br.getPage(link.getPluginPatternMatcher());
            if (br.getHttpConnection().getResponseCode() == 451) {
                /* 2020-11-23: HTTP/1.1 451 Unavailable For Legal Reasons */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML(">Page not found<|>Error<|\"/img/404\\-img\\.png\"|\"/img/forbidden\\.png\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            trackID = br.getRegex("id=\"pl_track(\\d+)\"").getMatch(0);
            if (trackID == null) {
                trackID = br.getRegex("p\\.playTrackId\\((\\d+)\\)").getMatch(0);
                if (trackID == null) {
                    trackID = br.getRegex("p\\.playTrackIdPart\\((\\d+)").getMatch(0);
                    if (trackID == null) {
                        trackID = br.getRegex("var page_id = \\'(\\d+)\\';").getMatch(0);
                    }
                }
            }
            if (trackID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty("trackid", trackID);
        }
        br.getPage("http://" + getHost() + "/player/getTrackInfo/" + trackID);
        if ("[]".equals(br.getRequest().getHtmlCode().trim())) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = getJson("artist") + " - " + getJson("title");
        if (filename.contains("null")) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        Account premiumAcc = null;
        List<Account> accs = AccountController.getInstance().getValidAccounts(getHost());
        if (accs != null && accs.size() > 0) {
            for (Account acc : accs) {
                if (acc.isEnabled() && AccountType.PREMIUM.equals(acc.getType())) {
                    premiumAcc = acc;
                }
            }
        }
        if (premiumAcc != null) {
            /* Premium users can download the high quality track --> Filesize is given via 'API' */
            dllink = "http://" + getHost() + "/download/" + trackID;
            final String filesizeBytesStr = getJson("sizebyte");
            if (filesizeBytesStr == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setDownloadSize(Long.parseLong(filesizeBytesStr));
        } else {
            dllink = "http://" + getHost() + "/player/play/" + trackID + "/0/track.mp3";
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(dllink);
                if (!this.looksLikeDownloadableContent(con)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (con.isContentDecoded()) {
                    link.setDownloadSize(con.getCompleteContentLength());
                } else {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        link.setFinalFileName(Encoding.htmlDecode(filename).trim() + extDefault);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getJson(final String parameter) {
        String result = br.getRegex("\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) {
            result = br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        }
        return result;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}