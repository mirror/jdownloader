package org.jdownloader.captcha.v2.solver.myjd;

public class MyJDCESInfo {

    private MyJDCESStatus status;

    public MyJDCESStatus getStatus() {
        return status;
    }

    public void setStatus(MyJDCESStatus status) {
        this.status = status;
    }

    private boolean connected;

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
