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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Browser.BlockedByException;
import jd.http.Cookies;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountUnavailableException;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PreFilesCom extends XFileSharingProBasic {
    public PreFilesCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2019-07-03: no limits at all<br />
     * captchatype-info: 2019-07-03: reCaptchaV2<br />
     * other:<br />
     */
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

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "prefiles.com" });
        return ret;
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

    @Override
    protected String regExTrafficLeft(final Browser br) {
        /* 2019-07-03: Special */
        String trafficleft = super.regExTrafficLeft(br);
        if (StringUtils.isEmpty(trafficleft)) {
            final String src = this.getCorrectBR(br);
            /* 2019-07-03: Free Accounts: According to this place, 5 GB (per day?) but another place states 2 GB/day */
            trafficleft = new Regex(src, "Traffic Remaining</td>\\s*?<td>\\s*([^<>\"]+)\\s*</td>").getMatch(0);
            if (StringUtils.isEmpty(trafficleft)) {
                /* 2020-11-13 */
                trafficleft = new Regex(src, "Traffic remaining</label></td>\\s*<td>([^<>\"]+)<").getMatch(0);
            }
        }
        return trafficleft;
    }

    @Override
    protected void checkErrors(final Browser br, final String correctedBR, final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        /* 2019-07-03: Special */
        super.checkErrors(br, correctedBR, link, account, checkAll);
        final String wait = new Regex(correctedBR, ">\\s*Your subsequent download will be started in([^<>]+)").getMatch(0);
        if (wait != null) {
            /* adjust this regex to catch the wait time string for COOKIE_HOST */
            String tmphrs = new Regex(wait, "\\s+(\\d+)\\s+hours?").getMatch(0);
            if (tmphrs == null) {
                tmphrs = new Regex(correctedBR, "You have to wait.*?\\s+(\\d+)\\s+hours?").getMatch(0);
            }
            String tmpmin = new Regex(wait, "\\s+(\\d+)\\s+minutes?").getMatch(0);
            if (tmpmin == null) {
                tmpmin = new Regex(correctedBR, "You have to wait.*?\\s+(\\d+)\\s+minutes?").getMatch(0);
            }
            String tmpsec = new Regex(wait, "\\s+(\\d+)\\s+seconds?").getMatch(0);
            String tmpdays = new Regex(wait, "\\s+(\\d+)\\s+days?").getMatch(0);
            if (tmphrs == null && tmpmin == null && tmpsec == null && tmpdays == null) {
                logger.info("Waittime regexes seem to be broken");
                if (account != null) {
                    throw new AccountUnavailableException("Download limit reached", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 60 * 60 * 1000l);
                }
            } else {
                int minutes = 0, seconds = 0, hours = 0, days = 0;
                if (tmphrs != null) {
                    hours = Integer.parseInt(tmphrs);
                }
                if (tmpmin != null) {
                    minutes = Integer.parseInt(tmpmin);
                }
                if (tmpsec != null) {
                    seconds = Integer.parseInt(tmpsec);
                }
                if (tmpdays != null) {
                    days = Integer.parseInt(tmpdays);
                }
                int waittime = ((days * 24 * 3600) + (3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
                logger.info("Detected waittime #2, waiting " + waittime + "milliseconds");
                /* Not enough wait time to reconnect -> Wait short and retry */
                if (waittime < 180000) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait until new downloads can be started", waittime);
                }
                if (account != null) {
                    throw new AccountUnavailableException("Download limit reached", waittime);
                } else {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
                }
            }
        }
    }

    @Override
    public boolean isPremiumOnly(final Browser br) {
        if (br == null) {
            return false;
        }
        if (br.containsHTML(">\\s*The file owner does not allow FREE")) {
            return true;
        } else {
            return super.isPremiumOnly(br);
        }
    }

    @Override
    public boolean isLoggedin(Browser br) {
        boolean loggedin = super.isLoggedin(br);
        if (!loggedin) {
            /* 2020-11-13 */
            final String mainpage = getMainPage();
            loggedin = StringUtils.isAllNotEmpty(br.getCookie(mainpage, "login", Cookies.NOTDELETEDPATTERN), br.getCookie(mainpage, "xfss", Cookies.NOTDELETEDPATTERN));
        }
        return loggedin;
    }

    @Override
    protected String getRelativeAccountInfoURL() {
        /* 2020-11-13 */
        return "/my-account";
    }

    @Override
    protected String getDllink(final DownloadLink link, final Account account, final Browser br, String src) {
        String dllink = super.getDllink(link, account, br, src);
        if (dllink != null) {
            return dllink;
        } else {
            /* 2020-12-11: They're using simple redirectors here e.g. "pro.sh" */
            dllink = new Regex(src, "href=\"(https?://[^\"]+)\"[^>]*>\\s*Click here to Download").getMatch(0);
            if (dllink == null) {
                return null;
            }
            if (dllink.matches("https?://[^/]+/[A-Za-z0-9]+")) {
                /* pro.sh/ouo.io --> Use dedicated crawler plugin. */
                logger.info("Processing special redirect URL");
                try {
                    final Browser brc = br.cloneBrowser();
                    final PluginForDecrypt plg = this.getNewPluginForDecryptInstance("pro.sh");
                    plg.setBrowser(brc);
                    /* We expect exactly one result. */
                    final ArrayList<DownloadLink> results = plg.decryptIt(new CryptedLink(dllink), null);
                    dllink = results.get(0).getPluginPatternMatcher();
                    return dllink;
                } catch (final Throwable e) {
                    if (e instanceof BlockedByException) {
                        /* 2024-06-20: Dirty hack to make this fail later in order to let upper handling run into BlockedByException. */
                        return dllink;
                    } else {
                        e.printStackTrace();
                        return null;
                    }
                }
            } else {
                return dllink;
            }
        }
    }
}