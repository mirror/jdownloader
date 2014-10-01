package org.jdownloader.extensions.schedulerV2.gui.actions;

import java.awt.event.ActionEvent;

import org.jdownloader.extensions.schedulerV2.gui.AddScheduleEntryDialog;
import org.jdownloader.gui.views.components.AbstractAddAction;

public class NewAction extends AbstractAddAction {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public NewAction() {
        super();
    }

    public void actionPerformed(ActionEvent e) {
        /*
         * no need for IOEQ or Thread, as we want to show Dialog and that blocks EDT anyway
         */
        AddScheduleEntryDialog.showDialog();
        // Dialog.I().showMessageDialog("ToDo Here...");
    }

    @Override
    public String getTooltipText() {
        return "TODO";
    }

}
