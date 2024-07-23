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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class DatoidCz extends PluginForHost {
    public DatoidCz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://datoid.cz/cenik");
        // Prevents server errors
        this.setStartIntervall(2 * 1000);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    @Override
    public String getAGBLink() {
        return "http://datoid.cz/kontakty";
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "datoid.cz", "datoid.sk", "datoid.com" });
        ret.add(new String[] { "pornoid.cz", "pornoid.sk" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([A-Za-z0-9]{2,})(/([^/]+))?");
        }
        return ret.toArray(new String[0]);
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("datoid.sk/", "datoid.cz/"));
    }

    private static final String API_BASE = "https://api.datoid.cz/v1";
    /* 2022-06-08: API doesn't work anymore due to ERR_SSL_VERSION_OR_CIPHER_MISMATCH */
    private final boolean       USE_API  = true;

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        if (USE_API) {
            return this.requestFileInformationAPI(link, account);
        } else {
            return this.requestFileInformationWebsite(link, account);
        }
    }

    private void setWeakFilename(final DownloadLink link) {
        if (!link.isNameSet()) {
            final String fid = this.getFID(link);
            final String filename_url = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(2);
            if (filename_url != null) {
                link.setName(filename_url);
            } else {
                link.setName(fid);
            }
        }
    }

    public AvailableStatus requestFileInformationAPI(final DownloadLink link, final Account account) throws Exception {
        setWeakFilename(link);
        final String contenturl = link.getPluginPatternMatcher().replaceFirst("(?i)https://", "http://");
        final String api_param_url = prepareApiParam_URL(contenturl);
        br.getPage(API_BASE + "/get-file-details?url=" + Encoding.urlEncode(api_param_url));
        final Map<String, Object> entries = this.checkErrorsAPI(br, link, account);
        final Map<String, Object> filemap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "files/{0}");
        link.setFinalFileName(filemap.get("filename").toString());
        final Number filesize_bytes = (Number) filemap.get("filesize_bytes");
        if (filesize_bytes != null) {
            link.setVerifiedFileSize(filesize_bytes.longValue());
        }
        return AvailableStatus.TRUE;
    }

    public AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account) throws Exception {
        setWeakFilename(link);
        final String fid = this.getFID(link);
        if (account != null) {
            this.loginWebsite(account);
        }
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getURL().contains(fid)) {
            /* E.g. redirect to mainpage */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex(">\\s*Název souboru:\\s*</th>\\s*<td>([^<]+)</td>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<]+)").getMatch(0);
        }
        String filesize = br.getRegex("Velikost:\\s*</th>\\s*<td>([^<]+)</td>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("class=\"icon-size-extra\"></i>([^<]+<small>[^<]+)</small>").getMatch(0);
            if (filesize != null) {
                filesize = filesize.replace("<small>", "");
            }
        }
        if (filename != null) {
            filename = Encoding.htmlDecode(filename).trim();
            filename = filename.replaceFirst("(?i)o ke stažení \\| Datoid\\.cz$", "");
            link.setFinalFileName(filename);
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        } else {
            logger.warning("Failed to find filesize");
        }
        /* Check for non-file subpages e.g. https://datoid.cz/faq */
        if (filename == null && filesize == null && !br.containsHTML("data-code=\"" + fid)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    /* 2019-02-04: Basically a workaround as their API only accepts URLs with one of their (at least) 2 domains.F */
    private String prepareApiParam_URL(final String url_source) {
        final String curr_domain = Browser.getHost(url_source);
        return url_source.replace(curr_domain, "datoid.cz");
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownloadWebsite(link, null);
    }

    private void handleDownloadWebsite(final DownloadLink link, final Account account) throws Exception, PluginException {
        requestFileInformation(link, account);
        final String fid = getFID(link);
        if (USE_API) {
            /* Continue via website */
            requestFileInformationWebsite(link, null);
        }
        if (br.containsHTML("<div class=\"bPopup free-popup file-on-page big-file\">")) {
            throw new AccountRequiredException();
        } else if (br.containsHTML(">\\s*Soubor byl zablokován\\.")) {
            /* 2019-08-12: WTF: Log: 6759186935451 : svn.jdownloader.org/issues/87302 */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Soubor byl zablokován.", 30 * 60 * 1000l);
        }
        final String continue_url = br.getRegex("class=\"[^\"]*?btn btn-large btn-download detail-download\" href=\"(/f/[^<>\"]+)\"").getMatch(0);
        if (continue_url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(continue_url);
        /* 2019-08-05: Ignore errormessages at this stage e.g. "{"error":"Lack of credits"}" */
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("content-type", "application/json; charset=utf-8");
        br.getPage(continue_url + "?request=1&_=" + System.currentTimeMillis());
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        String dllink = (String) entries.get("download_link");
        // final String dllink = (String) entries.get("download_link_cdn");
        if (StringUtils.isEmpty(dllink)) {
            /* 2019-07-31: Seems like we always have to access that URL */
            final boolean force_popup_download = true;
            final String redirect = (String) entries.get("redirect");
            if (!StringUtils.isEmpty(redirect) || force_popup_download) {
                /* Redirect will lead to main-page and we don't want that! */
                // br.getPage(redirect);
                br.getPage("/detail/popup-download?code=" + fid + "&_=" + System.currentTimeMillis());
            }
            // TODO: Use json parser here
            if (br.containsHTML("\"error\":\"IP in use\"")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            } else if (br.containsHTML("\"No anonymous free slots\"")
                    || br.containsHTML("class=\"hidden free-slots-in-use\"") /* 2018-10-15 */) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free slots available", 5 * 60 * 1000l);
            } else if (br.containsHTML("class=\"hidden big\\-file\"")) {
                /* 2019-07-31 e.g. "<span class="hidden big-file">Soubory větší než 1 GB můžou stahovat pouze <span" */
                throw new AccountRequiredException();
            }
            // final int wait = Integer.parseInt(getJson("wait"));
            dllink = PluginJSonUtils.getJsonValue(br, "download_link");
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        /* 2019-02-04: Waittime can be skipped */
        // sleep(wait * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    /** Returns login-token. */
    private String loginAPI(final Account account) throws Exception {
        synchronized (account) {
            br.getPage(API_BASE + "/login?email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            final Map<String, Object> entries = this.checkErrorsAPI(br, null, account);
            final boolean success = ((Boolean) entries.get("success")).booleanValue();
            if (!success) {
                throw new AccountInvalidException();
            }
            final String token = entries.get("token").toString();
            if (StringUtils.isEmpty(token)) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            account.setProperty("logintoken", token);
            return token;
        }
    }

    private void loginWebsite(final Account account) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                boolean isLoggedin = false;
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    br.getPage("https://" + account.getHoster() + "/");
                    isLoggedin = isLoggedin();
                }
                if (!isLoggedin) {
                    br.getPage("https://" + account.getHoster() + "/prihlaseni");
                    final Form loginform = br.getFormbyProperty("id", "login");
                    if (loginform == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    loginform.put("username", account.getUser());
                    loginform.put("password", account.getPass());
                    br.submitForm(loginform);
                    if (!isLoggedin()) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
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

    private boolean isLoggedin() {
        return br.getCookie(br.getHost(), "login", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        final String token = loginAPI(account);
        br.getPage(API_BASE + "/get-user-details?token=" + token);
        final Map<String, Object> entries = this.checkErrorsAPI(br, null, account);
        final Object credits = entries.get("credits");
        /** 1 Credit = 1 MB */
        long trafficleft = 0;
        if (credits instanceof Number) {
            trafficleft = ((Number) credits).longValue() * 1024 * 1024;
        }
        ai.setTrafficLeft(trafficleft);
        if (trafficleft > 0) {
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
        }
        /* Allow downloads without credits, see comments in handlePremium! */
        ai.setSpecialTraffic(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /*
         * 2019-08-25: Free accounts may not have any credits (= premium traffic) but are required to e.g. download files > 1 GB. The same
         * accounts for premium accounts which do not have enough traffic left.
         */
        if (account.getType() == AccountType.FREE || link.getView().getBytesTotal() > account.getAccountInfo().getTrafficLeft()) {
            this.handleDownloadWebsite(link, account);
        } else {
            /* Premium download -> Use API */
            requestFileInformationAPI(link, account);
            final String token = account.getStringProperty("logintoken", null);
            if (token == null) {
                /* This should never happen! */
                throw new AccountInvalidException("Session expired?");
            }
            final String contenturl = link.getPluginPatternMatcher();
            final String api_param_url = prepareApiParam_URL(contenturl);
            br.getPage(API_BASE + "/get-download-link?token=" + token + "&url=" + Encoding.urlEncode(api_param_url));
            final Map<String, Object> entries = this.checkErrorsAPI(br, link, account);
            final String dllink = entries.get("download_link").toString();
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, -3);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection(true);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken file?");
            }
            dl.startDownload();
        }
    }

    protected Map<String, Object> checkErrorsAPI(final Browser br, final DownloadLink link, final Account account) throws NumberFormatException, PluginException {
        /**
         * 2019-10-31: TODO: Add support for more errorcodes e.g. downloadlimit reached, premiumonly, password protected, wrong password,
         * wrong captcha. [PW protected + captcha protected download handling is not yet implemented serverside]
         */
        final long defaultWaitAccount = 3 * 60 * 1000;
        final long defaultWaitLink = 3 * 60 * 1000;
        Map<String, Object> entries = null;
        try {
            entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        } catch (final JSonMapperException e) {
            logger.log(e);
            final String errormessage = "Invalid API response";
            if (link == null) {
                throw new AccountUnavailableException(errormessage, defaultWaitAccount);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormessage, defaultWaitLink);
            }
        }
        final String errormsg = (String) entries.get("error");
        if (errormsg == null) {
            return entries;
        }
        if (errormsg.equalsIgnoreCase("File not found") || errormsg.equalsIgnoreCase("File was blocked") || errormsg.equalsIgnoreCase("File was deleted")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (errormsg.equalsIgnoreCase("File is password protected")) {
            link.setPasswordProtected(true);
            throw new PluginException(LinkStatus.ERROR_FATAL, "Password protected files are not supported in API mode");
        } else {
            logger.info("Unknown API error: " + errormsg);
            if (link == null) {
                throw new AccountUnavailableException(errormsg, defaultWaitAccount);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormsg, defaultWaitLink);
            }
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 5;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}