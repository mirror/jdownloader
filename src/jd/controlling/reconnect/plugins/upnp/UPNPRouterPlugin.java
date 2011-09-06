package jd.controlling.reconnect.plugins.upnp;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jd.controlling.JDLogger;
import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.ReconnectInvoker;
import jd.controlling.reconnect.ReconnectResult;
import jd.controlling.reconnect.RouterPlugin;
import jd.controlling.reconnect.ipcheck.IP;
import jd.controlling.reconnect.ipcheck.IPCheckException;
import jd.controlling.reconnect.ipcheck.IPCheckProvider;
import jd.controlling.reconnect.ipcheck.InvalidIPRangeException;
import jd.controlling.reconnect.ipcheck.InvalidProviderException;
import jd.controlling.reconnect.plugins.upnp.translate.T;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.event.ProcessCallBack;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.advanced.AdvancedConfigManager;

public class UPNPRouterPlugin extends RouterPlugin implements ActionListener, IPCheckProvider {

    public static final String            ID = "SIMPLEUPNP";

    private JButton                       find;
    private ExtTextField                  serviceTypeTxt;
    private ExtTextField                  controlURLTxt;
    private JLabel                        wanType;

    protected ArrayList<UpnpRouterDevice> devices;

    private ImageIcon                     icon;

    private UPUPReconnectSettings         settings;

    private JButton                       auto;

    public UPNPRouterPlugin() {
        super();
        icon = NewTheme.I().getIcon("upnp", 16);
        settings = JsonConfig.create(UPUPReconnectSettings.class);
        AdvancedConfigManager.getInstance().register(settings);
    }

    public void actionPerformed(final ActionEvent e) {
        // TODO: Do not use Appwork controller here
        // mixing two different Dialog controllers is not a good idea
        if (e.getSource() == this.find) {
            final ProgressDialog dialog = new ProgressDialog(new ProgressGetter() {

                public int getProgress() {
                    return -1;
                }

                public String getString() {
                    return null;
                }

                public void run() throws Exception {

                    final ArrayList<UpnpRouterDevice> devices = UPNPScanner.scanDevices();
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
                    UPNPRouterPlugin.this.setDevice(devices.get(ret));

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

    /**
     * sets the correct router settings automatically
     * 
     * @throws InterruptedException
     */
    @Override
    public ArrayList<ReconnectResult> runDetectionWizard(ProcessCallBack processCallBack) throws InterruptedException {

        ArrayList<ReconnectResult> ret = new ArrayList<ReconnectResult>();

        for (final UpnpRouterDevice device : getDevices()) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();

            ReconnectResult res;
            try {
                res = new UPNPReconnectInvoker(this, device.getServiceType(), device.getControlURL()).validate();
                if (res != null && res.isSuccess()) ret.add(res);
            } catch (ReconnectException e) {
                e.printStackTrace();
            }

        }

        return ret;
    }

    public IP getExternalIP() throws IPCheckException {
        String ipxml;
        try {
            ipxml = UPNPReconnectInvoker.runCommand(settings.getServiceType(), settings.getControlURL(), "GetExternalIPAddress");
        } catch (final Exception e) {
            this.setCanCheckIP(false);

            throw new InvalidProviderException("UPNP Command Error");
        }
        try {
            final Matcher ipm = Pattern.compile("<\\s*NewExternalIPAddress\\s*>\\s*(.*)\\s*<\\s*/\\s*NewExternalIPAddress\\s*>", Pattern.CASE_INSENSITIVE).matcher(ipxml);
            if (ipm.find()) { return IP.getInstance(ipm.group(1)); }
        } catch (final InvalidIPRangeException e2) {
            throw new InvalidProviderException(e2);
        }
        this.setCanCheckIP(false);

        throw new InvalidProviderException("Unknown UPNP Response Error");
    }

    @Override
    public ImageIcon getIcon16() {
        return icon;
    }

    public void setSetup(ReconnectResult reconnectResult) {
        UPNPReconnectResult r = (UPNPReconnectResult) reconnectResult;
        UPNPReconnectInvoker i = (UPNPReconnectInvoker) r.getInvoker();
        settings.setControlURL(i.getControlURL());
        settings.setModelName(i.getName());
        settings.setServiceType(i.getServiceType());
        updateGUI();
    }

    @Override
    public JComponent getGUI() {
        final JPanel p = new JPanel(new MigLayout("ins 0,wrap 3", "[][][grow,fill]", "[]"));
        this.find = new JButton(T._.literally_choose_router());
        this.find.addActionListener(this);
        this.auto = new JButton(new AutoDetectAction(this));

        this.serviceTypeTxt = new ExtTextField();
        this.controlURLTxt = new ExtTextField();

        serviceTypeTxt.setHelpText(T._.servicetype_help());
        controlURLTxt.setHelpText(T._.controlURLTxt_help());

        this.wanType = new JLabel();
        p.add(this.auto, "aligny top,gapright 15,sg buttons");
        p.add(new JLabel(T._.literally_router()), "");
        p.add(this.wanType, "spanx");
        p.add(this.find, "aligny top,gapright 15,newline,sg buttons");
        p.add(new JLabel(T._.literally_service_type()), "");
        p.add(this.serviceTypeTxt);
        p.add(new JLabel(T._.literally_control_url()), "newline,skip");
        p.add(this.controlURLTxt);

        p.add(Box.createGlue(), "pushy,growy");
        this.updateGUI();
        this.serviceTypeTxt.getDocument().addDocumentListener(new DocumentListener() {

            public void changedUpdate(final DocumentEvent e) {
                this.update();

            }

            public void insertUpdate(final DocumentEvent e) {
                this.update();

            }

            public void removeUpdate(final DocumentEvent e) {
                this.update();

            }

            private void update() {
                settings.setServiceType(UPNPRouterPlugin.this.serviceTypeTxt.getText());
                UPNPRouterPlugin.this.setCanCheckIP(true);
            }

        });
        this.controlURLTxt.getDocument().addDocumentListener(new DocumentListener() {

            public void changedUpdate(final DocumentEvent e) {
                this.update();

            }

            public void insertUpdate(final DocumentEvent e) {
                this.update();

            }

            public void removeUpdate(final DocumentEvent e) {
                this.update();

            }

            private void update() {
                settings.setControlURL(UPNPRouterPlugin.this.controlURLTxt.getText());
                UPNPRouterPlugin.this.setCanCheckIP(true);
            }

        });

        return p;
    }

    @Override
    public String getID() {
        return UPNPRouterPlugin.ID;
    }

    public int getIpCheckInterval() {
        return 1;
    }

    public IPCheckProvider getIPCheckProvider() {
        if (!this.isIPCheckEnabled()) { return null; }
        return this;
    }

    @Override
    public String getName() {
        return "UPNP - Universal Plug & Play (Fritzbox,...)";
    }

    @Override
    public int getWaittimeBeforeFirstIPCheck() {
        // if ipcheck is done over upnp, we do not have to use long intervals
        return 0;
    }

    public boolean isIPCheckEnabled() {
        return settings.isIPCheckEnabled();
    }

    public void setCanCheckIP(final boolean b) {
        settings.setIPCheckEnabled(b);

    }

    private void setDevice(final UpnpRouterDevice upnpRouterDevice) {
        if (upnpRouterDevice == null) {
            settings.setControlURL(null);
            settings.setModelName(null);
            settings.setIPCheckEnabled(false);
            settings.setServiceType(null);
            settings.setWANService(null);
        } else {
            JDLogger.getLogger().info(upnpRouterDevice + "");

            settings.setControlURL(upnpRouterDevice.getControlURL());
            settings.setModelName(upnpRouterDevice.getModelname());

            settings.setServiceType(upnpRouterDevice.getServiceType());
            settings.setWANService(upnpRouterDevice.getWanservice());

            this.setCanCheckIP(true);

            this.updateGUI();
        }
    }

    private void updateGUI() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {

                if (UPNPRouterPlugin.this.wanType != null) {
                    try {
                        UPNPRouterPlugin.this.wanType.setText(settings.getModelName() + (settings.getWANService().length() > 0 ? " (" + settings.getWANService() + ")" : ""));
                    } catch (final Throwable e) {
                    }
                    try {
                        UPNPRouterPlugin.this.serviceTypeTxt.setText(settings.getServiceType());
                    } catch (final Throwable e) {
                    }
                    try {
                        UPNPRouterPlugin.this.controlURLTxt.setText(settings.getControlURL());
                    } catch (final Throwable e) {
                    }
                    // final String ipcheckEnabled =
                    // UPNPRouterPlugin.this.isIPCheckEnabled() ?
                    // Loc.L("jd.controlling.reconnect.plugins.upnp.UPNPRouterPlugin.text.yes",
                    // "Yes") :
                    // Loc.L("jd.controlling.reconnect.plugins.upnp.UPNPRouterPlugin.text.no",
                    // "No");

                }
            }

        };
    }

    @Override
    public ReconnectInvoker getReconnectInvoker() {
        return new UPNPReconnectInvoker(this, settings.getServiceType(), settings.getControlURL());
    }

    public synchronized ArrayList<UpnpRouterDevice> getDevices() throws InterruptedException {
        if (devices == null) {
            devices = UPNPScanner.scanDevices();
        }
        return devices;
    }
}