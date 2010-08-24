package jd.plugins.optional.jdpremclient;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

public class Ochloadorg extends PluginForHost implements JDPremInterface {

    private boolean proxyused = false;
    private String infostring = null;
    private PluginForHost plugin = null;
    private static ArrayList<String> premiumHosts = new ArrayList<String>();
    private static final Object LOCK = new Object();
    private static final int MAXDOWNLOADS = 3;
    private static int currentMaxDownloads = 3;

    public Ochloadorg(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.ochload.org/Profil");
        infostring = "OchLoad.Org @ " + wrapper.getHost();
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
    public String getVersion() {
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
    public void handleFree(DownloadLink link) throws Exception {
        if (plugin == null) return;
        proxyused = false;
        if (handleOchLoad(link)) return;
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

    private boolean handleOchLoad(DownloadLink link) throws Exception {
        synchronized (LOCK) {
            if (currentMaxDownloads == 0) return false;
            currentMaxDownloads = Math.max(0, --currentMaxDownloads);
        }
        try {
            Account acc = null;
            synchronized (LOCK) {
                /* jdpremium enabled */
                if (!JDPremium.isEnabled()) return false;
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
            br = new Browser();
            br.setConnectTimeout(30000);
            br.setDebug(true);
            dl = null;
            String url = Encoding.Base64Encode(link.getDownloadURL());
            try {
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, "http://www.ochload.org/?apiv2&method=startDownload&nick=" + login + "&pass=" + pw + "&url=" + url, false, 1);
            } catch (Throwable e) {
                try {
                    dl.getConnection().disconnect();
                } catch (Throwable e2) {
                }
                return false;
            }
            if (!dl.getConnection().isContentDisposition()) {
                /* unknown error */
                br.followConnection();
                logger.severe("OchLoad: error!");
                logger.severe(br.toString());
                synchronized (LOCK) {
                    premiumHosts.remove(link.getHost());
                }
                return false;
            }
            dl.startDownload();
            return true;
        } finally {
            currentMaxDownloads = Math.min(MAXDOWNLOADS, ++currentMaxDownloads);
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
            AccountInfo ac = new AccountInfo();
            br = new Browser();
            br.setConnectTimeout(30000);
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
                ac.setStatus("OchLoad Server Error, temp disabled");
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
                    ac.setStatus("Account valid: 0 Hosts via OchLoad available");
                } else {
                    ac.setStatus("Account valid: " + premiumHosts.size() + " Hosts via OchLoad available");
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
        if (handleOchLoad(downloadLink)) return;
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
            if (JDPremium.isEnabled()) {
                synchronized (LOCK) {
                    if (premiumHosts.contains(plugin.getHost())) return currentMaxDownloads;
                }
            }
            return plugin.getMaxSimultanFreeDownloadNum();
        }
        return super.getMaxSimultanFreeDownloadNum();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        if (plugin != null) {
            if (JDPremium.isEnabled()) {
                synchronized (LOCK) {
                    if (premiumHosts.contains(plugin.getHost())) {
                        if (currentMaxDownloads == 0) { return Integer.MIN_VALUE; }
                        return currentMaxDownloads;
                    }
                }
            }
            return plugin.getMaxSimultanPremiumDownloadNum();
        }
        return super.getMaxSimultanPremiumDownloadNum();
    }

    @Override
    public int getMaxSimultanDownload(final Account account) {
        if (plugin != null) {
            synchronized (LOCK) {
                if (premiumHosts.contains(plugin.getHost())) {
                    if (currentMaxDownloads == 0) { return Integer.MIN_VALUE; }
                    return currentMaxDownloads;
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

    public void setReplacedPlugin(PluginForHost plugin) {
        this.plugin = plugin;
    }

}
