package jd.gui.userio;

import jd.gui.UserIF;
import jd.plugins.Account;
import jd.plugins.PluginForHost;

public class NoUserIF extends UserIF {

    public NoUserIF() {
        super();
    }

    @Override
    public void requestPanel(byte panelID) {
        System.out.println("NoUserIF set!");
    }

    @Override
    public void showAccountInformation(PluginForHost pluginForHost, Account account) {
        System.out.println("NoUserIF set!");
    }

    @Override
    public void displayMiniWarning(String shortWarn, String longWarn) {
        System.out.println("NoUserIF set!");
    }

    @Override
    public void setFrameStatus(int id) {
        System.out.println("NoUserIF set!");
    }

}
