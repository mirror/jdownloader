package jd.gui;

import jd.gui.userio.NoUserIF;
import jd.plugins.Account;
import jd.plugins.PluginForHost;

public abstract class UserIF {
    protected static UserIF INSTANCE = null;

    public static UserIF getInstance() {
        if (INSTANCE == null) INSTANCE = new NoUserIF();
        return INSTANCE;
    }

    public static void setInstance(UserIF instance) {
        INSTANCE = instance;
    }
/** 
 * An Abstract panelrepresentation used in requestPanel(Panels.*,Parameter
 * @author Coalado
 *
 */
    public static enum Panels {
        /**
         * REepresents the downloadlist view*
         */
        DOWNLOADLIST,
        /**
         * Represents the linkgrabber view
         * 
         */
        LINKGRABBER, 
        /**
         * Represents a configview. Parameter is the Subconfiguration or Property instance/ConfigContainer
         * 
         */
        CONFIGPANEL
    };

    /**
     * requests a special view from the gui. example:
     * 
     * JDUtilities.getGUI().requestPanel(Panels.*,parameter);
     * 
     * asks the gui backend to display the downloadlist. IT depends on the
     * guibackend which requests are fullfilled and which not.
     * 
     * @param parameter
     *            TODO
     * @param panelIdDownloadlist
     */
    public abstract void requestPanel(Panels panelID, Object parameter);

    /**
     * Zeigt die AccountInformationen an
     * 
     * @param pluginForHost
     *            Das HostPlugin für den der Account gilt
     * @param account
     *            Der Account für den die Informationen geholt werden soll
     */
    public abstract void showAccountInformation(PluginForHost pluginForHost, Account account);

    public abstract void displayMiniWarning(String shortWarn, String longWarn);

    /**
     * 
     * Minimiert die GUI. als ID können die WINDOW_STATUS_IDS aus UIConstants,*
     * verwendet werden
     */
    public abstract void setFrameStatus(int id);

}
