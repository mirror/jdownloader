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
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mixupload.org" }, urls = { "https?://(www\\.)?mixupload\\.(org|com)/((es|de)/)?track/[^<>\"/]+" })
public class MixUploadOrg extends PluginForHost {
    public MixUploadOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://mixupload.com/about/agreement";
    }

    public void correctDownloadLink(final DownloadLink link) {
        final String trackid = link.getDownloadURL().substring(link.getDownloadURL().lastIndexOf("/") + 1);
        link.setUrlDownload("http://mixupload.com/track/" + trackid);
    }

    @Override
    public String rewriteHost(String host) {
        if ("mixupload.org".equals(getHost())) {
            if (host == null || "mixupload.org".equals(host)) {
                return "mixupload.com";
            }
        }
        return super.rewriteHost(host);
    }

    private String DLLINK = null;

    /**
     * TODO: Add account support & show filesizes based on accounts e.g. show full size (available via '/player/getTrackInfo/') for premium
     * users and stream size for free users.
     */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String trackID = link.getStringProperty("trackid", null);
        if (trackID == null) {
            br.getPage(link.getDownloadURL());
            if (br.containsHTML(">Page not found<|>Error<|\"/img/404\\-img\\.png\"|\"/img/forbidden\\.png\"")) {
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
        br.getPage("http://mixupload.org/player/getTrackInfo/" + trackID);
        if ("[]".equals(br.toString().trim())) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = getJson("artist") + " - " + getJson("title");
        if (filename.contains("null")) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        Account premiumAcc = null;
        List<Account> accs = AccountController.getInstance().getValidAccounts("mixupload.org");
        if (accs != null && accs.size() > 0) {
            for (Account acc : accs) {
                if (acc.isEnabled() && !acc.getBooleanProperty("free", true)) {
                    premiumAcc = acc;
                }
            }
        }
        String filesize = null;
        if (premiumAcc != null) {
            /* Premium users can download the high quality track --> Filesize is given via 'API' */
            DLLINK = "http://mixupload.org/download/" + trackID;
            filesize = getJson("sizebyte");
            if (filesize == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            DLLINK = "http://mixupload.org/player/play/" + trackID + "/0/track.mp3";
            URLConnectionAdapter con = null;
            try {
                try {
                    con = br.openGetConnection(DLLINK);
                } catch (final BrowserException e) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (!con.getContentType().contains("html")) {
                    filesize = Long.toString(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp3");
        link.setDownloadSize(Long.parseLong(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
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
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}