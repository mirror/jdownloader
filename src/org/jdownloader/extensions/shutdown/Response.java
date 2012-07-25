package org.jdownloader.extensions.shutdown;

public class Response {
    private String std;

    public String getStd() {
        return std;
    }

    public void setStd(String std) {
        this.std = std;
    }

    public String getErr() {
        return err;
    }

    public String toString() {
        return "Error:\r\n" + err + "\r\nStd:\r\n" + std + "\r\n ExitCode: " + exit;
    }

    public void setErr(String err) {
        this.err = err;
    }

    public int getExit() {
        return exit;
    }

    public void setExit(int exit) {
        this.exit = exit;
    }

    private String err;
    private int    exit = -1;
}
