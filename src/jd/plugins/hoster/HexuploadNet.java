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
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.Regex;
import org.appwork.utils.Time;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class HexuploadNet extends XFileSharingProBasic {
    public HexuploadNet(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
        /* 2023-10-16: For free account downloads, see: https://board.jdownloader.org/showthread.php?t=94468 */
        this.setStartIntervall(1500);
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2020-03-11: Premium untested <br />
     * captchatype-info: 2020-03-11: reCaptchaV2<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "hexload.com", "hexupload.net", "hexupload.com" });
        return ret;
    }

    @Override
    public Browser prepBrowser(final Browser prepBr, final String host) {
        super.prepBrowser(prepBr, host);
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            prepBr.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            prepBr.getHeaders().put("sec-ch-ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"");
            prepBr.getHeaders().put("sec-ch-ua-mobile", "?0");
            prepBr.getHeaders().put("sec-ch-ua-platform", "\"Windows\"");
            prepBr.getHeaders().put("Accept", "*/*");
            prepBr.getHeaders().put("sec-fetch-site", "same-origin");
            prepBr.getHeaders().put("sec-fetch-mode", "cors");
            prepBr.getHeaders().put("sec-fetch-dest", "empty");
            prepBr.getHeaders().put("Origin", "https://hexload.com");
            // prepBr.setCookie(getMainPage(), "lang", "german");
        }
        return prepBr;
    }

    @Override
    protected String getDllink(DownloadLink link, Account account, Browser br, String src) {
        if (br.getRequest().getHtmlCode().startsWith("{")) {
            /* 2023-08-14 */
            try {
                final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                final String urlb64Encoded = entries.get("link").toString();
                return Encoding.Base64Decode(urlb64Encoded);
            } catch (final Throwable e) {
                logger.log(e);
                logger.warning("Ajax handling failed");
            }
        }
        String ret = super.getDllink(link, account, br, src);
        if (ret == null) {
            final String base64 = br.getRegex("ldl\\.ld\\('(aHR0c.*?)'").getMatch(0);
            if (base64 != null) {
                ret = Encoding.Base64Decode(base64);
            }
        }
        return ret;
    }

    @Override
    public String rewriteHost(final String host) {
        /* 2023-12-19: Main domain changed from hexupload.net to hexload.com */
        return this.rewriteHost(getPluginDomains(), host, new String[0]);
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
    public Form findFormDownload2Premium(final DownloadLink downloadLink, final Account account, final Browser br) throws Exception {
        /* 2022-04-07: Special */
        handleSecurityVerification(br);
        Form form = super.findFormDownload2Premium(downloadLink, account, br);
        if (form == null) {
            form = br.getFormbyProperty("name", "FDL");
            if (form == null) {
                form = br.getFormByInputFieldPropertyKeyValue("op", "download2");
            }
        }
        return form;
    }

    @Override
    public Form findFormDownload1Free(final Browser br) throws Exception {
        /* 2022-04-07: Special */
        handleSecurityVerification(br);
        final Form download1 = super.findFormDownload1Free(br);
        if (download1 != null && br.containsHTML("type: 'POST',\\s*url: 'https?://[^/]+/download'")) {
            /* 2023-08-14 */
            download1.put("ajax", "1");
            download1.put("method_free", "1");
            download1.put("dataType", "json");
        }
        return download1;
    }

    @Override
    protected void getPage(final String page) throws Exception {
        /* 2022-04-07: Special */
        super.getPage(page);
        handleSecurityVerification(br);
    }

    @Override
    protected void postPage(final String page, final String data) throws Exception {
        /* 2022-04-07: Special */
        super.postPage(page, data);
        handleSecurityVerification(br);
    }

    @Override
    protected void submitForm(final Form form) throws Exception {
        /* 2022-04-07: Special */
        super.submitForm(form);
        handleSecurityVerification(br);
    }

    protected void handleSecurityVerification(final Browser br) throws Exception {
        /* Usually this == FUID of DownloadLink. */
        if (br.getURL() == null) {
            return;
        }
        final String siteProtectionID = br.getRegex("class=\"cssbuttons-io-button fake-link\"[^>]*><span class=\"url\"[^>]*>([a-z0-9]{12})</span>").getMatch(0);
        if (siteProtectionID == null) {
            return;
        }
        if (!(Thread.currentThread() instanceof SingleDownloadController)) {
            logger.info("We only handle site-protection during download");
            return;
        }
        logger.info("Handling securityVerification");
        final boolean oldFollowRedirectValue = br.isFollowingRedirects();
        try {
            // final String sourceURL = br.getURL();
            final String sourceHost = br.getHost();
            br.setFollowRedirects(true);
            br.getPage("https://digiomo.com/het" + siteProtectionID + ".php");
            final Form securityVerification = br.getFormbyKey("file_code");
            if (securityVerification == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final long timeBeforeCaptcha = Time.systemIndependentCurrentJVMTimeMillis();
            /* 2022-04-07: Waittime is skippable */
            final boolean skipWaittime = true;
            final String waitStr = br.getRegex("sec = (\\d+);").getMatch(0);
            final long waitMillis = Long.parseLong(waitStr) * 1001l;
            boolean captchaRequired = false;
            /* 2022-04-07: Currently they require one reCaptchaV3 (invisible) + one hCaptcha to be solved. */
            if (containsHCaptcha(securityVerification)) {
                captchaRequired = true;
                handleHCaptcha(getDownloadLink(), br, securityVerification);
            }
            if (containsRecaptchaV2Class(securityVerification)) {
                captchaRequired = true;
                handleRecaptchaV2(getDownloadLink(), br, securityVerification);
            }
            if (!captchaRequired) {
                logger.warning("No captcha required --> Possible failure");
            }
            final long timePassedDuringCaptcha = Time.systemIndependentCurrentJVMTimeMillis() - timeBeforeCaptcha;
            final long remainingWaittime = waitMillis - timePassedDuringCaptcha;
            if (remainingWaittime > 0) {
                logger.info("Remaining wait after captcha: " + (remainingWaittime / 1000));
                if (!skipWaittime) {
                    this.sleep(remainingWaittime, this.getDownloadLink());
                }
            }
            submitForm(br, securityVerification);
            final Form lookaheadDownload1Form = br.getFormbyActionRegex(".*" + Regex.escape(sourceHost) + ".*");
            if (lookaheadDownload1Form != null) {
                logger.info("SiteVerification result: Success");
            } else {
                logger.warning("SiteVerification result: Possible failure");
            }
        } finally {
            br.setFollowRedirects(oldFollowRedirectValue);
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
}