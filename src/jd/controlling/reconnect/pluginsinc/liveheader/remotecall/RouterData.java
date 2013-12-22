package jd.controlling.reconnect.pluginsinc.liveheader.remotecall;

import java.util.HashMap;

import org.appwork.storage.Storable;
import org.appwork.utils.Hash;

public class RouterData implements Storable {

    private String routerIP;
    private String script;

    private String routerName;
    private int    priorityIndicator;
    private String mac;
    private long   avgOfD;

    public long getAvgOfD() {
        return avgOfD;
    }

    public void setAvgOfD(long averageOfflineDuration) {
        this.avgOfD = averageOfflineDuration;
    }

    public long getAvgScD() {
        return avgScD;
    }

    public void setAvgScD(long averageSuccessDuration) {
        this.avgScD = averageSuccessDuration;
    }

    private long   avgScD;
    private String manufactor;
    private String scriptID;
    private long   avgScDDev;

    public long getAvgScDDev() {
        return avgScDDev;
    }

    public void setAvgScDDev(long avgScDDev) {
        this.avgScDDev = avgScDDev;
    }

    public long getAvgOfDDev() {
        return avgOfDDev;
    }

    public void setAvgOfDDev(long avgFaDDev) {
        this.avgOfDDev = avgFaDDev;
    }

    private long avgOfDDev;

    public String getScriptID() {
        if (scriptID != null && scriptID.length() == 32) return scriptID;

        final StringBuilder sb = new StringBuilder();

        sb.append(this.getIsp());
        sb.append("\r\n");
        sb.append(this.getTitle());
        sb.append("\r\n");
        sb.append(this.getRouterName());
        sb.append("\r\n");
        sb.append(this.getScript());
        sb.append("\r\n");
        sb.append(this.getResponseCode());
        sb.append("\r\n");
        sb.append(this.getTagFootprint());
        sb.append("\r\n");
        sb.append(this.getMac());
        sb.append("\r\n");
        sb.append(this.getFavIconHash());
        sb.append("\r\n");
        sb.append(this.getTitle());

        sb.append("\r\n");
        sb.append(this.getSslResponseCode());

        sb.append("\r\n");
        sb.append(this.getSslTagFootprint());

        sb.append("\r\n");
        sb.append(this.getSslFavIconHash());
        sb.append("\r\n");
        addHeader(sb, "content-length");
        addHeader(sb, "content-encoding");
        addHeader(sb, "content-type");
        addHeader(sb, "server");
        addHeader(sb, "mime-version");

        return Hash.getMD5(sb.toString());
    }

    private void addHeader(final StringBuilder sb, final String string) {
        sb.append(string);
        sb.append("=");
        sb.append(getResponseHeaders() != null ? this.getResponseHeaders().get(string) : null);
        sb.append("\r\n");

        sb.append(string);
        sb.append("=");
        sb.append(this.getSslResponseHeaders() != null ? this.getSslResponseHeaders().get(string) : null);
        sb.append("\r\n");

    }

    public void setScriptID(String scriptID) {
        this.scriptID = scriptID;
    }

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

    private int testStart;

    public int getTestStart() {
        return testStart;
    }

    public void setTestStart(int testStart) {
        this.testStart = testStart;
    }

    public int getTestEnd() {
        return testEnd;
    }

    public void setTestEnd(int testEnd) {
        this.testEnd = testEnd;
    }

    private int                     testEnd;
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
    private String isp;

    public RouterData() {
        this.createTime = System.currentTimeMillis();
    }

    public RouterData(String scriptID) {
        this.scriptID = scriptID;
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

    public String getIsp() {
        return isp;
    }

    public void setIsp(String isp) {
        this.isp = isp;
    }

}
