package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;

import jd.config.Configuration;
import jd.gui.UIInterface;
import jd.gui.skins.simple.components.BrowseFile;

public class ConfigPanelReconnect extends ConfigPanel implements ActionListener {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3383448498625377495L;
    private static final String PARAM_RECONNECT_TYPE = null;
    private JLabel            lblHomeDir;
    private BrowseFile        brsHomeDir;
    private Configuration     configuration;
    private JTabbedPane tabbedPane;
    private JComboBox box;
    private SubPanelHTTPReconnect httpReconnect=null;
    ConfigPanelReconnect(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        this.configuration = configuration;
        initPanel();
        load();
    }
    public void save() {
        this.saveConfigEntries();
        httpReconnect.save();
        httpReconnect.saveConfigEntries();
    }
    @Override
    public void initPanel() {
  String reconnectType=configuration.getStringProperty(PARAM_RECONNECT_TYPE);
       box= new JComboBox(new String[]{"HTTPReconnect/Routercontrol","LiveHeader/Curl","Extern"});
       box.addActionListener(this);
      panel.setLayout(new BorderLayout());
        panel.add(box,BorderLayout.PAGE_START);
       // panel.add(new JSeparator());
        if(reconnectType!=null)box.setSelectedItem(reconnectType);
        add(panel, BorderLayout.NORTH);
        this.setReconnectType();
    }
    private void setReconnectType() {
       if(((String)box.getSelectedItem()).equals("HTTPReconnect/Routercontrol")){
         //  panel.remove(1);
           panel.add(httpReconnect=new SubPanelHTTPReconnect(configuration, uiinterface),BorderLayout.CENTER);
           
       }else   if(((String)box.getSelectedItem()).equals("LiveHeader/Curl")){
           panel.add(httpReconnect=new SubPanelHTTPReconnect(configuration, uiinterface),BorderLayout.CENTER);
           
       }else   if(((String)box.getSelectedItem()).equals("Extern")){
           
       }
    }
    @Override
    public void load() {
        this.loadConfigEntries();
    }
    @Override
    public String getName() {
        return "Reconnect";
    }
    public void actionPerformed(ActionEvent e) {
     if(e.getSource()==box){
         configuration.setProperty(PARAM_RECONNECT_TYPE,(String)box.getSelectedItem());
         
         
     }
        
    }
}
