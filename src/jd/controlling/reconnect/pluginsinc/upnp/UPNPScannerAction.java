package jd.controlling.reconnect.pluginsinc.upnp;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

import jd.controlling.reconnect.pluginsinc.upnp.cling.UPNPDeviceScanner;
import jd.controlling.reconnect.pluginsinc.upnp.cling.UpnpRouterDevice;
import jd.controlling.reconnect.pluginsinc.upnp.translate.T;

import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.tooltips.BasicTooltipFactory;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.images.NewTheme;

public class UPNPScannerAction extends BasicAction {

    private UPNPRouterPlugin plugin;

    public UPNPScannerAction(UPNPRouterPlugin upnpRouterPlugin) {
        super(T._.literally_choose_router());
        this.plugin = upnpRouterPlugin;
        putValue(SMALL_ICON, NewTheme.I().getIcon("list", 18));
        setTooltipFactory(new BasicTooltipFactory(getName(), T._.UPNPScannerAction_UPNPScannerAction_tt(), NewTheme.I().getIcon("list", 32)));

    }

    public void actionPerformed(ActionEvent e) {

        final ProgressDialog dialog = new ProgressDialog(new ProgressGetter() {

            public int getProgress() {
                return -1;
            }

            public String getString() {
                return null;
            }

            public void run() throws Exception {

                final java.util.List<UpnpRouterDevice> devices = new UPNPDeviceScanner().scan();
                if (devices.size() == 0) {
                    Dialog.getInstance().showErrorDialog(T._.UPNPRouterPlugin_run_error());

                    return;
                }
                if (Thread.currentThread().isInterrupted()) { return; }

                int ret = Dialog.getInstance().showComboDialog(0, T._.UPNPRouterPlugin_run_wizard_title(), T._.UPNPRouterPlugin_run_mesg(), devices.toArray(new UpnpRouterDevice[] {}), 0, NewTheme.I().getIcon("upnp", 32), null, null, new DefaultListCellRenderer() {

                    private static final long serialVersionUID = 3607383089555373774L;

                    @Override
                    public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
                        final JLabel label = (JLabel) super.getListCellRendererComponent(list, ((UpnpRouterDevice) value).getModelname() + "(" + ((UpnpRouterDevice) value).getWanservice() + ")", index, isSelected, cellHasFocus);

                        return label;
                    }
                });
                if (Thread.currentThread().isInterrupted()) { return; }
                if (ret < 0) { return; }
                plugin.setDevice(devices.get(ret));

            }

            @Override
            public String getLabelString() {
                return null;
            }

        }, 0, "Looking for routers", "Wait while JDownloader is looking for router interfaces", null);

        try {
            Dialog.getInstance().showDialog(dialog);
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }

}
