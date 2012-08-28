package org.jdownloader.extensions.streaming.dlna.profiles.streams.video;

public class Break {

    private int denominator;
    private int counter;

    public Break(int counter, int denominator) {
        this.counter = counter;
        this.denominator = denominator;
    }

    public String toString() {
        return counter + "/" + denominator;
    }

    public double getDouble() {
        return counter / (double) denominator;
    }

    public boolean equals(int c, int d) {
        return c == counter && d == denominator;
    }

}
