package jd.controlling.reconnect.plugins.liveheader;

import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.controlling.reconnect.ReconnectConfig;
import jd.controlling.reconnect.ReconnectInvoker;
import jd.controlling.reconnect.ReconnectPluginController;
import jd.controlling.reconnect.ReconnectResult;
import jd.controlling.reconnect.RouterPlugin;
import jd.controlling.reconnect.ipcheck.IP;
import jd.controlling.reconnect.plugins.liveheader.recorder.Gui;
import jd.controlling.reconnect.plugins.liveheader.remotecall.RouterData;
import jd.controlling.reconnect.plugins.liveheader.translate.T;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.ConfigEventListener;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.KeyHandler;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.event.ProcessCallBack;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.InputDialog;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.advanced.AdvancedConfigManager;

public class LiveHeaderReconnect extends RouterPlugin implements ControlListener, ConfigEventListener {

    private ExtTextField     txtUser;
    private ExtPasswordField txtPassword;

    private ExtTextField     txtIP;

    @Override
    public ImageIcon getIcon16() {
        return icon;
    }

    private ExtTextField                txtName;
    private ImageIcon                   icon;
    protected boolean                   dosession = true;
    private LiveHeaderReconnectSettings settings;

    protected static final Logger       LOG       = JDLogger.getLogger();
    public static final String          ID        = "httpliveheader";

    public LiveHeaderReconnect() {
        super();
        this.icon = NewTheme.I().getIcon("modem", 16);

        // only listen to system to autosend script
        JDController.getInstance().addControlListener(this);
        // Send routerscript if there were 3 successful recoinnects in a row
        JsonConfig.create(ReconnectConfig.class).getStorageHandler().getEventSender().addListener(this);
        settings = JsonConfig.create(LiveHeaderReconnectSettings.class);
        settings.getStorageHandler().getEventSender().addListener(this);
        AdvancedConfigManager.getInstance().register(JsonConfig.create(LiveHeaderReconnectSettings.class));

    }

    public void controlEvent(final ControlEvent event) {
        // if (event.getEventID() == ControlEvent.CONTROL_AFTER_RECONNECT &&
        // ReconnectPluginController.getInstance().getActivePlugin() == this &&
        // !this.getStorage().get("SENT", false)) {
        // final boolean rcOK =
        // JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_RECONNECT_OKAY,
        // true);
        // final int failCount =
        // JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_RECONNECT_FAILED_COUNTER,
        // 0);
        // if (failCount == 0 && rcOK) {
        // final int count = this.getStorage().get("SUCCESSCOUNT", 0) + 1;
        // this.getStorage().put("SUCCESSCOUNT", count);
        // if (count > 2) {
        // try {
        // RouterSender.getInstance().run();
        //
        // this.getStorage().put("SENT", true);
        //
        // } catch (final Exception e) {
        // e.printStackTrace();
        // }
        // }
        // } else {
        // this.getStorage().put("SUCCESSCOUNT", 0);
        // }
        //
        // }
    }

    void editScript() {

        final InputDialog dialog = new InputDialog(Dialog.STYLE_LARGE | Dialog.STYLE_HIDE_ICON, "Script Editor", "Please enter a Liveheader script below.", settings.getScript(), null, T._.jd_controlling_reconnect_plugins_liveheader_LiveHeaderReconnect_actionPerformed_save(), null);
        dialog.setPreferredSize(new Dimension(700, 400));
        // CLR Import
        // dialog.setLeftActions(new AbstractAction("Browser Scripts") {
        //
        // private static final long serialVersionUID = 1L;
        //
        // public void actionPerformed(final ActionEvent e) {
        //
        // final ImportRouterDialog importDialog = new
        // ImportRouterDialog(LiveHeaderReconnect.getLHScripts());
        // try {
        // Dialog.getInstance().showDialog(importDialog);
        // final String[] data = importDialog.getResult();
        //
        // if (data != null) {
        //
        // if (data[2].toLowerCase().indexOf("curl") >= 0) {
        // UserIO.getInstance().requestMessageDialog(T._.gui_config_liveHeader_warning_noCURLConvert());
        // }
        //
        // dialog.setDefaultMessage(data[2]);
        // settings.setRouterName(data[0] + " - " + data[1]);
        //
        // }
        // } catch (DialogClosedException e1) {
        // e1.printStackTrace();
        // } catch (DialogCanceledException e1) {
        // e1.printStackTrace();
        // }
        //
        // }
        // }
        // , new AbstractAction("Import CLR Script") {
        //
        // private static final long serialVersionUID = 1L;
        //
        // public void actionPerformed(final ActionEvent e) {
        //
        // final InputDialog clrDialog = new InputDialog(Dialog.STYLE_LARGE |
        // Dialog.STYLE_HIDE_ICON, "CLR Import",
        // "Please enter a Liveheader script below.", "", null, null, null);
        // clrDialog.setPreferredSize(new Dimension(500, 400));
        // try {
        // final String clr = Dialog.getInstance().showDialog(clrDialog);
        // if (clr == null) { return; }
        //
        // final String[] ret = CLRConverter.createLiveHeader(clr);
        // if (ret != null) {
        // settings.setRouterName(ret[0]);
        // dialog.setDefaultMessage(ret[1]);
        // }
        // } catch (DialogClosedException e1) {
        // e1.printStackTrace();
        // } catch (DialogCanceledException e1) {
        // e1.printStackTrace();
        // }
        //
        // }
        // }

        // );
        String newScript;
        try {
            newScript = Dialog.getInstance().showDialog(dialog);
            if (newScript != null) {
                settings.setScript(newScript);
            }
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }

    }

    @Override
    public JComponent getGUI() {
        final JPanel p = new JPanel(new MigLayout("ins 0 0 0 0,wrap 3", "[][][grow,fill]", ""));
        JButton btnAuto = new JButton(new AutoDetectAction());
        btnAuto.setHorizontalAlignment(SwingConstants.LEFT);
        // auto search is not ready yet
        // this.btnAuto.setEnabled(false);
        JButton btnRecord = new JButton(new ReconnectRecorderAction(this));
        btnRecord.setHorizontalAlignment(SwingConstants.LEFT);
        JButton btnFindIP = new JButton(new GetIPAction(this));
        btnFindIP.setHorizontalAlignment(SwingConstants.LEFT);
        JButton btnEditScript = new JButton(new EditScriptAction(this));
        btnEditScript.setHorizontalAlignment(SwingConstants.LEFT);
        this.txtUser = new ExtTextField();
        txtUser.setHelpText(T._.LiveHeaderReconnect_getGUI_help_user());
        txtUser.addFocusListener(new FocusListener() {

            public void focusLost(FocusEvent e) {
                settings.setUserName(LiveHeaderReconnect.this.txtUser.getText());
            }

            public void focusGained(FocusEvent e) {
            }
        });

        this.txtPassword = new ExtPasswordField() {
            protected void onChanged() {
                settings.setPassword(new String(LiveHeaderReconnect.this.txtPassword.getPassword()));
            }
        };
        txtPassword.setHelpText(T._.LiveHeaderReconnect_getGUI_help_password());
        this.txtIP = new ExtTextField();
        txtIP.setHelpText(T._.LiveHeaderReconnect_getGUI_help_ip());

        txtIP.addFocusListener(new FocusListener() {

            public void focusLost(FocusEvent e) {
                settings.setRouterIP(LiveHeaderReconnect.this.txtIP.getText());
            }

            public void focusGained(FocusEvent e) {
            }
        });

        this.txtName = new ExtTextField();
        txtName.setEditable(false);
        txtName.setBorder(null);
        SwingUtils.setOpaque(txtName, false);

        //

        p.add(btnAuto, "sg buttons,aligny top,newline,gapright 15");

        p.add(new JLabel(T._.literally_router_model()), "");
        p.add(this.txtName, "spanx");
        //
        p.add(btnFindIP, "sg buttons,aligny top,newline");
        p.add(new JLabel(T._.literally_router_ip()), "");
        p.add(this.txtIP, "spanx");
        //
        p.add(btnRecord, "sg buttons,aligny top,newline");
        p.add(new JLabel(T._.literally_username()), "");
        p.add(this.txtUser, "spanx");
        //
        p.add(btnEditScript, "sg buttons,aligny top,newline");
        p.add(new JLabel(T._.literally_password()), "");
        p.add(this.txtPassword, "spanx");
        //

        // p.add(new JLabel("Control URL"), "newline,skip");
        // p.add(this.controlURLTxt);
        // p.add(Box.createGlue(), "pushy,growy");
        this.updateGUI();
        return p;
    }

    @Override
    public String getID() {
        return LiveHeaderReconnect.ID;
    }

    @Override
    public String getName() {
        return "LiveHeader";
    }

    public void routerRecord() {

        if (JsonConfig.create(ReconnectConfig.class).isIPCheckGloballyDisabled()) {
            UserIO.getInstance().requestMessageDialog(UserIO.ICON_WARNING, T._.jd_gui_swing_jdgui_settings_panels_downloadandnetwork_advanced_ipcheckdisable_warning_title(), T._.jd_gui_swing_jdgui_settings_panels_downloadandnetwork_advanced_ipcheckdisable_warning_message());
        } else {
            new Thread() {
                @Override
                public void run() {
                    final String text = LiveHeaderReconnect.this.txtIP.getText().toString();
                    if (text == null || !IP.isValidRouterIP(text)) {
                        new GetIPAction(LiveHeaderReconnect.this).actionPerformed(null);
                    }

                    new GuiRunnable<Object>() {

                        @Override
                        public Object runSave() {

                            final Gui jd = new Gui(settings.getRouterIP());
                            try {
                                Dialog.getInstance().showDialog(jd);
                                if (jd.saved) {
                                    settings.setRouterIP(jd.ip);

                                    if (jd.user != null) {
                                        settings.setUserName(jd.user);
                                    }
                                    if (jd.pass != null) {
                                        settings.setPassword(jd.pass);

                                    }
                                    settings.setScript(jd.methode);
                                    setName("Router Recorder Custom Script");

                                }
                            } catch (DialogClosedException e) {
                                e.printStackTrace();
                            } catch (DialogCanceledException e) {
                                e.printStackTrace();
                            }

                            return null;
                        }

                    }.start();

                }
            }.start();
        }
    }

    public ArrayList<ReconnectResult> runDetectionWizard(ProcessCallBack processCallBack) throws InterruptedException {
        final LiveHeaderDetectionWizard wizard = new LiveHeaderDetectionWizard();
        try {
            return wizard.runOnlineScan(processCallBack);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    void updateGUI() {
        new EDTRunner() {
            protected void runInEDT() {
                try {
                    String str = "";
                    if (settings.getRouterData().getRouterName() != null) str += settings.getRouterData().getRouterName();
                    str = str.trim();

                    if (settings.getRouterData().getManufactor() != null && settings.getRouterData().getManufactor().length() > 0) {
                        if (str.length() > 0) str += " - ";
                        str += settings.getRouterData().getManufactor();
                    }
                    LiveHeaderReconnect.this.txtName.setText(str);
                } catch (final Throwable e) {
                    // throws an Throwable if the caller
                    // is a changelistener of this field's document
                }
                try {
                    LiveHeaderReconnect.this.txtIP.setText(settings.getRouterIP());
                } catch (final Throwable e) {
                    // throws an Throwable if the caller
                    // is a changelistener of this field's document
                }
                try {
                    LiveHeaderReconnect.this.txtPassword.setPassword(settings.getPassword().toCharArray());
                } catch (final Throwable e) {
                    // throws an Throwable if the caller
                    // is a changelistener of this field's document
                }
                try {
                    LiveHeaderReconnect.this.txtUser.setText(settings.getUserName());
                } catch (final Throwable e) {
                    // throws an Throwable if the caller
                    // is a changelistener of this field's document
                }

            }

        };

    }

    public void onConfigValidatorError(ConfigInterface config, Throwable validateException, KeyHandler methodHandler) {
    }

    public void onConfigValueModified(ConfigInterface config, String key, Object newValue) {
        if (config == settings) {
            System.out.println("Key: " + key + "=" + newValue);
            updateGUI();
        } else {
            if (dosession && ReconnectPluginController.getInstance().getActivePlugin() == this) {
                if (JsonConfig.create(ReconnectConfig.class).getSuccessCounter() > 3) {
                    new RouterSendAction(this).actionPerformed(null);
                    dosession = false;
                }
            }
        }

    }

    public void setSetup(ReconnectResult reconnectResult) {

        if (reconnectResult.getInvoker() instanceof LiveHeaderInvoker) {
            LiveHeaderInvoker i = (LiveHeaderInvoker) reconnectResult.getInvoker();
            RouterData rd = ((LiveHeaderReconnectResult) reconnectResult).getRouterData();
            rd.setRouterName(i.getName());
            settings.setRouterData(rd);
            settings.setPassword(i.getPass());
            settings.setUserName(i.getUser());
            settings.setRouterIP(i.getIp());
            settings.setScript(i.getScript());
            updateGUI();

        }
    }

    @Override
    public ReconnectInvoker getReconnectInvoker() {
        String script;

        script = settings.getScript();
        System.out.println("H");
        final String user = settings.getUserName();
        final String pass = settings.getPassword();
        final String ip = settings.getRouterIP();

        return new LiveHeaderInvoker(this, script, user, pass, ip, settings.getRouterData().getRouterName());

    }
}