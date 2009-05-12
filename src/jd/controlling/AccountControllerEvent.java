package jd.controlling;

import jd.event.JDEvent;
import jd.plugins.Account;

public class AccountControllerEvent extends JDEvent {

    private Account account;
    private String host;

    public AccountControllerEvent(Object source, int ID, String host, Account account) {
        super(source, ID);
        this.account = account;
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public Account getAccount() {
        return account;
    }

    public static final int ACCOUNT_ADDED = 1;
    public static final int ACCOUNT_REMOVED = 2;
    public static final int ACCOUNT_GET = 3;
    public static final int ACCOUNT_UPDATE = 4;

}
