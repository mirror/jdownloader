package jd.plugins;

public interface AccountTrafficView {
    public long getTrafficLeft();

    public long getTrafficMax();

    public long getReservedTraffic();

    public boolean isUnlimitedTraffic();

    public boolean isTrafficRefill();

    public boolean isSpecialTraffic();
}
