package jd.gui.swing.jdgui.views.settings.panels.proxy;

import java.awt.event.ActionEvent;

import jd.controlling.IOEQ;
import jd.controlling.proxy.ProxyController;
import jd.gui.swing.dialog.ProxyDialog;

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
            IOEQ.add(new Runnable() {

                public void run() {
                    ProxyController.getInstance().addProxy(proxy);
                }
            });
        } catch (final DialogNoAnswerException e1) {
        }
    }
}
