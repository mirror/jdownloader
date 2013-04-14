package org.jdownloader.gui.views.components.packagetable.context;

import org.jdownloader.controlling.Priority;
import org.jdownloader.gui.views.SelectionInfo;

public class PriorityLowerAction extends PriorityActionEntry {

    public PriorityLowerAction(SelectionInfo si) {
        super(Priority.LOWER, si);
    }

}
