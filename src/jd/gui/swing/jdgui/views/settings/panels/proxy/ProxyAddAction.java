package jd.gui.swing.jdgui.views.settings.panels.proxy;

import java.awt.event.ActionEvent;

import jd.controlling.TaskQueue;
import jd.controlling.proxy.ProxyController;
import jd.gui.swing.dialog.ProxyDialog;

import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.gui.views.components.AbstractAddAction;

public class ProxyAddAction extends AbstractAddAction {

    public ProxyAddAction(ProxyTable table) {
        super();

    }

    public ProxyAddAction() {
        super();

    }

    /**
     * 
     */
    private static final long serialVersionUID = -197136045388327528L;

    public void actionPerformed(ActionEvent e) {
        ProxyDialog proxyDialog = new ProxyDialog();
        try {
            final HTTPProxy proxy = Dialog.getInstance().showDialog(proxyDialog);
            if (proxy == null) return;
            TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    ProxyController.getInstance().addProxy(proxy);
                    return null;
                }
            });
        } catch (final DialogNoAnswerException e1) {
        }
    }
}
