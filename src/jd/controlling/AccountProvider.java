package jd.controlling;

import java.util.ArrayList;
import java.util.EventListener;

import jd.plugins.Account;

public interface AccountProvider extends EventListener {

    public ArrayList<Account> provideAccountsFor(String host);

}
