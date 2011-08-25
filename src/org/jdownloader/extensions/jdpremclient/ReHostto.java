package org.jdownloader.extensions.jdpremclient;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.ImageIcon;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.controlling.AccountController;
import jd.controlling.JDPluginLogger;
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

public class ReHostto extends PluginForHost implements JDPremInterface {

    private boolean                  proxyused    = false;
    private String                   infostring   = null;
    private PluginForHost            plugin       = null;
    private static boolean           enabled      = false;
    private static ArrayList<String> premiumHosts = new ArrayList<String>();
    private static final Object      LOCK         = new Object();

    public ReHostto(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://rehost.to/index.php?page=download");
        infostring = "rehost.to @ " + wrapper.getHost();
    }

    @Override
    public String getAGBLink() {
        if (plugin == null) return "http://rehost.to";
        return plugin.getAGBLink();
    }

    @Override
    public long getVersion() {
        if (plugin == null) return getVersion("$Revision: 13504 $");
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
        if (plugin == null) return "rehost.to";
        return plugin.getHost();
    }

    @Override
    public ConfigContainer getConfig() {
        if (plugin == null) return super.getConfig();
        return plugin.getConfig();
    }

    @Override
    public String getBuyPremiumUrl() {
        if (plugin == null) return "http://rehost.to/index.php?page=download";
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
        /* try rehost.to first */
        if (account == null) {
            if (handleReHost(downloadLink)) return;
        } else if (!PremiumCompoundExtension.preferLocalAccounts()) {
            if (handleReHost(downloadLink)) return;
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

    private boolean handleReHost(DownloadLink link) throws Exception {
        Account acc = null;
        synchronized (LOCK) {
            /* jdpremium enabled */
            if (!PremiumCompoundExtension.isStaticEnabled() || !enabled) return false;
            /* premium available for this host */
            if (!premiumHosts.contains(link.getHost())) return false;
            acc = AccountController.getInstance().getValidAccount("rehost.to");
            /* enabled account found? */
            if (acc == null || !acc.isEnabled()) return false;
        }
        proxyused = true;
        requestFileInformation(link);
        if (link.isAvailabilityStatusChecked() && !link.isAvailable()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        resetFavIcon();
        String user = Encoding.urlEncode(acc.getUser());
        String pw = Encoding.urlEncode(acc.getPass());
        br.setConnectTimeout(90 * 1000);
        br.setReadTimeout(90 * 1000);
        br.setDebug(true);
        dl = null;
        String url = Encoding.urlEncode(link.getDownloadURL());
        br.getPage("http://rehost.to/api.php?cmd=login&user=" + user + "&pass=" + pw);
        String long_ses = br.getRegex("long_ses=(.+)").getMatch(0);
        int retry = 0;
        while (true) {
            br.setFollowRedirects(false);
            br.postPage("http://rehost.to/process_download.php", "user=cookie&pass=" + long_ses + "&dl=" + url);
            if (br.getRedirectLocation() == null) continue;
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, br.getRedirectLocation(), resumePossible(this.getHost()), -5);
            if (dl.getConnection().getResponseCode() == 404) {
                /* file offline */
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!dl.getConnection().isContentDisposition()) {
                /* unknown error */
                br.followConnection();
                if (br.getURL().contains("error=download_failed") && retry++ <= 3) continue;
                if (br.containsHTML("The file is offline")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                logger.severe("Rehost: error!");
                logger.severe(br.toString());
                synchronized (LOCK) {
                    premiumHosts.remove(link.getHost());
                }
                return false;
            }
            dl.startDownload();
            return true;
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
            final boolean follow = br.isFollowingRedirects();
            br.setFollowRedirects(true);
            br.setConnectTimeout(60 * 1000);
            br.setReadTimeout(60 * 1000);
            br.setDebug(true);
            String username = Encoding.urlEncode(account.getUser());
            String pass = Encoding.urlEncode(account.getPass());
            String loginPage = null;
            long trafficLeft = 0;
            String ses = null;
            String hosts = null;
            long expire = 0;
            try {
                loginPage = br.getPage("http://rehost.to/api.php?cmd=login&user=" + username + "&pass=" + pass);
                ses = new Regex(loginPage, "ses=(.*?),").getMatch(0);
                if (ses == null) {
                    loginPage = null;
                } else {
                    br.getPage("http://rehost.to/api.php?cmd=get_premium_credits&ses=" + ses);
                    String infos[] = br.getRegex("(.*?)(,|$)").getColumn(0);
                    trafficLeft = Long.parseLong(infos[0]);
                    expire = Long.parseLong(infos[1]);
                    hosts = br.getPage("http://rehost.to/api.php?cmd=get_supported_och_dl&ses=" + ses);
                }
            } catch (Exception e) {
                account.setTempDisabled(true);
                account.setValid(true);
                resetAvailablePremium();
                ac.setStatus("Rehost Server Error, temp disabled" + restartReq);
                return ac;
            } finally {
                br.setFollowRedirects(follow);
            }
            if (loginPage == null || trafficLeft <= 0 || expire == 0) {
                account.setValid(false);
                account.setTempDisabled(false);
                ac.setStatus("Account invalid");
                resetAvailablePremium();
            } else {
                ac.setValidUntil(expire * 1000);
                ac.setTrafficLeft(trafficLeft * 1024 * 1024);
                synchronized (LOCK) {
                    premiumHosts.clear();
                    if (!"0".equals(hosts.trim())) {
                        String hoster[] = new Regex(hosts, "(.*?)(,|$)").getColumn(0);
                        if (hoster != null) {
                            for (String host : hoster) {
                                if (host == null || host.length() == 0) continue;
                                premiumHosts.add(host.trim());
                            }
                        }
                    }
                }
                account.setValid(true);
                if (premiumHosts.size() == 0) {
                    ac.setStatus(restartReq + "Account valid: 0 Hosts via Rehost.to available");
                } else {
                    ac.setStatus(restartReq + "Account valid: " + premiumHosts.size() + " Hosts via Rehost.to available");
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
            if (PremiumCompoundExtension.preferLocalAccounts() && account != null) {
                /* user prefers usage of local account */
                return plugin.getMaxSimultanDownload(account);
            } else if (PremiumCompoundExtension.isStaticEnabled() && enabled) {
                /* Rehost */
                synchronized (LOCK) {
                    if (premiumHosts.contains(plugin.getHost()) && AccountController.getInstance().getValidAccount("rehost.to") != null) return Integer.MAX_VALUE;
                }
            }
            return plugin.getMaxSimultanDownload(account);
        }
        return 0;
    }

    private boolean resumePossible(String hoster) {
        if (hoster != null) {
            if (hoster.contains("megaupload.com")) return true;
            if (hoster.contains("rapidshare.com")) return true;
            if (hoster.contains("oron.com")) return true;
            if (hoster.contains("netload.in")) return true;
            if (hoster.contains("uploaded.to")) return true;
            if (hoster.contains("x7.to")) return true;
            if (hoster.contains("freakshare.")) return true;
            if (hoster.contains("filesonic")) return true;
            if (hoster.contains("easy-share")) return true;
            if (hoster.contains("megaupload")) return true;
            if (hoster.contains("megavideo")) return true;
            if (hoster.contains("fileserve.com")) return true;
            if (hoster.contains("bitshare.com")) return true;
            if (hoster.contains("hotfile.com")) return true;
            if (hoster.contains("depositfiles.com")) return true;
        }
        return false;
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

    @Override
    public String getCustomFavIconURL() {
        if (proxyused) return "rehost.to";
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
