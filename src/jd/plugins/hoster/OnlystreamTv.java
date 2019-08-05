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
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class OnlystreamTv extends XFileSharingProBasic {
    public OnlystreamTv(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2019-08-06: null<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        /* 2019-08-06: Current list of domains can be found here: https://onlystream.tv/?op=safe_domains */
        ret.add(new String[] { "onlystream.tv", "ostream.pro", "ostream.me" });
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
        return XFileSharingProBasic.buildAnnotationUrls(getPluginDomains());
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
            return -2;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return -2;
        } else {
            /* Free(anonymous) and unknown account type */
            return -2;
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return 5;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return 5;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 5;
    }

    protected boolean isVideohoster_enforce_video_filename() {
        /* 2019-08-06: Special */
        return true;
    }

    public String[] scanInfo(final String[] fileInfo) {
        /* 2019-08-06: Special */
        fileInfo[0] = new Regex(correctedBR, "<title>([^<>\"]+) - Onlystream\\.tv</title>").getMatch(0);
        fileInfo[1] = new Regex(correctedBR, "Download video \\(([^<>\"]+)\\)").getMatch(0);
        if (StringUtils.isEmpty(fileInfo[0]) || StringUtils.isEmpty(fileInfo[1])) {
            super.scanInfo(fileInfo);
        }
        return fileInfo;
    }

    @Override
    protected boolean isOffline(final DownloadLink link) {
        boolean offline = super.isOffline(link);
        if (!offline) {
            offline = correctedBR.contains(">File you are looking for is not found");
        }
        return offline;
    }

    @Override
    protected String findAPIKey(String src) throws Exception {
        /* 2019-08-06: Special */
        final Browser br2 = br.cloneBrowser();
        br2.setFollowRedirects(true);
        getPage(br2, "/?op=my_api");
        src = br2.toString();
        final Pattern apikeyPattern = Pattern.compile("API Key\\s*?</td>\\s*?<td>\\s*?<input type=\"text\" value=\"([a-z0-9]+)\"");
        String apikey = new Regex(src, apikeyPattern).getMatch(0);
        String generate_apikey_url = new Regex(src, "\"([^\"]*?op=my_account[^\"]*?generate_api_key=1[^\"]*?token=[a-f0-9]{32}[^\"]*?)\"").getMatch(0);
        if (apikey == null && generate_apikey_url != null) {
            if (Encoding.isHtmlEntityCoded(generate_apikey_url)) {
                generate_apikey_url = Encoding.htmlDecode(generate_apikey_url);
            }
            logger.info("Failed to find apikey but host has api-mod enabled --> Trying to generate first apikey for this account");
            try {
                br2.setFollowRedirects(true);
                getPage(br2, generate_apikey_url);
                apikey = br2.getRegex(apikeyPattern).getMatch(0);
            } catch (final Throwable e) {
                e.printStackTrace();
            }
            if (apikey == null) {
                logger.info("Failed to find generated apikey - possible plugin failure");
            } else {
                logger.info("Successfully found newly generated apikey");
            }
        }
        return apikey;
    }
}