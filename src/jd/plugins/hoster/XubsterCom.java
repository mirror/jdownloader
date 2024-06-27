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

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class XubsterCom extends XFileSharingProBasic {
    public XubsterCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2019-06-27: Premium untested, set FREE account limits <br />
     * captchatype-info: 2019-06-27: reCaptchaV2<br />
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
        ret.add(new String[] { "xubster.com" });
        return ret;
    }

    @Override
    public void doFree(DownloadLink link, Account account) throws Exception, PluginException {
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        super.doFree(link, account);
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
            return -2;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return -2;
        } else {
            /* Free(anonymous) and unknown account type */
            return -2;
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return -1;
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
    public void handleCaptcha(final DownloadLink link, final Browser br, final Form captchaForm) throws Exception {
        if (br.getURL().contains("/login") && captchaForm.hasInputFieldByName("g-recaptcha-response") && captchaForm.containsHTML("recaptcha\\.com")) {
            /* 2020-11-13: Special - login reCaptchaV2 required */
            if (handleRecaptchaV2(link, br, captchaForm)) {
                link.setProperty(PROPERTY_captcha_required, Boolean.TRUE);
            }
        } else if (StringUtils.containsIgnoreCase(getCorrectBR(br), "/captchas/")) {
            /* 2021-08-23: Special */
            logger.info("Detected captcha method \"Standard captcha\" for this host");
            final String[] sitelinks = HTMLParser.getHttpLinks(br.toString(), "");
            String captchaurl = null;
            if (sitelinks == null || sitelinks.length == 0) {
                logger.warning("Standard captcha captchahandling broken!");
                checkErrorsLastResort(br, link, null);
            }
            for (final String linkTmp : sitelinks) {
                if (linkTmp.contains("/captchas/")) {
                    captchaurl = linkTmp;
                    break;
                }
            }
            if (StringUtils.isEmpty(captchaurl)) {
                /* Fallback e.g. for relative URLs (e.g. subyshare.com [bad example, needs special handling anways!]) */
                captchaurl = new Regex(getCorrectBR(br), "(/captchas/[a-z0-9]+\\.jpg)").getMatch(0);
            }
            if (captchaurl == null) {
                logger.warning("Standard captcha captchahandling broken2!");
                checkErrorsLastResort(br, link, null);
            }
            /* 2021-08-23: Special: Their captchas are different from the normal XFS captchas and we cannot auto-recognize them. */
            String code = getCaptchaCode("xfilesharingprobasic_special", captchaurl, link);
            captchaForm.put("code", code);
            logger.info("Put captchacode " + code + " obtained by captcha metod \"Standard captcha\" in the form.");
            link.setProperty(PROPERTY_captcha_required, Boolean.TRUE);
        } else {
            super.handleCaptcha(link, br, captchaForm);
        }
    }
    /**
     * 2021-07-02: This was because of hcaptcha when we did not yet support it --> They're not using hcaptcha anymore --> Normal login is
     * possible again!
     */
    // @Override
    // protected boolean requiresCookieLogin() {
    // return true;
    // }
}