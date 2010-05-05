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
import javax.swing.JTextArea;
import javax.swing.JTextField;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.dialog.ImportRouterDialog;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.gui.swing.jdgui.views.settings.GUIConfigEntry;
import jd.router.FindRouterIP;
import jd.router.reconnectrecorder.Gui;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class SubPanelLiveHeaderReconnect extends ConfigPanel implements ActionListener {

    private static final long serialVersionUID = 6710420298517566329L;

    private Configuration configuration;

    private JButton btnFindIP;

    private JButton btnSelectRouter;

    private GUIConfigEntry ip;

    private GUIConfigEntry pass;

    private GUIConfigEntry user;

    private JButton btnRouterRecorder;

    private GUIConfigEntry script;

    public SubPanelLiveHeaderReconnect(Configuration configuration) {
        super();
        this.configuration = configuration;
        initPanel();
        load();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnFindIP) {
            new FindRouterIP(ip);
        } else if (e.getSource() == this.btnRouterRecorder) {
            if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                UserIO.getInstance().requestMessageDialog(UserIO.ICON_WARNING, JDL.L("jd.gui.swing.jdgui.settings.panels.downloadandnetwork.advanced.ipcheckdisable.warning.title", "IP-Check disabled!"), JDL.L("jd.gui.swing.jdgui.settings.panels.downloadandnetwork.advanced.ipcheckdisable.warning.message", "You disabled the IP-Check. This will increase the reconnection times dramatically!\r\n\r\nSeveral further modules like Reconnect Recorder are disabled."));
            } else {
                new Thread() {
                    @Override
                    public void run() {
                        if (((JTextField) ip.getInput()[0]).getText() == null || ((JTextField) ip.getInput()[0]).getText().trim().equals("")) {
                            Thread th = new Thread() {
                                @Override
                                public void run() {
                                    FindRouterIP.findIP(ip);
                                }
                            };
                            th.start();
                            while (th.isAlive()) {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    return;
                                }
                            }
                        }
                        new GuiRunnable<Object>() {

                            @Override
                            public Object runSave() {
                                Gui jd = new Gui(((JTextField) ip.getInput()[0]).getText());
                                if (jd.saved) {
                                    ((JTextField) ip.getInput()[0]).setText(jd.ip);
                                    if (jd.user != null) ((JTextField) user.getInput()[0]).setText(jd.user);
                                    if (jd.pass != null) ((JTextField) pass.getInput()[0]).setText(jd.pass);
                                    ((JTextArea) script.getInput()[0]).setText(jd.methode);
                                }
                                return null;
                            }

                        }.start();

                    }
                }.start();
            }
        } else if (e.getSource() == btnSelectRouter) {
            String[] data = ImportRouterDialog.showDialog();
            if (data != null) {
                if (data[2].toLowerCase().indexOf("curl") >= 0) {
                    UserIO.getInstance().requestMessageDialog(JDL.L("gui.config.liveHeader.warning.noCURLConvert", "JD could not convert this curl-batch to a Live-Header Script. Please consult your JD-Support Team!"));
                }
                script.setData(data[2]);
                String username = (String) user.getText();
                if (username == null || username.matches("[\\s]*")) {
                    user.setData(data[4]);
                }
                String pw = (String) pass.getText();
                if (pw == null || pw.matches("[\\s]*")) {
                    pass.setData(data[5]);
                }
            }
        }
    }

    @Override
    public void initPanel() {
        JPanel buttons = new JPanel(new MigLayout("ins 0"));

        btnSelectRouter = new JButton(JDL.L("gui.config.liveheader.selectrouter", "Select Router"));
        btnSelectRouter.addActionListener(this);
        buttons.add(btnSelectRouter, "sizegroup btns");

        btnFindIP = new JButton(JDL.L("gui.config.liveheader.btnfindip", "Fetch Router IP"));
        btnFindIP.addActionListener(this);
        buttons.add(btnFindIP, "sizegroup btns");

        btnRouterRecorder = new JButton(JDL.L("gui.config.liveheader.recorder", "Create Reconnect Script"));
        btnRouterRecorder.addActionListener(this);
        buttons.add(btnRouterRecorder, "sizegroup btns");

        panel.setLayout(new MigLayout("ins 5, wrap 2", "[fill]10[grow,fill]"));
        panel.add(buttons, "spanx");

        addGUIConfigEntry(user = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, configuration, Configuration.PARAM_HTTPSEND_USER, JDL.L("gui.config.httpliveheader.user", "User"))));
        addGUIConfigEntry(pass = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, configuration, Configuration.PARAM_HTTPSEND_PASS, JDL.L("gui.config.httpliveheader.password", "Password"))));
        addGUIConfigEntry(ip = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, configuration, Configuration.PARAM_HTTPSEND_IP, JDL.L("gui.config.httpliveheader.routerip", "Router's ip"))));
        addGUIConfigEntry(script = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, configuration, Configuration.PARAM_HTTPSEND_REQUESTS, null)));

        this.setBorder(null);
        this.add(panel);
    }

}