package jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;

import jd.controlling.IOEQ;
import jd.controlling.proxy.ProxyController;
import jd.controlling.proxy.ProxyInfo;

import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class ProxyDeleteAction extends AbstractAction {

    private ArrayList<ProxyInfo> selected = null;
    private ProxyTable           table;

    public ProxyDeleteAction(ProxyTable table) {
        super("Remove Proxy(s)");
        this.table = table;
        this.putValue(NAME, _JDT._.basics_remove());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("remove", 20));
    }

    public ProxyDeleteAction(final ArrayList<ProxyInfo> selected, boolean force) {
        super("Remove Proxy(s)");
        this.selected = selected;
        this.putValue(NAME, _JDT._.basics_remove());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("remove", 16));
    }

    @Override
    public boolean isEnabled() {
        if (this.table != null) return super.isEnabled();
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