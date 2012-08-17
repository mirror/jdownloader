package org.jdownloader.extensions.jdpremclient;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

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

public class FilePremiercom extends PluginForHost implements JDPremInterface {

    private boolean                  proxyused    = false;
    private String                   infostring   = null;
    private PluginForHost            plugin       = null;
    private static boolean           enabled      = false;
    private static java.util.List<String> premiumHosts = new ArrayList<String>();
    private static final Object      LOCK         = new Object();

    public FilePremiercom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://filepremier.com/");
        infostring = "Filepremier.com @ " + wrapper.getLazy().getDisplayName();
    }

    @Override
    public String getAGBLink() {
        if (plugin == null) return "http://filepremier.com/termini.php";
        return plugin.getAGBLink();
    }

    @Override
    public long getVersion() {
        if (plugin == null) return Formatter.getRevision("$Revision: 13980 $");
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
        if (plugin == null) return "filepremier.com";
        return plugin.getHost();
    }

    @Override
    public ConfigContainer getConfig() {
        if (plugin == null) return super.getConfig();
        return plugin.getConfig();
    }

    @Override
    public String getBuyPremiumUrl() {
        if (plugin == null) return "http://filepremier.com/premium.php";
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
        /* try filepremier.com first */

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

    private boolean handleFilePremier(DownloadLink link) throws Exception {
        Account acc = null;
        synchronized (LOCK) {
            /* jdpremium enabled */

            /* premium available for this host */
            if (!premiumHosts.contains(link.getHost())) return false;
            // acc =
            // AccountController.getInstance().getValidAccount("filepremier.com");
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
            String genlink = link.getStringProperty("genLinkFilePremier", null);
            /* remove generated link */
            link.setProperty("genLinkFilePremier", null);
            if (genlink != null) {
                /* try saved link first */
                try {
                    dl = jd.plugins.BrowserAdapter.openDownload(br, link, genlink, true, 0);
                    if (dl.getConnection().isContentDisposition() || "application/force-download".equals(dl.getConnection().getContentType())) {
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
                String user = Encoding.urlEncode(acc.getUser());
                String pw = Encoding.urlEncode(acc.getPass());
                String url = Encoding.urlEncode(link.getDownloadURL());
                genlink = br.getPage("http://www.filepremier.com/api.php?generate=&u=" + user + "&p=" + pw + "&link=" + url);
                if (!genlink.startsWith("http://")) {
                    logger.severe("FilePremier(Error): " + genlink);
                    if (genlink.contains("limit")) {
                        /* limit reached for this host */
                        synchronized (LOCK) {
                            premiumHosts.remove(link.getHost());
                        }
                        return false;
                    }
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
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, genlink, true, 0);
                if (dl.getConnection().getResponseCode() == 404) {
                    /* file offline */
                    dl.getConnection().disconnect();
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (!dl.getConnection().isContentDisposition() && !"application/force-download".equals(dl.getConnection().getContentType())) {
                    /* unknown error */
                    br.followConnection();
                    logger.severe("FilePremier(Error): " + br.toString());
                    synchronized (LOCK) {
                        premiumHosts.remove(link.getHost());
                    }
                    return false;
                }
            }
            /* save generated link */
            link.setProperty("genLinkFilepremier", genlink);
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
            HashMap<String, String> accDetails = new HashMap<String, String>();
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
                page = br.getPage("http://www.filepremier.com/api.php?check=&u=" + username + "&p=" + pass);
                hosts = br.getPage("http://www.filepremier.com/fpapi/hosts.php");
            } catch (Exception e) {
                account.setTempDisabled(true);
                account.setValid(true);
                resetAvailablePremium();
                ac.setStatus("FilePremier Server Error, temp disabled" + restartReq);
                return ac;
            }
            /* parse api response in easy2handle hashmap */
            String info[][] = new Regex(page, "<([^<>]*?)>([^<]*?)</.*?>").getMatches();

            for (String data[] : info) {
                accDetails.put(data[0].toLowerCase(Locale.ENGLISH), data[1].toLowerCase(Locale.ENGLISH));
            }
            String type = accDetails.get("status");
            if ("valid".equals(type)) {
                /* only premium support */
                synchronized (LOCK) {
                    premiumHosts.clear();
                    if (hosts != null) {
                        String hoster[] = new Regex(hosts, "\"(.*?)\"").getColumn(0);
                        if (hosts != null) {
                            for (String host : hoster) {
                                if (hosts == null || host.length() == 0) continue;
                                host = host.trim();

                                premiumHosts.add(host.trim());
                            }
                        }
                    }
                }
                String daysLeft = accDetails.get("expiration");
                if (daysLeft != null) {
                    account.setValid(true);
                    ac.setValidUntil((Long.parseLong(daysLeft)) * 1000);
                } else {
                    /* no daysleft available?! */
                    account.setValid(false);
                }
            } else {
                /* all others are invalid */
                account.setValid(false);
            }
            if (account.isValid()) {
                if (premiumHosts.size() == 0) {
                    ac.setStatus(restartReq + "Account valid: 0 Available Hoster via FilePremier");
                } else {
                    ac.setStatus(restartReq + "Account valid: " + premiumHosts.size() + " Available Hoster(s) via FilePremier");
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
        link.setProperty("genLinkFilePremier", null);
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
        if (proxyused) return "filepremier.com";
        if (plugin != null) return plugin.getCustomFavIconURL();
        return null;
    }

}