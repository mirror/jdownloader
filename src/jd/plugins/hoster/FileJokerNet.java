package jd.plugins.hoster;

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
//along with this program.  If not, see <http://www.gnu.org/licenses/>.;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.XFileSharingProBasic;
import org.jdownloader.plugins.components.XFileSharingProBasicSpecialFilejoker;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FileJokerNet extends XFileSharingProBasicSpecialFilejoker {
    public FileJokerNet(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
        this.setConfigElements();
        /* 2019-08-20: Without this, users might experience 503 errors via API and captchas via website. Website is more strict than API! */
        if (useAPIZeusCloudManager()) {
            /* 2019-08-28: Tested - this should avoid 503 errors completely */
            this.setStartIntervall(2000l);
        } else {
            /* 2019-08-28: Tested - this should avoid 'site-verification' captchas completely! */
            this.setStartIntervall(8000l);
        }
    }

    @Override
    public Browser prepBrowser(final Browser prepBr, final String host) {
        super.prepBrowser(prepBr, host);
        final String custom_referer = this.getPluginConfig().getStringProperty("CUSTOM_REFERER", null);
        if (!StringUtils.isEmpty(custom_referer)) {
            prepBr.getHeaders().put("Referer", custom_referer);
        }
        return prepBr;
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2019-08-21: All checked and set <br />
     * captchatype-info: 2019-08-21: reCaptchaV2<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "filejoker.net" });
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
            return 1;
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
        /* 2019-08-15: Special */
        final Regex finfo = new Regex(correctedBR, "class=\"name-size\">([^<>\"]+)<small>\\(([^<>\"]+)\\)</small>");
        fileInfo[0] = finfo.getMatch(0);
        fileInfo[1] = finfo.getMatch(1);
        if (StringUtils.isEmpty(fileInfo[0]) || StringUtils.isEmpty(fileInfo[1])) {
            /* Fallback to template method */
            super.scanInfo(fileInfo);
        }
        return fileInfo;
    }

    @Override
    protected boolean supports_availablecheck_filesize_html() {
        /* 2019-08-15: Special */
        return false;
    }

    @Override
    protected String regexWaittime() {
        /* 2019-08-15: Special */
        String wait = new Regex(correctedBR, "class=\"alert\\-success\">(\\d+)</span>").getMatch(0);
        if (StringUtils.isEmpty(wait)) {
            wait = super.regexWaittime();
        }
        return wait;
    }

    @Override
    protected void checkErrors(final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        /* 2019-08-15: Special */
        final String waitMsg = new Regex(correctedBR, "((You have reached the download(\\-| )limit|You have to wait|Wait .*? to download for free)[^<>]+)").getMatch(0);
        super.checkErrors(link, account, checkAll);
        if (waitMsg != null) {
            /* adjust this regex to catch the wait time string for COOKIE_HOST */
            final String tmphrs = new Regex(waitMsg, "\\s+(\\d+)\\s+hours?").getMatch(0);
            final String tmpmin = new Regex(waitMsg, "\\s+(\\d+)\\s+minutes?").getMatch(0);
            final String tmpsec = new Regex(waitMsg, "\\s+(\\d+)\\s+seconds?").getMatch(0);
            final String tmpdays = new Regex(waitMsg, "\\s+(\\d+)\\s+days?").getMatch(0);
            int waittime;
            if (tmphrs == null && tmpmin == null && tmpsec == null && tmpdays == null) {
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
        if (correctedBR.contains("There is not enough traffic available to download this file")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Not enough traffic available", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        if (correctedBR.contains("You don't have permission to download this file")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "You don't have permission to download this file", 30 * 60 * 1000l);
        }
    }

    @Override
    protected String getRelativeAccountInfoURL() {
        /* 2019-08-20: Special */
        return "/profile";
    }

    @Override
    protected boolean allows_multiple_login_attempts_in_one_go() {
        /* 2019-08-20: Special */
        return true;
    }

    private void setConfigElements() {
        /* 2019-08-20: This host grants (free-)users with special Referer values better downloadspeeds. */
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, this.getPluginConfig(), "CUSTOM_REFERER", "Set custom Referer here").setDefaultValue(null));
    }
    /* *************************** SPECIAL API STUFF STARTS HERE *************************** */

    @Override
    protected boolean useAPIZeusCloudManager() {
        return true;
    }

    @Override
    protected String getRelativeAPIBaseAPIZeusCloudManager() {
        return "/zapi";
    }

    @Override
    protected String getRelativeAPILoginParamsFormatAPIZeusCloudManager() {
        return "?op=login&email=%s&pass=%s";
    }
}