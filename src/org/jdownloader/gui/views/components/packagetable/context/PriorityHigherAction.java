package org.jdownloader.gui.views.components.packagetable.context;

import org.jdownloader.controlling.Priority;
import org.jdownloader.gui.views.SelectionInfo;

public class PriorityHigherAction extends PriorityActionEntry {

    public PriorityHigherAction(SelectionInfo si) {
        super(Priority.HIGHER, si);
    }

}
