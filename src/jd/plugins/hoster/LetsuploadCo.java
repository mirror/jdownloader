//jDownloader - Downloadmanager
//Copyright (C) 2016  JD-Team support@jdownloader.org
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

import org.jdownloader.plugins.components.YetiShareCore;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class LetsuploadCo extends YetiShareCore {
    public LetsuploadCo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(getPurchasePremiumURL());
    }

    /**
     * DEV NOTES YetiShare<br />
     ****************************
     * mods: See overridden functions<br />
     * limit-info: 2019-04-25: no limits at all<br />
     * captchatype-info: 2019-04-25: null<br />
     * other: <br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "letsupload.io", "letsupload.org", "letsupload.to", "letsupload.co" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String rewriteHost(String host) {
        /* 2020-11-06: Main domain changed from letsupload.org to letsupload.io */
        return this.rewriteHost(getPluginDomains(), host, new String[0]);
    }

    @Override
    public boolean requires_WWW() {
        // 07.11.2019, now redirects www. to normal domain. www domain is redirected to cloudhost.to
        return false;
    }

    @Override
    protected String getFUID(final DownloadLink link) {
        final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), "^https?://[^/]+/plugins/mediaplayer/site/_embed\\.php\\?u=([A-Za-z0-9]+)");
        final String fid = urlinfo.getMatch(0);
        if (fid != null) {
            return fid;
        } else {
            return super.getFUID(link);
        }
    }

    public static String getDefaultAnnotationPatternPartLetsupload() {
        return "/(plugins/mediaplayer/site/_embed\\.php\\?u=[A-Za-z0-9]+.*|(?!folder)[A-Za-z0-9]+(?:/[^/<>]+)?)";
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + getDefaultAnnotationPatternPartLetsupload());
        }
        return ret.toArray(new String[0]);
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
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
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public int getMaxSimultaneousFreeAccountDownloads() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    /** 2019-04-25: Special */
    @Override
    public String[] scanInfo(DownloadLink link, final String[] fileInfo) {
        if (supports_availablecheck_over_info_page(link)) {
            fileInfo[0] = br.getRegex("<span>Filename[^<]*?<p>([^<>\"]+)</p>").getMatch(0);
            fileInfo[1] = br.getRegex("<span>Filesize[^<]*?<p>([^<>\"]+)</p>").getMatch(0);
        } else {
            fileInfo[0] = br.getRegex("class=\"fa fa-file\\-text\"></i>([^<>\"]+)</div>").getMatch(0);
            fileInfo[1] = br.getRegex("size[^<]*?<p>([^<>\"]+)</p>").getMatch(0);
        }
        return super.scanInfo(link, fileInfo);
    }
}