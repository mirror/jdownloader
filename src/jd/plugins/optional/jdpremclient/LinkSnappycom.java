package jd.plugins.optional.jdpremclient;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.ImageIcon;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.controlling.AccountController;
import jd.controlling.JDPluginLogger;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
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
import org.appwork.utils.formatter.TimeFormatter;

public class LinkSnappycom extends PluginForHost implements JDPremInterface {

    private boolean                  proxyused    = false;
    private String                   infostring   = null;
    private PluginForHost            plugin       = null;
    private static boolean           counted      = false;
    private static boolean           enabled      = false;
    private static ArrayList<String> premiumHosts = new ArrayList<String>();
    private static final Object      LOCK         = new Object();

    public LinkSnappycom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.linksnappy.com/members/index.php?act=register");
        infostring = "LinkSnappy.com @ " + wrapper.getHost();
    }

    @Override
    public String getAGBLink() {
        if (plugin == null) return "http://www.linksnappy.com/";
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
        if (plugin == null) return "linksnappy.com";
        return plugin.getHost();
    }

    @Override
    public ConfigContainer getConfig() {
        if (plugin == null) return super.getConfig();
        return plugin.getConfig();
    }

    @Override
    public String getBuyPremiumUrl() {
        if (plugin == null) return "http://www.linksnappy.com/members/index.php?act=register";
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
        /* try linksnappy.com first */
        if (account == null) {
            if (handleLinkSnappy(downloadLink)) return;
        } else if (!JDPremium.preferLocalAccounts()) {
            if (handleLinkSnappy(downloadLink)) return;
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

    private boolean handleLinkSnappy(DownloadLink link) throws Exception {
        Account acc = null;
        synchronized (LOCK) {
            /* jdpremium enabled */
            if (!JDPremium.isEnabled() || !enabled) return false;
            /* premium available for this host */
            if (!premiumHosts.contains(link.getHost())) return false;
            acc = AccountController.getInstance().getValidAccount("linksnappy.com");
            /* enabled account found? */
            if (acc == null || !acc.isEnabled()) return false;
        }
        proxyused = true;
        requestFileInformation(link);
        if (link.isAvailabilityStatusChecked() && !link.isAvailable()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        resetFavIcon();
        br.setConnectTimeout(90 * 1000);
        br.setReadTimeout(90 * 1000);
        br.setDebug(true);
        dl = null;
        try {
            login(acc, false);
        } catch (PluginException e) {
            resetAvailablePremium();
            acc.setValid(false);
            return false;
        }
        boolean savedLinkValid = false;
        String genlink = link.getStringProperty("genLink", null);
        /* remove generated link */
        link.setProperty("genLink", null);
        if (genlink != null) {
            /* try saved link first */
            try {
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, genlink, resumePossible(this.getHost()), 1);
                if (dl.getConnection().isContentDisposition()) {
                    savedLinkValid = true;
                }
            } catch (final Throwable e) {
                savedLinkValid = false;
            } finally {
                if (savedLinkValid == false) {
                    try {
                        dl.getConnection().disconnect();
                    } catch (final Throwable e1) {
                    }
                }
            }
        }
        if (savedLinkValid == false) {
            /* generate new downloadlink */
            String postData = "genLinks={\"links\" : \"" + Encoding.urlEncode(link.getDownloadURL()) + "\", \"Kcookies\" : \"" + br.getCookie("www.linksnappy.com", "lseSavePass") + "\"}";
            String response = br.postPageRaw("http://linksnappy.com/lseAPI.php", postData);
            response = response.replaceAll("\\\\/", "/");
            String status = new Regex(response, "status\":\"(.*?)\"").getMatch(0);
            // String error = new Regex(response, "error\":(.*?)}").getMatch(0);
            genlink = new Regex(response, "generated\":\"(http.*?)\"").getMatch(0);
            if ("FAILED".equalsIgnoreCase(status)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if ("OK".equalsIgnoreCase(status) && genlink != null) {
                br.setFollowRedirects(true);
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, genlink, resumePossible(this.getHost()), 1);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (dl.getConnection().getResponseCode() == 404) {
                /* file offline */
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!dl.getConnection().isContentDisposition()) {
                /* unknown error */
                br.followConnection();
                if (br.containsHTML("this download server is disabled at the moment")) throw new PluginException(LinkStatus.ERROR_RETRY);
                if (br.containsHTML("The file is offline")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                logger.severe("LinkSnappy: error!");
                logger.severe(br.toString());
                synchronized (LOCK) {
                    premiumHosts.remove(link.getHost());
                }
                return false;
            }
        }
        try {
            Browser br2 = new Browser();
            br2.setFollowRedirects(true);
            if (!counted) br2.getPage("http://www.jdownloader.org/scripts/linksnappy.php?id=" + Encoding.urlEncode(acc.getUser()));
            counted = true;
        } catch (Exception e) {
        }
        /* save generated link */
        link.setProperty("genLink", genlink);
        link.getTransferStatus().usePremium(true);
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

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws PluginException, IOException {
        synchronized (LOCK) {
            this.setBrowserExclusive();
            this.br.setDebug(true);
            final Object ret = account.getProperty("cookies", null);
            if (ret != null && ret instanceof HashMap<?, ?> && !force) {
                final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                if (cookies.containsKey("lseSavePass") && account.isValid()) {
                    for (final String key : cookies.keySet()) {
                        this.br.setCookie("www.linksnappy.com", key, cookies.get(key));
                    }
                    return;
                }
            }
            try {
                br.postPage("http://www.linksnappy.com/members/index.php?act=login", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&submit=Login");
            } catch (final Exception e) {
            }
            boolean invalid = false;
            final String premCookie = this.br.getCookie("www.linksnappy.com", "lseSavePass");
            if (this.br.containsHTML("Wrong username and")) {
                invalid = true;
            }
            br.getPage("http://www.linksnappy.com/members/index.php?act=index");
            if (!br.containsHTML("\">Active<")) {
                invalid = true;
            }
            if (premCookie == null || invalid) {
                account.setProperty("cookies", null);
                if (invalid) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
                AccountInfo ai = account.getAccountInfo();
                if (ai == null) {
                    ai = new AccountInfo();
                    account.setAccountInfo(ai);
                }
                ai.setStatus("ServerProblems(1), will try again in few minutes!");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = this.br.getCookies("www.linksnappy.com");
            for (final Cookie c : add.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            account.setProperty("cookies", cookies);
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
            String hosts = null;
            try {
                hosts = br.getPage("http://www.linksnappy.com/hosters.html");
                login(account, true);
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
                ac.setStatus("LinkSnappy Server Error, temp disabled" + restartReq);
                return ac;
            }
            String validUntil = br.getRegex("Expire Date:</strong>(.*?)\\(").getMatch(0);
            account.setValid(true);
            ac.setValidUntil(TimeFormatter.getMilliSeconds(validUntil, "dd MMM yyyy", null));
            synchronized (LOCK) {
                premiumHosts.clear();
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
            if (premiumHosts.size() == 0) {
                ac.setStatus(restartReq + "Account valid: 0 Hosts via LinkSnappy.com available");
            } else {
                ac.setStatus(restartReq + "Account valid: " + premiumHosts.size() + " Hosts via LinkSnappy.com available");
            }
            return ac;
        } else
            return plugin.fetchAccountInfo(account);
    }

    private boolean resumePossible(String hoster) {
        if (hoster != null) {
            if (hoster.contains("megaupload.com")) return true;
            if (hoster.contains("rapidshare.com")) return true;
            if (hoster.contains("oron.com")) return true;
            if (hoster.contains("netload.in")) return true;
            if (hoster.contains("uploaded.to")) return true;
            if (hoster.contains("x7.to")) return true;
            if (hoster.contains("shragle.com")) return true;
            if (hoster.contains("freakshare.")) return true;
            if (hoster.contains("fileserve.com")) return true;
            if (hoster.contains("bitshare.com")) return true;
            if (hoster.contains("hotfile.com")) return true;
        }
        return false;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        link.setProperty("genLink", null);
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
                    if (premiumHosts.contains(plugin.getHost()) && AccountController.getInstance().getValidAccount("linksnappy.com") != null) return Integer.MAX_VALUE;
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

    @Override
    public String getCustomFavIconURL() {
        if (proxyused) return "linksnappy.com";
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
