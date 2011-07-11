package org.jdownloader.extensions.jdpremclient;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.ImageIcon;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.controlling.AccountController;
import jd.controlling.JDPluginLogger;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.http.Browser;
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

import org.appwork.utils.Hash;
import org.appwork.utils.formatter.TimeFormatter;

public class PremGet extends PluginForHost implements JDPremInterface {

    private boolean                  proxyused    = false;
    private String                   infostring   = null;
    private PluginForHost            plugin       = null;
    private static boolean           enabled      = false;
    private static ArrayList<String> premiumHosts = new ArrayList<String>();
    private static final Object      LOCK         = new Object();
    private String                   Info         = null;
    private String                   validUntil   = null;
    private boolean                  expired      = false;

    /* function returns transfer left */
    private long GetTransferLeft(String wynik) {
        String[] temp = wynik.split(" ");
        String[] tab = temp[0].split("=");
        long rozmiar = Long.parseLong(tab[1]);
        rozmiar *= 1024;
        rozmiar *= 1024;
        return rozmiar;

    }

    public PremGet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.premget.pl/packages.php");
        infostring = "PremGet.pl @ " + wrapper.getHost();
    }

    @Override
    public String getAGBLink() {
        if (plugin == null) return "http://www.premget.pl/tos.php";
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
        if (plugin == null) return getVersion("$Revision: 00001 $");
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
        if (plugin == null) return "premget.pl";
        return plugin.getHost();
    }

    @Override
    public ConfigContainer getConfig() {
        if (plugin == null) return super.getConfig();
        return plugin.getConfig();
    }

    @Override
    public String getBuyPremiumUrl() {
        if (plugin == null) return "http://www.premget.pl/packages.php";
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
        /* try premget.pl first */
        if (account == null) {
            if (handlePremGet(downloadLink)) return;
        } else if (!PremiumCompoundExtension.preferLocalAccounts()) {
            if (handlePremGet(downloadLink)) return;
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

    private boolean handlePremGet(DownloadLink link) throws Exception {
        Account acc = null;
        synchronized (LOCK) {
            /* jdpremium enabled */
            if (!PremiumCompoundExtension.isStaticEnabled() || !enabled) return false;
            /* premium available for this host */
            if (!premiumHosts.contains(link.getHost())) return false;
            acc = AccountController.getInstance().getValidAccount("premget.pl");
            /* enabled account found? */
            if (acc == null || !acc.isEnabled()) return false;
        }
        if (expired) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Konto wygasło"); }
        proxyused = true;
        requestFileInformation(link);
        if (link.isAvailabilityStatusChecked() && !link.isAvailable()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        resetFavIcon();
        br.setConnectTimeout(90 * 1000);
        br.setReadTimeout(90 * 1000);
        br.setDebug(true);
        dl = null;
        String response = null;
        login(acc, false);
        /* generate new downloadlink */

        String postData = "username=" + acc.getUser() + "&password=" + Hash.getSHA1(Hash.getMD5(acc.getPass())) + "&info=0&url=" + link.getDownloadURL() + "&site=premget";

        response = br.postPage("http://crypt.premget.pl", postData);

        String genlink = response;

        br.setFollowRedirects(true);

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, genlink, true, 1);
        link.getTransferStatus().usePremium(true);
        /*
         * I realy wanted to use Content Disposition below, but it just don't
         * work for resume at hotfile
         */
        if (dl.getConnection().getContentType().equalsIgnoreCase("text/html")) // unknown
        // error
        {
            br.followConnection();
            if (br.containsHTML("Brak")) {
                /* No transfer left */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Brak transferu!");

            }
            if (br.containsHTML("Nieprawidlowa")) {
                /* Wrong username/password */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Błędny Login!");
            }
            if (br.containsHTML("Niepoprawny")) {
                /* File not found */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Nie znaleziono pliku!");
            }
            if (br.containsHTML("Konto")) {
                /* Account Expired */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Konto Wygasło!");
            }
        }

        if (dl.getConnection().getResponseCode() == 404) {
            /* file offline */
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

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

    private void login(Account account, boolean force) throws PluginException, IOException {
        synchronized (LOCK) {
            this.setBrowserExclusive();
            this.br.setDebug(true);
        }
        try {

            br.postPage("http://crypt.premget.pl", "username=" + account.getUser() + "&password=" + Hash.getSHA1(Hash.getMD5(account.getPass())) + "&info=1&site=premget");
            String adres = br.toString();
            br.getPage(adres);
            adres = br.getRedirectLocation();
            br.getPage(adres);

            if (this.br.containsHTML("balance")) {
                Info = br.toString();

            }
            if (this.br.containsHTML("expire")) {
                char temp = Info.charAt(Info.length() - 11);
                validUntil = Info.substring(Info.length() - 10);

                if (temp == '1') {
                    expired = false;
                } else {
                    expired = true;
                }

            } else {
                expired = false;
            }

        } catch (final Exception e) {
        }
        boolean invalid = false;
        if (this.br.containsHTML("Nieprawidlowa")) {
            invalid = true;
        }

        if (invalid) {

            if (invalid) { throw new PluginException(LinkStatus.ERROR_PREMIUM,

            PluginException.VALUE_ID_PREMIUM_DISABLE); }
            AccountInfo ai = account.getAccountInfo();
            if (ai == null) {
                ai = new AccountInfo();
                account.setAccountInfo(ai);
            }
            ai.setStatus("ServerProblems(1), will try again in few minutes!");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
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
            ac.setSpecialTraffic(true);
            String hosts = null;

            try {
                hosts = br.getPage("http://www.nopremium.pl/clipboard.php");
                login(account, true);

            } catch (Exception e) {
                account.setTempDisabled(true);
                account.setValid(true);
                resetAvailablePremium();
                synchronized (LOCK) {
                    premiumHosts.clear();
                }
                ac.setStatus("Nieprawidłowe dane lub brak odpowiedzi serwera" + restartReq);
                ac.setExpired(true);
                return ac;
            }

            if (Info != null) ac.setTrafficLeft(GetTransferLeft(Info));

            synchronized (LOCK) {
                premiumHosts.clear();
                if (hosts != null) {
                    String hoster[] = new Regex(hosts, "(.*?)(<br />|$)").getColumn(0);
                    if (hosts != null) {
                        for (String host : hoster) {
                            if (hosts == null || host.length() == 0) continue;
                            premiumHosts.add(host.trim());
                        }
                    }
                }
            }
            if (expired) {
                ac.setExpired(true);
                ac.setStatus("Konto Nieaktywne");
                ac.setValidUntil(0);
                return ac;
            } else {
                ac.setExpired(false);
                if (validUntil != null) {
                    ac.setValidUntil(TimeFormatter.getMilliSeconds(validUntil));

                }

            }
            if (premiumHosts.size() == 0) {
                ac.setStatus(restartReq + "Zalogowano poprawnie, jednak nie pobrano listy obsługiwanych serwisów");
            } else {
                ac.setStatus(restartReq + "Zalogowano poprawnie. " + premiumHosts.size() + " obsługiwanych serwisów");

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

                return Integer.MAX_VALUE;
            } else if (PremiumCompoundExtension.isStaticEnabled() && enabled) {
                /* OchLoad */
                synchronized (LOCK) {
                    if (premiumHosts.contains(plugin.getHost()) && AccountController.getInstance().getValidAccount

                    ("PremGet.pl") != null) return Integer.MAX_VALUE;
                }
            }

            return Integer.MAX_VALUE;
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
        if (proxyused) return "PremGet.pl";
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
