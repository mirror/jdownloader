package jd.plugins;

import java.awt.Color;

public class PluginProgress {

    private long total;
    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getCurrent() {
        return current;
    }

    public void setCurrent(long current) {
        this.current = current;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    private long current;
    private Color color;

    public PluginProgress(long current, long totalSize, Color yellow) {
        this.total=totalSize;
        this.current=current;
        this.color=yellow;
    }

    public double getPercent() {
        // TODO Auto-generated method stub
        return Math.round((current*10000.0)/total)/100.0;
    }

}
