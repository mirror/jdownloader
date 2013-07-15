package jd.gui.swing.jdgui.views.settings.panels.proxy;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;

import jd.controlling.ClipboardMonitoring;
import jd.controlling.proxy.ProxyInfo;
import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ProxyTable extends BasicJDTable<ProxyInfo> {

    /**
     * 
     */
    private static final long serialVersionUID = 1153823766916158314L;

    public ProxyTable() {
        super(new ProxyTableModel());
        setSearchEnabled(true);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.swing.exttable.ExtTable#onContextMenu(javax.swing.JPopupMenu , java.lang.Object, java.util.ArrayList,
     * org.appwork.swing.exttable.ExtColumn)
     */
    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, ProxyInfo contextObject, java.util.List<ProxyInfo> selection, ExtColumn<ProxyInfo> column, MouseEvent ev) {
        popup.add(new JMenuItem(new ProxyAddAction()));
        popup.add(new JMenuItem(new ProxyDeleteAction(selection, false)));
        return popup;
    }

    @Override
    protected boolean onShortcutDelete(final java.util.List<ProxyInfo> selectedObjects, final KeyEvent evt, final boolean direct) {

        new ProxyDeleteAction(selectedObjects, direct).actionPerformed(null);
        return true;
    }

    protected boolean onShortcutCopy(final java.util.List<ProxyInfo> selectedObjects, final KeyEvent evt) {
        export(selectedObjects);
        return true;
    }

    public static void export(java.util.List<ProxyInfo> selectedObjects) {
        StringBuilder sb = new StringBuilder();
        for (ProxyInfo pi : selectedObjects) {
            if (sb.length() > 0) sb.append("\r\n");
            boolean hasUSerInfo = false;
            switch (pi.getType()) {
            case HTTP:
                sb.append("http://");

                break;
            case SOCKS4:
                sb.append("socks4://");
                break;

            case SOCKS5:
                sb.append("socks5://");
                break;
            default:
                continue;

            }

            if (!StringUtils.isEmpty(pi.getUser())) {
                sb.append(pi.getUser());
                hasUSerInfo = true;
            }

            if (!StringUtils.isEmpty(pi.getPass())) {
                if (hasUSerInfo) sb.append(":");
                hasUSerInfo = true;
                sb.append(pi.getPass());
            }
            if (hasUSerInfo) {
                sb.append("@");

            }
            sb.append(pi.getHost());
            if (pi.getPort() > 0) {
                sb.append(":");
                sb.append(pi.getPort());
            }

        }
        ClipboardMonitoring.getINSTANCE().setCurrentContent(sb.toString());
        try {

            Dialog.getInstance().showInputDialog(Dialog.STYLE_LARGE | UIOManager.BUTTONS_HIDE_CANCEL, _GUI._.ProxyTable_copy_export_title_(), null, sb.toString(), NewTheme.getInstance().getIcon("proxy", 32), null, null);
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }

    }
}
