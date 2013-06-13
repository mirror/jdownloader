package jd.plugins.hoster;

//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.crypt.Base64;
import jd.http.Browser;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.os.CrossSystem;

@HostPlugin(revision = "$Revision: 19039 $", interfaceVersion = 2, names = { "cloudzer.net" }, urls = { "http://(www\\.)?(cloudzer\\.net/.*?(file/|\\?id=|\\&id=)[\\w]+/?|clz\\.to/(file/)?(?!f/)[\\w]+/?)" }, flags = { 2 })
public class CloudzerNet extends PluginForHost {

    public static class StringContainer {
        public String string = null;
    }

    private static AtomicInteger   maxPrem          = new AtomicInteger(1);
    private char[]                 FILENAMEREPLACES = new char[] { '_' };
    private Pattern                IPREGEX          = Pattern.compile("(([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9]))", Pattern.CASE_INSENSITIVE);
    private static AtomicBoolean   hasDled          = new AtomicBoolean(false);
    private static AtomicLong      timeBefore       = new AtomicLong(0);
    private String                 LASTIP           = "LASTIP";
    private static StringContainer lastIP           = new StringContainer();
    private static final long      RECONNECTWAIT    = 3600000;

    private static void showFreeDialog(final String domain) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    try {
                        String lng = System.getProperty("user.language");
                        String message = null;
                        String title = null;
                        String tab = "                        ";
                        if ("de".equalsIgnoreCase(lng)) {
                            title = domain + " Free Download";
                            message = "Du lädst im kostenlosen Modus von " + domain + ".\r\n";
                            message += "Wie bei allen anderen Hostern holt JDownloader auch hier das Beste für dich heraus!\r\n\r\n";
                            message += tab + "  Falls du allerdings mehrere Dateien\r\n" + "          - und das möglichst mit Fullspeed und ohne Unterbrechungen - \r\n" + "             laden willst, solltest du dir den Premium Modus anschauen.\r\n\r\nUnserer Erfahrung nach lohnt sich das - Aber entscheide am besten selbst. Jetzt ausprobieren?  ";
                        } else {
                            title = domain + " Free Download";
                            message = "You are using the " + domain + " Free Mode.\r\n";
                            message += "JDownloader always tries to get the best out of each hoster's free mode!\r\n\r\n";
                            message += tab + "   However, if you want to download multiple files\r\n" + tab + "- possibly at fullspeed and without any wait times - \r\n" + tab + "you really should have a look at the Premium Mode.\r\n\r\nIn our experience, Premium is well worth the money. Decide for yourself, though. Let's give it a try?   ";
                        }
                        if (CrossSystem.isOpenBrowserSupported()) {
                            int result = JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                            if (JOptionPane.OK_OPTION == result) CrossSystem.openURL(new URL("http://update3.jdownloader.org/jdserv/BuyPremiumInterface/redirect?" + domain + "&freedialog"));
                        }
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

    private void checkShowFreeDialog() {
        SubConfiguration config = null;
        try {
            config = getPluginConfig();
            if (config.getBooleanProperty("premAdShown", Boolean.FALSE) == false) {
                if (config.getProperty("premAdShown2") == null) {
                    File checkFile = JDUtilities.getResourceFile("tmp/cldzn");
                    if (!checkFile.exists()) {
                        checkFile.mkdirs();
                        showFreeDialog("cloudzer.net");
                    }
                } else {
                    config = null;
                }
            } else {
                config = null;
            }
        } catch (final Throwable e) {
        } finally {
            if (config != null) {
                config.setProperty("premAdShown", Boolean.TRUE);
                config.setProperty("premAdShown2", "shown");
                config.save();
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException, InterruptedException {
        String id = getID(downloadLink);
        boolean red = br.isFollowingRedirects();
        br.setFollowRedirects(false);
        try {
            br.getPage("http://cloudzer.net/file/" + id + "/status");
            String ret = br.getRedirectLocation();
            if (ret != null && ret.contains("/404")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (ret != null && ret.contains("/410")) throw new PluginException(LinkStatus.ERROR_FATAL, "The requested file isn't available anymore!");
            String name = br.getRegex("(.*?)(\r|\n)").getMatch(0);
            String size = br.getRegex("[\r\n]([0-9\\, TGBMK]+)").getMatch(0);
            if (name == null || size == null) return AvailableStatus.UNCHECKABLE;
            downloadLink.setFinalFileName(Encoding.htmlDecode(name.trim()));
            downloadLink.setDownloadSize(SizeFormatter.getSize(size));
        } finally {
            br.setFollowRedirects(red);
        }
        return AvailableStatus.TRUE;
    }

    static class Sec {

        private byte[] prep;

        public Sec() {
            prep = Base64.decode("bWFpMUVONFppZWdoZXkxUXVlR2llN2ZlaTRlZWg1bmU=");
        }

        public String run() {

            return new String(new byte[] { 97, 112, 105, 107, 101, 121 }) + "=" + new String(prep);

        }
    }

    private static void workAroundTimeOut(final Browser br) {
        try {
            if (br != null) {
                br.setConnectTimeout(45000);
                br.setReadTimeout(45000);
            }
        } catch (final Throwable e) {
        }
    }

    private static String[] IPCHECK = new String[] { "http://ipcheck0.jdownloader.org", "http://ipcheck1.jdownloader.org", "http://ipcheck2.jdownloader.org", "http://ipcheck3.jdownloader.org" };

    public CloudzerNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://cloudzer.net/");
        this.setStartIntervall(2000l);
    }

    public String filterPackageID(String packageIdentifier) {
        return packageIdentifier.replaceAll("([^a-zA-Z0-9]+)", "");
    }

    public char[] getFilenameReplaceMap() {
        return FILENAMEREPLACES;
    }

    public boolean isHosterManipulatesFilenames() {
        return true;
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            Browser br = new Browser();
            workAroundTimeOut(br);
            br.setCookiesExclusive(true);
            StringBuilder sb = new StringBuilder();
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 80 links at once */
                    if (index == urls.length || links.size() > 80) break;
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append(new Sec().run());
                int c = 0;
                for (DownloadLink dl : links) {
                    sb.append("&id_" + c + "=" + getID(dl));
                    c++;
                }
                int retry = 0;
                while (true) {
                    /*
                     * workaround for api issues, retry 5 times when content length is only 20 bytes
                     */
                    if (retry == 5) return false;
                    br.postPage("http://cloudzer.net/api/filemultiple", sb.toString());
                    if (br.getHttpConnection().getLongContentLength() != 20) {
                        break;
                    }
                    Thread.sleep(500);
                    retry++;
                }
                sb = null;
                String infos[][] = br.getRegex(Pattern.compile("(.*?),(.*?),(.*?),(.*?),(.*?)(\r|\n|$)")).getMatches();
                for (DownloadLink dl : links) {
                    String id = getID(dl);
                    int hit = -1;
                    for (int i = 0; i < infos.length; i++) {
                        if (infos[i][1].equalsIgnoreCase(id)) {
                            hit = i;
                            break;
                        }
                    }
                    if (hit == -1) {
                        /* id not in response, so its offline */
                        dl.setAvailable(false);
                    } else {
                        dl.setFinalFileName(Encoding.htmlDecode(infos[hit][4].trim()));
                        long size = SizeFormatter.getSize(infos[hit][2]);
                        dl.setDownloadSize(size);
                        if (size > 0) {
                            dl.setProperty("VERIFIEDFILESIZE", size);
                        }
                        if ("online".equalsIgnoreCase(infos[hit][0].trim())) {
                            dl.setAvailable(true);
                            String sha1 = infos[hit][3].trim();
                            if (sha1.length() == 0) sha1 = null;
                            dl.setSha1Hash(sha1);
                            dl.setMD5Hash(null);
                        } else {
                            dl.setAvailable(false);
                        }
                    }
                }
                if (index == urls.length) break;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        try {
            synchronized (account) {
                String token = api_getAccessToken(account, false);
                String tokenType = null;
                try {
                    tokenType = api_getTokenType(account, token, true);
                } catch (final PluginException e) {
                    token = api_getAccessToken(account, false);
                    tokenType = api_getTokenType(account, token, true);
                }
                if ("free".equals(tokenType)) {
                    /* free user */
                    ai.setStatus("Free accounts are not supported");
                    account.setValid(false);
                    account.setProperty("token", null);
                    account.setProperty("tokenType", null);
                } else if ("premium".equals(tokenType)) {
                    String traffic = br.getRegex("traffic_left\":\\s*?\"?(\\d+)").getMatch(0);
                    long max = 100 * 1024 * 1024 * 1024l;
                    long current = Long.parseLong(traffic);
                    ai.setTrafficMax(Math.max(max, current));
                    ai.setTrafficLeft(current);
                    String expireDate = br.getRegex("account_premium\":\\s*?\"?(\\d+)").getMatch(0);
                    ai.setValidUntil(Long.parseLong(expireDate) * 1000);
                    if (current <= 0 || br.containsHTML("download_available\":false")) {
                        String refreshIn = br.getRegex("traffic_reset\":\\s*?(\\d+)").getMatch(0);
                        if (refreshIn != null) {
                            account.setProperty("PROPERTY_TEMP_DISABLED_TIMEOUT", Long.parseLong(refreshIn) * 1000);
                        } else {
                            account.setProperty("PROPERTY_TEMP_DISABLED_TIMEOUT", Property.NULL);
                        }
                        logger.info("Download_available: " + br.containsHTML("download_available\":true"));
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                    }
                    ai.setStatus("Premium account");
                    if (!ai.isExpired()) account.setValid(true);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        } catch (final PluginException e) {
            account.setProperty("token", null);
            account.setProperty("tokenType", null);
            throw e;
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://cloudzer.net/legal";
    }

    private String getID(final DownloadLink downloadLink) {
        String id = new Regex(downloadLink.getDownloadURL(), "cloudzer.net/file/([\\w]+)/?").getMatch(0);
        if (id == null) id = new Regex(downloadLink.getDownloadURL(), "clz.to/(file/)?([\\w]+)/?").getMatch(1);
        return id;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private String getPassword(final DownloadLink downloadLink) throws Exception {
        String passCode = null;
        if (br.containsHTML("<h2>Authentifizierung</h2>")) {
            logger.info("pw protected link");
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = getUserInput(null, downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
        }
        return passCode;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 400;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doFree(downloadLink, null);
    }

    public void doFree(final DownloadLink downloadLink, final Account account) throws Exception {
        logger.info("Free mode");
        checkShowFreeDialog();
        String currentIP = getIP();
        try {
            workAroundTimeOut(br);
            String id = getID(downloadLink);
            br.setFollowRedirects(false);
            br.setCookie("http://cloudzer.net/", "lang", "de");
            br.getPage("http://cloudzer.net/language/de");
            if (br.containsHTML("<title>[^<].*?\\- Wartungsarbeiten</title>")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "ServerMaintenance", 10 * 60 * 1000);

            /**
             * Reconnect handling to prevent having to enter a captcha just to see that a limit has been reached
             */
            logger.info("New Download: currentIP = " + currentIP);
            if (hasDled.get() && ipChanged(currentIP, downloadLink) == false) {
                long result = System.currentTimeMillis() - timeBefore.get();
                if (result < RECONNECTWAIT && result > 0) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, RECONNECTWAIT - result);
            }

            br.getPage("http://cloudzer.net/file/" + id);
            if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("/404")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
            if (br.containsHTML(">Sie haben die max\\. Anzahl an Free\\-Downloads f\\&#252;r diese Stunde erreicht")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
            String passCode = null;
            if (br.containsHTML("<h2>Authentifizierung</h2>")) {
                passCode = getPassword(downloadLink);
                Form form = br.getForm(0);
                form.put("pw", Encoding.urlEncode(passCode));
                br.submitForm(form);
                if (br.containsHTML("<h2>Authentifizierung</h2>")) {
                    downloadLink.setProperty("pass", null);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Password wrong!");
                }
                downloadLink.setProperty("pass", passCode);
            }
            final Browser brc = br.cloneBrowser();
            brc.getPage("http://cloudzer.net/js/download.js");
            final String rcID = brc.getRegex("Recaptcha\\.create\\(\"([^<>\"]*?)\"").getMatch(0);
            String wait = br.getRegex("Aktuelle Wartezeit: <span>(\\d+)</span> Sekunden</span>").getMatch(0);
            if (rcID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (wait == null) {
                wait = "30";
            }
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage("http://cloudzer.net/io/ticket/slot/" + getID(downloadLink), "");
            if (!br.containsHTML("\"succ\":true")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            final long timebefore = System.currentTimeMillis();
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setId(rcID);
            rc.load();
            for (int i = 0; i <= 5; i++) {
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, downloadLink);
                int passedTime = (int) ((System.currentTimeMillis() - timebefore) / 1000) - 1;
                if (i == 0 && passedTime < Integer.parseInt(wait)) {
                    sleep((Integer.parseInt(wait) - passedTime) * 1001l, downloadLink);
                }
                br.postPage("http://cloudzer.net/io/ticket/captcha/" + getID(downloadLink), "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c);
                if (br.containsHTML("\"err\":\"captcha\"")) {
                    try {
                        invalidateLastChallengeResponse();
                    } catch (final Throwable e) {
                    }
                    rc.reload();
                    continue;
                } else {
                    try {
                        validateLastChallengeResponse();
                    } catch (final Throwable e) {
                    }
                }
                break;
            }
            generalFreeErrorhandling(account);
            if (br.containsHTML("err\":\"Ticket kann nicht")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 30 * 60 * 1000l);
            if (br.containsHTML("err\":\"Leider sind derzeit all unsere")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free Downloadslots available", 15 * 60 * 1000l);
            if (br.containsHTML("limit\\-parallel")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You're already downloading", RECONNECTWAIT);
            if (br.containsHTML("welche von Free\\-Usern gedownloadet werden kann")) throw new PluginException(LinkStatus.ERROR_FATAL, "Only Premium users are allowed to download files lager than 1,00 GB.");
            if (br.containsHTML("\"err\":\"Das Verteilen dieser Datei ist vermutlich nicht erlaubt")) throw new PluginException(LinkStatus.ERROR_FATAL, "Link abused, download not possible!");
            String url = br.getRegex("\"url\":\\s*?\"(.*?dl\\\\/.*?)\"").getMatch(0);
            if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            url = url.replaceAll("\\\\/", "/");
            dl = BrowserAdapter.openDownload(br, downloadLink, url, false, 1);
            try {
                /* remove next major update */
                /* workaround for broken timeout in 0.9xx public */
                ((RAFDownload) dl).getRequest().setConnectTimeout(30000);
                ((RAFDownload) dl).getRequest().setReadTimeout(60000);
            } catch (final Throwable ee) {
            }
            if (!dl.getConnection().isContentDisposition()) {
                try {
                    br.followConnection();
                } catch (final Throwable e) {
                    logger.severe(e.getMessage());
                }
                logger.info(br.toString());
                if (dl.getConnection().getResponseCode() == 404) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
                generalFreeErrorhandling(account);
                if (br.containsHTML("please try again in an hour or purchase one of our")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, RECONNECTWAIT);
                if (dl.getConnection().getResponseCode() == 508) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError(508)", 30 * 60 * 1000l);
                if (br.containsHTML("try again later")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 30 * 60 * 1000l);
                if (br.containsHTML("All of our free\\-download capacities are")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "All of our free-download capacities are exhausted currently", 10 * 60 * 1000l);
                if (br.containsHTML("File not found!")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                if (br.getURL().contains("view=error")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 10 * 60 * 1000l);
                if (br.containsHTML("Aus technischen Gr") && br.containsHTML("ist ein Download momentan nicht m")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 30 * 60 * 1000l);
                if ("No htmlCode read".equalsIgnoreCase(br.toString())) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 30 * 60 * 1000l);
                if (br.containsHTML("Datei herunterladen")) {
                    /*
                     * we get fresh entry page after clicking download, means we have to start from beginning
                     */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Serverproblem", 5 * 60 * 1000l);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (dl.getConnection().getResponseCode() == 404) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (account != null) account.setProperty("LASTDOWNLOAD", System.currentTimeMillis());
            dl.startDownload();
            hasDled.set(true);
        } catch (Exception e) {
            hasDled.set(false);
            throw e;
        } finally {
            timeBefore.set(System.currentTimeMillis());
            setIP(currentIP, downloadLink, account);
        }
    }

    private void generalFreeErrorhandling(final Account account) throws PluginException {
        if (br.containsHTML("No connection to database")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 30 * 60 * 1000l);
        if (br.containsHTML("Sie haben die max\\. Anzahl an Free\\-Downloads") || br.containsHTML("err\":\"limit-dl")) {
            if (account == null) {
                logger.info("Limit reached, throwing reconnect exception");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, RECONNECTWAIT);
            } else {
                logger.info("Limit reached, disabling account to use the next one!");
                account.setProperty("LASTDOWNLOAD", System.currentTimeMillis());
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
        }
    }

    private void handleErrorCode(Browser br, Account acc, String usedToken, boolean throwPluginDefect) throws Exception {
        String errCode = br.getRegex("code\":\\s*?\"?(\\d+)").getMatch(0);
        if (errCode == null) errCode = br.getRegex("errCode\":\\s*?\"?(\\d+)").getMatch(0);
        if (errCode != null) {
            logger.info("ErrorCode: " + errCode);
            int code = Integer.parseInt(errCode);
            switch (code) {
            case 1:
            case 2:
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "User does not exist!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 3:
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!\r\n\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 4:
                if (acc != null) {
                    synchronized (acc) {
                        String savedToken = acc.getStringProperty("token", null);
                        if (usedToken != null && usedToken.equals(savedToken)) {
                            acc.setProperty("token", null);
                        }
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "LoginToken invalid", 60 * 1000l);
                    }
                }
            case 16:
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Disabled because of flood protection", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            case 20:
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Locked account!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 404:
            case 410:
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            case 500:
                logger.info("Received unknown API response error 500!\nIf this happened during the login process, the account was not accepted!");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "API doesn't accept account (error 500).", PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 8000:
                /* traffic exhausted */
                if (acc != null) {
                    String reset = br.getRegex("reset\":\\s*?\"?(\\d+)").getMatch(0);
                    if (reset != null) {
                        acc.setProperty("PROPERTY_TEMP_DISABLED_TIMEOUT", Long.parseLong(reset) * 1000);
                    } else {
                        acc.setProperty("PROPERTY_TEMP_DISABLED_TIMEOUT", Property.NULL);
                    }
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            case 8011:
                /* direct download but upload user deleted */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Upload User deleted");
            case 8016:
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server in maintenance", 20 * 60 * 1000l);
            case 8017:
                /* file is probably prohibited */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        if (throwPluginDefect) {
            logger.info("ErrorCode: unknown\r\n" + br);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

    }

    private String api_getAccessToken(Account account, boolean liveToken) throws Exception {
        synchronized (account) {
            try {
                String token = account.getStringProperty("token", null);
                if (token != null && liveToken == false) return token;
                br.postPage("http://cloudzer.net/api/user/login", "name=" + Encoding.urlEncode(account.getUser()) + "&pass=" + JDHash.getSHA1(URLDecoder.decode(account.getPass(), "UTF-8").toLowerCase(Locale.ENGLISH)) + "&ishash=1&app=JDownloader");
                token = br.getRegex("access_token\":\"(.*?)\"").getMatch(0);
                if (token == null) handleErrorCode(br, account, token, true);
                account.setProperty("token", token);
                return token;
            } catch (final PluginException e) {
                account.setProperty("token", null);
                account.setProperty("tokenType", null);
                throw e;
            }
        }
    }

    private String api_getTokenType(Account account, String token, boolean liveToken) throws Exception {
        synchronized (account) {
            try {
                String tokenType = account.getStringProperty("tokenType", null);
                if (tokenType != null && liveToken == false) return tokenType;
                br.getPage("http://cloudzer.net/api/user/jdownloader?access_token=" + token);
                tokenType = br.getRegex("account_type\":\\s*?\"(premium|free)").getMatch(0);
                if (tokenType == null) handleErrorCode(br, account, token, true);
                account.setProperty("tokenType", tokenType);
                if ("premium".equals(tokenType)) {
                    try {
                        maxPrem.set(-1);
                        account.setMaxSimultanDownloads(-1);
                        account.setConcurrentUsePossible(true);
                    } catch (final Throwable e) {
                    }
                } else {
                    try {
                        maxPrem.set(1);
                        account.setMaxSimultanDownloads(1);
                        account.setConcurrentUsePossible(false);
                    } catch (final Throwable e) {
                    }
                }
                return tokenType;
            } catch (final PluginException e) {
                maxPrem.set(-1);
                account.setProperty("token", null);
                account.setProperty("tokenType", null);
                throw e;
            }
        }
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        String token = api_getAccessToken(account, false);
        // String tokenType = api_getTokenType(account, token, false);
        // if (!"premium".equals(tokenType)) {
        // logger.info("Free mode");
        // login(account);
        // doFree(downloadLink, account);
        // return;
        // }
        logger.info("Premium mode");
        String id = getID(downloadLink);
        br.postPage("http://cloudzer.net/api/download/jdownloader", "access_token=" + token + "&auth=" + id);
        String url = br.getRegex("link\":\\s*?\"(http.*?)\"").getMatch(0);
        String sha1 = br.getRegex("sha1\":\\s*?\"([0-9a-fA-F]+)\"").getMatch(0);
        String name = br.getRegex("name\":\\s*?\"(.*?)\"").getMatch(0);
        String size = br.getRegex("size\":\\s*?\"?(\\d+)\"").getMatch(0);
        String concurrent = br.getRegex("concurrent\":\\s*?\"?(\\d+)").getMatch(0);
        if (url == null) handleErrorCode(br, account, token, true);
        if (sha1 != null) downloadLink.setSha1Hash(sha1);
        if (downloadLink.getFinalFileName() == null) downloadLink.setFinalFileName(name);
        if (size != null) {
            try {
                downloadLink.setVerifiedFileSize(Long.parseLong(size));
            } catch (final Throwable e) {
                /* not available in old 09581 stable */
                downloadLink.setDownloadSize(Long.parseLong(size));
            }
        }
        url = url.replaceAll("\\\\/", "/");
        /* we must append access_token because without the url won't work */
        url = url + "?access_token=" + token;
        int maxChunks = 0;
        if (concurrent != null) {
            int maxConcurrent = Math.abs(Integer.parseInt(concurrent));
            if (maxConcurrent == 1) {
                maxChunks = 1;
            } else if (maxConcurrent > 1) {
                maxChunks = -maxConcurrent;
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, maxChunks);
        try {
            /* remove next major update */
            /* workaround for broken timeout in 0.9xx public */
            ((RAFDownload) dl).getRequest().setConnectTimeout(30000);
            ((RAFDownload) dl).getRequest().setReadTimeout(60000);
        } catch (final Throwable ee) {
        }
        if (dl.getConnection().getLongContentLength() == 0 || !dl.getConnection().isContentDisposition()) {
            try {
                br.followConnection();
            } catch (final Throwable e) {
                logger.severe(e.getMessage());
            }
            handleErrorCode(br, account, token, false);
            if (dl.getConnection().getResponseCode() == 404) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            if (dl.getConnection().getResponseCode() == 508) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError(508)", 30 * 60 * 1000l);
            if (br.containsHTML("try again later")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 30 * 60 * 1000l);
            if (br.containsHTML("File not found!")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.containsHTML("No connection to database")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 10 * 60 * 1000l);
            if ((br.containsHTML("Aus technischen Gr") && br.containsHTML("ist ein Download momentan nicht m")) || br.containsHTML("download this file due to technical issues at the moment")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 30 * 60 * 1000l);
            if (br.getURL().contains("view=error")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 10 * 60 * 1000l);
            try {
                logger.info(br.toString());
            } catch (final Throwable e) {
            }
            try {
                logger.info(dl.getConnection().toString());
            } catch (final Throwable e) {
            }
            if ("No htmlCode read".equalsIgnoreCase(br.toString())) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 30 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dl.getConnection().getResponseCode() == 404) {
            try {
                br.followConnection();
            } catch (final Throwable e) {
                logger.severe(e.getMessage());
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(DownloadLink link, Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (!"premium".equalsIgnoreCase(acc.getStringProperty("tokenType", null))) return true;
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }

    // private void login(Account account) throws Exception {
    // this.setBrowserExclusive();
    // workAroundTimeOut(br);
    // br.setDebug(true);
    // br.setFollowRedirects(true);
    // br.setAcceptLanguage("en, en-gb;q=0.8");
    // br.setCookie("http://cloudzer.net", "lang", "en");
    // br.getPage("http://cloudzer.net");
    // br.getPage("http://cloudzer.net/language/en");
    // br.postPage("http://cloudzer.net/io/login", "id=" +
    // Encoding.urlEncode(account.getUser()) + "&pw=" +
    // Encoding.urlEncode(account.getPass()));
    // if (br.containsHTML("<title>[^<].*?- Wartungsarbeiten</title>")) throw
    // new
    // PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE,
    // "ServerMaintenance", 10 * 60 * 1000);
    // if (br.containsHTML("User and password do not match")) {
    // AccountInfo ai = account.getAccountInfo();
    // if (ai != null) ai.setStatus("User and password do not match");
    // throw new PluginException(LinkStatus.ERROR_PREMIUM,
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // }
    // if (br.getCookie("http://cloudzer.net", "auth") == null) throw new
    // PluginException(LinkStatus.ERROR_PREMIUM,
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    private String getIP() throws PluginException {
        Browser ip = new Browser();
        String currentIP = null;
        ArrayList<String> checkIP = new ArrayList<String>(Arrays.asList(IPCHECK));
        Collections.shuffle(checkIP);
        for (String ipServer : checkIP) {
            if (currentIP == null) {
                try {
                    ip.getPage(ipServer);
                    currentIP = ip.getRegex(IPREGEX).getMatch(0);
                    if (currentIP != null) break;
                } catch (Throwable e) {
                }
            }
        }
        if (currentIP == null) {
            logger.warning("firewall/antivirus/malware/peerblock software is most likely is restricting accesss to JDownloader IP checking services");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return currentIP;
    }

    private boolean setIP(String IP, final DownloadLink link, final Account account) throws PluginException {
        synchronized (IPCHECK) {
            if (IP != null && !new Regex(IP, IPREGEX).matches()) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (ipChanged(IP, link) == false) {
                // Static IP or failure to reconnect! We don't change lastIP
                logger.warning("Your IP hasn't changed since last download");
                return false;
            } else {
                String lastIP = IP;
                link.setProperty(LASTIP, lastIP);
                CloudzerNet.lastIP.string = lastIP;
                logger.info("LastIP = " + lastIP);
                return true;
            }
        }
    }

    private boolean ipChanged(String IP, DownloadLink link) throws PluginException {
        String currentIP = null;
        if (IP != null && new Regex(IP, IPREGEX).matches()) {
            currentIP = IP;
        } else {
            currentIP = getIP();
        }
        if (currentIP == null) return false;
        String lastIP = link.getStringProperty(LASTIP, null);
        if (lastIP == null) lastIP = CloudzerNet.lastIP.string;
        return !currentIP.equals(lastIP);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

}
