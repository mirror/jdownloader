package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class DisableAllAction extends AppAction {

    private ArrayList<Filter> selection;

    public DisableAllAction(ArrayList<Filter> selection) {
        super();
        this.selection = selection;
        setName(_GUI._.DisableAllAction_DisableAllAction_object_());
        setSmallIcon(NewTheme.I().getIcon("checkbox_false", -1));
    }

    public void actionPerformed(ActionEvent e) {
        for (Filter f : selection) {
            f.setEnabled(false);
        }
    }

}
