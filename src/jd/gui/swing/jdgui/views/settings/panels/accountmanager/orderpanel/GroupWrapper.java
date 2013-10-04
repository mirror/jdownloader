package jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel;

import java.util.ArrayList;

import org.appwork.swing.exttable.tree.TreeNodeInterface;
import org.jdownloader.controlling.hosterrule.AccountGroup;
import org.jdownloader.controlling.hosterrule.AccountGroup.Rules;
import org.jdownloader.controlling.hosterrule.AccountReference;

public class GroupWrapper implements AccountInterface {

    private AccountGroup                 group;
    private Rules                        rule = null;
    private boolean                      enabled;
    private ArrayList<TreeNodeInterface> children;
    private String                       name = null;

    public void setName(String name) {
        this.name = name;
    }

    public GroupWrapper(AccountGroup g) {
        this.group = g;
        enabled = group.isEnabled();
        rule = group.getRule();
        name = g.getName();
        children = new ArrayList<TreeNodeInterface>();
        for (AccountReference acc : group.getChildren()) {
            AccountWrapper aw = new AccountWrapper(acc);
            aw.setParent(this);
            children.add(aw);
        }
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
        return "";
    }

    @Override
    public String getUser() {
        return "";
    }

    public Rules getRule() {
        return rule;
    }

    public void setRule(Rules value) {
        this.rule = value;
    }

    public void getGroup() {
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public ArrayList<TreeNodeInterface> getChildren() {
        return children;
    }

    @Override
    public TreeNodeInterface getParent() {
        return null;
    }

    @Override
    public void setParent(TreeNodeInterface parent) {
    }

    public String getName() {
        return name;
    }
}
