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

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "debrid-file.com" }, urls = { "" })
public class DebridFileCom extends PluginForHost {
    /* This is a "updated" version of website tout-debrid.ch */
    private static final String          WEBSITE_BASE = "https://debrid-file.com";
    private static MultiHosterManagement mhm          = new MultiHosterManagement("debrid-file.com");
    private static final boolean         resume       = true;
    /** 2020-05-22: PHG: In my tests, it is OK for the chunkload with the value of 10 */
    private static final int             maxchunks    = -10;
    private static final int             maxdownloads = -1;

    @SuppressWarnings("deprecation")
    public DebridFileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(WEBSITE_BASE + "/site/paiement");
    }

    @Override
    public String getAGBLink() {
        return WEBSITE_BASE + "/site/faq";
    }

    private Browser prepBR(final Browser br) {
        br.setCookiesExclusive(true);
        // br.getHeaders().put("User-Agent", "JDownloader");
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        prepBR(this.br);
        mhm.runCheck(account, link);
        if (!attemptStoredDownloadurlDownload(link)) {
            br.setFollowRedirects(true);
            this.loginWebsite(account, false);
            br.getPage(WEBSITE_BASE + "/service");
            final String csrfTokenStr = br.getRegex("meta name=\"csrf-token\" content=\"(.*?)>").getMatch(0);
            br.setHeader("Referer", WEBSITE_BASE + "/service");
            br.setHeader("x-csrf-token", csrfTokenStr);
            br.postPage("/service/get-link", "link=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final String html = entries.get("html").toString();
            final String dllink = new Regex(html, "href='(http[^<>\"\\']+)'>").getMatch(0);
            if (dllink == null) {
                final String errormsg = new Regex(html, "class=\"alert alert-danger\">([^<]+)</div>").getMatch(0);
                if (errormsg != null) {
                    mhm.handleErrorGeneric(account, link, errormsg, 50, 5 * 60 * 1000l);
                } else {
                    mhm.handleErrorGeneric(account, link, "Failed to find final downloadurl", 50, 5 * 60 * 1000l);
                }
            }
            link.setProperty(this.getHost() + "directlink", dllink);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                mhm.handleErrorGeneric(account, link, "Final downloadurl did not lead to downloadable content", 10, 5 * 60 * 1000l);
            }
        }
        this.dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link) throws Exception {
        final String url = link.getStringProperty(this.getHost() + "directlink");
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        boolean valid = false;
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resume, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                valid = true;
                return true;
            } else {
                link.removeProperty(this.getHost() + "directlink");
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            return false;
        } finally {
            if (!valid) {
                try {
                    dl.getConnection().disconnect();
                } catch (Throwable ignore) {
                }
                this.dl = null;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        prepBR(this.br);
        final AccountInfo ai = new AccountInfo();
        loginWebsite(account, true);
        final String premiumDaysStr = br.getRegex("(Premium :|VIP\\sexpires\\sin)\\s*(\\d+)\\s*<small").getMatch(1);
        String trafficleftStr = br.getRegex("</small>((<b>(\\d+(\\.|)\\d{1,2} [A-Za-z]+)</b>)|<strong>([A-Za-zé]+)</strong>)").getMatch(0);
        if (trafficleftStr == null) {
            /* 2022-08-09 */
            trafficleftStr = br.getRegex("<strong id=\"trafficLeft\"[^>]*>([^<]+)<").getMatch(0);
        }
        if (premiumDaysStr != null) {
            /* Premium */
            account.setType(AccountType.PREMIUM);
            ai.setValidUntil(System.currentTimeMillis() + Long.parseLong(premiumDaysStr) * 24 * 60 * 60 * 1000l, this.br);
        } else {
            /* Free or plugin failure */
            account.setType(AccountType.FREE);
            ai.setTrafficMax("10 GB");
        }
        account.setMaxSimultanDownloads(maxdownloads);
        if (trafficleftStr == null) {
            /* Downloads are not possible if the traffic has not be retrieved */
            ai.setTrafficLeft(0);
        } else if (trafficleftStr.contains("Unlimited")) {
            ai.setUnlimitedTraffic();
        } else {
            ai.setTrafficLeft(SizeFormatter.getSize(trafficleftStr));
        }
        /*
         * Get list of supported hosts.
         */
        final String[] hosts = br.getRegex("/hostlar/([^<>\"]+)\\.png\"").getColumn(0);
        ai.setMultiHostSupport(this, Arrays.asList(hosts));
        account.setConcurrentUsePossible(true);
        return ai;
    }

    private void loginWebsite(final Account account, final boolean verifyCookies) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Trying to login via cookies");
                    br.setCookies(WEBSITE_BASE, cookies);
                    if (!verifyCookies) {
                        /* Do not check cookies */
                        return;
                    }
                    br.getPage(WEBSITE_BASE + "/?language-picker-language=en-US");
                    /* Small workaround to prevent unwanted redirect: Website redirects to "/pay" in 120 seconds! */
                    final boolean followRedirectsOld = br.isFollowingRedirects();
                    try {
                        br.setFollowRedirects(false);
                        br.getPage("/service");
                    } finally {
                        br.setFollowRedirects(followRedirectsOld);
                    }
                    if (this.isLoggedIN(this.br)) {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(br.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                    }
                }
                logger.info("Performing full login");
                br.getPage(WEBSITE_BASE + "/?language-picker-language=en-US");
                br.getPage(WEBSITE_BASE + "/site/login");
                final Form loginform = br.getFormbyAction("/site/login");
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("LoginForm%5Busername%5D", Encoding.urlEncode(account.getUser()));
                loginform.put("LoginForm%5Bpassword%5D", Encoding.urlEncode(account.getPass()));
                if (CaptchaHelperHostPluginRecaptchaV2.containsRecaptchaV2Class(loginform)) {
                    /* 2020-03-19: reCaptchaV2 is always required but they may make it optional in the future. */
                    final DownloadLink dlinkbefore = this.getDownloadLink();
                    try {
                        final DownloadLink dl_dummy;
                        if (dlinkbefore != null) {
                            dl_dummy = dlinkbefore;
                        } else {
                            dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster() + "/service", true);
                            this.setDownloadLink(dl_dummy);
                        }
                        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                        loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    } finally {
                        this.setDownloadLink(dlinkbefore);
                    }
                }
                br.submitForm(loginform);
                if (!isLoggedIN(this.br)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedIN(final Browser br) throws PluginException {
        if (br.containsHTML("403") && br.containsHTML("Forbidden")) {
            /*
             * 2020-03-27: What does this mean? Is this supposed to be a temporary error? If so, you should use e.g. throw new
             * AccountUnavailableException("Error 403 'blocked by debrid-file'", 10 * 60 * 1000);
             *
             * 2020-03-27 : phg : This a temporary fix as we are not supposed to have a 403 error and we must fix the 403. I let the
             * previous code as it is not a temporary account error but a fatal plugin error
             */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Blocked by Debrid-File");
        } else if (br.containsHTML("/site/logout")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}