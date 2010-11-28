package jd.plugins.optional.jdpremclient;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.controlling.AccountController;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.http.Browser;
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

public class Ochloadorg extends PluginForHost implements JDPremInterface {

    private boolean                  proxyused           = false;
    private String                   infostring          = null;
    private PluginForHost            plugin              = null;
    private static boolean           enabled             = false;
    private static ArrayList<String> premiumHosts        = new ArrayList<String>();
    private static final Object      LOCK                = new Object();
    private static final int         MAXDOWNLOADS        = 5;
    private volatile static int      currentMaxDownloads = MAXDOWNLOADS;
    private static boolean           counted             = false;

    public Ochloadorg(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.ochload.org/Profil");
        infostring = "OCHload.Org @ " + wrapper.getHost();
    }

    @Override
    public String getAGBLink() {
        if (plugin == null) return "http://www.ochload.org/";
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
        if (plugin == null) return "ochload.org";
        return plugin.getHost();
    }

    @Override
    public ConfigContainer getConfig() {
        if (plugin == null) return super.getConfig();
        return plugin.getConfig();
    }

    @Override
    public String getBuyPremiumUrl() {
        if (plugin == null) return "http://www.ochload.org";
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
        /* try ochload.org first */
        if (account == null) {
            if (handleOchLoad(downloadLink)) return;
        } else if (!JDPremium.preferLocalAccounts()) {
            if (handleOchLoad(downloadLink)) return;
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

    private boolean handleOchLoad(DownloadLink link) throws Exception {
        synchronized (LOCK) {
            if (currentMaxDownloads == 0) return false;
            currentMaxDownloads--;
        }
        try {
            Account acc = null;
            synchronized (LOCK) {
                /* jdpremium enabled */
                if (!JDPremium.isEnabled() || !enabled) return false;
                /* premium available for this host */
                if (!premiumHosts.contains(link.getHost())) return false;
                acc = AccountController.getInstance().getValidAccount("ochload.org");
                /* enabled account found? */
                if (acc == null || !acc.isEnabled()) return false;
            }
            proxyused = true;
            requestFileInformation(link);
            if (link.isAvailabilityStatusChecked() && !link.isAvailable()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String login = Encoding.Base64Encode(acc.getUser());
            String pw = Encoding.Base64Encode(acc.getPass());
            br.setConnectTimeout(90 * 1000);
            br.setReadTimeout(90 * 1000);
            br.setDebug(true);
            dl = null;
            String url = Encoding.Base64Encode(link.getDownloadURL());
            int conTry = 1;
            while (true) {
                showMessage(link, "ConnectTry: " + conTry);
                try {
                    dl = jd.plugins.BrowserAdapter.openDownload(br, link, "http://www.ochload.org/?apiv2&method=startDownload&jd=1&nick=" + login + "&pass=" + pw + "&url=" + url, false, 1);
                    break;
                } catch (Throwable e) {
                    try {
                        dl.getConnection().disconnect();
                    } catch (Throwable e2) {
                    }
                    if (++conTry > 4) { return false; }
                }
                this.sleep(15 * 1000l, link, "Error, wait and retry");
            }
            if (dl.getConnection().getResponseCode() == 404) {
                /* file offline */
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!dl.getConnection().isContentDisposition()) {
                /* unknown error */
                br.followConnection();
                logger.severe("OchLoad: error!");
                logger.severe(br.toString());
                String error = Encoding.urlEncode(br.toString());
                /* post error message to api */
                br.postPage("http://www.ochload.org/?apiv2&method=reportError", "hoster=" + link.getHost() + "&error=" + error + "&nick=" + Encoding.urlEncode(acc.getUser()) + "&link=" + Encoding.urlEncode(link.getDownloadURL()));
                synchronized (LOCK) {
                    premiumHosts.remove(link.getHost());
                }
                return false;
            }
            try {
                Browser br2 = Browser.getNewBrowser(br);
                br2.setFollowRedirects(true);
                if (!counted) br2.getPage("http://www.jdownloader.org/scripts/ochload.php?id=" + Encoding.urlEncode(acc.getUser()));
                counted = true;
            } catch (Exception e) {
            }
            dl.startDownload();
            return true;
        } finally {
            synchronized (LOCK) {
                currentMaxDownloads++;
                if (currentMaxDownloads > MAXDOWNLOADS) currentMaxDownloads = MAXDOWNLOADS;
            }
        }
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

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        if (plugin == null) {
            String restartReq = enabled == false ? "(Restart required) " : "";
            AccountInfo ac = new AccountInfo();
            br.setConnectTimeout(60 * 1000);
            br.setReadTimeout(60 * 1000);
            br.setDebug(true);
            String username = Encoding.Base64Encode(account.getUser());
            String pass = Encoding.Base64Encode(account.getPass());
            String page = null;
            String hosts = null;
            try {
                page = br.getPage("http://www.ochload.org/?apiv2&nick=" + username + "&pass=" + pass + "&method=getInfos4jD");
                hosts = br.getPage("http://www.ochload.org/hoster.html");
            } catch (Exception e) {
                account.setTempDisabled(true);
                account.setValid(true);
                resetAvailablePremium();
                synchronized (LOCK) {
                    premiumHosts.clear();
                }
                ac.setStatus("OchLoad Server Error, temp disabled" + restartReq);
                return ac;
            }
            boolean isPremium = page.startsWith("1");
            if (!isPremium) {
                account.setValid(false);
                account.setTempDisabled(false);
                ac.setStatus("Account invalid");
                resetAvailablePremium();
            } else {
                String infos[] = new Regex(page, "(.*?):(.*?):(.+)").getRow(0);
                ac.setValidUntil(Long.parseLong(infos[2]) * 1000);
                boolean megaupload = "1".equalsIgnoreCase(infos[1]);
                synchronized (LOCK) {
                    premiumHosts.clear();
                    if (megaupload) premiumHosts.add("megaupload.com");
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
                    ac.setStatus(restartReq + "Account valid: 0 Hosts via OCHLoad.org available");
                } else {
                    ac.setStatus(restartReq + "Account valid: " + premiumHosts.size() + " Hosts via OCHLoad.org available");
                }
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
                /* OchLoad */
                synchronized (LOCK) {
                    if (currentMaxDownloads > 0 && premiumHosts.contains(plugin.getHost()) && AccountController.getInstance().getValidAccount("ochload.org") != null) return MAXDOWNLOADS;
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
