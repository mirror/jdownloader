package org.jdownloader.statistics;

import jd.controlling.downloadcontroller.AccountCache.CachedAccount;

import org.appwork.storage.Storable;
import org.jdownloader.myjdownloader.client.json.AbstractJsonData;

public class Candidate extends AbstractJsonData implements Storable {

    public static final String ACCOUNT_MULTI_PREMIUM = "account.multi.premium";
    public static final String ACCOUNT_MULTI_FREE    = "account.multi.free";
    public static final String ACCOUNT_PREMIUM       = "account.premium";
    public static final String ACCOUNT_FREE          = "account.free";

    public static final String FREE                  = "free";
    private String             plugin;

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
    private String id;
    private String clazz;

    public Candidate(/* storable */) {

    }

    public Candidate(String pluginHoster, long revision, String type) {
        this.plugin = pluginHoster;
        this.revision = revision;
        this.type = type;
        this.id = this.plugin + "_" + this.revision + "_" + this.type;
    }

    public static Candidate create(CachedAccount account) {
        Candidate ret = new Candidate();
        ret.plugin = account.getAccount() == null ? account.getHost() : account.getPlugin().getHost();

        ret.revision = account.getPlugin().getVersion();

        try {
            ret.clazz = account.getPlugin().getClass().getName();
            if (ret.clazz.startsWith("jd.plugins.hoster.")) {
                ret.clazz = ret.clazz.substring("jd.plugins.hoster.".length());
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        switch (account.getType()) {
        case MULTI:
            // nopremium z.b. 4shared.com
            if (account.getAccount().getBooleanProperty("free", false) || account.getAccount().getBooleanProperty("nopremium", false)) {
                ret.type = ACCOUNT_MULTI_FREE;
            } else {
                ret.type = ACCOUNT_MULTI_PREMIUM;
            }
            break;

        case ORIGINAL:
            // nopremium z.b. 4shared.com
            if (account.getAccount().getBooleanProperty("free", false) || account.getAccount().getBooleanProperty("nopremium", false)) {
                ret.type = ACCOUNT_FREE;
            } else {
                ret.type = ACCOUNT_PREMIUM;
            }
            break;

        }
        ret.id = ret.plugin + "_" + ret.revision + "_" + ret.type;
        return ret;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public String toID() {

        return id;
    }
}
