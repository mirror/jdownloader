package jd.controlling;

import org.appwork.utils.event.Eventsender;

public class AccountCheckerEventSender extends Eventsender<AccountCheckerEventListener, AccountCheckerEvent> {

    @Override
    protected void fireEvent(AccountCheckerEventListener listener, AccountCheckerEvent event) {
        switch (event.getType()) {
        case CHECK_STARTED:
            listener.onCheckStarted();
            break;
        case CHECK_STOPPED:
            listener.onCheckStopped();
            break;
        }
    }

}
