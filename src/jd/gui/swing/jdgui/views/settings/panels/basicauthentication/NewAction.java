package jd.gui.swing.jdgui.views.settings.panels.basicauthentication;

import java.awt.event.ActionEvent;

import jd.controlling.TaskQueue;
import jd.controlling.authentication.AuthenticationController;
import jd.controlling.authentication.AuthenticationInfo;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.gui.views.components.AbstractAddAction;

public class NewAction extends AbstractAddAction {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public NewAction(AuthTable table) {
        this.setIconKey("add");
    }

    public void actionPerformed(ActionEvent e) {
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                AuthenticationController.getInstance().add(new AuthenticationInfo());
                return null;
            }
        });
    }

}
