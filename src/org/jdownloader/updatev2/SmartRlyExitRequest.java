package org.jdownloader.updatev2;

import org.appwork.shutdown.BasicShutdownRequest;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoListener;

public class SmartRlyExitRequest extends BasicShutdownRequest {

    private volatile boolean alreadyAsked = false;
    private volatile boolean gotVeto      = false;

    public SmartRlyExitRequest(boolean alreadyAsked) {
        super();
        this.alreadyAsked = alreadyAsked;

    }

    public SmartRlyExitRequest() {
        this(false);
    }

    @Override
    public boolean askForVeto(final ShutdownVetoListener listener) {
        if (listener instanceof RestartController) {
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
    public void addVeto(ShutdownVetoException e) {
        super.addVeto(e);
        gotVeto = true;
    }

}
