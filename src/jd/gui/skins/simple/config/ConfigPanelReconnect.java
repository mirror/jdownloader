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

package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.LogRecord;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.ScrollPaneConstants;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.reconnect.BatchReconnect;
import jd.controlling.reconnect.ExternReconnect;
import jd.controlling.reconnect.HTTPLiveHeader;
import jd.controlling.reconnect.ReconnectMethod;
import jd.controlling.reconnect.Reconnecter;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.skins.simple.components.MiniLogDialog;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ConfigPanelReconnect extends ConfigPanel implements ActionListener, ControlListener {

    private static final long serialVersionUID = 3383448498625377495L;

    private JComboBox box;

    private JButton btn;

    private Configuration configuration;

    private ConfigEntriesPanel cep;

    private ConfigEntriesPanel er;

    private SubPanelLiveHeaderReconnect lh;

    private SubPanelCLRReconnect lhclr;

    private MiniLogDialog mld;

    public ConfigPanelReconnect(Configuration configuration) {
        super();
        this.configuration = configuration;
        initPanel();
        load();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == box) {
            configuration.setProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, box.getSelectedIndex());
            setReconnectType();
        } else if (e.getSource() == btn) {
            save();

            mld = new MiniLogDialog("Reconnect");
            mld.getBtnOK().setEnabled(false);
            mld.getBtnOK().setText(JDLocale.L("gui.warning.reconnect.pleaseWait", "Bitte Warten...Reconnect läuft"));
            mld.getProgress().setMaximum(100);
            mld.getProgress().setValue(2);
            JDUtilities.getController().addControlListener(this);

            logger.info("Start Reconnect");
            JDUtilities.getConfiguration().setProperty(ReconnectMethod.PARAM_RETRIES, 0);

            new Thread() {
                @Override
                public void run() {
                    boolean tmp = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false);
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, false);
                    if (Reconnecter.waitForNewIP(1)) {
                        mld.setText(JDLocale.L("gui.warning.reconnectSuccess", "Reconnect successfull") + "\r\n\r\n\r\n" + mld.getText());
                    } else {

                        mld.setText(JDLocale.L("gui.warning.reconnectFailed", "Reconnect failed!") + "\r\n\r\n\r\n" + mld.getText());
                        if (JDUtilities.getController().getRunningDownloadNum() > 0) {
                            mld.setText(JDLocale.L("gui.warning.reconnectFailedRunningDownloads", "Please stop all running Downloads first!") + "\r\n\r\n\r\n" + mld.getText());
                        }
                    }
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, tmp);
                    mld.getProgress().setValue(100);
                    mld.getBtnOK().setEnabled(true);
                    mld.getBtnOK().setText(JDLocale.L("gui.warning.reconnect.close", "Fenster schließen"));
                    JDUtilities.getController().removeControlListener(ConfigPanelReconnect.this);
                }
            }.start();
        }
    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_LOG_OCCURED && mld != null && mld.isEnabled()) {
            LogRecord l = (LogRecord) event.getParameter();

            if (l.getSourceClassName().startsWith("jd.controlling.interaction")) {
                mld.setText(JDUtilities.formatSeconds((int) l.getMillis() / 1000) + " : " + l.getMessage() + "\r\n" + mld.getText());
                mld.getScrollPane().setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
                mld.getProgress().setValue(mld.getProgress().getValue() + 1);
            }
        }

    }

    @Override
    public void initPanel() {
        box = new JComboBox(new String[] { JDLocale.L("modules.reconnect.types.liveheader", "LiveHeader/Curl"), JDLocale.L("modules.reconnect.types.extern", "Extern"), JDLocale.L("modules.reconnect.types.batch", "Batch"), JDLocale.L("modules.reconnect.types.clr", "CLR Script") });
        box.setSelectedIndex(configuration.getIntegerProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, 0));
        box.addActionListener(this);

        btn = new JButton(JDLocale.L("modules.reconnect.testreconnect", "Test Reconnect"));
        btn.addActionListener(this);

        ConfigContainer config = new ConfigContainer(this);
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), ReconnectMethod.PARAM_IPCHECKWAITTIME, JDLocale.L("reconnect.waitTimeToFirstIPCheck", "Wartezeit bis zum ersten IP-Check [sek]"), 0, 600).setDefaultValue(5));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), ReconnectMethod.PARAM_RETRIES, JDLocale.L("reconnect.retries", "Max. Wiederholungen (-1 = unendlich)"), -1, 20).setDefaultValue(5));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), ReconnectMethod.PARAM_WAITFORIPCHANGE, JDLocale.L("reconnect.waitForIp", "Auf neue IP warten [sek]"), 0, 600).setDefaultValue(20));
        cep = new ConfigEntriesPanel(config);

        JDUtilities.addToGridBag(panel, cep, 0, 0, 5, 1, 0, 0, new Insets(0, 0, 5, 0), GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);
        JDUtilities.addToGridBag(panel, new JLabel(JDLocale.L("modules.reconnect.pleaseSelect", "Bitte Methode auswählen:")), 0, 1, 1, 1, 0, 0, new Insets(0, 7, 5, 0), GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);
        JDUtilities.addToGridBag(panel, box, 1, 1, 1, 1, 0, 0, new Insets(0, 7, 5, 0), GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);
        JDUtilities.addToGridBag(panel, btn, 2, 1, 1, 1, 0, 0, new Insets(0, 10, 5, 0), GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);
        JDUtilities.addToGridBag(panel, new JSeparator(), 0, 2, 5, 1, 1, 1, new Insets(0, 7, 3, 0), GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);

        add(panel, BorderLayout.NORTH);

        setReconnectType();
    }

    @Override
    public void load() {
        loadConfigEntries();
    }

    @Override
    public void save() {
        saveConfigEntries();
        cep.save();
        cep.saveConfigEntries();
        if (lh != null) {
            lh.save();
            lh.saveConfigEntries();
        }
        if (er != null) {
            er.save();
            er.saveConfigEntries();
        }
        if (lhclr != null) {
            lhclr.save();
            lhclr.saveConfigEntries();
        }
    }

    private void setReconnectType() {
        if (lh != null) {
            panel.remove(lh);
            lh = null;
        } else if (er != null) {
            panel.remove(er);
            er = null;
        } else if (lhclr != null) {
            panel.remove(lhclr);
            lhclr = null;
        }

        switch (box.getSelectedIndex()) {
        case 0:
            lh = new SubPanelLiveHeaderReconnect(configuration, new HTTPLiveHeader());
            JDUtilities.addToGridBag(panel, lh, 0, 3, 5, 1, 1, 1, new Insets(0, 0, 0, 0), GridBagConstraints.BOTH, GridBagConstraints.NORTH);
            break;
        case 1:
            er = new ConfigEntriesPanel(new ExternReconnect().getConfig());
            JDUtilities.addToGridBag(panel, er, 0, 3, 5, 1, 1, 1, new Insets(0, 0, 0, 0), GridBagConstraints.BOTH, GridBagConstraints.NORTH);
            break;
        case 2:
            er = new ConfigEntriesPanel(new BatchReconnect().getConfig());
            JDUtilities.addToGridBag(panel, er, 0, 3, 5, 1, 1, 1, new Insets(0, 0, 0, 0), GridBagConstraints.BOTH, GridBagConstraints.NORTH);
            break;
        case 3:
            lhclr = new SubPanelCLRReconnect(configuration);
            JDUtilities.addToGridBag(panel, lhclr, 0, 3, 5, 1, 1, 1, new Insets(0, 0, 0, 0), GridBagConstraints.BOTH, GridBagConstraints.NORTH);
            break;
        }

        validate();
    }
}
