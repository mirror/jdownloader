package org.jdownloader.extensions.jdpremclient;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

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

public class Premium4me extends PluginForHost implements JDPremInterface {

    private boolean                  proxyused    = false;
    private String                   infostring   = null;
    private PluginForHost            plugin       = null;
    private static boolean           enabled      = false;
    private static java.util.List<String> premiumHosts = new ArrayList<String>();
    private static final Object      LOCK         = new Object();

    public Premium4me(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://premium4.me/");
        infostring = "Premium4.me @ " + wrapper.getLazy().getDisplayName();
    }

    @Override
    public String getAGBLink() {
        if (plugin == null) return "http://premium4.me/";
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
        if (plugin == null) return "premium4.me";
        return plugin.getHost();
    }

    @Override
    public ConfigContainer getConfig() {
        if (plugin == null) return super.getConfig();
        return plugin.getConfig();
    }

    @Override
    public String getBuyPremiumUrl() {
        if (plugin == null) return "http://premium4.me/";
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
        /* try premium4me first */

        if (proxyused = true) {
            /* failed, now try normal */
            proxyused = false;

        }
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

    private boolean handlePremium4me(DownloadLink link) throws Exception {
        Account acc = null;
        synchronized (LOCK) {
            /* jdpremium enabled */

            /* premium available for this host */
            if (!premiumHosts.contains(link.getHost())) return false;
            // acc =
            // AccountController.getInstance().getValidAccount("premium4.me");
            /* enabled account found? */
            if (acc == null || !acc.isEnabled()) return false;
        }
        proxyused = true;
        requestFileInformation(link);
        if (link.isAvailabilityStatusChecked() && !link.isAvailable()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        boolean dofollow = br.isFollowingRedirects();
        try {
            br.setFollowRedirects(true);
            br.setConnectTimeout(90 * 1000);
            br.setReadTimeout(90 * 1000);
            br.setDebug(true);
            dl = null;
            String user = Encoding.urlEncode(acc.getUser());
            String pw = Encoding.urlEncode(acc.getPass());
            String url = link.getDownloadURL().replaceFirst("https?://", "");
            url = Encoding.urlEncode(url);
            br.postPageRaw("http://premium4.me/login.php", "{\"u\":\"" + user + "\", \"p\":\"" + pw + "\", \"r\":true}");
            if (br.getCookie("http://premium4.me", "auth") == null) {
                resetAvailablePremium();
                acc.setValid(false);
                return false;
            }
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, "http://premium4.me/getfile.php?link=" + url, true, 1);
            if (dl.getConnection().getResponseCode() == 404) {
                /* file offline */
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!dl.getConnection().isContentDisposition()) {
                br.followConnection();
                logger.severe("Premium4Me(Error): " + br.toString());
                /*
                 * after x retries we disable this host and retry with normal plugin
                 */
                if (link.getLinkStatus().getRetryCount() >= 3) {
                    synchronized (LOCK) {
                        premiumHosts.remove(link.getHost());
                    }
                    /* reset retrycounter */
                    link.getLinkStatus().setRetryCount(0);
                    return false;
                }
                String msg = "(" + link.getLinkStatus().getRetryCount() + 1 + "/" + 3 + ")";
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 10 * 1000l);
            }

            dl.startDownload();
        } finally {
            br.setFollowRedirects(dofollow);
        }
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

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        if (plugin == null) {
            String restartReq = enabled == false ? "(Restart required) " : "";
            AccountInfo ac = new AccountInfo();
            br.setConnectTimeout(60 * 1000);
            br.setReadTimeout(60 * 1000);
            br.setDebug(true);
            String username = Encoding.urlEncode(account.getUser());
            String pass = Encoding.urlEncode(account.getPass());
            // String page = null;
            String hosts = null;
            String traffic = null;
            br.setFollowRedirects(true);
            br.setAcceptLanguage("en, en-gb;q=0.8");
            try {
                br.postPageRaw("http://premium4.me/login.php", "{\"u\":\"" + username + "\", \"p\":\"" + pass + "\", \"r\":true}");
                if (br.getCookie("http://premium4.me", "auth") != null) {
                    // page = br.getPage("http://premium4.me/account.php");
                    traffic = br.getPage("http://premium4.me/traffic.php").trim() + " MB";
                    hosts = br.getPage("http://premium4.me/hosters.php");
                }
            } catch (Exception e) {
                account.setTempDisabled(true);
                account.setValid(true);
                resetAvailablePremium();
                ac.setStatus("Premium4Me Server Error, temp disabled" + restartReq);
                return ac;
            }
            if (br.getCookie("http://premium4.me", "auth") == null) {
                resetAvailablePremium();
                account.setValid(false);
                return ac;
            }
            ac.setTrafficLeft(traffic);
            // String date = new Regex(page, "\"d\":\"(.*?)\",").getMatch(0);
            account.setValid(true);
            /* expire date does currently not work */
            // ac.setValidUntil(TimeFormatter.getMilliSeconds(date,
            // "dd MMM yyyy", null));
            synchronized (LOCK) {
                premiumHosts.clear();
                String hoster[] = new Regex(hosts.trim(), "(.+?)(;|$)").getColumn(0);
                if (hoster != null) {
                    for (String host : hoster) {
                        if (hosts == null || host.length() == 0) continue;
                        premiumHosts.add(host.trim());
                    }
                }
            }
            if (account.isValid()) {
                if (premiumHosts.size() == 0) {
                    ac.setStatus(restartReq + "Account valid: 0 Hosts via premium4.me available");
                } else {
                    ac.setStatus(restartReq + "Account valid: " + premiumHosts.size() + " Hosts via premium4.me available");
                }
            } else {
                account.setTempDisabled(false);
                account.setValid(false);
                resetAvailablePremium();
                ac.setStatus("Account invalid");
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
        if (proxyused) return "premium4.me";
        if (plugin != null) return plugin.getCustomFavIconURL();
        return null;
    }

}
