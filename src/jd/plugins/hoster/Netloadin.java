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
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "netload.in" }, urls = { "http://[\\w\\.]*?netload\\.in/[^(http://)].+" }, flags = { 2 })
public class Netloadin extends PluginForHost {
    static private final String AGB_LINK  = "http://netload.in/index.php?id=13";

    static public Object        LOGINLOCK = new Object();

    private static String getID(String link) {
        String id = new Regex(link, "\\/datei([a-zA-Z0-9]+)").getMatch(0);
        if (id == null) id = new Regex(link, "file_id=([a-zA-Z0-9]+)").getMatch(0);
        if (id == null) id = new Regex(link, "netload\\.in\\/([a-zA-Z0-9]+)\\/.+").getMatch(0);
        return id;
    }

    private boolean showDialog = false;

    public Netloadin(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(1000l);
        this.enablePremium("http://netload.in/index.php?refer_id=134847&id=5");
    }

    private void checkErrors(DownloadLink downloadLink, Browser br, boolean checkFail) throws Exception {
        if (br.getRedirectLocation() != null && br.getRedirectLocation().endsWith("index.php")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 5 * 60 * 1000l);
        String state = br.getRegex("state\":\"(.*?)\"").getMatch(0);
        if ("hddcrash".equalsIgnoreCase(state)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "HDDCrash(In Recovery)", 12 * 60 * 60 * 1000l);
        if ("maintenance".equalsIgnoreCase(state)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Maintenance", 2 * 60 * 60 * 1000l);
        if ("only_premium_download".equalsIgnoreCase(state)) {
            if (JDL.isGerman()) {
                if (UserIO.isOK(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN, "Download nur mit Netload Premium", "Leider kann diese Datei nur mit einem Netload Premium Account heruntergeladen werden.", null, "Mehr Informationen...", "Datei überspringen"))) {
                    try {
                        java.awt.Desktop.getDesktop().browse(new URL("http://jdownloader.org/r.php?u=http%3A%2F%2Fnetload.in%2Findex.php%3Frefer_id%3D134847%26id%3D5").toURI());
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, "Nur mit Premium");
            } else {
                if (UserIO.isOK(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN, "Netload Premium required", "This file can be only downloaded with a Netload premium account.", null, "More Information...", "Skip Link"))) {
                    try {
                        java.awt.Desktop.getDesktop().browse(new URL("http://jdownloader.org/r.php?u=http%3A%2F%2Fnetload.in%2Findex.php%3Frefer_id%3D134847%26id%3D5").toURI());
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, "Premium only");
            }
        }
        if (checkFail && "fail".equalsIgnoreCase(state)) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 4 * 60 * 60 * 1000l); }
        if (checkFail && br.containsHTML(">Error</h1>")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 15 * 60 * 1000l); }
    }

    private void checkLimit(DownloadLink downloadLink, Browser br) throws Exception {
        String state = br.getRegex("state\":\"(.*?)\"").getMatch(0);
        String countdown = br.getRegex("countdown\":(\\d+)").getMatch(0);
        if (countdown == null) return;
        if ("limitexceeded".equalsIgnoreCase(state)) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(countdown) * 1000l);
        } else if ("ok".equalsIgnoreCase(state)) {
            this.sleep(Integer.parseInt(countdown) * 1000l, downloadLink);
        } else {
            if ("fail".equalsIgnoreCase(state)) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 4 * 60 * 60 * 1000l); }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public boolean checkLinks(DownloadLink[] urls) {
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
                sb.append("auth=BVm96BWDSoB4WkfbEhn42HgnjIe1ilMt&bz=1&md5=1&file_id=");
                int c = 0;
                for (DownloadLink dl : links) {
                    if (c > 0) sb.append(";");
                    sb.append(Netloadin.getID(dl.getDownloadURL()));
                    c++;
                }
                br.postPage("http://api.netload.in/info.php", sb.toString());
                String infos[][] = br.getRegex(Pattern.compile("(.*?);(.*?);(\\d+);(.*?);([0-9a-fA-F]+)")).getMatches();
                for (DownloadLink dl : links) {
                    String id = Netloadin.getID(dl.getDownloadURL());
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
                        dl.setFinalFileName(infos[hit][1].trim());
                        long size;
                        dl.setDownloadSize(size = SizeFormatter.getSize(infos[hit][2]));
                        if (size > 0) {
                            dl.setProperty("VERIFIEDFILESIZE", size);
                        }
                        if (infos[hit][3].trim().equalsIgnoreCase("online")) {
                            dl.setAvailable(true);
                            dl.setMD5Hash(infos[hit][4].trim());
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

    private void checkPassword(DownloadLink downloadLink, Browser br) throws Exception {
        String state = br.getRegex("state\":\"(.*?)\"").getMatch(0);
        if (!"failpass".equalsIgnoreCase(state)) return;
        String ID = getID(downloadLink.getDownloadURL());
        String pass = downloadLink.getStringProperty("pass", null);
        if (pass == null) {
            pass = Plugin.getUserInput(JDL.LF("plugins.hoster.netload.downloadPassword_question", "Password protected. Enter Password for %s", downloadLink.getName()), downloadLink);
        }
        br.getPage("http://netload.in/json/datei" + ID + ".htm?password=" + Encoding.urlEncode(pass));
        state = br.getRegex("state\":\"(.*?)\"").getMatch(0);
        if ("failpass".equalsIgnoreCase(state)) {
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_FATAL, "Password wrong");
        } else {
            downloadLink.setProperty("pass", pass);
        }
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("http://netload.in/datei" + Netloadin.getID(link.getDownloadURL()) + ".htm");
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        showDialog = true;
        try {
            setBrowserExclusive();
            loginAPI(account, ai);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 800;
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
                    File checkFile = JDUtilities.getResourceFile("tmp/nltmp");
                    if (!checkFile.exists()) {
                        checkFile.mkdirs();
                        showFreeDialog("netload.in");
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
        setBrowserExclusive();
        workAroundTimeOut(br);
        requestFileInformation(downloadLink);
        checkShowFreeDialog();
        br.setDebug(true);
        this.setBrowserExclusive();
        String ID = getID(downloadLink.getDownloadURL());
        br.getPage("http://netload.in/json/datei" + ID + ".htm");
        checkPassword(downloadLink, br);
        checkErrors(downloadLink, br, false);
        checkLimit(downloadLink, br);
        String url = br.getRegex("link\":\"(http.*?)\"").getMatch(0);
        if (url != null) {
            url = url.replaceAll("\\\\/", "/");
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, false, 1);
            try {
                /* remove next major update */
                /* workaround for broken timeout in 0.9xx public */
                dl.getConnection().setConnectTimeout(30000);
                dl.getConnection().setReadTimeout(120000);
            } catch (Throwable e) {
            }
            dl.startDownload();
        } finally {
            logger.info("used serverurl: " + url);
        }

    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        setBrowserExclusive();
        workAroundTimeOut(br);
        requestFileInformation(downloadLink);
        showDialog = false;
        loginAPI(account, null);
        String cookie = br.getCookie("http://www.netload.in", "cookie_user");
        if (cookie == null) {
            if (br.containsHTML("too_many_logins_wait_5_min")) {
                logger.info("too_many_logins_wait_5_min");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            logger.severe("no cookie!");
            logger.severe(br.toString());
            try {
                logger.info(br.getHttpConnection().toString());
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 10 * 60 * 1000l);
        }
        String ID = getID(downloadLink.getDownloadURL());
        br.getPage("http://netload.in/json/datei" + ID + ".htm");
        checkPassword(downloadLink, br);
        checkErrors(downloadLink, br, true);
        workAroundTimeOut(br);
        String url = br.getRegex("link\":\"(http.*?)\"").getMatch(0);
        if (url != null) {
            url = url.replaceAll("\\\\/", "/");
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, url, true, 0);
            try {
                /* remove next major update */
                /* workaround for broken timeout in 0.9xx public */
                dl.getConnection().setConnectTimeout(30000);
                dl.getConnection().setReadTimeout(120000);
            } catch (Throwable e) {
            }
            if (!dl.startDownload()) {
                if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaderparseerror", "Unexpected rangeheader format:"))) {
                    logger.severe("Workaround for Netload Server-Problem! Setting Resume to false and Chunks to 1!");
                    downloadLink.setProperty("nochunk", true);
                    downloadLink.setChunksProgress(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } finally {
            logger.info("used serverurl: " + url);
        }
    }

    private void loginAPI(Account account, AccountInfo ai) throws IOException, PluginException {
        synchronized (LOGINLOCK) {
            workAroundTimeOut(br);
            if (ai == null) {
                ai = account.getAccountInfo();
                if (ai == null) {
                    ai = new AccountInfo();
                    account.setAccountInfo(ai);
                }
            }
            try {
                String res = br.getPage("http://api.netload.in/user_info.php?auth=BVm96BWDSoB4WkfbEhn42HgnjIe1ilMt&user_id=" + Encoding.urlEncode(account.getUser()) + "&user_password=" + Encoding.urlEncode(account.getPass()));
                if (res == null || res.trim().length() == 0) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                res = res.trim();
                account.setValid(true);
                if ("disallowed_agent".equalsIgnoreCase(res) || "unknown_auth".equalsIgnoreCase(res)) {
                    logger.severe("api reports: " + res);
                    ai.setStatus("api reports: " + res);
                    if (showDialog) UserIO.getInstance().requestMessageDialog(0, "Netload.in Premium Error", "Unexpected error occured during login: '" + res + "'\r\nPlease contact JDownloader Support.");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else if ("0".equalsIgnoreCase(res)) {
                    /* free user */
                    ai.setStatus("No premium user");
                    if (showDialog) UserIO.getInstance().requestMessageDialog(0, "Netload.in Premium Error", "Account '" + account.getUser() + "' is a free account and this not supported.\r\nPlease buy a Netload.in Premium account!");

                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    // gibt es laut netload nicht mehr
                    // } else if ("unknown_user".equalsIgnoreCase(res)) {
                    // ai.setStatus("Unknown user");
                    // if (showDialog)
                    // UserIO.getInstance().requestMessageDialog(0,
                    // "Netload.in Premium Error", "The username '" +
                    // account.getUser() +
                    // "' is unknown.\r\nPlease check your Username!");
                    //
                    // throw new PluginException(LinkStatus.ERROR_PREMIUM,
                    // PluginException.VALUE_ID_PREMIUM_DISABLE);
                    // } else if ("unknown_password".equalsIgnoreCase(res) ||
                    // "wrong_password".equalsIgnoreCase(res)) {
                    // ai.setStatus("Wrong password");
                    // if (showDialog)
                    // UserIO.getInstance().requestMessageDialog(0,
                    // "Netload.in Premium Error", "The username '" +
                    // account.getUser() +
                    // "' is ok, but the given password is wrong.\r\nPlease check your Password!");
                    // throw new PluginException(LinkStatus.ERROR_PREMIUM,
                    // PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if ("login_failed".equalsIgnoreCase(res)) {
                    ai.setStatus("Login failed");
                    if (showDialog) UserIO.getInstance().requestMessageDialog(0, "Netload.in Premium Error", "Login failed!\r\nPlease check your Username and Password!");

                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if ("-1".equalsIgnoreCase(res)) {
                    /* lifetime */
                    ai.setStatus("Lifetime premium");
                    ai.setValidUntil(-1);
                    return;
                } else {
                    /* normal premium */
                    ai.setStatus("Premium");
                    ai.setValidUntil(TimeFormatter.getMilliSeconds(res, "yyyy-MM-dd HH:mm", null));
                    if (ai.isExpired()) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }

            } catch (PluginException e) {
                try {
                    /* verbose debug */
                    logger.info(br.toString());
                    logger.info(br.getHttpConnection().toString());
                } catch (Throwable e2) {
                }
                throw e;
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException {
        try {
            DownloadLink urls[] = new DownloadLink[1];
            urls[0] = downloadLink;
            checkLinks(urls);
            if (!downloadLink.isAvailabilityStatusChecked()) return AvailableStatus.UNCHECKED;
            if (downloadLink.isAvailable()) return AvailableStatus.TRUE;
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } catch (PluginException e) {
            /* workaround for buggy api */
            /* workaround for stable */
            DownloadLink tmpLink = new DownloadLink(null, "temp", "temp", "temp", false);
            LinkStatus linkState = new LinkStatus(tmpLink);
            e.fillLinkStatus(linkState);
            if (linkState.hasStatus(LinkStatus.ERROR_FILE_NOT_FOUND)) {
                return websiteFileCheck(downloadLink);
            } else {
                throw e;
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        link.setProperty("nochunk", false);
    }

    @Override
    public void resetPluginGlobals() {

    }

    public AvailableStatus websiteFileCheck(DownloadLink downloadLink) throws PluginException {
        this.setBrowserExclusive();
        logger.info("FileCheckAPI error, try website check!");
        workAroundTimeOut(br);
        IOException ex = null;
        String id = Netloadin.getID(downloadLink.getDownloadURL());
        for (int i = 0; i < 5; i++) {
            try {
                Thread.sleep(500 + (i * 200));
            } catch (InterruptedException e) {
                return AvailableStatus.UNCHECKABLE;
            }
            ex = null;
            try {
                br.getPage("http://netload.in/index.php?id=10&file_id=" + id + "&lang=de");
                break;
            } catch (IOException e2) {
                ex = e2;
            }
        }
        if (ex != null) return AvailableStatus.UNCHECKABLE;
        String filename = br.getRegex("<div class=\"dl_first_filename\">(.*?)<").getMatch(0);
        String filesize = br.getRegex("<div class=\"dl_first_filename\">.*?style=.*?>.*?(\\d+.*?)<").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    /* TODO: remove me after 0.9xx public */
    private void workAroundTimeOut(Browser br) {
        try {
            if (br != null) {
                br.setConnectTimeout(30000);
                br.setReadTimeout(120000);
            }
        } catch (Throwable e) {
        }
    }
}