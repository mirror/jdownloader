package org.jdownloader.api.downloads;

import jd.plugins.DownloadLink;

import org.appwork.storage.Storable;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class DownloadLinkStorable implements Storable {

    public long getId() {
        if (link == null) return 0;
        return link.getUniqueID().getID();
    }

    public void setId(long id) {
    }

    public String getName() {
        if (link == null) return null;
        return link.getName();
    }

    public void setName(String name) {
    }

    public String getComment() {
        if (link == null) return null;
        return link.getSourcePluginComment();
    }

    public void setComment(String comment) {
    }

    public String getHost() {
        if (link == null) return null;
        return link.getHost();
    }

    public void setHost(String hoster) {
    }

    public String getOnlinestatus() {
        if (link == null) return null;
        return "";
    }

    public void setOnlinestatus(String onlinestatus) {
    }

    public long getSize() {
        if (link == null) return -1l;
        return link.getDownloadSize();
    }

    public void setSize(long size) {
    }

    public long getDone() {
        if (link == null) return -1l;
        return link.getFinishedDate();
    }

    public void setDone(long done) {
    }

    public boolean isEnabled() {
        if (link == null) return true;
        return link.isEnabled();
    }

    public void setEnabled(boolean enabled) {
    }

    public long getSpeed() {
        if (link == null) return 0;
        return 0;
    }

    public void setSpeed(long speed) {
    }

    public long getAdded() {
        if (link == null) return -1l;
        return link.getCreated();
    }

    public void setAdded(long added) {
    }

    public long getFinished() {
        if (link == null) return -1l;
        return link.getFinishedDate();
    }

    public void setFinished(long finished) {
    }

    public int getPriority() {
        if (link == null) return 0;
        return link.getPriority();
    }

    public void setPriority(int priority) {
    }

    public int getChunks() {
        if (link == null) return 0;
        return 0;
    }

    public void setChunks(int chunks) {
    }

    public String getBrowserurl() {
        if (link == null) return null;
        return link.getBrowserUrl();
    }

    public void setBrowserurl(String browserurl) {
    }

    private DownloadLink link;

    private DownloadLinkStorable() {
        this.link = null;
    }

    public DownloadLinkStorable(DownloadLink link) {
        this.link = link;
    }
}
