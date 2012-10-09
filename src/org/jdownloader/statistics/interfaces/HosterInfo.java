package org.jdownloader.statistics.interfaces;

import org.appwork.storage.Storable;

public class HosterInfo implements Storable {
    public HosterInfo(/* storable */) {

    }

    private String host;
    private long   speedAvgPremium;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public double getDownloadsPremiumPercent() {
        if (downloadsPremiumTotal == 0) return -1d;
        return (downloadsPremium * 10000 / downloadsPremiumTotal) / 100d;

    }

    public double getDownloadsFreePercent() {
        if (downloadsFreeTotal == 0) return -1d;
        return (downloadsFree * 10000 / downloadsFreeTotal) / 100d;

    }

    public long getSpeedAvgPremium() {
        return speedAvgPremium;
    }

    public long getSpeedAvgFree() {
        return speedAvgFree;
    }

    public long getDownloadsPremium() {
        return downloadsPremium;
    }

    public long getSpeedPremium() {
        return speedPremium;
    }

    public long getDownloadsFree() {
        return downloadsFree;
    }

    public long getSpeedFree() {
        return speedFree;
    }

    private long speedAvgFree;
    private long downloadsPremium;
    private long speedPremium;
    private long downloadsFree;
    private long speedFree;
    private long downloadsPremiumTotal;
    private long downloadsFreeTotal;

    public HosterInfo(String string) {
        this.host = string;
    }

    public void setSpeedAvgPremium(long speedPremium) {
        this.speedAvgPremium = speedPremium;
    }

    public void setSpeedAvgFree(long speed) {
        this.speedAvgFree = speed;
    }

    public void setDownloadsPremium(long long1) {
        this.downloadsPremium = long1;
    }

    public void setSpeedPremium(long long1) {
        this.speedPremium = long1;
    }

    public void setDownloadsFree(long long1) {
        this.downloadsFree = long1;
    }

    public void setSpeedFree(long long1) {
        this.speedFree = long1;
    }

    public void setDownloadsPremiumTotal(long totalPremium) {
        downloadsPremiumTotal = totalPremium;
    }

    public long getDownloadsPremiumTotal() {
        return downloadsPremiumTotal;
    }

    public long getDownloadsFreeTotal() {
        return downloadsFreeTotal;
    }

    public void setDownloadsFreeTotal(long totalFree) {
        downloadsFreeTotal = totalFree;
    }

}
