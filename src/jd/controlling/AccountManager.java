package jd.controlling;

import java.util.ArrayList;

import jd.HostPluginWrapper;
import jd.config.SubConfiguration;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.event.JDBroadcaster;
import jd.plugins.Account;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

public class AccountManager extends SubConfiguration implements ControlListener {

    private static final long serialVersionUID = -9118055222709490746L;
    public static AccountManager INSTANCE = null;
    private static final String PROPERTY_PREMIUM = "PREMIUM";
    private static final String IMPORTEDFLAG = "imported";
    private JDBroadcaster<AccountListener, AccountsUpdateEvent> broadcaster;
    private boolean inited = false;

    private AccountManager() {
        super("AccountManager");
        this.broadcaster = new JDBroadcaster<AccountListener, AccountsUpdateEvent>() {

            // @Override
            protected void fireEvent(AccountListener listener, AccountsUpdateEvent event) {
                listener.onUpdate();
            }

        };
        JDController.getInstance().addControlListener(this);

    }

    public void fireChange(Object account) {
        broadcaster.fireEvent(new AccountsUpdateEvent(account, AccountsUpdateEvent.CHANGED));
    }

    private void init() {
        if (this.inited) return;
        inited = true;
        if (!hasProperty(IMPORTEDFLAG)) importOld();
    }

    @SuppressWarnings("unchecked")
    private void importOld() {
        for (HostPluginWrapper wrapper : JDUtilities.getPluginsForHost()) {
            ArrayList<Account> list = (ArrayList<Account>) wrapper.getPluginConfig().getProperty(PROPERTY_PREMIUM, new ArrayList<Account>());
            if (list != null) {
                setProperty(wrapper.getHost(), list);
            }
        }
        setProperty(IMPORTEDFLAG, true);
        save();
    }

    public synchronized static AccountManager getInstance() {
        if (INSTANCE == null) INSTANCE = new AccountManager();
        return INSTANCE;
    }

    public static ArrayList<Account> getAccounts(PluginForHost pluginForHost) {
        return getInstance().getAccountsForHost(pluginForHost.getHost());
    }

    @SuppressWarnings("unchecked")
    private ArrayList<Account> getAccountsForHost(String host) {
        return (ArrayList<Account>) getProperty(host, new ArrayList<Account>());
    }

    /**
     * Sets the accountlist for a special host. !Overwrites existing ones
     * 
     * @param plugin
     * @param accounts
     */
    public void setAccountsForHost(PluginForHost plugin, ArrayList<Account> accounts) {
        setProperty(plugin.getHost(), accounts);
        save();
    }

    public synchronized Account getValidAccount(PluginForHost pluginForHost) {

        ArrayList<Account> accounts = getAccountsForHost(pluginForHost.getHost());
        Account ret = null;

        for (int i = 0; i < accounts.size(); i++) {
            Account next = accounts.get(i);

            if (!next.isTempDisabled() && next.isEnabled() && next.getPass() != null && next.getPass().trim().length() > 0) {
                ret = next;

                break;
            }
        }

        return ret;
    }

    // @Override
    public synchronized void setProperty(String key, Object value) {
        init();
        super.setProperty(key, value);
    }

    // @Override
    public Object getProperty(String key, Object def) {
        init();
        return super.getProperty(key, def);
    }

    public void addAccountListener(AccountListener listener) {
        this.broadcaster.addListener(listener);

    }

    public void removeAccountListener(AccountListener listener) {
        this.broadcaster.removeListener(listener);

    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_JDPROPERTY_CHANGED && event.getSource() == this) {
            broadcaster.fireEvent(new AccountsUpdateEvent(this, AccountsUpdateEvent.CHANGED));
        }
    }

    // @Override
    public void save() {
        init();
        super.save();
    }
}
