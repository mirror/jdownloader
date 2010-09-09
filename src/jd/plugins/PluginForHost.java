//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins;

import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import jd.HostPluginWrapper;
import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.CaptchaController;
import jd.controlling.DownloadController;
import jd.controlling.FavIconController;
import jd.controlling.FavIconRequestor;
import jd.controlling.JDLogger;
import jd.controlling.SingleDownloadController;
import jd.gui.UserIF;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.ToolBarAction.Types;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.http.Browser;
import jd.nutils.Formatter;
import jd.nutils.JDImage;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.download.DownloadInterface;
import jd.utils.locale.JDL;

/**
 * Dies ist die Oberklasse fuer alle Plugins, die von einem Anbieter Dateien
 * herunterladen koennen
 * 
 * @author astaldo
 */
public abstract class PluginForHost extends Plugin implements FavIconRequestor {

    private static final String JDL_PREFIX = "jd.plugins.PluginsForHost.";

    public PluginForHost(final PluginWrapper wrapper) {
        super(wrapper);
        /* defaultPlugin does not need any Browser instance */
        br = null;
        dl = null;
    }

    public void setBrowser(Browser br) {
        this.br = br;
    }

    protected String getCaptchaCode(final String captchaAddress, final DownloadLink downloadLink) throws IOException, PluginException {
        return getCaptchaCode(getHost(), captchaAddress, downloadLink);
    }

    @Override
    public long getVersion() {
        return wrapper.getVersion();
    }

    protected String getCaptchaCode(final String method, final String captchaAddress, final DownloadLink downloadLink) throws IOException, PluginException {
        if (captchaAddress == null) {
            logger.severe("Captcha Adresse nicht definiert");
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        final File captchaFile = getLocalCaptchaFile();
        try {
            Browser.download(captchaFile, br.cloneBrowser().openGetConnection(captchaAddress));
        } catch (Exception e) {
            logger.severe("Captcha Download fehlgeschlagen: " + captchaAddress);
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        final String captchaCode = getCaptchaCode(method, captchaFile, downloadLink);
        return captchaCode;
    }

    protected String getCaptchaCode(final File captchaFile, final DownloadLink downloadLink) throws PluginException {
        return getCaptchaCode(getHost(), captchaFile, downloadLink);
    }

    protected String getCaptchaCode(final String methodname, final File captchaFile, final DownloadLink downloadLink) throws PluginException {
        return getCaptchaCode(methodname, captchaFile, 0, downloadLink, null, null);
    }

    protected String getCaptchaCode(final String method, final File file, final int flag, final DownloadLink link, final String defaultValue, final String explain) throws PluginException {
        final LinkStatus linkStatus = link.getLinkStatus();
        final String status = linkStatus.getStatusText();
        final DownloadController downloadController = DownloadController.getInstance();
        try {
            linkStatus.addStatus(LinkStatus.WAITING_USERIO);
            linkStatus.setStatusText(JDL.L("gui.downloadview.statustext.jac", "Captcha recognition"));
            try {
                final BufferedImage img = ImageIO.read(file);
                linkStatus.setStatusIcon(JDImage.getScaledImageIcon(img, 16, 16));
            } catch (Exception e) {
                e.printStackTrace();
            }
            downloadController.fireDownloadLinkUpdate(link);

            final String cc = new CaptchaController(getHost(), getHosterIconUnscaled(), method, file, defaultValue, explain).getCode(flag);
            if (cc == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            return cc;
        } finally {
            linkStatus.removeStatus(LinkStatus.WAITING_USERIO);
            linkStatus.setStatusText(status);
            linkStatus.setStatusIcon(null);
            downloadController.fireDownloadLinkUpdate(link);
        }
    }

    private static final String                AGB_CHECKED            = "AGB_CHECKED";
    private static int                         currentConnections     = 0;

    public static final String                 PARAM_MAX_RETRIES      = "MAX_RETRIES";
    protected DownloadInterface                dl                     = null;
    private int                                maxConnections         = 50;

    private static final HashMap<String, Long> LAST_CONNECTION_TIME   = new HashMap<String, Long>();
    private static final HashMap<String, Long> LAST_STARTED_TIME      = new HashMap<String, Long>();

    private Long                               WAIT_BETWEEN_STARTS    = 0L;

    private boolean                            enablePremium          = false;

    private boolean                            accountWithoutUsername = false;

    private String                             premiumurl             = null;

    protected ImageIcon                        hosterIcon             = null;
    protected boolean                          hosterIconRequested    = false;
    private DownloadLink                       link                   = null;

    public boolean checkLinks(final DownloadLink[] urls) {
        return false;
    }

    @Override
    public HostPluginWrapper getWrapper() {
        return (HostPluginWrapper) super.getWrapper();
    }

    @Override
    public void clean() {
        dl = null;
        super.clean();
    }

    protected void setBrowserExclusive() {
        if (br == null) return;
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        final int eID = e.getID();
        if (eID == 1) {
            UserIF.getInstance().requestPanel(UserIF.Panels.CONFIGPANEL, config);
            return;
        }
        if (eID == 2) {
            UserIF.getInstance().requestPanel(UserIF.Panels.PREMIUMCONFIG, null);
            ActionController.getToolBarAction("action.premiumview.addacc").actionPerformed(new ActionEvent(this, 0, "addaccount"));
            return;
        }
        if (eID == 3) {
            UserIF.getInstance().requestPanel(UserIF.Panels.PREMIUMCONFIG, null);
            try {
                JLink.openURL(getBuyPremiumUrl());
            } catch (Exception ex) {
            }
            return;
        }
        final ArrayList<Account> accounts = getPremiumAccounts();
        if (eID >= 200) {
            final int accountID = eID - 200;
            final Account account = accounts.get(accountID);
            UserIF.getInstance().requestPanel(UserIF.Panels.PREMIUMCONFIG, account);

        } else if (eID >= 100) {
            final int accountID = eID - 100;
            Account account = accounts.get(accountID);
            account.setEnabled(!account.isEnabled());
        }
    }

    /** default fetchAccountInfo, set account valid to true */
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        account.setValid(true);
        return null;
    }

    public boolean getAccountwithoutUsername() {
        return accountWithoutUsername;
    }

    public void setAccountwithoutUsername(boolean b) {
        accountWithoutUsername = b;
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        if (!enablePremium) return null;

        final ArrayList<MenuAction> menuList = new ArrayList<MenuAction>();
        MenuAction account;
        MenuAction m;

        if (config != null && config.getEntries().size() > 0) {
            m = new MenuAction("plugins.configs", 1);
            m.setActionListener(this);
            menuList.add(m);
            menuList.add(new MenuAction(Types.SEPARATOR));
        }

        MenuAction premiumAction = new MenuAction("accounts", 0);
        premiumAction.setType(Types.CONTAINER);
        ArrayList<Account> accounts = getPremiumAccounts();

        int i = 1;
        int c = 0;
        for (final Account a : accounts) {
            if (a != null) {
                try {
                    c++;
                    if (getAccountwithoutUsername()) {
                        if (a.getPass() == null || a.getPass().trim().length() == 0) continue;
                        account = new MenuAction();
                        account.setTitle(i++ + ". " + JDL.L(JDL_PREFIX + "account", "Account"));
                        account.setType(Types.CONTAINER);
                    } else {
                        if (a.getUser() == null || a.getUser().trim().length() == 0) continue;
                        account = new MenuAction();
                        account.setTitle(i++ + ". " + a.getUser());
                        account.setType(Types.CONTAINER);
                    }
                    m = AccountMenuItemSyncer.getInstance().get(a);

                    if (m == null) {
                        m = new MenuAction("plugins.PluginForHost.enable_premium", 100 + c - 1);
                    }
                    m.setActionID(100 + c - 1);
                    m.setSelected(a.isEnabled());
                    m.setActionListener(this);
                    account.addMenuItem(m);

                    AccountMenuItemSyncer.getInstance().map(a, m);

                    m = new MenuAction("plugins.PluginForHost.premiumInfo", 200 + c - 1);
                    m.setActionListener(this);
                    account.addMenuItem(m);
                    premiumAction.addMenuItem(account);

                } catch (Exception e) {
                    JDLogger.exception(e);
                }
            }
        }

        if (premiumAction.getSize() != 0) {
            menuList.add(premiumAction);
        } else {
            menuList.add(m = new MenuAction("plugins.menu.noaccounts", 2));
            m.setActionListener(this);
        }
        menuList.add(m = new MenuAction("plugins.menu.buyaccount", 3));
        m.setActionListener(this);

        return menuList;
    }

    public abstract String getAGBLink();

    protected void enablePremium() {
        enablePremium(null);
    }

    protected void enablePremium(final String url) {
        premiumurl = url;
        enablePremium = true;
    }

    public static synchronized int getCurrentConnections() {
        return currentConnections;
    }

    /**
     * Hier werden Treffer fuer Downloadlinks dieses Anbieters in diesem Text
     * gesucht. Gefundene Links werden dann in einem ArrayList zurueckgeliefert
     * 
     * @param data
     *            Ein Text mit beliebig vielen Downloadlinks dieses Anbieters
     * @return Ein ArrayList mit den gefundenen Downloadlinks
     */
    public ArrayList<DownloadLink> getDownloadLinks(final String data, final FilePackage fp) {
        ArrayList<DownloadLink> links = null;

        final String[] hits = new Regex(data, getSupportedLinks()).getColumn(-1);
        if (hits != null && hits.length > 0) {
            links = new ArrayList<DownloadLink>();
            for (String file : hits) {
                while (file.charAt(0) == '"') {
                    file = file.substring(1);
                }
                while (file.charAt(file.length() - 1) == '"') {
                    file = file.substring(0, file.length() - 1);
                }

                try {
                    final DownloadLink link = new DownloadLink((PluginForHost) wrapper.getPlugin(), file.substring(file.lastIndexOf("/") + 1, file.length()), getHost(), file, true);
                    links.add(link);
                    if (fp != null) {
                        link.setFilePackage(fp);
                    }
                } catch (IllegalArgumentException e) {
                    JDLogger.exception(e);
                } catch (SecurityException e) {
                    JDLogger.exception(e);
                }
            }
        }
        return links;
    }

    /** ueberschreiben falls die downloadurl erst rekonstruiert werden muss */
    public void correctDownloadLink(final DownloadLink link) throws Exception {
    }

    /**
     * Holt Informationen zu einem Link. z.B. dateigroeße, Dateiname,
     * verfuegbarkeit etc.
     * 
     * @param parameter
     * @return true/false je nach dem ob die Datei noch online ist (verfuegbar)
     * @throws IOException
     */
    public abstract AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception;

    /**
     * Gibt einen String mit den Dateiinformationen zurueck. Die Defaultfunktion
     * gibt nur den dateinamen zurueck. Allerdings Sollte diese Funktion
     * ueberschrieben werden. So kann ein Plugin zusatzinfos zu seinen Links
     * anzeigen (Nach dem aufruf von getFileInformation()
     * 
     * @param downloadLink
     * @return
     */
    public String getFileInformationString(final DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + Formatter.formatReadable(downloadLink.getDownloadSize()) + ")";
    }

    public synchronized int getFreeConnections() {
        return Math.max(1, getMaxConnections() - currentConnections);
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public int getMaxRetries() {
        return SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(PARAM_MAX_RETRIES, 3);
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    public int getMaxSimultanDownload(final Account account) {
        int max;
        if (account == null) {
            max = getMaxSimultanFreeDownloadNum();
        } else {
            max = account.getMaxSimultanDownloads();
            if (max < 0) {
                return Integer.MAX_VALUE;
            } else if (max == 0) {
                max = getMaxSimultanPremiumDownloadNum();
            } else {
                return max;
            }
        }
        if (max <= 0) return Integer.MAX_VALUE;
        if (max == Integer.MIN_VALUE) return 0;
        return max;
    }

    /* TODO: remove with next major update */
    @Deprecated
    public boolean isPremiumDownload() {
        return true;
    }

    public synchronized long getLastTimeStarted() {
        if (!LAST_STARTED_TIME.containsKey(getHost())) { return 0; }
        return Math.max(0, (LAST_STARTED_TIME.get(getHost())));
    }

    public synchronized void putLastTimeStarted(long time) {
        LAST_STARTED_TIME.put(getHost(), time);
    }

    public synchronized long getLastConnectionTime() {
        if (!LAST_CONNECTION_TIME.containsKey(getHost())) { return 0; }
        return Math.max(0, (LAST_CONNECTION_TIME.get(getHost())));
    }

    public synchronized void putLastConnectionTime(long time) {
        LAST_CONNECTION_TIME.put(getHost(), time);
    }

    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        final LinkStatus linkStatus = link.getLinkStatus();
        linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFECT);
        linkStatus.setErrorMessage(JDL.L("plugins.hoster.nopremiumsupport", "Plugin has no handlePremium Method!"));
    }

    public abstract void handleFree(DownloadLink link) throws Exception;

    public void handle(final DownloadLink downloadLink, final Account account) throws Exception {
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

        if (account != null) {
            /* with account */
            final long before = downloadLink.getDownloadCurrent();
            boolean blockAccount = false;
            try {
                transferStatus.usePremium(true);
                handlePremium(downloadLink, account);
                if (dl != null && dl.getConnection() != null) {
                    try {
                        dl.getConnection().disconnect();
                    } catch (Exception e) {
                    }
                }
            } catch (PluginException e) {
                e.fillLinkStatus(downloadLink.getLinkStatus());
                if (e.getLinkStatus() == LinkStatus.ERROR_PLUGIN_DEFECT) logger.info(JDLogger.getStackTrace(e));
                logger.info(downloadLink.getLinkStatus().getLongErrorMessage());
                if (downloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_IP_BLOCKED) || downloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE)) {
                    blockAccount = true;
                }
            }

            final long traffic = Math.max(0, downloadLink.getDownloadCurrent() - before);
            boolean throwupdate = false;
            final AccountInfo accountInfo = account.getAccountInfo();
            synchronized (AccountController.ACCOUNT_LOCK) {
                final AccountInfo ai = accountInfo;
                /* check traffic of account (eg traffic limit reached) */
                if (traffic > 0 && ai != null && !ai.isUnlimitedTraffic()) {
                    long left = Math.max(0, ai.getTrafficLeft() - traffic);
                    ai.setTrafficLeft(left);
                    if (left == 0 && ai.isSpecialTraffic()) {
                        logger.severe("Premium Account " + account.getUser() + ": Traffic Limit could be reached, but SpecialTraffic might be available!");
                    } else if (left == 0) {
                        logger.severe("Premium Account " + account.getUser() + ": Traffic Limit reached");
                        account.setTempDisabled(true);
                    }
                    throwupdate = true;
                }
                /* check blocked account(eg free user accounts with waittime) */
                if (blockAccount) {
                    logger.severe("Account: " + account.getUser() + " is blocked, temp. disabling it!");
                    AccountController.getInstance().addAccountBlocked(account);
                }
            }
            if (throwupdate) {
                AccountController.getInstance().throwUpdateEvent(this, account);
            }
            if (downloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_PREMIUM)) {
                if (downloadLink.getLinkStatus().getValue() == PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE) {
                    logger.severe("Premium Account " + account.getUser() + ": Traffic Limit reached");
                    account.setTempDisabled(true);
                    account.getAccountInfo().setTrafficLeft(0);
                    if (accountInfo != null) {
                        accountInfo.setStatus(downloadLink.getLinkStatus().getErrorMessage());
                    }
                } else if (downloadLink.getLinkStatus().getValue() == PluginException.VALUE_ID_PREMIUM_DISABLE) {
                    account.setEnabled(false);
                    if (accountInfo != null) accountInfo.setStatus(downloadLink.getLinkStatus().getErrorMessage());
                    logger.severe("Premium Account " + account.getUser() + ": expired:" + downloadLink.getLinkStatus().getLongErrorMessage());
                } else {
                    account.setEnabled(false);
                    if (accountInfo != null) accountInfo.setStatus(downloadLink.getLinkStatus().getErrorMessage());
                    logger.severe("Premium Account " + account.getUser() + ":" + downloadLink.getLinkStatus().getLongErrorMessage());
                }
            } else {
                if (accountInfo != null) {
                    accountInfo.setStatus(JDL.L("plugins.hoster.premium.status_ok", "Account is ok"));
                }
            }
        } else {
            /* without account */
            try {
                handleFree(downloadLink);
                if (dl != null && dl.getConnection() != null) {
                    try {
                        dl.getConnection().disconnect();
                    } catch (Exception e) {
                    }
                }
            } catch (PluginException e) {
                e.fillLinkStatus(downloadLink.getLinkStatus());
                if (e.getLinkStatus() == LinkStatus.ERROR_PLUGIN_DEFECT) logger.info(JDLogger.getStackTrace(e));
                logger.info(downloadLink.getLinkStatus().getLongErrorMessage());
            }
        }
        return;
    }

    public boolean isAGBChecked() {
        return getPluginConfig().getBooleanProperty(AGB_CHECKED, false);
    }

    /**
     * Stellt das Plugin in den Ausgangszustand zurueck (variablen intialisieren
     * etc)
     */
    public abstract void reset();

    public abstract void resetDownloadlink(DownloadLink link);

    public void resetPluginGlobals() {
    }

    public static synchronized void setCurrentConnections(final int CurrentConnections) {
        currentConnections = CurrentConnections;
    }

    public int getTimegapBetweenConnections() {
        return 50;
    }

    public void setStartIntervall(long interval) {
        WAIT_BETWEEN_STARTS = interval;
    }

    public boolean waitForNextStartAllowed(final DownloadLink downloadLink) throws InterruptedException {
        final long time = Math.max(0, WAIT_BETWEEN_STARTS - (System.currentTimeMillis() - getLastTimeStarted()));
        if (time > 0) {
            try {
                sleep(time, downloadLink);
            } catch (PluginException e) {
                throw new InterruptedException();
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean waitForNextConnectionAllowed() throws InterruptedException {
        final long time = Math.max(0, getTimegapBetweenConnections() - (System.currentTimeMillis() - getLastConnectionTime()));
        if (time > 0) {
            Thread.sleep(time);
            return true;
        } else {
            return false;
        }
    }

    public void setMaxConnections(final int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public void sleep(final long i, final DownloadLink downloadLink) throws PluginException {
        sleep(i, downloadLink, "");
    }

    public void sleep(long i, DownloadLink downloadLink, String message) throws PluginException {
        SingleDownloadController dlc = downloadLink.getDownloadLinkController();
        try {
            while (i > 0 && dlc != null && !dlc.isAborted()) {
                i -= 1000;
                downloadLink.getLinkStatus().setStatusText(message + JDL.LF("gui.download.waittime_status2", "Wait %s", Formatter.formatSeconds(i / 1000)));
                downloadLink.requestGuiUpdate();
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            throw new PluginException(LinkStatus.TODO);
        }
        downloadLink.getLinkStatus().setStatusText(null);
    }

    /**
     * may only be used from within the plugin, because it only works when
     * SingleDownloadController is still running
     */
    protected boolean isAborted(DownloadLink downloadLink) {
        SingleDownloadController dlc = downloadLink.getDownloadLinkController();
        return (dlc != null && dlc.isAborted());
    }

    public Browser getBrowser() {
        return br;
    }

    public void setDownloadInterface(DownloadInterface dl2) {
        this.dl = dl2;
    }

    /**
     * Gibt die Url zurueck, unter welcher ein PremiumAccount gekauft werden
     * kann
     * 
     * @return
     */
    public String getBuyPremiumUrl() {
        if (premiumurl != null) return "http://jdownloader.org/r.php?u=" + Encoding.urlEncode(premiumurl);
        return premiumurl;
    }

    public boolean isPremiumEnabled() {
        return enablePremium;
    }

    public ArrayList<Account> getPremiumAccounts() {
        return AccountController.getInstance().getAllAccounts(this);
    }

    /**
     * returns hosterspecific infos. for example the downloadserver
     * 
     * @return
     */
    public String getSessionInfo() {
        return getHost();
    }

    public ImageIcon getHosterIconUnscaled() {
        if (hosterIcon == null && getCustomFavIconURL() != null && !hosterIconRequested) {
            hosterIcon = FavIconController.getFavIcon(getCustomFavIconURL(), this, true);
            hosterIconRequested = true;
        }
        if (hosterIcon != null) return hosterIcon;
        return getWrapper().getIconUnscaled();
    }

    public final ImageIcon getHosterIconScaled() {
        if (hosterIcon == null && getCustomFavIconURL() != null && !hosterIconRequested) {
            hosterIcon = FavIconController.getFavIcon(getCustomFavIconURL(), this, true);
            hosterIconRequested = true;
        }
        if (hosterIcon != null) return JDImage.getScaledImageIcon(getHosterIconUnscaled(), 16, -1);
        return getWrapper().getIconScaled();
    }

    /* override this to use customized favicons */
    public String getCustomFavIconURL() {
        return null;
    }

    /* reset customized favicon */
    public void resetFavIcon() {
        hosterIconRequested = false;
        hosterIcon = null;
    }

    public void setFavIcon(ImageIcon icon) {
        this.hosterIcon = icon;
    }

    public void setDownloadLink(DownloadLink link) {
        this.link = link;
    }

    public DownloadLink getDownloadLink() {
        return link;
    }

    public void setAGBChecked(boolean b) {
        ((HostPluginWrapper) wrapper).setAGBChecked(b);
    }

    /* override this if you want to change a link to use this plugin */
    /* dont forget to change host with setHost */
    /* must return true if changing was successfull */
    /* if this function needs a browser, it must create an instance on its own */
    public boolean rewriteHost(DownloadLink link) {
        return false;
    }

}
