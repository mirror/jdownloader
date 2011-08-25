package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.plugins.PackageLinkNode;

import org.appwork.utils.logging.Log;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ConfirmAllAction extends AbstractAction {
    private PackageLinkNode value;
    private boolean         force;

    public ConfirmAllAction(boolean force) {
        putValue(SMALL_ICON, force ? NewTheme.I().getIcon("media-playback-start_forced", 16) : NewTheme.I().getIcon("media-playback-start", 16));

        putValue(NAME, force ? _GUI._.ConfirmAllAction_ConfirmAllAction_force() : _GUI._.ConfirmAllAction_ConfirmAllAction_());
        this.force = force;

    }

    public ConfirmAllAction() {
        this(false);
    }

    public void actionPerformed(ActionEvent e) {
        Log.L.finer("Start " + value);
    }

}
