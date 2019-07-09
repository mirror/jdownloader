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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.XFileSharingProBasic;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class SubyShareCom extends XFileSharingProBasic {
    public SubyShareCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2019-07-08: Premium untested, set FREE account limits <br />
     * captchatype-info: 2019-07-08: 4dignum --> xfilesharingprobasic_subysharecom_special <br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "subyshare.com" });
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
            return 1;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return 3;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return 3;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 3;
    }

    @Override
    protected void checkErrors(final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        super.checkErrors(link, account, checkAll);
        /* 2019-07-08: Special */
        if (new Regex(correctedBR, "Sorry\\s*,\\s*we do not support downloading from Dedicated servers|Please download from your PC without using any above services|If this is our mistake\\s*,\\s*please contact").matches()) {
            if (account != null) {
                throw new AccountUnavailableException("VPN download prohibited by this filehost", 15 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "VPN download prohibited by this filehost");
            }
        }
    }

    @Override
    protected String regexWaittime() {
        String waitStr = super.regexWaittime();
        if (StringUtils.isEmpty(waitStr)) {
            /* 2018-07-19: Special */
            waitStr = new Regex(correctedBR, "class\\s*=\\s*\"seconds\"[^>]*?>\\s*?(\\d+)\\s*?<").getMatch(0);
        }
        return waitStr;
    }

    @Override
    public void handleCaptcha(final DownloadLink link, final Form captchaForm) throws Exception {
        /*
         * 2019-07-08: Special for two reasons: 1. Upper handling won't find the '/captchas/' URL. 2. These captchas are special: 6 digits
         * instead of 4 and colored (orange instead of black) - thus our standard XFS captcha-mathod won't be able to recognize them!
         */
        if (StringUtils.containsIgnoreCase(correctedBR, "/captchas/")) {
            logger.info("Detected captcha method \"Standard captcha\" for this host");
            final String captchaurl = new Regex(correctedBR, "(/captchas/[^<>\"\\']*)").getMatch(0);
            if (captchaurl == null) {
                logger.warning("Standard captcha captchahandling broken!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String code = getCaptchaCode("xfilesharingprobasic_subysharecom_special", captchaurl, link);
            captchaForm.put("code", code);
            logger.info("Put captchacode " + code + " obtained by captcha metod \"Standard captcha\" in the form.");
        } else {
            super.handleCaptcha(link, captchaForm);
        }
    }

    @Override
    protected String regExTrafficLeft() {
        /* 2018-07-19: Special */
        String trafficleftStr = super.regExTrafficLeft();
        if (StringUtils.isEmpty(trafficleftStr)) {
            trafficleftStr = new Regex(correctedBR, "Usable Bandwidth\\s*<span class=\"[^\"]+\">(\\d+(?:\\.\\d{1,2})? [A-Za-z]{2,5}) / [^<]+<").getMatch(0);
        }
        return trafficleftStr;
    }
}