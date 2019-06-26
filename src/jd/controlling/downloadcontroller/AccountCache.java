package jd.controlling.downloadcontroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jd.controlling.downloadcontroller.AccountCache.CachedAccount;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.hosterrule.AccountGroup.Rules;
import org.jdownloader.controlling.hosterrule.CachedAccountGroup;

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
        private final String        host;

        public String getHost() {
            return host;
        }

        public CachedAccount(String host, Account account, PluginForHost plugin) {
            this.account = account;
            if (account == null) {
                this.type = ACCOUNTTYPE.NONE;
            } else {
                if (StringUtils.equalsIgnoreCase(host, account.getHoster())) {
                    this.type = ACCOUNTTYPE.ORIGINAL;
                } else {
                    this.type = ACCOUNTTYPE.MULTI;
                }
            }
            this.plugin = plugin;
            this.host = host;
        }

        public final Account getAccount() {
            return account;
        }

        public final ACCOUNTTYPE getType() {
            return type;
        }

        public final PluginForHost getPlugin() {
            return plugin;
        }

        public boolean hasCaptcha(DownloadLink link) {
            return plugin != null && Boolean.TRUE.equals(plugin.expectCaptcha(link, account));
        }

        public boolean canHandle(DownloadLink link) throws Exception {
            if (plugin == null) {
                return false;
            }
            final PluginForHost linkPlugin = link.getDefaultPlugin();
            boolean canHandle = linkPlugin == null ? true : linkPlugin.allowHandle(link, plugin);
            if (canHandle) {
                canHandle = plugin.canHandle(link, account) && plugin.enoughTrafficFor(link, account);
            }
            if (canHandle && ACCOUNTTYPE.MULTI.equals(getType()) && getAccount() != null) {
                final AccountInfo ai = getAccount().getAccountInfo();
                /* verify again because plugins can modify list on runtime */
                if (ai != null) {
                    final List<String> supported = ai.getMultiHostSupport();
                    if (supported != null) {
                        canHandle = supported.contains(link.getHost());
                    }
                }
            }
            return canHandle;
        }

        @Override
        public String toString() {
            if (plugin == null) {
                return "Plugin:none|Type:" + type + "|Account:" + account;
            } else {
                return "Plugin:" + plugin.getHost() + "|Version:" + plugin.getVersion() + "|Type:" + type + "|Account:" + account;
            }
        }

        public static boolean sameAccount(CachedAccount x, CachedAccount y) {
            if (x != null && y != null) {
                final Account xx = x.getAccount();
                final Account yy = y.getAccount();
                return xx != null && yy != null && xx.equals(yy);
            } else {
                return false;
            }
        }

        public static boolean samePlugin(CachedAccount x, CachedAccount y) {
            if (x != null && y != null) {
                final PluginForHost xx = x.getPlugin();
                final PluginForHost yy = y.getPlugin();
                return xx != null && yy != null && xx.equals(yy);
            } else {
                return false;
            }
        }

        public boolean equals(CachedAccount cachedAccount) {
            if (cachedAccount == null) {
                return false;
            } else if (cachedAccount == this) {
                return true;
            } else if (getType() != cachedAccount.getType()) {
                return false;
            } else if (!sameAccount(this, cachedAccount) || !samePlugin(this, cachedAccount)) {
                return false;
            } else {
                return true;
            }
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
                    throw new UnsupportedOperationException();
                }
            };
        };
    };
    protected final List<CachedAccountGroup> cache;

    public boolean isCustomizedCache() {
        return false;
    }

    public AccountCache(List<CachedAccountGroup> cache) {
        this.cache = cache;
    }

    protected Iterator<CachedAccount> getRuleAwareIterator() {
        if (cache == null || cache.size() == 0) {
            return new ArrayList<CachedAccount>(0).iterator();
        } else if (cache.size() == 1 && Rules.ORDER.equals(cache.get(0).getRule())) {
            return cache.get(0).iterator();
        } else {
            final List<CachedAccount> ret = new ArrayList<AccountCache.CachedAccount>();
            for (final CachedAccountGroup cachedAccountGroup : cache) {
                switch (cachedAccountGroup.getRule()) {
                case DISABLED:
                    continue;
                case BALANCED:
                    // TODO
                    ret.addAll(cachedAccountGroup);
                    break;
                case RANDOM:
                    synchronized (cachedAccountGroup) {
                        Collections.shuffle(cachedAccountGroup);
                        ret.addAll(cachedAccountGroup);
                    }
                    break;
                default:
                case ORDER:
                    ret.addAll(cachedAccountGroup);
                    break;
                }
            }
            return ret.iterator();
        }
    }

    @Override
    public Iterator<CachedAccount> iterator() {
        return new Iterator<AccountCache.CachedAccount>() {
            final Iterator<CachedAccount>        it   = getRuleAwareIterator();
            final AtomicReference<CachedAccount> next = new AtomicReference<AccountCache.CachedAccount>(null);

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public CachedAccount next() {
                final CachedAccount ret = next.getAndSet(null);
                if (ret != null) {
                    return ret;
                } else if (hasNext()) {
                    return next.getAndSet(null);
                } else {
                    return null;
                }
            }

            @Override
            public boolean hasNext() {
                if (next.get() != null) {
                    return true;
                } else {
                    while (it.hasNext()) {
                        final CachedAccount iNext = it.next();
                        if (iNext.getAccount() != null) {
                            if (iNext.getAccount().getAccountController() == null) {
                                continue;
                            } else if (!iNext.getAccount().isEnabled()) {
                                continue;
                            } else if (!iNext.getAccount().isValid()) {
                                continue;
                            }
                        }
                        next.set(iNext);
                        break;
                    }
                    return next.get() != null;
                }
            }
        };
    }
}
