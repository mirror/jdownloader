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
    private long trafficFree;
    private long trafficPremium;
    private long trafficPremiumTotal;
    private long trafficFreeTotal;

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

    // public long getDownloadsPremiumTotal() {
    // return downloadsPremiumTotal;
    // }

    // public long getDownloadsFreeTotal() {
    // return downloadsFreeTotal;
    // }

    public void setDownloadsFreeTotal(long totalFree) {
        downloadsFreeTotal = totalFree;
    }

    public void setTrafficFree(long long1) {
        trafficFree = long1;
    }

    public void setTrafficPremium(long long1) {
        trafficPremium = long1;
    }

    public long getTrafficFree() {
        return trafficFree;
    }

    public long getTrafficPremium() {
        return trafficPremium;
    }

    public void setTrafficPremiumTotal(long trafficPremium2) {
        trafficPremiumTotal = trafficPremium2;
    }

    public void setTrafficFreeTotal(long trafficFree2) {
        trafficFreeTotal = trafficFree2;
    }

    public double getTrafficPremiumPercent() {
        if (trafficPremiumTotal == 0) return -1d;
        return (trafficPremium * 10000 / trafficPremiumTotal) / 100d;

    }

    public double getTrafficFreePercent() {
        if (trafficFreeTotal == 0) return -1d;
        return (trafficFree * 10000 / trafficFreeTotal) / 100d;

    }

}
