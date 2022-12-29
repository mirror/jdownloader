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

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "zapisz.se" }, urls = { "https?://zapisz.se/files/\\d+/([^/]+)?" })
public class ZapiszSe extends PluginForHost {
    private static final String          WEBSITE_BASE = "https://zapisz.se";
    private static MultiHosterManagement mhm          = new MultiHosterManagement("zapisz.se");
    private static final boolean         resume       = true;
    private static final int             maxchunks    = -10;

    @SuppressWarnings("deprecation")
    public ZapiszSe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(WEBSITE_BASE + "/premium.html");
    }

    @Override
    public String getAGBLink() {
        return WEBSITE_BASE + "/terms.html";
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new AccountRequiredException();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.loginWebsite(account, false);
        br.setFollowRedirects(true);
        // looks like those files cannot be resumed!?
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), false, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (br.getURL().matches("https?://zapisz.se/?")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                mhm.handleErrorGeneric(account, link, "unknown_dl_error", 10, 5 * 60 * 1000l);
            }
        }
        this.dl.startDownload();
    }

    private void handleDLMultihoster(final Account account, final DownloadLink link, final String dllink) throws Exception {
        if (StringUtils.isEmpty(dllink)) {
            mhm.handleErrorGeneric(account, link, "dllinknull", 50, 5 * 60 * 1000l);
        }
        link.setProperty(this.getHost() + "directlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            mhm.handleErrorGeneric(account, link, "unknown_dl_error", 10, 5 * 60 * 1000l);
        }
        this.dl.startDownload();
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.br = newBrowser();
        mhm.runCheck(account, link);
        String dllink = checkDirectLink(link, this.getHost() + "directlink");
        br.setFollowRedirects(true);
        if (dllink == null) {
            this.loginWebsite(account, false);
            br.getPage(WEBSITE_BASE + "/addfiles.html");
            final Form dlform = br.getFormbyKey("list");
            if (dlform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // Only https URLs are downloaded from zapisz.se, http URLs are forwarded to the normal hoster
            dlform.put("list", Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this).replace("http://", "https://")));
            // k2s.cc Captcha fields do not need to be filled in
            dlform.put("k2s", "");
            dlform.put("k2skey", "");
            // An invisible ReCaptcha is now required for the form
            // Because the next part is blocking, it could happen that the captcha becomes invalid, since it has a one minute timeout
            // However, this way several captchas can be solved at once.
            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
            dlform.put("recaptcha_response", Encoding.urlEncode(recaptchaV2Response));
            synchronized (account) {
                br.submitForm(dlform);
                // Check if success message can be found: "1 link has been processed. To download files, go to the Your Files tab."
                final Boolean successNoteFound = br.getRegex("<div class=\"note-info note-success\">([^<]+)</div>").count() >= 1;
                if (!successNoteFound) {
                    // For unknown reasons zapisz.se sometimes does not add links to the queue
                    // For Rapidgator links, zapisz.se has unknown but noticeable rate limitations
                    // For Nitroflare and k2s.cc this may mean that an additional captcha has to be solved
                    mhm.handleErrorGeneric(account, link, "Failed to generate downloadurl", 10, 5 * 60 * 1000l);
                }
                br.getPage("/update.php?ie=0." + System.currentTimeMillis() + "&u=1&lastupdate=0");
            }
            final String[] urls = HTMLParser.getHttpLinks(br.toString(), br.getURL());
            if (urls == null || urls.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = urls[0];
            logger.info("Using first found downloadurl: " + dllink);
        }
        handleDLMultihoster(account, link, dllink);
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        final String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        } else {
            return null;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.br = newBrowser();
        final AccountInfo ai = new AccountInfo();
        loginWebsite(account, true);
        if (br.getURL() == null || !br.getURL().contains("/profile.html")) {
            br.getPage(WEBSITE_BASE + "/profile.html");
        }
        /* 2020-10-20: I wasn't able to determine any kind of account type ... */
        account.setType(AccountType.PREMIUM);
        ai.setStatus("Premium account");
        final String trafficLeft = br.getRegex("Pozostały transfer\\s*</div>\\s*<div class\\s*=\\s*\"data-value\"\\s*>\\s*([0-9,\\.TKGBM ]+)\\s*</div>").getMatch(0);
        if (trafficLeft != null) {
            ai.setTrafficLeft(SizeFormatter.getSize(trafficLeft));
        }
        // final String premiumDaysStr = br.getRegex("VIP\\sexpires\\sin\\s*(\\d+)\\s*<small").getMatch(0);
        // String trafficleftStr = br.getRegex("</small>((<b>(\\d+(\\.|)\\d{1,2}
        // [A-Za-z]+)</b>)|<strong>([A-Za-zé]+)</strong>)").getMatch(0);
        // if (premiumDaysStr == null) {
        // /* Free or plugin failure */
        // /*
        // * account.setType(AccountType.FREE); ai.setTrafficMax("10 GB"); ai.setStatus("Free Account");
        // * account.setMaxSimultanDownloads(account_FREE_maxdownloads); account.setValid(true);
        // */
        // // 2020.03.27 : phg : Free Accounts are not allowed for this plugin
        // throw new PluginException(LinkStatus.ERROR_PREMIUM, "Plugin for premium accounts only");
        // } else {
        // /* Premium */
        // account.setType(AccountType.PREMIUM);
        // ai.setStatus("Premium account");
        // account.setMaxSimultanDownloads(account_PREMIUM_maxdownloads);
        // ai.setValidUntil(System.currentTimeMillis() + Long.parseLong(premiumDaysStr) * 24 * 60 * 60 * 1000l, this.br);
        // }
        // if (trafficleftStr == null) {
        // /* Downloads are not possible if the traffic has not be retrieved */
        // ai.setTrafficLeft(0);
        // } else if (trafficleftStr.contains("Unlimited")) {
        // ai.setUnlimitedTraffic();
        // } else {
        // ai.setTrafficLeft(SizeFormatter.getSize(trafficleftStr));
        // }
        /*
         * Get list of supported hosts.
         */
        br.getPage("/addfiles.html");
        final String[] hosts = br.getRegex("<div class=\"col-1-6 host-item\"><img src=\"https?://[^\"]+/img/server/([^\"]+)\\.png\" />").getColumn(0);
        ai.setMultiHostSupport(this, Arrays.asList(hosts));
        account.setConcurrentUsePossible(true);
        return ai;
    }

    private void loginWebsite(final Account account, final boolean validateCookies) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Trying to login via cookies");
                    br.setCookies(WEBSITE_BASE, cookies);
                    if (!validateCookies) {
                        logger.info("Trust cookies without check");
                        return;
                    }
                    br.getPage(WEBSITE_BASE + "/profile.html");
                    if (this.isLoggedIN()) {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(br.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                    }
                }
                logger.info("Performing full login");
                br.getPage(WEBSITE_BASE + "/index.html");
                final Form loginform = br.getFormbyKey("password");
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("login", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                loginform.put("remember", "ON");
                /* 2021-09-20: reCaptchaV2 invisible required. */
                if (loginform.hasInputFieldByName("recaptcha_response")) {
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    loginform.put("recaptcha_response", Encoding.urlEncode(recaptchaV2Response));
                }
                br.submitForm(loginform);
                if (!isLoggedIN()) {
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

    private boolean isLoggedIN() throws PluginException {
        return br.containsHTML("/logout\\.html");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}