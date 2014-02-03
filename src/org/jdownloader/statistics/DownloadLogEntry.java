package org.jdownloader.statistics;

import jd.controlling.downloadcontroller.DownloadLinkCandidateResult.RESULT;

public class DownloadLogEntry extends AbstractLogEntry {
    DownloadLogEntry() {

    }

    private int chunks = 1;

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

    private long waittime = -1;

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

    private long pluginRuntime  = -1;
    private long captchaRuntime = -1;

    public long getCaptchaRuntime() {
        return captchaRuntime;
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

    private boolean proxy = false;

    public boolean isProxy() {
        return proxy;
    }

    public void setProxy(boolean proxy) {
        this.proxy = proxy;
    }
}
