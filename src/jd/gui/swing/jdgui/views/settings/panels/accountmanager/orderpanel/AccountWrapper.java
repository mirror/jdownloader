package jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel;

import java.util.Date;
import java.util.List;

import jd.plugins.AccountInfo;

import org.appwork.swing.exttable.tree.TreeNodeInterface;
import org.jdownloader.controlling.hosterrule.AccountReference;

public class AccountWrapper implements AccountInterface {

    private AccountReference account;
    private boolean          enabled;
    private GroupWrapper     parent;

    public AccountWrapper(AccountReference a) {
        this.account = a;
        enabled = a.isEnabled();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean value) {
        enabled = value;
    }

    @Override
    public String getHost() {
        return account.getHoster();
    }

    @Override
    public String getUser() {
        return account.getUser();
    }

    public Date getExpireDate() {
        return account.getExpireDate();
    }

    public AccountReference getAccount() {
        return account;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public List<TreeNodeInterface> getChildren() {
        return null;
    }

    public AccountInfo getAccountInfo() {
        return account.getAccountInfo();
    }

    public boolean isValid() {

        return account.isValid();
    }

    public boolean isTempDisabled() {
        return account.isTempDisabled();
    }

    public long getTmpDisabledTimeout() {
        return account.getTmpDisabledTimeout();
    }

    public void setParent(GroupWrapper groupWrapper) {
        this.parent = groupWrapper;
    }

    @Override
    public GroupWrapper getParent() {
        return parent;
    }

    @Override
    public void setParent(TreeNodeInterface parent) {
        this.parent = (GroupWrapper) parent;
    }

}
