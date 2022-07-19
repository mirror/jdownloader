//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.WeakHashMap;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class UfileIo extends antiDDoSForHost {
    public UfileIo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://uploadfiles.io/#packages");
    }

    @Override
    public String getAGBLink() {
        return "https://uploadfiles.io/terms";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        return "://" + this.getHost() + this.getFID(link);
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "ufile.io", "uploadfiles.io" });
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
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:\\w+\\.)?" + buildHostsPatternPart(domains) + "/([A-Za-z0-9]{2,})");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String rewriteHost(final String host) {
        return this.rewriteHost(getPluginDomains(), host);
    }

    /* Connection stuff */
    private static final boolean           FREE_RESUME                  = true;
    private static final int               FREE_MAXCHUNKS               = 1;
    private static final int               FREE_MAXDOWNLOADS            = 20;
    private static final boolean           ACCOUNT_FREE_RESUME          = true;
    private static final int               ACCOUNT_FREE_MAXCHUNKS       = 1;
    private static final int               ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    private static final boolean           ACCOUNT_PREMIUM_RESUME       = true;
    private static final int               ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int               ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    protected WeakHashMap<Request, String> correctedBrowserRequestMap   = new WeakHashMap<Request, String>();

    /** Traits used to cleanup html of our basic browser object and put it into correctedBR. */
    public ArrayList<String> getCleanupHTMLRegexes() {
        final ArrayList<String> regexStuff = new ArrayList<String>();
        // remove custom rules first!!! As html can change because of generic cleanup rules.
        /* generic cleanup */
        regexStuff.add("<\\!(\\-\\-.*?\\-\\-)>");
        regexStuff.add("(display: ?none;\">.*?</div>)");
        regexStuff.add("(visibility:hidden>.*?<)");
        return regexStuff;
    }

    protected String replaceCorrectBR(Browser br, String pattern, String target) {
        /* Do not e.g. remove captcha forms from html! */
        if (StringUtils.containsIgnoreCase(pattern, "none") && (containsHCaptcha(target) || containsRecaptchaV2Class(target))) {
            return null;
        } else {
            return "";
        }
    }

    @Override
    public void clean() {
        try {
            super.clean();
        } finally {
            synchronized (correctedBrowserRequestMap) {
                correctedBrowserRequestMap.clear();
            }
        }
    }

    /** Removes HTML code which could break the plugin and puts it into correctedBR. */
    protected String correctBR(final Browser br) {
        synchronized (correctedBrowserRequestMap) {
            final Request request = br.getRequest();
            String correctedBR = correctedBrowserRequestMap.get(request);
            if (correctedBR == null) {
                correctedBR = br.toString();
                final ArrayList<String> regexStuff = getCleanupHTMLRegexes();
                // remove custom rules first!!! As html can change because of generic cleanup rules.
                /* generic cleanup */
                boolean modified = false;
                for (final String aRegex : regexStuff) {
                    final String results[] = new Regex(correctedBR, aRegex).getColumn(0);
                    if (results != null) {
                        for (final String result : results) {
                            final String replace = replaceCorrectBR(br, aRegex, result);
                            if (replace != null) {
                                correctedBR = correctedBR.replace(result, replace);
                                modified = true;
                            }
                        }
                    }
                }
                if (modified && request != null && request.isRequested()) {
                    correctedBrowserRequestMap.put(request, correctedBR);
                } else {
                    correctedBrowserRequestMap.remove(request);
                }
            }
            return correctedBR;
        }
    }

    protected String getCorrectBR(Browser br) {
        synchronized (correctedBrowserRequestMap) {
            final String ret = correctedBrowserRequestMap.get(br.getRequest());
            if (ret != null) {
                return ret;
            } else {
                return br.toString();
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (!link.isNameSet()) {
            link.setName(this.getFID(link));
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">\\s*File no longer available|>\\s*That file has now been permanently removed")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("class=\"file-name\">([^<>\"]+)<").getMatch(0);
        final String filesize = br.getRegex("File Size\\s*:([^<>\"]+)").getMatch(0);
        if (!StringUtils.isEmpty(filename)) {
            filename = Encoding.htmlDecode(filename).trim();
            /* 2020-07-27: Set final filename here as contentDisposition filenames are sometimes crippled. */
            link.setFinalFileName(filename);
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        if (filename == null && filesize == null && !br.containsHTML("class=\"download\"")) {
            /* 2020-12-10: E.g. https://ufile.io/about */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        handleDownload(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void handleDownload(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            if (br.containsHTML("(?i)>\\s*Premium Access Only")) {
                throw new AccountRequiredException();
            }
            final String fileID = this.getFID(link);
            String csrftest = br.getRegex("name=\"csrf_test_name\" value=\"([a-f0-9]+)\"").getMatch(0);
            if (csrftest == null) {
                csrftest = br.getCookie(br.getURL(), "csrf_cookie_name");
            }
            if (csrftest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* 2020-01-27: They've added reCaptchaV2 (invisible) */
            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
            String postData = "csrf_test_name=" + csrftest + "&slug=" + fileID + "&token=" + Encoding.urlEncode(recaptchaV2Response);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            postPage("/ajax/generate_download/", postData);
            if (!br.toString().startsWith("\"http")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = PluginJSonUtils.unescape(br.toString().replace("\"", ""));
            if (StringUtils.isEmpty(dllink)) {
                logger.warning("Failed to find final downloadurl");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (IOException ignore) {
                logger.log(ignore);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        final String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = openAntiDDoSRequestConnection(br2, br2.createHeadRequest(dllink));
                if (!looksLikeDownloadableContent(con)) {
                    link.setProperty(property, Property.NULL);
                } else {
                    return dllink;
                }
            } catch (final Exception e) {
                logger.log(e);
                link.setProperty(property, Property.NULL);
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return null;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    if (!force && System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 5 * 60 * 1000l) {
                        logger.info("Trust cookies without check");
                    }
                    getPage("https://" + this.getHost() + "/dashboard");
                    if (isLoggedIN()) {
                        logger.info("Cookie login successful");
                        /* Save new cookie timestamp */
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearAll();
                    }
                }
                logger.info("Performing full login");
                getPage("https://" + this.getHost() + "/login");
                final Form loginform = br.getFormbyKey("password");
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("email", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                if (loginform.containsHTML("g-recaptcha")) {
                    final DownloadLink dlinkbefore = this.getDownloadLink();
                    try {
                        final DownloadLink dl_dummy;
                        if (dlinkbefore != null) {
                            dl_dummy = dlinkbefore;
                        } else {
                            dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
                            this.setDownloadLink(dl_dummy);
                        }
                        final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br);
                        final String recaptchaV2Response = rc2.getToken();
                        loginform.put("g-recaptcha-response", recaptchaV2Response);
                    } finally {
                        this.setDownloadLink(dlinkbefore);
                    }
                }
                this.submitForm(loginform);
                if (!isLoggedIN()) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedIN() {
        return br.containsHTML("/logout");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        if (br.getRequest() == null || !br.getURL().contains("/dashboard")) {
            this.getPage("/dashboard");
        }
        ai.setUnlimitedTraffic();
        br.getRequest().setHtmlCode(correctBR(br));
        if (br.containsHTML("class=\"label\">Free Account|>\\s*As a free user, you have \\d+")) {
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
            ai.setStatus("Registered (free) user");
        } else {
            /* 2019-01-29: TODO */
            // final String expire = br.getRegex("").getMatch(0);
            // if (expire == null) {
            // if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            // throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort oder nicht unterstützter Account
            // Typ!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein
            // Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            // } else {
            // throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or unsupported account type!\r\nQuick
            // help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters,
            // change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            // }
            // } else {
            // ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", Locale.ENGLISH));
            // }
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium account");
            this.getPage("/dashboard/settings/billing");
            final Regex expireinfo = br.getRegex("Your next billing date is (\\d+)[a-z]* ([A-Za-z]+ \\d{4})<");
            final String expireDay = expireinfo.getMatch(0);
            final String expireRest = expireinfo.getMatch(1);
            if (expireDay != null && expireRest != null) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDay + " " + expireRest, "dd MMM yyyy", Locale.ENGLISH));
            }
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        getPage(link.getPluginPatternMatcher());
        if (account.getType() == AccountType.FREE) {
            handleDownload(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            /* 2020-06-15: WTF premium users will have to enter captchas too */
            handleDownload(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "premium_directlink");
        }
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (acc.getType() == AccountType.FREE) {
            /* Free accounts can have captchas */
            return true;
        }
        /* Premium accounts do not have captchas */
        return false;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}