package org.jdownloader.controlling.hosterrule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jdownloader.gui.translate._GUI;

public class AccountGroup {
    public static enum Rules {
        RANDOM(_GUI.T.Rules_random()),
        // BALANCED IS NOT ACTIVE IN release version yet. when finished, please edit
        // jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.dialog.GroupRuleColumn.getRules()
        BALANCED("TODO JIAZ: " + _GUI.T.Rules_balanced()),
        ORDER(_GUI.T.Rules_order()),
        DISABLED(_GUI.T.Rules_disabled());
        private String translation;

        private Rules(String translation) {
            this.translation = translation;
        }

        public String translate() {
            return translation;
        }
    }

    private final List<AccountReference> children;
    private Rules                        rule = Rules.RANDOM;

    public void setRule(Rules rule) {
        if (rule == null) {
            this.rule = Rules.RANDOM;
        } else {
            this.rule = rule;
        }
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AccountGroup(List<AccountReference> childs) {
        if (childs != null) {
            children = new CopyOnWriteArrayList<AccountReference>(childs);
        } else {
            children = new CopyOnWriteArrayList<AccountReference>();
        }
    }

    public String toString() {
        return "AC: " + getName() + " - " + getChildren();
    }

    public AccountGroup(ArrayList<AccountReference> refList, String name) {
        this(refList);
        setName(name);
    }

    public List<AccountReference> getChildren() {
        return children;
    }

    public Rules getRule() {
        return rule;
    }
}
