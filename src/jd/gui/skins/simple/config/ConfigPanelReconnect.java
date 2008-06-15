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
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.ScrollPaneConstants;

import jd.config.Configuration;
import jd.controlling.interaction.BatchReconnect;
import jd.controlling.interaction.ExternReconnect;
import jd.controlling.interaction.HTTPLiveHeader;
import jd.controlling.interaction.Interaction;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.UIInterface;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.MiniLogDialog;
import jd.router.GetRouterInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.Reconnecter;

public class ConfigPanelReconnect extends ConfigPanel implements ActionListener, ControlListener {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3383448498625377495L;

    private Configuration configuration;

    private JComboBox box;

    private SubPanelLiveHeaderReconnect lh;

    private ConfigPanelDefault er;

    private JButton btn;

    private StringBuffer buffer;

    private MiniLogDialog mld;

    public ConfigPanelReconnect(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        this.configuration = configuration;
        initPanel();
        load();
    }

    public void save() {
        this.saveConfigEntries();
        if (lh != null) lh.save();
        if (lh != null) lh.saveConfigEntries();
        if (er != null) er.save();
        if (er != null) er.saveConfigEntries();
    }

    @Override
    public void initPanel() {
        String reconnectType = configuration.getStringProperty(Configuration.PARAM_RECONNECT_TYPE, JDLocale.L("modules.reconnect.types.liveheader", "LiveHeader/Curl"));
        JPanel p = new JPanel();

        box = new JComboBox(new String[] { JDLocale.L("modules.reconnect.types.liveheader", "LiveHeader/Curl"), JDLocale.L("modules.reconnect.types.extern", "Extern"), JDLocale.L("modules.reconnect.types.batch", "Batch") });
        box.addActionListener(this);
        p.add(new JLabel(JDLocale.L("modules.reconnect.pleaseSelect", "Bitte Methode auswählen:")));
        p.add(box);
        
        JDUtilities.addToGridBag(panel,new JLabel(JDLocale.L("modules.reconnect.pleaseSelect", "Bitte Methode auswählen:")),0,0,1,1,0,0,new Insets(0,7,5,0),GridBagConstraints.NONE, GridBagConstraints.NORTHWEST);
        JDUtilities.addToGridBag(panel,box,0,1,1,1,0,0,new Insets(0,7,5,0),GridBagConstraints.NONE, GridBagConstraints.NORTHWEST);
        
       JDUtilities.addToGridBag(panel, btn = new JButton("Test Reconnect"), 1,1,1, 1, 0, 0, new Insets(0,5,5,0), GridBagConstraints.NONE, GridBagConstraints.NORTHWEST);
       JDUtilities.addToGridBag(panel, new JSeparator(),0, 3, 5, 1, 1, 1, new Insets(0,7,3,0), GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);
       
       btn.addActionListener(this);

        // panel.add(new JSeparator());
        if (reconnectType != null) box.setSelectedItem(reconnectType);
        add(panel, BorderLayout.NORTH);

        this.setReconnectType();
    }

    private void setReconnectType() {

        if (lh != null) panel.remove(lh);
        if (er != null) panel.remove(er);
        lh = null;
        er = null;
        if (((String) box.getSelectedItem()).equals(JDLocale.L("modules.reconnect.types.liveheader", "LiveHeader/Curl"))) {
            lh = new SubPanelLiveHeaderReconnect(uiinterface, (Interaction) new HTTPLiveHeader());

            JDUtilities.addToGridBag(panel, lh, 0, 4, 5, 1, 1, 1, new Insets(0,0,0,0), GridBagConstraints.BOTH, GridBagConstraints.NORTH);

        } else if (((String) box.getSelectedItem()).equals(JDLocale.L("modules.reconnect.types.extern", "Extern"))) {
            er = new ConfigPanelDefault(uiinterface, ((Interaction) new ExternReconnect()).getConfig());

            JDUtilities.addToGridBag(panel, er, 0, 4, 5, 1, 1, 1, new Insets(0,0,0,0), GridBagConstraints.BOTH, GridBagConstraints.NORTH);

        }

        else if (((String) box.getSelectedItem()).equals(JDLocale.L("modules.reconnect.types.batch", "Batch"))) {
            er = new ConfigPanelDefault(uiinterface, ((Interaction) new BatchReconnect()).getConfig());

            JDUtilities.addToGridBag(panel, er, 0, 4, 5, 1, 1, 1, new Insets(0,0,0,0), GridBagConstraints.BOTH, GridBagConstraints.NORTH);

        }
        this.validate();
    }

    @Override
    public void load() {
        this.loadConfigEntries();
    }

    @Override
    public String getName() {
        return JDLocale.L("gui.config.reconnect.name", "Reconnect");
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == box) {

            configuration.setProperty(Configuration.PARAM_RECONNECT_TYPE, (String) box.getSelectedItem());
            setReconnectType();

        }
        if (e.getSource() == btn) {
            save();

            

            
            
            
            
            
            mld = new MiniLogDialog("Reconnect");
             mld.getBtnOK().setEnabled(false);
             mld.getBtnOK().setText(JDLocale.L("gui.warning.reconnect.pleaseWait", "Bitte Warten...Reconnect läuft"));
            mld.getProgress().setMaximum(100);
            mld.getProgress().setValue(2);
            //mld.setEnabled(true);
            JDUtilities.getController().addControlListener(this);

            logger.info("Start Reconnect");
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_RETRIES, 0);
            JDUtilities.getSubConfig("BATCHRECONNECT").setProperty(BatchReconnect.PARAM_RETRIES, 0);
            JDUtilities.getConfiguration().setProperty(ExternReconnect.PARAM_RETRIES, 0);

            new Thread() {
                public void run() {

                    if (Reconnecter.waitForNewIP(1)) {
                        mld.setText(JDLocale.L("gui.warning.reconnectSuccess", "Reconnect successfull")+ "\r\n\r\n\r\n"+mld.getText());
                    } else {
                        
                        mld.setText(JDLocale.L("gui.warning.reconnectFailed", "Reconnect failed!")+ "\r\n\r\n\r\n"+mld.getText());
                        
                     }
                    mld.getProgress().setValue(100);
                    mld.getBtnOK().setEnabled(true);
                    mld.getBtnOK().setText(JDLocale.L("gui.warning.reconnect.close", "Fenster schließen"));
                    //mld.setEnabled(false);
                    JDUtilities.getController().removeControlListener(ConfigPanelReconnect.this);
                }
            }.start();
           

            // mld.getBtnOK().setEnabled(true);
        }
    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_LOG_OCCURED&&mld!=null&&mld.isEnabled()) {
            LogRecord l = (LogRecord) event.getParameter();

            if (l.getSourceClassName().startsWith("jd.controlling.interaction")) {
                mld.setText(JDUtilities.formatSeconds((int)l.getMillis()/1000)+" : "+ l.getMessage() + "\r\n"+mld.getText());
                mld.getScrollPane().setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
                mld.getProgress().setValue(mld.getProgress().getValue()+1);
            }
        }

    }
}
