package org.jdownloader.api.cnl2;

import java.util.List;

import org.appwork.storage.Storable;

public class CnlQueryStorable implements Storable {
    private String urls;
    private String jk;
    private String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    private String       source;
    private String       orgSource;
    private String       referrer;
    private String       orgReferrer;
    private List<String> passwords;
    private String       comment;
    private String       packageName;
    private String       crypted;
    private String       dir;
    private boolean      permission;

    public boolean isPermission() {
        return permission;
    }

    public void setPermission(boolean permission) {
        this.permission = permission;
    }

    public String getUrls() {
        return urls;
    }

    public void setUrls(String urls) {
        this.urls = urls;
    }

    public String getJk() {
        return jk;
    }

    public void setJk(String jk) {
        this.jk = jk;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getOrgSource() {
        return orgSource;
    }

    public void setOrgSource(String orgSource) {
        this.orgSource = orgSource;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    public String getOrgReferrer() {
        return orgReferrer;
    }

    public void setOrgReferrer(String orgReferrer) {
        this.orgReferrer = orgReferrer;
    }

    public List<String> getPasswords() {
        return passwords;
    }

    public void setPasswords(List<String> passwords) {
        this.passwords = passwords;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getCrypted() {
        return crypted;
    }

    public void setCrypted(String crypted) {
        this.crypted = crypted;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }
}
