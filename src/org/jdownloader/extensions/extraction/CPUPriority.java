package org.jdownloader.extensions.extraction;

public enum CPUPriority {
    HIGH(0),
    MIDDLE(5),
    LOW(10);

    private int time = 0;

    private CPUPriority(int time) {
        this.time = Math.max(time, 0);
    }

    public int getTime() {
        return time;
    }
}