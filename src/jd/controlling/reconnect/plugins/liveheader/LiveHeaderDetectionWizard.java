package jd.controlling.reconnect.plugins.liveheader;

import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jd.controlling.reconnect.ReconnectPluginController;
import jd.controlling.reconnect.ReconnectWizardProgress;
import jd.controlling.reconnect.RouterUtils;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.dialog.ContainerDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;

public class LiveHeaderDetectionWizard {

    private JTextField              txtName;

    private JTextField              txtManufactor;
    private JTextField              txtIP;
    private JTextField              txtUser;
    private JTextField              txtPass;

    private ReconnectWizardProgress progress;

    public LiveHeaderDetectionWizard(ReconnectWizardProgress progress) {
        this.progress = progress;
    }

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
                return -1;
            }
            try {
                if (this.txtUser.getText().trim().length() < 2 || this.txtPass.getText().trim().length() < 2) {

                    Dialog.getInstance().showConfirmDialog(0, "Warning", "Username and Password are not set. In most cases, \r\nthese information is required for a successfull reconnection.\r\n\r\nContinue anyway?");

                }

                if (!RouterUtils.checkPort(this.txtIP.getText().trim(), 80)) {
                    Dialog.getInstance().showConfirmDialog(0, "Warning", "There is no Webinterface at http://" + this.txtIP.getText() + "\r\nAre you sure that the Router IP is correct?\r\nA correct Router IP is required to find the correct settings.\r\n\r\nContinue anyway?");
                }
                String man = this.txtManufactor.getText().trim();
                String name = this.txtName.getText().trim();
                ArrayList<TestScript> tests = this.scanOfflineRouters(name.length() > 0 ? ".*" + name.toLowerCase().replaceAll("\\W", ".*?") + ".*" : null, man.toLowerCase().length() > 0 ? ".*" + man.toLowerCase().replaceAll("\\W", ".*?") + ".*" : null);
                ret = runTests(tests);
                if (ret <= 0) {
                    try {
                        Dialog.getInstance().showConfirmDialog(0, "Warning", "Could not find correct settings based on your inputs?\r\n\r\nTry again?");
                        continue;
                    } catch (DialogClosedException e) {
                        return -1;
                    } catch (DialogCanceledException e) {
                        return -1;
                    }
                } else {
                    return ret;
                }
            } catch (DialogClosedException e) {
                return -1;
            } catch (DialogCanceledException e) {
                continue;
            }

        }

    }

    private int runTests(ArrayList<TestScript> tests) throws InterruptedException {

        for (int i = 0; i < tests.size(); i++) {
            progress.setProgress((i + 1) * 100 / tests.size());
            TestScript test = tests.get(i);
            progress.setStatusMessage(JDL.LF("jd.controlling.reconnect.plugins.liveheader.LiveHeaderDetectionWizard.runTests", "Test Script %s/%s: %s", (i + 1), tests.size(), test.getManufactor() + " - " + test.getModel()));

            if (test.run(getPlugin())) {
                // return first Script found
                return test.getTestDuration();
            }
        }
        return -1;
    }

    public int runOnlineScan() {
        return -1;
    }

    private ArrayList<TestScript> scanOfflineRouters(final String name, final String manufactor) throws InterruptedException {
        progress.setStatusMessage("Scan available Scripts");
        final ArrayList<String[]> scripts = LiveHeaderReconnect.getLHScripts();
        final ArrayList<String[]> filtered = new ArrayList<String[]>();
        String man, mod;
        for (final String[] script : scripts) {
            man = script[0].trim().toLowerCase();
            mod = script[1].trim().toLowerCase();
            if (name != null && name.trim().length() > 2) {
                if (!mod.matches(name)) {
                    continue;
                }
            }
            if (manufactor != null && manufactor.trim().length() > 2) {
                if (!man.matches(manufactor)) {
                    continue;
                }
            }
            filtered.add(script);
        }
        ArrayList<TestScript> ret = new ArrayList<TestScript>();
        for (final String[] script : filtered) {
            TestScript test = new TestScript(script[0], script[1]);
            ret.add(test);

            test.setRouterIP(this.txtIP.getText());
            test.setUser(this.txtUser.getText());
            test.setPassword(this.txtPass.getText());
            test.setScript(script[2]);

        }

        return ret;
    }

}
