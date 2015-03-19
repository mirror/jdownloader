package jd.controlling.reconnect.pluginsinc.liveheader.recoll;

import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;

public class AddRouterResponse implements Storable {
    public static final TypeRef<AddRouterResponse> TYPE_REF = new TypeRef<AddRouterResponse>() {
                                                            };

    public AddRouterResponse(/* Storable */) {
    }

    private boolean dupe = false;

    public boolean isDupe() {
        return dupe;
    }

    public void setDupe(boolean dupe) {
        this.dupe = dupe;
    }

    public String getScriptID() {
        return scriptID;
    }

    public void setScriptID(String scriptID) {
        this.scriptID = scriptID;
    }

    private String scriptID = null;
}
