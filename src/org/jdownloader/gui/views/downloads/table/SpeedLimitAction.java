package org.jdownloader.gui.views.downloads.table;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class SpeedLimitAction extends AppAction {

    private java.util.List<AbstractNode> inteliSelect;
    private AbstractNode            context;

    public SpeedLimitAction(AbstractNode contextObject, java.util.List<AbstractNode> inteliSelect) {

        setName(_GUI._.ContextMenuFactory_createPopup_speed());
        setIconKey("speed");
        this.context = contextObject;
        this.inteliSelect = inteliSelect;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        try {
            Dialog.getInstance().showDialog(new SpeedLimitator(context, inteliSelect));
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }

}
