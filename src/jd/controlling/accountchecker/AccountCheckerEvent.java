package jd.controlling.accountchecker;

import org.appwork.utils.event.SimpleEvent;

public class AccountCheckerEvent extends SimpleEvent<AccountChecker, Object, AccountCheckerEvent.Types> {

    public AccountCheckerEvent(AccountChecker caller, Types event, Object[] parameters) {
        super(caller, event, parameters);
    }

    public static enum Types {
        CHECK_STARTED,
        CHECK_STOPPED
    }
}
