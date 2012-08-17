package org.jdownloader.extensions.jdpremclient;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
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

public class FastDebridcom extends PluginForHost implements JDPremInterface {

    private boolean                        proxyused    = false;
    private String                         infostring   = null;
    private PluginForHost                  plugin       = null;
    private static boolean                 enabled      = false;
    private static java.util.List<String>       premiumHosts = new ArrayList<String>();
    private static final Object            LOCK         = new Object();
    private static HashMap<String, String> accDetails   = new HashMap<String, String>();

    public FastDebridcom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.fast-debrid.com/");
        infostring = "Fast-Debrid.com @ " + wrapper.getLazy().getDisplayName();
    }

    @Override
    public String getAGBLink() {
        if (plugin == null) return "https://www.fast-debrid.com/charte.php";
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
        if (plugin == null) return "fast-debrid.com";
        return plugin.getHost();
    }

    @Override
    public ConfigContainer getConfig() {
        if (plugin == null) return super.getConfig();
        return plugin.getConfig();
    }

    @Override
    public String getBuyPremiumUrl() {
        if (plugin == null) return "https://www.fast-debrid.com";
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

    private boolean handleFastDebrid(DownloadLink link) throws Exception {
        Account acc = null;
        synchronized (LOCK) {
            /* jdpremium enabled */

            /* premium available for this host */
            if (!premiumHosts.contains(link.getHost())) return false;
            // acc =
            // AccountController.getInstance().getValidAccount("fast-debrid.com");
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
            String genlink = link.getStringProperty("genLinkFastDebrid", null);
            /* remove generated link */
            link.setProperty("genLinkFastDebrid", null);
            if (genlink != null) {
                /* try saved link first */
                try {
                    fastLogin(acc);
                    dl = jd.plugins.BrowserAdapter.openDownload(br, link, genlink, true, 0);
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
                String url = Encoding.urlEncode(link.getDownloadURL());
                fastLogin(acc);
                /*
                 * Removing old method because a lot of the link this API returns are corrupted; - the API isn't updated by fast-debrid, but we can count on the
                 * new method to always be updated
                 * 
                 * genlink = br.getPage("https://www.fast-debrid.com/tool.php?pseudo=" + user + "&password=" + pw + "&view=1&viewlink=1" + "&link=" + url);
                 */

                /*
                 * Getting the link via a new method, that is better because: it returns valid links for all hosts; mimics the way their firefox plugin
                 * downloads, and therefore we can count on this API to always be up to date links gotten by this method allow for more connections to be made
                 */

                br.getPage("https://www.fast-debrid.com/plugin.php?link=" + url);
                genlink = br.getRegex("iframe src.*?\"(http.*?hostp.*?)\"").getMatch(0);
                // Getting just the link from HTML

                if (genlink == null || !genlink.startsWith("http://")) {
                    logger.severe("FastDebrid(Error): " + genlink);
                    if (genlink.contains("_limit")) {
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
                        /* reset retry counter */
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
                if (!dl.getConnection().isContentDisposition()) {
                    /* unknown error */
                    br.followConnection();
                    logger.severe("FastDebrid(Error): " + br.toString());
                    synchronized (LOCK) {
                        premiumHosts.remove(link.getHost());
                    }
                    return false;
                }
            }
            /* save generated link */
            link.setProperty("genLinkFastDebrid", genlink);
            dl.startDownload();
        } finally {
            br.setFollowRedirects(dofollow);
        }
        return true;
    }

    private void fastLogin(Account acc) throws IOException {
        synchronized (acc) {
            Object cookies = acc.getProperty("cookies");
            if (cookies != null && cookies instanceof HashMap && !((Map<String, String>) cookies).isEmpty()) {
                for (final Map.Entry<String, String> cookieEntry : ((Map<String, String>) cookies).entrySet()) {
                    final String key = cookieEntry.getKey();
                    final String value = cookieEntry.getValue();
                    br.setCookie("fast-debrid.com", key, value);
                }
                return;
            }
            acc.setProperty("cookies", null);
            String user = Encoding.urlEncode(acc.getUser());
            String pw = Encoding.urlEncode(acc.getPass());
            String postData = "pseudo=" + user + "&password=" + pw;
            br.postPageRaw("https://www.fast-debrid.com/index.php", postData);
            HashMap<String, String> cookies2 = new HashMap<String, String>();
            final Cookies cYT = br.getCookies("fast-debrid.com");
            for (final Cookie c : cYT.getCookies()) {
                cookies2.put(c.getKey(), c.getValue());
            }
            acc.setProperty("cookies", cookies2);
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
            br.setConnectTimeout(60 * 1000);
            br.setReadTimeout(60 * 1000);
            br.setDebug(true);
            String username = Encoding.urlEncode(account.getUser());
            String pass = Encoding.urlEncode(account.getPass());
            String page = null;
            String hosts = null;
            synchronized (account) {
                try {
                    page = br.getPage("https://www.fast-debrid.com/api_account.php?login=" + username + "&pw=" + pass);
                    hosts = br.getPage("https://www.fast-debrid.com/listhost.php");
                } catch (Exception e) {
                    account.setTempDisabled(true);
                    account.setValid(true);
                    account.setProperty("cookies", null);
                    resetAvailablePremium();
                    ac.setStatus("FastDebrid Server Error, temp disabled" + restartReq);
                    return ac;
                }
            }
            /* parse api response in easy2handle hashmap */
            String info[][] = new Regex(page, "<([^<>]*?)>([^<]*?)</.*?>").getMatches();
            synchronized (accDetails) {
                accDetails.clear();
                for (String data[] : info) {
                    accDetails.put(data[0].toLowerCase(Locale.ENGLISH), data[1].toLowerCase(Locale.ENGLISH));
                }
                String type = accDetails.get("type");
                if ("platinium".equals(type) || "premium".equals(type)) {
                    /* only platinum and premium support */
                    synchronized (LOCK) {
                        premiumHosts.clear();
                        if (hosts != null) {
                            String hoster[] = new Regex(hosts, "\"(.*?)\"").getColumn(0);
                            if (hosts != null) {
                                for (String host : hoster) {
                                    if (hosts == null || host.length() == 0) continue;
                                    host = host.trim();
                                    try {
                                        if (host.equals("rapidshare.com") && accDetails.get("limite_rs") != null && Integer.parseInt(accDetails.get("limite_rs")) == 0) continue;
                                    } catch (final Throwable e) {
                                        logger.severe(e.toString());
                                    }
                                    try {
                                        if (host.equals("depositfiles.com") && accDetails.get("limite_dp") != null && Integer.parseInt(accDetails.get("limite_dp")) == 0) continue;
                                    } catch (final Throwable e) {
                                        logger.severe(e.toString());
                                    }
                                    premiumHosts.add(host.trim());
                                }
                            }
                        }
                    }
                    String daysLeft = accDetails.get("date");
                    if (daysLeft != null) {
                        account.setValid(true);
                        ac.setValidUntil(System.currentTimeMillis() + (Long.parseLong(daysLeft) * 1000 * 60 * 60 * 24));
                    } else {
                        /* no days left available?! */
                        account.setValid(false);
                    }
                } else {
                    /* all others are invalid */
                    account.setValid(false);
                }
                if (account.isValid()) {
                    if (premiumHosts.size() == 0) {
                        ac.setStatus(restartReq + "Account valid: 0 Hosts via Fast-Debrid.com available");
                    } else {
                        ac.setStatus(restartReq + "Account valid: " + premiumHosts.size() + " Hosts via Fast-Debrid.com available");
                    }
                } else {
                    account.setTempDisabled(false);
                    resetAvailablePremium();
                    ac.setStatus("Account invalid");
                }
            }
            return ac;
        } else
            return plugin.fetchAccountInfo(account);
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        link.setProperty("genLinkFastDebrid", null);
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
        if (proxyused) return "fast-debrid.com";
        if (plugin != null) return plugin.getCustomFavIconURL();
        return null;
    }

}
