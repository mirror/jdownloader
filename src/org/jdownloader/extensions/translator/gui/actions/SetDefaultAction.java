package org.jdownloader.extensions.translator.gui.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.gui.swing.jdgui.interfaces.ContextMenuAction;

import org.appwork.utils.logging.Log;
import org.jdownloader.extensions.translator.TranslateEntry;

public class SetDefaultAction extends ContextMenuAction {
    private ArrayList<TranslateEntry> selection;

    public SetDefaultAction(ArrayList<TranslateEntry> selection) {
        this.selection = selection;
        init();
    }

    public void actionPerformed(ActionEvent e) {
        for (TranslateEntry obj : selection) {
            if (obj.getDefault() != null) obj.setTranslation(obj.getDefault());
            Log.L.info(obj.getKey());
        }
    }

    @Override
    protected String getName() {
        Integer cntSelected = selection.size();
        String ret = "Set to default (" + cntSelected.toString() + " Entr" + ((cntSelected > 1) ? "ies" : "y") + ")";
        return ret;
    }

    @Override
    protected String getIcon() {
        // return "flags/en";
        return null;
    }

}
