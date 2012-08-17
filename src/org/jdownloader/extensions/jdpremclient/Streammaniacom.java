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

public class Streammaniacom extends PluginForHost implements JDPremInterface {

    private boolean                  proxyused    = false;
    private String                   infostring   = null;
    private PluginForHost            plugin       = null;
    private static boolean           enabled      = false;
    private static java.util.List<String> premiumHosts = new ArrayList<String>();
    private static final Object      LOCK         = new Object();

    public Streammaniacom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.streammania.com/");
        infostring = "Streammania.com @ " + wrapper.getLazy().getDisplayName();
    }

    @Override
    public String getAGBLink() {
        if (plugin == null) return "http://www.streammania.com/";
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
        if (plugin == null) return "streammania.com";
        return plugin.getHost();
    }

    @Override
    public ConfigContainer getConfig() {
        if (plugin == null) return super.getConfig();
        return plugin.getConfig();
    }

    @Override
    public String getBuyPremiumUrl() {
        if (plugin == null) return "http://www.streammania.com/premium.php";
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
        /* try streammania.com first */

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

    private boolean handleStreammania(DownloadLink link) throws Exception {
        Account acc = null;
        synchronized (LOCK) {
            /* jdpremium enabled */

            /* premium available for this host */
            if (!premiumHosts.contains(link.getHost())) return false;
            // acc =
            // AccountController.getInstance().getValidAccount("streammania.com");
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
            boolean savedLinkValid = false;
            String genlink = link.getStringProperty("genLinkStreammania", null);
            /* remove generated link */
            link.setProperty("genLinkStreammania", null);
            if (genlink != null) {
                /* try saved link first */
                try {
                    dl = jd.plugins.BrowserAdapter.openDownload(br, link, genlink, true, 1);
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
            if (!savedLinkValid) {
                String user = Encoding.urlEncode(acc.getUser());
                String pw = Encoding.urlEncode(acc.getPass());
                String url = Encoding.urlEncode(link.getDownloadURL());
                genlink = br.getPage("http://www.streammania.com/api/get_ddl.php?login=" + user + "&password=" + pw + "&url=" + url);
                if (!genlink.startsWith("http://")) {
                    logger.severe("Streammania(Error): " + genlink);
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
                /* TODO: add support for chunks */
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, genlink, true, 1);
                if (dl.getConnection().getResponseCode() == 404) {
                    /* file offline */
                    dl.getConnection().disconnect();
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (!dl.getConnection().isContentDisposition()) {
                    /* unknown error */
                    br.followConnection();
                    logger.severe("Streammania(Error): " + br.toString());
                    synchronized (LOCK) {
                        premiumHosts.remove(link.getHost());
                    }
                    return false;
                }
            }
            /* save generated link */
            link.setProperty("genLinkStreammania", genlink);
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
            String page = null;
            String hosts = null;
            try {
                page = br.getPage("http://www.streammania.com/api/get_pa_info.php?login=" + username + "&password=" + pass);
                hosts = br.getPage("http://www.streammania.com/api/get_filehosters.php");
            } catch (Exception e) {
                account.setTempDisabled(true);
                account.setValid(true);
                resetAvailablePremium();
                ac.setStatus("Streammania Server Error, temp disabled" + restartReq);
                return ac;
            }
            if (page.startsWith("ERROR: Auth")) {
                account.setValid(false);
                resetAvailablePremium();
                ac.setStatus(page);
                return ac;
            }
            /* parse api response in easy2handle hashmap */
            String info[] = new Regex(page, "(\\d+)($|\\|)").getColumn(0);
            boolean isunlimited = "1".equalsIgnoreCase(info[1]);
            long validUntil = Long.parseLong(info[2]);
            long inC = Long.parseLong(info[0]) * 1024 * 1024l;
            long outC = Long.parseLong(info[3]) * 1024 * 1024l;
            if (validUntil == 0) {
                account.setValid(false);
                resetAvailablePremium();
                return ac;
            }
            ac.setValidUntil(validUntil * 1000);
            if (!isunlimited) {
                ac.setTrafficLeft(Math.min(inC, outC));
            } else {
                ac.setUnlimitedTraffic();
            }
            synchronized (LOCK) {
                premiumHosts.clear();
                if (hosts != null) {
                    String hoster[] = new Regex(hosts, "(.+?)($|\\|)").getColumn(0);
                    for (String host : hoster) {
                        if (hosts != null) {
                            premiumHosts.add(host.trim());
                        }
                    }
                }
            }
            if (account.isValid()) {
                if (premiumHosts.size() == 0) {
                    ac.setStatus(restartReq + "Account valid: 0 Hosts via streammania.com available");
                } else {
                    ac.setStatus(restartReq + "Account valid: " + premiumHosts.size() + " Hosts via streammania.com available");
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
        link.setProperty("genLinkStreammania", null);
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
        if (proxyused) return "streammania.com";
        if (plugin != null) return plugin.getCustomFavIconURL();
        return null;
    }

}
