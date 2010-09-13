package jd.plugins.optional.jdpremclient;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.nutils.Formatter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.download.DownloadInterface;

public class PremShare extends PluginForHost implements JDPremInterface {

    private boolean                  proxyused    = false;
    private String                   infostring   = null;
    private PluginForHost            plugin       = null;
    private static ArrayList<String> premiumHosts = new ArrayList<String>();
    private static final Object      LOCK         = new Object();
    private static boolean           enabled      = false;

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
    public void handleFree(DownloadLink link) throws Exception {
        if (plugin == null) return;
        proxyused = false;
        if (handleJDPremServ(link)) return;
        link.getTransferStatus().usePremium(false);
        proxyused = false;
        plugin.clean();
        plugin.handleFree(link);
    }

    @Override
    public void setBrowser(Browser br) {
        this.br = br;
        if (plugin != null) plugin.setBrowser(br);
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
        br = new Browser();
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
            String restartReq = enabled == false ? "(Restart required)" : "";
            AccountInfo ac = new AccountInfo();
            String jdpremServer = JDPremium.getJDPremServer();
            if (jdpremServer == null || jdpremServer.length() == 0) {
                ac.setStatus("No JDPremServ set!");
                account.setValid(false);
                resetAvailablePremium();
                return ac;
            }
            br = new Browser();
            br.setConnectTimeout(60 * 1000);
            br.setReadTimeout(60 * 1000);
            try {
                br.getPage(jdpremServer);
            } catch (Exception e) {
                account.setTempDisabled(true);
                account.setValid(true);
                resetAvailablePremium();
                synchronized (LOCK) {
                    premiumHosts.clear();
                }
                ac.setStatus("JDPrem Server Error, temp disabled" + restartReq);
                return ac;
            }
            /* user info */
            Form form = new Form();
            form.setAction("/?info=1");
            form.setMethod(MethodType.GET);
            form.put("username", Encoding.urlEncode(account.getUser()));
            form.put("password", Encoding.urlEncode(account.getPass()));            
            String page = br.submitForm(form);
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
                        ac.setStatus("Account valid: 0 Hosts via PremShare available" + restartReq);
                    } else {
                        ac.setStatus("Account valid: " + premiumHosts.size() + " Hosts via PremShare available" + restartReq);
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
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        if (plugin == null) return;
        proxyused = false;
        if (!JDPremium.preferLocalAccounts()) {
            if (handleJDPremServ(downloadLink)) return;
            proxyused = false;
        }
        plugin.clean();
        plugin.handlePremium(downloadLink, account);
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        if (plugin != null) plugin.resetDownloadlink(link);
        synchronized (LOCK) {
            if (JDPremium.isEnabled() && enabled && premiumHosts.contains(link.getHost())) {
                String jdpremServer = JDPremium.getJDPremServer();
                try {
                    if (jdpremServer == null || jdpremServer.length() == 0) return;
                    br = new Browser();
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
    public void setDownloadInterface(DownloadInterface dl2) {
        this.dl = dl2;
        if (plugin != null) plugin.setDownloadInterface(dl2);
    }

    @Override
    public boolean rewriteHost(DownloadLink link) {
        if (plugin != null) return plugin.rewriteHost(link);
        return false;
    }

}
