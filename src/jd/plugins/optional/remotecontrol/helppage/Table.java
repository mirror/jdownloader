package jd.plugins.optional.remotecontrol.helppage;

import java.util.Vector;

public class Table {

    private int index = -1;
    private String name = "";

    private Vector<Entry> entries = new Vector<Entry>();

    public Table(String name) {
        this.setName(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setCommand(String command) {
        entries.add(new Entry(command));
        index++;
    }

    public void setInfo(String info) {
        entries.get(index).setInfo(info);
    }

    public Vector<Entry> getEntries() {
        return entries;
    }

    public int size() {
        return (index + 1);
    }
}
