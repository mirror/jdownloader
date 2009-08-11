package jd.plugins;

import java.util.HashMap;

import jd.config.MenuAction;
import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.gui.swing.GuiRunnable;

/**
 * This calss maps a MenItem to an account, and help to synchronize states like
 * enabled/selected
 * 
 * @author Coalado
 * 
 */
public class AccountMenuItemSyncer implements AccountControllerListener {
    private static AccountMenuItemSyncer INSTANCE = null;
    private HashMap<Account, MenuAction> map;

    private AccountMenuItemSyncer() {
        map = new HashMap<Account, MenuAction>();
        AccountController.getInstance().addListener(this);
    }

    public synchronized static AccountMenuItemSyncer getInstance() {
        if (INSTANCE == null) INSTANCE = new AccountMenuItemSyncer();
        return INSTANCE;
    }

    public void map(Account a, MenuAction m) {

        map.put(a, m);

    }

    public void onAccountControllerEvent(final AccountControllerEvent event) {

        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                MenuAction item = map.get(event.getAccount());
                if (item != null) item.setSelected(event.getAccount().isEnabled());
                return null;
            }

        }.start();

    }

    public boolean vetoAccountGetEvent(String host, Account account) {
        // TODO Auto-generated method stub
        return false;
    }

    public MenuAction get(Account a) {
        // TODO Auto-generated method stub
        return map.get(a);
    }

    // 

}
