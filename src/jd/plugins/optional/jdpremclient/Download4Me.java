package jd.plugins.optional.jdpremclient;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.controlling.AccountController;
import jd.controlling.JDPluginLogger;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.TransferStatus;
import jd.plugins.download.DownloadInterface;

public class Download4Me extends PluginForHost implements JDPremInterface {

    private boolean                  proxyused    = false;
    private String                   infostring   = null;
    private PluginForHost            plugin       = null;
    private static boolean           enabled      = false;
    private static ArrayList<String> premiumHosts = new ArrayList<String>();
    private static final Object      LOCK         = new Object();

    public Download4Me(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.dwnld4me.com/signup.php");
        infostring = "Download4Me.com @ " + wrapper.getHost();
    }

    @Override
    public String getAGBLink() {
        if (plugin == null) return "http://www.dwnld4me.com";
        return plugin.getAGBLink();
    }

    @Override
    public synchronized int getFreeConnections() {
        if (plugin != null) return plugin.getFreeConnections();
        return super.getFreeConnections();
    }

    @Override
    public int getMaxConnections() {
        if (plugin != null) return plugin.getMaxConnections();
        return super.getMaxConnections();
    }

    @Override
    public long getVersion() {
        if (plugin == null) return getVersion("$Revision$");
        return plugin.getVersion();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (plugin != null) {
            plugin.actionPerformed(e);
        } else {
            super.actionPerformed(e);
        }
    }

    @Override
    public String getHost() {
        if (plugin == null) return "dwnld4me.com";
        return plugin.getHost();
    }

    @Override
    public ConfigContainer getConfig() {
        if (plugin == null) return super.getConfig();
        return plugin.getConfig();
    }

    @Override
    public String getBuyPremiumUrl() {
        if (plugin == null) return "http://www.dwnld4me.com";
        return plugin.getBuyPremiumUrl();
    }

    @Override
    public void handle(final DownloadLink downloadLink, final Account account) throws Exception {
        if (plugin == null) {
            super.handle(downloadLink, account);
            return;
        }
        proxyused = false;
        /* copied from PluginForHost */
        final TransferStatus transferStatus = downloadLink.getTransferStatus();
        transferStatus.usePremium(false);
        transferStatus.setResumeSupport(false);
        try {
            while (waitForNextStartAllowed(downloadLink)) {
            }
        } catch (InterruptedException e) {
            return;
        }
        putLastTimeStarted(System.currentTimeMillis());
        if (!isAGBChecked()) {
            logger.severe("AGB not signed : " + this.getWrapper().getID());
            downloadLink.getLinkStatus().addStatus(LinkStatus.ERROR_AGB_NOT_SIGNED);
            return;
        }
        /* try download4me first */
        if (account == null) {
            if (handleDwnld4Me(downloadLink)) return;
        } else if (!JDPremium.preferLocalAccounts()) {
            if (handleDwnld4Me(downloadLink)) return;
        }
        /* failed, now try normal */
        proxyused = false;
        plugin.handle(downloadLink, account);
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        if (plugin == null) return;
        proxyused = false;
        br.reset();
        plugin.handleFree(link);
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        if (plugin == null) return;
        proxyused = false;
        br.reset();
        plugin.handlePremium(downloadLink, account);
    }

    @Override
    public void setLogger(JDPluginLogger logger) {
        this.logger = logger;
        if (plugin != null) plugin.setLogger(logger);
    }

    @Override
    public JDPluginLogger getLogger() {
        if (plugin != null) plugin.getLogger();
        return (JDPluginLogger) logger;
    }

    @Override
    public void setBrowser(Browser br) {
        this.br = br;
        if (plugin != null) plugin.setBrowser(br);
    }

    @Override
    public Browser getBrowser() {
        if (plugin != null) return plugin.getBrowser();
        return this.br;
    }

    @Override
    public void clean() {
        super.clean();
        if (plugin != null) plugin.clean();
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
        link.requestGuiUpdate();
    }

    private boolean handleDwnld4Me(DownloadLink link) throws Exception {
        Account acc = null;
        synchronized (LOCK) {
            /* jdpremium enabled */
            if (!JDPremium.isEnabled() || !enabled) return false;
            /* premium available for this host */
            if (!premiumHosts.contains(link.getHost())) return false;
            acc = AccountController.getInstance().getValidAccount("dwnld4me.com");
            /* enabled account found? */
            if (acc == null || !acc.isEnabled()) return false;
        }
        proxyused = true;
        requestFileInformation(link);
        if (link.isAvailabilityStatusChecked() && !link.isAvailable()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Browser br = new Browser();
        this.login(acc, br, false);
        br.setConnectTimeout(90 * 1000);
        br.setReadTimeout(90 * 1000);
        br.setDebug(true);
        dl = null;
        br.getPage("http://dwnld4me.com/download.php");
        final String uid = br.getRegex("uid\" type=\"hidden\" value=\"(.*?)\"").getMatch(0);
        if (uid == null) {
            logger.severe("Download4Me: error!");
            logger.severe(br.toString());
            synchronized (LOCK) {
                premiumHosts.remove(link.getHost());
            }
            return false;
        }
        final String url = Encoding.urlEncode_light(link.getDownloadURL() + "\r\n");
        String dlUrl = null;
        /* needed? */
        final Browser brc = br.cloneBrowser();
        brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        /* this call blocks */
        new Thread(new Runnable() {
            public void run() {
                URLConnectionAdapter con = null;
                try {
                    con = brc.openGetConnection("http://www.dwnld4me.com/ajax/test.py?uid=" + uid + "&urls=" + url);
                } catch (Throwable e) {
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
            }
        }).start();

        int refresh = 10 * 1000;
        String id = null;
        while (true) {
            br.getPage("http://www.dwnld4me.com/ajax/progressBar.py?uid=" + uid + "&urls=" + url);
            if (br.containsHTML("<b>Download</b>")) {
                /* finished */
                id = br.getRegex("download.php\\?id=(.*?)'").getMatch(0);
                showMessage(link, "Download4Me finished");
                break;
            } else if (br.containsHTML("Starting download")) {
                /* still in queue */
                refresh = 10 * 1000;
                showMessage(link, "Queued");
            } else if (br.containsHTML("progress.png")) {
                /* loading */
                link.getTransferStatus().usePremium(true);
                String done = br.getRegex("<td>([0-9.]+)%<").getMatch(0);
                showMessage(link, done + " % Done");
                refresh = 5 * 1000;
            } else if (br.containsHTML("Error downloading")) {
                logger.severe("Download4Me: error!");
                logger.severe(br.toString());
                synchronized (LOCK) {
                    premiumHosts.remove(link.getHost());
                }
                return false;
            }
            Thread.sleep(refresh);
        }
        if (id == null) {
            logger.severe("Download4Me: error!");
            logger.severe(br.toString());
            synchronized (LOCK) {
                premiumHosts.remove(link.getHost());
            }
            return false;
        }
        link.getTransferStatus().usePremium(true);
        dlUrl = "http://www.dwnld4me.com/data/download.php?id=" + id;
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlUrl, true, 1);
        if (!dl.getConnection().isContentDisposition()) {
            /* unknown error */
            br.followConnection();
            logger.severe("Download4Me: error!");
            logger.severe(br.toString());
            synchronized (LOCK) {
                premiumHosts.remove(link.getHost());
            }
            return false;
        }
        dl.startDownload();
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        if (plugin == null) return AvailableStatus.UNCHECKABLE;
        return plugin.requestFileInformation(parameter);
    }

    @Override
    public void reset() {
        if (plugin != null) {
            plugin.reset();
        }
    }

    @Override
    public void init() {
        if (plugin != null) {
            plugin.init();
        } else {
            super.init();
        }
    }

    private void resetAvailablePremium() {
        synchronized (LOCK) {
            premiumHosts.clear();
        }
    }

    private Browser login(Account account, Browser br, boolean forceUpdate) throws Exception {
        synchronized (LOCK) {
            if (br == null) {
                br = new Browser();
            }
            if (forceUpdate == false && account.getStringProperty("cookie", null) != null) {
                br.setCookie("http://www.dwnld4me.com", "PHPSESSID", account.getStringProperty("cookie", null));
                return br;
            }
            String username = Encoding.urlEncode(account.getUser());
            String pass = JDHash.getMD5(account.getPass());
            br.getPage("http://www.dwnld4me.com");
            br.postPage("http://www.dwnld4me.com/login_form.php", "user=" + username + "&pass=" + pass + "&sublogin=");
            br.getPage("http://www.dwnld4me.com/account.php");
            if (!br.containsHTML("Account created on")) {
                account.setProperty("cookie", null);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            String cookie = br.getCookie("http://www.dwnld4me.com", "PHPSESSID");
            if (cookie == null) {
                account.setProperty("cookie", null);
                throw new Exception("no cookie");
            }
            account.setProperty("cookie", cookie);
            return br;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        if (plugin == null) {
            String restartReq = enabled == false ? "(Restart required) " : "";
            AccountInfo ac = new AccountInfo();
            br.setConnectTimeout(60 * 1000);
            br.setReadTimeout(60 * 1000);
            br.setDebug(true);
            try {
                login(account, br, true);
            } catch (Exception e) {
                if (e instanceof PluginException) {
                    account.setValid(false);
                    resetAvailablePremium();
                    ac.setStatus("Invalid Account");
                    return ac;
                }
                account.setTempDisabled(true);
                account.setValid(true);
                resetAvailablePremium();
                synchronized (LOCK) {
                    premiumHosts.clear();
                }
                ac.setStatus("Dwnld4Me Server Error, temp disabled" + restartReq);
                return ac;
            }
            String validUntil = br.getRegex("Account expires on</b></td><td>(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
            if (validUntil != null) ac.setValidUntil(Regex.getMilliSeconds(validUntil, "yyyy-MM-dd HH:mm:ss", null));
            ac.setStatus("Valid Account");
            synchronized (LOCK) {
                premiumHosts.clear();
                String hosts = "rapidshare.com;hotfile.com;megaupload.com";
                if (hosts != null) {
                    String hoster[] = new Regex(hosts, "(.*?)(;|$)").getColumn(0);
                    if (hosts != null) {
                        for (String host : hoster) {
                            if (hosts == null || host.length() == 0) continue;
                            premiumHosts.add(host.trim());
                        }
                    }
                }
            }
            account.setValid(true);
            if (premiumHosts.size() == 0) {
                ac.setStatus(restartReq + "Account valid: 0 Hosts via Download4Me.com available");
            } else {
                ac.setStatus(restartReq + "Account valid: " + premiumHosts.size() + " Hosts via Download4Me.com available");
            }
            return ac;
        } else
            return plugin.fetchAccountInfo(account);
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        if (plugin != null) plugin.resetDownloadlink(link);
    }

    @Override
    public String getSessionInfo() {
        if (proxyused || plugin == null) return infostring;
        return plugin.getSessionInfo();
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        if (plugin != null) plugin.correctDownloadLink(link);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        if (plugin != null) return plugin.getMaxSimultanFreeDownloadNum();
        return super.getMaxSimultanFreeDownloadNum();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        if (plugin != null) return plugin.getMaxSimultanPremiumDownloadNum();
        return super.getMaxSimultanPremiumDownloadNum();
    }

    @Override
    public int getMaxSimultanDownload(final Account account) {
        if (plugin != null) {
            if (JDPremium.preferLocalAccounts() && account != null) {
                /* user prefers usage of local account */
                return plugin.getMaxSimultanDownload(account);
            } else if (JDPremium.isEnabled() && enabled) {
                /* Download4Me */
                synchronized (LOCK) {
                    if (premiumHosts.contains(plugin.getHost()) && AccountController.getInstance().getValidAccount("dwnld4me.com") != null) return Integer.MAX_VALUE;
                }
            }
            return plugin.getMaxSimultanDownload(account);
        }
        return 0;
    }

    @Override
    public boolean checkLinks(DownloadLink[] urls) {
        if (plugin == null) return false;
        return plugin.checkLinks(urls);
    }

    @Override
    public String getFileInformationString(DownloadLink downloadLink) {
        if (proxyused || plugin == null) return "";
        return plugin.getFileInformationString(downloadLink);
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        if (plugin == null) return super.createMenuitems();
        return plugin.createMenuitems();
    }

    @Override
    public ArrayList<Account> getPremiumAccounts() {
        if (plugin != null) return plugin.getPremiumAccounts();
        return super.getPremiumAccounts();
    }

    public void setReplacedPlugin(PluginForHost plugin) {
        this.plugin = plugin;
    }

    public void enablePlugin() {
        enabled = true;
    }

    @Override
    public int getTimegapBetweenConnections() {
        if (plugin != null) return plugin.getTimegapBetweenConnections();
        return super.getTimegapBetweenConnections();
    }

    @Override
    public boolean rewriteHost(DownloadLink link) {
        if (plugin != null) return plugin.rewriteHost(link);
        return false;
    }

    @Override
    public void setDownloadInterface(DownloadInterface dl) {
        this.dl = dl;
        if (plugin != null) plugin.setDownloadInterface(dl);
    }

}
