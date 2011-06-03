package jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;

import jd.controlling.IOEQ;
import jd.controlling.proxy.ProxyController;
import jd.controlling.proxy.ProxyInfo;

public class ProxyDeleteAction extends AbstractAction {

    private ArrayList<ProxyInfo> selected = null;

    public ProxyDeleteAction(final ArrayList<ProxyInfo> selected) {
        super("Remove Proxy(s)");
        this.selected = selected;
    }

    @Override
    public boolean isEnabled() {
        boolean canremove = false;
        if (selected != null) {
            for (ProxyInfo pi : selected) {
                if (pi.getProxy().isRemote()) {
                    canremove = true;
                    break;
                }
            }
        }
        return canremove;
    }

    /**
     * 
     */
    private static final long serialVersionUID = -197136045388327528L;

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        IOEQ.add(new Runnable() {
            public void run() {
                for (ProxyInfo proxy : selected) {
                    ProxyController.getInstance().remove(proxy);
                }
            }
        });

    }
}