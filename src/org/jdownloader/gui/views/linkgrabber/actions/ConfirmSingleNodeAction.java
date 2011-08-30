package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.utils.logging.Log;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ConfirmSingleNodeAction extends AbstractAction {

    private AbstractNode value;

    public AbstractNode getValue() {
        return value;
    }

    public void setValue(AbstractNode value) {
        this.value = value;
    }

    public ConfirmSingleNodeAction() {
        putValue(SMALL_ICON, NewTheme.I().getIcon("media-playback-start", 16));

        putValue(SHORT_DESCRIPTION, _GUI._.ConfirmSingleNodeAction_ConfirmSingleNodeAction_tt());

    }

    public void actionPerformed(ActionEvent e) {
        Log.L.finer("Start " + value);
    }

}
