package org.jdownloader.api.accounts;

import jd.plugins.Account;

import org.appwork.storage.Storable;
import org.jdownloader.myjdownloader.client.json.JsonMap;

@Deprecated
public class AccountAPIStorable implements Storable {

    private Account acc;
    private JsonMap infoMap = null;

    public long getUUID() {
        return acc.getId().getID();
    }

    public String getHostname() {
        return acc.getHoster();
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
