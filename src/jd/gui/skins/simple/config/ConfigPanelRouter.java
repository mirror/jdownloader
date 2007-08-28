package jd.gui.skins.simple.config;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;

import jd.Configuration;
import jd.JDUtilities;
import jd.router.RouterData;

class ConfigPanelRouter extends JComponent{
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -4338544183688967675L;
    private RouterData routerData;
    private JTextField txtRouterName;
    private JTextField txtLogin;
    private JTextField txtLogoff;
    private JTextField txtConnect;
    private JTextField txtDisconnect;
    
    ConfigPanelRouter(Configuration configuration){
        this.routerData = configuration.getRouterData();
        setLayout(new GridBagLayout());
        JLabel lblRouterName       = new JLabel("Name des Routers",JLabel.RIGHT);
        JLabel lblLoginString      = new JLabel("Anmeldestring",JLabel.RIGHT);
        JLabel lblLogoffString     = new JLabel("Anmeldestring",JLabel.RIGHT);
        JLabel lblConnectString    = new JLabel("Verbindungsaufbau",JLabel.RIGHT);
        JLabel lblDisconnectString = new JLabel("Verbindungsabbruch",JLabel.RIGHT);
        
        txtRouterName = new JTextField(routerData.getRouterName());
        txtLogin      = new JTextField(routerData.getLogin());
        txtLogoff     = new JTextField(routerData.getLogoff());
        txtConnect    = new JTextField(routerData.getConnect());
        txtDisconnect = new JTextField(routerData.getDisconnect());
        
        JDUtilities.addToGridBag(this, lblRouterName,       0, 0, 1, 1, 1, 1, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(this, lblLoginString,      0, 1, 1, 1, 1, 1, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(this, lblLogoffString,     0, 2, 1, 1, 1, 1, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(this, lblConnectString,    0, 3, 1, 1, 1, 1, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(this, lblDisconnectString, 0, 4, 1, 1, 1, 1, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);

        JDUtilities.addToGridBag(this, txtRouterName, 1, 0, 1, 1, 1, 1, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(this, txtLogin,      1, 1, 1, 1, 1, 1, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(this, txtLogoff,     1, 2, 1, 1, 1, 1, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(this, txtConnect,    1, 3, 1, 1, 1, 1, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(this, txtDisconnect, 1, 4, 1, 1, 1, 1, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    }
    void save(){
        routerData.setRouterName(txtRouterName.getText());
        routerData.setLoginString(txtLogin.getText());
        routerData.setConnect(txtConnect.getText());
        routerData.setDisconnect(txtDisconnect.getText());
    }
}
