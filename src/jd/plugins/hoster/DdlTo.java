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

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.XFileSharingProBasic;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DdlTo extends XFileSharingProBasic {
    private final String   maxSimultaneousDownloads_LIMIT = "MaxSimultaneousDownloads_LIMIT_2019_06";
    private final String[] maxSimultaneousDownloads       = new String[] { "DEFAULT", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" };

    public DdlTo(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
        setConfigElements();
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2019-05-22: premium untested, set FREE account limits <br />
     * captchatype-info: 2019-05-22: null<br />
     * other:<br />
     */
    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        final String[] extraNames = { "ddl" };
        final String[] officiallySupportedNames = buildSupportedNames(getPluginDomains());
        String[] finalSupportedNames = new String[officiallySupportedNames.length + extraNames.length];
        System.arraycopy(officiallySupportedNames, 0, finalSupportedNames, 0, officiallySupportedNames.length);
        System.arraycopy(extraNames, 0, finalSupportedNames, officiallySupportedNames.length, extraNames.length);
        return finalSupportedNames;
    }

    public static String[] getAnnotationUrls() {
        return XFileSharingProBasic.buildAnnotationUrls(getPluginDomains());
    }

    /** 2020-04-17: TODO: Switch to ddownload.com once possible */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "ddl.to", "ddownload.com", "api.ddl.to" });
        return ret;
    }

    @Override
    public String rewriteHost(String host) {
        return this.rewriteHost(getPluginDomains(), host, new String[0]);
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        final String fuid = this.fuid != null ? this.fuid : getFUIDFromURL(link);
        if (fuid != null) {
            /* link cleanup, prefer https if possible */
            if (link.getPluginPatternMatcher() != null && link.getPluginPatternMatcher().matches("https?://[A-Za-z0-9\\-\\.:]+/embed-[a-z0-9]{12}")) {
                link.setContentUrl(getMainPage() + "/embed-" + fuid + ".html");
            }
            link.setPluginPatternMatcher(getMainPage() + "/" + fuid);
            link.setLinkID(getHost() + "://" + fuid);
        }
        /* Keep that legacy setting */
        if (this.getPluginConfig().getBooleanProperty("ENABLE_HTTP", true)) {
            link.setPluginPatternMatcher(link.getPluginPatternMatcher().replaceFirst("^https://", "http://"));
        } else {
            link.setPluginPatternMatcher(link.getPluginPatternMatcher().replaceFirst("^http://", "https://"));
        }
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
            return 1;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    public int getMaxDownloadSelect() {
        final int chosenDownloadLimit = getPluginConfig().getIntegerProperty(maxSimultaneousDownloads_LIMIT, 0);
        try {
            final String value = maxSimultaneousDownloads[chosenDownloadLimit];
            if ("DEFAULT".equals(value)) {
                return 1;
            } else {
                return Integer.parseInt(value);
            }
        } catch (final Throwable e) {
            /* Return default limit */
            logger.log(e);
            return 1;
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return getMaxDownloadSelect();
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return getMaxDownloadSelect();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 10;
    }

    @Override
    protected boolean supports_availablecheck_filesize_html() {
        /* 2020-01-16: Special */
        return false;
    }

    @Override
    public void handleCaptcha(final DownloadLink link, final Form captchaForm) throws Exception {
        /* 2019-08-14: Special: This might increase downloadspeed for free users */
        if (captchaForm != null && captchaForm.hasInputFieldByName("adblock_detected")) {
            captchaForm.put("adblock_detected", "0");
        }
        super.handleCaptcha(link, captchaForm);
    }

    @Override
    protected String regExTrafficLeft() {
        /* 2019-11-03: Special */
        final Regex trafficleft = new Regex(correctedBR, "<span>Traffic available</span>\\s*<div class=\"price\"><sup>([^<>]+)</sup>(\\d+)</div>");
        String availabletraffic = null;
        final String trafficleftUnit = trafficleft.getMatch(0);
        final String trafficleftTmp = trafficleft.getMatch(1);
        if (trafficleftUnit != null && trafficleftTmp != null) {
            availabletraffic = trafficleftTmp + trafficleftUnit;
        }
        if (availabletraffic == null) {
            /* Fallback to template handling */
            availabletraffic = super.regExTrafficLeft();
        }
        return availabletraffic;
    }

    @Override
    public String regexFilenameAbuse(final Browser br) {
        String filename = br.getRegex("label>Filename</label>\\s*<input[^>]*value=\"([^<>\"]+)\"").getMatch(0);
        if (StringUtils.isEmpty(filename)) {
            /* Fallback to template */
            filename = super.regexFilenameAbuse(br);
        }
        return filename;
    }

    @Override
    public void doFree(DownloadLink link, Account account) throws Exception, PluginException {
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        super.doFree(link, account);
    }

    @Override
    public String[] scanInfo(final String[] fileInfo) {
        fileInfo[0] = new Regex(correctedBR, "<div class=\"name\">\\s*<h4>([^<>\"]+)</h4>").getMatch(0);
        fileInfo[1] = new Regex(correctedBR, "<span>Uploaded on[^<]*?</span>\\s*<span>([^<>\"]+)</span>").getMatch(0);
        if (StringUtils.isEmpty(fileInfo[0]) || StringUtils.isEmpty(fileInfo[1])) {
            /* Fallback to template handling */
            super.scanInfo(fileInfo);
        }
        return fileInfo;
    }

    // /** TODO: 2019-11-11: Use this once they've updated their API. 2019-11-27: They will not fix this issue as they're afraid of it being
    // used by multihosts lol */
    // @Override
    // protected boolean allow_api_download_if_apikey_is_available(final Account account) {
    // final boolean apikey_is_available = this.getAPIKey(account) != null;
    // /* API download is available for premium accounts only. */
    // return apikey_is_available && account != null && account.getType() == AccountType.PREMIUM;
    // }
    //
    @Override
    protected AccountInfo fetchAccountInfoAPI(final Browser br, final Account account, final boolean setAndAnonymizeUsername) throws Exception {
        final Browser brc = br.cloneBrowser();
        final AccountInfo ai = super.fetchAccountInfoAPI(brc, account, setAndAnonymizeUsername);
        /* Original XFS API ('API Mod') does not return trafficleft but theirs is modified and more useful! */
        /* 2019-11-27: Not sure but this must be the traffic you can buy via 'extend traffic': /?op=payments */
        final String premium_extra_trafficStr = PluginJSonUtils.getJson(brc, "premium_traffic_left");
        final String trafficleftStr = PluginJSonUtils.getJson(brc, "traffic_left");
        // final String trafficusedStr = PluginJSonUtils.getJson(brc, "traffic_used");
        if (account.getType() != null && account.getType() == AccountType.PREMIUM && trafficleftStr != null && trafficleftStr.matches("\\d+")) {
            long traffic_left = Long.parseLong(trafficleftStr) * 1000 * 1000;
            if (premium_extra_trafficStr != null && premium_extra_trafficStr.matches("\\d+")) {
                final long premium_extra_traffic = Long.parseLong(premium_extra_trafficStr) * 1000 * 1000;
                traffic_left += premium_extra_traffic;
                if (premium_extra_traffic > 0) {
                    if (ai.getStatus() != null) {
                        ai.setStatus(ai.getStatus() + " | Extra traffic available: " + SizeFormatter.formatBytes(premium_extra_traffic));
                    } else {
                        ai.setStatus("Premium account | Extra traffic available: " + SizeFormatter.formatBytes(premium_extra_traffic));
                    }
                }
            }
            ai.setTrafficLeft(traffic_left);
            /*
             * 2020-02-17: Their API has a bug where it randomly returns wrong values for some users and they did not fix it within 2 weeks:
             * https://board.jdownloader.org/showthread.php?t=82525&page=2
             */
            logger.info("Setting unlimited traffic instead of API trafficleft value " + traffic_left + " to prefer website value");
            ai.setUnlimitedTraffic();
        } else {
            /*
             * They will return "traffic_left":"0" for free accounts which is wrong. It is unlimited on their website. By setting it to
             * unlimited here it will be re-checked via website by our XFS template!
             */
            ai.setUnlimitedTraffic();
        }
        return ai;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            /* 2019-11-11: Reset final downloadurls in dev mode. */
            link.removeProperty("freelink");
            link.removeProperty("freelink2");
            link.removeProperty("premlink");
        } else {
            super.resetDownloadlink(link);
        }
    }

    @Override
    protected boolean isOffline(final DownloadLink link) {
        /* 2020-01-17: Special */
        if (new Regex(correctedBR, ">\\s*This file was banned by copyright").matches()) {
            /* "<strong>Oops!</strong> This file was banned by copyright owner's report" */
            return true;
        }
        return super.isOffline(link);
    }

    @Override
    protected void checkErrors(final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        /* 2020-01-20: Special */
        if (new Regex(correctedBR, ">\\s*This server is in maintenance mode").matches()) {
            /* <strong>Oops!</strong> This server is in maintenance mode. Refresh this page in some minutes. */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This server is in maintenance mode", 15 * 60 * 1000l);
        }
        /* Now execute template handling */
        super.checkErrors(link, account, checkAll);
    }

    /* 2020-04-14: Workaround for Cloudflare hcaptcha issues: https://board.jdownloader.org/showthread.php?t=83712 */
    @Override
    protected String getMainPage() {
        /** 2020-04-17: TODO: Remove this Override / switch to new domain ddownload.com once possible. */
        final String host = "ddownload.com";
        // api.ddownload.com alternative
        // final String browser_host = this.br != null ? br.getHost() : null;
        // final String[] hosts = this.siteSupportedNames();
        // if (browser_host != null) {
        // host = browser_host;
        // } else {
        // /* 2019-07-25: This may not be correct out of the box e.g. for imgmaze.com */
        // host = hosts[0];
        // }
        String mainpage;
        final String protocol;
        if (this.supports_https()) {
            protocol = "https://";
        } else {
            protocol = "http://";
        }
        mainpage = protocol;
        if (requires_WWW()) {
            mainpage += "www.";
        }
        mainpage += host;
        return mainpage;
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), maxSimultaneousDownloads_LIMIT, maxSimultaneousDownloads, "Max. simultaneous downloads (Free+Free account)").setDefaultValue(0));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "ENABLE_HTTP", "Enable HTTP").setDefaultValue(false));
    }
}