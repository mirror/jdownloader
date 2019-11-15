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
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.PluginException;

import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.XFileSharingProBasic;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DdlTo extends XFileSharingProBasic {
    private final String   maxSimultaneousDownloads_LIMIT = "MaxSimultaneousDownloads_LIMIT_2019_06";
    private final String[] maxSimultaneousDownloads       = new String[] { "DEFAULT", "2", "1" };

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
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return XFileSharingProBasic.buildAnnotationUrls(getPluginDomains());
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "ddl.to" });
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
        super.scanInfo(fileInfo);
        if (StringUtils.isEmpty(fileInfo[0])) {
            fileInfo[0] = new Regex(correctedBR, "<div class=\"name\">\\s*<h4>([^<>\"]+)</h4>").getMatch(0);
        }
        return fileInfo;
    }

    // /** TODO: 2019-11-11: Use this once they've updated their API. */
    // @Override
    // protected boolean allow_api_download_if_apikey_is_available(final Account account) {
    // final boolean apikey_is_available = this.getAPIKey(account) != null;
    // /* API download is available for premium accounts only. */
    // return apikey_is_available && account != null && account.getType() == AccountType.PREMIUM;
    // }
    //
    // /** TODO: 2019-11-11: Use this once they've updated their API. */
    // @Override
    // protected AccountInfo fetchAccountInfoAPI(final Browser br, final Account account, final boolean setAndAnonymizeUsername) throws
    // Exception {
    // final Browser brc = br.cloneBrowser();
    // final AccountInfo ai = super.fetchAccountInfoAPI(brc, account, setAndAnonymizeUsername);
    // /* Original XFS API ('API Mod') does not return trafficleft - their API does. Set it! */
    // final String trafficleftStr = PluginJSonUtils.getJson(brc, "traffic_left");
    // if (trafficleftStr != null && trafficleftStr.matches("\\d+")) {
    // ai.setTrafficLeft(Long.parseLong(trafficleftStr) * 1024 * 1024);
    // }
    // return ai;
    // }
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

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), maxSimultaneousDownloads_LIMIT, maxSimultaneousDownloads, "Max. simultaneous downloads (Free+Free account)").setDefaultValue(0));
    }
}