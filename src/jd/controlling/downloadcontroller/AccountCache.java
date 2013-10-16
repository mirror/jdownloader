package jd.controlling.downloadcontroller;

import java.util.ArrayList;
import java.util.Iterator;

import jd.controlling.downloadcontroller.AccountCache.CachedAccount;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

import org.appwork.utils.NullsafeAtomicReference;

public class AccountCache implements Iterable<CachedAccount> {

    public static enum ACCOUNTTYPE {
        /* DO NOT CHANGE ORDER HERE, we use compareTo which uses ordinal */
        ORIGINAL,
        MULTI,
        NONE
    }

    public static class CachedAccount {
        private final Account       account;
        private final ACCOUNTTYPE   type;
        private final PluginForHost plugin;
        private final int           hashCode;
        private final String        host;

        public String getHost() {
            return host;
        }

        protected CachedAccount(String host, Account account, ACCOUNTTYPE type, PluginForHost plugin) {
            this.account = account;
            this.type = type;
            this.plugin = plugin;
            this.host = host;
            StringBuilder sb = new StringBuilder();
            if (host != null) {
                sb.append("HOST").append(host);
            }
            if (account != null) {
                sb.append("ACC").append(account.hashCode());
            }
            if (type != null) {
                sb.append("TYPE").append(type.name());
            }
            if (plugin != null) {
                sb.append("PLUGIN").append(plugin.getLazyP().getHost());
            }
            hashCode = sb.toString().hashCode();
        }

        public final Account getAccount() {
            return account;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        public final ACCOUNTTYPE getType() {
            return type;
        }

        public final PluginForHost getPlugin() {
            return plugin;
        }

        public boolean hasCaptcha(DownloadLink link) {
            if (plugin == null) return false;
            return plugin.hasCaptcha(link, account);
        }

        public boolean canHandle(DownloadLink link) {
            if (plugin == null) return false;
            return plugin.canHandle(link, account) && plugin.enoughTrafficFor(link, account);
        }

        @Override
        public String toString() {
            return "Plugin:" + plugin.getHost() + "|Version:" + plugin.getVersion() + "|Type:" + type + "|Account:" + account;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof CachedAccount)) return false;
            if (obj == this) return true;
            CachedAccount other = (CachedAccount) obj;
            if (getType() != other.getType()) return false;
            if ((getAccount() == null && other.getAccount() != null) || (other.getAccount() == null && getAccount() != null)) return false;
            if ((other.getPlugin() == null && getPlugin() != null) || (other.getPlugin() != null && getPlugin() == null)) return false;
            if (getPlugin() != null && !getPlugin().getLazyP().equals(other.getPlugin().getLazyP())) return false;
            if (getAccount() != null && !getAccount().equals(other.getAccount())) return false;
            return true;
        }
    }

    protected final static AccountCache      NA = new AccountCache(null) {

                                                    public java.util.Iterator<CachedAccount> iterator() {
                                                        return new Iterator<AccountCache.CachedAccount>() {

                                                            @Override
                                                            public boolean hasNext() {
                                                                return false;
                                                            }

                                                            @Override
                                                            public CachedAccount next() {
                                                                return null;
                                                            }

                                                            @Override
                                                            public void remove() {
                                                            }

                                                        };

                                                    };
                                                };

    protected final ArrayList<CachedAccount> cache;

    protected AccountCache(ArrayList<CachedAccount> cache) {
        this.cache = cache;
    }

    @Override
    public Iterator<CachedAccount> iterator() {
        return new Iterator<AccountCache.CachedAccount>() {

            Iterator<CachedAccount>                it   = cache.iterator();
            NullsafeAtomicReference<CachedAccount> next = new NullsafeAtomicReference<AccountCache.CachedAccount>(null);

            @Override
            public void remove() {
            }

            @Override
            public CachedAccount next() {
                CachedAccount ret = next.getAndSet(null);
                if (ret != null) return ret;
                if (hasNext()) {
                    return next.getAndSet(null);
                } else
                    return null;
            }

            @Override
            public boolean hasNext() {
                if (next.get() != null) return true;
                while (it.hasNext()) {
                    CachedAccount iNext = it.next();
                    if (iNext.getAccount() != null) {
                        if (!iNext.getAccount().isEnabled()) continue;
                        if (!iNext.getAccount().isValid()) continue;
                        if (iNext.getAccount().isTempDisabled()) continue;
                    }
                    next.set(iNext);
                    break;
                }
                return next.get() != null;
            }
        };
    }
}
