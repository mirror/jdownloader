package jd.controlling;

import java.util.EventListener;

import jd.plugins.Account;

public interface AccountControllerListener extends EventListener {

    public boolean vetoAccountGetEvent(String host, Account account);

    public void onAccountControllerEvent(AccountControllerEvent event);
}
