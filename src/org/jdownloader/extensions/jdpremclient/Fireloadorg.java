package org.jdownloader.extensions.jdpremclient;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.http.Browser;
import jd.nutils.Formatter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

import org.appwork.utils.Regex;

public class Fireloadorg extends PluginForHost implements JDPremInterface {

    private boolean                       proxyused      = false;
    private String                        infostring     = null;
    private PluginForHost                 plugin         = null;
    private static boolean                enabled        = false;
    private static java.util.List<String>      premiumHosts   = new ArrayList<String>();
    private static final Object           LOCK           = new Object();
    private volatile static int           MAXDOWNLOADS   = 5;
    private volatile static AtomicInteger currentRunning = new AtomicInteger(0);

    public Fireloadorg(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://fireload.org");
        infostring = "Fireload.org @ " + wrapper.getLazy().getDisplayName();
    }

    @Override
    public String getAGBLink() {
        if (plugin == null) return "http://fireload.org";
        return plugin.getAGBLink();
    }

    @Override
    public long getVersion() {
        if (plugin == null) return Formatter.getRevision("$Revision$");
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
        if (plugin == null) return "fireload.org";
        return plugin.getHost();
    }

    @Override
    public ConfigContainer getConfig() {
        if (plugin == null) return super.getConfig();
        return plugin.getConfig();
    }

    @Override
    public String getBuyPremiumUrl() {
        if (plugin == null) return "http://fireload.org";
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
        try {
            while (waitForNextStartAllowed(downloadLink)) {
            }
        } catch (InterruptedException e) {
            return;
        }
        putLastTimeStarted(System.currentTimeMillis());
        /* try fireload.org first */

        plugin.handle(downloadLink, account);
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        if (plugin == null) return;
        proxyused = false;

        plugin.handleFree(link);
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        if (plugin == null) return;
        proxyused = false;

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
    }

    private boolean handleFireload(DownloadLink link) throws Exception {
        synchronized (LOCK) {
            if (currentRunning.get() > MAXDOWNLOADS) return false;
            currentRunning.incrementAndGet();
        }
        try {
            Account acc = null;
            synchronized (LOCK) {
                /* jdpremium enabled */

                /* premium available for this host */
                if (!premiumHosts.contains(link.getHost())) return false;
                // acc =
                // AccountController.getInstance().getValidAccount("fireload.org");
                /* enabled account found? */
                if (acc == null || !acc.isEnabled()) return false;
            }
            proxyused = true;
            requestFileInformation(link);
            if (link.isAvailabilityStatusChecked() && !link.isAvailable()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

            String user = Encoding.urlEncode(acc.getUser());
            String pw = Encoding.urlEncode(acc.getPass());
            br.setConnectTimeout(90 * 1000);
            br.setReadTimeout(90 * 1000);
            br.setDebug(true);
            dl = null;
            String url = Encoding.urlEncode(link.getDownloadURL());
            int conTry = 1;
            while (true) {
                showMessage(link, "ConnectTry: " + conTry);
                try {
                    dl = jd.plugins.BrowserAdapter.openDownload(br, link, "http://dl.fireload.org/srv_dl.php?name=" + user + "&pass=" + pw + "&url=" + url, false, 1);
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
                if (br.containsHTML("The file is offline")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                logger.severe("Fireload: error!");
                logger.severe(br.toString());
                synchronized (LOCK) {
                    premiumHosts.remove(link.getHost());
                }
                return false;
            }
            dl.startDownload();
            return true;
        } finally {
            synchronized (LOCK) {
                currentRunning.decrementAndGet();
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
            final boolean follow = br.isFollowingRedirects();
            br.setFollowRedirects(true);
            br.setConnectTimeout(60 * 1000);
            br.setReadTimeout(60 * 1000);
            br.setDebug(true);
            String username = Encoding.urlEncode(account.getUser());
            String pass = Encoding.urlEncode(account.getPass());
            String page = null;
            String hosts = null;
            try {
                page = br.getPage("http://fireload.org/srv_acc.php?name=" + username + "&pass=" + pass);
                hosts = br.getPage("http://fireload.org/srv_hoster.php");
            } catch (Exception e) {
                account.setTempDisabled(true);
                account.setValid(true);
                resetAvailablePremium();
                ac.setStatus("Fireload Server Error, temp disabled" + restartReq);
                return ac;
            } finally {
                br.setFollowRedirects(follow);
            }
            if (page.startsWith("1:")) {
                account.setValid(true);
                String valid = new Regex(page, "1:(.+)").getMatch(0);
                ac.setValidUntil(Long.parseLong(valid) * 1000);
                synchronized (LOCK) {
                    premiumHosts.clear();
                    if (hosts != null) {
                        String hoster[] = new Regex(hosts, "(.+?)(;|$)").getColumn(0);
                        if (hosts != null) {
                            for (String host : hoster) {
                                if (hosts == null || host.length() == 0) continue;
                                host = host.replaceFirst("freakshare\\.com", "freakshare.net");
                                premiumHosts.add(host.trim());
                            }
                        }
                    }
                }
                if (premiumHosts.size() == 0) {
                    ac.setStatus(restartReq + "Account valid: 0 Hosts via Fireload.org available");
                } else {
                    ac.setStatus(restartReq + "Account valid: " + premiumHosts.size() + " Hosts via Fireload.org available");
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
    public boolean checkLinks(DownloadLink[] urls) {
        if (plugin == null) return false;
        return plugin.checkLinks(urls);
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
        if (proxyused) return "fireload.org";
        if (plugin != null) return plugin.getCustomFavIconURL();
        return null;
    }

}
