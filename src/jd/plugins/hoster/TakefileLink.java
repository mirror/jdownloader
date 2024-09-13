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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class TakefileLink extends XFileSharingProBasic {
    public TakefileLink(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    public String getPurchasePremiumURL() {
        return this.getMainPage() + "/upgrade";
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2021-09-28 <br />
     * captchatype-info: 2019-02-11: null<br />
     * other:<br />
     */
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
            /* 2020-10-02 */
            return -2;
        } else {
            /* Free(anonymous) and unknown account type */
            return -2;
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        /*
         * 2020-02-19: According to forum users, they will allow one more connection every 24 hours. It seems like they often host single
         * files > 10GB which means this scenario is realistic.
         */
        return -1;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        /*
         * 2020-02-19: According to forum users, they will allow one more connection every 24 hours. It seems like they often host single
         * files > 10GB which means this scenario is realistic.
         */
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* 2020-10-02 */
        return -1;
    }

    @Override
    protected boolean isOffline(final DownloadLink link, final Browser br) {
        final String fuid = super.getFUIDFromURL(link);
        if (fuid != null && !br.getURL().contains(fuid) || (br.getRedirectLocation() != null && !br.getRedirectLocation().contains(fuid))) {
            /* 2018-11-15: Special - redirect to: https://takefile.link/upgrade */
            return true;
        } else {
            return super.isOffline(link, br);
        }
    }

    @Override
    public String getLoginURL() {
        return getMainPage() + "/user_login";
    }

    @Override
    public boolean isLoggedin(Browser br) {
        boolean isLoggedinHTML = super.isLoggedin(br);
        if (!isLoggedinHTML) {
            isLoggedinHTML = br.containsHTML("/user_logout");
        }
        return isLoggedinHTML;
    }

    @Override
    public String regExTrafficLeft(final Browser br) {
        String trafficleft = super.regExTrafficLeft(br);
        if (StringUtils.isEmpty(trafficleft)) {
            final String src = this.getCorrectBR(br);
            trafficleft = new Regex(src, "Traffic available today</TD></TR>\\s*?</thead>\\s*?<TR><TD><b>\\s*([^<>\"]+)\\s*</b><").getMatch(0);
        }
        return trafficleft;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "takefile.link", "filecheck.link" });
        ret.addAll(getVirtualPluginDomains());
        return ret;
    }

    public static List<String[]> getVirtualPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // virtual file hosting
        final Set<String> dupes;
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            dupes = new HashSet<String>();
        } else {
            dupes = null;
        }
        for (final String virtualPluginDomain : new String[] { "vip", "erofullsets", "musiclibrary", "mega-rip", "webcam", "pwc", "scat", "rare", "files", "3dmodel", "4kuhd", "copro", "eros", "fetish", "goldenrain", "hentai", "hmu", "momroleplay", "monster", "pissing", "spy", "test", "voyeur", "gaybb", "hdmusic", "wandering-voyeur", "mh", "siteriplinks", "88nsm", "allvoyeur", "asian", "avcens", "babes", "bestero", "camvip", "eliteporn", "erotelki", "extremesiterips", "goldhiphop", "kaprettiscat", "npkps", "payperview", "pornogayphy", "premiumbbwcontent", "seduction4life", "shitting", "siterip", "siteripz", "spanking", "spyerotic", "submales", "supervoyeur", "voyeurauthor", "voyeurzona", "watches" }) {
            if (virtualPluginDomain.contains(".")) {
                ret.add(new String[] { virtualPluginDomain });
            } else {
                ret.add(new String[] { virtualPluginDomain + ".takefile.link" });
            }
            if (dupes != null && !dupes.add(ret.get(ret.size() - 1)[0])) {
                // domain duplicate detected
                DebugMode.debugger();
            }
        }
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    /**
     * returns the annotation pattern array: 'https?://(?:www\\.)?(?:domain1|domain2)/(?:embed\\-)?[a-z0-9]{12}'
     *
     */
    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + XFileSharingProBasic.getDefaultAnnotationPatternPart());
        }
        return ret.toArray(new String[0]);
    }

    @Override
    protected boolean supportsShortURLs() {
        return true;
    }

    @Override
    protected void checkErrors(final Browser br, final String correctedBR, final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        super.checkErrors(br, correctedBR, link, account, checkAll);
        /* 2021-01-11: Some files are "paid files": The user has to pay a fee for that single file to be able to download it. */
        if (new Regex(correctedBR, "class=\"price\"").patternFind()) {
            throw new AccountRequiredException();
        }
    }

    @Override
    protected AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        final AccountInfo aiNormal;
        final String takefileVipProperty = "takefileVip";
        try {
            aiNormal = super.fetchAccountInfoWebsite(account);
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.removeProperty(takefileVipProperty);
            }
            if (br.containsHTML(">\\s*Your IP is banned")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Your IP is banned", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw e;
            }
        }
        if (account.getType() == AccountType.PREMIUM && !account.hasProperty(takefileVipProperty)) {
            return aiNormal;
        } else if ("takefile.link".equals(getHost())) {
            // workaround for old takefile -> vip accounts
            /*
             * 2021-01-13: Special: They got accounts which are only premium when accessing their website via specified subdomain
             * vip.takefile.link so always check both!
             */
            final AccountInfo aiVip = new AccountInfo();
            logger.info("Double-checking for premium status");
            getPage("https://vip." + this.getHost() + this.getRelativeAccountInfoURL());
            final String validUntilStr = new Regex(this.getCorrectBR(br), "class=\"acc_data\">(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})<").getMatch(0);
            final String trafficLeftStr = regExTrafficLeft(br);
            if (trafficLeftStr != null) {
                /* Need to set 0 traffic left, as getSize returns positive result, even when negative value supplied. */
                long trafficLeft = 0;
                if (trafficLeftStr.startsWith("-")) {
                    /* Negative traffic value = User downloaded more than he is allowed to (rare case) --> No traffic left */
                    trafficLeft = 0;
                } else {
                    trafficLeft = SizeFormatter.getSize(trafficLeftStr);
                }
                aiVip.setTrafficLeft(trafficLeft);
            }
            if (validUntilStr != null) {
                logger.info("Account is special VIP premium");
                account.setProperty(takefileVipProperty, Boolean.TRUE);
                aiVip.setValidUntil(TimeFormatter.getMilliSeconds(validUntilStr, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH), this.br);
                this.setAccountLimitsByType(account, AccountType.PREMIUM);
                /* Return result of 2nd check */
                return aiVip;
            } else {
                account.removeProperty(takefileVipProperty);
                logger.info("Account is free account");
                /* Return result of first check */
                return aiNormal;
            }
        } else {
            return aiNormal;
        }
    }
}