package org.jdownloader.container.sft;

import java.util.ArrayList;

public class DelphiFormEntry {

    protected final int                  itemType;
    protected final DelphiFormEntry      parent;
    protected final boolean              accept_childs;
    protected String                     itemName;
    protected ArrayList<DelphiFormEntry> childs;

    public DelphiFormEntry(DelphiFormEntry parent, int itemType, boolean accept_childs) {
        this.accept_childs = accept_childs;
        this.itemType = itemType;
        this.parent = parent;
        this.childs = null;
        if (parent != null) parent.addChild(this);
    }

    final public DelphiFormEntry getParent() {
        return this.parent;
    }

    final public int getChildLength() {
        if (this.childs == null) return 0;
        return this.childs.size();
    }

    final public DelphiFormEntry getChildAt(int index) {
        if (this.childs == null) return null;
        return this.childs.get(index);
    }

    final public boolean removeChild(DelphiFormEntry child) {
        if (this.childs == null) return false;
        return this.childs.remove(child);
    }

    final public void remove() {
        if (this.parent != null) this.parent.removeChild(this);
    }

    final public void addChild(DelphiFormEntry child) {
        if (this.accept_childs) {
            if (this.childs == null) this.childs = new ArrayList<DelphiFormEntry>();
            this.childs.add(child);
        }
    }

    final public DelphiFormEntry get(int index) {
        return this.childs.get(index);
    }

    final public String getName() {
        return this.itemName;
    }

    final public void setName(String name) {
        this.itemName = new String(name);
    }

    final public int getType() {
        return this.itemType;
    }

    final public boolean isRoot() {
        return null == this.parent;
    }

    final public DelphiFormEntry find(String itemName) {
        for (DelphiFormEntry element : this.childs) {
            if (element.getName().equals(itemName)) return element;
        }
        return null;
    }

    public String getValue() {
        return null;
    }

    public void buildString(StringBuilder builder, String prepend) {
        if (this.childs == null) return;

        for (DelphiFormEntry element : this.childs) {
            element.buildString(builder, prepend);
        }
    }
}
