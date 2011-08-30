package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.utils.logging.Log;
import org.jdownloader.images.NewTheme;

public class RemoveAction extends AbstractAction {
    private AbstractNode value;

    public RemoveAction() {
        putValue(SMALL_ICON, NewTheme.I().getIcon("cancel", 14));

    }

    public void actionPerformed(ActionEvent e) {
        Log.L.finer("delete " + value);
    }

    public void setValue(AbstractNode value) {
        this.value = value;
    }

}
