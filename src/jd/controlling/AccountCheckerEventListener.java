package jd.controlling;

import java.util.EventListener;

public interface AccountCheckerEventListener extends EventListener {

    public void onCheckStarted();

    public void onCheckStopped();

}
