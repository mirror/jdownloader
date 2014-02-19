package org.jdownloader.statistics;

import java.util.ArrayList;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;

public class DownloadFeedbackLogEntry extends AbstractFeedbackLogEntry implements Storable {
    protected DownloadFeedbackLogEntry(/* storable */) {
        super();
    }

    protected DownloadFeedbackLogEntry(boolean b) {

        super(b);
    }

    @Override
    public String toString() {
        return JSonStorage.toString(this);
    }

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

    private long sessionStart = 0;

    public long getSessionStart() {
        return sessionStart;
    }

    public void setSessionStart(long sessionStart) {
        this.sessionStart = sessionStart;
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

    private String os = "WINDOWS";

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    private ArrayList<Candidate> candidates = null;

    public ArrayList<Candidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(ArrayList<Candidate> candidates) {
        this.candidates = candidates;
    }

    private int utcOffset = 0;

    public int getUtcOffset() {
        return utcOffset;
    }

    public void setUtcOffset(int utcOffset) {
        this.utcOffset = utcOffset;
    }

    public long getFilesize() {
        return filesize;
    }

    public void setFilesize(long filesize) {
        this.filesize = filesize;
    }

    private long   filesize = 0;

    private long   linkID   = -1;
    private long   buildTime;
    private String host;
    private int    counter;

    public String getHost() {
        return host;
    }

    public long getBuildTime() {
        return buildTime;
    }

    public long getLinkID() {
        return linkID;
    }

    public void setLinkID(long id) {
        this.linkID = id;
    }

    public void setBuildTime(long parseLong) {
        this.buildTime = parseLong;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public int getCounter() {
        return counter;
    }
}
