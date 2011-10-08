package org.jdownloader.gui.views.linkgrabber.quickfilter;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.jdownloader.controlling.filter.LinkgrabberFilterRuleWrapper;
import org.jdownloader.images.NewTheme;

public class CustomizedFilter<E extends AbstractPackageNode<V, E>, V extends AbstractPackageChildrenNode<E>> extends Filter<E, V> {

    private LinkgrabberFilterRuleWrapper rule;

    public CustomizedFilter(LinkgrabberFilterRuleWrapper rule) {
        super(rule.getRule().getName(), NewTheme.I().getIcon("find", 16), false);
        this.rule = rule;
    }

    @Override
    public boolean isFiltered(V link) {
        return true;
    }

    @Override
    public boolean isFiltered(E link) {
        return false;
    }

}
