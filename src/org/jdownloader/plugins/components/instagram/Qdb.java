package org.jdownloader.plugins.components.instagram;

public class Qdb {
    public static enum QUERY {
        HASHTAG,
        STORY,
        USER,
        USER_SAVED
    }

    public Qdb() {
    }

    private String queryHash;
    private String fbAppId;

    public final String getQueryHash() {
        return queryHash;
    }

    public final String getFbAppId() {
        return fbAppId;
    }

    public final void setQueryHash(String queryHash) {
        this.queryHash = queryHash;
    }

    public final void setFbAppId(String fbAppId) {
        this.fbAppId = fbAppId;
    }
}