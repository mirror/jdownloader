package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.awt.event.ActionEvent;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class EnabledAllAction extends AppAction {

    private java.util.List<Filter> selection;
    private boolean           enableIt;

    public EnabledAllAction(java.util.List<Filter> selection) {
        super();
        this.selection = selection;
        int enabled = 0;
        int disabled = 0;
        for (Filter f : selection) {
            if (f.isEnabled()) {
                enabled++;
            } else {
                disabled++;
            }
        }
        if (disabled == 0 && enabled == 0) {
            setEnabled(false);
        }
        if (disabled > 0) {
            enableIt = true;
            setName(_GUI._.EnabledAllAction_EnabledAllAction_object_show());
            setSmallIcon(NewTheme.I().getIcon("checkbox_true", -1));
        } else {
            enableIt = false;
            setName(_GUI._.EnabledAllAction_EnabledAllAction_object_hide());
            setSmallIcon(NewTheme.I().getIcon("checkbox_false", -1));
        }

    }

    public void actionPerformed(ActionEvent e) {
        for (Filter f : selection) {
            f.setEnabled(enableIt);
        }
    }

}
