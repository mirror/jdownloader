package jd.gui.swing.jdgui.views.settings.panels.basicauthentication;

import java.awt.event.ActionEvent;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.auth.AuthenticationController;
import org.jdownloader.auth.AuthenticationInfo;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.components.AbstractAddAction;

import jd.controlling.TaskQueue;

public class NewAction extends AbstractAddAction {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public NewAction(AuthTable table) {
        this.setIconKey(IconKey.ICON_ADD);
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
