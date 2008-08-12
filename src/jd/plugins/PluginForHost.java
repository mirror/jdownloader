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

    private static HashMap<Class<? extends PluginForHost>, Integer> HOSTER_WAIT_TIMES = new HashMap<Class<? extends PluginForHost>, Integer>();
    private static HashMap<Class<? extends PluginForHost>, Long> HOSTER_WAIT_UNTIL_TIMES = new HashMap<Class<? extends PluginForHost>, Long>();
    // private static HashMap<Class<? extends PluginForHost>, boolean[]>
    // HOSTER_TMP_ACCOUNT_STATUS = new HashMap<Class<? extends PluginForHost>,
    // boolean[]>();

    public static final String PARAM_MAX_RETRIES = "MAX_RETRIES";
    // public static final String PARAM_MAX_ERROR_RETRIES = "MAX_ERROR_RETRIES";
    // private static long END_OF_DOWNLOAD_LIMIT = 0;
    // public abstract URLConnection getURLConnection();
    protected DownloadInterface dl = null;
    // private int retryOnErrorCount = 0;
    private int maxConnections = 50;

    private static final int ACCOUNT_NUM = 5;
    public static final String PROPERTY_PREMIUM = "PREMIUM";
    protected Browser br = new Browser();
    private boolean enablePremium = false;

    // private boolean[] tmpAccountDisabled = new boolean[ACCOUNT_NUM];

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
        switch (e.getID()) {
        case 1:

            ConfigEntriesPanel cpanel = new ConfigEntriesPanel(config, "Select where filesdownloaded with JDownloader should be stored.");
           
           
            
            JPanel panel = new JPanel(new BorderLayout());
            JPanel topPanel = new JPanel();
            panel.add(topPanel, BorderLayout.NORTH);
            panel.add(cpanel, BorderLayout.CENTER);
            ConfigurationPopup pop = new ConfigurationPopup(SimpleGUI.CURRENTGUI.getFrame(), cpanel, panel, SimpleGUI.CURRENTGUI, JDUtilities.getConfiguration());
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

    public AccountInfo getAccountInformation(Account account) {
        // TODO Auto-generated method stub
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
        // MenuItem hh = new MenuItem(MenuItem.CONTAINER,
        // JDLocale.L("plugins.rapidshare.menu.happyHour", "Happy Hours"), 0);
        menuList.add(m);
        menuList.add(premium);
        ArrayList<Account> accounts = (ArrayList<Account>) getPluginConfig().getProperty(PROPERTY_PREMIUM, new ArrayList<Account>());

        int i = 1;
        int c = 0;
        for (Account a : accounts) {
            c++;
            if (a.getUser() == null || a.getUser().trim().length() == 0) continue;

            account = new MenuItem(MenuItem.CONTAINER, i + ". " + a.getUser(), 0);

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
        // for (int i = 1; i <= ACCOUNT_NUM; i++) {
        // premiumConfig.addEntry(cfg = new
        // ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        //
        // premiumConfig.addEntry(cfg = new
        // ConfigEntry(ConfigContainer.TYPE_LABEL, i + ". " +
        // JDLocale.L("plugins.hoster.premiumAccount", "Premium Account")));
        // conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
        // getProperties(), PROPERTY_USE_PREMIUM + "_" + i,
        // JDLocale.L("plugins.hoster.usePremium", "Premium Account
        // verwenden"));
        // premiumConfig.addEntry(conditionEntry);
        // premiumConfig.addEntry(cfg = new
        // ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(),
        // PROPERTY_PREMIUM_USER + "_" + i,
        // JDLocale.L("plugins.hoster.premiumUser", "Premium User")));
        // cfg.setDefaultValue(JDLocale.L("plugins.hoster.userid",
        // "Kundennummer"));
        // cfg.setEnabledCondidtion(conditionEntry, "==", true);
        // premiumConfig.addEntry(cfg = new
        // ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, getProperties(),
        // PROPERTY_PREMIUM_PASS + "_" + i,
        // JDLocale.L("plugins.hoster.premiumPass", "Premium Pass")));
        // cfg.setDefaultValue(JDLocale.L("plugins.hoster.pass", "Passwort"));
        // cfg.setEnabledCondidtion(conditionEntry, "==", true);
        // premiumConfig.addEntry(cfg = new
        // ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(),
        // PROPERTY_PREMIUM_MESSAGE + "_" + i,
        // JDLocale.L("plugins.hoster.premiumMessage", "Last account status")));
        //
        // conditionEntry.setDefaultValue(false);
        //
        // }

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

    // /**
    // * Diese methode führt den Nächsten schritt aus. Der gerade ausgeführte
    // * Schritt wir zurückgegeben
    // *
    // * @param parameter
    // * Ein Übergabeparameter
    // * @return der nächste Schritt oder null, falls alle abgearbeitet wurden
    // */
    // public void doNextStep(Object parameter) {
    //
    // nextStep(currentStep);
    //
    // if (//currentStep == null) {
    // logger.info(this + " Pluginende erreicht!");
    // return null;
    // }
    // logger.finer("Current Step: " + currentStep + "/" + steps);
    // if (!this.isAGBChecked()) {
    // current//step.setStatus(PluginStep.STATUS_ERROR);
    // logger.severe("AGB not signed : " + this.getPluginID());
    // ((DownloadLink) parameter).setStatus(LinkStatus.ERROR_AGB_NOT_SIGNED);
    // return currentStep;
    // }
    // //currentStep = doStep(currentStep, parameter);
    // logger.finer("got/return step: " + currentStep + " Linkstatus: " +
    // ((DownloadLink) parameter).getStatus());
    //
    // return currentStep;
    // }

    // public boolean isListOffline() {
    // return true;
    // }

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

        // Vector<String> hits = SimpleMatches.getMatches(data,
        // getSupportedLinks());
        String[] hits = new Regex(data, getSupportedLinks()).getMatches(0);
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
     */
    public abstract boolean getFileInformation(DownloadLink parameter);

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
        return parameter.getName();
    }

    // /**
    // * Diese Funktion verarbeitet jeden Schritt des Plugins.
    // *
    // * @param step
    // * @param parameter
    // * @return
    // */
    // public abstract PluginStep doStep( DownloadLink parameter) throws
    // Exception;

    public int getFreeConnections() {
        return Math.max(1, maxConnections - currentConnections);
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

    // public void abort(){
    // super.abort();
    // if(this.getDownloadInstance()!=null){
    // this.getDownloadInstance().abort();
    // }
    // }
    /*
     * private DownloadInterface getDownloadInstance() {
     * 
     * return this.dl; }
     */
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

    // public int getMaxRetriesOnError() {
    // return
    // JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(PARAM_MAX_ERROR_RETRIES,
    // 0);
    // }

    // /**
    // * Delegiert den doStep Call mit einem Downloadlink als Parameter weiter
    // an
    // * die Plugins. Und fängt übrige Exceptions ab.
    // *
    // * @param parameter
    // * Downloadlink
    // */
    // public void handle( Object parameter) {
    //
    // try {
    // PluginStep ret = doStep(step, (DownloadLink) parameter);
    // logger.finer("got/return step: " + step + " Linkstatus: " +
    // ((DownloadLink) parameter).getStatus());
    // return ret;
    // // if(ret==null){
    // // return;
    // // }else{
    // // return ret;
    // // }
    // } catch (Exception e) {
    // e.printStackTrace();
    // //step.setStatus(PluginStep.STATUS_ERROR);
    // ((DownloadLink) parameter).setStatus(LinkStatus.ERROR_PLUGIN_SPECIFIC);
    // //step.setParameter(e.getLocalizedMessage());
    // logger.finer("got/return 2 step: " + step + " Linkstatus: " +
    // ((DownloadLink) parameter).getStatus());
    //
    // return;
    // }
    // }

    // /**
    // * Kann im Downloadschritt verwendet werden um einen einfachen Download
    // * vorzubereiten
    // *
    // * @param downloadLink
    // * @param step
    // * @param url
    // * @param cookie
    // * @param redirect
    // * @return
    // */
    // protected boolean defaultDownloadStep(DownloadLink downloadLink, String
    // url, String cookie, boolean redirect) {
    // try {
    // requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(url), cookie, null,
    // redirect);
    //
    // int length = requestInfo.getConnection().getContentLength();
    // downloadLink.setDownloadMax(length);
    // logger.finer("Filename: " +
    // getFileNameFormHeader(requestInfo.getConnection()));
    //
    // downloadLink.setName(getFileNameFormHeader(requestInfo.getConnection()));
    // dl = new RAFDownload(this, downloadLink, requestInfo.getConnection());
    // dl.startDownload(); \r\n if (!dl.startDownload() && step.getStatus() !=
    // PluginStep.STATUS_ERROR && step.getStatus() != PluginStep.STATUS_TODO) {
    // linkStatus.addStatus(LinkStatus.ERROR_RETRY);
    //
    // //step.setStatus(PluginStep.STATUS_ERROR);
    //
    // }
    // return true;
    // } catch (MalformedURLException e) {
    //
    // e.printStackTrace();
    // } catch (IOException e) {
    //
    // e.printStackTrace();
    // }
    //
    // //step.setStatus(PluginStep.STATUS_ERROR);
    // linkStatus.addStatus(LinkStatus.ERROR_RETRY);
    // return false;
    //
    // }

    public long getRemainingHosterWaittime() {
        // TODO Auto-generated method stub
        if (!HOSTER_WAIT_UNTIL_TIMES.containsKey(this.getClass())) { return 0; }
        return Math.max(0, (HOSTER_WAIT_UNTIL_TIMES.get(this.getClass()) - System.currentTimeMillis()));
    }

    public void handlePremium(DownloadLink link, Account account) throws Exception {
        link.getLinkStatus().addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
        link.getLinkStatus().setErrorMessage("Plugin has no hanldPremium Method");
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

            // downloadLink.getLinkStatus().addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
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

                if (!next.isTempDisabled() && next.isEnabled()&&next.getPass()!=null&&next.getPass().trim().length()>0) {
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
                        account.setStatus(downloadLink.getLinkStatus().getErrorMessage());

                        getPluginConfig().save();
                        logger.severe("Premium Account " + account.getUser() + ": expired");
                    } else {

                        account.setEnabled(false);
                        account.setStatus(downloadLink.getLinkStatus().getErrorMessage());
                        getPluginConfig().save();
                        logger.severe("Premium Account " + account.getUser() + ":" + downloadLink.getLinkStatus().getErrorMessage());
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
        HOSTER_WAIT_TIMES.put(this.getClass(), 0);
        HOSTER_WAIT_UNTIL_TIMES.put(this.getClass(), 0l);

    }

    /**
     * Führt alle restevorgänge aus und bereitet das Plugin dadurch auf einen
     * Neustart vor. Sollte nicht überschrieben werden
     */
    @SuppressWarnings("unchecked")
    public final void resetPlugin() {
        // this.resetSteps();
        reset();
        ArrayList<Account> accounts = (ArrayList<Account>) getPluginConfig().getProperty(PROPERTY_PREMIUM, new ArrayList<Account>());

        for (Account account : accounts)
            account.setTempDisabled(false);
        // this.aborted = false;
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

    //
    // public void setRetryOnErrorCount(int retryOnErrorcount) {
    // this.retryOnErrorCount = retryOnErrorcount;
    // }

    // public static long getEndOfDownloadLimit() {
    // return END_OF_DOWNLOAD_LIMIT;
    // }
    //
    // public static void setEndOfDownloadLimit(long end_of_download_limit) {
    // END_OF_DOWNLOAD_LIMIT = end_of_download_limit;
    // }
    //
    // public static void setDownloadLimitTime(long downloadlimit) {
    // END_OF_DOWNLOAD_LIMIT = System.currentTimeMillis() + downloadlimit;
    // }
    //
    // public static long getRemainingWaittime() {
    // return Math.max(0, END_OF_DOWNLOAD_LIMIT - System.currentTimeMillis());
    // }

    public void setHosterWaittime(int milliSeconds) {

        HOSTER_WAIT_TIMES.put(this.getClass(), milliSeconds);
        HOSTER_WAIT_UNTIL_TIMES.put(this.getClass(), System.currentTimeMillis() + milliSeconds);

    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public void sleep(int i, DownloadLink downloadLink) throws InterruptedException {
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
        HOSTER_WAIT_TIMES = new HashMap<Class<? extends PluginForHost>, Integer>();
        HOSTER_WAIT_UNTIL_TIMES = new HashMap<Class<? extends PluginForHost>, Long>();

    }

    // public void handleDownloadLimit( DownloadLink downloadLink) {
    // long waitTime = getRemainingWaittime();
    // logger.finer("wait (intern) " + waitTime + " minutes");
    // downloadLink.getLinkStatus().setStatus(LinkStatus.ERROR_TRAFFIC_LIMIT);
    // ////step.setStatus(PluginStep.STATUS_ERROR);
    // logger.info(" Waittime(intern) set to " + step + " : " + waitTime);
    // //step.setParameter((long) waitTime);
    // return;
    // }

}
