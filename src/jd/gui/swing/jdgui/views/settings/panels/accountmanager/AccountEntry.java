package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;
import jd.gui.swing.jdgui.views.settings.panels.pluginsettings.PluginSettings;
import jd.plugins.Account;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class AccountEntry {
    private final Account account;

    public Account getAccount() {
        return account;
    }

    public AccountEntry(Account acc) {
        this.account = acc;
    }

    public void showConfiguration() {
        JsonConfig.create(GraphicalUserInterfaceSettings.class).setConfigViewVisible(true);
        JDGui.getInstance().setContent(ConfigurationView.getInstance(), true);
        ConfigurationView.getInstance().setSelectedSubPanel(PluginSettings.class);
        if (account != null) {
            ConfigurationView.getInstance().getSubPanel(PluginSettings.class).setPlugin(account.getPlugin().getLazyP());
            ConfigurationView.getInstance().getSubPanel(PluginSettings.class).scrollToAccount(account);
        }
    }
}
