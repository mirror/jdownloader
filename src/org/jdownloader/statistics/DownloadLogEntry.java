package org.jdownloader.statistics;

import jd.controlling.downloadcontroller.DownloadLinkCandidateResult.RESULT;

public class DownloadLogEntry extends AbstractLogEntry {
    DownloadLogEntry() {

    }

    private int    chunks  = 0;
    private String country = null;
    private String isp     = null;

    public String getIsp() {
        return isp;
    }

    private long timestamp = 0;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setIsp(String isp) {
        this.isp = isp;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public int getChunks() {
        return chunks;
    }

    private boolean resume = false;

    public boolean isResume() {
        return resume;
    }

    public void setResume(boolean resume) {
        this.resume = resume;
    }

    private boolean canceled = false;

    public boolean isCanceled() {
        return canceled;
    }

    private String os = "WINDOWS";

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public void setChunks(int chunks) {
        this.chunks = chunks;
    }

    private long waittime = 0;

    public long getWaittime() {
        return waittime;
    }

    private String host = null;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    private String account = null;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public void setWaittime(long waittime) {
        this.waittime = waittime;
    }

    private long pluginRuntime  = 0;
    private long captchaRuntime = 0;

    public long getCaptchaRuntime() {
        return captchaRuntime;
    }

    private String errorID = null;

    public String getErrorID() {
        return errorID;
    }

    public void setErrorID(String errorID) {
        this.errorID = errorID;
    }

    private int utcOffset = 0;

    public int getUtcOffset() {
        return utcOffset;
    }

    public void setUtcOffset(int utcOffset) {
        this.utcOffset = utcOffset;
    }

    private long revision = 0;

    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }

    public void setCaptchaRuntime(long captchaRuntime) {
        this.captchaRuntime = captchaRuntime;
    }

    public long getSpeed() {
        return speed;
    }

    public void setSpeed(long speed) {
        this.speed = speed;
    }

    public long getFilesize() {
        return filesize;
    }

    public void setFilesize(long filesize) {
        this.filesize = filesize;
    }

    public RESULT getResult() {
        return result;
    }

    public void setResult(RESULT result) {
        this.result = result;
    }

    private long   speed    = 0;
    private long   filesize = 0;
    private RESULT result   = null;

    public long getPluginRuntime() {
        return pluginRuntime;
    }

    public void setPluginRuntime(long pluginRuntime) {
        this.pluginRuntime = pluginRuntime;
    }

    private boolean proxy   = false;
    private int     counter = 1;

    public int getCounter() {
        return counter;
    }

    public boolean isProxy() {
        return proxy;
    }

    public void setProxy(boolean proxy) {
        this.proxy = proxy;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }
}
