package jd.plugins.optional.jdpremclient;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.http.Browser;
import jd.nutils.Formatter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;

public class PremShare extends PluginForHost {

    private boolean proxyused = false;
    private String infostring = null;
    private PluginForHost plugin = null;
    private static ArrayList<String> premiumHosts = new ArrayList<String>();
    private static final Object LOCK = new Object();

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
    public String getVersion() {
        if (plugin == null) return getVersion("$Revision: 86 $");
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
    public boolean isAGBChecked() {
        if (plugin == null) return true;
        return plugin.isAGBChecked();
    }

    @Override
    public void setAGBChecked(boolean value) {
        if (plugin == null) return;
        plugin.setAGBChecked(value);
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
        proxyused = false;
        plugin.clean();
        plugin.handleFree(link);
    }

    @Override
    public void clean() {
        super.clean();
        if (plugin != null) plugin.clean();
    }

    private boolean handleJDPremServ(DownloadLink link) throws Exception {
        Account acc = null;
        synchronized (LOCK) {
            /* jdpremium enabled */
            if (!JDPremium.isEnabled()) return false;
            /* premium available for this host */
            if (!premiumHosts.contains(link.getHost())) return false;
            acc = JDPremium.getAccount();
            /* enabled account found? */
            if (acc == null || !acc.isEnabled()) return false;
        }
        proxyused = true;
        requestFileInformation(link);
        br = new Browser();
        br.setDebug(true);
        String jdpremServer = JDUtilities.getOptionalPlugin("jdpremium").getPluginConfig().getStringProperty("SERVER");
        try {
            if (jdpremServer == null || jdpremServer.length() == 0) return false;
            br.getPage(jdpremServer);
        } catch (Exception e) {
            return false;
        }
        /* add and force download */
        Form form = new Form();
        form.setAction("/?force=" + link.getDownloadURL());
        form.setMethod(MethodType.GET);
        form.put("username", Encoding.urlEncode(acc.getUser()));
        form.put("password", Encoding.urlEncode(acc.getPass()));
        showMessage(link, "Add Link to Queue");
        /* first request,with force */
        String status = br.submitForm(form);
        if (status == null) status = "";
        if (status.contains("ERROR: -10")) {
            /* account invalid */
            acc.setEnabled(false);
            logger.info("JDPremium account invalid");
            return false;
        }
        int refresh = 10 * 1000;
        while (true) {
            if (status.length() == 0 || status.contains("ERROR")) {
                /* error found */
                logger.info(status);
                return false;
            }
            /* add host from available premium list */
            synchronized (premiumHosts) {
                if (!premiumHosts.contains(link.getHost())) premiumHosts.add(link.getHost());
            }
            if (status.contains("OK: 100")) {
                /* download complete */
                break;
            }
            if (status.contains("OK: 0")) {
                /* download queued */
                refresh = 10 * 1000;
                showMessage(link, "Queued");
            } else if (status.contains("OK: 1")) {
                /* download in progress */
                /* show speed and ETA */
                String ints[] = new Regex(status, "(\\d+)/(\\d+)/(\\d+)").getRow(0);
                Long size = Long.parseLong(ints[1]);
                Long current = Long.parseLong(ints[0]);
                Long speed = Long.parseLong(ints[2]);
                String sp = Formatter.formatReadable(speed) + "/s";
                if (speed != 0 && size > 0) {
                    Long left = (size - current) / speed;
                    showMessage(link, "Downloading " + Formatter.formatSeconds(left) + " " + sp);
                } else {
                    showMessage(link, "Downloading " + sp);
                }
                refresh = 5 * 1000;
            }
            Thread.sleep(refresh);
            status = br.submitForm(form);
        }
        /* download now */
        form.setAction("/?download=1&force=" + link.getDownloadURL());
        showMessage(link, "Request Download");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, form, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            /* unknown error */
            br.followConnection();
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
        if (plugin != null) plugin.reset();
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        if (plugin == null) {
            AccountInfo ac = new AccountInfo();
            String jdpremServer = JDUtilities.getOptionalPlugin("jdpremium").getPluginConfig().getStringProperty("SERVER");
            if (jdpremServer == null || jdpremServer.length() == 0) {
                ac.setStatus("No JDPremServ set!");
                account.setValid(false);
                synchronized (LOCK) {
                    /* no jdpremium available */
                    premiumHosts.clear();
                }
                return ac;
            }
            br = new Browser();
            br.setDebug(true);
            try {
                br.getPage(jdpremServer);
            } catch (Exception e) {
                account.setTempDisabled(true);
                /* no jdpremium available */
                synchronized (LOCK) {
                    premiumHosts.clear();
                }
                ac.setStatus("JDPrem Server Error, temp disabled");
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
                account.setValid(true);
                ac.setStatus("Account valid");
            } else {
                account.setValid(false);
                ac.setStatus("Account invalid");
                /* no jdpremium available */
                synchronized (LOCK) {
                    premiumHosts.clear();
                }
            }
            return ac;
        } else
            return plugin.fetchAccountInfo(account);
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        if (plugin == null) return;
        proxyused = false;
        if (handleJDPremServ(downloadLink)) return;
        proxyused = false;
        plugin.clean();
        plugin.handlePremium(downloadLink, account);
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
        if (plugin != null) {
            synchronized (LOCK) {
                if (premiumHosts.contains(plugin.getHost())) return Integer.MAX_VALUE;
            }
            return plugin.getMaxSimultanFreeDownloadNum();
        }
        return super.getMaxSimultanFreeDownloadNum();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        if (plugin != null) {
            synchronized (LOCK) {
                if (premiumHosts.contains(plugin.getHost())) return Integer.MAX_VALUE;
            }
            return plugin.getMaxSimultanPremiumDownloadNum();
        }
        return super.getMaxSimultanPremiumDownloadNum();
    }

    @Override
    public int getMaxSimultanDownload(final Account account) {
        if (plugin != null) {
            synchronized (LOCK) {
                if (premiumHosts.contains(plugin.getHost())) return Integer.MAX_VALUE;
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

    @Override
    public String getCustomFavIconURL() {
        if (proxyused) return "http://www.jdownloader.org/";
        return null;
    }

}
