
public class TwoLayerSpeedMeter {
    private long[] values;
    private long[] times;
    private long sum = 0;
    private long timeOldest = 0;
    private long timeYoungest = 0;
    private int i;
    public TwoLayerSpeedMeter(int i) {
        this.values = new long[i];
        this.times = new long[i];
        for (int c = 0; c < i; c++) {
            values[c] = 0;
            times[c] = System.currentTimeMillis();
        }
    }
    public synchronized void update(long value) {
        i = (i) % values.length;
        sum -= values[i];
        sum += value;
        values[i] = value;
        timeOldest = times[i];
        timeYoungest = System.currentTimeMillis();
        times[i++] = timeYoungest;        
    }
    public long getSpeed() {
        return (1000 * sum) / Math.max(1, (timeYoungest - timeOldest));
    }
}