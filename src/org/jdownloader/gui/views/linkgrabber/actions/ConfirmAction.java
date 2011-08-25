package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;

import jd.plugins.PackageLinkNode;

import org.appwork.utils.logging.Log;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ConfirmAction extends AbstractAction {

    private ArrayList<PackageLinkNode> values;
    private boolean                    force;

    public ConfirmAction(boolean force, ArrayList<PackageLinkNode> arrayList) {
        putValue(SMALL_ICON, force ? NewTheme.I().getIcon("media-playback-start_forced", 16) : NewTheme.I().getIcon("media-playback-start", 16));

        putValue(NAME, force ? _GUI._.ConfirmAction_ConfirmAction_forced() : _GUI._.ConfirmAction_ConfirmAction_());
        this.values = arrayList;
        this.force = force;
    }

    public ConfirmAction() {
        this(false, null);
    }

    public void actionPerformed(ActionEvent e) {
        Log.L.finer("Start " + values);
    }

}
