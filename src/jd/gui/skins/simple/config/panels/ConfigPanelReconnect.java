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

package jd.gui.skins.simple.config.panels;

import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.config.ConfigEntry.PropertyType;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.controlling.reconnect.BatchReconnect;
import jd.controlling.reconnect.ExternReconnect;
import jd.controlling.reconnect.HTTPLiveHeader;
import jd.controlling.reconnect.ReconnectMethod;
import jd.controlling.reconnect.Reconnecter;
import jd.gui.skins.simple.Factory;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.config.ConfigEntriesPanel;
import jd.gui.skins.simple.config.ConfigPanel;
import jd.gui.skins.simple.config.GUIConfigEntry;
import jd.gui.skins.simple.config.subpanels.SubPanelCLRReconnect;
import jd.gui.skins.simple.config.subpanels.SubPanelLiveHeaderReconnect;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class ConfigPanelReconnect extends ConfigPanel implements ActionListener {

    private static final long serialVersionUID = 3383448498625377495L;

    private JButton btn;

    private Configuration configuration;

    private JTabbedPane tabbed;

    private JLabel currentip;

    private JLabel success;

    private JLabel beforeIPLabel;

    private JLabel beforeIP;

    private JLabel message;

    private JLabel timeLabel;

    private JLabel time;

    public ConfigPanelReconnect(Configuration configuration) {
        super();
        this.configuration = configuration;
        initPanel();
        load();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btn) {
            save();

            JDLogger.addHeader("Reconnect Testing");

            final ProgressController progress = new ProgressController(JDLocale.L("gui.warning.reconnect.pleaseWait", "Bitte Warten...Reconnect läuft"), 100);

            logger.info("Start Reconnect");
            message.setText(JDLocale.L("gui.warning.reconnect.running", "running..."));
            message.setEnabled(true);
            beforeIP.setText(currentip.getText());
            beforeIP.setEnabled(true);
            beforeIPLabel.setEnabled(true);
            currentip.setText("?");
            final long timel = System.currentTimeMillis();

            final Thread timer = new Thread() {
                public void run() {
                    while (true) {
                        new GuiRunnable<Object>() {

                            @Override
                            public Object runSave() {
                                time.setText(JDUtilities.formatSeconds((System.currentTimeMillis() - timel) / 1000));
                                time.setEnabled(true);
                                timeLabel.setEnabled(true);
                                return null;
                            }

                        }.start();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                }
            };
            timer.start();
            JDUtilities.getConfiguration().setProperty(ReconnectMethod.PARAM_RETRIES, 0);
            progress.setStatus(30);
            new Thread() {
                @Override
                public void run() {
                    boolean tmp = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false);
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, false);
                    if (Reconnecter.waitForNewIP(1)) {
                        progress.setStatusText(JDLocale.L("gui.warning.reconnectSuccess", "Reconnect successfull"));

                        message.setText(JDLocale.L("gui.warning.reconnectSuccess", "Reconnect successfull"));
                        success.setIcon(JDTheme.II("gui.images.selected", 32, 32));
                        success.setEnabled(true);
                        currentip.setText(JDUtilities.getIPAddress(null));

                    } else {

                        progress.setStatusText(JDLocale.L("gui.warning.reconnectFailed", "Reconnect failed!"));

                        success.setIcon(JDTheme.II("gui.images.unselected", 32, 32));
                        success.setEnabled(true);
                        currentip.setText(JDUtilities.getIPAddress(null));
                        message.setText(JDLocale.L("gui.warning.reconnectFailed", "Reconnect failed!"));
                        if (JDUtilities.getController().getRunningDownloadNum() > 0) {
                            message.setText(JDLocale.L("gui.warning.reconnectFailedRunningDownloads", "Please stop all running Downloads first!"));
                            progress.setStatusText(JDLocale.L("gui.warning.reconnectFailedRunningDownloads", "Please stop all running Downloads first!"));
                        }
                        progress.setColor(Color.RED);
                    }

                    timer.interrupt();
                    progress.setStatus(100);
                    progress.finalize(5000);

                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, tmp);

                }
            }.start();
        }
    }

    @Override
    public void initPanel() {
        /* 0=LiveHeader, 1=Extern, 2=Batch,3=CLR */

        // types = new String[] { , , };
        panel.setLayout(new MigLayout("ins 10,wrap 2", "[fill,grow 10]10[fill,grow]"));
        ConfigGroup group = new ConfigGroup(JDLocale.L("gui.config.reconnect.shared", "General Reconnect Settings"), JDTheme.II("gui.images.reconnect_settings", 32, 32));
        this.addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), ReconnectMethod.PARAM_IPCHECKWAITTIME, JDLocale.L("reconnect.waitTimeToFirstIPCheck", "Wartezeit bis zum ersten IP-Check [sek]"), 0, 600).setDefaultValue(5).setGroup(group)));
        this.addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), ReconnectMethod.PARAM_RETRIES, JDLocale.L("reconnect.retries", "Max. Wiederholungen (-1 = unendlich)"), -1, 20).setDefaultValue(5).setGroup(group)));
        this.addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), ReconnectMethod.PARAM_WAITFORIPCHANGE, JDLocale.L("reconnect.waitForIp", "Auf neue IP warten [sek]"), 0, 600).setDefaultValue(20).setGroup(group)));

        this.addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, SubConfiguration.getConfig("DOWNLOAD"), "PARAM_DOWNLOAD_AUTORESUME_ON_RECONNECT", JDLocale.L("gui.config.download.autoresume", "Let Reconnects interrupt resumeable downloads")).setDefaultValue(true).setGroup(group)));
        panel.add(Factory.createHeader(new ConfigGroup(JDLocale.L("gui.config.reconnect.methods", "Reconnect Methods"), JDTheme.II("gui.images.reconnect_selection", 32, 32))), "spanx");

        panel.add(tabbed = new JTabbedPane(), "spanx,pushy,growy");
        tabbed.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        tabbed.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbed.setTabPlacement(SwingConstants.TOP);
        tabbed.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {

            }

        });

        addLiveheader();
        addExtern();
        addBatch();
        addCLR();
        tabbed.setSelectedIndex(configuration.getIntegerProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, ReconnectMethod.LIVEHEADER));
        panel.add(Factory.createHeader(new ConfigGroup(JDLocale.L("gui.config.reconnect.test", "Showcase"), JDTheme.II("gui.images.config.network_local", 32, 32))), "spanx,gaptop 15");
        JPanel p = new JPanel(new MigLayout(" ins 0,wrap 7", "[]5[fill]5[align right]20[align right]20[align right]20[align right]20[align right]", "[][]"));
        panel.add(p, "spanx,gapright 20");
        btn = new JButton(JDLocale.L("gui.config.reconnect.showcase.reconnect", "Change IP"));
        btn.addActionListener(this);
        p.add(btn, "spany, aligny top");
        p.add(new JPanel(), "height 32!,spany,alignx left,pushx");

        p.add(timeLabel = new JLabel(JDLocale.L("gui.config.reconnect.showcase.time", "Reconnect duration")));
        p.add(time = new JLabel("---"));

        timeLabel.setEnabled(false);
        time.setEnabled(false);
        p.add(new JLabel(JDLocale.L("gui.config.reconnect.showcase.currentip", "Your current IP")));
        p.add(currentip = new JLabel("---"));

        success = new JLabel(JDTheme.II("gui.images.selected", 32, 32));
        success.setEnabled(false);
        p.add(success, "spany,alignx right");

        p.add(message = new JLabel(JDLocale.L("gui.config.reconnect.showcase.message.none", "Not tested yet")), "spanx 2");
        message.setEnabled(false);

        p.add(beforeIPLabel = new JLabel(JDLocale.L("gui.config.reconnect.showcase.lastip", "Ip before reconnect")));
        p.add(beforeIP = new JLabel("---"));
        beforeIPLabel.setEnabled(false);
        beforeIP.setEnabled(false);

        new Thread() {
            public void run() {
                String ip = JDUtilities.getIPAddress(null);
                currentip.setText(ip);
            }
        }.start();

        //
        add(panel);

        // setReconnectType();
    }

    private void addCLR() {
        String name = JDLocale.L("modules.reconnect.types.clr", "CLR Script");

        tabbed.addTab(name, new SubPanelCLRReconnect(configuration));

    }

    private void addBatch() {
        String name = JDLocale.L("modules.reconnect.types.batch", "Batch");

        tabbed.addTab(name, new ConfigEntriesPanel(new BatchReconnect().getConfig()));

    }

    private void addExtern() {
        String name = JDLocale.L("modules.reconnect.types.extern", "Extern");

        tabbed.addTab(name, new ConfigEntriesPanel(new ExternReconnect().getConfig()));

    }

    private void addLiveheader() {
        String name = JDLocale.L("modules.reconnect.types.liveheader", "LiveHeader/Curl");

        tabbed.addTab(name, new SubPanelLiveHeaderReconnect(configuration, new HTTPLiveHeader()));

    }

    @Override
    public void load() {
        loadConfigEntries();
    }

    public ConfigEntry.PropertyType hasChanges() {
        ConfigEntry.PropertyType ret = tabbed.getSelectedIndex() != configuration.getIntegerProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, ReconnectMethod.LIVEHEADER) ? PropertyType.NORMAL : PropertyType.NONE;
        return PropertyType.getMax(ret, super.hasChanges(), ((ConfigPanel) tabbed.getSelectedComponent()).hasChanges());
    }

    @Override
    public void save() {

        saveConfigEntries();
        configuration.setProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, tabbed.getSelectedIndex());
        ((ConfigPanel) tabbed.getSelectedComponent()).save();
        ((ConfigPanel) tabbed.getSelectedComponent()).saveConfigEntries();

    }

    // public PropertyType hasChanges() {
    //
    // PropertyType ret = PropertyType.getMax(super.hasChanges(),
    // cep.hasChanges());
    //
    // if (lh != null) {
    //
    // return lh.hasChanges().getMax(ret); }
    // if (er != null) { return er.hasChanges().getMax(ret); }
    // if (lhclr != null) { return lhclr.hasChanges().getMax(ret); }
    // return ret;
    // }

    // private void setReconnectType() {
    // if (lh != null) {
    // container.remove(lh);
    // lh = null;
    // } else if (er != null) {
    // container.remove(er);
    // er = null;
    // } else if (lhclr != null) {
    // container.remove(lhclr);
    // lhclr = null;
    // }
    // container.setBorder(BorderFactory.createTitledBorder(types[box.
    // getSelectedIndex()]));
    // switch (box.getSelectedIndex()) {
    // case 0:
    //           
    //
    // container.add(lh);
    // break;
    // case 1:
    //           
    //
    // container.add(er);
    // break;
    // case 2:
    //           
    //
    // container.add(er);
    // break;
    // case 3:
    //          
    //
    // container.add(lhclr);
    // break;
    // }
    //
    // validate();
    // }
}
