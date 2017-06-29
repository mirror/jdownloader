package jd.controlling.downloadcontroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import jd.controlling.downloadcontroller.AccountCache.CachedAccount;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.hosterrule.AccountGroup;
import org.jdownloader.controlling.hosterrule.AccountGroup.Rules;

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
            }
            return false;
        }

        public static boolean samePlugin(CachedAccount x, CachedAccount y) {
            if (x != null && y != null) {
                final PluginForHost xx = x.getPlugin();
                final PluginForHost yy = y.getPlugin();
                return xx != null && yy != null && xx.equals(yy);
            }
            return false;
        }

        public boolean equals(CachedAccount cachedAccount) {
            if (cachedAccount == null) {
                return false;
            }
            if (cachedAccount == this) {
                return true;
            }
            if (getType() != cachedAccount.getType()) {
                return false;
            }
            if (!sameAccount(this, cachedAccount) || !samePlugin(this, cachedAccount)) {
                return false;
            }
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
                    throw new UnsupportedOperationException();
                }
            };
        };
    };
    protected final ArrayList<CachedAccount> cache;
    protected final ArrayList<Rules>         rules;
    protected final boolean                  customized;

    public AccountCache(ArrayList<CachedAccount> cache) {
        this(cache, null);
    }

    public boolean isCustomizedCache() {
        return customized;
    }

    public AccountCache(ArrayList<CachedAccount> cache, ArrayList<AccountGroup.Rules> rules) {
        this.cache = cache;
        if (rules != null) {
            if (rules != null && rules.size() < cache.size()) {
                throw new IllegalArgumentException("rules must have at least <= length of cache!");
            }
            customized = true;
            boolean nonOrder = false;
            AccountGroup.Rules lastRule = null;
            for (AccountGroup.Rules rule : rules) {
                /* validate rules */
                if (rule == null) {
                    lastRule = null;
                    continue;
                } else {
                    if (lastRule == null) {
                        lastRule = rule;
                    } else {
                        if (!lastRule.equals(rule)) {
                            throw new IllegalArgumentException("different rules within same rulegroup?!");
                        }
                        nonOrder = true;
                        break;
                    }
                }
            }
            if (nonOrder == false) {
                rules = null;
            }
        } else {
            customized = false;
        }
        this.rules = rules;
    }

    protected Iterator<CachedAccount> getRuleAwareIterator() {
        if (rules == null) {
            return cache.iterator();
        }
        ArrayList<CachedAccount> orderedCache = new ArrayList<AccountCache.CachedAccount>(cache);
        int startRandom = -1;
        int cacheIndex = -1;
        for (int ruleIndex = 0; ruleIndex < rules.size(); ruleIndex++) {
            final Rules rule = rules.get(ruleIndex);
            if (rule == null) {
                if (startRandom >= 0) {
                    Collections.shuffle(orderedCache.subList(startRandom, cacheIndex + 1));
                    startRandom = -1;
                }
                continue;
            } else {
                cacheIndex++;
            }
            switch (rule) {
            case ORDER:
                startRandom = -1;
                break;
            case BALANCED:
                // unsupported
                startRandom = -1;
                break;
            case RANDOM:
                if (startRandom < 0) {
                    startRandom = cacheIndex;
                }
                break;
            default:
                startRandom = -1;
                break;
            }
        }
        if (startRandom >= 0) {
            Collections.shuffle(orderedCache.subList(startRandom, orderedCache.size()));
        }
        return orderedCache.iterator();
    }

    @Override
    public Iterator<CachedAccount> iterator() {
        return new Iterator<AccountCache.CachedAccount>() {
            Iterator<CachedAccount>                it   = getRuleAwareIterator();
            NullsafeAtomicReference<CachedAccount> next = new NullsafeAtomicReference<AccountCache.CachedAccount>(null);

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public CachedAccount next() {
                CachedAccount ret = next.getAndSet(null);
                if (ret != null) {
                    return ret;
                }
                if (hasNext()) {
                    return next.getAndSet(null);
                } else {
                    return null;
                }
            }

            @Override
            public boolean hasNext() {
                if (next.get() != null) {
                    return true;
                }
                while (it.hasNext()) {
                    CachedAccount iNext = it.next();
                    if (iNext.getAccount() != null) {
                        if (iNext.getAccount().getAccountController() == null) {
                            continue;
                        }
                        if (!iNext.getAccount().isEnabled()) {
                            continue;
                        }
                        if (!iNext.getAccount().isValid()) {
                            continue;
                        }
                    }
                    next.set(iNext);
                    break;
                }
                return next.get() != null;
            }
        };
    }
}
