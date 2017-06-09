package jd.plugins.components;

import java.util.HashMap;

import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

/**
 * Instead of duplication we create a class
 *
 * This class will handle disabling of a host at 3 levels 1) multihoster wide, 2) AccountType 3) Account
 *
 * @author raztoki
 *
 */
public class MultiHosterManagement {

    private String getHost;

    public MultiHosterManagement(final String getHost) {
        this.getHost = getHost;
    }

    private final HashMap<Object, HashMap<String, UnavailableHost>> db = new HashMap<Object, HashMap<String, UnavailableHost>>();

    public void putError(final Object account, final DownloadLink dl, final Long timeout, final String reason) throws PluginException {
        synchronized (db) {
            // null(multihosterwide) && AccountType && Account
            final UnavailableHost nue = new UnavailableHost(System.currentTimeMillis() + timeout, reason);
            HashMap<String, UnavailableHost> unavailableMap = db.get(account);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, UnavailableHost>();
                db.put(account, unavailableMap);
            }
            unavailableMap.put(dl.getHost(), nue);
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    /**
     * intended purpose is predownload. CanHandle can't deal with exceptions at this time. Later to be called within canHandle
     *
     * @param account
     * @param dl
     * @throws PluginException
     */
    public void runCheck(final Account account, final DownloadLink dl) throws PluginException {
        synchronized (db) {
            // check for null(multihosterwide) first, AccountTypes(specific to this account type) second, and Account (specific to this
            // account) last!
            final Object[] acc = new Object[] { null, account.getType(), account };
            for (final Object ob : acc) {
                final HashMap<String, UnavailableHost> unavailableMap = db.get(ob);
                final UnavailableHost nue = unavailableMap != null ? (UnavailableHost) unavailableMap.get(dl.getHost()) : null;
                if (nue != null) {
                    final Long lastUnavailable = nue.getErrorTimeout();
                    final String errorReason = nue.getErrorReason();
                    if (lastUnavailable == null) {
                        // never can download from
                        throw new PluginException(LinkStatus.ERROR_FATAL, "Not possible to download from " + dl.getHost());
                    } else if (System.currentTimeMillis() < lastUnavailable) {
                        final long wait = lastUnavailable - System.currentTimeMillis();
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Temporarily unavailable by this MultiHoster Provider: " + errorReason != null ? errorReason : "via " + getHost, wait);
                    } else {
                        unavailableMap.remove(dl.getHost());
                        if (unavailableMap.size() == 0) {
                            db.remove(acc);
                        }
                    }
                }
            }
        }
    }

}
