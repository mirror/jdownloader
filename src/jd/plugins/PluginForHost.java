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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.CaptchaController;
import jd.controlling.DownloadController;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.gui.skins.simple.SimpleGUI;
import jd.http.Browser;
import jd.nutils.Formatter;
import jd.nutils.JDImage;
import jd.parser.Regex;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.download.DownloadInterface;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Dies ist die Oberklasse für alle Plugins, die von einem Anbieter Dateien
 * herunterladen können
 * 
 * @author astaldo
 */
public abstract class PluginForHost extends Plugin {

    public PluginForHost(PluginWrapper wrapper) {
        super(wrapper);
    }

    protected String getCaptchaCode(String captchaAddress, DownloadLink downloadLink) throws IOException, PluginException {
        return getCaptchaCode(getHost(), captchaAddress, downloadLink);
    }

    protected String getCaptchaCode(String method, String captchaAddress, DownloadLink downloadLink) throws IOException, PluginException {
        if (captchaAddress == null) {
            logger.severe("Captcha Adresse nicht definiert");
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        File captchaFile = this.getLocalCaptchaFile();
        try {
            Browser.download(captchaFile, br.cloneBrowser().openGetConnection(captchaAddress));
        } catch (Exception e) {
            logger.severe("Captcha Download fehlgeschlagen: " + captchaAddress);
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String captchaCode = getCaptchaCode(method, captchaFile, downloadLink);
        return captchaCode;
    }

    protected String getCaptchaCode(File captchaFile, DownloadLink downloadLink) throws PluginException {
        return getCaptchaCode(getHost(), captchaFile, downloadLink);
    }

    protected String getCaptchaCode(String methodname, File captchaFile, DownloadLink downloadLink) throws PluginException {
        return getCaptchaCode(methodname, captchaFile, 0, downloadLink, null, null);
    }

    protected String getCaptchaCode(String method, File file, int flag, DownloadLink link, String defaultValue, String explain) throws PluginException {
        String status = link.getLinkStatus().getStatusText();
        try {
            link.getLinkStatus().addStatus(LinkStatus.WAITING_USERIO);
            link.getLinkStatus().setStatusText(JDLocale.L("gui.downloadview.statustext.jac", "Captcha recognition"));
            try {
                BufferedImage img = ImageIO.read(file);
                link.getLinkStatus().setStatusIcon(JDImage.getScaledImageIcon(img, 16, 16));
            } catch (IOException e) {
                e.printStackTrace();
            }
            DownloadController.getInstance().fireDownloadLinkUpdate(link);
            String cc = new CaptchaController(method, file, defaultValue, explain).getCode(flag);

            if (cc == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            return cc;
        } finally {
            link.getLinkStatus().removeStatus(LinkStatus.WAITING_USERIO);
            link.getLinkStatus().setStatusText(status);
            link.getLinkStatus().setStatusIcon(null);
            DownloadController.getInstance().fireDownloadLinkUpdate(link);
        }
    }

    private static final String AGB_CHECKED = "AGB_CHECKED";
    private static final String CONFIGNAME = "pluginsForHost";
    private static int currentConnections = 0;

    private static HashMap<Class<? extends PluginForHost>, Long> HOSTER_WAIT_TIMES = new HashMap<Class<? extends PluginForHost>, Long>();
    private static HashMap<Class<? extends PluginForHost>, Long> HOSTER_WAIT_UNTIL_TIMES = new HashMap<Class<? extends PluginForHost>, Long>();

    public static final String PARAM_MAX_RETRIES = "MAX_RETRIES";
    protected DownloadInterface dl = null;
    private int maxConnections = 50;

    private static HashMap<Class<? extends PluginForHost>, Long> LAST_CONNECTION_TIME = new HashMap<Class<? extends PluginForHost>, Long>();
    private static HashMap<Class<? extends PluginForHost>, Long> LAST_STARTED_TIME = new HashMap<Class<? extends PluginForHost>, Long>();
    private Long WAIT_BETWEEN_STARTS = 0L;

    private boolean enablePremium = false;

    private boolean accountWithoutUsername = false;

    private String premiumurl = null;

    private boolean canResume = false;

    private ImageIcon hosterIcon;

    public void setResume(boolean b) {
        canResume = b;
    }

    public boolean checkLinks(DownloadLink[] urls) {
        return false;
    }

    // @Override
    public void clean() {
        dl = null;
        super.clean();
    }

    protected int waitForFreeConnection(DownloadLink downloadLink) throws InterruptedException {
        int free;
        while ((free = this.getMaxConnections() - getCurrentConnections()) <= 0) {
            Thread.sleep(1000);
            downloadLink.getLinkStatus().setStatusText(JDLocale.LF("download.system.waitForconnection", "Cur. %s/%s connections...waiting", getCurrentConnections() + "", this.getMaxConnections() + ""));
            downloadLink.requestGuiUpdate();
        }
        return free;

    }

    protected void setBrowserExclusive() {
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
    }

    // @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getID() == 1) {
            SimpleGUI.displayConfig(config, 0);
            return;
        }
        if (e.getID() == 2) {
            SimpleGUI.displayConfig(config, 1);
            return;
        }
        ArrayList<Account> accounts = getPremiumAccounts();
        if (e.getID() >= 200) {
            int accountID = e.getID() - 200;
            Account account = accounts.get(accountID);
            JDUtilities.getGUI().showAccountInformation(this, account);
        } else if (e.getID() >= 100) {
            int accountID = e.getID() - 100;
            Account account = accounts.get(accountID);
            account.setEnabled(!account.isEnabled());
        }
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        if (account.getPass() == null || account.getUser() == null) {
            // frische INstanz
            return null;
        }
        if (account.getPass().trim().length() == 0 && account.getUser().trim().length() == 0) {
            // frische INstanz
            return null;
        }
        if (account.getProperty(AccountInfo.PARAM_INSTANCE) != null) {
            AccountInfo ai = (AccountInfo) account.getProperty(AccountInfo.PARAM_INSTANCE);
            if ((System.currentTimeMillis() - ai.getCreateTime()) < 5 * 60 * 1000) return ai;
        }
        try {
            AccountInfo ret = fetchAccountInfo(account);

            if (ret == null) return null;

            if (ret.isExpired()) {
                account.setEnabled(false);
                account.setProperty(AccountInfo.PARAM_INSTANCE, null);
                String shortWarn = JDLocale.LF("gui.shortwarn.accountdisabled.expired", "Account %s(%s) got disabled(expired)", this.getHost(), account.getUser());

                if (JDController.getInstance().getUiInterface() != null) JDController.getInstance().getUiInterface().displayMiniWarning(JDLocale.L("gui.ballon.accountmanager.title", "Accountmanager"), shortWarn);
            } else if (!ret.isValid()) {
                account.setEnabled(false);
                account.setProperty(AccountInfo.PARAM_INSTANCE, null);
                String shortWarn = JDLocale.LF("gui.shortwarn.accountdisabled.invalid", "Account %s(%s) got disabled(invalid)", this.getHost(), account.getUser());

                if (JDController.getInstance().getUiInterface() != null) JDController.getInstance().getUiInterface().displayMiniWarning(JDLocale.L("gui.ballon.accountmanager.title", "Accountmanager"), shortWarn);
            }

            account.setProperty(AccountInfo.PARAM_INSTANCE, ret);
            return ret;
        } catch (PluginException e) {
            account.setEnabled(false);
            account.setProperty(AccountInfo.PARAM_INSTANCE, null);
            String shortWarn = JDLocale.LF("gui.shortwarn.accountdisabled", "Account %s(%s) got disabled: %s", this.getHost(), account.getUser(), e.getMessage());

            if (JDController.getInstance().getUiInterface() != null) JDController.getInstance().getUiInterface().displayMiniWarning(shortWarn, shortWarn);
            throw e;
        }

    }

    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        return null;
    }

    public boolean getAccountwithoutUsername() {
        return accountWithoutUsername;
    }

    public void setAccountwithoutUsername(boolean b) {
        accountWithoutUsername = b;
    }

    // @Override
    public ArrayList<MenuItem> createMenuitems() {

        ArrayList<MenuItem> menuList = new ArrayList<MenuItem>();
        if (!this.enablePremium) return null;
        MenuItem account;
        MenuItem m = new MenuItem(MenuItem.NORMAL, JDLocale.L("plugins.menu.configs", "Configuration"), 1);
        m.setActionListener(this);
        MenuItem premium = new MenuItem(MenuItem.CONTAINER, JDLocale.L("plugins.menu.accounts", "Accounts"), 0);
        menuList.add(m);
        ArrayList<Account> accounts = getPremiumAccounts();

        int i = 1;
        int c = 0;
        for (Account a : accounts) {
            if (a == null) continue;
            try {
                c++;
                if (getAccountwithoutUsername()) {
                    if (a.getPass() == null || a.getPass().trim().length() == 0) continue;
                    account = new MenuItem(MenuItem.CONTAINER, i++ + ". " + "Account " + (i - 1), 0);
                } else {

                    if (a.getUser() == null || a.getUser().trim().length() == 0) continue;
                    account = new MenuItem(MenuItem.CONTAINER, i++ + ". " + a.getUser(), 0);
                    m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("plugins.menu.enable_premium", "Aktivieren"), 100 + c - 1);
                    m.setSelected(a.isEnabled());
                    m.setActionListener(this);
                    account.addMenuItem(m);

                    m = new MenuItem(JDLocale.L("plugins.menu.premiumInfo", "Accountinformationen abrufen"), 200 + c - 1);
                    m.setActionListener(this);
                    account.addMenuItem(m);
                    premium.addMenuItem(account);

                }

            } catch (Exception e) {
                JDLogger.exception(e);
            }
        }
        if (premium.getSize() != 0) {
            menuList.add(premium);
        } else {
            menuList.add(m = new MenuItem(JDLocale.L("plugins.menu.noaccounts", "Add account"), 2));
            m.setActionListener(this);
        }

        return menuList;

    }

    public abstract String getAGBLink();

    protected void enablePremium() {
        this.enablePremium(null);
    }

    protected void enablePremium(String url) {

        this.premiumurl = url;

        enablePremium = true;
        ConfigEntry cfg;

        ConfigContainer premiumConfig = new ConfigContainer(JDLocale.L("plugins.hoster.premiumtab", "Premium Einstellungen"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CONTAINER, premiumConfig));

        premiumConfig.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_PREMIUMPANEL, null, this.getHost(), 0));
        cfg.setActionListener(this);
        cfg.setDefaultValue(new ArrayList<Account>());

    }

    public synchronized int getCurrentConnections() {
        return currentConnections;
    }

    /**
     * Hier werden Treffer für Downloadlinks dieses Anbieters in diesem Text
     * gesucht. Gefundene Links werden dann in einem ArrayList zurückgeliefert
     * 
     * @param data
     *            Ein Text mit beliebig vielen Downloadlinks dieses Anbieters
     * @return Ein ArrayList mit den gefundenen Downloadlinks
     */
    public ArrayList<DownloadLink> getDownloadLinks(String data, FilePackage fp) {

        ArrayList<DownloadLink> links = null;
        String[] hits = new Regex(data, getSupportedLinks()).getColumn(-1);
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
                    // Zwecks Multidownload braucht jeder Link seine eigene
                    // Plugininstanz
                    PluginForHost plg = (PluginForHost) wrapper.getNewPluginInstance();
                    DownloadLink link = new DownloadLink(plg, file.substring(file.lastIndexOf("/") + 1, file.length()), getHost(), file, true);
                    try {
                        correctDownloadLink(link);
                    } catch (Exception e) {
                        JDLogger.exception(e);
                    }
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

    public void correctDownloadLink(DownloadLink link) throws Exception {
        /* überschreiben falls die downloadurl erst rekonstruiert werden muss */
    }

    /**
     * Holt Informationen zu einem Link. z.B. dateigröße, Dateiname,
     * verfügbarkeit etc.
     * 
     * @param parameter
     * @return true/false je nach dem ob die Datei noch online ist (verfügbar)
     * @throws IOException
     */
    public abstract AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception;

    /**
     * Gibt einen String mit den Dateiinformationen zurück. Die Defaultfunktion
     * gibt nur den dateinamen zurück. Allerdings Sollte diese Funktion
     * überschrieben werden. So kann ein Plugin zusatzinfos zu seinen Links
     * anzeigen (Nach dem aufruf von getFileInformation()
     * 
     * @param downloadLink
     * @return
     */
    public String getFileInformationString(DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + Formatter.formatReadable(downloadLink.getDownloadSize()) + ")";
    }

    public synchronized int getFreeConnections() {
        return Math.max(1, this.getMaxConnections() - currentConnections);
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
        return getMaxSimultanDownloadNum();
    }

    public int getMaxSimultanDownloadNum() {
        return SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2);
    }

    public boolean ignoreHosterWaittime(DownloadLink link) {
        if (AccountController.getInstance().getValidAccount(this) == null) return false;
        if (!this.enablePremium || !JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true)) return false;
        return true;
    }

    public int getMaxSimultanDownloadNum(DownloadLink link) {
        return ignoreHosterWaittime(link) ? getMaxSimultanPremiumDownloadNum() : getMaxSimultanFreeDownloadNum();
    }

    public long getRemainingHosterWaittime() {
        if (!HOSTER_WAIT_UNTIL_TIMES.containsKey(this.getClass())) { return 0; }
        return Math.max(0, (HOSTER_WAIT_UNTIL_TIMES.get(this.getClass()) - System.currentTimeMillis()));
    }

    public synchronized long getLastTimeStarted() {
        if (!LAST_STARTED_TIME.containsKey(this.getClass())) { return 0; }
        return Math.max(0, (LAST_STARTED_TIME.get(this.getClass())));
    }

    public synchronized void putLastTimeStarted(long time) {
        LAST_STARTED_TIME.put(this.getClass(), time);
    }

    public synchronized long getLastConnectionTime() {
        if (!LAST_CONNECTION_TIME.containsKey(this.getClass())) { return 0; }
        return Math.max(0, (LAST_CONNECTION_TIME.get(this.getClass())));
    }

    public synchronized void putLastConnectionTime(long time) {
        LAST_CONNECTION_TIME.put(this.getClass(), time);
    }

    public void handlePremium(DownloadLink link, Account account) throws Exception {
        link.getLinkStatus().addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
        link.getLinkStatus().setErrorMessage(JDLocale.L("plugins.hoster.nopremiumsupport", "Plugin has no handlePremium Method!"));
    }

    public boolean canResume(DownloadLink link) {
        return canResume;
    }

    public abstract void handleFree(DownloadLink link) throws Exception;

    public void handle(DownloadLink downloadLink) throws Exception {
        try {
            while (waitForNextStartAllowed(downloadLink)) {
            }
        } catch (InterruptedException e) {
            return;
        }
        putLastTimeStarted(System.currentTimeMillis());
        if (!isAGBChecked()) {
            logger.severe("AGB not signed : " + getPluginID());
            downloadLink.getLinkStatus().addStatus(LinkStatus.ERROR_AGB_NOT_SIGNED);
            return;
        }

        Long t = 0l;

        if (HOSTER_WAIT_UNTIL_TIMES.containsKey(this.getClass())) {
            t = HOSTER_WAIT_UNTIL_TIMES.get(this.getClass());
        }
        Account account = null;
        if (enablePremium) account = AccountController.getInstance().getValidAccount(this);
        if (account != null) {
            long before = downloadLink.getDownloadCurrent();
            try {
                handlePremium(downloadLink, account);
                if (dl != null && dl.getConnection() != null) {
                    try {
                        dl.getConnection().disconnect();
                    } catch (Exception e) {
                    }
                }
            } catch (PluginException e) {
                e.fillLinkStatus(downloadLink.getLinkStatus());
                logger.info(downloadLink.getLinkStatus().getLongErrorMessage());
            }

            long traffic = downloadLink.getDownloadCurrent() - before;
            if (traffic > 0 && account.getProperty(AccountInfo.PARAM_INSTANCE) != null) {
                AccountInfo ai = (AccountInfo) account.getProperty(AccountInfo.PARAM_INSTANCE);
                ai.setTrafficLeft(ai.getTrafficLeft() - traffic);
            }

            if (downloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_PREMIUM)) {
                if (downloadLink.getLinkStatus().getValue() == LinkStatus.VALUE_ID_PREMIUM_TEMP_DISABLE) {
                    logger.severe("Premium Account " + account.getUser() + ": Traffic Limit reached");
                    account.setTempDisabled(true);
                    account.setStatus(downloadLink.getLinkStatus().getErrorMessage());
                } else if (downloadLink.getLinkStatus().getValue() == LinkStatus.VALUE_ID_PREMIUM_DISABLE) {
                    account.setEnabled(false);
                    account.setStatus(downloadLink.getLinkStatus().getLongErrorMessage());
                    logger.severe("Premium Account " + account.getUser() + ": expired:" + downloadLink.getLinkStatus().getLongErrorMessage());
                } else {
                    account.setEnabled(false);
                    account.setStatus(downloadLink.getLinkStatus().getLongErrorMessage());
                    logger.severe("Premium Account " + account.getUser() + ":" + downloadLink.getLinkStatus().getLongErrorMessage());
                }
            } else {
                account.setStatus(JDLocale.L("plugins.hoster.premium.status_ok", "Account is ok"));
            }
        } else {
            if (t > 0) {
                this.resetHosterWaitTime();
                DownloadController.getInstance().fireGlobalUpdate();
            }
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
                logger.info(downloadLink.getLinkStatus().getLongErrorMessage());
            }
        }
        return;
    }

    public boolean isAGBChecked() {
        if (!getPluginConfig().hasProperty(AGB_CHECKED)) {
            getPluginConfig().setProperty(AGB_CHECKED, SubConfiguration.getConfig(CONFIGNAME).getBooleanProperty("AGBS_CHECKED_" + getPluginID(), false) || SubConfiguration.getConfig(CONFIGNAME).getBooleanProperty("AGB_CHECKED_" + getHost(), false));
            getPluginConfig().save();
        }
        return getPluginConfig().getBooleanProperty(AGB_CHECKED, false);
    }

    /**
     * Stellt das Plugin in den Ausgangszustand zurück (variablen intialisieren
     * etc)
     */
    public abstract void reset();

    public abstract void reset_downloadlink(DownloadLink link);

    public void resetHosterWaitTime() {
        HOSTER_WAIT_TIMES.put(this.getClass(), 0l);
        HOSTER_WAIT_UNTIL_TIMES.put(this.getClass(), 0l);
    }

    public void resetPluginGlobals() {
        br = new Browser();
    }

    public void setAGBChecked(boolean value) {
        getPluginConfig().setProperty(AGB_CHECKED, value);
        getPluginConfig().save();
    }

    public synchronized void setCurrentConnections(int CurrentConnections) {
        currentConnections = CurrentConnections;
    }

    public void setHosterWaittime(long milliSeconds) {
        HOSTER_WAIT_TIMES.put(this.getClass(), milliSeconds);
        HOSTER_WAIT_UNTIL_TIMES.put(this.getClass(), System.currentTimeMillis() + milliSeconds);
    }

    public int getTimegapBetweenConnections() {
        return 750;
    }

    public void setStartIntervall(long interval) {
        WAIT_BETWEEN_STARTS = interval;
    }

    public boolean waitForNextStartAllowed(DownloadLink downloadLink) throws InterruptedException {
        long time = Math.max(0, WAIT_BETWEEN_STARTS - (System.currentTimeMillis() - getLastTimeStarted()));
        if (time > 0) {
            try {
                this.sleep(time, downloadLink);
            } catch (PluginException e) {

                // downloadLink.getLinkStatus().setStatusText(null);
                throw new InterruptedException();
            }
            // downloadLink.getLinkStatus().setStatusText(null);
            return true;
        } else {
            // downloadLink.getLinkStatus().setStatusText(null);
            return false;
        }
    }

    public boolean waitForNextConnectionAllowed() throws InterruptedException {
        long time = Math.max(0, getTimegapBetweenConnections() - (System.currentTimeMillis() - getLastConnectionTime()));
        if (time > 0) {
            Thread.sleep(time);
            return true;
        } else {
            return false;
        }
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public void sleep(long i, DownloadLink downloadLink) throws PluginException {
        sleep(i, downloadLink, "");
    }

    public void sleep(long i, DownloadLink downloadLink, String message) throws PluginException {
        try {
            while (i > 0 && downloadLink.getDownloadLinkController() != null && !downloadLink.getDownloadLinkController().isAborted()) {
                i -= 1000;
                downloadLink.getLinkStatus().setStatusText(message + JDLocale.LF("gui.downloadlink.status.wait", "wait %s min", Formatter.formatSeconds(i / 1000)));
                downloadLink.requestGuiUpdate();
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            throw new PluginException(LinkStatus.TODO);
        }
        downloadLink.getLinkStatus().setStatusText(null);
    }

    /**
     * wird vom controlling (watchdog) beim stoppen aufgerufen. Damit werdend ie
     * hostercontrollvariablen zurückgesetzt.
     */
    public static void resetStatics() {
        HOSTER_WAIT_TIMES = new HashMap<Class<? extends PluginForHost>, Long>();
        HOSTER_WAIT_UNTIL_TIMES = new HashMap<Class<? extends PluginForHost>, Long>();
    }

    public Browser getBrowser() {
        return br;
    }

    public void setDownloadInterface(DownloadInterface dl2) {
        this.dl = dl2;
    }

    /**
     * Gibt die Url zurück, unter welcher ein PremiumAccount gekauft werden kann
     * 
     * @return
     */
    public String getBuyPremiumUrl() {
        return this.premiumurl;
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
        return "";
    }

    public final boolean hasHosterIcon() {
        return hosterIcon != null || JDImage.getImage("hosterlogos/" + getHost()) != null;
    }

    public final ImageIcon getHosterIcon() {
        if (hosterIcon == null) hosterIcon = initHosterIcon();
        return hosterIcon;
    }

    private final ImageIcon initHosterIcon() {
        Image image = JDImage.getImage("hosterlogos/" + getHost());
        if (image == null) image = createDefaultIcon();
        if (image != null) return new ImageIcon(image);
        return null;
    }

    /**
     * Creates a dummyHosterIcon
     */
    private final Image createDefaultIcon() {
        int w = 16;
        int h = 16;
        int size = 9;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        final BufferedImage image = gc.createCompatibleImage(w, h, Transparency.BITMASK);
        Graphics2D g = image.createGraphics();
        String host = this.getClass().getSimpleName();
        String dummy = host.replaceAll("[a-z0-9]", "");
        if (dummy.length() < 2) dummy = host.toUpperCase();
        if (dummy.length() > 2) dummy = dummy.substring(0, 2);
        g.setFont(new Font("Arial", Font.BOLD, size));
        int ww = g.getFontMetrics().stringWidth(dummy);
        // g.setColor(Color.BLACK);
        // g.drawRect(0, 0, w - 1, h - 1);

        g.setColor(Color.WHITE);
        g.fillRect(1, 1, w - 2, h - 2);
        g.setColor(Color.BLACK);
        g.drawString(dummy, (w - ww) / 2, 2 + size);

        g.dispose();
        try {
            File imageFile = JDUtilities.getResourceFile("jd/img/hosterlogos/" + getHost() + ".png");
            if (!imageFile.getParentFile().exists()) imageFile.getParentFile().mkdirs();
            ImageIO.write(image, "png", imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return image;

    }
}
