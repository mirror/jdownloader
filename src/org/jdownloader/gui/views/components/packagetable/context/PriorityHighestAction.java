package org.jdownloader.gui.views.components.packagetable.context;

import org.jdownloader.controlling.Priority;
import org.jdownloader.gui.views.SelectionInfo;

public class PriorityHighestAction extends PriorityActionEntry {

    public PriorityHighestAction(SelectionInfo si) {
        super(Priority.HIGHEST, si);
    }

}
