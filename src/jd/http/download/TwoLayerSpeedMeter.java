package jd.http.download;

/**
 * Diese Klasse verwendet zwei sich überschneidende Zähler, um den mittleren
 * Speed über ein Interval möglichst gut berechnen zu können.
 * 
 * @author coalado
 */
public class TwoLayerSpeedMeter {

    private int interval;
    private long latestCheckTime;
    private long bytes;
    private long bytes2;

    private int firstInterval;
    private int lastInterval;
    private long speed;
    private static final int DIVISOR = 3;

    public TwoLayerSpeedMeter(int i) {
        this.interval = i;
        bytes = 0;
        bytes2 = 0;

        latestCheckTime = System.currentTimeMillis();
        this.firstInterval = interval / DIVISOR;
        this.lastInterval = 0;
    }

    public void update(int value) {
        try {
            long current = System.currentTimeMillis();
            if (current > latestCheckTime + interval) {
                latestCheckTime = current;
                bytes = bytes2;
                bytes2 = 0;
                lastInterval = interval * (DIVISOR - 1) / DIVISOR;
            }
            this.speed = (bytes * 1000) / (current - latestCheckTime + lastInterval);
            bytes += value;
            if (current > latestCheckTime + firstInterval) {
                bytes2 += value;
            }
        } catch (Exception e) {
        }
    }

    public long getSpeed() {
        return speed;
    }

}