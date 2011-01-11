package jd.controlling.reconnect.plugins.liveheader;

import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jd.config.Configuration;
import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.ReconnectPluginController;
import jd.controlling.reconnect.RouterUtils;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.dialog.ContainerDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;

public class LiveHeaderDetectionWizard {

    private JTextField txtName;

    private JTextField txtManufactor;
    private JTextField txtIP;
    private JTextField txtUser;
    private JTextField txtPass;

    private LiveHeaderReconnect getPlugin() {
        return (LiveHeaderReconnect) ReconnectPluginController.getInstance().getPluginByID(LiveHeaderReconnect.ID);
    }

    public int runOfflineScan() throws InterruptedException {
        int ret = -1;
        // final ArrayList<String[]> scripts =
        // LiveHeaderReconnect.getLHScripts();

        this.txtName = new JTextField();
        this.txtManufactor = new JTextField();
        this.txtUser = new JTextField();
        this.txtPass = new JTextField();
        this.txtIP = new JTextField();

        final JPanel p = new JPanel(new MigLayout("ins 5,wrap 2", "[][grow,fill]", "[grow,fill]"));
        p.add(new JLabel("Please enter your router information as far as possible."), "spanx");
        p.add(new JLabel("Model Name"));
        p.add(this.txtName);

        p.add(new JLabel("Manufactor"));
        p.add(this.txtManufactor);

        // p.add(new JLabel("Firmware"));
        // p.add(this.txtFirmware);

        p.add(new JLabel("Webinterface IP"));
        p.add(this.txtIP);

        p.add(new JLabel("Webinterface User"));
        p.add(this.txtUser);
        p.add(new JLabel("Webinterface Password"));
        p.add(this.txtPass);
        this.txtUser.setText(this.getPlugin().getUser());
        this.txtPass.setText(this.getPlugin().getPassword());
        this.txtName.setText(this.getPlugin().getRouterName());
        this.txtIP.setText(this.getPlugin().getRouterIP());
        while (true) {

            final ContainerDialog routerInfo = new ContainerDialog(0, "Enter Router Information", p, null, "Continue", null);

            try {
                Dialog.getInstance().showDialog(routerInfo);
            } catch (DialogNoAnswerException e) {
                e.printStackTrace();
            }
            try {
                if (this.txtUser.getText().trim().length() < 2 || this.txtPass.getText().trim().length() < 2) {

                    Dialog.getInstance().showConfirmDialog(0, "Warning", "Username and Password are not set. In most cases, \r\nthese information is required for a successfull reconnection.\r\n\r\nContinue anyway?");

                }

                if (!RouterUtils.checkPort(this.txtIP.getText().trim(), 80)) {
                    Dialog.getInstance().showConfirmDialog(0, "Warning", "There is no Webinterface at http://" + this.txtIP.getText() + "\r\nAre you sure that the Router IP is correct?\r\nA correct Router IP is required to find the correct settings.\r\n\r\nContinue anyway?");
                }

                ret = this.scanOfflineRouters(".*" + this.txtName.getText().trim().toLowerCase().replaceAll("\\W", ".*?") + ".*", ".*" + this.txtManufactor.getText().trim().toLowerCase().replaceAll("\\W", ".*?") + ".*");
                if (ret <= 0) {
                    Dialog.getInstance().showConfirmDialog(0, "Warning", "Could not find correct settings based on your inputs?\r\n\r\nTry again?");
                } else {
                    return ret;
                }
            } catch (DialogClosedException e) {
                return -1;
            } catch (DialogCanceledException e) {
                continue;
            }
            return -1;

        }

    }

    public int runOnlineScan() {
        return 0;
    }

    private int scanOfflineRouters(final String name, final String manufactor) throws InterruptedException {

        final ArrayList<String[]> scripts = LiveHeaderReconnect.getLHScripts();
        final ArrayList<String[]> filtered = new ArrayList<String[]>();
        String man, mod;
        for (final String[] script : scripts) {
            man = script[0].trim().toLowerCase();
            mod = script[1].trim().toLowerCase();
            if (name.trim().length() > 2) {
                if (!mod.matches(name)) {
                    continue;
                }
            }
            if (manufactor.trim().length() > 2) {
                if (!man.matches(manufactor)) {
                    continue;
                }
            }
            filtered.add(script);
        }
        for (final String[] script : filtered) {
            if (Thread.currentThread().isInterrupted()) { return -1; }
            this.getPlugin().setRouterIP(this.txtIP.getText());
            this.getPlugin().setUser(this.txtUser.getText());
            this.getPlugin().setPassword(this.txtPass.getText());
            this.getPlugin().setScript(script[2]);
            final long start = System.currentTimeMillis();
            if (Thread.currentThread().isInterrupted()) { return -1; }
            final int waitTimeBefore = JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_WAITFORIPCHANGE, 30);
            try {
                // at least after 5 (init) +10 seconds, we should be offline. if
                // we
                // are offline, reconnectsystem increase waittime about 120
                // seconds
                // anyway
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_WAITFORIPCHANGE, 10);
                try {
                    if (ReconnectPluginController.getInstance().doReconnect(this.getPlugin())) {
                        // restore afterwards

                        return (int) (System.currentTimeMillis() - start);
                    }
                } catch (final ReconnectException e) {
                    e.printStackTrace();
                }
            } finally {
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_WAITFORIPCHANGE, waitTimeBefore);
            }
        }

        return -1;
    }

}
