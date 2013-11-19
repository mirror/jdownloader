package jd.gui.swing.jdgui.views.settings.panels.proxy;

import java.awt.event.ActionEvent;

import jd.controlling.TaskQueue;
import jd.controlling.proxy.ProxyController;
import jd.controlling.proxy.ProxyInfo;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.actions.AppAction;

public class ProxyDeleteAction extends AppAction {

    private java.util.List<ProxyInfo> selected = null;
    private ProxyTable                table;

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
        final java.util.List<ProxyInfo> finalSelection;
        if (selected == null) {
            finalSelection = table.getModel().getSelectedObjects();
        } else {
            finalSelection = selected;
        }
        if (finalSelection != null && finalSelection.size() > 0) {
            TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    for (ProxyInfo proxy : finalSelection) {
                        ProxyController.getInstance().remove(proxy);
                    }
                    return null;
                }
            });
        }
    }
}