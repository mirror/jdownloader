//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.util.LinkedHashMap;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "nopy.to" }, urls = { "https?://(?:www\\.)?nopy\\.to/([A-Za-z0-9]+)/([^<>/\"]+)" })
public class NopyTo extends PluginForHost {
    public NopyTo(PluginWrapper wrapper) {
        super(wrapper);
        /*
         * 2019-05-09: They do not have any premium accounts - this URL is for them in case anyone wants to support their free service
         * (requested by admin - this is an Exception, we usually don't do this!).
         */
        this.enablePremium("https://patreon.com/nopy");
    }

    @Override
    public String getAGBLink() {
        return "https://nopy.to/";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        if (linkid != null) {
            return linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    /* Connection stuff */
    private static final boolean          FREE_RESUME       = true;
    private static final int              FREE_MAXCHUNKS    = 1;
    /* 2019-05-09: Admin: 'Max. 8 connections per downloadserver' --> Let's keep it safe */
    private static final int              FREE_MAXDOWNLOADS = 8;
    private LinkedHashMap<String, Object> entries           = null;

    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private static final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    //
    // /* don't touch the following! */
    // private static AtomicInteger maxPrem = new AtomicInteger(1);
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", "JDownloader");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        final String linkid = this.getLinkID(link);
        final String filename_url = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
        br.postPage("https://data." + this.getHost() + "/file", "code=" + linkid + "&file=" + Encoding.urlEncode(filename_url));
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final Object statusO = entries.get("status");
        if (statusO != null && statusO instanceof LinkedHashMap) {
            entries = (LinkedHashMap<String, Object>) statusO;
            final String status = (String) entries.get("status");
            if ("error".equalsIgnoreCase(status)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        entries = (LinkedHashMap<String, Object>) entries.get("msg");
        final String sha1hash = (String) entries.get("hash");
        String filename = (String) entries.get("filename");
        if (StringUtils.isEmpty(filename)) {
            /* Fallback */
            filename = filename_url;
        }
        long filesize = JavaScriptEngineFactory.toLong(entries.get("raw_size"), 0);
        link.setFinalFileName(filename);
        if (filesize > 0) {
            link.setDownloadSize(filesize);
        }
        if (sha1hash != null) {
            link.setSha1Hash(sha1hash);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            final boolean not_downloadable_at_this_moment = ((Boolean) entries.get("error_fatal")).booleanValue();
            if (not_downloadable_at_this_moment) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            }
            final String fid = (String) entries.get("fid");
            final String request = (String) entries.get("request");
            final String session = (String) entries.get("session");
            final String linkid = this.getLinkID(downloadLink);
            if (!StringUtils.isAllNotEmpty(fid, request, session)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.postPage("/download", "code=" + linkid + "&fid=" + Encoding.urlEncode(fid) + "&request=" + Encoding.urlEncode(request) + "&session=" + Encoding.urlEncode(session));
            dllink = PluginJSonUtils.getJson(br, "download");
            if (StringUtils.isEmpty(dllink) || !dllink.startsWith("http")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        account.setType(AccountType.FREE);
        ai.setStatus("Dummy account");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* 2019-05-09: They do not have any premium accounts - this is just so that dummy accounts work (see URL in "enablePremium()") */
        this.handleFree(link);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 8;
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        /* No captchas at all. */
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}