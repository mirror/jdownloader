package org.jdownloader.extensions.translator.gui.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.gui.swing.jdgui.interfaces.ContextMenuAction;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.extensions.translator.TranslateEntry;

public class SetDefaultAction extends ContextMenuAction {
    private ArrayList<TranslateEntry> selection;

    public SetDefaultAction(ArrayList<TranslateEntry> selection) {
        this.selection = selection;
        init();
    }

    public void actionPerformed(ActionEvent e) {
        Integer cntSelected = selection.size();
        try {
            if (cntSelected > 1) Dialog.getInstance().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, "Setting " + cntSelected + " key" + ((cntSelected > 1) ? "s" : "") + " to default", "Are you sure?", null, "Set", null);
            for (TranslateEntry obj : selection) {
                if (obj.getDefault() != null) obj.setTranslation(obj.getDefault());
            }
        } catch (DialogClosedException e1) {
        } catch (DialogCanceledException e1) {
        }
    }

    @Override
    protected String getName() {
        Integer cntSelected = selection.size();
        String ret = "Set to default (" + cntSelected.toString() + " Key" + ((cntSelected > 1) ? "s" : "") + ")";
        return ret;
    }

    @Override
    protected String getIcon() {
        // return "flags/en";
        return null;
    }

}
