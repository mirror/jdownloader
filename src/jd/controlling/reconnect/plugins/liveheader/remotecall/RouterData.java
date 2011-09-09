package jd.controlling.reconnect.plugins.liveheader.remotecall;

import java.util.HashMap;

import org.appwork.storage.Storable;

public class RouterData implements Storable {

    private String                  routerIP;
    private String                  script;

    private String                  routerName;
    private int                     priorityIndicator;
    private String                  mac;

    private String                  manufactor;

    private int                     responseCode;
    private HashMap<String, String> responseHeaders;
    private String                  title;
    private int                     pTagsCount;

    private int                     frameTagCount;

    private String                  favIconHash;

    private String                  exception;
    private int                     commitDupe;

    public int getCommitDupe() {
        return commitDupe;
    }

    public void setCommitDupe(int commitDupe) {
        this.commitDupe = commitDupe;
    }

    public int getSuccess() {
        return success;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    private int                     success;
    private int                     failed;
    private String                  firmware;
    private String                  sslException;
    private String                  sslFavIconHash;
    private int                     sslFrameTagCount;
    private int                     sslPTagsCount;
    private String                  sslResponse;

    private int                     sslResponseCode;

    private long                    createTime;

    private HashMap<String, String> sslResponseHeaders;
    private String                  sslTitle;
    private String                  response;
    private String                  tagFootprint;

    public String getTagFootprint() {
        return tagFootprint;
    }

    public void setTagFootprint(String tagFootprint) {
        this.tagFootprint = tagFootprint;
    }

    public String getSslTagFootprint() {
        return sslTagFootprint;
    }

    public void setSslTagFootprint(String sslTagFootprint) {
        this.sslTagFootprint = sslTagFootprint;
    }

    private String sslTagFootprint;

    private String routerHost;

    public RouterData() {
        this.createTime = System.currentTimeMillis();
    }

    public long getCreateTime() {
        return this.createTime;
    }

    public String getException() {
        return this.exception;
    }

    public String getFavIconHash() {
        return this.favIconHash;
    }

    public String getFirmware() {
        return this.firmware;
    }

    public int getFrameTagCount() {
        return this.frameTagCount;
    }

    public String getMac() {
        return this.mac;
    }

    public String getManufactor() {
        return this.manufactor;
    }

    public int getpTagsCount() {
        return this.pTagsCount;
    }

    public String getResponse() {
        return this.response;
    }

    public int getResponseCode() {
        return this.responseCode;
    }

    public HashMap<String, String> getResponseHeaders() {
        return this.responseHeaders;
    }

    public String getRouterHost() {
        return this.routerHost;
    }

    public String getRouterIP() {
        return this.routerIP;
    }

    public String getRouterName() {
        return this.routerName;
    }

    public String getScript() {
        return this.script;
    }

    public String getSslException() {
        return this.sslException;
    }

    public String getSslFavIconHash() {
        return this.sslFavIconHash;
    }

    public int getSslFrameTagCount() {
        return this.sslFrameTagCount;
    }

    public int getSslPTagsCount() {
        return this.sslPTagsCount;
    }

    public String getSslResponse() {
        return this.sslResponse;
    }

    public int getSslResponseCode() {
        return this.sslResponseCode;
    }

    public HashMap<String, String> getSslResponseHeaders() {
        return this.sslResponseHeaders;
    }

    public String getSslTitle() {
        return this.sslTitle;
    }

    public int getPriorityIndicator() {
        return this.priorityIndicator;
    }

    public String getTitle() {
        return this.title;
    }

    public void setCreateTime(final long createTime) {
        this.createTime = createTime;
    }

    public void setException(final String exception) {
        this.exception = exception;
    }

    public void setFavIconHash(final String favIconHash) {
        this.favIconHash = favIconHash;
    }

    public void setFirmware(final String firmware) {
        this.firmware = replace(firmware);
    }

    private String replace(String str) {
        if (str == null) return null;
        return str.replace("&nbsp;", " ");
    }

    public void setFrameTagCount(final int frameTagCount) {
        this.frameTagCount = frameTagCount;
    }

    public void setMac(final String mac) {
        this.mac = mac;
    }

    public void setManufactor(final String manufactor) {
        this.manufactor = replace(manufactor);
    }

    public void setpTagsCount(final int pTagsCount) {
        this.pTagsCount = pTagsCount;
    }

    public void setResponse(final String response) {
        this.response = response;
    }

    public void setResponseCode(final int responseCode) {
        this.responseCode = responseCode;
    }

    public void setResponseHeaders(final HashMap<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public void setRouterHost(final String gatewayAdressHost) {
        this.routerHost = gatewayAdressHost;
    }

    public void setRouterIP(final String routerIP) {
        this.routerIP = routerIP;
    }

    public void setRouterName(final String routerName) {
        this.routerName = replace(routerName);
    }

    public void setScript(final String script) {
        this.script = script;
    }

    public void setSslException(final String sslException) {
        this.sslException = sslException;
    }

    public void setSslFavIconHash(final String sslFavIconHash) {
        this.sslFavIconHash = sslFavIconHash;
    }

    public void setSslFrameTagCount(final int sslFrameTagCount) {
        this.sslFrameTagCount = sslFrameTagCount;
    }

    public void setSslPTagsCount(final int sslPTagsCount) {
        this.sslPTagsCount = sslPTagsCount;
    }

    public void setSslResponse(final String sslResponse) {
        this.sslResponse = sslResponse;
    }

    public void setSslResponseCode(final int sslResponseCode) {
        this.sslResponseCode = sslResponseCode;
    }

    public void setSslResponseHeaders(final HashMap<String, String> sslResponseHeaders) {
        this.sslResponseHeaders = sslResponseHeaders;
    }

    public void setSslTitle(final String sslTitle) {
        this.sslTitle = replace(sslTitle);
    }

    public void setPriorityIndicator(final int successCount) {
        this.priorityIndicator = successCount;
    }

    public void setTitle(final String title) {
        this.title = replace(title);
    }

}
