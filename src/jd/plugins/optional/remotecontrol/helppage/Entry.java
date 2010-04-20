package jd.plugins.optional.remotecontrol.helppage;

public class Entry {
    private String command = "";
    private String info = "";

    public Entry(String command) {
        this.setCommand(command);
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getInfo() {
        return info;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}
