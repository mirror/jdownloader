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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class MixdropCo extends PluginForHost {
    public MixdropCo(PluginWrapper wrapper) {
        super(wrapper);
        /* 2019-09-30: They do not have/sell premium accounts */
        // this.enablePremium("");
    }

    @Override
    public String getAGBLink() {
        return "https://mixdrop.co/terms/";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "mixdrop.co" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/f/([a-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME           = true;
    private static final int     FREE_MAXCHUNKS        = 1;
    private static final int     FREE_MAXDOWNLOADS     = 20;
    /** Documentation: https://mixdrop.co/api */
    private static final String  API_BASE              = "https://api.mixdrop.co";
    private static final boolean USE_API_FOR_LINKCHECK = true;

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
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    private String getAPIMail() {
        return "psp@jdownloader.org";
    }

    private String getAPIKey() {
        return "u3aH2kgUYOQ36hd";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        final String fid = this.getFID(link);
        String filename = null;
        String filesize = null;
        if (USE_API_FOR_LINKCHECK) {
            /* 2019-09-30: Let's just use it that way and hope it keeps working. */
            /*
             * https://mixdrop.co/api#fileinfo --> Also supports multiple fileIDs but as we are unsure how long this will last and this is
             * only a small filehost, we're only using this to check single fileIDs.
             */
            br.getPage(API_BASE + "/fileinfo?email=" + getAPIMail() + "&key=" + getAPIKey() + "&ref[]=" + this.getFID(link));
            if (br.getHttpConnection().getResponseCode() == 404 || !"true".equalsIgnoreCase(PluginJSonUtils.getJson(br, "success"))) {
                /* E.g. {"success":false,"result":{"msg":"file not found"}} */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final boolean isDeleted = "true".equalsIgnoreCase(PluginJSonUtils.getJson(br, "deleted"));
            if (isDeleted) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = PluginJSonUtils.getJson(br, "title");
            filesize = PluginJSonUtils.getJson(br, "size");
        } else {
            br.getPage(link.getPluginPatternMatcher());
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("/imgs/illustration-notfound\\.png")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Regex fileinfo = br.getRegex("imgs/icon-file\\.png\"[^>]*/> <span title=\"([^\"]+)\">[^<>]*</span>([^<>\"]+)</div>");
            filename = fileinfo.getMatch(0);
            filesize = fileinfo.getMatch(1);
        }
        if (StringUtils.isEmpty(filename)) {
            /* Fallback */
            filename = fid;
        }
        filename = Encoding.htmlDecode(filename).trim();
        link.setName(filename);
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            if (USE_API_FOR_LINKCHECK) {
                br.getPage(link.getPluginPatternMatcher());
            }
            final String csrftoken = br.getRegex("name=\"csrf\" content=\"([^<>\"]+)\"").getMatch(0);
            if (StringUtils.isEmpty(csrftoken)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getHeaders().put("x-requested-with", "XMLHttpRequest");
            br.postPage(br.getURL(), "a=genticket&csrf=" + csrftoken);
            dllink = PluginJSonUtils.getJson(br, "url");
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* 2019-09-30: Skip short pre-download waittime */
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
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
                if (con.getContentType().contains("text") || !con.isOK() || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                logger.log(e);
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return dllink;
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (acc.getType() == AccountType.FREE) {
            /* Free accounts can have captchas */
            return true;
        }
        /* Premium accounts do not have captchas */
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