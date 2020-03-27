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
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FileupOrg extends XFileSharingProBasic {
    public FileupOrg(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2019-02-18: reCaptchaV2<br />
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
        ret.add(new String[] { "file-up.org", "file-up.io", "file-up.cc", "file-up.com", "file-upload.org", "file-upload.io", "file-upload.cc", "file-upload.com" });
        return ret;
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
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
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
    public String[] scanInfo(final String[] fileInfo) {
        super.scanInfo(fileInfo);
        if (StringUtils.isEmpty(fileInfo[1])) {
            fileInfo[1] = new Regex(correctedBR, "You have requested.*?https?://(?:www\\.)?[^/]+/\\s*?" + this.fuid + "</span>\\s*?\\((\\d+(?:\\.\\d{1,2})? [A-Za-z]{2,5})\\)</p>").getMatch(0);
        }
        return fileInfo;
    }

    @Override
    protected boolean supports_availablecheck_filesize_html() {
        /* 2019-07-02: Special */
        return false;
    }

    @Override
    public String regexWaittime() {
        String ttt = super.regexWaittime();
        if (StringUtils.isEmpty(ttt)) {
            ttt = new Regex(correctedBR, "<span id=\"countdown\">[^<>]*?<span class=\"label label\\-danger seconds\">(\\d+)</span>").getMatch(0);
        }
        return ttt;
    }
    // @Override
    // protected void handleRecaptchaV2(final DownloadLink link, final Form captchaForm) throws Exception {
    // /* 2020-03-27: Special */
    // final String reCaptchav2key = br.getRegex("grecaptcha\\.execute\\(\\'([^<>\"\\']+)'").getMatch(0);
    // if (reCaptchav2key == null) {
    // /* Fallback to template handling */
    // super.handleRecaptchaV2(link, captchaForm);
    // return;
    // }
    // logger.info("Detected captcha method \"RecaptchaV2\" type 'normal' for this host");
    // final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br, reCaptchav2key);
    // if (!this.preDownloadWaittimeSkippable()) {
    // final String waitStr = regexWaittime();
    // if (waitStr != null && waitStr.matches("\\d+")) {
    // final int preDownloadWaittime = Integer.parseInt(waitStr) * 1001;
    // final int reCaptchaV2Timeout = rc2.getSolutionTimeout();
    // if (preDownloadWaittime > reCaptchaV2Timeout) {
    // /*
    // * Admins may sometimes setup waittimes that are higher than the reCaptchaV2 timeout so lets say they set up 180 seconds
    // * of pre-download-waittime --> User solves captcha immediately --> Captcha-solution times out after 120 seconds -->
    // * User has to re-enter it (and it would fail in JD)! If admins set it up in a way that users can solve the captcha via
    // * the waittime counts down, this failure may even happen via browser (example: xubster.com)! See workaround below!
    // */
    // /*
    // * This is basically a workaround which avoids running into reCaptchaV2 timeout: Make sure that we wait less than 120
    // * seconds after the user has solved the captcha. If the waittime is higher than 120 seconds, we'll wait two times:
    // * Before AND after the captcha!
    // */
    // final int prePrePreDownloadWait = preDownloadWaittime - reCaptchaV2Timeout;
    // logger.info("Waittime is higher than reCaptchaV2 timeout --> Waiting a part of it before solving captcha to avoid timeouts");
    // logger.info("Pre-pre download waittime seconds: " + (prePrePreDownloadWait / 1000));
    // this.sleep(prePrePreDownloadWait, link);
    // }
    // }
    // }
    // final String recaptchaV2Response = rc2.getToken();
    // captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
    // }
}