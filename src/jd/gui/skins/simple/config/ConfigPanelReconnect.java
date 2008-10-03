//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import jd.config.Configuration;
import jd.controlling.interaction.BatchReconnect;
import jd.controlling.interaction.ExternReconnect;
import jd.controlling.interaction.HTTPLiveHeader;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.UIInterface;
import jd.gui.skins.simple.components.MiniLogDialog;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.Reconnecter;

public class ConfigPanelReconnect extends ConfigPanel implements ActionListener, ControlListener {

    private static final long serialVersionUID = 3383448498625377495L;

    private JComboBox box;

    private JButton btn;

    private Configuration configuration;

    private ConfigEntriesPanel er;

    private SubPanelLiveHeaderReconnect lh;
    private SubPanelCLRReconnect lhclr;

    private MiniLogDialog mld;

    public ConfigPanelReconnect(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        this.configuration = configuration;
        initPanel();
        load();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == box) {
            configuration.setProperty(Configuration.PARAM_RECONNECT_TYPE, box.getSelectedItem());
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
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_RETRIES, 0);
            JDUtilities.getSubConfig("BATCHRECONNECT").setProperty(BatchReconnect.PARAM_RETRIES, 0);
            JDUtilities.getConfiguration().setProperty(ExternReconnect.PARAM_RETRIES, 0);

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
    public String getName() {
        return JDLocale.L("gui.config.reconnect.name", "Reconnect");
    }

    @Override
    public void initPanel() {
        box = new JComboBox(new String[] { JDLocale.L("modules.reconnect.types.liveheader", "LiveHeader/Curl"), JDLocale.L("modules.reconnect.types.extern", "Extern"), JDLocale.L("modules.reconnect.types.batch", "Batch"), JDLocale.L("modules.reconnect.types.clr", "CLR Script") });
        box.setSelectedItem(configuration.getStringProperty(Configuration.PARAM_RECONNECT_TYPE, JDLocale.L("modules.reconnect.types.liveheader", "LiveHeader/Curl")));
        box.addActionListener(this);

        btn = new JButton(JDLocale.L("modules.reconnect.testreconnect", "Test Reconnect"));
        btn.addActionListener(this);

        JDUtilities.addToGridBag(panel, new JLabel(JDLocale.L("modules.reconnect.pleaseSelect", "Bitte Methode auswählen:")), 0, 0, 1, 1, 0, 0, new Insets(0, 7, 5, 0), GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);
        JDUtilities.addToGridBag(panel, box, 1, 0, 1, 1, 0, 0, new Insets(0, 7, 5, 0), GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);
        JDUtilities.addToGridBag(panel, btn, 2, 0, 1, 1, 0, 0, new Insets(0, 10, 5, 0), GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);
        JDUtilities.addToGridBag(panel, new JSeparator(), 0, 1, 5, 1, 1, 1, new Insets(0, 7, 3, 0), GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);

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

        if (lh != null) panel.remove(lh);
        if (er != null) panel.remove(er);
        if (lhclr != null) panel.remove(lhclr);
        lh = null;
        er = null;
        lhclr = null;

        switch (box.getSelectedIndex()) {
        case 0:
            lh = new SubPanelLiveHeaderReconnect(uiinterface, new HTTPLiveHeader());
            JDUtilities.addToGridBag(panel, lh, 0, 2, 5, 1, 1, 1, new Insets(0, 0, 0, 0), GridBagConstraints.BOTH, GridBagConstraints.NORTH);
            break;
        case 1:
            er = new ConfigEntriesPanel(new ExternReconnect().getConfig());
            JDUtilities.addToGridBag(panel, er, 0, 2, 5, 1, 1, 1, new Insets(0, 0, 0, 0), GridBagConstraints.BOTH, GridBagConstraints.NORTH);
            break;
        case 2:
            er = new ConfigEntriesPanel(new BatchReconnect().getConfig());
            JDUtilities.addToGridBag(panel, er, 0, 2, 5, 1, 1, 1, new Insets(0, 0, 0, 0), GridBagConstraints.BOTH, GridBagConstraints.NORTH);
            break;
        case 3:
            lhclr = new SubPanelCLRReconnect(uiinterface, new HTTPLiveHeader());
            JDUtilities.addToGridBag(panel, lhclr, 0, 2, 5, 1, 1, 1, new Insets(0, 0, 0, 0), GridBagConstraints.BOTH, GridBagConstraints.NORTH);
            break;
        }

        validate();
    }
}
