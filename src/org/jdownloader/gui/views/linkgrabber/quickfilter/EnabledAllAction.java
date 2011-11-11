package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class EnabledAllAction extends AppAction {

    private ArrayList<Filter> selection;

    public EnabledAllAction(ArrayList<Filter> selection) {
        super();
        this.selection = selection;
        setName(_GUI._.EnabledAllAction_EnabledAllAction_object_());
        setSmallIcon(NewTheme.I().getIcon("checkbox_true", -1));
    }

    public void actionPerformed(ActionEvent e) {
        for (Filter f : selection) {
            f.setEnabled(true);
        }
    }

}
