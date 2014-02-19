package org.jdownloader.statistics;

import jd.controlling.downloadcontroller.AccountCache.CachedAccount;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.jdownloader.myjdownloader.client.json.AbstractJsonData;

public class Candidate extends AbstractJsonData implements Storable {

    public static final String  ACCOUNT_MULTI_PREMIUM = "account.multi.premium";
    public static final String  ACCOUNT_MULTI_FREE    = "account.multi.free";
    public static final String  ACCOUNT_PREMIUM       = "account.premium";
    public static final String  ACCOUNT_FREE          = "account.free";

    private static final String FREE                  = "free";
    private String              plugin;

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    private long   revision;
    private String type = FREE;

    public Candidate(/* storable */) {

    }

    public Candidate(String pluginHoster, long revision, String type) {
        this.plugin = pluginHoster;
        this.revision = revision;
        this.type = type;
    }

    public static Candidate create(CachedAccount account) {
        Candidate ret = new Candidate();
        ret.plugin = account.getAccount() == null ? account.getHost() : account.getPlugin().getHost();
        ret.revision = account.getPlugin().getVersion();
        switch (account.getType()) {
        case MULTI:

            if (account.getAccount().getBooleanProperty("free", false)) {
                ret.type = ACCOUNT_MULTI_FREE;
            } else {
                ret.type = ACCOUNT_MULTI_PREMIUM;
            }
            break;

        case ORIGINAL:
            if (account.getAccount().getBooleanProperty("free", false)) {
                ret.type = ACCOUNT_FREE;
            } else {
                ret.type = ACCOUNT_PREMIUM;
            }
            break;

        }

        return ret;
    }

    public String toID() {
        return JSonStorage.serializeToJson(this);
    }
}
