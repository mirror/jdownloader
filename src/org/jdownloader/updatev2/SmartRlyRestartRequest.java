package org.jdownloader.updatev2;

import org.appwork.shutdown.BasicShutdownRequest;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoListener;

public class SmartRlyRestartRequest extends BasicShutdownRequest implements RestartRequest {

    private volatile boolean alreadyAsked = false;
    private volatile boolean gotVeto      = false;
    private String[]         restartParams;

    public SmartRlyRestartRequest(boolean alreadyAsked, String[] arguments) {
        super();
        this.alreadyAsked = alreadyAsked;
        restartParams = arguments;
    }

    public SmartRlyRestartRequest(boolean alreadyAsked) {
        super();
        this.alreadyAsked = alreadyAsked;
        restartParams = new String[] {};
    }

    public SmartRlyRestartRequest() {
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

    @Override
    public String[] getArguments() {
        return restartParams;
    }
}
