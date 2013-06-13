package org.jdownloader.updatev2;

import org.appwork.shutdown.ShutdownVetoFilter;
import org.appwork.shutdown.ShutdownVetoListener;

public class AvoidRlyExistListener implements ShutdownVetoFilter {

    private volatile boolean alreadyAsked = false;
    private volatile boolean gotVeto      = false;

    public AvoidRlyExistListener(boolean alreadyAsked) {
        this.alreadyAsked = alreadyAsked;
    }

    @Override
    public boolean askForVeto(ShutdownVetoListener listener) {
        if (listener instanceof RestartController) {
            if (alreadyAsked) {
                return false;
            } else {
                alreadyAsked = true;
                return true;
            }
        }
        if (listener instanceof RlyExitListener) {
            if (alreadyAsked) {
                return false;
            } else {
                alreadyAsked = true;
                return true;
            }
        }
        if (gotVeto) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void gotVetoFrom(ShutdownVetoListener listener) {
        gotVeto = true;
    }

}
