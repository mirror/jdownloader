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
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.os.CrossSystem;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "share-online.biz" }, urls = { "https?://(www\\.)?(share\\-online\\.biz|egoshare\\.com)/(download.php\\?id\\=|dl/)[\\w]+" }, flags = { 2 })
public class ShareOnlineBiz extends PluginForHost {

    private static HashMap<Account, HashMap<String, String>> ACCOUNTINFOS         = new HashMap<Account, HashMap<String, String>>();
    private static Object                                    LOCK                 = new Object();
    private static HashMap<Long, Long>                       noFreeSlot           = new HashMap<Long, Long>();
    private static HashMap<Long, Long>                       overloadedServer     = new HashMap<Long, Long>();
    private long                                             server               = -1;
    private long                                             waitNoFreeSlot       = 10 * 60 * 1000l;
    private long                                             waitOverloadedServer = 5 * 60 * 1000l;
    private static String                                    UA                   = RandomUserAgent.generate();
    private boolean                                          hideID               = true;
    private static AtomicInteger                             maxChunksnew         = new AtomicInteger(-2);
    private char[]                                           FILENAMEREPLACES     = new char[] { '_', '&', 'ü' };
    private final String                                     SHARED_IP_WORKAROUND = "SHARED_IP_WORKAROUND";

    public ShareOnlineBiz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.share-online.biz/service.php?p=31353834353B4A44616363");
        setConfigElements();
    }

    @Override
    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            Browser br = new Browser();
            br.setCookiesExclusive(true);
            br.setFollowRedirects(true);
            StringBuilder sb = new StringBuilder();
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 80 links at once */
                    if (index == urls.length || links.size() > 200) break;
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("links=");
                int c = 0;
                for (DownloadLink dl : links) {
                    if (c > 0) sb.append("\n");
                    sb.append(getID(dl));
                    c++;
                }
                br.postPage("http://api.share-online.biz/cgi-bin?q=checklinks&md5=1", sb.toString());
                String infos[][] = br.getRegex(Pattern.compile("(.*?);(.*?);(.*?);(.*?);([0-9a-fA-F]+)")).getMatches();
                for (DownloadLink dl : links) {
                    String id = getID(dl);
                    int hit = -1;
                    for (int i = 0; i < infos.length; i++) {
                        if (infos[i][0].equalsIgnoreCase(id)) {
                            hit = i;
                            break;
                        }
                    }
                    if (hit == -1) {
                        /* id not in response, so its offline */
                        dl.setAvailable(false);
                    } else {
                        dl.setFinalFileName(infos[hit][2].trim());
                        long size = -1;
                        dl.setDownloadSize(size = SizeFormatter.getSize(infos[hit][3]));
                        if (size > 0) {
                            dl.setProperty("VERIFIEDFILESIZE", size);
                        }
                        if (infos[hit][1].trim().equalsIgnoreCase("OK")) {
                            dl.setAvailable(true);
                            dl.setMD5Hash(infos[hit][4].trim());
                        } else {
                            dl.setAvailable(false);
                        }
                    }
                }
                if (index == urls.length) break;
            }
        } catch (Throwable e) {
            return false;
        }
        return true;
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        // We do not have to change anything here, the regexp also works for
        // egoshare links!
        String id = new Regex(link.getDownloadURL(), "(id\\=|/dl/)([a-zA-Z0-9]+)").getMatch(1);
        link.setUrlDownload("http://www.share-online.biz/dl/" + id);
        if (hideID) link.setName("download.php");
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SHARED_IP_WORKAROUND, JDL.L("plugins.hoster.shareonline.sharedipworkaround", "Enable shared IP workaround?")).setDefaultValue(false));
    }

    private void errorHandling(Browser br, DownloadLink downloadLink, Account acc, HashMap<String, String> usedPremiumInfos) throws PluginException {
        /* file is offline */
        if (br.containsHTML("The requested file is not available")) {
            logger.info("The following link was marked as online by the API but is offline: " + downloadLink.getDownloadURL());
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* no free slot */
        if (br.containsHTML("No free slots for free users") || br.getURL().contains("failure/full")) {
            downloadLink.getLinkStatus().setRetryCount(0);
            if (server != -1) {
                synchronized (noFreeSlot) {
                    noFreeSlot.put(server, System.currentTimeMillis());
                }
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.shareonlinebiz.errors.servernotavailable3", "No free Free-User Slots! Get PremiumAccount or wait!"), waitNoFreeSlot);
        }
        // shared IP error
        if (br.containsHTML("<strong>The usage of different IPs is not possible!</strong>")) {
            /* disabled as it causes problem, added debug to find cause */
            logger.info("IPDEBUG: " + br.toString());
            // // for no account!?
            // if (acc == null) {
            // throw new
            // PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE,
            // "The usage of different IPs is not possible!", 60
            // * 60 * 1000L);
            // } else {
            // // for Premium
            // acc.setValid(false);
            // UserIO.getInstance().requestMessageDialog(0,
            // "ShareOnlineBiz Premium Error (account has been deactivated, free mode enabled)",
            // "Server reports: "
            // +
            // "You're trying to use your account from more than one IP-Adress.\n"
            // +
            // "The usage of different IP addresses is not allowed with every type of access,\nthe same affects any kind of account sharing.\n"
            // +
            // "You are free to buy a further access for pay accounts, in order to use it from every place you want to.\n"
            // +
            // "A contempt of this rules can result in a complete account deactivation.");
            // throw new PluginException(LinkStatus.ERROR_PREMIUM,
            // "Premium disabled, continued as free user");
            // }
        }
        String url = br.getURL();
        if (url.endsWith("/free/") || url.endsWith("/free")) {
            /* workaround when the redirect was missing */
            String windowLocation = br.getRegex("'(http://[^']*?/failure/[^']*?)'").getMatch(0);
            if (windowLocation != null) url = windowLocation;
        }
        if (url.contains("failure/server")) {
            /* server offline */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server currently Offline", 2 * 60 * 60 * 1000l);
        }
        if (url.contains("failure/threads")) {
            /* already loading,too many threads */
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 15 * 60 * 1000l);
        }
        if (url.contains("failure/chunks")) {
            /* max chunks reached */
            String maxCN = new Regex(url, "failure/chunks/(\\d+)").getMatch(0);
            if (maxCN != null) maxChunksnew.set(Integer.parseInt(maxCN));
            downloadLink.setChunksProgress(null);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "MaxChunks Error", 10 * 60 * 1000l);
        }
        if (url.contains("failure/freelimit")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "File too big, limited by the file owner.");
        }
        if (url.contains("failure/bandwidth")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l); }
        if (url.contains("failure/filenotfound")) {
            try {
                final Browser br2 = new Browser();
                final String id = this.getID(downloadLink);
                br2.getPage("http://api.share-online.biz/api/account.php?act=fileError&fid=" + id);
            } catch (Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (url.contains("failure/overload")) {
            if (server != -1) {
                synchronized (overloadedServer) {
                    overloadedServer.put(server, System.currentTimeMillis());
                }
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server overloaded", waitOverloadedServer);
        }
        if (url.contains("failure/precheck")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 10 * 60 * 1000l); }
        if (url.contains("failure/invalid")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 15 * 60 * 1000l); }
        if (url.contains("failure/ip")) {
            if (acc != null && getPluginConfig().getBooleanProperty(SHARED_IP_WORKAROUND, false)) {
                logger.info("Using SharedIP workaround to avoid disabling the premium account!");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "SharedIPWorkaround", 2 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP Already loading", 15 * 60 * 1000l);
        }
        if (url.contains("failure/size")) { throw new PluginException(LinkStatus.ERROR_FATAL, "File too big. Premium needed!"); }
        if (url.contains("failure/expired") || url.contains("failure/session")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Wait for new ticket", 60 * 1000l); }
        if (url.contains("failure/cookie")) {
            if (acc != null) {
                synchronized (LOCK) {
                    HashMap<String, String> infos = ACCOUNTINFOS.get(acc);
                    if (infos != null && usedPremiumInfos == infos) {
                        logger.info("Force refresh of cookies");
                        ACCOUNTINFOS.remove(acc);
                    }
                }
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "CookieError", 3 * 60 * 1000l);
        }
        if (br.containsHTML("nput invalid, halting. please avoid more of these requests")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        setBrowserExclusive();
        HashMap<String, String> infos = null;
        try {
            infos = loginAPI(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        /* evaluate expire date */
        final Long validUntil = Long.parseLong(infos.get("expire_date"));
        account.setValid(true);
        if (validUntil > 0) {
            ai.setValidUntil(validUntil * 1000);
        } else {
            ai.setValidUntil(-1);
        }
        if (infos.containsKey("points")) ai.setPremiumPoints(Long.parseLong(infos.get("points")));
        if (infos.containsKey("money")) ai.setAccountBalance(infos.get("money"));
        /* set account type */
        ai.setStatus(infos.get("group"));
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://share-online.biz/rules.php";
    }

    private final String getID(DownloadLink link) {
        return new Regex(link.getDownloadURL(), "(id\\=|/dl/)([a-zA-Z0-9]+)").getMatch(1);
    }

    /* parse the response from api into an hashmap */
    private HashMap<String, String> getInfos(String response, String seperator) throws PluginException {
        if (response == null || response.length() == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String infos[] = Regex.getLines(response);
        HashMap<String, String> ret = new HashMap<String, String>();
        for (String info : infos) {
            String data[] = info.split(seperator);
            if (data.length == 1) {
                ret.put(data[0].trim(), null);
            } else if (data.length == 2) {
                ret.put(data[0].trim(), data[1].trim());
            } else {
                logger.warning("GetInfos failed, browser content:\n");
                logger.warning(br.toString());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        return ret;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /*
         * because of You have got max allowed threads from same download session
         */
        return 10;
    }

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
                    File checkFile = JDUtilities.getResourceFile("tmp/sotmp");
                    if (!checkFile.exists()) {
                        checkFile.mkdirs();
                        showFreeDialog("share-online.biz");
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
    public void handleFree(DownloadLink downloadLink) throws Exception {
        checkShowFreeDialog();
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        if (server != -1) {
            synchronized (noFreeSlot) {
                Long ret = noFreeSlot.get(server);
                if (ret != null) {
                    if (System.currentTimeMillis() - ret < waitNoFreeSlot) {
                        if (downloadLink.getLinkStatus().getRetryCount() >= 5) {
                            /*
                             * reset counter this error does not cause plugin to stop
                             */
                            downloadLink.getLinkStatus().setRetryCount(0);
                        }
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.shareonlinebiz.errors.servernotavailable3", "No free Free-User Slots! Get PremiumAccount or wait!"), waitNoFreeSlot);
                    } else {
                        noFreeSlot.remove(server);
                    }
                }
            }
            synchronized (overloadedServer) {
                Long ret = overloadedServer.get(server);
                if (ret != null) {
                    if (System.currentTimeMillis() - ret < waitOverloadedServer) {
                        if (downloadLink.getLinkStatus().getRetryCount() >= 5) {
                            /*
                             * reset counter this error does not cause plugin to stop
                             */
                            downloadLink.getLinkStatus().setRetryCount(0);
                        }
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server overloaded", waitNoFreeSlot);
                    } else {
                        overloadedServer.remove(server);
                    }
                }
            }
        }
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", UA);
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-us,de;q=0.7,en;q=0.3");
        br.getHeaders().put("Pragma", null);
        br.getHeaders().put("Cache-Control", null);
        br.setCookie("http://www.share-online.biz", "page_language", "english");
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().contains("/failure/proxy/1")) throw new PluginException(LinkStatus.ERROR_FATAL, "Proxy error");
        Browser brc = br.cloneBrowser();
        try {
            brc.openGetConnection("http://www.share-online.biz/template/images/corp/uploadking.php?show=last");
        } finally {
            try {
                brc.getHttpConnection().disconnect();
            } catch (final Throwable e) {
            }
        }
        errorHandling(br, downloadLink, null, null);
        if (!br.containsHTML(">>> continue for free <<<")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String ID = getID(downloadLink);
        br.postPage("http://www.share-online.biz/dl/" + ID + "/free/", "dl_free=1");
        errorHandling(br, downloadLink, null, null);
        String wait = br.getRegex("var wait=(\\d+)").getMatch(0);
        boolean captcha = br.containsHTML("RECAPTCHA active");
        long startWait = 0;
        if (captcha == true) {
            startWait = System.currentTimeMillis();
        } else {
            if (wait != null) {
                this.sleep(Integer.parseInt(wait) * 1000l, downloadLink);
            }
        }
        String dlINFO = br.getRegex("var dl=\"(.*?)\"").getMatch(0);
        String url = Encoding.Base64Decode(dlINFO);
        if (captcha) {
            /* recaptcha handling */
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setId("6LdatrsSAAAAAHZrB70txiV5p-8Iv8BtVxlTtjKX");
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode("recaptcha", cf, downloadLink);
            if (wait != null) {
                long gotWait = Integer.parseInt(wait) * 500l;
                long waited = System.currentTimeMillis() - startWait;
                gotWait -= waited;
                if (gotWait > 0) {
                    this.sleep(gotWait, downloadLink);
                }
            }
            br.postPage("http://www.share-online.biz/dl/" + ID + "/free/captcha/" + System.currentTimeMillis(), "dl_free=1&recaptcha_response_field=" + Encoding.urlEncode(c) + "&recaptcha_challenge_field=" + rc.getChallenge());
            url = br.getRegex("([a-zA-Z0-9/=]+)").getMatch(0);
            if ("0".equals(url)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            url = Encoding.Base64Decode(url);
            if (url == null || !url.startsWith("http")) url = null;
            if (wait != null) {
                this.sleep(Integer.parseInt(wait) * 1000l, downloadLink);
            }
        }
        br.setFollowRedirects(true);
        /* Datei herunterladen */
        if (url != null && url.trim().length() == 0) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 5 * 60 * 1000l);
        if (url == null || !url.startsWith("http")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            errorHandling(br, downloadLink, null, null);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        this.setBrowserExclusive();
        final HashMap<String, String> infos = loginAPI(account, false);
        final String linkID = getID(parameter);
        String dlC = infos.get("dl");
        if (dlC != null && !"not_available".equalsIgnoreCase(dlC)) {
            br.setCookie("http://www.share-online.biz", "dl", dlC);
        }
        String a = infos.get("a");
        if (a != null && !"not_available".equalsIgnoreCase(a)) {
            br.setCookie("http://www.share-online.biz", "a", a);
        }
        br.setFollowRedirects(true);
        final String response = br.getPage("http://api.share-online.biz/cgi-bin?q=linkdata&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&lid=" + linkID);
        if (response.contains("** USER DATA INVALID")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (response.contains("** REQUESTED DOWNLOAD LINK NOT FOUND **")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (response.contains("EXCEPTION request download link not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // This one is NOT an API error
        if (br.containsHTML(">Share\\-Online \\- Page not found \\- #404<|The desired content is not available")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Serverfehler, bitte warten...", 30 * 1000l);
        final HashMap<String, String> dlInfos = getInfos(response, ": ");
        final String filename = dlInfos.get("NAME");
        final String size = dlInfos.get("SIZE");
        final String status = dlInfos.get("STATUS");
        if (filename == null || size == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        parameter.setMD5Hash(dlInfos.get("MD5"));
        if (!"online".equalsIgnoreCase(status)) {
            if ("server_under_maintenance".equalsIgnoreCase(dlInfos.get("URL"))) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server currently Offline", 2 * 60 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (size != null) parameter.setDownloadSize(Long.parseLong(size));
        if (filename != null) parameter.setFinalFileName(filename);
        final String dlURL = dlInfos.get("URL");
        // http://api.share-online.biz/api/account.php?act=fileError&fid=FILE_ID
        if (dlURL == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if ("server_under_maintenance".equals(dlURL)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server currently Offline", 2 * 60 * 60 * 1000l);
        br.setFollowRedirects(true);
        /* Datei herunterladen */
        /* api does allow resume, but only 1 chunk */
        logger.info("used url: " + dlURL);
        br.setDebug(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, dlURL, true, maxChunksnew.get());
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            errorHandling(br, parameter, account, infos);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    public HashMap<String, String> loginAPI(Account account, boolean forceLogin) throws IOException, PluginException {
        synchronized (LOCK) {
            HashMap<String, String> infos = ACCOUNTINFOS.get(account);
            if (infos == null || forceLogin) {
                boolean follow = br.isFollowingRedirects();
                br.setFollowRedirects(true);
                String page = null;
                try {
                    page = br.getPage("http://api.share-online.biz/cgi-bin?q=userdetails&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                } finally {
                    br.setFollowRedirects(follow);
                }
                infos = getInfos(page, "=");
                ACCOUNTINFOS.put(account, infos);
            }
            /* check dl cookie, must be available for premium accounts */
            String dl = infos.get("dl");
            String a = infos.get("a");
            if ("Sammler".equals(infos.get("group"))) { throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nEs werden nur share-online Premiumaccounts akzeptiert, dies ist ein Sammleraccount!\r\nJDownloader only accepts premium accounts, this is a collectors account!", PluginException.VALUE_ID_PREMIUM_DISABLE); }
            if (dl == null && a == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nFalscher Benutzername/Passwort!\r\nInvalid username/password!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            boolean valid = dl != null && !"not_available".equalsIgnoreCase(dl);
            if (valid == false) valid = a != null && !"not_available".equalsIgnoreCase(a);
            if (valid == false) throw new PluginException(LinkStatus.ERROR_PREMIUM, "Invalid username/password!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            /*
             * check expire date, expire >0 (normal handling) expire<0 (never expire)
             */
            final Long validUntil = Long.parseLong(infos.get("expire_date"));
            if (validUntil > 0 && System.currentTimeMillis() / 1000 > validUntil) throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account expired! || Account abgelaufen!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            return infos;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        hideID = false;
        correctDownloadLink(downloadLink);
        this.setBrowserExclusive();
        server = -1;
        br.setCookie("http://www.share-online.biz", "king_mylang", "en");
        br.setAcceptLanguage("en, en-gb;q=0.8");
        String id = getID(downloadLink);
        br.setDebug(true);
        br.setFollowRedirects(true);
        if (br.postPage("http://api.share-online.biz/cgi-bin?q=checklinks&md5=1&snr=1", "links=" + id).matches("\\s*")) {
            String startURL = downloadLink.getDownloadURL();
            // workaround to bypass new layout and use old site
            br.getPage(startURL += startURL.contains("?") ? "&v2=1" : "?v2=1");
            String[] strings = br.getRegex("</font> \\((.*?)\\) \\.</b></div></td>.*?<b>File name:</b>.*?<b>(.*?)</b></div></td>").getRow(0);
            if (strings == null || strings.length != 2) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setDownloadSize(SizeFormatter.getSize(strings[0].trim()));
            downloadLink.setName(strings[1].trim());
            return AvailableStatus.TRUE;
        }
        String infos[] = br.getRegex("(.*?);(.*?);(.*?);(.*?);(.*?);(\\d+)").getRow(0);
        if (infos == null || !infos[1].equalsIgnoreCase("OK")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        long size = -1;
        downloadLink.setDownloadSize(size = Long.parseLong(infos[3].trim()));
        if (size > 0) {
            downloadLink.setProperty("VERIFIEDFILESIZE", size);
        }
        downloadLink.setName(infos[2].trim());
        server = Long.parseLong(infos[5].trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        synchronized (noFreeSlot) {
            noFreeSlot.clear();
        }
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