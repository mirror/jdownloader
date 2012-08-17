package jd.controlling.reconnect.pluginsinc.upnp;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import jd.controlling.reconnect.ReconnectConfig;
import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.ReconnectInvoker;
import jd.controlling.reconnect.ReconnectResult;
import jd.controlling.reconnect.RouterPlugin;
import jd.controlling.reconnect.ipcheck.IP;
import jd.controlling.reconnect.ipcheck.IPCheckException;
import jd.controlling.reconnect.ipcheck.IPCheckProvider;
import jd.controlling.reconnect.ipcheck.InvalidIPRangeException;
import jd.controlling.reconnect.ipcheck.InvalidProviderException;
import jd.controlling.reconnect.pluginsinc.upnp.translate.T;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.event.ProcessCallBack;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.advanced.AdvancedConfigManager;

public class UPNPRouterPlugin extends RouterPlugin implements IPCheckProvider {

    public static final String            ID = "SIMPLEUPNP";

    private ExtTextField                  serviceTypeTxt;
    private ExtTextField                  controlURLTxt;
    private JLabel                        wanType;

    protected java.util.List<UpnpRouterDevice> devices;

    private ImageIcon                     icon;

    private UPUPReconnectSettings         settings;

    public UPNPRouterPlugin() {
        super();
        icon = NewTheme.I().getIcon("upnp", 16);
        settings = JsonConfig.create(UPUPReconnectSettings.class);
        AdvancedConfigManager.getInstance().register(settings);

    }

    /**
     * sets the correct router settings automatically
     * 
     * @throws InterruptedException
     */
    @Override
    public java.util.List<ReconnectResult> runDetectionWizard(ProcessCallBack processCallBack) throws InterruptedException {
        LogSource logger = LogController.getInstance().getLogger("UPNPReconnect");
        try {
            java.util.List<ReconnectResult> ret = new ArrayList<ReconnectResult>();
            java.util.List<UpnpRouterDevice> devices = getDevices();
            logger.info("Found devices: " + devices);
            for (int i = 0; i < devices.size(); i++) {
                UpnpRouterDevice device = devices.get(i);
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException();

                ReconnectResult res;
                try {
                    processCallBack.setStatusString(this, T._.try_reconnect(device.getFriendlyname() == null ? device.getModelname() : device.getFriendlyname()));
                    logger.info("Try " + device);
                    res = new UPNPReconnectInvoker(this, device.getServiceType(), device.getControlURL()).validate();
                    logger.info("REsult " + res);
                    if (res != null && res.isSuccess()) {
                        ret.add(res);
                        processCallBack.setStatus(this, ret);
                        if (i < devices.size() - 1) {

                            if (ret.size() == 1) Dialog.getInstance().showConfirmDialog(0, _GUI._.LiveHeaderDetectionWizard_testList_firstSuccess_title(), _GUI._.LiveHeaderDetectionWizard_testList_firstsuccess_msg(TimeFormatter.formatMilliSeconds(res.getSuccessDuration(), 0)), NewTheme.I().getIcon("ok", 32), _GUI._.LiveHeaderDetectionWizard_testList_ok(), _GUI._.LiveHeaderDetectionWizard_testList_use());

                        }
                    }

                } catch (ReconnectException e) {
                    e.printStackTrace();
                } catch (DialogClosedException e) {

                } catch (DialogCanceledException e) {
                    return ret;
                }

            }
            return ret;
        } finally {
            logger.close();
        }

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

        JsonConfig.create(ReconnectConfig.class).setSecondsBeforeFirstIPCheck((int) reconnectResult.getOfflineDuration() / 1000);
        JsonConfig.create(ReconnectConfig.class).setSecondsToWaitForIPChange((int) (reconnectResult.getMaxSuccessDuration()) / 1000);
        JsonConfig.create(ReconnectConfig.class).setSecondsToWaitForOffline((int) reconnectResult.getMaxOfflineDuration() / 1000);
        updateGUI();
    }

    @Override
    public JComponent getGUI() {
        final JPanel p = new JPanel(new MigLayout("ins 0 0 0 0,wrap 3", "[][][grow,fill]", "[fill]"));
        p.setOpaque(false);
        JButton find = new ExtButton(new UPNPScannerAction(this)).setTooltipsEnabled(true);
        find.setHorizontalAlignment(SwingConstants.LEFT);
        JButton auto = new ExtButton(new AutoDetectUpnpAction(this)).setTooltipsEnabled(true);
        auto.setHorizontalAlignment(SwingConstants.LEFT);
        this.serviceTypeTxt = new ExtTextField() {

            @Override
            public void onChanged() {
                settings.setServiceType(UPNPRouterPlugin.this.serviceTypeTxt.getText());
                UPNPRouterPlugin.this.setCanCheckIP(true);
            }

        };
        this.controlURLTxt = new ExtTextField() {

            @Override
            public void onChanged() {
                settings.setControlURL(UPNPRouterPlugin.this.controlURLTxt.getText());
                UPNPRouterPlugin.this.setCanCheckIP(true);
            }

        };

        serviceTypeTxt.setHelpText(T._.servicetype_help());
        controlURLTxt.setHelpText(T._.controlURLTxt_help());

        this.wanType = new JLabel();
        p.add(auto, "aligny top,gapright 15,sg buttons");
        p.add(new JLabel(T._.literally_router()), "");
        p.add(this.wanType, "spanx");
        p.add(find, "aligny top,gapright 15,newline,sg buttons");
        p.add(new JLabel(T._.literally_service_type()), "");
        p.add(this.serviceTypeTxt);
        p.add(new JLabel(T._.literally_control_url()), "newline,skip");
        p.add(this.controlURLTxt);

        // p.add(Box.createGlue(), "pushy,growy");
        this.updateGUI();

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

    void setDevice(final UpnpRouterDevice upnpRouterDevice) {
        if (upnpRouterDevice == null) {
            settings.setControlURL(null);
            settings.setModelName(null);
            settings.setIPCheckEnabled(false);
            settings.setServiceType(null);
            settings.setWANService(null);
        } else {
            LogController.CL().info(upnpRouterDevice + "");

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

    public synchronized java.util.List<UpnpRouterDevice> getDevices() throws InterruptedException {
        if (devices == null || devices.size() == 0) {
            // upnp somtimes works, sometimes not - no idea why. that's why we
            // do a scan if we have no responses
            devices = UPNPScanner.scanDevices();
        }
        return devices;
    }
}