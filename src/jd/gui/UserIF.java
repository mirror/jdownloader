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
     * requests a special view from the gui. example:
     * 
     * JDUtilities.getGUI().requestPanel(UIConstants.PANEL_ID_DOWNLOADLIST);
     * 
     * asks the gui backend to display the downloadlist. IT depends on the
     * guibackend which requests are fullfilled and which not.
     * 
     * @param panelIdDownloadlist
     */
    public abstract void requestPanel(byte panelID);

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
