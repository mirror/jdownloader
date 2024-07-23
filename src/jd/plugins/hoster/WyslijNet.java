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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class WyslijNet extends XFileSharingProBasic {
    public WyslijNet(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: null 4dignum reCaptchaV2, hcaptcha<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "wyslij.net" });
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
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return true;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return true;
        }
    }

    @Override
    public int getMaxChunks(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return 0;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return -10;
        } else {
            /* Free(anonymous) and unknown account type */
            return 0;
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    protected boolean supportsAPIMassLinkcheck() {
        return looksLikeValidAPIKey(this.getAPIKey());
    }

    @Override
    protected boolean supportsAPISingleLinkcheck() {
        return looksLikeValidAPIKey(this.getAPIKey());
    }

    @Override
    protected boolean allowAPIDownloadIfApikeyIsAvailable(final DownloadLink link, final Account account) {
        /**
         * 2024-04-23: API can be used for linkcheck but not for downloading. </br>
         * During account-check it looks as if the API supports downloads but it does not.
         */
        return false;
    }

    @Override
    protected AccountInfo fetchAccountInfoAPI(final Browser br, final Account account) throws Exception {
        try {
            return super.fetchAccountInfoAPI(br, account);
        } finally {
            /* Small hack */
            account.setProperty(PROPERTY_ACCOUNT_ALLOW_API_DOWNLOAD_ATTEMPT_IN_WEBSITE_MODE, false);
        }
    }

    @Override
    protected boolean containsRecaptchaV2Class(String string) {
        /* 2024-04-23 */
        if (new Regex(string, "<input[^>]*name=\"g-recaptcha-response\"").patternFind()) {
            return true;
        } else {
            return super.containsRecaptchaV2Class(string);
        }
    }

    @Override
    public String[] scanInfo(final String html, final String[] fileInfo) {
        super.scanInfo(html, fileInfo);
        final String betterFilesize = new Regex(html, ">\\s*Rozmiar pliku\\s*</span>\\s*</div>\\s*<div class=\"col-xl-8\">\\s*<p class=\"text-muted fs-14\">([^<]+)</p>").getMatch(0);
        if (betterFilesize != null) {
            fileInfo[1] = betterFilesize;
        }
        return fileInfo;
    }

    @Override
    protected String regExTrafficLeft(final Browser br) {
        final String ret = br.getRegex(">\\s*Pozostały transfer\\s*</h6>\\s*<h2[^>]*>([^<]+)</h2>").getMatch(0);
        if (ret != null) {
            return ret;
        } else {
            return super.regExTrafficLeft(br);
        }
    }
}