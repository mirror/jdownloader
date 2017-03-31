package org.jdownloader.statistics;

import org.appwork.storage.JSonStorage;

public class DownloadLogEntry extends AbstractLogEntry {
    public DownloadLogEntry() {
    }

    @Override
    public String toString() {
        return JSonStorage.toString(this);
    }

    private int chunks = 0;

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

    private Candidate candidate = null;

    public Candidate getCandidate() {
        return candidate;
    }

    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
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

    public DownloadResult getResult() {
        return result;
    }

    public void setResult(DownloadResult result) {
        this.result = result;
    }

    private long           speed    = 0;
    private long           filesize = 0;
    private DownloadResult result   = null;

    public long getPluginRuntime() {
        return pluginRuntime;
    }

    public void setPluginRuntime(long pluginRuntime) {
        this.pluginRuntime = pluginRuntime;
    }

    private boolean proxy   = false;
    private int     counter = 1;
    private long    linkID  = -1;
    private String  host;
    private int     revU;

    public int getRevU() {
        return revU;
    }

    public void setRevU(int revU) {
        this.revU = revU;
    }

    public int getRev() {
        return rev;
    }

    public void setRev(int rev) {
        this.rev = rev;
    }

    private int rev;

    public long getLinkID() {
        return linkID;
    }

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

    public void setLinkID(long id) {
        this.linkID = id;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }
}
