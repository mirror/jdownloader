package org.jdownloader.plugins.components.realDebridCom.api.json;

import org.appwork.storage.Storable;

public class ClientSecret implements Storable {
    public static final org.appwork.storage.TypeRef<ClientSecret> TYPE = new org.appwork.storage.TypeRef<ClientSecret>(ClientSecret.class) {
                                                                       };
    String                                                        client_id;

    String                                                        client_secret;

    public ClientSecret(/* Storable */) {
    }

    public String getClient_id() {
        return client_id;
    }

    public String getClient_secret() {
        return client_secret;
    }

    public void setClient_id(String client_id) {
        this.client_id = client_id;
    }

    public void setClient_secret(String client_secret) {
        this.client_secret = client_secret;
    }
}