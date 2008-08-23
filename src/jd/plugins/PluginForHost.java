//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JPanel;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.MenuItem;
import jd.event.ControlEvent;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.config.ConfigEntriesPanel;
import jd.gui.skins.simple.config.ConfigurationPopup;
import jd.http.Browser;
import jd.parser.Regex;
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
    private static final String AGB_CHECKED = "AGB_CHECKED";
    private static final String CONFIGNAME = "pluginsForHost";
    private static int currentConnections = 0;

    private static HashMap<Class<? extends PluginForHost>, Long> HOSTER_WAIT_TIMES = new HashMap<Class<? extends PluginForHost>, Long>();
    private static HashMap<Class<? extends PluginForHost>, Long> HOSTER_WAIT_UNTIL_TIMES = new HashMap<Class<? extends PluginForHost>, Long>();

    public static final String PARAM_MAX_RETRIES = "MAX_RETRIES";
    protected DownloadInterface dl = null;
    private int maxConnections = 50;

    public static final String PROPERTY_PREMIUM = "PREMIUM";
    private static Long LAST_CONNECTION_TIME = 0L;
    protected Browser br = new Browser();
    private boolean enablePremium = false;

    public boolean[] checkLinks(DownloadLink[] urls) {
        return null;
    }

    @Override
    public void clean() {
        dl = null;
        br = new Browser();
        super.clean();
    }

    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent e) {
        if (e.getID() == 1) {
            ConfigEntriesPanel cpanel = new ConfigEntriesPanel(config, "Select where filesdownloaded with JDownloader should be stored.");

            JPanel panel = new JPanel(new BorderLayout());
            panel.add(new JPanel(), BorderLayout.NORTH);
            panel.add(cpanel, BorderLayout.CENTER);
            ConfigurationPopup pop = new ConfigurationPopup(SimpleGUI.CURRENTGUI.getFrame(), cpanel, panel);
            pop.setLocation(JDUtilities.getCenterOfComponent(SimpleGUI.CURRENTGUI.getFrame(), pop));
            pop.setVisible(true);
        }
        ArrayList<Account> accounts = (ArrayList<Account>) getPluginConfig().getProperty(PROPERTY_PREMIUM, new ArrayList<Account>());
        if (e.getID() >= 200) {
            int accountID = e.getID() - 200;
            Account account = accounts.get(accountID);
            JDUtilities.getGUI().showAccountInformation(this, account);
            return;
        }

        if (e.getID() >= 100) {
            int accountID = e.getID() - 100;
            Account account = accounts.get(accountID);

            account.setEnabled(!account.isEnabled());
            getPluginConfig().save();
            return;
        }

    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ArrayList<MenuItem> createMenuitems() {

        ArrayList<MenuItem> menuList = new ArrayList<MenuItem>();
        if (!this.enablePremium) return null;
        MenuItem account;
        MenuItem m;
        m = new MenuItem(MenuItem.NORMAL, JDLocale.L("plugins.menu.configs", "Configuration"), 1);
        m.setActionListener(this);
        MenuItem premium = new MenuItem(MenuItem.CONTAINER, JDLocale.L("plugins.menu.accounts", "Accounts"), 0);
        menuList.add(m);
        menuList.add(premium);
        ArrayList<Account> accounts = (ArrayList<Account>) getPluginConfig().getProperty(PROPERTY_PREMIUM, new ArrayList<Account>());

        int i = 1;
        int c = 0;
        for (Account a : accounts) {
            c++;
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
        return menuList;

    }

    public abstract String getAGBLink();

    protected void enablePremium() {
        enablePremium = true;
        ConfigEntry cfg;

        ConfigContainer premiumConfig = new ConfigContainer(this, JDLocale.L("plugins.hoster.premiumtab", "Premium Einstellungen"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CONTAINER, premiumConfig));

        premiumConfig.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_PREMIUMPANEL, getPluginConfig(), PROPERTY_PREMIUM, 5));
        cfg.setActionListener(this);
        cfg.setDefaultValue(new ArrayList<Account>());

    }

    /**
     * Gibt zurück wie lange nach einem erkanntem Bot gewartet werden muss. Bei
     * -1 wird ein reconnect durchgeführt
     * 
     * @return
     */
    public long getBotWaittime() {

        return -1;
    }

    public int getChunksPerFile() {
        return JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2);
    }

    public int getCurrentConnections() {
        return currentConnections;
    }

    /**
     * Hier werden Treffer für Downloadlinks dieses Anbieters in diesem Text
     * gesucht. Gefundene Links werden dann in einem Vector zurückgeliefert
     * 
     * @param data
     *            Ein Text mit beliebig vielen Downloadlinks dieses Anbieters
     * @return Ein Vector mit den gefundenen Downloadlinks
     */
    public Vector<DownloadLink> getDownloadLinks(String data, FilePackage fp) {

        Vector<DownloadLink> links = null;
        String[] hits = new Regex(data, getSupportedLinks()).getColumn(-1);
        if (hits != null && hits.length > 0) {
            links = new Vector<DownloadLink>();
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
                    PluginForHost plg = this.getClass().newInstance();

                    DownloadLink link = new DownloadLink(plg, file.substring(file.lastIndexOf("/") + 1, file.length()), getHost(), file, true);
                    links.add(link);
                    if (fp != null) {
                        link.setFilePackage(fp);
                    }
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return links;
    }

    /**
     * Holt Informationen zu einem Link. z.B. dateigröße, Dateiname,
     * verfügbarkeit etc.
     * 
     * @param parameter
     * @return true/false je nach dem ob die Datei noch online ist (verfügbar)
     * @throws IOException
     */
    public abstract boolean getFileInformation(DownloadLink parameter) throws IOException;

    /**
     * Gibt einen String mit den Dateiinformationen zurück. Die Defaultfunktion
     * gibt nur den dateinamen zurück. Allerdings Sollte diese Funktion
     * überschrieben werden. So kann ein Plugin zusatzinfos zu seinen Links
     * anzeigen (Nach dem aufruf von getFileInformation()
     * 
     * @param parameter
     * @return
     */
    public String getFileInformationString(DownloadLink parameter) {
        return "";
    }

    public int getFreeConnections() {
        return Math.max(1, this.getMaxConnections() - currentConnections);
    }

    /**
     * Wird nicht gebraucht muss aber implementiert werden.
     */

    @Override
    public String getLinkName() {

        return null;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public int getMaxRetries() {
        return JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(PARAM_MAX_RETRIES, 3);
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public int getMaxSimultanPremiumDownloadNum() {
        return 20;
    }

    @SuppressWarnings("unchecked")
    public boolean ignoreHosterWaittime(DownloadLink link) {
        if (!this.enablePremium || !JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true)) { return false; }

        Account currentAccount = null;

        ArrayList<Account> accounts = (ArrayList<Account>) getPluginConfig().getProperty(PROPERTY_PREMIUM, new ArrayList<Account>());

        synchronized (accounts) {
            for (int i = 0; i < accounts.size(); i++) {
                if (!accounts.get(i).isTempDisabled() && accounts.get(i).isEnabled()) {

                    currentAccount = accounts.get(i);
                    break;
                }
            }
        }
        if (currentAccount != null) return true;
        return false;
    }

    public int getMaxSimultanDownloadNum(DownloadLink link) {
        return ignoreHosterWaittime(link) ? getMaxSimultanPremiumDownloadNum() : getMaxSimultanFreeDownloadNum();
    }

    public long getRemainingHosterWaittime() {
        // TODO Auto-generated method stub
        if (!HOSTER_WAIT_UNTIL_TIMES.containsKey(this.getClass())) { return 0; }
        return Math.max(0, (HOSTER_WAIT_UNTIL_TIMES.get(this.getClass()) - System.currentTimeMillis()));
    }

    public void handlePremium(DownloadLink link, Account account) throws Exception {
        link.getLinkStatus().addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
        link.getLinkStatus().setErrorMessage("Plugin has no hanldPremium Method");
    }

    public boolean canResume(DownloadLink link) {
        return ignoreHosterWaittime(link) ? true : false;
    }

    public abstract void handleFree(DownloadLink link) throws Exception;

    @SuppressWarnings("unchecked")
    public void handle(DownloadLink downloadLink) throws Exception {
        if (!isAGBChecked()) {

            logger.severe("AGB not signed : " + getPluginID());
            downloadLink.getLinkStatus().addStatus(LinkStatus.ERROR_AGB_NOT_SIGNED);
            downloadLink.getLinkStatus().setErrorMessage(JDLocale.L("plugins.hoster.error.agb", "TOC not signed"));
            return;
        }

        if (true) {

            // downloadLink.getLinkStatus().addStatus(LinkStatus.
            // ERROR_TEMPORARILY_UNAVAILABLE);
            // downloadLink.getLinkStatus().setErrorMessage("bla und so");
            // downloadLink.getLinkStatus().setValue(0);
            // return;
        }
        Long t = 0l;

        if (HOSTER_WAIT_UNTIL_TIMES.containsKey(this.getClass())) {
            t = HOSTER_WAIT_UNTIL_TIMES.get(this.getClass());
        }
        // RequestInfo requestInfo;
        if (!enablePremium || !JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true)) {

            if (t > 0) {
                this.resetHosterWaitTime();

                this.fireControlEvent(ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, JDUtilities.getController().getDownloadLinks(this));
            }
            handleFree(downloadLink);
            return;
        }
        Account account = null;
        ArrayList<Account> disabled = new ArrayList<Account>();

        ArrayList<Account> accounts = (ArrayList<Account>) getPluginConfig().getProperty(PROPERTY_PREMIUM, new ArrayList<Account>());

        synchronized (accounts) {
            for (int i = 0; i < accounts.size(); i++) {
                Account next = accounts.get(i);

                if (!next.isTempDisabled() && next.isEnabled() && next.getPass() != null && next.getPass().trim().length() > 0) {
                    account = next;

                    break;
                } else if (next.isTempDisabled() && next.isEnabled()) {

                    disabled.add(next);

                }
            }
        }
        if (account != null) {
            handlePremium(downloadLink, account);
            synchronized (accounts) {
                if (downloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_PREMIUM)) {
                    if (downloadLink.getLinkStatus().getValue() == LinkStatus.VALUE_ID_PREMIUM_TEMP_DISABLE) {
                        logger.severe("Premium Account " + account.getUser() + ": Traffic Limit reached");
                        account.setTempDisabled(true);
                        account.setStatus(downloadLink.getLinkStatus().getErrorMessage());
                        getPluginConfig().save();
                    } else if (downloadLink.getLinkStatus().getValue() == LinkStatus.VALUE_ID_PREMIUM_DISABLE) {

                        account.setEnabled(false);
                        account.setStatus(downloadLink.getLinkStatus().getLongErrorMessage());

                        getPluginConfig().save();
                        logger.severe("Premium Account " + account.getUser() + ": expired");
                    } else {

                        account.setEnabled(false);
                        account.setStatus(downloadLink.getLinkStatus().getLongErrorMessage());
                        getPluginConfig().save();
                        logger.severe("Premium Account " + account.getUser() + ":" + downloadLink.getLinkStatus().getLongErrorMessage());
                    }

                } else {
                    account.setStatus(JDLocale.L("plugins.hoster.premium.status_ok", "Account is ok"));
                    getPluginConfig().save();
                }
            }

        } else {
            if (t > 0) {
                this.resetHosterWaitTime();
                this.fireControlEvent(ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, JDUtilities.getController().getDownloadLinks(this));
            }
            handleFree(downloadLink);
            synchronized (accounts) {
                if (disabled.size() > 0) {
                    int randId = (int) (Math.random() * disabled.size());
                    disabled.get(randId).setTempDisabled(false);
                    getPluginConfig().save();
                }
            }
        }

        return;
    }

    public boolean isAGBChecked() {
        if (!getPluginConfig().hasProperty(AGB_CHECKED)) {
            getPluginConfig().setProperty(AGB_CHECKED, JDUtilities.getSubConfig(CONFIGNAME).getBooleanProperty("AGBS_CHECKED_" + getPluginID(), false) || JDUtilities.getSubConfig(CONFIGNAME).getBooleanProperty("AGB_CHECKED_" + getHost(), false));
            getPluginConfig().save();
        }
        return getPluginConfig().getBooleanProperty(AGB_CHECKED, false);
    }

    /**
     * Stellt das Plugin in den Ausgangszustand zurück (variablen intialisieren
     * etc)
     */
    public abstract void reset();

    public void resetHosterWaitTime() {
        HOSTER_WAIT_TIMES.put(this.getClass(), 0l);
        HOSTER_WAIT_UNTIL_TIMES.put(this.getClass(), 0l);

    }

    /**
     * Führt alle restevorgänge aus und bereitet das Plugin dadurch auf einen
     * Neustart vor. Sollte nicht überschrieben werden
     */
    @SuppressWarnings("unchecked")
    public final void resetPlugin() {
        reset();
        ArrayList<Account> accounts = (ArrayList<Account>) getPluginConfig().getProperty(PROPERTY_PREMIUM, new ArrayList<Account>());

        for (Account account : accounts)
            account.setTempDisabled(false);
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
        return 0;
    }

    public void waitForNextConnectionAllowed() throws InterruptedException {
        synchronized (LAST_CONNECTION_TIME) {
            long time = Math.max(0, getTimegapBetweenConnections() - (System.currentTimeMillis() - LAST_CONNECTION_TIME));
            Thread.sleep(time);
            LAST_CONNECTION_TIME = System.currentTimeMillis();
        }

    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public void sleep(long i, DownloadLink downloadLink) throws InterruptedException {
        while (i > 0 && downloadLink.getDownloadLinkController() != null && !downloadLink.getDownloadLinkController().isAborted()) {

            i -= 1000;
            downloadLink.getLinkStatus().setStatusText(String.format(JDLocale.L("gui.downloadlink.status.wait", "wait %s min"), JDUtilities.formatSeconds(i / 1000)));
            downloadLink.requestGuiUpdate();
            Thread.sleep(1000);

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

}
