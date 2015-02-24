package org.jdownloader.extensions.extraction.multi;


public class FilePermissionSet {

    private boolean groupRead = false;

    public boolean isGroupRead() {
        return groupRead;
    }

    public void setGroupRead(boolean groupRead) {
        this.groupRead = groupRead;
    }

    public boolean isGroupWrite() {
        return groupWrite;
    }

    public void setGroupWrite(boolean groupWrite) {
        this.groupWrite = groupWrite;
    }

    public boolean isGroupExecute() {
        return groupExecute;
    }

    public void setGroupExecute(boolean groupExecute) {
        this.groupExecute = groupExecute;
    }

    public boolean isOtherRead() {
        return otherRead;
    }

    public void setOtherRead(boolean otherRead) {
        this.otherRead = otherRead;
    }

    public boolean isOtherWrite() {
        return otherWrite;
    }

    public void setOtherWrite(boolean otherWrite) {
        this.otherWrite = otherWrite;
    }

    public boolean isOtherExecute() {
        return otherExecute;
    }

    public void setOtherExecute(boolean otherExecute) {
        this.otherExecute = otherExecute;
    }

    public boolean isUserRead() {
        return userRead;
    }

    public void setUserRead(boolean userRead) {
        this.userRead = userRead;
    }

    public boolean isUserWrite() {
        return userWrite;
    }

    public void setUserWrite(boolean userWrite) {
        this.userWrite = userWrite;
    }

    public boolean isUserExecute() {
        return userExecute;
    }

    public void setUserExecute(boolean userExecute) {
        this.userExecute = userExecute;
    }

    private boolean groupWrite   = false;
    private boolean groupExecute = false;

    private boolean otherRead    = false;
    private boolean otherWrite   = false;
    private boolean otherExecute = false;

    private boolean userRead     = false;
    private boolean userWrite    = false;
    private boolean userExecute  = false;

    @Override
    public String toString() {
        final char[] mask = { isUserRead() ? 'r' : '-', isUserWrite() ? 'w' : '-', isUserExecute() ? 'x' : '-', isGroupRead() ? 'r' : '-', isGroupWrite() ? 'w' : '-', isGroupExecute() ? 'x' : '-', isOtherRead() ? 'r' : '-', isOtherWrite() ? 'w' : '-', isOtherExecute() ? 'x' : '-' };
        return new String(mask);
    }
}
