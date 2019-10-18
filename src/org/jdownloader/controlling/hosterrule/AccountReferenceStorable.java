package org.jdownloader.controlling.hosterrule;

import java.util.List;

import jd.plugins.Account;

import org.appwork.storage.Storable;
import org.appwork.utils.StringUtils;
import org.appwork.utils.UniqueAlltimeID;

public class AccountReferenceStorable implements Storable {
    private long   id    = -1;
    private String ref   = null;
    private long   refId = UniqueAlltimeID.next();

    public long getRefId() {
        return refId;
    }

    public void setRefId(long refId) {
        this.refId = refId;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    private AccountReferenceStorable(/* storable */) {
    }

    public AccountReferenceStorable(final AccountReference acc) {
        this.id = acc.getID();
        this.ref = acc.getRef(getRefId());
        this.enabled = acc.isEnabled();
    }

    public AccountReference restore(String hoster, List<Account> availableAccounts) {
        AccountReference ret = null;
        if (getId() == FreeAccountReference.FREE_ID) {
            ret = new FreeAccountReference(hoster);
        } else if (availableAccounts != null) {
            for (final Account acc : availableAccounts) {
                if (acc.getId().getID() == getId()) {
                    ret = new AccountReference(acc);
                    break;
                }
            }
            if (ret == null && getRef() != null) {
                for (final Account acc : availableAccounts) {
                    final AccountReference accountReference = new AccountReference(acc);
                    if (StringUtils.equals(getRef(), accountReference.getRef(getRefId()))) {
                        ret = accountReference;
                        break;
                    }
                }
            }
        }
        if (ret != null) {
            ret.setEnabled(isEnabled());
            return ret;
        } else {
            return null;
        }
    }
}
