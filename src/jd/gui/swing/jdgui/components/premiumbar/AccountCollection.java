package jd.gui.swing.jdgui.components.premiumbar;

import java.util.ArrayList;
import java.util.HashSet;

import jd.plugins.Account;
import jd.plugins.AccountInfo;

import org.jdownloader.DomainInfo;

public class AccountCollection extends ArrayList<Account> {

    private DomainInfo       domainInfo;
    private boolean          enabled;
    private HashSet<Account> hashSet;

    private boolean          multi = true;

    public DomainInfo getDomainInfo() {
        return domainInfo;
    }

    public AccountCollection(DomainInfo domainInfo) {
        this.domainInfo = domainInfo;
        enabled = false;
        hashSet = new HashSet<Account>();

    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean add(Account acc) {
        if (!hashSet.add(acc)) return false;
        if (!(acc.isMulti() && acc.getHoster().equalsIgnoreCase(domainInfo.getTld()))) {
            multi = false;
        }
        AccountInfo ai = acc.getAccountInfo();
        if (acc.isEnabled() && acc.isValid() && !(ai != null && ai.isExpired())) {

            enabled = true;
        }
        return super.add(acc);
    }

    public boolean isMulti() {
        return multi;
    }

}
