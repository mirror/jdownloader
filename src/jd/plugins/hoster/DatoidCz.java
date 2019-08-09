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

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "datoid.cz", "pornoid.cz" }, urls = { "https?://(?:www\\.)?datoid\\.(?:cz|sk)/([A-Za-z0-9]+)(/([^/]+))?", "https?://(?:www\\.)?pornoid\\.(?:cz|sk)/([A-Za-z0-9]+)(/([^/]+))?" })
public class DatoidCz extends antiDDoSForHost {
    public DatoidCz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://datoid.cz/cenik");
        // Prevents server errors
        this.setStartIntervall(2 * 1000);
    }

    @Override
    public String getAGBLink() {
        return "http://datoid.cz/kontakty";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("datoid.sk/", "datoid.cz/"));
    }

    private static final String API_BASE = "https://api.datoid.cz/v1";

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
        final String linkid = this.getFID(link);
        final String filename_url = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(2);
        boolean set_final_filename = true;
        String filename = null;
        String filesize = null;
        String downloadURL = link.getPluginPatternMatcher();
        String api_param_url = prepareApiParam_URL(downloadURL);
        boolean api_failed = false;
        final boolean trust_API = true;
        getPage(API_BASE + "/get-file-details?url=" + Encoding.urlEncode(api_param_url));
        if (fileIsOfflineAPI() && StringUtils.startsWithCaseInsensitive(downloadURL, "https://")) {
            downloadURL = downloadURL.replace("https://", "http://");
            api_param_url = prepareApiParam_URL(downloadURL);
            getPage(API_BASE + "/get-file-details?url=" + Encoding.urlEncode(api_param_url));
            if (fileIsOfflineAPI() && !trust_API) {
                /* Double-check - API fallback */
                api_failed = true;
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(true);
                getPage(brc, downloadURL);
                if (!brc.getURL().contains(linkid) || brc.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                filename = brc.getRegex("class=\"filename\">([^<>\"]+)<").getMatch(0);
                filesize = brc.getRegex("class=\"icon-size\"></i>([^<>\"]+)<").getMatch(0);
                if (filename != null) {
                    link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
                    if (filesize != null) {
                        link.setDownloadSize(SizeFormatter.getSize(filesize));
                    }
                }
            }
        }
        if (!api_failed) {
            if (fileIsOfflineAPI()) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML("\"error\":\"File is password protected\"")) {
                logger.info("Password protected links are not yet supported (via API)!");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = PluginJSonUtils.getJsonValue(br, "filename");
            filesize = PluginJSonUtils.getJsonValue(br, "filesize_bytes");
        }
        if (StringUtils.isEmpty(filename)) {
            /* Fallback */
            filename = filename_url;
            if (StringUtils.isEmpty(filename)) {
                /* Final fallback */
                filename = linkid;
            }
        }
        if (StringUtils.isEmpty(filename)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (set_final_filename) {
            link.setFinalFileName(filename);
        } else {
            link.setName(filename);
        }
        if (filesize != null && filesize.matches("\\d+")) {
            /* Filesize via API */
            link.setDownloadSize(Long.parseLong(filesize));
        } else if (filesize != null) {
            /* Filesize via website */
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    /* 2019-02-04: Basically a workaround as their API only accepts URLs with one of their (at least) 2 domains.F */
    private String prepareApiParam_URL(final String url_source) {
        final String curr_domain = Browser.getHost(url_source);
        return url_source.replace(curr_domain, "datoid.cz");
    }

    private boolean fileIsOfflineAPI() {
        return br.containsHTML("\"error\":\"(File not found|File was blocked|File was deleted)\"");
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        final String fid = getFID(link);
        br.setFollowRedirects(true);
        getPage(link.getPluginPatternMatcher());
        if (br.containsHTML("<div class=\"bPopup free-popup file-on-page big-file\">")) {
            logger.info("Only downloadable by Premium Account holders");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        final String continue_url = br.getRegex("class=\"[^\"]*?btn btn-large btn-download detail-download\" href=\"(/f/[^<>\"]+)\"").getMatch(0);
        if (continue_url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        getPage(continue_url);
        /* 2019-08-05: Ignore errormessages at this stage e.g. "{"error":"Lack of credits"}" */
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("content-type", "application/json; charset=utf-8");
        getPage(continue_url + "?request=1&_=" + System.currentTimeMillis());
        String dllink = PluginJSonUtils.getJsonValue(br, "download_link");
        if (StringUtils.isEmpty(dllink)) {
            /* 2019-07-31: Seems like we always have to access that URL */
            final boolean force_popup_download = true;
            final String redirect = PluginJSonUtils.getJson(br, "redirect");
            if (!StringUtils.isEmpty(redirect) || force_popup_download) {
                /* Redirect will lead to main-page and we don't want that! */
                // br.getPage(redirect);
                getPage("/detail/popup-download?code=" + fid + "&_=" + System.currentTimeMillis());
            }
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
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String loginAPI(final Account account) throws Exception {
        br.setFollowRedirects(false);
        getPage(API_BASE + "/login?email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        try {
            if (br.containsHTML("\\{\"success\":false\\}")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nUngültiger Benutzername oder ungültiges Passwort!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            final String token = PluginJSonUtils.getJsonValue(br, "token");
            if (token == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            account.setProperty("logintoken", token);
            return token;
        } catch (PluginException e) {
            account.removeProperty("logintoken");
            throw e;
        }
    }

    private void loginWebsite(final Account account) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                boolean isLoggedin = false;
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    getPage("https://" + account.getHoster() + "/");
                    isLoggedin = isLoggedin();
                }
                if (!isLoggedin) {
                    getPage("https://" + account.getHoster() + "/prihlaseni");
                    final Form loginform = br.getFormbyProperty("id", "login");
                    if (loginform == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    loginform.put("username", account.getUser());
                    loginform.put("password", account.getPass());
                    submitForm(loginform);
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
        AccountInfo ai = new AccountInfo();
        final String token = loginAPI(account);
        getPage(API_BASE + "/get-user-details?token=" + token);
        /** 1 Credit = 1 MB */
        final String credits = PluginJSonUtils.getJsonValue(br, "credits");
        long trafficleft = 0;
        if (!StringUtils.isEmpty(credits)) {
            trafficleft = SizeFormatter.getSize(credits + " MB");
        }
        ai.setTrafficLeft(trafficleft);
        if (trafficleft > 0) {
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium Account");
        } else {
            account.setType(AccountType.FREE);
            ai.setStatus("Free Account");
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
        final boolean enforceFreeAccountDownload = account.getType() == AccountType.FREE || link.getView().getBytesTotal() > account.getAccountInfo().getTrafficLeft();
        if (enforceFreeAccountDownload) {
            loginWebsite(account);
            handleFree(link);
        } else {
            requestFileInformation(link);
            br.setFollowRedirects(false);
            final String token = account.getStringProperty("logintoken", null);
            if (token == null) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            String downloadURL = link.getPluginPatternMatcher();
            String api_param_url = prepareApiParam_URL(downloadURL);
            getPage(API_BASE + "/get-download-link?token=" + token + "&url=" + Encoding.urlEncode(api_param_url));
            if (br.containsHTML("\"error\":\"File not found\"") && StringUtils.startsWithCaseInsensitive(downloadURL, "https://")) {
                /* Workaround */
                downloadURL = downloadURL.replace("https://", "http://");
                api_param_url = prepareApiParam_URL(downloadURL);
                getPage(API_BASE + "/get-download-link?token=" + token + "&url=" + Encoding.urlEncode(api_param_url));
            }
            if (br.containsHTML("\"error\":\"Lack of credits\"")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            if (fileIsOfflineAPI()) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String dllink = PluginJSonUtils.getJsonValue(br, "download_link");
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, -3);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
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