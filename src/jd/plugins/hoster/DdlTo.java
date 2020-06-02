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

import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.XFileSharingProBasic;
import org.jdownloader.plugins.components.config.XFSConfigVideoDdownloadCom;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
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
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DdlTo extends XFileSharingProBasic {
    public DdlTo(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2019-05-22: premium untested, set FREE account limits <br />
     * captchatype-info: 2020-05-18: reCaptchaV2<br />
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

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "ddownload.com", "ddl.to", "api.ddl.to", "esimpurcuesc.ddownload.com" });
        return ret;
    }

    @Override
    public String rewriteHost(String host) {
        return this.rewriteHost(getPluginDomains(), host, new String[0]);
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
    public String buildContainerDownloadURL(DownloadLink downloadLink, PluginForHost buildForThisPlugin) {
        final String ret = downloadLink.getContentUrl();
        if (ret != null) {
            return ret;
        } else {
            return buildExternalDownloadURL(downloadLink, buildForThisPlugin);
        }
    }

    @Override
    public String buildExternalDownloadURL(DownloadLink downloadLink, PluginForHost buildForThisPlugin) {
        final String fid = getFUIDFromURL(downloadLink);
        if (fid != null) {
            if (this.supports_https()) {
                return "http://" + getHost() + "/" + fid;
            } else {
                return "https://" + getHost() + "/" + fid;
            }
        } else {
            return super.buildExternalDownloadURL(downloadLink, buildForThisPlugin);
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
        final XFSConfigVideoDdownloadCom cfg = PluginJsonConfig.get(this.getConfigInterface());
        return cfg.getMaxSimultaneousFreeDownloads();
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
        /* 2020-01-16: Special: Disabled as it would return invalid results */
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
        final Regex trafficleft = new Regex(correctedBR, "<span>Traffic available</span>\\s*<div class=\"price\"><sup>([^<>]+)</sup>(-?\\d+)</div>");
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
    public void doFree(DownloadLink link, Account account) throws Exception, PluginException {
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        super.doFree(link, account);
    }

    @Override
    public String[] scanInfo(final String[] fileInfo) {
        /* 2020-05-17 */
        fileInfo[0] = new Regex(correctedBR, "<div class=\"name position-relative\">\\s*<h4>([^<>\"]+)</h4>").getMatch(0);
        fileInfo[1] = new Regex(correctedBR, "class=\"file-size\">([^<>\"]+)<").getMatch(0);
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
    protected AccountInfo fetchAccountInfoAPI(final Browser br, final Account account) throws Exception {
        final Browser brc = br.cloneBrowser();
        final AccountInfo ai = super.fetchAccountInfoAPI(brc, account);
        /* Original XFS API ('API Mod') does not return trafficleft but theirs is modified and more useful! */
        /* 2019-11-27: Not sure but this must be the traffic you can buy via 'extend traffic': /?op=payments */
        final String premium_extra_trafficStr = PluginJSonUtils.getJson(brc, "premium_traffic_left");
        final String trafficleftStr = PluginJSonUtils.getJson(brc, "traffic_left");
        // final String trafficusedStr = PluginJSonUtils.getJson(brc, "traffic_used");
        /*
         * 2020-02-17: Their API has a bug where it randomly returns wrong values for some users and they did not fix it within 2 weeks:
         * https://board.jdownloader.org/showthread.php?t=82525&page=2
         */
        /* 2020-05-22: This bug is fixed now according to their support --> We can trust this API value again! */
        final boolean trustAPITrafficLeft = false;
        if (account.getType() != null && account.getType() == AccountType.PREMIUM && trafficleftStr != null && trafficleftStr.matches("\\d+")) {
            long traffic_left = SizeFormatter.getSize(trafficleftStr + "MB");
            if (premium_extra_trafficStr != null && premium_extra_trafficStr.matches("\\d+")) {
                final long premium_extra_traffic = SizeFormatter.getSize(premium_extra_trafficStr + "MB");
                if (premium_extra_traffic > 0) {
                    traffic_left += premium_extra_traffic;
                    if (ai.getStatus() != null) {
                        ai.setStatus(ai.getStatus() + " | Extra traffic available: " + SizeFormatter.formatBytes(premium_extra_traffic));
                    } else {
                        ai.setStatus("Premium account | Extra traffic available: " + SizeFormatter.formatBytes(premium_extra_traffic));
                    }
                }
            }
            if (trustAPITrafficLeft) {
                logger.info("Trust API trafficleft value: " + traffic_left);
                ai.setTrafficLeft(traffic_left);
            } else {
                logger.info("Setting unlimited traffic instead of API trafficleft value " + traffic_left + " to prefer website value");
                ai.setUnlimitedTraffic();
            }
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
    protected AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        final AccountInfo ai = super.fetchAccountInfoWebsite(account);
        /*
         * 2020-05-05: Accounts created e.g. with premium balance of other accounts will be fine to login but if they do not yet contain an
         * e-mail address, they cannot be used for downloading and no matter which URL the user accesses (apart from API), the website will
         * redirect him to the account overview page with a message that tells him to add his e-mail address.
         */
        /*
         * 2020-05-06: Template also has handling for this but will not detect it until download-start which is why we will keep it in here
         * too.
         */
        this.getPage("/?op=my_reports");
        if (new Regex(correctedBR, ">\\s*?Please enter your e-mail").matches()) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, String.format("Ergänze deine E-Mail Adresse unter %s/?op=my_account#settings um diesen Account verwenden zu können!", this.getHost()), PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, String.format("Go to %s/?op=my_account#settings and enter your e-mail in order to be able to use this account!", this.getHost()), PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
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

    @Override
    protected boolean supports_availablecheck_filename_abuse() {
        /* 2020-04-20: Not supported anymore */
        return false;
    }

    @Override
    protected boolean supports_availablecheck_alt() {
        /* 2020-04-20: Not supported anymore */
        return false;
    }

    @Override
    protected boolean supports_mass_linkcheck_over_api() {
        return isAPIKey(this.getAPIKey());
    }

    @Override
    protected boolean supports_single_linkcheck_over_api() {
        return isAPIKey(this.getAPIKey());
    }

    // @Override
    // public String regexFilenameAbuse(final Browser br) {
    // String filename = br.getRegex("label>Filename</label>\\s*<input[^>]*value=\"([^<>\"]+)\"").getMatch(0);
    // if (StringUtils.isEmpty(filename)) {
    // /* Fallback to template */
    // filename = super.regexFilenameAbuse(br);
    // }
    // return filename;
    // }
    @Override
    public Class<? extends XFSConfigVideoDdownloadCom> getConfigInterface() {
        return XFSConfigVideoDdownloadCom.class;
    }
}