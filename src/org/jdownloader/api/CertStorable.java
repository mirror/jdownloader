package org.jdownloader.api;

import org.appwork.storage.Storable;

public class CertStorable implements Storable {

    private String privateKey = null;

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getCert() {
        return cert;
    }

    public void setCert(String cert) {
        this.cert = cert;
    }

    public String[] getSubjects() {
        return subjects;
    }

    public void setSubjects(String[] subjects) {
        this.subjects = subjects;
    }

    private String   publicKey = null;
    private String   cert      = null;
    private String[] subjects  = null;

    public CertStorable(/* Storable */) {
    }
}
