package jd.gui.swing.jdgui.views.settings.panels.proxy;

import java.awt.event.ActionEvent;

import jd.controlling.TaskQueue;
import jd.controlling.proxy.AbstractProxySelectorImpl;
import jd.controlling.proxy.ProxyController;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class ProxyDeleteAction extends AppAction {

    private java.util.List<AbstractProxySelectorImpl> selected = null;
    private ProxyTable                           table;

    public ProxyDeleteAction(ProxyTable table) {

        this.table = table;
        setName(_GUI._.literally_remove());
        setIconKey(IconKey.ICON_REMOVE);

    }

    public ProxyDeleteAction(final java.util.List<AbstractProxySelectorImpl> selected, boolean force) {
        super();
        this.selected = selected;
        setName(_GUI._.literally_remove());
        setIconKey(IconKey.ICON_REMOVE);

    }

    @Override
    public boolean isEnabled() {
        if (this.table != null) return super.isEnabled();
        boolean canremove = false;
        if (selected != null) {
            for (AbstractProxySelectorImpl pi : selected) {
                switch (pi.getType()) {
                case NONE:
                    continue;
                }

                canremove = true;
                break;
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
        final java.util.List<AbstractProxySelectorImpl> finalSelection;
        if (selected == null) {
            finalSelection = table.getModel().getSelectedObjects();
        } else {
            finalSelection = selected;
        }
        if (finalSelection != null && finalSelection.size() > 0) {
            TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    for (AbstractProxySelectorImpl proxy : finalSelection) {
                        ProxyController.getInstance().remove(proxy);
                    }
                    return null;
                }
            });
        }
    }
}