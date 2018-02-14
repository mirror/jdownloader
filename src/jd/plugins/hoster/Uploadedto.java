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
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.crypt.Base64;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountUnavailableException;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.locale.JDL;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploaded.to" }, urls = { "https?://(www\\.)?(uploaded\\.(to|net)/(file/|\\?id=)?[\\w]+|ul\\.to/(file/|\\?id=)?[\\w]+)" })
public class Uploadedto extends PluginForHost {
    // DEV NOTES:
    // other: respects https in download methods, even though final download
    // link isn't https (free tested).
    /* Enable/disable usage of multiple free accounts at the same time */
    private static final boolean           ACCOUNT_FREE_CONCURRENT_USAGE_POSSIBLE    = true;
    private static final boolean           ACCOUNT_PREMIUM_CONCURRENT_USAGE_POSSIBLE = true;
    private static final int               ACCOUNT_FREE_MAXDOWNLOADS                 = 1;
    /* note: CAN NOT be negative or zero! (ie. -1 or 0) Otherwise math sections fail. .:. use [1-20] */
    private static AtomicInteger           totalMaxSimultanFreeDownload              = new AtomicInteger(20);
    /* don't touch the following! */
    private static AtomicInteger           maxFree                                   = new AtomicInteger(1);
    private static final int               ACCOUNT_PREMIUM_MAXDOWNLOADS              = -1;
    private static AtomicInteger           maxPrem                                   = new AtomicInteger(1);
    // spaces will be '_'(checkLinks) and ' '(requestFileInformation), '_' stay '_'
    private char[]                         FILENAMEREPLACES                          = new char[] { ' ', '_', '[', ']' };
    /* Reconnect-workaround-related */
    private static final long              FREE_RECONNECTWAIT                        = 3 * 60 * 60 * 1000L;
    private String                         PROPERTY_LASTIP                           = "UPLOADEDNET_PROPERTY_LASTIP";
    private static final String            PROPERTY_LASTDOWNLOAD                     = "uploadednet_lastdownload_timestamp";
    private final String                   ACTIVATEACCOUNTERRORHANDLING              = "ACTIVATEACCOUNTERRORHANDLING";
    private final String                   EXPERIMENTALHANDLING                      = "EXPERIMENTALHANDLING";
    private Pattern                        IPREGEX                                   = Pattern.compile("(([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9]))", Pattern.CASE_INSENSITIVE);
    private static AtomicReference<String> lastIP                                    = new AtomicReference<String>();
    private static AtomicReference<String> currentIP                                 = new AtomicReference<String>();
    private static HashMap<String, Long>   blockedIPsMap                             = new HashMap<String, Long>();
    private static Object                  CTRLLOCK                                  = new Object();
    private static String[]                IPCHECK                                   = new String[] { "http://ipcheck0.jdownloader.org", "http://ipcheck1.jdownloader.org", "http://ipcheck2.jdownloader.org", "http://ipcheck3.jdownloader.org" };
    private static AtomicBoolean           usePremiumAPI                             = new AtomicBoolean(true);
    private static final String            NOCHUNKS                                  = "NOCHUNKS";
    private static final String            NORESUME                                  = "NORESUME";
    private static final String            SSL_CONNECTION                            = "SSL_CONNECTION";
    private static final String            PREFER_PREMIUM_DOWNLOAD_API               = "PREFER_PREMIUM_DOWNLOAD_API_V2";
    private static final String            DOWNLOAD_ABUSED                           = "DOWNLOAD_ABUSED";
    private static final String            DISABLE_START_INTERVAL                    = "DISABLE_START_INTERVAL";
    private static final String            EXPERIMENTAL_MULTIFREE                    = "EXPERIMENTAL_MULTIFREE2";
    private boolean                        PREFERSSL                                 = true;
    private boolean                        avoidHTTPS                                = false;
    private static final String            UPLOADED_FINAL_FILENAME                   = "UPLOADED_FINAL_FILENAME";
    private static final String            CURRENT_DOMAIN                            = "http://uploaded.net/";
    private static final String            HTML_MAINTENANCE                          = ">uploaded\\.net - Maintenance|Dear User, Uploaded is in maintenance mode|Lieber Kunde, wir führen kurze Wartungsarbeiten durch";

    private String getProtocol() {
        if (avoidHTTPS) {
            return "http://";
        }
        if (getPluginConfig().getBooleanProperty(SSL_CONNECTION, PREFERSSL)) {
            /**
             * add this do disable https for old java 1.6 (dh > 1024)
             *
             * && Application.getJavaVersion() >= Application.JAVA17
             **/
            return "https://";
        } else {
            return "http://";
        }
    }

    @Override
    public String rewriteHost(String host) {
        if (host == null || "uploaded.net".equals(host) || "uploaded.to".equals(host) || "ul.to".equals(host)) {
            return "uploaded.to";
        }
        return super.rewriteHost(host);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        String protocol = new Regex(link.getDownloadURL(), "(https?)://").getMatch(0);
        String id = getID(link);
        link.setLinkID(getHost() + "://" + id);
        link.setUrlDownload(protocol + "://uploaded.net/file/" + id);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException, InterruptedException {
        this.correctDownloadLink(downloadLink);
        final String id = getID(downloadLink);
        boolean red = br.isFollowingRedirects();
        br.setFollowRedirects(false);
        try {
            try {
                getPage(br, getProtocol() + "uploaded.net/file/" + id + "/status");
            } catch (BrowserException e) {
                if (e.getRequest() != null && e.getRequest().getHttpConnection().getResponseCode() == 451) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                throw e;
            }
            final String ret = br.getRedirectLocation();
            if (ret != null) {
                if (ret.contains("/404")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (ret.contains("/410")) {
                    if (dmcaDlEnabled()) {
                        return AvailableStatus.UNCHECKABLE;
                    } else {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "The requested file isn't available anymore (410)!");
                    }
                }
                br.getPage(ret);
            }
            final Request request = br.getRequest();
            if (!StringUtils.endsWithCaseInsensitive(request.getUrl(), "/status") || request.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String name = br.getRegex("(.*?)(\r|\n)").getMatch(0);
            if (StringUtils.startsWithCaseInsensitive(name, "<!DOCTYPE html PUBLIC")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String size = br.getRegex("[\r\n]([0-9\\, TGBMK]+)").getMatch(0);
            if (name == null || size == null) {
                checkGeneralErrors();
                return AvailableStatus.UNCHECKABLE;
            }
            name = name.trim();
            if (StringUtils.contains(name, "filename*")) {
                final String fakeHeader = " ".concat(name.substring(name.indexOf("filename*")));
                final String contentName = HTTPConnectionUtils.getFileNameFromDispositionHeader(fakeHeader);
                if (contentName != null) {
                    name = contentName;
                } else {
                    try {
                        name = URLDecoder.decode(name, "UTF-8");
                    } catch (final Throwable e) {
                    }
                }
            } else {
                try {
                    name = URLDecoder.decode(name, "UTF-8");
                } catch (final Throwable e) {
                }
            }
            setFinalFileName(downloadLink, name);
            downloadLink.setDownloadSize(SizeFormatter.getSize(size));
        } finally {
            br.setFollowRedirects(red);
        }
        return AvailableStatus.TRUE;
    }

    private String fixFileName(final String fileName) {
        if (fileName != null && fileName.endsWith("\";")) {
            return fileName.substring(0, fileName.length() - 2);
        }
        return fileName;
    }

    private void getPage(Browser br, String url) throws IOException, PluginException, InterruptedException {
        br.getPage(url);
        for (int i = 0; i < 50; i++) {
            boolean isHTTPS = url.startsWith("https");
            String redirect = br.getRedirectLocation();
            boolean followRedirect = false;
            if (redirect != null) {
                String urlA = br.getURL().replaceFirst("https:", "http:");
                String urlB = redirect.replaceFirst("https:", "http:");
                followRedirect = urlA.equals(urlB);
            }
            if (followRedirect) {
                if (isHTTPS && !redirect.startsWith("https")) {
                    logger.info("Downgrading https to http!");
                    avoidHTTPS = true;
                }
                Thread.sleep(100 + (i / 10) * 200);
                br.getPage(redirect);
            } else {
                return;
            }
        }
        if (br.getRedirectLocation() != null) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "API Error. Please contact Uploaded.to Support.", 5 * 60 * 1000l);
        }
    }

    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if ((account == null || account.getBooleanProperty("free", false)) && downloadLink.getVerifiedFileSize() > 1073741824) {
            return false;
        } else {
            return true;
        }
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

        @SuppressWarnings("deprecation")
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

    protected void showFreeDialog(final String domain) {
        if (System.getProperty("org.jdownloader.revision") != null) { /* JD2 ONLY! */
            super.showFreeDialog(domain);
        } else {
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
                                if (JOptionPane.OK_OPTION == result) {
                                    CrossSystem.openURL(new URL("http://update3.jdownloader.org/jdserv/BuyPremiumInterface/redirect?ul.to&freedialog"));
                                }
                            }
                        } catch (Throwable e) {
                        }
                    }
                });
            } catch (Throwable e) {
            }
        }
    }

    @SuppressWarnings("deprecation")
    public Uploadedto(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.enablePremium("http://uploaded.to/");
    }

    protected long getStartIntervall(final DownloadLink downloadLink, final Account account) {
        if (!this.getPluginConfig().getBooleanProperty(DISABLE_START_INTERVAL, false) && downloadLink != null) {
            final long verifiedFileSize = downloadLink.getVerifiedFileSize();
            if (account != null) {
                if (verifiedFileSize >= 50 * 1000 * 1000l) {
                    return 1000;
                }
                return 150;
            }
        }
        return 0;
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
        if (urls == null || urls.length == 0) {
            return false;
        }
        for (DownloadLink link : urls) {
            correctDownloadLink(link);
        }
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
                    if (index == urls.length || links.size() > 80) {
                        break;
                    }
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
                    if (retry == 5) {
                        return false;
                    }
                    postPage(br, getProtocol() + "uploaded.net/api/filemultiple", sb.toString());
                    if (br.containsHTML(HTML_MAINTENANCE)) {
                        return false;
                    }
                    if (br.getHttpConnection().getLongContentLength() != 20) {
                        break;
                    } else {
                        try {
                            br.getHttpConnection().disconnect();
                        } catch (final Throwable e) {
                        }
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
                        String name = infos[hit][4].trim();
                        if (StringUtils.contains(name, "filename*")) {
                            final String fakeHeader = " ".concat(name.substring(name.indexOf("filename*")));
                            final String contentName = HTTPConnectionUtils.getFileNameFromDispositionHeader(fakeHeader);
                            if (contentName != null) {
                                name = contentName;
                            } else {
                                try {
                                    name = URLDecoder.decode(name, "UTF-8");
                                } catch (final Throwable e) {
                                }
                            }
                        } else {
                            try {
                                name = URLDecoder.decode(name, "UTF-8");
                            } catch (final Throwable e) {
                            }
                        }
                        setFinalFileName(dl, name);
                        final long size = SizeFormatter.getSize(infos[hit][2]);
                        if (dl.getVerifiedFileSize() == -1 && size > 0) {
                            dl.setVerifiedFileSize(size);
                        }
                        if ("online".equalsIgnoreCase(infos[hit][0].trim())) {
                            dl.setAvailable(true);
                            String sha1 = infos[hit][3].trim();
                            if (sha1.length() == 0) {
                                sha1 = null;
                            }
                            dl.setSha1Hash(sha1);
                        } else {
                            if (!dmcaDlEnabled()) {
                                dl.setAvailable(false);
                            }
                        }
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (Exception e) {
            return false;
        } finally {
            try {
                br.getHttpConnection().disconnect();
            } catch (final Throwable e) {
            }
        }
        return true;
    }

    private void setFinalFileName(DownloadLink downloadLink, final String name) {
        if (downloadLink.getBooleanProperty(UPLOADED_FINAL_FILENAME, false) == false) {
            downloadLink.setFinalFileName(fixFileName(name));
        }
    }

    private boolean preferAPI(final Account account) {
        if (account == null) {
            return this.getPluginConfig().getBooleanProperty(PREFER_PREMIUM_DOWNLOAD_API, default_ppda);
        } else {
            if (this.getPluginConfig().getBooleanProperty(PREFER_PREMIUM_DOWNLOAD_API, default_ppda)) {
                return !StringUtils.equals(account.getPass(), account.getStringProperty("NOAPI", null));
            }
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (usePremiumAPI.get() && preferAPI(account)) {
            try {
                // This password won't work: FLR&Y$9i,?+yk=Kx08}:PhkmÖ]nmYAr#n6O=xHiZzm,NI&k)Qü
                return api_Fetch_accountinfo(account);
            } catch (Exception e) {
                // for password that cause getLoginSHA1Hash to fail.
                getLogger().log(e);
                return site_Fetch_accountinfo(account);
            }
        } else {
            return site_Fetch_accountinfo(account);
        }
    }

    @SuppressWarnings("deprecation")
    public AccountInfo api_Fetch_accountinfo(final Account account) throws Exception {
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
                    account.setValid(true);
                    /* free user */
                    ai.setUnlimitedTraffic();
                    ai.setValidUntil(-1);
                    ai.setStatus("Free account");
                    account.setProperty("free", true);
                    account.setType(AccountType.FREE);
                } else if ("premium".equals(tokenType)) {
                    String traffic = br.getRegex("traffic_left\":\\s*?\"?(\\d+)").getMatch(0);
                    long max = 100 * 1024 * 1024 * 1024l;
                    long current = Long.parseLong(traffic);
                    ai.setTrafficMax(Math.max(max, current));
                    ai.setTrafficLeft(current);
                    String expireDate = br.getRegex("account_premium\":\\s*?\"?(\\d+)").getMatch(0);
                    ai.setValidUntil(Long.parseLong(expireDate) * 1000);
                    if (current <= 0 || br.containsHTML("download_available\":false")) {
                        final boolean downloadAvailable = br.containsHTML("download_available\":true");
                        logger.info("Download_available: " + br.containsHTML("download_available\":true"));
                        String refreshIn = br.getRegex("traffic_reset\":\\s*?(\\d+)").getMatch(0);
                        if (refreshIn != null) {
                            throw new AccountUnavailableException("DownloadAvailable:" + downloadAvailable, Long.parseLong(refreshIn) * 1000);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "DownloadAvailable:" + downloadAvailable, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                        }
                    }
                    ai.setStatus("Premium account");
                    account.setProperty("free", false);
                    account.setType(AccountType.PREMIUM);
                    if (!ai.isExpired()) {
                        account.setValid(true);
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        } catch (final PluginException e) {
            if (e.getLinkStatus() != LinkStatus.ERROR_PREMIUM) {
                if (usePremiumAPI.compareAndSet(true, false)) {
                    getLogger().info("Disable API");
                }
            }
            account.setProperty("token", null);
            account.setProperty("tokenType", null);
            throw e;
        } catch (final Exception e) {
            if (usePremiumAPI.compareAndSet(true, false)) {
                getLogger().info("Disable API");
            }
            account.setProperty("token", null);
            account.setProperty("tokenType", null);
            throw e;
        }
        return ai;
    }

    @SuppressWarnings("deprecation")
    public AccountInfo site_Fetch_accountinfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        prepBrowser();
        site_login(account, true);
        postPage(br, getProtocol() + "uploaded.net/status", "uid=" + Encoding.urlEncode(account.getUser()) + "&upw=" + Encoding.urlEncode(account.getPass()));
        if (br.containsHTML("blocked")) {
            ai.setStatus("Too many failed logins! Wait 15 mins");
            account.setTempDisabled(true);
            return ai;
        }
        if (br.containsHTML("wrong password")) {
            ai.setStatus("Wrong password | Ungültiges Passwort");
            account.setValid(false);
            return ai;
        }
        if (br.containsHTML("wrong user")) {
            ai.setStatus("Wrong username | Ungültiger Benutzername");
            account.setValid(false);
            return ai;
        }
        String isPremium = br.getMatch("status: (premium)");
        if (isPremium == null) {
            account.setValid(true);
            ai.setStatus("Free account");
            ai.setUnlimitedTraffic();
            try {
                maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
                account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
                account.setConcurrentUsePossible(ACCOUNT_FREE_CONCURRENT_USAGE_POSSIBLE);
            } catch (final Throwable e) {
            }
            account.setProperty("free", true);
            if (preferAPI(account)) {
                account.setProperty("NOAPI", account.getPass());
            }
        } else {
            account.setValid(true);
            String traffic = br.getMatch("traffic: (\\d+)");
            String expire = br.getMatch("expire: (\\d+)");
            if (expire != null) {
                ai.setValidUntil(Long.parseLong(expire) * 1000);
            }
            ai.setStatus("Premium account");
            long max = 100 * 1024 * 1024 * 1024l;
            long current = Long.parseLong(traffic);
            ai.setTrafficMax(Math.max(max, current));
            ai.setTrafficLeft(current);
            try {
                maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
                account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
                account.setConcurrentUsePossible(ACCOUNT_PREMIUM_CONCURRENT_USAGE_POSSIBLE);
            } catch (final Throwable e) {
            }
            account.setProperty("free", false);
            if (preferAPI(account)) {
                account.setProperty("NOAPI", account.getPass());
            }
        }
        return ai;
    }

    private void postPage(Browser br, String url, String data) throws IOException, PluginException, InterruptedException {
        br.postPage(url, data);
        for (int i = 0; i < 25; i++) {
            boolean isHTTPS = url.startsWith("https");
            String redirect = br.getRedirectLocation();
            boolean followRedirect = false;
            if (redirect != null) {
                String urlA = br.getURL().replaceFirst("https:", "http:");
                String urlB = redirect.replaceFirst("https:", "http:");
                followRedirect = urlA.equals(urlB);
            }
            if (followRedirect) {
                if (isHTTPS && !redirect.startsWith("https")) {
                    logger.info("Downgrading https to http!");
                    avoidHTTPS = true;
                }
                logger.info("Redirect Wait");
                Thread.sleep(100);
                br.postPage(redirect, data);
            } else {
                return;
            }
        }
        if (br.getRedirectLocation() != null) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "API Error. Please contact Uploaded.to Support.", 5 * 60 * 1000l);
        }
    }

    @Override
    public String getAGBLink() {
        return "http://uploaded.net/legal";
    }

    @SuppressWarnings("deprecation")
    private String getID(final DownloadLink downloadLink) {
        String id = new Regex(downloadLink.getDownloadURL(), "/file/([\\w]+)/?").getMatch(0);
        if (id != null) {
            return id;
        }
        id = new Regex(downloadLink.getDownloadURL(), "\\?id=([\\w]+)/?").getMatch(0);
        if (id != null) {
            return id;
        }
        id = new Regex(downloadLink.getDownloadURL(), "(\\.net|\\.to)/([\\w]+)/?").getMatch(1);
        return id;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxFree.get();
    }

    private String getPassword(final DownloadLink downloadLink) throws Exception {
        String passCode = downloadLink.getStringProperty("pass", null);
        if (passCode == null) {
            passCode = getUserInput(null, downloadLink);
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

    @SuppressWarnings({ "deprecation", "unchecked" })
    public void doFree(final DownloadLink downloadLink, final Account account) throws Exception {
        currentIP.set(this.getIP());
        if (account == null) {
            logger.info("Free, WEB download method in use!");
            synchronized (CTRLLOCK) {
                /* Load list of saved IPs + timestamp of last download */
                final Object lastdownloadmap = this.getPluginConfig().getProperty(PROPERTY_LASTDOWNLOAD);
                if (lastdownloadmap != null && lastdownloadmap instanceof HashMap && blockedIPsMap.isEmpty()) {
                    blockedIPsMap = (HashMap<String, Long>) lastdownloadmap;
                }
            }
        } else {
            // good to know account been used.
            logger.info("Free account, WEB download method in use!");
        }
        String baseURL = getProtocol() + "uploaded.net/";
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        workAroundTimeOut(br);
        String id = getID(downloadLink);
        br.setFollowRedirects(false);
        prepBrowser();
        /**
         * Free-Account Errorhandling: This allows users to switch between free accounts instead of reconnecting when a limit is reached
         */
        long lastdownload = 0;
        long passedTimeSinceLastDl = 0;
        logger.info("New Download: currentIP = " + currentIP.get());
        /**
         * Experimental reconnect handling to prevent having to enter a captcha just to see that a limit has been reached!
         */
        if (this.getPluginConfig().getBooleanProperty(EXPERIMENTALHANDLING, default_eh)) {
            /*
             * If the user starts a download in free (unregistered) mode the waittime is on his IP. This also affects free accounts if he
             * tries to start more downloads via free accounts afterwards BUT nontheless the limit is only on his IP so he CAN download
             * using the same free accounts after performing a reconnect!
             */
            lastdownload = getPluginSavedLastDownloadTimestamp();
            passedTimeSinceLastDl = System.currentTimeMillis() - lastdownload;
            if (passedTimeSinceLastDl < FREE_RECONNECTWAIT) {
                logger.info("Experimental handling active --> There still seems to be a waittime on the current IP --> ERROR_IP_BLOCKED");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, FREE_RECONNECTWAIT - passedTimeSinceLastDl);
            }
        }
        if (account != null && this.getPluginConfig().getBooleanProperty(ACTIVATEACCOUNTERRORHANDLING, default_aaeh)) {
            /* User has no limit on his IP in general --> Check if the current account has a limit. */
            lastdownload = getLongProperty(account, PROPERTY_LASTDOWNLOAD, 0);
            passedTimeSinceLastDl = System.currentTimeMillis() - lastdownload;
            if (passedTimeSinceLastDl < FREE_RECONNECTWAIT) {
                /**
                 * Experimental reconnect handling to prevent having to enter a captcha just to see that a limit has been reached!
                 */
                /* IP was changed - now we only have to switch to the next account! */
                logger.info("IP has changed -> Disabling current free account to try to use the next free account or free unregistered mode");
                throw new AccountUnavailableException("Free limit reached", 3 * 60 * 60 * 1000l);
            }
        }
        final String addedDownloadlink = baseURL + "file/" + id;
        getPage(br, addedDownloadlink);
        String dllink = null;
        String redirect = br.getRedirectLocation();
        if (redirect != null) {
            if (redirect.contains("/404")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            dllink = redirect;
            logger.info("Maybe direct download");
        }
        if (dllink == null) {
            generalFreeErrorhandling(account);
            String passCode = null;
            if (br.containsHTML("<h2>Authentification</h2>")) {
                logger.info("Password protected link");
                passCode = getPassword(downloadLink);
                if (passCode == null || passCode.equals("")) {
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Password wrong!");
                }
                postPage(br, br.getURL(), "pw=" + Encoding.urlEncode(passCode));
                if (br.containsHTML("<h2>Authentification</h2>")) {
                    downloadLink.setProperty("pass", null);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Password wrong!");
                }
                downloadLink.setProperty("pass", passCode);
            }
            // free account might not have captcha...
            if (dllink == null) {
                dllink = br.getRegex("(\"|')(https?://[a-z0-9\\-]+\\.(uploaded\\.net|uploaded\\.to)/dl/[a-z0-9\\-]+)\\1").getMatch(1);
            }
            final Browser brc = br.cloneBrowser();
            getPage(brc, baseURL + "js/download.js");
            String siteKey = brc.getRegex("'sitekey'\\s*:\\s*'(.*?)'").getMatch(0);
            if (siteKey == null) {
                if (brc.containsHTML("<title></title>")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
                }
                logger.warning("Sitekey not found! Use known one!");
                siteKey = "6Le1WUIUAAAAAG0gEh0atRevv3TT-WP4HW8FLMoe";
            }
            int wait = 30;
            final String waitTime = br.getRegex("<span>Current waiting period: <span>(\\d+)</span> seconds</span>").getMatch(0);
            if (waitTime != null) {
                wait = Integer.parseInt(waitTime);
            }
            final String rcv1 = brc.getRegex("Recaptcha\\.create\\(\"([^<>\"]*?)\"").getMatch(0);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            postPage(br, baseURL + "io/ticket/slot/" + getID(downloadLink), "");
            if (!br.containsHTML("\\{succ:true\\}")) {
                if (br.containsHTML("File not found")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final long timebefore = System.currentTimeMillis();
            Recaptcha rc1 = null;
            for (int i = 0; i <= 15; i++) {
                if (rcv1 != null) {
                    if (rc1 == null) {
                        rc1 = new Recaptcha(br, this);
                        rc1.setId(rcv1);
                        rc1.load();
                    }
                    final File cf = rc1.downloadCaptcha(getLocalCaptchaFile());
                    String c = getCaptchaCode("recaptcha", cf, downloadLink);
                    if (c == null || c.length() == 0) {
                        rc1.reload();
                        continue;
                    }
                    final int passedTime = (int) ((System.currentTimeMillis() - timebefore) / 1000) - 1;
                    if (i == 0 && passedTime < wait) {
                        sleep((wait - passedTime) * 1001l, downloadLink);
                    }
                    postPage(br, baseURL + "io/ticket/captcha/" + getID(downloadLink), "recaptcha_challenge_field=" + rc1.getChallenge() + "&recaptcha_response_field=" + c);
                } else {
                    final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br, siteKey);
                    final String recaptchaV2Response = rc2.getToken();
                    if (recaptchaV2Response == null) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    final int passedTime = (int) ((System.currentTimeMillis() - timebefore) / 1000) - 1;
                    if (i == 0 && passedTime < wait) {
                        sleep((wait - passedTime) * 1001l, downloadLink);
                    }
                    postPage(br, baseURL + "io/ticket/captcha/" + getID(downloadLink), "g-recaptcha-response=" + recaptchaV2Response);
                }
                if (br.containsHTML("\"err\":\"captcha\"")) {
                    if (rc1 != null) {
                        rc1.reload();
                    }
                    continue;
                }
                break;
            }
            if (br.containsHTML("\\{succ:true\\}") || br.containsHTML("\"err\":\"captcha\"")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            generalFreeErrorhandling(account);
            if (br.containsHTML("limit\\-parallel")) {
                freeDownloadlimitReached("You're already downloading");
            }
            dllink = br.getRegex("url:'(http.*?)'").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("url:'(dl/.*?)'").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("(\"|')(https?://[a-z0-9\\-]+\\.(uploaded\\.net|uploaded\\.to)/dl/[a-z0-9\\-]+)\\1").getMatch(1);
                    if (dllink == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
        }
        /* The download attempt already triggers reconnect waittime! Save timestamp here to calculate correct remaining waittime later! */
        synchronized (CTRLLOCK) {
            if (account != null) {
                account.setProperty(PROPERTY_LASTDOWNLOAD, System.currentTimeMillis());
            } else {
                blockedIPsMap.put(currentIP.get(), System.currentTimeMillis());
                getPluginConfig().setProperty(PROPERTY_LASTDOWNLOAD, blockedIPsMap);
            }
            setIP(downloadLink, account);
        }
        if ("http://".equals(getProtocol()) && StringUtils.startsWithCaseInsensitive(dllink, "https")) {
            dllink = dllink.replaceFirst("https://", "http://");
        }
        dl = BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        try {
            /* remove next major update */
            /* workaround for broken timeout in 0.9xx public */
            ((RAFDownload) dl).getRequest().setConnectTimeout(30000);
            ((RAFDownload) dl).getRequest().setReadTimeout(60000);
        } catch (final Throwable ee) {
        }
        handleGeneralServerErrors();
        if (!dl.getConnection().isContentDisposition()) {
            try {
                br.followConnection();
            } catch (final Throwable e) {
                logger.severe(e.getMessage());
            }
            logger.info(br.toString());
            generalFreeErrorhandling(account);
            if (br.containsHTML("please try again in an hour or purchase one of our")) {
                freeDownloadlimitReached(null);
            }
            checkGeneralErrors();
            if ("No htmlCode read".equalsIgnoreCase(br.toString())) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 30 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (downloadLink.getBooleanProperty(UPLOADED_FINAL_FILENAME, false) == false) {
            final String uploadedFinalFileName = Plugin.getFileNameFromDispositionHeader(dl.getConnection());
            if (StringUtils.isNotEmpty(uploadedFinalFileName)) {
                downloadLink.setFinalFileName(uploadedFinalFileName);
                downloadLink.setProperty(UPLOADED_FINAL_FILENAME, true);
            }
        }
        final boolean multiFree = getPluginConfig().getBooleanProperty(EXPERIMENTAL_MULTIFREE, false);
        try {
            if (multiFree) {
                /* add a download slot */
                controlFree(+1);
            }
            /* start the dl */
            dl.startDownload();
        } finally {
            if (multiFree) {
                /* remove download slot */
                controlFree(-1);
            }
        }
    }

    /**
     * Prevents more than one free download from starting at a given time. One step prior to dl.startDownload(), it adds a slot to maxFree
     * which allows the next singleton download to start, or at least try.
     *
     * This is needed because xfileshare(website) only throws errors after a final dllink starts transferring or at a given step within pre
     * download sequence. But this template(XfileSharingProBasic) allows multiple slots(when available) to commence the download sequence,
     * this.setstartintival does not resolve this issue. Which results in x(20) captcha events all at once and only allows one download to
     * start. This prevents wasting peoples time and effort on captcha solving and|or wasting captcha trading credits. Users will experience
     * minimal harm to downloading as slots are freed up soon as current download begins.
     *
     * @param controlFree
     *            (+1|-1)
     */
    private synchronized void controlFree(final int num) {
        logger.info("maxFree was = " + maxFree.get());
        maxFree.set(Math.min(Math.max(1, maxFree.addAndGet(num)), totalMaxSimultanFreeDownload.get()));
        logger.info("maxFree now = " + maxFree.get());
    }

    /** Handles error-responsecodes coming from the connection of our DownloadInterface. */
    private void handleGeneralServerErrors() throws PluginException {
        final int responsecode = dl.getConnection().getResponseCode();
        if (responsecode == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 10 * 60 * 1000l);
        } else if (responsecode == 404) {
            /* We used to throw FILE_NOT_FOUND here in the past. */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
        } else if (responsecode == 508) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 508", 30 * 60 * 1000l);
        }
    }

    private void generalFreeErrorhandling(final Account account) throws PluginException {
        /* "err" strings: */
        if (br.containsHTML("\"err\":\"This file exceeds the max")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        if (br.containsHTML("\"err\":\"Sharing this type of file is prohibited")) {
            logger.info("File offline after captcha...");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("sorry but all of our available download slots are busy currently")) {
            /* Not yet clear if this happens for single links or if this means that globally free dls are not possible at the moment... */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No free Free-User Slots! Get a premium account or wait!", 5 * 60 * 1000l);
        }
        if (br.containsHTML("\"err\":\"Internal error\"")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Internal error'", 5 * 60 * 1000l);
        }
        /* "err" strings end */
        if (br.containsHTML("You have reached the max\\. number of possible free downloads|err\":\"limit\\-dl\"")) {
            if (account == null) {
                logger.info("Limit reached, throwing reconnect exception");
                freeDownloadlimitReached(null);
            } else {
                logger.info("Limit reached, disabling free account to use the next one!");
                account.setProperty(PROPERTY_LASTDOWNLOAD, System.currentTimeMillis());
                throw new AccountUnavailableException(3 * 60 * 60 * 1000l);
            }
        }
        checkGeneralErrors();
    }

    /** API error handling **/
    private void handleErrorCode(Browser br, Account acc, String usedToken, boolean throwPluginDefect) throws Exception {
        final String lang = System.getProperty("user.language");
        String errCode = br.getRegex("code\":\\s*?\"?(\\d+)").getMatch(0);
        if (errCode == null) {
            errCode = br.getRegex("errCode\":\\s*?\"?(\\d+)").getMatch(0);
        }
        String message = br.getRegex("message\":\"([^\"]+)").getMatch(0);
        if (message == null) {
            message = br.getRegex("err\":\\[\"([^\"]+)\"\\]").getMatch(0);
        }
        if (message != null) {
            message = Encoding.unicodeDecode(message);
        }
        if (errCode != null) {
            logger.info("ErrorCode: " + errCode);
            int code = Integer.parseInt(errCode);
            switch (code) {
            case 1:
                // {"err":{"code":1,"message":"Benutzer nicht vorhanden: e74ac48bef744497c56efaf45072579fbc945b45"}}
                // user does not exist, when random username entered into login field.
            case 2:
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUser does not exist!\r\nBenutzername existiert nicht!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 3: {
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
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
                if (acc != null) {
                    throw new AccountUnavailableException("Disabled because of flood protection", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Disabled because of flood protection", 60 * 60 * 1000l);
                }
            case 18:
                // {"err":{"code":18,"message":"Das \u00fcbergebene Passwort ist vom Typ sha1, erwartet wurde md5"}}
                // messaged unescaped: Das übergebene Passwort ist vom Typ sha1, erwartet wurde md5
                // effectively they are saying wrong hash value provided, sha1 provided and expected md5. been reported by users, seems
                // random for some users and not others, when sha1 was used
            case 19:
                // {"err":{"code":19,"message":"Das \u00fcbergebene Passwort ist vom Typ md5, erwartet wurde sha1"}}
                // message unescaped: Das übergebene Passwort ist vom Typ md5, erwartet wurde sha1
                // effectively they are saying wrong hash value provided, md5 provided and expected sha1. It also seems to throws this been
                // given randomly for some users and not others, when md5 was used (only used for a day to test)
            case 20:
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Locked account!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 404:
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "API error 404", 30 * 60 * 1000l);
            case 410:
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            case 500:
                logger.info("Received unknown API response error 500!\nIf this happened during the login process, the account was not accepted!");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "API doesn't accept account (error 500).", PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 8000:
                //
                /* traffic exhausted */
                if (acc != null) {
                    String reset = br.getRegex("reset\":\\s*?\"?(\\d+)").getMatch(0);
                    if (reset != null) {
                        throw new AccountUnavailableException(Long.parseLong(reset) * 1000);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            case 8011:
                /* direct download but upload user deleted */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Upload User deleted");
            case 8013:
                // {"err":["Leider haben wir Zugriffe von zu vielen verschiedenen IPs auf Ihren Account feststellen k&#246;nnen,
                // Account-Sharing ist laut unseren AGB strengstens untersagt. Sie k&#246;nnen f&#252;r den heutigen Tag leider keine
                // Premium-Downloads mehr starten."],"errCode":8013}
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Your account been flagged for 'Account sharing', Please contact " + this.getHost() + " support for resolution.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 8016:
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server in maintenance", 20 * 60 * 1000l);
            case 8017:
                /* file is probably prohibited */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        checkGeneralErrors();
        if (throwPluginDefect) {
            logger.info("ErrorCode: unknown\r\n" + br);
            if (usePremiumAPI.get() && preferAPI(acc)) {
                usePremiumAPI.set(false);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private void checkGeneralErrors() throws PluginException {
        if (br.containsHTML(HTML_MAINTENANCE)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server in maintenance", 20 * 60 * 1000l);
        }
        if (br.containsHTML("try again later")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 30 * 60 * 1000l);
        }
        if (br.containsHTML("File not found\\!")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("No connection to database")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'No connection to database'", 10 * 60 * 1000l);
        }
        if (br.containsHTML(">Internal error<")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 15 * 60 * 1000l);
        }
        // temp issue...?
        if (br.containsHTML("<p[^>]*>\\s*File not available!\\s*</p>")) {
            final String time = br.getRegex("In <b id=\"tout\">(\\d+)</b> Sekunden wird ein erneuter Versuch gestartet&hellip;\\s*</p>").getMatch(0);
            if (time != null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Download not possible at the moment'", (Long.parseLong(time) + 10) * 1001l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Download not possible at the moment'", 5 * 60 * 1000l);
            }
        }
        if ((br.containsHTML("Aus technischen Gr") && br.containsHTML("ist ein Download momentan nicht m")) || br.containsHTML("download this file due to technical issues at the moment")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Download not possible at the moment'", 30 * 60 * 1000l);
        }
        if (br.getURL().contains("view=error")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 10 * 60 * 1000l);
        }
    }

    private String api_getAccessToken(Account account, boolean liveToken) throws Exception {
        synchronized (account) {
            try {
                // DANGER: Even after user changed password this token is still valid->Uploaded.to was contacted by psp but no response!
                String token = account.getStringProperty("token", null);
                if (token != null && liveToken == false) {
                    return token;
                }
                /** URLDecoder can make the password invalid or throw an IllegalArgumentException */
                // JDHash.getSHA1(URLDecoder.decode(account.getPass(), "UTF-8").toLowerCase(Locale.ENGLISH))
                postPage(br, getProtocol() + "api.uploaded.net/api/user/login", "name=" + Encoding.urlEncode(account.getUser()) + "&pass=" + getLoginSHA1Hash(account.getPass()) + "&ishash=1&app=JDownloader");
                token = br.getRegex("access_token\":\"(.*?)\"").getMatch(0);
                if (token == null) {
                    //
                    handleErrorCode(br, account, token, true);
                }
                account.setProperty("token", token);
                return token;
            } catch (final PluginException e) {
                account.setProperty("token", null);
                account.setProperty("tokenType", null);
                throw e;
            } catch (final Exception e) {
                if (usePremiumAPI.compareAndSet(true, false)) {
                    getLogger().info("Disable API");
                }
                account.setProperty("token", null);
                account.setProperty("tokenType", null);
                throw e;
            }
        }
    }

    private static String byteArrayToHex(final byte[] digest) {
        final StringBuilder ret = new StringBuilder(digest.length * 2);
        String tmp;
        for (final byte d : digest) {
            tmp = Integer.toHexString(d & 0xFF);
            if (tmp.length() < 2) {
                ret.append('0');
            }
            ret.append(tmp);
        }
        return ret.toString();
    }

    private static String urlDecode(String s, String enc) throws UnsupportedEncodingException {
        final int numChars = s.length();
        final StringBuilder sb = new StringBuilder(numChars > 500 ? numChars / 2 : numChars);
        int i = 0;
        if (enc == null || enc.length() == 0) {
            throw new UnsupportedEncodingException("URLDecoder: empty string enc parameter");
        }
        char c;
        byte[] bytes = null;
        loop: while (i < numChars) {
            c = s.charAt(i);
            switch (c) {
            case '+':
                sb.append(' ');
                i++;
                break;
            case '%':
                /*
                 * Starting with this instance of %, process all consecutive substrings of the form %xy. Each substring %xy will yield a
                 * byte. Convert all consecutive bytes obtained this way to whatever character(s) they represent in the provided encoding.
                 */
                int pos = 0;
                while (((i + 2) < numChars) && (c == '%')) {
                    final String subString = s.substring(i + 1, i + 3);
                    int v = -1;
                    try {
                        v = Integer.parseInt(subString, 16);
                    } catch (NumberFormatException e) {
                    }
                    if (v < 0) {
                        if (bytes != null && pos > 0) {
                            sb.append(new String(bytes, 0, pos, enc));
                        }
                        sb.append("%");
                        sb.append(subString);
                        i += 3;
                        continue loop;
                    }
                    if (bytes == null) {
                        bytes = new byte[(numChars - i) / 3];
                    }
                    bytes[pos++] = (byte) v;
                    i += 3;
                    if (i < numChars) {
                        c = s.charAt(i);
                    }
                }
                // A trailing, incomplete byte encoding such as
                // "%x" will cause an exception to be thrown
                if (bytes != null && pos > 0) {
                    sb.append(new String(bytes, 0, pos, enc));
                }
                if ((i < numChars) && (c == '%')) {
                    sb.append(c);
                    i++;
                }
                break;
            default:
                sb.append(c);
                i++;
                break;
            }
        }
        return sb.toString();
    }

    public String getLoginSHA1Hash(String arg) throws PluginException {
        try {
            if (arg != null) {
                getLogger().info("Input " + arg);
                arg = arg.replaceAll("(\\\\|\\\"|\0|')", "\\\\$1");
                arg = urlDecode(arg, "ISO-8859-1"); // <<- causes issues with % in pw
                arg = arg.replaceAll("[ \t\n\r\0\u000B]", "");
                while (arg.startsWith("%20")) {
                    arg = arg.substring(3);
                }
                while (arg.endsWith("%20")) {
                    arg = arg.substring(0, arg.length() - 3);
                }
                arg = arg.replaceAll("(\\\\|\\\"|\0|')", "\\\\$1");
                arg = asciiToLower(arg);
                final MessageDigest md = MessageDigest.getInstance("SHA1");
                final byte[] digest = md.digest(arg.getBytes("latin1"));
                return byteArrayToHex(digest);
            }
            return null;
        } catch (final Throwable e) {
            getLogger().log(e);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private static String asciiToLower(String s) {
        char[] c = new char[s.length()];
        s.getChars(0, s.length(), c, 0);
        int d = 'a' - 'A';
        for (int i = 0; i < c.length; i++) {
            if (c[i] >= 'A' && c[i] <= 'Z') {
                c[i] = (char) (c[i] + d);
            }
        }
        return new String(c);
    }

    private String api_getTokenType(Account account, String token, boolean liveToken) throws Exception {
        synchronized (account) {
            try {
                String tokenType = account.getStringProperty("tokenType", null);
                if (tokenType != null && liveToken == false) {
                    return tokenType;
                }
                getPage(br, getProtocol() + "api.uploaded.net/api/user/jdownloader?access_token=" + token);
                tokenType = br.getRegex("account_type\":\\s*?\"(premium|free)").getMatch(0);
                if (tokenType == null) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "API Error. Please contact Uploaded.to Support.", 5 * 60 * 1000l);
                }
                account.setProperty("tokenType", tokenType);
                if ("premium".equals(tokenType)) {
                    try {
                        maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
                        account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
                        account.setConcurrentUsePossible(ACCOUNT_PREMIUM_CONCURRENT_USAGE_POSSIBLE);
                    } catch (final Throwable e) {
                    }
                } else {
                    try {
                        maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
                        account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
                        account.setConcurrentUsePossible(ACCOUNT_FREE_CONCURRENT_USAGE_POSSIBLE);
                    } catch (final Throwable e) {
                    }
                }
                return tokenType;
            } catch (final PluginException e) {
                maxPrem.set(-1);
                account.setProperty("token", null);
                account.setProperty("tokenType", null);
                throw e;
            } catch (final Exception e) {
                if (usePremiumAPI.compareAndSet(true, false)) {
                    getLogger().info("Disable API");
                }
                account.setProperty("token", null);
                account.setProperty("tokenType", null);
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        if (usePremiumAPI.get() && preferAPI(account) && !downloadLink.getBooleanProperty("preDlPass", false)) {
            api_handle_Premium(downloadLink, account);
            return;
        } else {
            String baseURL = getProtocol() + "uploaded.net/";
            if (!dmcaDlEnabled()) {
                /* Do NOT perform availablecheck here for DMCA-download mode. */
                requestFileInformation(downloadLink);
            }
            site_login(account, false);
            if (account.getBooleanProperty("free")) {
                doFree(downloadLink, account);
            } else {
                logger.info("Premium Account, WEB download method in use!");
                br.setFollowRedirects(false);
                String id = getID(downloadLink);
                String passCode = downloadLink.getStringProperty("pass", null);
                if (downloadLink.getBooleanProperty("preDlPass", false) && passCode != null) {
                    getPage(br, baseURL + "file/" + id + "/ddl?pw=" + Encoding.urlEncode(passCode));
                } else {
                    getPage(br, baseURL + "file/" + id + "/ddl");
                }
                /*
                 * Initial reason for this code: Uploaded.net site version does (sometimes) not like https and redirects to http - this will
                 * also avoid further unexpected redirects
                 */
                final String redirect = br.getRedirectLocation();
                if (redirect != null && redirect.matches("https?://uploaded\\.net/file/[a-z0-9]+/ddl")) {
                    final String ul_forced_protocol = new Regex(redirect, "^(https?://)").getMatch(0);
                    baseURL = ul_forced_protocol + "uploaded.net/";
                    logger.info("Changed uploaded.net used protocol from " + getProtocol() + " to " + ul_forced_protocol);
                    br.getPage(br.getRedirectLocation());
                }
                checkGeneralErrors();
                if (br.containsHTML("<h2>Authentification</h2>")) {
                    logger.info("Password protected link");
                    passCode = getPassword(downloadLink);
                    if (passCode == null || passCode.equals("")) {
                        throw new PluginException(LinkStatus.ERROR_RETRY, "Password wrong!");
                    }
                    postPage(br, br.getURL(), "pw=" + Encoding.urlEncode(passCode));
                    if (br.containsHTML("<h2>Authentification</h2>")) {
                        downloadLink.setProperty("pass", null);
                        throw new PluginException(LinkStatus.ERROR_RETRY, "Password wrong!");
                    }
                    downloadLink.setProperty("pass", passCode);
                }
                String error = new Regex(br.getRedirectLocation(), "https?://uploaded\\.net/\\?view=(.*)").getMatch(0);
                if (error == null) {
                    error = new Regex(br.getRedirectLocation(), "\\?view=(.*?)&i").getMatch(0);
                }
                if (error != null) {
                    if (error.contains("error_traffic")) {
                        throw new AccountUnavailableException(JDL.L("plugins.hoster.uploadedto.errorso.premiumtrafficreached", "Traffic limit reached"), 60 * 60 * 1000l);
                    }
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (br.containsHTML(">Download Blocked \\(ip\\)<") || br.containsHTML("Leider haben wir Zugriffe von zu vielen verschiedenen IPs auf Ihren Account feststellen k\\&#246;nnen, Account-Sharing ist laut unseren AGB strengstens untersagt")) {
                    logger.info("Download blocked (IP), disabling account...");
                    throw new AccountUnavailableException("Your account been flagged for 'Account sharing', Please contact " + this.getHost() + " support for resolution.", 60 * 60 * 1000l);
                } else if (br.containsHTML("You used too many different IPs, Downloads have been blocked for today\\.")) {
                    // shown in html of the download server, 'You used too many different IPs, Downloads have been blocked for today.'
                    logger.warning("Your account has been disabled due account access from too many different IP addresses, Please contact " + this.getHost() + " support for resolution.");
                    throw new AccountUnavailableException("Your account has been disabled due account access from too many different IP addresses, Please contact " + this.getHost() + " support for resolution.", 60 * 60 * 1000l);
                }
                int chunks = 0;
                boolean resume = true;
                if (downloadLink.getBooleanProperty(Uploadedto.NOCHUNKS, false) || resume == false) {
                    chunks = 1;
                }
                if (br.getRedirectLocation() == null) {
                    /* ul does not take care of set language.... */
                    if (br.containsHTML(">Traffic exhausted") || br.containsHTML(">Traffickontingent aufgebraucht") || br.containsHTML(">Your Download-, as well as your Hybrid-Traffic") || br.containsHTML("Leider ist Ihr Download-, als auch Hybrid-Traffic aufgebraucht")) {
                        logger.info("Traffic exhausted, temp disabled account");
                        /* temp debug info */
                        logger.info(br.toString());
                        throw new AccountUnavailableException(60 * 60 * 1000l);
                    }
                    logger.info("InDirect Downloads active");
                    Form form = br.getForm(0);
                    if (form == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    if (form.getAction() != null && form.getAction().contains("register")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    if (form.getAction() == null || form.getAction().contains("access")) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    logger.info("Download from:" + form.getAction());
                    if ("http://".equals(getProtocol()) && StringUtils.startsWithCaseInsensitive(form.getAction(), "https")) {
                        form.setAction(form.getAction().replaceFirst("https://", "http://"));
                    }
                    form.setMethod(MethodType.GET);
                    dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form, true, chunks);
                } else {
                    logger.info("Direct Downloads active");
                    logger.info("Download from:" + br.getRedirectLocation());
                    String dllink = br.getRedirectLocation();
                    if ("http://".equals(getProtocol()) && StringUtils.startsWithCaseInsensitive(dllink, "https")) {
                        dllink = dllink.replaceFirst("https://", "http://");
                    }
                    dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, chunks);
                }
                try {
                    /* remove next major update */
                    /* workaround for broken timeout in 0.9xx public */
                    ((RAFDownload) dl).getRequest().setConnectTimeout(30000);
                    ((RAFDownload) dl).getRequest().setReadTimeout(60000);
                } catch (final Throwable ee) {
                }
                handleGeneralServerErrors();
                if (dl.getConnection().getLongContentLength() == 0 || !dl.getConnection().isContentDisposition()) {
                    try {
                        br.followConnection();
                    } catch (final Throwable e) {
                        logger.severe(e.getMessage());
                    }
                    checkGeneralErrors();
                    try {
                        logger.info(br.toString());
                    } catch (final Throwable e) {
                    }
                    try {
                        logger.info(dl.getConnection().toString());
                    } catch (final Throwable e) {
                    }
                    if ("No htmlCode read".equalsIgnoreCase(br.toString())) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 30 * 60 * 1000l);
                    }
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (dl.getConnection().getResponseCode() == 404) {
                    br.followConnection();
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (downloadLink.getBooleanProperty(UPLOADED_FINAL_FILENAME, false) == false) {
                    final String uploadedFinalFileName = Plugin.getFileNameFromDispositionHeader(dl.getConnection());
                    if (StringUtils.isNotEmpty(uploadedFinalFileName)) {
                        downloadLink.setFinalFileName(uploadedFinalFileName);
                        downloadLink.setProperty(UPLOADED_FINAL_FILENAME, true);
                    }
                }
                if (!this.dl.startDownload()) {
                    try {
                        if (dl.externalDownloadStop()) {
                            return;
                        }
                    } catch (final Throwable e) {
                    }
                    if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload"))) {
                        if (downloadLink.getBooleanProperty(Uploadedto.NORESUME, false) == false) {
                            downloadLink.setChunksProgress(null);
                            downloadLink.setProperty(Uploadedto.NORESUME, Boolean.valueOf(true));
                            throw new PluginException(LinkStatus.ERROR_RETRY);
                        }
                    } else {
                        /* unknown error, we disable multiple chunks */
                        if (downloadLink.getBooleanProperty(Uploadedto.NOCHUNKS, false) == false) {
                            downloadLink.setProperty(Uploadedto.NOCHUNKS, Boolean.valueOf(true));
                            throw new PluginException(LinkStatus.ERROR_RETRY);
                        }
                    }
                }
            }
        }
    }

    public void api_handle_Premium(final DownloadLink downloadLink, final Account account) throws Exception {
        correctDownloadLink(downloadLink);
        String token = api_getAccessToken(account, false);
        String tokenType = api_getTokenType(account, token, false);
        if (!"premium".equals(tokenType)) {
            site_login(account, false);
            doFree(downloadLink, account);
            return;
        }
        logger.info("Premium Account, API download method in use!");
        String id = getID(downloadLink);
        postPage(br, getProtocol() + "api.uploaded.net/api/download/jdownloader", "access_token=" + token + "&auth=" + id);
        if (br.containsHTML("\"err\":\\{\"code\":403")) {
            downloadLink.setProperty("preDlPass", true);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        String url = br.getRegex("link\":\\s*?\"(http.*?)\"").getMatch(0);
        if (url == null) {
            url = br.getRegex("link\":\\s*?\"(\\\\/dl.*?)\"").getMatch(0);
            if (url != null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download currently not possible", 20 * 60 * 1000l);
            }
        }
        String sha1 = br.getRegex("sha1\":\\s*?\"([0-9a-fA-F]+)\"").getMatch(0);
        String name = br.getRegex("name\":\\s*?\"(.*?)\"").getMatch(0);
        String size = br.getRegex("size\":\\s*?\"?(\\d+)\"").getMatch(0);
        String concurrent = br.getRegex("concurrent\":\\s*?\"?(\\d+)").getMatch(0);
        if (url == null) {
            handleErrorCode(br, account, token, true);
        }
        if (sha1 != null) {
            downloadLink.setSha1Hash(sha1);
        }
        setFinalFileName(downloadLink, name);
        if (downloadLink.getVerifiedFileSize() == -1 && size != null) {
            downloadLink.setVerifiedFileSize(Long.parseLong(size));
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
        handleGeneralServerErrors();
        if (dl.getConnection().getLongContentLength() == 0 || !dl.getConnection().isContentDisposition()) {
            try {
                br.followConnection();
            } catch (final Throwable e) {
                logger.severe(e.getMessage());
            }
            handleErrorCode(br, account, token, false);
            checkGeneralErrors();
            try {
                logger.info(br.toString());
            } catch (final Throwable e) {
            }
            try {
                logger.info(dl.getConnection().toString());
            } catch (final Throwable e) {
            }
            if ("No htmlCode read".equalsIgnoreCase(br.toString())) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 30 * 60 * 1000l);
            }
            if (br.containsHTML("You used too many different IPs, Downloads have been blocked for today\\.")) {
                // shown in html of the download server, 'You used too many different IPs, Downloads have been blocked for today.'
                logger.warning("Your account has been disabled due account access from too many different IP addresses, Please contact " + this.getHost() + " support for resolution.");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Your account has been disabled due account access from too many different IP addresses, Please contact " + this.getHost() + " support for resolution.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (br.containsHTML("We're sorry but your download ticket couldn't have been found")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 5 * 60 * 1000l);
            }
            // unknown error/defect, lets try next time with web method!
            usePremiumAPI.set(false);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dl.getConnection().getResponseCode() == 404) {
            try {
                br.followConnection();
            } catch (final Throwable e) {
                logger.severe(e.getMessage());
            }
            // this does not mean that the file is offline. This is most likly a server error. try again. if the file is really offline, the
            // linkcheck will set the corrects status
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError(404)", 1 * 60 * 1000l);
        }
        if (downloadLink.getBooleanProperty(UPLOADED_FINAL_FILENAME, false) == false) {
            final String uploadedFinalFileName = Plugin.getFileNameFromDispositionHeader(dl.getConnection());
            if (StringUtils.isNotEmpty(uploadedFinalFileName)) {
                downloadLink.setFinalFileName(uploadedFinalFileName);
                downloadLink.setProperty(UPLOADED_FINAL_FILENAME, true);
            } else if (StringUtils.isNotEmpty(name)) {
                downloadLink.setFinalFileName(name);
                downloadLink.setProperty(UPLOADED_FINAL_FILENAME, true);
            }
        }
        dl.startDownload();
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(DownloadLink link, Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        final AccountType type = acc.getType();
        if (AccountType.PREMIUM.equals(type)) {
            return false;
        }
        return true;
    }

    private void changeToEnglish(Browser br) throws IOException, PluginException, InterruptedException {
        boolean red = br.isFollowingRedirects();
        try {
            br.setFollowRedirects(false);
            getPage(br, getProtocol() + "uploaded.net/language/en");
        } finally {
            br.setFollowRedirects(red);
        }
    }

    @SuppressWarnings("unchecked")
    private void site_login(final Account account, final boolean force) throws Exception {
        this.setBrowserExclusive();
        workAroundTimeOut(br);
        br.setDebug(true);
        br.setFollowRedirects(true);
        prepBrowser();
        synchronized (account) {
            try {
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?>) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(CURRENT_DOMAIN, key, value);
                        }
                        if (!force) {
                            /* No additional check needed. */
                            return;
                        }
                        br.getPage(CURRENT_DOMAIN);
                        if (br.containsHTML("href=\"logout\">Logout</a>") && br.getCookie("uploaded.net", "auth") != null) {
                            return;
                        }
                    }
                }
                boolean loginIssues = false;
                int counter = 0;
                do {
                    logger.info("Login attempt " + counter + " of 2");
                    if (counter > 0) {
                        Thread.sleep(3000l);
                    }
                    /* Use a fresh browser every time */
                    br = new Browser();
                    prepBrowser();
                    br.getPage("http://uploaded.net/");
                    br.getHeaders().put("X-Prototype-Version", "1.6.1");
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                    /* login method always returns empty body */
                    postPage(br, getProtocol() + "uploaded.net/io/login", "id=" + Encoding.urlEncode(account.getUser()) + "&pw=" + Encoding.urlEncode(account.getPass()));
                    checkGeneralErrors();
                    if (br.containsHTML("User and password do not match")) {
                        final AccountInfo ai = account.getAccountInfo();
                        if (ai != null) {
                            ai.setStatus("User and password do not match");
                        }
                        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                    loginIssues = br.getCookie("uploaded.net", "auth") == null;
                    counter++;
                } while (counter <= 5 && loginIssues);
                if (loginIssues) {
                    logger.warning("Account check failed because of login/server issues");
                    throw new AccountUnavailableException("Login issues", 60 * 60 * 1000l);
                }
                changeToEnglish(br);
                /* Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(CURRENT_DOMAIN);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (PluginException e) {
                account.setProperty("token", Property.NULL);
                account.setProperty("tokenType", Property.NULL);
                account.setProperty("cookies", Property.NULL);
                throw e;
            } finally {
                br.getHeaders().put("Content-Type", null);
                br.getHeaders().put("X-Prototype-Version", null);
                br.getHeaders().put("X-Requested-With", null);
            }
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    private void prepBrowser() throws IOException, PluginException, InterruptedException {
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.setCookie("http://uploaded.net", "lang", "en");
        br.setCookie("https://uploaded.net", "lang", "en");
        boolean red = br.isFollowingRedirects();
        try {
            br.setFollowRedirects(false);
            // do NOT use the getPage method here as it will follow redirects -> redirectloop in free account handling can happen
            // getPage(br, getProtocol() + "uploaded.net/language/en", false);
            br.getPage(getProtocol() + "uploaded.net/language/en");
        } finally {
            br.setFollowRedirects(red);
        }
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
                    if (currentIP != null) {
                        break;
                    }
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

    @SuppressWarnings("deprecation")
    private boolean setIP(final DownloadLink link, final Account account) throws PluginException {
        synchronized (IPCHECK) {
            if (currentIP.get() != null && !new Regex(currentIP.get(), IPREGEX).matches()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (ipChanged(link) == false) {
                // Static IP or failure to reconnect! We don't change lastIP
                logger.warning("Your IP hasn't changed since last download");
                return false;
            } else {
                String lastIP = currentIP.get();
                link.setProperty(PROPERTY_LASTIP, lastIP);
                Uploadedto.lastIP.set(lastIP);
                getPluginConfig().setProperty(PROPERTY_LASTIP, lastIP);
                logger.info("LastIP = " + lastIP);
                return true;
            }
        }
    }

    private boolean ipChanged(final DownloadLink link) throws PluginException {
        String currIP = null;
        if (currentIP.get() != null && new Regex(currentIP.get(), IPREGEX).matches()) {
            currIP = currentIP.get();
        } else {
            currIP = getIP();
        }
        if (currIP == null) {
            return false;
        }
        String lastIP = link.getStringProperty(PROPERTY_LASTIP, null);
        if (lastIP == null) {
            lastIP = Uploadedto.lastIP.get();
        }
        if (lastIP == null) {
            lastIP = this.getPluginConfig().getStringProperty(PROPERTY_LASTIP, null);
        }
        return !currIP.equals(lastIP);
    }

    private void freeDownloadlimitReached(final String message) throws PluginException {
        /* Even without IP workaround we can use the timestamp to determine needed waittime. */
        final long timestamp_last_download_started = getPluginSavedLastDownloadTimestamp();
        final long timePassed = System.currentTimeMillis() - timestamp_last_download_started;
        if (timePassed >= FREE_RECONNECTWAIT) {
            logger.info("According to saved waittime we passed the waittime which is impossible as uploaded has shown reconnect errormessage --> Throwing IP_BLOCKED exception with full reconnect time");
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, message, FREE_RECONNECTWAIT);
        } else {
            final long remainingWaittime = FREE_RECONNECTWAIT - timePassed;
            logger.info("According to saved waittime we have not yet waited enough --> Waiting/Reconnecting: Remaining time: " + remainingWaittime);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, message, remainingWaittime);
        }
    }

    private long getPluginSavedLastDownloadTimestamp() {
        long lastdownload = 0;
        synchronized (blockedIPsMap) {
            final Iterator<Entry<String, Long>> it = blockedIPsMap.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, Long> ipentry = it.next();
                final String ip = ipentry.getKey();
                final long timestamp = ipentry.getValue();
                if (System.currentTimeMillis() - timestamp >= FREE_RECONNECTWAIT) {
                    /* Remove old entries */
                    it.remove();
                }
                if (ip.equals(currentIP.get())) {
                    lastdownload = timestamp;
                }
            }
        }
        return lastdownload;
    }

    private static long getLongProperty(final Property link, final String key, final long def) {
        try {
            return link.getLongProperty(key, def);
        } catch (final Throwable e) {
            try {
                Object r = link.getProperty(key, def);
                if (r instanceof String) {
                    r = Long.parseLong((String) r);
                } else if (r instanceof Integer) {
                    r = ((Integer) r).longValue();
                }
                final Long ret = (Long) r;
                return ret;
            } catch (final Throwable e2) {
                return def;
            }
        }
    }

    @SuppressWarnings("deprecation")
    private boolean dmcaDlEnabled() {
        final SubConfiguration thiscfg = this.getPluginConfig();
        return (!thiscfg.getBooleanProperty(PREFER_PREMIUM_DOWNLOAD_API, default_ppda) && thiscfg.getBooleanProperty(DOWNLOAD_ABUSED, false));
    }

    private HashMap<String, String> phrasesEN = new HashMap<String, String>() {
        {
            put("SETTING_ACTIVATEACCOUNTERRORHANDLING", "Activate experimental free account errorhandling: Reconnect and switch between free accounts (to get more dl speed), also prevents having to enter additional captchas in between downloads.");
            put("SETTING_EXPERIMENTALHANDLING", "Activate reconnect workaround for freeusers: Prevents having to enter additional captchas in between downloads.");
            put("SETTING_SSL_CONNECTION", "Use Secure Communication over SSL (HTTPS://)");
            put("SETTING_PREFER_PREMIUM_DOWNLOAD_API", "By enabling this feature, JDownloader downloads via custom download API. On failure it will auto revert to web method!\r\nBy disabling this feature, JDownloader downloads via Web download method. Web method is generally less reliable than API method.");
            put("SETTING_DOWNLOAD_ABUSED", "Activate download of DMCA blocked links?\r\n-This function enabled uploaders to download their own links which have a 'legacy takedown' status till uploaded irrevocably deletes them\r\nNote the following:\r\n-When activated, links which have the public status 'offline' will get an 'uncheckable' status instead\r\n--> If they're still downloadable, their filename- and size will be shown on downloadstart\r\n--> If they're really offline, the correct (offline) status will be shown on downloadstart");
        }
    };
    private HashMap<String, String> phrasesDE = new HashMap<String, String>() {
        {
            put("SETTING_ACTIVATEACCOUNTERRORHANDLING", "Aktiviere experimentielles free Account Handling: Führe Reconnects aus und wechsle zwischen verfügbaren free Accounts (um die Downloadgeschwindigkeit zu erhöhen). Verhindert auch sinnlose Captchaabfragen zwischen Downloads.");
            put("SETTING_EXPERIMENTALHANDLING", "Aktiviere Reconnect Workaround: Verhindert sinnlose Captchaabfragen zwischen Downloads.");
            put("SETTING_SSL_CONNECTION", "Verwende sichere Verbindungen per SSL (HTTPS://)");
            put("SETTING_PREFER_PREMIUM_DOWNLOAD_API", "Ist dieses Feature aktiviert, verwendet JDownloader die Programmierschnittstelle (API). Nach Fehlversuchen wird automatisch zum Handling per Webseite gewechselt.\r\nIst dieses Feature deaktiviert benutzt JDownloader ausschließlich die Webseite. Die Webseite ist allgemein instabiler als die API.");
            put("SETTING_DOWNLOAD_ABUSED", "Aktiviere Download DMCA gesperrter Links?\r\nBedenke folgendes:\r\n-Diese Funktion erlaubt es Uploadern, ihre eigenen mit 'legacy takedown' Status versehenen Links in dem vom Hoster gegebenen Zeitraum noch herunterladen zu können\r\n-Diese Funktion führt dazu, dass Links, die öffentlich den Status 'offline' haben, stattdessen den Status 'nicht überprüft' bekommen\r\n--> Falls diese wirklich offline sind, wird der korrekte (offline) Status erst beim Downloadstart angezeigt\r\n--> Falls diese noch ladbar sind, werden deren Dateiname- und Größe beim Downloadstart angezeigt");
        }
    };

    /**
     * Returns a German/English translation of a phrase. We don't use the JDownloader translation framework since we need only German and
     * English.
     *
     * @param key
     * @return
     */
    private String getPhrase(String key) {
        if ("de".equals(System.getProperty("user.language")) && phrasesDE.containsKey(key)) {
            return phrasesDE.get(key);
        } else if (phrasesEN.containsKey(key)) {
            return phrasesEN.get(key);
        }
        return "Translation not found!";
    }

    private final boolean default_ppda   = true;
    private final boolean default_aaeh   = false;
    private final boolean default_eh     = false;
    private final boolean default_abused = false;

    public void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ACTIVATEACCOUNTERRORHANDLING, getPhrase("SETTING_ACTIVATEACCOUNTERRORHANDLING")).setDefaultValue(default_aaeh));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), EXPERIMENTALHANDLING, getPhrase("SETTING_EXPERIMENTALHANDLING")).setDefaultValue(default_eh));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SSL_CONNECTION, getPhrase("SETTING_SSL_CONNECTION")).setDefaultValue(PREFERSSL));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final ConfigEntry cfe = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PREFER_PREMIUM_DOWNLOAD_API, getPhrase("SETTING_PREFER_PREMIUM_DOWNLOAD_API")).setDefaultValue(default_ppda);
        getConfig().addEntry(cfe);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), DOWNLOAD_ABUSED, getPhrase("SETTING_DOWNLOAD_ABUSED")).setDefaultValue(default_abused).setEnabledCondidtion(cfe, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), DISABLE_START_INTERVAL, "Disable start interval. Warning: This may cause IP blocks from uploaded.to").setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), EXPERIMENTAL_MULTIFREE, "Experimental support of multiple free downloads").setDefaultValue(false));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        if (link != null) {
            link.removeProperty(UPLOADED_FINAL_FILENAME);
        }
    }

    @Override
    public void resetPluginGlobals() {
    }
}