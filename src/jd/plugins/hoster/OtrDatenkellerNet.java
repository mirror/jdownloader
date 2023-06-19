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
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
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

    private final String        DOWNLOADAVAILABLE = "onclick=\"startCount";
    private final String        MAINPAGE          = "http://otr.datenkeller.net";
    private final String        API_BASE_URL      = "https://otr.datenkeller.net/api.php";
    private static final String APIVERSION        = "1";
    private String              api_waitaws_url   = null;

    public static interface OtrDatenKellerInterface extends PluginConfigInterface {
        @AboutConfig
        @DefaultIntValue(10 * 60)
        @DescriptionForConfigEntry("Wait for Ticket (min)")
        int getWaitForTicket();

        void setWaitForTicket(int min);
    }

    public OtrDatenkellerNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
        this.setStartIntervall(60 * 1000l);
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
    public String rewriteHost(String host) {
        if ("otr.datenkeller.at".equals(getHost())) {
            if (host == null || "otr.datenkeller.at".equals(host)) {
                return "otr.datenkeller.net";
            }
        }
        return super.rewriteHost(host);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        checkLinks(new DownloadLink[] { link });
        if (!link.isAvailabilityStatusChecked()) {
            return AvailableStatus.UNCHECKED;
        }
        if (link.isAvailabilityStatusChecked() && !link.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            final Browser br = new Browser();
            api_prepBrowser();
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
                sb.append("api_version=" + APIVERSION + "&action=validate&file=");
                for (final DownloadLink dl : links) {
                    sb.append(Encoding.urlEncode(getFname(dl)));
                    sb.append("%2C");
                }
                br.postPage(API_BASE_URL, sb.toString());
                for (final DownloadLink dllink : links) {
                    final String current_filename = getFname(dllink);
                    final String online_source = br.getRegex("(\\{\"filesize\":\"\\d+\",\"filename\":\"" + current_filename + "\"\\})").getMatch(0);
                    if (online_source == null) {
                        dllink.setAvailable(false);
                    } else {
                        final String filesize = getJson(online_source, "filesize");
                        dllink.setAvailable(true);
                        dllink.setDownloadSize(Long.parseLong(filesize));
                    }
                    dllink.setFinalFileName(Encoding.htmlDecode(current_filename));
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

    public String getDllink() throws Exception, PluginException {
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
        return 1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        /* Use random UA again here because we do not use the same API as in linkcheck for free downloads */
        br.setFollowRedirects(true);
        br.clearCookies(null);
        br.getHeaders().put("User-Agent", getUserAgent());
        final String dlPage = getDlpage(link);
        String dllink = checkDirectLink(link, "free_finallink");
        if (dllink == null) {
            getPage(this.br, dlPage);
            // if (br.containsHTML("(?i)\"action needs to specified")) {
            // /**
            // * 2023-06-19: Looks like they've disabled API downloads without account(?) </br>
            // * {"status":"fail","reason":"api","message":"action needs to specified, it can be one of 'validate, login, getpremlink,
            // * getServers'"}
            // */
            // throw new AccountRequiredException();
            // }
            /* Not needed, also their limits are based on cookies only */
            // if (br.containsHTML(">Du kannst höchstens \\d+ Download Links pro Stunde anfordern")) {
            // final String waitUntil = br.getRegex("bitte warte bis (\\d{1,2}:\\d{1,2}) zum nächsten Download").getMatch(0);
            // if (waitUntil != null) {
            // final long wtime = TimeFormatter.getMilliSeconds(waitUntil, "HH:mm", Locale.GERMANY);
            // throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wtime);
            // }
            // throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l);
            // }
            String position;
            String site_lowSpeedLink;
            Browser br2 = br.cloneBrowser();
            String api_otrUID = null;
            final String finalfilenameurlencoded = Encoding.urlEncode(link.getFinalFileName());
            boolean api_otrUID_used = false;
            boolean api_failed = false;
            if (br.containsHTML(DOWNLOADAVAILABLE)) {
                dllink = getDllink();
            } else {
                site_lowSpeedLink = br.getRegex("\"(\\?lowSpeed=[^<>\\'\"]+)\"").getMatch(0);
                if (site_lowSpeedLink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br2.getPage("https://staticaws.lastverteiler.net/images/style.css");
                br2.getPage("https://staticaws.lastverteiler.net/otrfuncs/combo.js?r300613");
                br2.getPage("https://otr.datenkeller.net/otrfuncs/xajax_js/xajax_core.js");
                br2.getPage("https://staticaws.lastverteiler.net/otrfuncs/jquery-ui-1.8.16.custom.min.js");
                br2.getPage("https://staticaws.lastverteiler.net/otrfuncs/jquery.jmNotify.js");
                br2.getPage("https://staticaws.lastverteiler.net/otrfuncs/jquery.cookie.js");
                br2.openGetConnection("https://staticaws.lastverteiler.net/images/favicon.ico");
                br2.openGetConnection("https://otr.datenkeller.net/images/de.gif");
                br2.getPage("https://staticaws.lastverteiler.net/otrfuncs/countMe.js");
                link.getLinkStatus().setStatusText("Waiting for ticket...");
                final int minutes = PluginJsonConfig.get(OtrDatenKellerInterface.class).getWaitForTicket();
                /* Try up to 10 hours = 2250 loops */
                final int maxloops = 2250 / 10 * minutes / 60; // wait x minutes
                for (int i = 1; i <= maxloops; i++) {
                    br2 = br.cloneBrowser();
                    /* Whenever we got an otrUID the first time, we can use it for the whole process */
                    if (api_otrUID == null) {
                        api_otrUID = br.getRegex("waitaws\\.lastverteiler\\.net/([^<>\"]*?)/").getMatch(0);
                    }
                    if (api_otrUID == null) {
                        /* Basically the same/not relevant */
                        api_otrUID = br.getCookie("http://otr.datenkeller.net/", "otrUID");
                    }
                    if (api_otrUID != null) {
                        logger.info("Newway: New way active");
                        if (!api_otrUID_used) {
                            api_otrUID_used = true;
                            logger.info("NewwayUsing free API the first time...");
                            api_waitaws_url = "https://waitaws.lastverteiler.net/" + api_otrUID + "/" + finalfilenameurlencoded;
                            getPage(this.br, api_waitaws_url);
                            br2.getPage("https://waitaws.lastverteiler.net/style2.css");
                            br2.getPage("https://waitaws.lastverteiler.net/functions.js");
                            api_postPage(this.br, "https://waitaws.lastverteiler.net/api.php", "action=validate&otrUID=" + api_otrUID + "&file=" + finalfilenameurlencoded);
                            if (br.containsHTML("\"status\":\"fail\",\"reason\":\"user\"")) {
                                /* One retry is usually enough if the first attempt to use the API fails! */
                                if (api_failed) {
                                    /* This should never happen */
                                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "FATAL API failure", 30 * 60 * 1000l);
                                }
                                logger.info("Newway: Failed to start queue - refreshing api_otrUID");
                                br.clearCookies("http://otr.datenkeller.net/");
                                br.getPage("https://otr.datenkeller.net/");
                                br.getPage(this.getContentURL(link));
                                br.getPage(dlPage);
                                api_otrUID_used = false;
                                api_otrUID = null;
                                api_failed = true;
                                continue;
                            }
                            api_postPage(this.br, "https://waitaws.lastverteiler.net/api.php", "action=wait&status=ok&valid=ok&file=" + finalfilenameurlencoded + "&otrUID=" + api_otrUID);
                        }
                        sleep(16 * 1000l, link);
                        String postData = "";
                        String[] params = br.toString().split(",");
                        if (params == null || params.length == 0) {
                            logger.warning("Failed to get API postparameters");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        for (final String postPair : params) {
                            final String key = new Regex(postPair, "\"([^<>\"]*?)\"").getMatch(0);
                            String value = new Regex(postPair, "\"([^<>\"]*?)\":\"([^<>\"]*?)\"").getMatch(1);
                            if (value == null) {
                                value = new Regex(postPair, "\"([^<>\"]*?)\":(null|true|false|\\d+)").getMatch(1);
                            }
                            postData += key + "=" + Encoding.urlEncode(value) + "&";
                        }
                        api_postPage(this.br, "https://waitaws.lastverteiler.net/api.php", postData);
                        position = br.getRegex("\"wait_pos\":\"(\\d+)\"").getMatch(0);
                        dllink = br.getRegex("\"link\":\"(http:[^<>\"]*?)\"").getMatch(0);
                    } else {
                        logger.info("Oldway: Old way active");
                        sleep(27 * 1000l, link);
                        getPage(this.br, dlPage);
                        position = br.getRegex("Deine Position in der Warteschlange: </td><td>~(\\d+)</td>").getMatch(0);
                        if (br.containsHTML(DOWNLOADAVAILABLE)) {
                            logger.info("Oldway: dllink should be available, trying to get it");
                            dllink = getDllink();
                            break;
                        }
                    }
                    if (position != null) {
                        link.getLinkStatus().setStatusText("Warten auf Ticket...Position in der Warteschlange: " + position);
                    }
                    if (dllink != null) {
                        logger.info("Found dllink");
                        break;
                    }
                    if (i > 400 && site_lowSpeedLink != null) {
                        getPage(br2, "https://otr.datenkeller.net/" + site_lowSpeedLink);
                        dllink = br2.getRegex(">Dein Download Link:<br>[\t\n\r ]+<a href=\"(http://[^<>\\'\"]+)\"").getMatch(0);
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
                    logger.info("Didn't get a ticket on try " + i + "/" + maxloops + ". Retrying...Position: " + position);
                }
            }
            if (dllink == null) {
                logger.info("Didn't get a ticket --> Wait 30 minutes till next try");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Didn't get a ticket", 30 * 60 * 1000l);
            }
            dllink = dllink.replace("\\", "");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("free_finallink", dllink);
        dl.startDownload();
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
        api_prepBrowser();
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
        handleErrors();
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
        final String expires = getJson(br.toString(), "expires");
        if (expires != null && expires.matches("\\d+")) {
            ai.setValidUntil(Long.parseLong(expires) * 1000);
        }
        account.setType(AccountType.PREMIUM);
        account.setConcurrentUsePossible(true);
        ai.setUnlimitedTraffic();
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.postPage(API_BASE_URL, "api_version=" + APIVERSION + "&action=getpremlink&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&apikey=" + Encoding.urlEncode(getAPIKEY(account)) + "&filename=" + getFname(link));
        String dllink = getJson(br.toString(), "dllink");
        if (dllink == null) {
            handleErrors();
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            if (br.containsHTML("wurde wegen Missbrauch geblockt")) {
                logger.info("Account wurde wegen Missbrauch geblockt.");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
        }
        dl.startDownload();
    }

    /* Handles API errors */
    private void handleErrors() throws PluginException {
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

    private void api_prepBrowser() {
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
    }

    private void api_Free_prepBrowser(final Browser br) {
        br.getHeaders().put("Referer", api_waitaws_url);
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        br.getHeaders().put("Accept-Charset", null);
    }

    final String getFname(final DownloadLink link) {
        return new Regex(getContentURL(link), "otr\\.datenkeller\\.net/\\?file=(.+)").getMatch(0);
    }

    private String getDlpage(final DownloadLink link) {
        return getContentURL(link).replace("?file=", "?getFile=");
    }

    private String getAPIKEY(final Account acc) {
        String apikey = acc.getStringProperty("apikey", null);
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
        // correctBR(br);
    }

    @SuppressWarnings("unused")
    private void postPage(final Browser br, final String url, final String data) throws IOException {
        br.postPage(url, data);
        // correctBR(br);
    }

    private void api_postPage(final Browser br, final String url, final String data) throws IOException {
        this.api_Free_prepBrowser(br);
        br.postPage(url, data);
    }

    // private void correctBR(final Browser br) {
    // final String remove = br.getRegex("(<a href=\"#\" msgToJD=.*?href=\"#\")").getMatch(0);
    // if (remove != null) {
    // br.getRequest().setHtmlCode(br.toString().replace(remove, ""));
    // }
    // }
    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /*
         * Admin told us limit = 12 but when we checked it, only 7 with a large wait time in between were possible - anyways, works best
         * with only one
         */
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }
}