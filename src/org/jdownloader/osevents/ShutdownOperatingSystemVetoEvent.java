package org.jdownloader.osevents;

public class ShutdownOperatingSystemVetoEvent extends OperatingSystemEvent {

    private boolean veto;

    public boolean isVeto() {
        return veto;
    }

    public void setVeto(boolean veto) {
        this.veto = veto;
    }

    public ShutdownOperatingSystemVetoEvent() {
        super(OperatingSystemEvent.Type.SHUTDOWN_VETO);
        veto = false;
    }

}
