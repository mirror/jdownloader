//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.jdgui.views.settings.panels.reconnect;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.dialog.ImportRouterDialog;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.gui.swing.jdgui.views.settings.GUIConfigEntry;
import jd.nrouter.RouterUtils;
import jd.nrouter.recorder.Gui;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class SubPanelLiveHeaderReconnect extends ConfigPanel implements ActionListener {

    private static final long serialVersionUID = 6710420298517566329L;

    private Configuration configuration;

    private JButton btnSelectRouter;
    private JButton btnFindIP;
    private JButton btnRouterRecorder;

    private ConfigEntry ip;

    public SubPanelLiveHeaderReconnect(Configuration configuration) {
        super();

        this.configuration = configuration;

        init();
        this.setBorder(null);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnFindIP) {
            RouterUtils.findIP(((GUIConfigEntry) ip.getGuiListener()), false);
        } else if (e.getSource() == btnRouterRecorder) {
            if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                UserIO.getInstance().requestMessageDialog(UserIO.ICON_WARNING, JDL.L("jd.gui.swing.jdgui.settings.panels.downloadandnetwork.advanced.ipcheckdisable.warning.title", "IP-Check disabled!"), JDL.L("jd.gui.swing.jdgui.settings.panels.downloadandnetwork.advanced.ipcheckdisable.warning.message", "You disabled the IP-Check. This will increase the reconnection times dramatically!\r\n\r\nSeveral further modules like Reconnect Recorder are disabled."));
            } else {
                new Thread() {
                    @Override
                    public void run() {
                        String text = ip.getGuiListener().getText().toString();
                        if (text == null || text.trim().equals("")) {
                            RouterUtils.findIP((GUIConfigEntry) ip.getGuiListener(), true);
                        }

                        new GuiRunnable<Object>() {

                            @Override
                            public Object runSave() {
                                save();
                                Gui jd = new Gui(configuration.getStringProperty(Configuration.PARAM_HTTPSEND_IP, null));
                                if (jd.saved) {
                                    configuration.setProperty(Configuration.PARAM_HTTPSEND_IP, jd.ip);
                                    if (jd.user != null) configuration.setProperty(Configuration.PARAM_HTTPSEND_USER, jd.user);
                                    if (jd.pass != null) configuration.setProperty(Configuration.PARAM_HTTPSEND_PASS, jd.pass);
                                    configuration.setProperty(Configuration.PARAM_HTTPSEND_REQUESTS, jd.methode);
                                    configuration.save();
                                }
                                load();
                                return null;
                            }

                        }.start();

                    }
                }.start();
            }
        } else if (e.getSource() == btnSelectRouter) {
            String[] data = ImportRouterDialog.showDialog();
            if (data != null) {
                save();

                if (data[2].toLowerCase().indexOf("curl") >= 0) {
                    UserIO.getInstance().requestMessageDialog(JDL.L("gui.config.liveHeader.warning.noCURLConvert", "JD could not convert this curl-batch to a Live-Header Script. Please consult your JD-Support Team!"));
                }
                configuration.setProperty(Configuration.PARAM_HTTPSEND_REQUESTS, data[2]);
                String username = configuration.getStringProperty(Configuration.PARAM_HTTPSEND_USER, null);
                if (username == null || username.matches("[\\s]*")) {
                    configuration.setProperty(Configuration.PARAM_HTTPSEND_USER, data[4]);
                }
                String pw = configuration.getStringProperty(Configuration.PARAM_HTTPSEND_PASS, null);
                if (pw == null || pw.matches("[\\s]*")) {
                    configuration.setProperty(Configuration.PARAM_HTTPSEND_PASS, data[5]);
                }
                configuration.save();

                load();
            }
        }
    }

    @Override
    protected ConfigContainer setupContainer() {
        btnSelectRouter = new JButton(JDL.L("gui.config.liveheader.selectrouter", "Select Router"));
        btnSelectRouter.addActionListener(this);

        btnFindIP = new JButton(JDL.L("gui.config.liveheader.btnfindip", "Fetch Router IP"));
        btnFindIP.addActionListener(this);

        btnRouterRecorder = new JButton(JDL.L("gui.config.liveheader.recorder", "Create Reconnect Script"));
        btnRouterRecorder.addActionListener(this);

        JPanel buttons = new JPanel(new MigLayout("ins 0"));
        buttons.add(btnSelectRouter, "sizegroup btns");
        buttons.add(btnFindIP, "sizegroup btns");
        buttons.add(btnRouterRecorder, "sizegroup btns");

        ConfigContainer container = new ConfigContainer();

        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMPONENT, buttons, ""));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, configuration, Configuration.PARAM_HTTPSEND_USER, JDL.L("gui.config.httpliveheader.user", "User")));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, configuration, Configuration.PARAM_HTTPSEND_PASS, JDL.L("gui.config.httpliveheader.password", "Password")));
        container.addEntry(ip = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, configuration, Configuration.PARAM_HTTPSEND_IP, JDL.L("gui.config.httpliveheader.routerip", "Router's ip")));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, configuration, Configuration.PARAM_HTTPSEND_REQUESTS, null));

        return container;
    }

}