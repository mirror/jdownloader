package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;

import jd.controlling.IOEQ;
import jd.controlling.packagecontroller.AbstractNode;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ConfirmAction extends AbstractAction {

    /**
     * 
     */
    private static final long       serialVersionUID = -3937346180905569896L;
    private ArrayList<AbstractNode> values;
    private boolean                 force;

    public ConfirmAction(boolean force, ArrayList<AbstractNode> arrayList) {
        putValue(SMALL_ICON, force ? NewTheme.I().getIcon("media-playback-start_forced", 16) : NewTheme.I().getIcon("media-playback-start", 16));
        putValue(NAME, force ? _GUI._.ConfirmAction_ConfirmAction_forced() : _GUI._.ConfirmAction_ConfirmAction_());
        this.values = arrayList;
        this.force = force;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        IOEQ.add(new Runnable() {

            public void run() {
            }

        }, true);
    }

    @Override
    public boolean isEnabled() {
        return values != null && values.size() > 0;
    }

}
