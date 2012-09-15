//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.crypt.Base64;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
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
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.os.CrossSystem;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploaded.to" }, urls = { "(http://[\\w\\.-]*?uploaded\\.(to|net)/.*?(file/|\\?id=|&id=)[\\w]+/?)|(http://[\\w\\.]*?ul\\.to/(?!folder)(\\?id=|&id=)?[\\w\\-]+/.+)|(http://[\\w\\.]*?ul\\.to/(?!folder)(\\?id=|&id=)?[\\w\\-]+/?)" }, flags = { 2 })
public class Uploadedto extends PluginForHost {

    public static class StringContainer {
        public String string = null;
    }

    private static AtomicInteger   maxPrem                      = new AtomicInteger(1);
    private char[]                 FILENAMEREPLACES             = new char[] { '_' };
    private final String           ACTIVATEACCOUNTERRORHANDLING = "ACTIVATEACCOUNTERRORHANDLING";
    private final String           EXPERIMENTALHANDLING         = "EXPERIMENTALHANDLING";
    private Pattern                IPREGEX                      = Pattern.compile("(([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9]))", Pattern.CASE_INSENSITIVE);
    private static AtomicBoolean   hasDled                      = new AtomicBoolean(false);
    private static AtomicLong      timeBefore                   = new AtomicLong(0);
    private String                 LASTIP                       = "LASTIP";
    private static StringContainer lastIP                       = new StringContainer();

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException, InterruptedException {
        this.correctDownloadLink(downloadLink);
        String id = getID(downloadLink);
        boolean red = br.isFollowingRedirects();
        br.setFollowRedirects(false);
        try {
            br.getPage("http://uploaded.net/file/" + id + "/status");
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
        public static String d(final byte[] b, final byte[] key) {
            Cipher cipher;
            try {
                final IvParameterSpec ivSpec = new IvParameterSpec(key);
                final SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

                cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
                return new String(cipher.doFinal(b), "UTF-8");
            } catch (final Exception e) {
                e.printStackTrace();
                final IvParameterSpec ivSpec = new IvParameterSpec(key);
                final SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

                try {
                    cipher = Cipher.getInstance("AES/CBC/nopadding");

                    cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
                    return new String(cipher.doFinal(b), "UTF-8");
                } catch (final Exception e1) {
                    e.printStackTrace();
                }

            }
            return null;
        }

        private byte[] key;

        private byte[] prep;

        public Sec() {
            key = new byte[] { 0x01, 0x02, 0x11, 0x01, 0x01, 0x54, 0x01, 0x01, 0x01, 0x01, 0x12, 0x01, 0x01, 0x01, 0x22, 0x01 };
            prep = Base64.decode("MC8O21gQXUaeSgMxxiOGugSrROkQHTbadlwDeJqHOpU4Q2o38bGWkm3/2zfS0N0s");
        }

        public String run() {

            return new String(new byte[] { 97, 112, 105, 107, 101, 121 }) + "=" + d(prep, key);

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

    private static void showFreeDialog() {
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
                            title = "Uploaded.to Free Download";
                            message = "Du lädst im kostenlosen Modus von Uploaded.to.\r\n";
                            message += "Wie bei allen anderen Hostern holt JDownloader auch hier das Beste für dich heraus!\r\n\r\n";
                            message += tab + "  Falls du allerdings mehrere Dateien\r\n" + "          - und das möglichst mit Fullspeed und ohne Unterbrechungen - \r\n" + "             laden willst, solltest du dir den Premium Modus anschauen.\r\n\r\nUnserer Erfahrung nach lohnt sich das - Aber entscheide am besten selbst. Jetzt ausprobieren?  ";
                        } else {
                            title = "Uploaded.to Free Download";
                            message = "You are using the Uploaded.to Free Mode.\r\n";
                            message += "JDownloader always tries to get the best out of each hoster's free mode!\r\n\r\n";
                            message += tab + "   However, if you want to download multiple files\r\n" + tab + "- possibly at fullspeed and without any wait times - \r\n" + tab + "you really should have a look at the Premium Mode.\r\n\r\nIn our experience, Premium is well worth the money. Decide for yourself, though. Let's give it a try?   ";
                        }
                        if (CrossSystem.isOpenBrowserSupported()) {
                            int result = JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                            if (JOptionPane.OK_OPTION == result) CrossSystem.openURL(new URL("http://update3.jdownloader.org/jdserv/BuyPremiumInterface/redirect?ul.to&freedialog"));
                        }
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

    private static String[] IPCHECK = new String[] { "http://ipcheck0.jdownloader.org", "http://ipcheck1.jdownloader.org", "http://ipcheck2.jdownloader.org", "http://ipcheck3.jdownloader.org" };

    public Uploadedto(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.enablePremium("http://uploaded.to/");
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
                    br.postPage("http://uploaded.net/api/filemultiple", sb.toString());
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
    public void correctDownloadLink(final DownloadLink link) {
        String url = link.getDownloadURL();
        url = url.replaceFirst("http://.*?/", "http://uploaded.net/");
        url = url.replaceFirst("\\.net/.*?id=", ".net/file/");
        if (!url.contains("/file/")) {
            url = url.replaceFirst("uploaded.net/", "uploaded.net/file/");
        }
        String[] parts = url.split("\\/");
        String newLink = "";
        for (int t = 0; t < Math.min(parts.length, 5); t++) {
            newLink += parts[t] + "/";
        }
        link.setUrlDownload(newLink);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        br.postPage("http://uploaded.net/status", "uid=" + Encoding.urlEncode(account.getUser()) + "&upw=" + Encoding.urlEncode(account.getPass()));
        if (br.containsHTML("blocked")) {
            ai.setStatus("Too many failed logins! Wait 15 mins");
            account.setTempDisabled(true);
            return ai;
        }
        if (br.containsHTML("wrong password")) {
            ai.setStatus("Wrong password");
            account.setValid(false);
            return ai;
        }
        if (br.containsHTML("wrong user")) {
            ai.setStatus("Wrong username");
            account.setValid(false);
            return ai;
        }
        String isPremium = br.getMatch("status: (premium)");
        if (isPremium == null) {
            ai.setStatus("Registered (free) User");
            ai.setUnlimitedTraffic();
            try {
                maxPrem.set(1);
                account.setMaxSimultanDownloads(1);
                account.setConcurrentUsePossible(false);
            } catch (final Throwable e) {
            }
            account.setProperty("free", true);
        } else {
            String traffic = br.getMatch("traffic: (\\d+)");
            String expire = br.getMatch("expire: (\\d+)");
            if (expire != null) ai.setValidUntil(Long.parseLong(expire) * 1000);
            ai.setStatus("Premium account");
            account.setValid(true);
            long max = 100 * 1024 * 1024 * 1024l;
            long current = Long.parseLong(traffic);
            ai.setTrafficMax(Math.max(max, current));
            ai.setTrafficLeft(current);
            try {
                maxPrem.set(-1);
                account.setMaxSimultanDownloads(-1);
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
            }
            account.setProperty("free", false);
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://uploaded.net/legal";
    }

    private String getID(final DownloadLink downloadLink) {
        return new Regex(downloadLink.getDownloadURL(), "uploaded.net/file/(.*?)/").getMatch(0);
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
        String currentIP = getIP();
        try {
            SubConfiguration config = null;
            try {
                config = getPluginConfig();
                if (config.getBooleanProperty("premAdShown", Boolean.FALSE) == false) {
                    if (config.getProperty("premAdShown2") == null) {
                        showFreeDialog();
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

            workAroundTimeOut(br);
            String id = getID(downloadLink);
            br.setFollowRedirects(false);
            br.setCookie("http://uploaded.net/", "lang", "de");
            br.getPage("http://uploaded.net/language/de");
            if (br.containsHTML("<title>[^<].*?\\- Wartungsarbeiten</title>")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "ServerMaintenance", 10 * 60 * 1000);

            /**
             * Free-Account Errorhandling: This allows users to switch between free accounts instead of reconnecting if a limit is reached
             */
            if (this.getPluginConfig().getBooleanProperty(ACTIVATEACCOUNTERRORHANDLING, false) && account != null) {
                final long lastdownload = account.getLongProperty("LASTDOWNLOAD", 0);
                final long passedTime = System.currentTimeMillis() - lastdownload;
                if (passedTime < 3600000 && lastdownload > 0) {
                    logger.info("Limit must still exist on account, disabling it");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
            } else if (account == null && this.getPluginConfig().getBooleanProperty(EXPERIMENTALHANDLING, false)) {
                /**
                 * Experimental reconnect handling to prevent having to enter a captcha just to see that a limit has been reached
                 */
                logger.info("New Download: currentIP = " + currentIP);
                if (hasDled.get() && ipChanged(currentIP, downloadLink) == false) {
                    long result = System.currentTimeMillis() - timeBefore.get();
                    if (result < 3600000 && result > 0) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 3600000 - result);
                }
            }

            br.getPage("http://uploaded.net/file/" + id);
            if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("/404")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
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
            brc.getPage("http://uploaded.net/js/download.js");
            final String rcID = brc.getRegex("Recaptcha\\.create\\(\"([^<>\"]*?)\"").getMatch(0);
            final String wait = br.getRegex("Aktuelle Wartezeit: <span>(\\d+)</span> Sekunden</span>").getMatch(0);
            if (rcID == null || wait == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage("http://uploaded.net/io/ticket/slot/" + getID(downloadLink), "");
            if (!br.containsHTML("\\{succ:true\\}")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
                br.postPage("http://uploaded.net/io/ticket/captcha/" + getID(downloadLink), "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c);
                if (br.containsHTML("\"err\":\"captcha\"")) {
                    rc.reload();
                    continue;
                }
                break;
            }
            generalFreeErrorhandling(account);
            if (br.containsHTML("err\":\"Ticket kann nicht")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 30 * 60 * 1000l);
            if (br.containsHTML("err\":\"Leider sind derzeit all unsere")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free Downloadslots available", 15 * 60 * 1000l);
            if (br.containsHTML("limit\\-parallel")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You're already downloading", 60 * 60 * 1000l);
            if (br.containsHTML("welche von Free\\-Usern gedownloadet werden kann")) throw new PluginException(LinkStatus.ERROR_FATAL, "Only Premium users are allowed to download files lager than 1,00 GB.");
            String url = br.getRegex("url:\\'(http:.*?)\\'").getMatch(0);
            if (url == null) url = br.getRegex("url:\\'(dl/.*?)\\'").getMatch(0);
            if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
                if (br.containsHTML("please try again in an hour or purchase one of our")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
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
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
            } else {
                logger.info("Limit reached, disabling account to use the next one!");
                account.setProperty("LASTDOWNLOAD", System.currentTimeMillis());
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
        }
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        if (account.getBooleanProperty("free")) {
            doFree(downloadLink, account);
        } else {
            logger.info("Premium mode");
            br.setFollowRedirects(false);
            String id = getID(downloadLink);
            br.getPage("http://uploaded.net/file/" + id + "/ddl");
            if (br.containsHTML("<title>[^<].*?- Wartungsarbeiten</title>")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "ServerMaintenance", 10 * 60 * 1000);
            String error = new Regex(br.getRedirectLocation(), "http://uploaded.net/\\?view=(.*)").getMatch(0);
            if (error == null) {
                error = new Regex(br.getRedirectLocation(), "\\?view=(.*?)&i").getMatch(0);
            }
            if (error != null) {
                if (error.contains("error_traffic")) throw new PluginException(LinkStatus.ERROR_PREMIUM, JDL.L("plugins.hoster.uploadedto.errorso.premiumtrafficreached", "Traffic limit reached"), PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (br.containsHTML(">Download Blocked \\(ip\\)<") || br.containsHTML("Leider haben wir Zugriffe von zu vielen verschiedenen IPs auf Ihren Account feststellen k\\&#246;nnen, Account-Sharing ist laut unseren AGB strengstens untersagt")) {
                logger.info("Download blocked (IP), disabling account...");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            if (br.getRedirectLocation() == null) {
                /* ul does not take care of set language.... */
                if (br.containsHTML(">Traffic exhausted") || br.containsHTML(">Traffickontingent aufgebraucht")) {
                    logger.info("Traffic exhausted, temp disabled account");
                    /* temp debug info */
                    logger.info(br.toString());
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
                logger.info("InDirect Downloads active");
                Form form = br.getForm(0);
                if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                if (form.getAction() != null && form.getAction().contains("register")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
                if (form.getAction() == null || form.getAction().contains("access")) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                logger.info("Download from:" + form.getAction());
                form.setMethod(MethodType.GET);
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form, true, 0);
            } else {
                logger.info("Direct Downloads active");
                logger.info("Download from:" + br.getRedirectLocation());
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation(), true, 0);
            }
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
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            dl.startDownload();
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(DownloadLink link, Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if ("true".equalsIgnoreCase(acc.getStringProperty("free", "true"))) {
            /* free accounts also have captchas */
            /* currently they dont have captchas */
            return false;
        }
        return false;
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        workAroundTimeOut(br);
        br.setDebug(true);
        br.setFollowRedirects(true);
        br.setAcceptLanguage("en, en-gb;q=0.8");
        br.setCookie("http://uploaded.net", "lang", "en");
        br.getPage("http://uploaded.net");
        br.getPage("http://uploaded.net/language/en");
        br.postPage("http://uploaded.net/io/login", "id=" + Encoding.urlEncode(account.getUser()) + "&pw=" + Encoding.urlEncode(account.getPass()));
        if (br.containsHTML("User and password do not match")) {
            AccountInfo ai = account.getAccountInfo();
            if (ai != null) ai.setStatus("User and password do not match");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        if (br.getCookie("http://uploaded.net", "auth") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

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
                Uploadedto.lastIP.string = lastIP;
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
        if (lastIP == null) lastIP = Uploadedto.lastIP.string;
        return !currentIP.equals(lastIP);
    }

    public void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ACTIVATEACCOUNTERRORHANDLING, JDL.L("plugins.hoster.uploadedto.activateExperimentalFreeAccountErrorhandling", "Activate experimental free account errorhandling: Swith between free accounts instead of reconnecting if a limit is reached.")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), EXPERIMENTALHANDLING, JDL.L("plugins.hoster.uploadedto.activateExperimentalReconnectHandling", "Activate experimental reconnect handling for freeusers: Prevents having to enter captchas in between downloads.")).setDefaultValue(false));
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