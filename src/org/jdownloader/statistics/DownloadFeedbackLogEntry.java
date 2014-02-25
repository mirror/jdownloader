package org.jdownloader.statistics;

import java.util.ArrayList;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;

public class DownloadFeedbackLogEntry extends AbstractFeedbackLogEntry implements Storable {
    protected DownloadFeedbackLogEntry(/* storable */) {
        super();
    }

    @Override
    public String toString() {
        return JSonStorage.toString(this);
    }

    private ArrayList<Candidate> candidates = null;

    public ArrayList<Candidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(ArrayList<Candidate> candidates) {
        this.candidates = candidates;
    }

    public long getFilesize() {
        return filesize;
    }

    public void setFilesize(long filesize) {
        this.filesize = filesize;
    }

    private long   filesize = 0;

    private long   linkID   = -1;

    private String host;
    private int    counter;

    public String getHost() {
        return host;
    }

    public long getLinkID() {
        return linkID;
    }

    public void setLinkID(long id) {
        this.linkID = id;
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
