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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "otr.datenkeller.at" }, urls = { "https?://(www\\.)?otr\\.datenkeller\\.(at|net)/\\?(file|getFile)=.+" }, flags = { 2 })
public class OtrDatenkellerAt extends PluginForHost {

    public static String agent             = RandomUserAgent.generate();
    private final String DOWNLOADAVAILABLE = "onclick=\"startCount";
    private final String MAINPAGE          = "http://otr.datenkeller.net";

    public OtrDatenkellerAt(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
        // this.setStartIntervall(60 * 1000l);
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("otr.datenkeller.at/", "otr.datenkeller.net/"));
        link.setUrlDownload(link.getDownloadURL().replace("getFile", "file").replaceAll("\\&referer=otrkeyfinder\\&lang=[a-z]+", ""));
        link.setUrlDownload(link.getDownloadURL().replace("http://", "https://"));
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE;
    }

    /* API was implemented AFTER rev 26273 */
    private static final String APIVERSION = "1";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        correctDownloadLink(link);
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
                    /* we test 100 links at once */
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
                br.postPage("https://otr.datenkeller.net/api.php", sb.toString());
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
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        /* Use random UA again here because we do not use the same API as in linkcheck for free downloads */
        br.clearCookies("http://otr.datenkeller.net/");
        br.getHeaders().put("User-Agent", agent);
        final String dlPage = getDlpage(downloadLink);
        getPage(this.br, dlPage);
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
        String dllink = null;
        String site_lowSpeedLink;
        Browser br2 = br.cloneBrowser();
        String api_waitaws_url = null;
        String otrUID = null;
        final String finalfilenameurlencoded = Encoding.urlEncode(downloadLink.getFinalFileName());
        boolean api_otrUID_used = false;
        if (br.containsHTML(DOWNLOADAVAILABLE)) {
            dllink = getDllink();
        } else {
            final boolean pluginBroken = true;
            if (pluginBroken) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            site_lowSpeedLink = br.getRegex("\"(\\?lowSpeed=[^<>\\'\"]+)\"").getMatch(0);
            if (site_lowSpeedLink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br2.getPage("https://staticaws.lastverteiler.net/images/style.css");
            br2.getPage("https://staticaws.lastverteiler.net/otrfuncs/countMe.js");
            br2.getPage("https://waitaws.lastverteiler.net/style2.css");
            br2.getPage("https://waitaws.lastverteiler.net/functions.js");
            downloadLink.getLinkStatus().setStatusText("Waiting for ticket...");
            final int maxloops = 410;
            for (int i = 0; i <= maxloops; i++) {
                br2 = br.cloneBrowser();
                /* Whenever we got an otrUID the first time, we can use it for the whole process */
                if (otrUID == null) {
                    otrUID = br.getRegex("waitaws\\.lastverteiler\\.net/([^<>\"]*?)/").getMatch(0);
                }
                if (otrUID != null) {
                    logger.info("Newway: New way active");
                    br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    if (!api_otrUID_used) {
                        api_otrUID_used = true;
                        logger.info("Using free API the first time...");
                        api_waitaws_url = "https://waitaws.lastverteiler.net/" + otrUID + "/" + finalfilenameurlencoded;
                        br.getPage(api_waitaws_url);
                        br.postPage("https://waitaws.lastverteiler.net/api.php", "action=validate&otrUID=" + otrUID + "&file=" + finalfilenameurlencoded);
                        br.postPage("https://waitaws.lastverteiler.net/api.php", "action=wait&status=ok&valid=ok&file=" + finalfilenameurlencoded + "&otrUID=" + otrUID);
                    }
                    sleep(16 * 1000l, downloadLink);
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
                    br.getHeaders().put("Referer", api_waitaws_url);
                    br.postPage("https://waitaws.lastverteiler.net/api.php", postData);
                    position = br.getRegex("\"wait_pos\":\"(\\d+)\"").getMatch(0);
                    dllink = br.getRegex("\"link\":\"(http:[^<>\"]*?)\"").getMatch(0);
                } else {
                    logger.info("Oldway: Old way active");
                    sleep(27 * 1000l, downloadLink);
                    getPage(this.br, dlPage);
                    position = br.getRegex("Deine Position in der Warteschlange: </td><td>~(\\d+)</td>").getMatch(0);
                    if (br.containsHTML(DOWNLOADAVAILABLE)) {
                        logger.info("Oldway: dllink should be available, trying to get it");
                        dllink = getDllink();
                        break;
                    }
                }
                if (position != null) {
                    downloadLink.getLinkStatus().setStatusText("Warten auf Ticket...Position in der Warteschlange: " + position);
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
                        logger.info("Using lowspeed link for downloadlink: " + downloadLink.getDownloadURL());
                        break;
                    } else {
                        logger.warning("Failed to find low speed link, continuing to look for downloadticket...");
                    }
                }
                logger.info("Didn't get a ticket on try " + i + "/" + maxloops + ". Retrying...Position: " + position);
            }
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Didn't get a ticket");
        }
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account, boolean force) throws Exception {
        final String lang = System.getProperty("user.language");
        br.setCookiesExclusive(true);
        api_prepBrowser();
        String apikey = getAPIKEY(account);
        boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
        if (acmatch) {
            acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
        }
        if (acmatch && !force && apikey != null) {
            return;
        }
        br.setFollowRedirects(false);
        br.postPage("https://otr.datenkeller.net/api.php", "api_version=" + APIVERSION + "&action=login&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        if (br.containsHTML("\"status\":\"fail\"")) {
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        apikey = getJson(br.toString(), "apikey");
        if (apikey == null) {
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        account.setProperty("name", Encoding.urlEncode(account.getUser()));
        account.setProperty("pass", Encoding.urlEncode(account.getPass()));
        account.setProperty("apikey", apikey);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        account.setValid(true);
        final String expires = getJson(br.toString(), "expires");
        ai.setValidUntil(Long.parseLong(expires) * 1000);
        try {
            account.setType(AccountType.PREMIUM);
            account.setConcurrentUsePossible(true);
        } catch (final Throwable e) {
            /* not available in old Stable 0.9.581 */
        }
        ai.setUnlimitedTraffic();
        ai.setStatus("Premium user");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.postPage("https://otr.datenkeller.net/api.php", "api_version=" + APIVERSION + "&action=getpremlink&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&apikey=" + getAPIKEY(account) + "&filename=" + getFname(link));
        String dllink = getJson(br.toString(), "dllink");
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            if (br.containsHTML("wurde wegen Missbrauch geblockt")) {
                logger.info("Account wurde wegen Missbrauch geblockt.");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void api_prepBrowser() {
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
    }

    @SuppressWarnings("deprecation")
    final String getFname(final DownloadLink link) {
        return new Regex(link.getDownloadURL(), "otr\\.datenkeller\\.net/\\?file=(.+)").getMatch(0);
    }

    @SuppressWarnings("deprecation")
    private String getDlpage(DownloadLink downloadLink) {
        return downloadLink.getDownloadURL().replace("?file=", "?getFile=");
    }

    private String getAPIKEY(final Account acc) {
        return acc.getStringProperty("apikey", null);
    }

    private String getJson(final String source, final String parameter) {
        String result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?([0-9\\.]+)").getMatch(1);
        if (result == null) {
            result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?\"([^<>\"]*?)\"").getMatch(1);
        }
        return result;
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

    // private void correctBR(final Browser br) {
    // final String remove = br.getRegex("(<a href=\"#\" msgToJD=.*?href=\"#\")").getMatch(0);
    // if (remove != null) {
    // br.getRequest().setHtmlCode(br.toString().replace(remove, ""));
    // }
    // }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /*
         * Admin told us limit = 12 but when we checked it, only 7 with a large waittime in between were possible - anyways, works best with
         * only one
         */
        return 1;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}