package org.jdownloader.api.accounts;

import jd.plugins.Account;

import org.appwork.storage.Storable;
import org.jdownloader.myjdownloader.client.json.JsonMap;

@Deprecated
public class AccountAPIStorable implements Storable {

    private Account acc;
    private JsonMap infoMap = null;

    public long getUUID() {
        Account lacc = acc;
        if (lacc != null) return lacc.getId().getID();
        return 0;
    }

    public String getHostname() {
        Account lacc = acc;
        if (lacc != null) return lacc.getHoster();
        return null;
    }

    public JsonMap getInfoMap() {
        return infoMap;
    }

    public void setInfoMap(JsonMap infoMap) {
        this.infoMap = infoMap;
    }

    @SuppressWarnings("unused")
    private AccountAPIStorable(/* Storable */) {
    }

    public AccountAPIStorable(Account acc) {
        this.acc = acc;
    }

}
