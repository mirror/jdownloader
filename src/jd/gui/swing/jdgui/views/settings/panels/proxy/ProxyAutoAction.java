package jd.gui.swing.jdgui.views.settings.panels.proxy;

import java.awt.event.ActionEvent;
import java.util.List;

import jd.controlling.IOEQ;
import jd.controlling.proxy.ProxyController;

import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class ProxyAutoAction extends AppAction {

    public ProxyAutoAction() {
        super();
        setName(_GUI._.ProxyAutoAction_actionPerformed_d_title());
        setIconKey("plugin");
    }

    /**
     * 
     */
    private static final long serialVersionUID = -197136045388327528L;

    public void actionPerformed(ActionEvent e) {
        IOEQ.add(new Runnable() {

            public void run() {
                List<HTTPProxy> list = ProxyController.autoConfig();
                ProxyController.getInstance().addProxy(list);
            }
        });

    }
}
