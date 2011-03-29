package org.jdownloader.extensions.jdpremclient;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.ImageIcon;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.controlling.AccountController;
import jd.controlling.JDPluginLogger;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.TransferStatus;
import jd.plugins.download.DownloadInterface;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;

public class MultiShare extends PluginForHost implements JDPremInterface {

    private boolean                  proxyused    = false;
    private String                   infostring   = null;
    private PluginForHost            plugin       = null;
    private static boolean           enabled      = false;
    private static ArrayList<String> premiumHosts = new ArrayList<String>();
    private static final Object      LOCK         = new Object();

    public MultiShare(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.multishare.cz/cenik/");
        infostring = "MultishareCz @ " + wrapper.getHost();
    }

    public void setReplacedPlugin(PluginForHost plugin) {
        this.plugin = plugin;
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
        if (plugin == null) return getVersion("$Revision: 12739 $");
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
    public String getHost() {
        if (plugin == null) return "multishare.cz";
        return plugin.getHost();
    }

    @Override
    public ConfigContainer getConfig() {
        if (plugin == null) return super.getConfig();
        return plugin.getConfig();
    }

    @Override
    public String getBuyPremiumUrl() {
        if (plugin == null) return "http://www.multishare.cz/cenik/";
        return plugin.getBuyPremiumUrl();
    }

    @Override
    public String getAGBLink() {
        if (plugin == null) return "http://www.multishare.cz/kontakt/";
        return plugin.getAGBLink();
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
        /* try multishare.cz first */
        if (account == null) {
            if (handleMultiShare(downloadLink)) return;
        } else if (!PremiumCompoundExtension.preferLocalAccounts()) {
            if (handleMultiShare(downloadLink)) return;
        }
        if (proxyused = true) {
            /* failed, now try normal */
            proxyused = false;
            resetFavIcon();
        }
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

    private boolean handleMultiShare(DownloadLink link) throws Exception {
        Account acc = null;
        synchronized (LOCK) {
            /* jdpremium enabled */
            if (!PremiumCompoundExtension.isStaticEnabled() || !enabled) return false;
            /* premium available for this host */
            if (!premiumHosts.contains(link.getHost())) return false;
            acc = AccountController.getInstance().getValidAccount("multishare.cz");
            /* enabled account found? */
            if (acc == null || !acc.isEnabled()) return false;
        }
        proxyused = true;
        requestFileInformation(link);
        if (link.isAvailabilityStatusChecked() && !link.isAvailable()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(false);
        /* login to get u_ID and u_HASH */
        br.getPage("http://www.multishare.cz/");
        br.postPage("http://www.multishare.cz/html/prihlaseni_process.php", "jmeno=" + Encoding.urlEncode(acc.getUser()) + "&heslo=" + Encoding.urlEncode(acc.getPass()) + "&trvale=ano&akce=P%C5%99ihl%C3%A1sit");
        if (br.getCookie("http://www.multishare.cz", "sess_ID") == null) {
            acc.setValid(false);
            return false;
        }
        br.getPage("http://www.multishare.cz/");
        String trafficleft = br.getRegex("Kredit:</span>.*?<strong>(.*?)</strong").getMatch(0);
        if (trafficleft == null) trafficleft = br.getRegex("class=\"big\"><strong>Kredit:(.*?)</strong>").getMatch(0);
        if (trafficleft != null) {
            trafficleft = trafficleft.replace("&nbsp;", "");
            trafficleft = trafficleft.replace(" ", "");
            AccountInfo ai = acc.getAccountInfo();
            synchronized (LOCK) {
                if (ai != null) ai.setTrafficLeft(SizeFormatter.getSize(trafficleft));
            }
        }
        Form form = br.getForm(0);
        if (form == null) return false;
        /* parse u_ID and u_HASH */
        String u_ID = form.getVarsMap().get("u_ID");
        String u_HASH = form.getVarsMap().get("u_hash");
        if (u_ID == null || u_HASH == null) {
            logger.severe("MultiShare: error!");
            logger.severe(br.toString());
            synchronized (LOCK) {
                premiumHosts.remove(link.getHost());
            }
            return false;
        }
        showMessage(link, "Phase 1/3: Check Download");
        String url = Encoding.urlEncode(link.getDownloadURL());
        /* request Download */
        String page = br.postPage("http://www.multishare.cz/html/mms_ajax.php", "link=" + url);
        if (page.contains("Vašeho kreditu bude")) {
            showMessage(link, "Phase 2/3: Request Download");
            /* download is possible */
            br.getPage("http://www.multishare.cz/html/mms_process.php?link=" + url + "&u_ID=" + u_ID + "&u_hash=" + u_HASH + "&over=ano");
            if (br.containsHTML("ready")) {
                showMessage(link, "Phase 3/3: Download");
                /* download is ready */
                /* build final URL */
                String rnd = "dl" + Math.round(Math.random() * 10000l * Math.random());
                String fUrl = "http://" + rnd + "mms.multishare.cz/html/mms_process.php?link=" + url + "&u_ID=" + u_ID + "&u_hash=" + u_HASH;
                /*
                 * resume is supported, chunks make no sense and did not work
                 * for me either
                 */
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, fUrl, true, -10);
                if (dl.getConnection().isContentDisposition()) {
                    /* contentdisposition, lets download it */
                    dl.startDownload();
                    return true;
                } else {
                    /*
                     * download is not contentdisposition, so remove this host
                     * from premiumHosts list
                     */
                    br.followConnection();
                }
            }
        }
        showMessage(link, "Error: continue without Multishare");
        /* some error occured */
        logger.severe("MultiShare: error!");
        logger.severe(br.toString());
        /* temp disabled the host */
        synchronized (LOCK) {
            premiumHosts.remove(link.getHost());
        }
        return false;
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

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        if (plugin == null) {
            /* this plugin do not have fetchAccountInfo */
            return null;
        } else if (plugin.getHost().equalsIgnoreCase("multishare.cz")) {
            /* we first check account and then check possible hosters */
            String restartReq = enabled == false ? "(Restart required)" : "";
            synchronized (LOCK) {
                AccountInfo ai = null;
                try {
                    ai = plugin.fetchAccountInfo(account);
                    return ai;
                } finally {
                    premiumHosts.clear();
                    if (account.isValid()) {
                        try {
                            String hostsSup = br.getPage("http://www.multishare.cz/html/mms_support.php");
                            String[] hosts = Regex.getLines(hostsSup);
                            if (hosts != null) {
                                for (String host : hosts) {
                                    premiumHosts.add(host);
                                }
                            }
                        } catch (Throwable e) {
                            logger.info("Could not fetch ServerList from Multishare: " + e.toString());
                        }
                        if (ai != null) {
                            /* set Statusmessage of account */
                            if (premiumHosts.size() == 0) {
                                ai.setStatus(restartReq + "Account valid: 0 Hosts via MultiShare.cz available");
                            } else {
                                ai.setStatus(restartReq + "Account valid: " + premiumHosts.size() + " Hosts via MultiShare.cz available");
                            }
                        }
                    }
                }
            }
        }
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
            if (PremiumCompoundExtension.preferLocalAccounts() && account != null) {
                /* user prefers usage of local account */
                return plugin.getMaxSimultanDownload(account);
            } else if (PremiumCompoundExtension.isStaticEnabled() && enabled) {
                synchronized (LOCK) {
                    if (premiumHosts.contains(plugin.getHost()) && AccountController.getInstance().getValidAccount("multishare.cz") != null) return Integer.MAX_VALUE;
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
    public ArrayList<jd.gui.swing.jdgui.menu.MenuAction> createMenuitems() {
        if (plugin == null) return super.createMenuitems();
        return plugin.createMenuitems();
    }

    @Override
    public ArrayList<Account> getPremiumAccounts() {
        if (plugin != null) return plugin.getPremiumAccounts();
        return super.getPremiumAccounts();
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

    @Override
    public String getCustomFavIconURL() {
        if (proxyused) return "multishare.cz";
        if (plugin != null) return plugin.getCustomFavIconURL();
        return null;
    }

    @Override
    public void setFavIcon(ImageIcon icon) {
        if (plugin != null) plugin.setFavIcon(icon);
        this.hosterIcon = icon;
    }

    @Override
    public void resetFavIcon() {
        if (plugin != null) plugin.resetFavIcon();
        hosterIconRequested = false;
        hosterIcon = null;
    }

}
