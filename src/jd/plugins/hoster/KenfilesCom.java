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

import java.util.Locale;
import java.util.regex.Pattern;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class KenfilesCom extends XFileSharingProBasic {
    public KenfilesCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:2019-03-01: premium untested, set FREE account limits <br />
     * captchatype-info: 2019-03-01: reCaptchaV2<br />
     * other:<br />
     */
    private static String[] domains = new String[] { "kenfiles.com", "kfs.space" };

    protected boolean enableAccountApiOnlyMode() {
        return false;
    }

    @Override
    protected AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        AccountInfo ai = null;
        loginWebsite(null, account, true);
        /*
         * Only access URL if we haven't accessed it before already. Some sites will redirect to their Account-Info page right after
         * logging-in or our login-function when it is verifying cookies and not performing a full login.
         */
        if (br.getURL() == null || !br.getURL().contains(getRelativeAccountInfoURL())) {
            getPage(this.getMainPage() + getRelativeAccountInfoURL());
        }
        {
            logger.info("AccountInfo via API not possible -> Obtaining all AccountInfo from website");
            account.removeProperty(PROPERTY_ACCOUNT_apikey);
            ai = new AccountInfo();
        }
        String trafficLeftStr = regExTrafficLeft(br);
        final boolean userHasUnlimitedTraffic = trafficLeftStr != null && trafficLeftStr.matches(".*?(nlimited|Ilimitado).*?");
        if (trafficLeftStr != null && !userHasUnlimitedTraffic && !trafficLeftStr.equalsIgnoreCase("Mb")) {
            trafficLeftStr = Encoding.htmlDecode(trafficLeftStr).trim();
            /* Need to set 0 traffic left, as getSize returns positive result, even when negative value supplied. */
            long trafficLeft = 0;
            if (trafficLeftStr.startsWith("-")) {
                /* Negative traffic value = User downloaded more than he is allowed to (rare case) --> No traffic left */
                trafficLeft = 0;
            } else {
                trafficLeft = SizeFormatter.getSize(trafficLeftStr);
            }
            /* 2019-02-19: Users can buy additional traffic packages: Example(s): subyshare.com */
            final String usableBandwidth = br.getRegex("Traffic available today</span>\\s*<span[^>]*><a[^>]*>([0-9\\.]+\\s*[TGMKB|tgmb]+)<").getMatch(0);
            if (usableBandwidth != null) {
                trafficLeft = Math.max(trafficLeft, SizeFormatter.getSize(usableBandwidth));
            }
            ai.setTrafficLeft(trafficLeft);
        } else {
            ai.setUnlimitedTraffic();
        }
        final String space[] = new Regex(getCorrectBR(br), ">Used space:</span>\\s*<span>([0-9\\.]+)\\s*([KB|Kb|MB|Mb|GB|Gb|TB|Tb]+)?").getRow(0);
        if ((space != null && space.length != 0) && (space[0] != null && space[1] != null)) {
            /* free users it's provided by default */
            ai.setUsedSpace(space[0] + " " + space[1]);
        } else if ((space != null && space.length != 0) && space[0] != null) {
            /* premium users the Mb value isn't provided for some reason... */
            ai.setUsedSpace(space[0] + "Mb");
        }
        String expireStr = new Regex(getCorrectBR(br), ">Premium\\s*[account expire|until]+:</span>\\s*[^>]*>([\\d]+-[\\w{2}]+-[\\d]+\\s[\\d:]+)</").getMatch(0);
        long expire_milliseconds = 0;
        long expire_milliseconds_from_expiredate = 0;
        if (expireStr != null) {
            expire_milliseconds_from_expiredate = TimeFormatter.getMilliSeconds(expireStr, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        }
        final long currentTime = br.getCurrentServerTime(System.currentTimeMillis());
        if (expire_milliseconds_from_expiredate > 0) {
            logger.info("Using expire-date which is up to 24 hours precise");
            expire_milliseconds = expire_milliseconds_from_expiredate;
        } else {
            logger.info("Failed to find any useful expire-date at all");
        }
        if ((expire_milliseconds - currentTime) <= 0) {
            /* If the premium account is expired or we cannot find an expire-date we'll simply accept it as a free account. */
            if (expire_milliseconds > 0) {
                logger.info("Premium expired --> Free account");
            } else {
                logger.info("Account is a FREE account as no expiredate has been found");
            }
            setAccountLimitsByType(account, AccountType.FREE);
        } else {
            /* Expire date is in the future --> It is a premium account */
            logger.info("Premium account to " + expireStr);
            ai.setValidUntil(expire_milliseconds);
            setAccountLimitsByType(account, AccountType.PREMIUM);
        }
        return ai;
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return false;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return false;
        }
    }

    @Override
    public int getMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return 1;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return -2;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
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
    public boolean websiteSupportsHTTPS() {
        return true;
    }

    public static String[] getAnnotationNames() {
        return new String[] { domains[0] };
    }

    @Override
    public String[] siteSupportedNames() {
        return domains;
    }

    /**
     * returns the annotation pattern array: 'https?://(?:www\\.)?(?:domain1|domain2)/(?:embed\\-)?[a-z0-9]{12}'
     *
     */
    public static String[] getAnnotationUrls() {
        // construct pattern
        final String host = getHostsPattern();
        return new String[] { host + "/(?:embed\\-)?[a-z0-9]{12}(?:/[^/]+\\.html)?" };
    }

    /** returns 'https?://(?:www\\.)?(?:domain1|domain2)' */
    private static String getHostsPattern() {
        final String hosts = "https?://(?:www\\.)?" + "(?:" + getHostsPatternPart() + ")";
        return hosts;
    }

    /** Returns '(?:domain1|domain2)' */
    public static String getHostsPatternPart() {
        final StringBuilder pattern = new StringBuilder();
        for (final String name : domains) {
            pattern.append((pattern.length() > 0 ? "|" : "") + Pattern.quote(name));
        }
        return pattern.toString();
    }

    /** Function to find the final downloadlink. */
    @Override
    protected String getDllink(final DownloadLink link, final Account account, final Browser br, final String src) {
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            dllink = new Regex(src, "(\"|\\')(https?://(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|([\\w\\-]+\\.)?kfs\\.space)(:\\d{1,4})?/(files|d|p|f|cgi\\-bin/dl\\.cgi)/(\\d+/)?[a-z0-9]+/[^<>\"/]*?)(\"|\\')").getMatch(1);
            if (dllink == null) {
                final String cryptedScripts[] = new Regex(src, "p\\}\\((.*?)\\.split\\('\\|'\\)").getColumn(0);
                if (cryptedScripts != null && cryptedScripts.length != 0) {
                    for (String crypted : cryptedScripts) {
                        dllink = decodeDownloadLink(link, account, br, crypted);
                        if (dllink != null) {
                            break;
                        }
                    }
                }
            }
        }
        return dllink;
    }

    @Override
    public String decodeDownloadLink(final DownloadLink link, final Account account, final Browser br, final String s) {
        String decoded = null;
        try {
            Regex params = new Regex(s, "'(.*?[^\\\\])',(\\d+),(\\d+),'(.*?)'");
            String p = params.getMatch(0).replaceAll("\\\\", "");
            int a = Integer.parseInt(params.getMatch(1));
            int c = Integer.parseInt(params.getMatch(2));
            String[] k = params.getMatch(3).split("\\|");
            while (c != 0) {
                c--;
                if (k[c].length() != 0) {
                    p = p.replaceAll("\\b" + Integer.toString(c, a) + "\\b", k[c]);
                }
            }
            decoded = p;
        } catch (Exception e) {
            logger.log(e);
        }
        String finallink = null;
        if (decoded != null) {
            /* Open regex is possible because in the unpacked JS there are usually only 1-2 URLs. */
            finallink = new Regex(decoded, "(?:\"|')(https?://[^<>\"']*?\\.(avi|flv|mkv|mp4))(?:\"|')").getMatch(0);
            if (finallink == null) {
                /* Maybe rtmp */
                finallink = new Regex(decoded, "(?:\"|')(rtmp://[^<>\"']*?mp4:[^<>\"']+)(?:\"|')").getMatch(0);
            }
        }
        return finallink;
    }

    @Override
    protected Form findFormDownload2Free(final Browser br) {
        /* 2020-09-02: Special */
        Form dlForm = super.findFormDownload2Free(br);
        if (dlForm != null) {
            dlForm.remove("method_premium");
        }
        return dlForm;
    }

    @Override
    protected String regexWaittime() {
        String waitStr = super.regexWaittime();
        if (waitStr == null) {
            /* 2020-09-02: Special */
            waitStr = new Regex(correctedBR, ">(\\d+)</span> seconds<").getMatch(0);
        }
        return waitStr;
    }
}