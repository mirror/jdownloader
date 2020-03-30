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
import jd.http.Cookies;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class UpToBoxCom extends XFileSharingProBasic {
    public UpToBoxCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2020-03-27: null<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "uptobox.com", "uptostream.com" });
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
            return 1;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return 0;
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
        return 10;
    }

    @Override
    public String[] scanInfo(final String[] fileInfo) {
        fileInfo[0] = new Regex(correctedBR, "class='file-title'>([^<>\"]+) \\(\\d+[^\\)]+\\)\\s*<").getMatch(0);
        /* 2020-03-27: Special */
        return super.scanInfo(fileInfo);
    }

    @Override
    protected String getDllink(final DownloadLink link, final Account account, final Browser br, String src) {
        /* 2020-03-27: Special */
        final String dllink = new Regex(correctedBR, "\"(https?://[^\"]+/dl/[^\"]+)\"").getMatch(0);
        if (dllink != null) {
            return dllink;
        }
        return super.getDllink(link, account, br, src);
    }

    @Override
    protected void checkErrors(final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        /* 2020-03-27: Special */
        final String preciseWaittime = new Regex(correctedBR, "or you can wait ([^<>\"]+)<").getMatch(0);
        if (preciseWaittime != null) {
            /* Reconnect waittime with given (exact) waittime usually either up to the minute or up to the second. */
            final String tmphrs = new Regex(preciseWaittime, "\\s*(\\d+)\\s*hours?").getMatch(0);
            final String tmpmin = new Regex(preciseWaittime, "\\s*(\\d+)\\s*minutes?").getMatch(0);
            final String tmpsec = new Regex(preciseWaittime, "\\s*(\\d+)\\s*seconds?").getMatch(0);
            final String tmpdays = new Regex(preciseWaittime, "\\s*(\\d+)\\s*days?").getMatch(0);
            int waittime;
            if (tmphrs == null && tmpmin == null && tmpsec == null && tmpdays == null) {
                /* This should not happen! This is an indicator of developer-failure! */
                logger.info("Waittime RegExes seem to be broken - using default waittime");
                waittime = 60 * 60 * 1000;
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
                waittime = ((days * 24 * 3600) + (3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
            }
            logger.info("Detected reconnect waittime (milliseconds): " + waittime);
            /* Not enough wait time to reconnect -> Wait short and retry */
            if (waittime < 180000) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait until new downloads can be started", waittime);
            } else if (account != null) {
                throw new AccountUnavailableException("Download limit reached", waittime);
            } else {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
            }
        }
    }

    @Override
    public Form findLoginform(final Browser br) {
        Form loginform = br.getFormbyProperty("id", "login-form");
        if (loginform != null) {
            return loginform;
        }
        return super.findLoginform(br);
    }

    @Override
    public String getLoginURL() {
        /* 2020-03-27: Special */
        return getMainPage() + "/login";
    }

    @Override
    public boolean isLoggedin() {
        final boolean login_xfss_CookieOkay = br.getCookie(getMainPage(), "xfss", Cookies.NOTDELETEDPATTERN) != null;
        return login_xfss_CookieOkay;
    }

    @Override
    protected String regexWaittime() {
        /* 2020-03-27: Special */
        String waitStr = super.regexWaittime();
        if (waitStr == null) {
            waitStr = new Regex(correctedBR, "time-remaining' data-remaining-time='(\\d+)'").getMatch(0);
        }
        // if (waitStr == null) {
        // /* 2020-03-27: Workaround - hardcoded waittime */
        // waitStr = "40";
        // }
        return waitStr;
    }

    @Override
    public Form findFormDownload1Free() throws Exception {
        /* 2020-03-27: Special */
        Form download1 = super.findFormDownload1Free();
        if (download1 == null) {
            for (Form form : br.getForms()) {
                if (form.containsHTML("waitingToken")) {
                    download1 = form;
                    break;
                }
            }
        }
        if (download1 != null) {
            try {
                this.waitTime(this.getDownloadLink(), System.currentTimeMillis());
            } catch (final Throwable e) {
                logger.log(e);
            }
        }
        return download1;
    }
}