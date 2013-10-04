package org.jdownloader.controlling.hosterrule;

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.gui.translate._GUI;

public class AccountGroup {

    public static enum Rules {
        RANDOM(_GUI._.Rules_random()),
        ORDER(_GUI._.Rules_order());
        private String translation;

        private Rules(String translation) {
            this.translation = translation;
        }

        public String translate() {
            return translation;
        }
    }

    private List<AccountReference> children;
    private boolean                enabled;
    private Rules                  rule = Rules.RANDOM;

    public void setRule(Rules rule) {
        if (rule == null) rule = Rules.RANDOM;
        this.rule = rule;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public AccountGroup(List<AccountReference> childs) {
        children = new ArrayList<AccountReference>(childs == null ? 1 : childs.size());
        if (childs != null) children.addAll(childs);

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
