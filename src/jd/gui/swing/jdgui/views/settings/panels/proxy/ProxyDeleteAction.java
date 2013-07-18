package jd.gui.swing.jdgui.views.settings.panels.proxy;

import java.awt.event.ActionEvent;

import jd.controlling.IOEQ;
import jd.controlling.proxy.ProxyController;
import jd.controlling.proxy.ProxyInfo;

import org.jdownloader.gui.views.components.AbstractRemoveAction;

public class ProxyDeleteAction extends AbstractRemoveAction {

    private java.util.List<ProxyInfo> selected = null;
    private ProxyTable           table;

    public ProxyDeleteAction(ProxyTable table) {

        this.table = table;

    }

    public ProxyDeleteAction(final java.util.List<ProxyInfo> selected, boolean force) {
        super();
        this.selected = selected;
      
    }

    @Override
    public boolean isEnabled() {
        if (this.table != null) return super.isEnabled();
        boolean canremove = false;
        if (selected != null) {
            for (ProxyInfo pi : selected) {
                if (pi.isRemote() || pi.isDirect()) {
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
                if (selected == null) selected = table.getModel().getSelectedObjects();
                for (ProxyInfo proxy : selected) {
                    ProxyController.getInstance().remove(proxy);
                }
                selected = null;
            }
        });

    }
}