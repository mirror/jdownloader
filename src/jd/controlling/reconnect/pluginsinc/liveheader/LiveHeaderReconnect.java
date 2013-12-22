package jd.controlling.reconnect.pluginsinc.liveheader;

import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import jd.controlling.reconnect.ReconnectConfig;
import jd.controlling.reconnect.ReconnectInvoker;
import jd.controlling.reconnect.ReconnectPluginController;
import jd.controlling.reconnect.ReconnectResult;
import jd.controlling.reconnect.RouterPlugin;
import jd.controlling.reconnect.ipcheck.IP;
import jd.controlling.reconnect.pluginsinc.liveheader.recorder.Gui;
import jd.controlling.reconnect.pluginsinc.liveheader.remotecall.RouterData;
import jd.controlling.reconnect.pluginsinc.liveheader.translate.T;
import jd.gui.UserIO;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.ConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.swing.components.ExtTextField;
import org.appwork.uio.UIOManager;
import org.appwork.utils.event.ProcessCallBack;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.InputDialog;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.advanced.AdvancedConfigManager;

public class LiveHeaderReconnect extends RouterPlugin implements ConfigEventListener {

    private ExtTextField     txtUser;
    private ExtPasswordField txtPassword;

    private ExtTextField     txtIP;

    @Override
    public ImageIcon getIcon16() {
        return icon;
    }

    private ExtTextField                txtName;
    private ImageIcon                   icon;

    private LiveHeaderReconnectSettings settings;

    public static final String          ID = "httpliveheader";

    public LiveHeaderReconnect() {
        super();
        this.icon = NewTheme.I().getIcon("modem", 16);

        // only listen to system to autosend script
        // Send routerscript if there were 3 successful recoinnects in a row
        JsonConfig.create(ReconnectConfig.class)._getStorageHandler().getEventSender().addListener(this);
        settings = JsonConfig.create(LiveHeaderReconnectSettings.class);
        settings._getStorageHandler().getEventSender().addListener(this);
        AdvancedConfigManager.getInstance().register(JsonConfig.create(LiveHeaderReconnectSettings.class));

    }

    void editScript() {

        final InputDialog dialog = new InputDialog(Dialog.STYLE_LARGE | Dialog.STYLE_HIDE_ICON, "Script Editor", "Please enter a Liveheader script below.", settings.getScript(), NewTheme.I().getIcon("edit", 18), T._.jd_controlling_reconnect_plugins_liveheader_LiveHeaderReconnect_actionPerformed_save(), null);
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

                // changed script.reset router sender state
                if (settings.getScript() == null || newScript.equals(settings.getScript())) settings.setAlreadySendToCollectServer2(false);
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
        p.setOpaque(false);
        // auto search is not ready yet
        // this.btnAuto.setEnabled(false);

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
            public void onChanged() {
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

        p.add(createButton(new RouterSendAction(this)), "sg buttons,aligny top,newline");
        p.add(new JLabel(T._.literally_router_model()), "");
        p.add(this.txtName, "spanx");
        //
        p.add(createButton(new GetIPAction(this)), "sg buttons,aligny top,newline");
        p.add(new JLabel(T._.literally_router_ip()), "");
        p.add(this.txtIP, "spanx");
        //
        p.add(createButton(new ReconnectRecorderAction(this)), "sg buttons,aligny top,newline");
        p.add(new JLabel(T._.literally_username()), "");
        p.add(this.txtUser, "spanx");
        //
        p.add(createButton(new EditScriptAction(this)), "sg buttons,aligny top,newline");
        p.add(new JLabel(T._.literally_password()), "");
        p.add(this.txtPassword, "spanx");
        //

        // p.add(new JLabel("Control URL"), "newline,skip");
        // p.add(this.controlURLTxt);
        // p.add(Box.createGlue(), "pushy,growy");
        this.updateGUI();
        return p;
    }

    private JButton createButton(AbstractAction autoDetectAction) {
        ExtButton ret = new ExtButton(autoDetectAction);
        ret.setHorizontalAlignment(SwingConstants.LEFT);
        ret.setTooltipsEnabled(true);
        return ret;
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

                    new EDTHelper<Object>() {

                        @Override
                        public Object edtRun() {

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
                                    // changed script.reset router sender state
                                    if ((jd.methode != null && jd.methode.equals(settings.getScript()))) settings.setAlreadySendToCollectServer2(false);
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

    public java.util.List<ReconnectResult> runDetectionWizard(ProcessCallBack processCallBack) throws InterruptedException {
        final LiveHeaderDetectionWizard wizard = new LiveHeaderDetectionWizard();
        try {
            return wizard.runOnlineScan(processCallBack);
        } catch (InterruptedException e) {
            throw e;
        } catch (Throwable e) {
            LogController.CL().log(e);
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

    public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
    }

    public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
        if (keyHandler.isChildOf(settings)) {

            updateGUI();
            if (!keyHandler.getKey().equalsIgnoreCase("AlreadySendToCollectServer")) {
                settings.setAlreadySendToCollectServer2(false);
            }
        } else {
            LogController.CL().info("Successful reonnects in a row: " + JsonConfig.create(ReconnectConfig.class).getSuccessCounter());
            if (!settings.isAlreadySendToCollectServer2() && ReconnectPluginController.getInstance().getActivePlugin() == this) {
                if (JsonConfig.create(ReconnectConfig.class).getSuccessCounter() > 3) {

                    UIOManager.I().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, T._.LiveHeaderReconnect_onConfigValueModified_ask_title(), T._.LiveHeaderReconnect_onConfigValueModified_ask_msg(), icon, null, null);
                    new RouterSendAction(this).actionPerformed(null);

                    settings.setAlreadySendToCollectServer2(true);

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
            // changed script.reset router sender state
            if (i.getScript() != null && i.getScript().equals(JsonConfig.create(LiveHeaderReconnectSettings.class).getScript())) JsonConfig.create(LiveHeaderReconnectSettings.class).setAlreadySendToCollectServer2(false);

            settings.setScript(i.getScript());

            JsonConfig.create(ReconnectConfig.class).setSecondsBeforeFirstIPCheck((int) reconnectResult.getOfflineDuration() / 1000);
            JsonConfig.create(ReconnectConfig.class).setSecondsToWaitForIPChange((int) (reconnectResult.getMaxSuccessDuration() / 1000));
            JsonConfig.create(ReconnectConfig.class).setSecondsToWaitForOffline((int) reconnectResult.getMaxOfflineDuration() / 1000);
            updateGUI();

        }
    }

    @Override
    public ReconnectInvoker getReconnectInvoker() {
        String script;
        script = settings.getScript();
        if (script == null) return null;
        final String user = settings.getUserName();
        final String pass = settings.getPassword();
        final String ip = settings.getRouterIP();
        return new LiveHeaderInvoker(this, script, user, pass, ip, settings.getRouterData().getRouterName());

    }
}