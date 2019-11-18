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

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "nexusmods.com" }, urls = { "https?://(?:www\\.)?nexusmods\\.com+/Core/Libs/Common/Widgets/DownloadPopUp\\?id=(\\d+).+" })
public class NexusmodsCom extends antiDDoSForHost {
    public NexusmodsCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.nexusmods.com/signup");
    }

    @Override
    public String getAGBLink() {
        return "https://help.nexusmods.com/article/18-terms-of-service";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = true;
    private static final int     FREE_MAXCHUNKS               = 0;
    private static final int     FREE_MAXDOWNLOADS            = 20;
    private static final boolean ACCOUNT_FREE_RESUME          = true;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 0;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    public static final String   API_BASE                     = "https://api.nexusmods.com/v1";
    private String               dllink;
    private boolean              loginRequired;

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    public static Browser prepBrAPI(final Browser br, final Account account) throws PluginException {
        br.getHeaders().put("User-Agent", "JDownloader");
        final String apikey = getApikey(account);
        if (apikey == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("apikey", apikey);
        return br;
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    public boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("No files have been uploaded yet|>File not found<|>Not found<|/noimage-1.png");
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        final String url = link.getPluginPatternMatcher();
        if (StringUtils.contains(url, "nmm=1")) {
            link.setPluginPatternMatcher(url.replace("nmm=1", "nmm=0"));
        }
    }

    public boolean isLoginRequired(final Browser br) {
        if (br.containsHTML("<h1>Error</h1>") && br.containsHTML("<h2>Adult-only content</h2>")) {
            // adult only content.
            return true;
        } else if (br.containsHTML("You need to be a member and logged in to download files larger")) {
            // large files
            return true;
        } else if (br.containsHTML(">Please login or signup to download this file<")) {
            return true;
        } else {
            return false;
        }
    }

    /** URLs added <= rev. 41547 are missing properties which are required to do download & linkcheck via API! */
    private boolean linkIsAPICompatible(final DownloadLink link) {
        final String game_domain_name = link.getStringProperty("game_domain_name", null);
        final String mod_id = link.getStringProperty("mod_id", null);
        if (game_domain_name != null && mod_id != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account acc = AccountController.getInstance().getValidAccount(this.getHost());
        final String apikey = getApikey(acc);
        if (acc != null && apikey != null && linkIsAPICompatible(link)) {
            this.prepBrAPI(br, acc);
            return requestFileInformationAPI(link);
        } else {
            return requestFileInformationWebsite(link);
        }
    }

    private AvailableStatus requestFileInformationWebsite(final DownloadLink link) throws Exception {
        link.setMimeHint(CompiledFiletypeFilter.ArchiveExtensions.ZIP);
        final String fid = getFID(link.getPluginPatternMatcher());
        getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        {
            String loadBox = br.getRegex("loadBox\\('(https?://.*?)'").getMatch(0);
            if (loadBox != null) {
                getPage(loadBox);
                loadBox = br.getRegex("loadBox\\('(https?://.*?skipdonate)'").getMatch(0);
                if (loadBox != null) {
                    getPage(loadBox);
                }
            }
        }
        loginRequired = isLoginRequired(br);
        dllink = br.getRegex("window\\.location\\.href\\s*=\\s*\"(http[^<>\"]+)\";").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(https?://filedelivery\\.nexusmods\\.com/[^<>\"]+)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(https?://(?:www\\.)?nexusmods\\.com/[^<>\"]*Libs/Common/Managers/Downloads\\?Download[^<>\"]+)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("id\\s*=\\s*\"dl_link\"\\s*value\\s*=\\s*\"(https?://[^<>\"]*?)\"").getMatch(0);
                    if (dllink == null) {
                        dllink = br.getRegex("data-link\\s*=\\s*\"(https?://(?:premium-files|fs-[a-z0-9]+)\\.(?:nexusmods|nexus-cdn)\\.com/[^<>\"]*?)\"").getMatch(0);
                    }
                }
            }
        }
        String filename = br.getRegex("filedelivery\\.nexusmods\\.com/\\d+/([^<>\"]+)\\?fid=").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("data-link\\s*=\\s*\"https?://[^<>\"]+/files/\\d+/([^<>\"/]+)\\?").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("data-link\\s*=\\s*\"https?://[^<>\"]+/\\d+/\\d+/([^<>\"/]+)\\?").getMatch(0);
                if (filename == null && dllink != null) {
                    filename = getFileNameFromURL(new URL(dllink));
                }
            }
            if (filename == null && !link.isNameSet()) {
                filename = fid;
            }
        }
        if (filename != null) {
            filename = Encoding.htmlDecode(filename).trim();
            link.setName(filename);
        }
        return AvailableStatus.TRUE;
    }

    public AvailableStatus requestFileInformationAPI(final DownloadLink link) throws Exception {
        link.setMimeHint(CompiledFiletypeFilter.ArchiveExtensions.ZIP);
        final String game_domain_name = link.getStringProperty("game_domain_name", null);
        final String mod_id = link.getStringProperty("mod_id", null);
        final String file_id = new Regex(link.getPluginPatternMatcher(), "\\?id=(\\d+)").getMatch(0);
        if (file_id == null || mod_id == null || game_domain_name == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        getPage(API_BASE + String.format("/games/%s/mods/%s/files/%s", game_domain_name, mod_id, file_id));
        handleErrorsAPI(br);
        // if (br.getHttpConnection().getResponseCode() == 404) {
        // /* {"error":"File ID '12345' not found"} */
        // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // }
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("");
        String filename = (String) entries.get("file_name");
        final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
        if (!StringUtils.isEmpty(filename)) {
            link.setFinalFileName(filename);
        } else {
            /* Fallback */
            filename = game_domain_name + "_" + mod_id + "_" + file_id;
            link.setName(filename);
        }
        if (filesize > 0) {
            link.setVerifiedFileSize(filesize);
        }
        final String description = (String) entries.get("description");
        if (!StringUtils.isEmpty(description) && StringUtils.isEmpty(link.getComment())) {
            link.setComment(description);
        }
        /* TODO: Add free account (error-) handling */
        // loginRequired = isLoginRequired(br);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, null, FREE_RESUME, FREE_MAXCHUNKS);
    }

    private void doFree(final DownloadLink downloadLink, final Account account, final boolean resumable, final int maxchunks) throws Exception, PluginException {
        if (dllink == null) {
            if (loginRequired) {
                if (account == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } else {
                    /*
                     * 2019-01-23: Added errorhandling but this should never happen because if an account exists we should be able to
                     * download!
                     */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Login failure");
                }
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
        }
        dl.startDownload();
    }

    public String getFID(final String dlurl) {
        return new Regex(dlurl, "id=(\\d+)").getMatch(0);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    public void loginWebsite(final Account account) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                boolean loggedIN = false;
                if (cookies != null) {
                    br.setCookies(account.getHoster(), cookies);
                    getPage("https://www." + account.getHoster());
                    if (!isLoggedinCookies()) {
                        logger.info("Existing login invalid: Full login required!");
                        br.clearCookies(getHost());
                    } else {
                        loggedIN = true;
                    }
                }
                if (!loggedIN) {
                    getPage("https://www." + account.getHoster() + "/Core/Libs/Common/Widgets/LoginPopUp?url=%2F%2Fwww.nexusmods.com%2F");
                    final PostRequest request = new PostRequest("https://www.nexusmods.com/Sessions?TryNewLogin");
                    request.put("username", Encoding.urlEncode(account.getUser()));
                    request.put("password", Encoding.urlEncode(account.getPass()));
                    request.put("uri", "%2F%2Fwww.nexusmods.com%2F");
                    final DownloadLink original = this.getDownloadLink();
                    if (original == null) {
                        this.setDownloadLink(new DownloadLink(this, "Account", getHost(), "http://" + br.getRequest().getURL().getHost(), true));
                    }
                    try {
                        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6LfTA2EUAAAAAIyUT3sr2W8qKUV1IauZl-CduEix").getToken();
                        if (recaptchaV2Response == null) {
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        }
                        request.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    } finally {
                        if (original == null) {
                            this.setDownloadLink(null);
                        }
                    }
                    request.setContentType("application/x-www-form-urlencoded");
                    sendRequest(request);
                    if (!isLoggedinCookies()) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(account.getHoster()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedinCookies() {
        return br.getCookie(br.getURL(), "pass_hash", Cookies.NOTDELETEDPATTERN) != null && br.getCookie(br.getURL(), "member_id", Cookies.NOTDELETEDPATTERN) != null && br.getCookie(br.getURL(), "sid", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        String apikey = getApikey(account);
        if (apikey != null) {
            return fetchAccountInfoAPI(account);
        }
        final AccountInfo ai = new AccountInfo();
        loginWebsite(account);
        getPage("/users/myaccount?tab=api%20access");
        /* Try to find apikey - prefer API */
        /* TODO: Maybe generate apikey is it is not yet available */
        /*
         * TODO: In case we get an 'official app' linked on their site: Grab the apikey which has been generated specifically for
         * JDownloader!
         */
        apikey = br.getRegex("id=\"personal_key\"[^>]*>([^<>\"]+)<").getMatch(0);
        if (apikey != null) {
            /* TODO: Consider removing original logindata once we found an apikey for safety reasons! */
            logger.info("Found apikey");
            account.setProperty("apikey", apikey);
            return fetchAccountInfoAPI(account);
        } else {
            logger.info("Failed to find apikey - continuing via website");
            getPage("/users/myaccount");
            if (StringUtils.equalsIgnoreCase(br.getRegex("\"premium-desc\">\\s*(.*?)\\s*<").getMatch(0), "Inactive")) {
                account.setType(AccountType.FREE);
                account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
                account.setConcurrentUsePossible(false);
            } else {
                account.setType(AccountType.PREMIUM);
                account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
                account.setConcurrentUsePossible(true);
            }
            return ai;
        }
    }

    private AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        prepBrAPI(br, account);
        ai.setUnlimitedTraffic();
        getPage(API_BASE + "/users/validate.json");
        handleErrorsAPI(br);
        final String is_premium = PluginJSonUtils.getJson(br, "is_premium");
        if ("true".equalsIgnoreCase(is_premium)) {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
        } else {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
        }
        return ai;
    }

    public static void handleErrorsAPI(final Browser br) throws PluginException {
        if (br.getHttpConnection().getResponseCode() == 401) {
            /* {"message":"Please provide a valid API Key"} */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if (br.getHttpConnection().getResponseCode() == 403) {
            /*
             * According to API documentation, this may happen if we try to download a file via API with a free account (downloads are only
             * possible via website!)
             */
            throw new AccountRequiredException();
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            /* {"error":"File ID '12345' not found"} */
            /* {"code":404,"message":"No Game Found: xskyrimspecialedition"} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    public static String getApikey(final Account account) {
        if (account == null) {
            return null;
        }
        return account.getStringProperty("apikey");
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /*
         * TODO: Consider saving- and re-using direct downloadurls. Consider that premium users do not have any traffic limits so re-using
         * generated downloadurls does not bring any huge benefits. Also when re-using generated downloadlinks consider that they do have
         * different download mirrors/location and these are currently randomly selected on downloadstart!
         */
        if (getApikey(account) != null && linkIsAPICompatible(link)) {
            prepBrAPI(br, account);
            /* We do not have to perform an extra onlinecheck - if the file is offline, the download request will return 404. */
            // requestFileInformationAPI(link);
            final String game_domain_name = link.getStringProperty("game_domain_name", null);
            final String mod_id = link.getStringProperty("mod_id", null);
            final String file_id = new Regex(link.getPluginPatternMatcher(), "\\?id=(\\d+)").getMatch(0);
            if (file_id == null || mod_id == null || game_domain_name == null) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            getPage(API_BASE + String.format("/games/%s/mods/%s/files/%s/download_link.json", game_domain_name, mod_id, file_id));
            handleErrorsAPI(br);
            LinkedHashMap<String, Object> entries = null;
            final ArrayList<Object> ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            if (ressourcelist == null || ressourcelist.isEmpty()) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unable to find downloadlink");
            }
            /* TODO: Maybe add mirror (location) selection */
            final int randomMirrorSelector = new Random().nextInt(ressourcelist.size());
            final Object randomMirrorO = ressourcelist.get(randomMirrorSelector);
            entries = (LinkedHashMap<String, Object>) randomMirrorO;
            final String mirrorName = (String) entries.get("name");
            this.dllink = (String) entries.get("URI");
            if (StringUtils.isEmpty(this.dllink)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unable to find downloadlink for mirror: " + mirrorName);
            }
            logger.info("Selected random mirror: " + mirrorName);
        } else {
            /* Important! Login before requestFileInformation! */
            loginWebsite(account);
            requestFileInformation(link);
        }
        /* Free- and premium download handling is the same. */
        doFree(link, account, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS);
    }

    public void getPage(Browser ibr, String page) throws Exception {
        super.getPage(ibr, page);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}