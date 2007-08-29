package jd.gui.skins.simple.config;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import jd.Configuration;
import jd.JDUtilities;
import jd.plugins.Plugin;
import jd.router.RouterData;
/**
 * Hier können Routereinstellungen vorgenommen werden
 * 
 * TODO: RequestProperties
 * 
 * @author astaldo
 */
class ConfigPanelRouter extends JPanel{
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -4338544183688967675L;
    private JPanel panel;
    private Logger logger = Plugin.getLogger();
    private Configuration configuration;
    private RouterData routerData;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JTextField txtRouterIP;
    private JTextField txtRouterPort;
    private JTextField txtRouterName;
    private JTextField txtLogin;
    private JComboBox  cboLoginType;
    private JTextField txtLoginPostParams;
    private JTextField txtLogoff;
    private JTextField txtConnect;
    private JTextField txtDisconnect;
    private JComboBox  cboDisconnectType;
    private JTextField txtDisconnectPostParams;
    private JTextField txtIPAddressRegEx;
    private JTextField txtIPAddressSite;
    
    ConfigPanelRouter(Configuration configuration){
        this.configuration = configuration;
        this.routerData = configuration.getRouterData();
        panel = new JPanel(new GridBagLayout());
        String types[] = new String[]{"GET","POST"};
        setLayout(new GridBagLayout());
        JLabel lblUsername             = new JLabel("Benutzername",JLabel.RIGHT);
        JLabel lblPassword             = new JLabel("Password",JLabel.RIGHT);
        JLabel lblRouterIP             = new JLabel("Hostname/IP des Routers",JLabel.RIGHT);
        JLabel lblRouterPort           = new JLabel("Port des Routers",JLabel.RIGHT);
        JLabel lblRouterName           = new JLabel("Name des Routers",JLabel.RIGHT);
        JLabel lblLogin                = new JLabel("Anmeldestring",JLabel.RIGHT);
        JLabel lblLoginType            = new JLabel("Art der Anmeldung",JLabel.RIGHT);
        JLabel lblLoginPostParams      = new JLabel("POST Parameter fürs Login",JLabel.RIGHT);
        JLabel lblLogoff               = new JLabel("Anmeldestring",JLabel.RIGHT);
        JLabel lblConnect              = new JLabel("Verbindungsaufbau",JLabel.RIGHT);
        JLabel lblDisconnect           = new JLabel("Verbindungsabbruch",JLabel.RIGHT);
        JLabel lblDisconnectType       = new JLabel("Art des Verbindungsabbruchs",JLabel.RIGHT);
        JLabel lblDisconnectPostParams = new JLabel("POST Parameter für den Verbindungsabbruch",JLabel.RIGHT);
        JLabel lblIPAddressRegEx       = new JLabel("RegEx ausdruck zum Lesen der IP",JLabel.RIGHT);
        JLabel lblIPAddressSite        = new JLabel("RouterPage für die IPAdresse",JLabel.RIGHT);
        
        txtUsername             = new JTextField(configuration.getRouterUsername(),30);
        txtPassword             = new JPasswordField(configuration.getRouterPassword(),30);
        txtRouterIP             = new JTextField(configuration.getRouterIP(),30);
        txtRouterPort           = new JTextField(Integer.toString(configuration.getRouterPort()),30);
        txtRouterName           = new JTextField(routerData.getRouterName(),30);
        txtLogin                = new JTextField(routerData.getLogin(),30);
        cboLoginType            = new JComboBox(types);
        txtLoginPostParams      = new JTextField(routerData.getLoginPostParams(),30);
        txtLogoff               = new JTextField(routerData.getLogoff(),30);
        txtConnect              = new JTextField(routerData.getConnect(),30);
        txtDisconnect           = new JTextField(routerData.getDisconnect(),30);
        cboDisconnectType       = new JComboBox(types);
        txtDisconnectPostParams = new JTextField(routerData.getDisconnectPostParams(),30);
        txtIPAddressRegEx       = new JTextField(30);
        txtIPAddressSite        = new JTextField(routerData.getIpAddressSite(),30);
        
        if(routerData.getIpAddressRegEx() != null){
            txtIPAddressRegEx.setText(routerData.getIpAddressRegEx().toString()); 
        }
        cboLoginType.setSelectedItem(routerData.getLoginType());
        cboDisconnectType.setSelectedItem(routerData.getDisconnectType());

        txtRouterName = new JTextField(routerData.getRouterName(),30);
        txtLogin      = new JTextField(routerData.getLogin(),30);
        txtLogoff     = new JTextField(routerData.getLogoff(),30);
        txtConnect    = new JTextField(routerData.getConnect(),30);
        txtDisconnect = new JTextField(routerData.getDisconnect(),30);
        Insets insets = new Insets(1,5,1,5);
        
        JDUtilities.addToGridBag(panel, lblUsername,             0, 0, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, lblPassword,             0, 1, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, lblRouterIP,             0, 2, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, lblRouterPort,           0, 3, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, lblRouterName,           0, 4, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, lblLogin,                0, 5, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, lblLoginType,            0, 6, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, lblLoginPostParams,      0, 7, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, lblLogoff,               0, 8, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, lblConnect,              0, 9, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, lblDisconnect,           0,10, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, lblDisconnectType,       0,11, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, lblDisconnectPostParams, 0,12, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, lblIPAddressSite,        0,13, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, lblIPAddressRegEx,       0,14, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);

        JDUtilities.addToGridBag(panel, txtUsername,             1, 0, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, txtPassword,             1, 1, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, txtRouterIP,             1, 2, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, txtRouterPort,           1, 3, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, txtRouterName,           1, 4, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, txtLogin,                1, 5, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, cboLoginType,            1, 6, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, txtLoginPostParams,      1, 7, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, txtLogoff,               1, 8, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, txtConnect,              1, 9, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, txtDisconnect,           1,10, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, cboDisconnectType,       1,11, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, txtDisconnectPostParams, 1,12, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, txtIPAddressSite,        1,13, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, txtIPAddressRegEx,       1,14, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        
        add(panel);
    }
    void save(){
        configuration.setRouterUsername(txtUsername.getText().trim());
        configuration.setRouterPassword(txtUsername.getText().trim());
        configuration.setRouterIP(txtRouterIP.getText().trim());
        routerData.setRouterName(txtRouterName.getText().trim());
        routerData.setLogin(txtLogin.getText().trim());
        routerData.setLoginPostParams(txtLoginPostParams.getText().trim());
        routerData.setLogoff(txtLogoff.getText().trim());
        routerData.setConnect(txtConnect.getText().trim());
        routerData.setDisconnect(txtDisconnect.getText().trim());
        routerData.setDisconnectPostParams(txtDisconnectPostParams.getText().trim());
        routerData.setIpAddressSite(txtIPAddressSite.getText().trim());

        try {
            configuration.setRouterPort(Integer.parseInt(txtRouterPort.getText()));
        }
        catch (NumberFormatException e) { logger.severe("routerPort wrong"); }

        if(cboLoginType.getSelectedItem().equals("GET"))
            routerData.setLoginType(RouterData.TYPE_WEB_GET);
        else
            routerData.setLoginType(RouterData.TYPE_WEB_POST);
        if(cboDisconnectType.getSelectedItem().equals("GET"))
            routerData.setDisconnectType(RouterData.TYPE_WEB_GET);
        else
            routerData.setDisconnectType(RouterData.TYPE_WEB_POST);
        try {
            String txtForPattern = txtIPAddressRegEx.getText().trim();
            Pattern.compile(txtForPattern);
            routerData.setIpAddressRegEx(txtForPattern);
        }
        catch (Exception e) { logger.severe("RegEx for IPAddress wrong"); }

    }
}
