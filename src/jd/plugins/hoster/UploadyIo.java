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
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.Time;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
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
public class UploadyIo extends XFileSharingProBasic {
    public UploadyIo(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2021-10-04: reCaptchaV2<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "uploady.io", "uploadydl.com" });
        return ret;
    }

    @Override
    protected List<String> getDeadDomains() {
        final ArrayList<String> deadDomains = new ArrayList<String>();
        deadDomains.add("uploadydl.com");
        return deadDomains;
    }

    @Override
    public String rewriteHost(final String host) {
        /* 2023-05-23: Merged separate hosterplugin for uploadydl.com into this one. */
        return this.rewriteHost(getPluginDomains(), host);
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
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return false;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return false;
        } else {
            /* Free(anonymous) and unknown account type */
            return false;
        }
    }

    @Override
    public int getMaxChunks(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return 1;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return 1;
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
    protected String regexWaittime(final String html) {
        final String waitStr = new Regex(html, "id=\"wait-time\"[^>]*>\\s*(\\d+)").getMatch(0);
        if (waitStr != null) {
            return waitStr;
        } else {
            return super.regexWaittime(html);
        }
    }

    @Override
    public void doFree(final DownloadLink link, final Account account) throws Exception, PluginException {
        /* First bring up saved final links */
        if (this.attemptStoredDownloadurlDownload(link, account)) {
            try {
                if (dl.getConnection() != null) {
                    fixFilename(dl.getConnection(), link);
                } else {
                    fixFilenameHLSDownload(link);
                }
            } catch (final Exception ignore) {
                logger.log(ignore);
            }
            logger.info("Using stored directurl");
            /* add a download slot */
            controlMaxFreeDownloads(account, link, +1);
            try {
                /* start the dl */
                dl.startDownload();
            } finally {
                /* remove download slot */
                controlMaxFreeDownloads(account, link, -1);
            }
            return;
        }
        requestFileInformationWebsite(link, account, true);
        checkErrors(br, getCorrectBR(br), link, account, false);
        final Form download1 = findFormDownload1Free(br);
        if (download1 == null) {
            this.checkErrorsLastResort(br, link, account);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* 2024-06-17: Disabled for now as the captcha is now happening after the wait time (also hCaptcha instead of reCaptchaV2). */
        final boolean handleCaptchaDuringWait = false;
        final Browser br3 = br.cloneBrowser();
        /* Wait time is counting serverside now */
        getPage(br3, br._getURL().getPath() + "?start_countdown=1");
        final long timeBefore = Time.systemIndependentCurrentJVMTimeMillis();
        final Map<String, Object> entries = restoreFromString(br3.getRequest().getHtmlCode(), TypeRef.MAP);
        final String innerhtml = (String) entries.get("ihtml");
        final String rand = entries.get("rand").toString();
        download1.put("rand", Encoding.urlEncode(rand));
        String recaptchaV2Response = null;
        if (handleCaptchaDuringWait) {
            /* Solve captcha "during" wait time */
            final String waitSecondsStr = this.regexWaittime(br);
            long waitMillis = 0;
            if (waitSecondsStr != null) {
                waitMillis = Long.parseLong(waitSecondsStr) * 1001;
            }
            if (innerhtml != null) {
                // Small hack
                br3.getRequest().setHtmlCode(innerhtml);
            }
            // this.handleCaptcha(link, br, dummyform);
            final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br3);
            this.waitBeforeInteractiveCaptcha(link, waitMillis, rc2.getSolutionTimeout());
            recaptchaV2Response = rc2.getToken();
        }
        /* Wait remaining time if needed */
        this.waitTime(link, timeBefore);
        submitForm(download1);
        checkErrors(br, getCorrectBR(br), link, account, false);
        final Form download2 = this.findFormDownload2Free(br);
        if (download2 == null) {
            this.checkErrorsLastResort(br, link, account);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (handleCaptchaDuringWait) {
            /* Captcha has already been solved */
            download2.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
        } else {
            this.handleCaptcha(link, br, download2);
        }
        submitForm(download2);
        String dllink = this.getDllink(link, account, br, br.getRequest().getHtmlCode());
        if (StringUtils.isEmpty(dllink)) {
            logger.warning("Failed to find final downloadurl");
            checkErrors(br, getCorrectBR(br), link, account, true);
            checkErrorsLastResort(br, link, account);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        handleDownload(link, account, null, dllink, null);
    }

    protected void waitBeforeInteractiveCaptcha(final DownloadLink link, final long waitMillis, final int captchaTimeoutMillis) throws PluginException {
        if (waitMillis > captchaTimeoutMillis) {
            final int prePrePreDownloadWait = (int) (waitMillis - captchaTimeoutMillis);
            logger.info("Waittime is higher than interactive captcha timeout --> Waiting a part of it before solving captcha to avoid captcha-token-timeout");
            logger.info("Pre-pre download waittime seconds: " + (prePrePreDownloadWait / 1000));
            this.sleep(prePrePreDownloadWait, link);
        }
    }

    @Override
    protected boolean containsRecaptchaV2Class(String string) {
        /* 2024-04-23 */
        if (new Regex(string, "<button[^>]*id=\"mfree_dwn\"[^>]*data-sitekey=\"").patternFind()) {
            return true;
        } else {
            return super.containsRecaptchaV2Class(string);
        }
    }

    @Override
    protected void checkErrors(final Browser br, final String html, final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        final String ipWaitMinutes = br.getRegex("Delay between downloads must not be less than (\\d+) minutes").getMatch(0);
        if (ipWaitMinutes != null) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(ipWaitMinutes) * 60 * 1000);
        }
        super.checkErrors(br, html, link, account, checkAll);
    }
}