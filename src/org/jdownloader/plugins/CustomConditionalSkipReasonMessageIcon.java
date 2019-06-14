package org.jdownloader.plugins;

import javax.swing.Icon;

import jd.controlling.packagecontroller.AbstractNode;

public interface CustomConditionalSkipReasonMessageIcon {
    public String getMessage(ConditionalSkipReason conditionalSkipReason, AbstractNode node);

    public Icon getIcon(ConditionalSkipReason conditionalSkipReason, AbstractNode node);
}
