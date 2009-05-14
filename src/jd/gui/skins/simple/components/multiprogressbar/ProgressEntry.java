package jd.gui.skins.simple.components.multiprogressbar;

public class ProgressEntry {

    private long maximum = 0;
    private long value = 0;
    private long position;

    public long getMaximum() {
        return maximum;
    }

    public void setMaximum(long maximum) {
        this.maximum = maximum;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public ProgressEntry(long i) {
        this.maximum = i;
    }

    public void setPosition(long p) {
        position=p;
    }

    public long getPosition() {
        return position;
    }



}
