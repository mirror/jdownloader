package org.jdownloader.extensions.jdpremclient;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.ImageIcon;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.controlling.AccountController;
import jd.controlling.JDPluginLogger;
import jd.http.Browser;
import jd.nutils.Formatter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
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

public class PremShare extends PluginForHost implements JDPremInterface {

    private boolean proxyused = false;
    private String infostring = null;
    private PluginForHost plugin = null;
    private static ArrayList<String> premiumHosts = new ArrayList<String>();
    private static final Object LOCK = new Object();
    private static boolean enabled = false;

    public void setReplacedPlugin(PluginForHost plugin) {
        this.plugin = plugin;
    }

    public PremShare(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
        infostring = "JDPremium @ " + wrapper.getHost();
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
        if (plugin == null) return "jdownloader.org";
        return plugin.getHost();
    }

    @Override
    public ConfigContainer getConfig() {
        if (plugin == null) return super.getConfig();
        return plugin.getConfig();
    }

    @Override
    public String getBuyPremiumUrl() {
        if (plugin == null) return "http://www.jdownloader.org";
        return plugin.getBuyPremiumUrl();
    }

    @Override
    public String getAGBLink() {
        if (plugin == null) return "http://www.jdownloader.org";
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
        /* try premshare first */
        if (account == null) {
            if (handleJDPremServ(downloadLink)) return;
        } else if (!JDPremium.preferLocalAccounts()) {
            if (handleJDPremServ(downloadLink)) return;
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

    private boolean handleJDPremServ(DownloadLink link) throws Exception {
        Account acc = null;
        String jdpremServer = null;
        synchronized (LOCK) {
            /* jdpremium enabled */
            if (!JDPremium.isEnabled() || !enabled) return false;
            /* premium available for this host */
            if (!premiumHosts.contains(link.getHost())) return false;
            acc = AccountController.getInstance().getValidAccount("jdownloader.org");
            /* enabled account found? */
            if (acc == null || !acc.isEnabled()) return false;
            jdpremServer = JDPremium.getJDPremServer();
            if (jdpremServer == null || jdpremServer.length() == 0) return false;
        }
        proxyused = true;
        requestFileInformation(link);
        if (link.isAvailabilityStatusChecked() && !link.isAvailable()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        resetFavIcon();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        br.setDebug(true);
        try {
            br.getPage(jdpremServer);
        } catch (Throwable e) {
            return false;
        }
        /* add and force download */
        Form form = new Form();
        form.setAction("/");
        form.setMethod(MethodType.GET);
        form.put("force", Encoding.urlEncode(link.getDownloadURL()));
        form.put("username", Encoding.urlEncode(acc.getUser()));
        form.put("password", Encoding.urlEncode(acc.getPass()));
        if (link.getStringProperty("pass", null) != null) form.put("dlpw", Encoding.urlEncode(link.getStringProperty("pass", null)));
        showMessage(link, "Add Link to Queue");
        /* first request,with force */
        String status = br.submitForm(form);
        int refresh = 10 * 1000;
        while (true) {
            if (status.contains("ERROR: -20")) {
                synchronized (premiumHosts) {
                    premiumHosts.remove(link.getHost());
                }
                logger.info("account has no rights to use this host");
                return false;
            }
            if (status.contains("ERROR: -10") && !status.contains("ERROR: -100")) {
                /* account invalid */
                acc.setEnabled(false);
                logger.info("JDPremium account invalid");
                return false;
            }
            if (status.length() == 0 || status.contains("ERROR")) {
                /* error found */
                logger.info(status);
                return false;
            }
            if (status.contains("ERROR: -50")) {
                String reason = new Regex(status, "\\|\\|(.+)").getMatch(0);
                if (reason != null) throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, reason);
                throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED);
            }
            if (status.contains("OK: 100")) {
                /* download complete */
                link.getTransferStatus().usePremium(true);
                break;
            }
            if (status.contains("OK: 0")) {
                /* download queued */
                refresh = 10 * 1000;
                showMessage(link, "Queued");
            } else if (status.contains("OK: 1") || status.contains("OK: 2")) {
                boolean premium = status.contains("OK: 1");
                /* download in progress */
                /* update premium flag */
                link.getTransferStatus().usePremium(premium);
                String ints[] = new Regex(status, "(\\d+)/(\\d+)/(\\d+)").getRow(0);
                Long size = Long.parseLong(ints[1]);
                Long current = Long.parseLong(ints[0]);
                Long speed = Long.parseLong(ints[2]);
                String sp = Formatter.formatReadable(speed) + "/s";
                String done = Formatter.formatReadable(current) + "/" + Formatter.formatReadable(size);
                /* show speed and ETA */
                if (speed != 0 && size > 0) {
                    Long left = (size - current) / speed;
                    showMessage(link, done + " | " + Formatter.formatSeconds(left) + " | " + sp);
                } else {
                    showMessage(link, "Downloading with " + sp);
                }
                refresh = 5 * 1000;
            }
            Thread.sleep(refresh);
            status = br.submitForm(form);
            if (status == null) status = "";
        }
        /* download now */
        form.setAction("/");
        form.put("download", "1");
        showMessage(link, "Request Download");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, form, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            /* unknown error */
            br.followConnection();
            logger.severe("JDPremServServer: error!");
            logger.severe(br.toString());
            synchronized (LOCK) {
                premiumHosts.remove(link.getHost());
            }
            return false;
        }
        dl.startDownload();
        return true;
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

    private void resetAvailablePremium() {
        synchronized (LOCK) {
            premiumHosts.clear();
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        if (plugin == null) {
            Browser br = new Browser();
            String restartReq = enabled == false ? "(Restart required) " : "";
            AccountInfo ac = new AccountInfo();
            String jdpremServer = JDPremium.getJDPremServer();
            if (jdpremServer == null || jdpremServer.length() == 0) {
                ac.setStatus("No JDPremServ set!");
                account.setValid(false);
                resetAvailablePremium();
                return ac;
            }
            br.setConnectTimeout(60 * 1000);
            br.setReadTimeout(60 * 1000);
            String page = null;
            try {
                br.getPage(jdpremServer);
                Form form = new Form();
                form.setAction("/?info=1");
                form.setMethod(MethodType.GET);
                form.put("username", Encoding.urlEncode(account.getUser()));
                form.put("password", Encoding.urlEncode(account.getPass()));
                page = br.submitForm(form);
                if ("unknown HTTP response".equalsIgnoreCase(br.getHttpConnection().getResponseMessage())) throw new Exception("JDPrem not online!");
            } catch (Exception e) {
                account.setTempDisabled(true);
                account.setValid(true);
                resetAvailablePremium();
                ac.setStatus("JDPrem Server Error, temp disabled" + restartReq);
                return ac;
            }
            /* user info */

            if (page.contains("OK: USER")) {
                /* parse available premium hosts */
                String supportedHosts = new Regex(page, "HOSTS:(.+)").getMatch(0);
                synchronized (LOCK) {
                    premiumHosts.clear();
                    if (supportedHosts != null) {
                        String hosts[] = new Regex(supportedHosts, "(.*?)\\|\\|").getColumn(0);
                        if (hosts != null) {
                            for (String host : hosts) {
                                premiumHosts.add(host.trim());
                            }
                        }
                    }
                    account.setValid(true);
                    account.setTempDisabled(false);
                    if (premiumHosts.size() == 0) {
                        ac.setStatus(restartReq + "Account valid: 0 Hosts via PremShare available");
                    } else {
                        ac.setStatus(restartReq + "Account valid: " + premiumHosts.size() + " Hosts via PremShare available");
                    }
                }
            } else {
                account.setValid(false);
                account.setTempDisabled(false);
                ac.setStatus("Account invalid");
                resetAvailablePremium();
            }
            return ac;
        } else
            return plugin.fetchAccountInfo(account);
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        if (plugin != null) plugin.resetDownloadlink(link);
        synchronized (LOCK) {
            if (JDPremium.isEnabled() && enabled && premiumHosts.contains(link.getHost())) {
                String jdpremServer = JDPremium.getJDPremServer();
                try {
                    if (jdpremServer == null || jdpremServer.length() == 0) return;
                    Browser br = new Browser();
                    /* lower timeout is okay here */
                    br.setConnectTimeout(5000);
                    br.getPage(jdpremServer);
                    Form form = new Form();
                    form.setAction("/?reset=1");
                    form.setMethod(MethodType.GET);
                    form.put("link", Encoding.urlEncode(link.getDownloadURL()));
                    br.submitForm(form);
                    logger.info("Remote Queue file reset: " + br.toString());
                } catch (Throwable e) {
                    logger.severe("Could not reset Remote Queue file");
                }
            }
        }
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
                /* PremShare */
                synchronized (LOCK) {
                    if (premiumHosts.contains(plugin.getHost()) && AccountController.getInstance().getValidAccount("jdownloader.org") != null) return Integer.MAX_VALUE;
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
        if (proxyused) return "jdownloader.org";
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
