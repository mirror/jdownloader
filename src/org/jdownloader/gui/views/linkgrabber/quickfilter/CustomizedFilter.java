package org.jdownloader.gui.views.linkgrabber.quickfilter;

import javax.swing.ImageIcon;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.jdownloader.controlling.filter.LinkgrabberFilterRuleWrapper;
import org.jdownloader.images.NewTheme;

public abstract class CustomizedFilter<E extends AbstractPackageNode<V, E>, V extends AbstractPackageChildrenNode<E>> extends Filter<E, V> {

    protected LinkgrabberFilterRuleWrapper lgr;

    public CustomizedFilter(LinkgrabberFilterRuleWrapper rule) {
        super(rule.getRule().getName(), null, false);
        this.lgr = rule;
    }

    @Override
    public String getName() {
        return lgr.getRule().getName();
    }

    @Override
    public ImageIcon getIcon() {
        if (lgr.getRule().isAccept()) {
            return NewTheme.I().getIcon("ok", 16);
        } else {
            return NewTheme.I().getIcon("cancel", 16);
        }
    }

}
