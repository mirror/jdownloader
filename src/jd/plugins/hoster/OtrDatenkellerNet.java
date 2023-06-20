//    jDownloader - Downloadmanager
//    Copyright (C) 2011  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.storage.TypeRef;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.utils.Time;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "otr.datenkeller.net" }, urls = { "https?://otr\\.datenkeller\\.(?:at|net)/\\?(?:file|getFile)=.+" })
public class OtrDatenkellerNet extends PluginForHost {
    public static AtomicReference<String> agent = new AtomicReference<String>(null);

    private String getUserAgent() {
        while (true) {
            String agent = OtrDatenkellerNet.agent.get();
            if (agent == null) {
                OtrDatenkellerNet.agent.compareAndSet(null, RandomUserAgent.generate());
            } else {
                return agent;
            }
        }
    }

    private final String        MAINPAGE     = "http://otr.datenkeller.net";
    private final String        API_BASE_URL = "https://otr.datenkeller.net/api.php";
    private static final String APIVERSION   = "1";

    public static interface OtrDatenKellerInterface extends PluginConfigInterface {
        final String                    text_MaxWaitMinutesForTicket = "Max wait for ticket (minutes)";
        public static final TRANSLATION TRANSLATION                  = new TRANSLATION();

        public static class TRANSLATION {
            public String getMaxWaitMinutesForTicket_label() {
                return text_MaxWaitMinutesForTicket;
            }
        }

        @AboutConfig
        @DefaultIntValue(600)
        @DescriptionForConfigEntry(text_MaxWaitMinutesForTicket)
        int getMaxWaitMinutesForTicket();

        void setMaxWaitMinutesForTicket(int i);
    }

    public OtrDatenkellerNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
    }

    private String getContentURL(final DownloadLink link) {
        String url = link.getPluginPatternMatcher().replaceFirst("http://", "https://");
        url = url.replaceFirst("otr\\.datenkeller\\.at/", "otr.datenkeller.net/");
        url = url.replace("getFile", "file").replaceAll("\\&referer=otrkeyfinder\\&lang=[a-z]+", "");
        return url;
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        checkLinks(new DownloadLink[] { link });
        if (!link.isAvailabilityStatusChecked()) {
            return AvailableStatus.UNCHECKED;
        } else if (!link.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            return AvailableStatus.TRUE;
        }
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            api_prepBrowser(br);
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test up to 100 links at once */
                    if (index == urls.length || links.size() > 100) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                for (final DownloadLink dl : links) {
                    sb.append(Encoding.urlEncode(getFilenameFromURL(dl)));
                    sb.append("%2C");
                }
                final UrlQuery query = new UrlQuery();
                query.add("api_version", APIVERSION);
                query.add("action", "validate");
                query.add("file", sb.toString());
                br.postPage(API_BASE_URL, query);
                final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                /* If all checked files are offline, "file_ok" field does not exist. */
                final Map<String, Object> file_okMap = (Map<String, Object>) entries.get("file_ok");
                for (final DownloadLink link : links) {
                    final String filenameFromURL = getFilenameFromURL(link);
                    final Map<String, Object> fileinfomap = file_okMap != null ? (Map<String, Object>) file_okMap.get(filenameFromURL) : null;
                    if (fileinfomap == null) {
                        link.setAvailable(false);
                    } else {
                        link.setAvailable(true);
                        final Object filesizeO = fileinfomap.get("filesize");
                        if (filesizeO instanceof String) {
                            link.setVerifiedFileSize(Long.parseLong(filesizeO.toString()));
                        } else {
                            link.setVerifiedFileSize(((Number) filesizeO).longValue());
                        }
                    }
                    link.setFinalFileName(Encoding.htmlDecode(filenameFromURL));
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    public String getDllink(final Browser br) throws Exception, PluginException {
        final Regex allMatches = br.getRegex("onclick=\"startCount\\(\\d+ +, +\\d+, +\\'([^<>\"\\']+)\\', +\\'([^<>\"\\']+)\\', +\\'([^<>\"\\']+)\\'\\)");
        String firstPart = allMatches.getMatch(1);
        String secondPart = allMatches.getMatch(0);
        String thirdPart = allMatches.getMatch(2);
        if (firstPart == null || secondPart == null || thirdPart == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String dllink = "http://" + firstPart + "/" + secondPart + "/" + thirdPart;
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 5;
    }

    private final String WEBAPI_BASE = "https://waitaws.lastverteiler.net/api.php";

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        /* Use random UA again here because we do not use the same API as in linkcheck for free downloads */
        br.setFollowRedirects(true);
        br.clearCookies(null);
        br.getHeaders().put("User-Agent", getUserAgent());
        final String directlinkproperty = "free_finallink";
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            final String dlPageURL = "/?getFile=" + Encoding.urlEncode(this.getFilenameFromURL(link));
            // if (br.containsHTML("(?i)\"action needs to specified")) {
            // /**
            // * 2023-06-19: Looks like they've disabled API downloads without account(?) </br>
            // * {"status":"fail","reason":"api","message":"action needs to specified, it can be one of 'validate, login, getpremlink,
            // * getServers'"}
            // */
            // throw new AccountRequiredException();
            // }
            /* Check for limit is not needed, also their limits are based on cookies only */
            // if (br.containsHTML(">Du kannst höchstens \\d+ Download Links pro Stunde anfordern")) {
            // final String waitUntil = br.getRegex("bitte warte bis (\\d{1,2}:\\d{1,2}) zum nächsten Download").getMatch(0);
            // if (waitUntil != null) {
            // final long wtime = TimeFormatter.getMilliSeconds(waitUntil, "HH:mm", Locale.GERMANY);
            // throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wtime);
            // }
            // throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l);
            // }
            String positionStr = null;
            String site_lowSpeedLink = null;
            final Browser br2 = br.cloneBrowser();
            String api_otrUID = null;
            final String finalfilenameurlencoded = Encoding.urlEncode(this.getFilenameFromURL(link));
            final boolean alsoUseWebAPI = false;
            boolean api_otrUID_used = false;
            boolean api_failed = false;
            // final String jscounturl = br.getRegex("(https?://[^\"]+/countMe\\.js\\?\\d+)").getMatch(0);
            // br2.getPage("http://staticaws.lastverteiler.net/otrfuncs/countMe.js");
            link.getLinkStatus().setStatusText("Waiting for ticket...");
            final int userDefinedMaxWaitMinutes = PluginJsonConfig.get(OtrDatenKellerInterface.class).getMaxWaitMinutesForTicket();
            int loops = 0;
            final long timeBefore = Time.systemIndependentCurrentJVMTimeMillis();
            while (true) {
                getPage(this.br, dlPageURL);
                final String refreshSecondsStr = br.getRegex("http-equiv=\"refresh\" content=\"(\\d{1,2})").getMatch(0);
                final int refreshSeconds;
                if (refreshSecondsStr != null) {
                    refreshSeconds = Integer.parseInt(refreshSecondsStr);
                } else {
                    refreshSeconds = 20;
                }
                site_lowSpeedLink = br.getRegex("\"(\\?lowSpeed=[^<>\\'\"]+)\"").getMatch(0);
                if (site_lowSpeedLink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (looksLikeDownloadIsAvailable(br)) {
                    dllink = getDllink(br);
                    break;
                }
                if (api_otrUID == null) {
                    api_otrUID = br.getRegex("waitaws\\.lastverteiler\\.net/([^<>\"]*?)/").getMatch(0);
                }
                if (api_otrUID == null) {
                    /* Basically the same/not relevant */
                    api_otrUID = br.getCookie(br.getHost(), "otrUID", Cookies.NOTDELETEDPATTERN);
                }
                positionStr = br.getRegex("(?i)Deine Position in der Warteschlange\\s*:\\s*</td><td>~(\\d+)</td>").getMatch(0);
                if (api_otrUID != null && alsoUseWebAPI) {
                    logger.info("Newway: New way active");
                    if (!api_otrUID_used) {
                        api_otrUID_used = true;
                        logger.info("NewwayUsing free API the first time...");
                        final String api_waitaws_url = "https://waitaws.lastverteiler.net/" + api_otrUID + "/" + finalfilenameurlencoded;
                        getPage(this.br, api_waitaws_url);
                        // TODO: Remove the following two lines
                        api_postPage(this.br, WEBAPI_BASE, "action=validate&otrUID=" + api_otrUID + "&file=" + finalfilenameurlencoded);
                        if (br.containsHTML("\"status\":\"fail\",\"reason\":\"user\"")) {
                            /* One retry is usually enough if the first attempt to use the API fails! */
                            if (api_failed) {
                                /* This should never happen */
                                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "FATAL API failure", 30 * 60 * 1000l);
                            }
                            logger.info("Newway: Failed to start queue - refreshing api_otrUID");
                            br.clearCookies(null);
                            br.getPage("https://" + this.getHost() + "/");
                            br.getPage(this.getContentURL(link));
                            br.getPage(dlPageURL);
                            api_otrUID_used = false;
                            api_otrUID = null;
                            api_failed = true;
                            continue;
                        }
                        api_postPage(this.br, WEBAPI_BASE, "action=wait&status=ok&valid=ok&file=" + finalfilenameurlencoded + "&otrUID=" + api_otrUID);
                    }
                    String postData = "";
                    String[] params = br.toString().split(",");
                    if (params == null || params.length == 0) {
                        logger.warning("Failed to get API postparameters");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    sleep(refreshSeconds * 1000l, link);
                    for (final String postPair : params) {
                        final String key = new Regex(postPair, "\"([^<>\"]*?)\"").getMatch(0);
                        String value = new Regex(postPair, "\"([^<>\"]*?)\":\"([^<>\"]*?)\"").getMatch(1);
                        if (value == null) {
                            value = new Regex(postPair, "\"([^<>\"]*?)\":(null|true|false|\\d+)").getMatch(1);
                        }
                        postData += key + "=" + Encoding.urlEncode(value) + "&";
                    }
                    api_postPage(this.br, WEBAPI_BASE, postData);
                    positionStr = br.getRegex("\"wait_pos\":\"(\\d+)\"").getMatch(0);
                    dllink = br.getRegex("\"link\":\"(http:[^<>\"]*?)\"").getMatch(0);
                }
                sleep(refreshSeconds * 1000l, link);
                link.getLinkStatus().setStatusText("Warten auf Ticket...Position in der Warteschlange: " + positionStr);
                if (loops > 400 && site_lowSpeedLink != null) {
                    logger.info("Waited too long - trying to use low speed downloadlink");
                    getPage(br2, site_lowSpeedLink);
                    dllink = br2.getRegex("(?i)>\\s*Dein Download Link:<br>\\s*<a href=\"(http://[^<>\\'\"]+)\"").getMatch(0);
                    if (dllink == null) {
                        dllink = br2.getRegex("\"(http://\\d+\\.\\d+\\.\\d+\\.\\d+/low/[a-z0-9]+/[^<>\\'\"]+)\"").getMatch(0);
                    }
                    if (dllink != null) {
                        logger.info("Using lowspeed link for downloadlink: " + link.getDownloadURL());
                        break;
                    } else {
                        logger.warning("Failed to find low speed link, continuing to look for downloadticket...");
                    }
                }
                logger.info("Didn't get a ticket on try " + loops + ". Retrying...Position: " + positionStr);
                if (Time.systemIndependentCurrentJVMTimeMillis() - timeBefore > userDefinedMaxWaitMinutes * 60 * 1000) {
                    logger.info("Stopping because: Did not get a ticket");
                    break;
                }
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Didn't get a ticket", 30 * 60 * 1000l);
            }
            dllink = dllink.replace("\\", "");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        checkErrorsAfterDownloadstart();
        link.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private void checkErrorsAfterDownloadstart() throws IOException, PluginException {
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (br.containsHTML("(?i)>\\s*Maximale Anzahl an Verbindungen verbraucht")) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many connections");
            } else if (br.containsHTML("(?i)wurde wegen Missbrauch geblockt")) {
                throw new AccountInvalidException("Account wurde wegen Missbrauch geblockt");
            } else if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
            }
        }
    }

    private boolean looksLikeDownloadIsAvailable(final Browser br) {
        return br.containsHTML("onclick=\"startCount");
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                link.removeProperty(property);
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private void login(Account account, boolean force) throws Exception {
        br.setCookiesExclusive(true);
        api_prepBrowser(br);
        String apikey = getAPIKEY(account);
        boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
        if (acmatch) {
            acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
        }
        if (acmatch && !force && apikey != null) {
            /* Re-use existing apikey */
            return;
        }
        br.setFollowRedirects(false);
        br.postPage(API_BASE_URL, "api_version=" + APIVERSION + "&action=login&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        handleErrorsAPI();
        apikey = getJson(br.toString(), "apikey");
        if (apikey == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        account.setProperty("name", Encoding.urlEncode(account.getUser()));
        account.setProperty("pass", Encoding.urlEncode(account.getPass()));
        account.setProperty("apikey", apikey);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        login(account, true);
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Object expiresO = entries.get("expires");
        if (expiresO == null) {
            ai.setExpired(true);
            return ai;
        }
        if (expiresO instanceof Number) {
            ai.setValidUntil(((Number) expiresO).longValue() * 1000);
        } else {
            ai.setValidUntil(Long.parseLong(expiresO.toString()) * 1000);
        }
        account.setType(AccountType.PREMIUM);
        account.setConcurrentUsePossible(true);
        ai.setUnlimitedTraffic();
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        final UrlQuery query = new UrlQuery();
        query.add("api_version", APIVERSION);
        query.add("action", "getpremlink");
        query.add("username", Encoding.urlEncode(account.getUser()));
        query.add("password", Encoding.urlEncode(account.getPass()));
        query.add("apikey", Encoding.urlEncode(getAPIKEY(account)));
        query.add("filename", Encoding.urlEncode(getFilenameFromURL(link)));
        br.postPage(API_BASE_URL, query);
        String dllink = getJson(br.toString(), "dllink");
        if (dllink == null) {
            handleErrorsAPI();
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 1);
        checkErrorsAfterDownloadstart();
        dl.startDownload();
    }

    private Map<String, Object> checkErrorsAPI(final Browser br) throws PluginException {
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final String status = getJson("status");
        final String message = getJson("message");
        if ("fail".equals(status)) {
            /* TODO: Add support for more failure cases here */
            if (message.equalsIgnoreCase("failed to login due to wrong credentials, missing or wrong apikey")) {
                throw new AccountInvalidException();
            } else if (message.equalsIgnoreCase("failed to login due to wrong credentials or expired account")) {
                throw new AccountInvalidException();
            } else {
                throw new PluginException(LinkStatus.ERROR_FATAL, message);
            }
        }
        return entries;
    }

    /* Handles API errors */
    @Deprecated
    private void handleErrorsAPI() throws PluginException {
        final String status = getJson("status");
        final String message = getJson("message");
        if ("fail".equals(status) && message != null) {
            /* TODO: Add support for more failure cases here */
            if (message.equalsIgnoreCase("failed to login due to wrong credentials, missing or wrong apikey")) {
                /* This should never happen. */
                /* Temp disable --> We will get a new apikey next full login --> Maybe that will help */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else if (message.equalsIgnoreCase("failed to login due to wrong credentials or expired account")) {
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            logger.warning("Unknown API errorstate");
        }
    }

    private void api_prepBrowser(final Browser br) {
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
    }

    private void api_Free_prepBrowser(final Browser br) {
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        br.getHeaders().put("Accept-Charset", null);
    }

    final String getFilenameFromURL(final DownloadLink link) throws MalformedURLException {
        return UrlQuery.parse(getContentURL(link)).get("file");
    }

    private String getAPIKEY(final Account acc) {
        String apikey = acc.getStringProperty("apikey");
        if (apikey != null) {
            apikey = apikey.replace("\\", "");
        }
        return apikey;
    }

    private String getJson(final String source, final String parameter) {
        String result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?([0-9\\.]+)").getMatch(1);
        if (result == null) {
            result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?\"([^<>\"]*?)\"").getMatch(1);
        }
        return result;
    }

    private String getJson(final String parameter) {
        return getJson(this.br.toString(), parameter);
    }

    private void getPage(final Browser br, final String url) throws IOException {
        br.getPage(url);
    }

    @SuppressWarnings("unused")
    private void postPage(final Browser br, final String url, final String data) throws IOException {
        br.postPage(url, data);
    }

    private void api_postPage(final Browser br, final String url, final String data) throws IOException {
        this.api_Free_prepBrowser(br);
        br.postPage(url, data);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /*
         * Admin told us limit = 12 but when we checked it, only 7 with a large wait time in between were possible - anyways, works best
         * with only one
         */
        /* 2023-06-20: Changed limit from 1 to 5. */
        return 5;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        return false;
    }
}