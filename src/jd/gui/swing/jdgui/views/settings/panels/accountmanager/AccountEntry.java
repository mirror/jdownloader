package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jd.plugins.Account;
import jd.plugins.PluginForHost;

import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;

public class AccountEntry extends Object {

    private Account account;

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public LazyHostPlugin getPlugin() {
        return plugin;
    }

    public void setPlugin(LazyHostPlugin plugin) {
        this.plugin = plugin;
    }

    private LazyHostPlugin plugin;
    private Method         accountDetailsMethod;

    public AccountEntry(Account acc, LazyHostPlugin plugin) {
        this.account = acc;
        this.plugin = plugin;
        try {
            Class<? extends PluginForHost> cls = plugin.getPrototype(null).getClass();
            accountDetailsMethod = cls.getDeclaredMethod("showAccountDetailsDialog", new Class[] { Account.class });

        } catch (Exception e) {

        }
    }

    public boolean isDetailsDialogSupported() {
        return accountDetailsMethod != null;
    }

    public void showAccountInfoDialog() {
        if (accountDetailsMethod != null) {
            try {
                accountDetailsMethod.invoke(plugin.getPrototype(null), account);
            } catch (UpdateRequiredClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

}
