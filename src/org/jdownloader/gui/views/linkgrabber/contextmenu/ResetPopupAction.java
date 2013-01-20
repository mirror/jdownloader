package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberPanel;
import org.jdownloader.gui.views.linkgrabber.actions.ResetAction;

public class ResetPopupAction extends AppAction {
    /**
     * 
     */
    private static final long serialVersionUID = 841782078416257540L;
    private LinkGrabberPanel  panel;

    public ResetPopupAction(LinkGrabberPanel panel) {
        setName(_GUI._.ResetPopupAction_ResetPopupAction_());
        setIconKey("reset");
        this.panel = panel;
    }

    public void actionPerformed(ActionEvent e) {
        new ResetAction(panel).actionPerformed(e);
    }

}
