package jd.gui.swing.jdgui.components.premiumbar;

import java.util.HashSet;

import javax.swing.Icon;

import jd.plugins.Account;
import jd.plugins.AccountInfo;

import org.appwork.swing.components.tooltips.ExtTooltip;
import org.jdownloader.DomainInfo;

public class AccountServiceCollection extends ServiceCollection<Account> {

    /**
     * 
     */
    private static final long serialVersionUID   = -6958497120849521678L;
    private DomainInfo        domainInfo;
    private boolean           enabled;
    private HashSet<Account>  hashSet;

    private boolean           multi              = true;
    private long              lastValidTimeStamp = -1;

    public DomainInfo getDomainInfo() {
        return domainInfo;
    }

    public AccountServiceCollection(DomainInfo domainInfo) {
        this.domainInfo = domainInfo;
        enabled = false;
        hashSet = new HashSet<Account>();

    }

    public boolean isEnabled() {
        return enabled && org.jdownloader.settings.staticreferences.CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.getValue();
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
        lastValidTimeStamp = Math.max(acc.getLastValidTimestamp(), lastValidTimeStamp);
        return super.add(acc);
    }

    public boolean isMulti() {
        return multi;
    }

    @Override
    public Icon getIcon() {
        return domainInfo.getFavIcon();
    }

    @Override
    protected long getLastActiveTimestamp() {
        return lastValidTimeStamp;
    }

    @Override
    protected String getName() {
        return domainInfo.getTld();
    }

    @Override
    public ExtTooltip createTooltip(ServicePanel owner) {

        return new AccountTooltip(owner, this);

    }

}
