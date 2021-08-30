//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.plugins.components.XFileSharingProBasic;
import org.jdownloader.plugins.components.config.XFSConfigVideoVuploadCom;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class VuploadCom extends XFileSharingProBasic {
    public VuploadCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2019-10-01: Set to default as this host uses HLS streaming only <br />
     * captchatype-info: null<br />
     * other: Tags: ddl.to sister-site - is a videohoster <br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "vupload.com", "vup.to" });
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
        /* 2019-10-01: Special: They have customized embed URLs. */
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/((?:embed\\-|emb\\.html\\?)?[a-z0-9]{12}(?:/[^/]+(?:\\.html)?)?|(?:e|v)/([a-z0-9]{12})(/([^/]+))?)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String rewriteHost(final String host) {
        /* 2021-04-26: Main domain has changed from vup.to to upload.com */
        return this.rewriteHost(getPluginDomains(), host);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return true;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return true;
        }
    }

    @Override
    public int getMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return 0;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 0;
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return -1;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }
    /* 2021-04-26: Not (yet) required */
    // @Override
    // protected String buildEmbedURLPath(final String fuid) {
    // return "/e/" + fuid;
    // }
    //
    // @Override
    // protected String buildNormalURLPath(final String fuid) {
    // return "/v/" + fuid;
    // }

    @Override
    protected boolean isVideohosterEmbedHTML(final Browser br) {
        /* 2019-10-01: Special: They have customized embed URLs. */
        if (super.isVideohosterEmbedHTML(br)) {
            return true;
        } else {
            return br.containsHTML("/emb\\.html\\?|/e/");
        }
    }

    @Override
    public String getFUIDFromURL(final DownloadLink dl) {
        final String result = new Regex(dl.getPluginPatternMatcher(), "https?://[^/]+/(?:embed\\-|emb\\.html\\?|v/|e/)?([a-z0-9]{12})").getMatch(0);
        return result;
    }

    @Override
    protected boolean isVideohoster_enforce_video_filename() {
        /* 2019-10-01: Special */
        return true;
    }

    @Override
    protected boolean supportsAPIMassLinkcheck() {
        return isAPIKey(this.getAPIKey());
    }

    @Override
    protected boolean supportsAPISingleLinkcheck() {
        return isAPIKey(this.getAPIKey());
    }

    @Override
    public Class<? extends XFSConfigVideoVuploadCom> getConfigInterface() {
        return XFSConfigVideoVuploadCom.class;
    }

    @Override
    protected String findAPIKey(final Browser brc) throws Exception {
        String apikey = super.findAPIKey(brc);
        if (apikey != null) {
            return apikey;
        } else {
            /* 2021-04-26: Special */
            this.getPage(brc, "/api");
            apikey = brc.getRegex("\\?key=([a-z0-9]+)").getMatch(0);
            if (apikey == null) {
                return null;
            } else {
                findAPIHost(brc, apikey);
                return apikey;
            }
        }
    }

    @Override
    protected String getDllinkVideohost(final String src) {
        /* 2021-08-30: Special as upper handling may fail to pick the highest quality in this case. */
        String dllink = null;
        final String[] specialPreferredQuals = new String[] { "1080", "720" };
        for (final String quality : specialPreferredQuals) {
            dllink = new Regex(src, quality + "p  <span class='vup-hd-quality-badge'></span>\",sources:\\[\\{src:\"(https?://[^\"]+)").getMatch(0);
            if (dllink != null) {
                break;
            }
        }
        if (dllink != null) {
            return dllink;
        } else {
            /* Fallback to upper handling */
            return super.getDllinkVideohost(src);
        }
    }

    @Override
    public String[] scanInfo(final String[] fileInfo) {
        /* 2021-08-30: Special */
        super.scanInfo(fileInfo);
        final String betterFilename = br.getRegex("<h1 class=\"video-title\"[^>]*>(.*?)</h1>").getMatch(0);
        if (betterFilename != null) {
            fileInfo[0] = betterFilename;
        }
        return fileInfo;
    }
}