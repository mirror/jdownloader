package jd.gui.swing.jdgui.components.premiumbar;

import java.util.HashSet;

import javax.swing.Icon;
import javax.swing.JComponent;

import jd.plugins.Account;
import jd.plugins.AccountInfo;

import org.appwork.swing.components.ExtMergedIcon;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;

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
    private int               invalid;
    private boolean           inuse;

    public DomainInfo getDomainInfo() {
        return domainInfo;
    }

    protected boolean isInUse() {
        return inuse;
    }

    public AccountServiceCollection(DomainInfo domainInfo) {
        this.domainInfo = domainInfo;
        enabled = false;
        inuse = false;
        invalid = 0;
        hashSet = new HashSet<Account>();

    }

    public JComponent createIconComponent(ServicePanel servicePanel) {

        return new TinyProgressBar(servicePanel, this);
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
        if (acc.isEnabled()) {
            enabled = true;
        }
        if (!acc.isValid()) {
            invalid++;
        } else if (acc.isEnabled()) {
            inuse = true;

        }

        lastValidTimeStamp = Math.max(acc.getLastValidTimestamp(), lastValidTimeStamp);
        return super.add(acc);
    }

    public boolean isMulti() {
        return multi;
    }

    @Override
    public Icon getIcon() {
        if (!inuse && invalid > 0) { return new ExtMergedIcon(domainInfo.getFavIcon(), 0, 0).add(new AbstractIcon(IconKey.ICON_ERROR, 12), 6, 6); }
        if (invalid > 0) { return new ExtMergedIcon(domainInfo.getFavIcon(), 0, 0).add(new AbstractIcon(IconKey.ICON_WARNING, 12), 6, 6); }

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

    public int getInvalidCount() {
        return invalid;
    }

}
