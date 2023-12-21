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
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "zapisz.se" }, urls = { "https?://zapisz\\.se/files/(\\d+)/([^/]+)?" })
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
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getAGBLink() {
        return WEBSITE_BASE + "/terms.html";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return "zapisz.se://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            return false;
        } else {
            mhm.runCheck(account, link);
            return true;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new AccountRequiredException();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.loginWebsite(account, false);
        // looks like those files cannot be resumed!?
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), resume, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (br.getURL().matches("(?i)https?://zapisz\\.se/?")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown download error occured");
            }
        }
        dl.startDownload();
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        this.loginWebsite(account, false);
        final String directlinkproperty = this.getHost() + "directlink";
        final String storedDirecturl = link.getStringProperty(directlinkproperty);
        String dllink = null;
        if (storedDirecturl != null) {
            logger.info("Re-using stored directurl: " + storedDirecturl);
            dllink = storedDirecturl;
        } else {
            logger.info("Generating fresh directurl");
            br.getPage(WEBSITE_BASE + "/addfiles.html");
            final Form dlform = br.getFormbyKey("list");
            if (dlform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // Only https URLs are downloaded from zapisz.se, http URLs are forwarded to the normal hoster
            dlform.put("list", Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this).replaceFirst("(?i)http://", "https://")));
            // k2s.cc Captcha fields do not need to be filled in
            dlform.put("k2s", "");
            dlform.put("k2skey", "");
            // An invisible ReCaptcha is now required for the form
            // Because the next part is blocking, it could happen that the captcha becomes invalid, since it has a one minute timeout
            // However, this way several captchas can be solved at once.
            final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br);
            final int reCaptchaV2Timeout = rc2.getSolutionTimeout();
            final long timestampBeforeCaptchaSolving = System.currentTimeMillis();
            final String recaptchaV2Response = rc2.getToken();
            dlform.put("recaptcha_response", Encoding.urlEncode(recaptchaV2Response));
            synchronized (account) {
                if (isAbort()) {
                    throw new InterruptedException();
                }
                final long passedTime = System.currentTimeMillis() - timestampBeforeCaptchaSolving;
                if (passedTime >= reCaptchaV2Timeout) {
                    /* 2023-01-09: @c0d3d3v this should never happen as the obtained token is used immediately here... */
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                br.submitForm(dlform);
                // Check if success message can be found: "1 link has been processed. To download files, go to the Your Files tab."
                final Boolean successNoteFound = br.getRegex("<div class=\"note-info note-success\">([^<]+)</div>").count() >= 1;
                if (!successNoteFound) {
                    // For unknown reasons zapisz.se sometimes does not add links to the queue
                    // For Rapidgator links, zapisz.se has unknown but noticeable rate limitations
                    // For Nitroflare and k2s.cc this may mean that an additional captcha has to be solved
                    if (link.getHost().equalsIgnoreCase("nitroflare.com")) {
                        // Solve Nitroflare captcha to show that we are not robots
                        // At the time nitroflareCaptchaURL is a constant value:
                        // https://nitroflare.com/api/v2/solveCaptcha?user=user@domain.tld
                        final String nitroflareCaptchaURL = br.getRegex("<a id=\"nitroflareHref\" href=\"([^\"]+)\"").getMatch(0);
                        br.getPage(nitroflareCaptchaURL);
                        final String nitroflareRecaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                        final UrlQuery query = new UrlQuery();
                        query.add("response", Encoding.urlEncode(nitroflareRecaptchaV2Response));
                        br.postPage(nitroflareCaptchaURL, query);
                        if (!br.getRequest().getHtmlCode().equalsIgnoreCase("passed")) {
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        } else {
                            // Looks like captcha was solved successfully
                            br.getPage(nitroflareCaptchaURL + "&solved=1");
                        }
                    }
                    mhm.handleErrorGeneric(account, link, "Failed to generate downloadurl", 20, 3 * 60 * 1000l);
                }
                br.getPage("/update.php?ie=0." + System.currentTimeMillis() + "&u=1&lastupdate=0");
            }
            final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
            if (urls == null || urls.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = urls[0];
            logger.info("Using first found downloadurl: " + dllink);
        }
        if (StringUtils.isEmpty(dllink)) {
            /* This should never happen! */
            mhm.handleErrorGeneric(account, link, "Failed to find final downloadurl", 5, 3 * 60 * 1000l);
        }
        try {
            br.getHeaders().put("Referer", "https://zapisz.se/files.html");
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                mhm.handleErrorGeneric(account, link, "Unknown download error", 20, 5 * 60 * 1000l);
            }
            /* Store directurl to re-use it later */
            link.setProperty(directlinkproperty, dllink);
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(directlinkproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired", e);
            } else {
                throw e;
            }
        }
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        loginWebsite(account, true);
        if (br.getURL() == null || !br.getURL().contains("/profile.html")) {
            br.getPage(WEBSITE_BASE + "/profile.html");
        }
        /* 2020-10-20: I wasn't able to determine any kind of account type ... */
        account.setType(AccountType.PREMIUM);
        final String trafficLeft = br.getRegex("Pozostały transfer\\s*</div>\\s*<div class\\s*=\\s*\"data-value\"\\s*>\\s*([0-9,\\.TKGBM ]+)\\s*</div>").getMatch(0);
        if (trafficLeft != null) {
            ai.setTrafficLeft(SizeFormatter.getSize(trafficLeft));
        } else {
            logger.warning("Failed to find trafficLeft value");
        }
        /*
         * Get list of supported hosts.
         */
        br.getPage("/addfiles.html");
        final String[] hosts = br.getRegex("<div class=\"col-1-6 host-item\"><img src=\"https?://[^\"]+/img/server/([^\"]+)\\.png\" />").getColumn(0);
        if (hosts == null || hosts.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        ai.setMultiHostSupport(this, Arrays.asList(hosts));
        account.setConcurrentUsePossible(true);
        return ai;
    }

    private void loginWebsite(final Account account, final boolean validateCookies) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            try {
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Trying to login via cookies");
                    br.setCookies(WEBSITE_BASE, cookies);
                    if (!validateCookies) {
                        /* Do not validate cookies */
                        return;
                    }
                    br.getPage(WEBSITE_BASE + "/profile.html");
                    if (isLoggedIN(br)) {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(br.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(null);
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
                if (!isLoggedIN(br)) {
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
        return br.containsHTML("/logout\\.html");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}