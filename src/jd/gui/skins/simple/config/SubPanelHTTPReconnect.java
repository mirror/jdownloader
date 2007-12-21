package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import jd.config.Configuration;
import jd.gui.UIInterface;
import jd.router.RouterData;
import jd.router.RouterParser;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Hier können Routereinstellungen vorgenommen werden
 * 
 * @author astaldo
 */
class SubPanelHTTPReconnect extends ConfigPanel implements ItemListener, ActionListener {
    private static String     GET              = "GET";

    private static String     POST             = "POST";

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -4338544183688967675L;

    // private JPanel panel;
    private JTabbedPane       tabbedPane;

    private JPanel            pnlControl;

    private JPanel            pnlRouter;

    private JPanel            pnlLogin;

    private JPanel            pnlDisconnect;

    private JPanel            pnlConnect;

    private JPanel            pnlIpCheck;

    private RouterData        routerData;

    private JButton           btnImport;



    private JLabel            lblWaitForIPCheck;

    private JLabel            lblImport;

    private JLabel            lblDisconnectTest;

    private JLabel            lblUsername;

    private JLabel            lblPassword;

    private JLabel            lblRouterIP;

    private JLabel            lblRouterPort;

    private JLabel            lblRouterName;

    private JLabel            lblLogin;

    private JLabel            lblLoginType;

    private JLabel            lblLoginRequestProperties;

    private JLabel            lblLoginPostParams;

    private JLabel            lblLogoff;

    private JLabel            lblConnect;

    private JLabel            lblDisconnect;

    private JLabel            lblDisconnectType;

    private JLabel            lblDisconnectRequestProperties;

    private JLabel            lblDisconnectPostParams;



    private JLabel            lblRetries;

    private JLabel            lblConnectType;

    private JLabel            lblConnectRequestProperties;

    private JLabel            lblConnectPostParams;

    private JTextField        txtUsername;

    private JPasswordField    txtPassword;

    private JTextField        txtRouterIP;

    private JTextField        txtRouterPort;

    private JTextField        txtRouterName;

    private JTextField        txtLogin;

    private JComboBox         cboLoginType;

    private JTextField        txtLoginRequestProperties;

    private JTextField        txtLoginPostParams;

    private JTextField        txtLogoff;

    private JTextField        txtConnect;

    private JTextField        txtDisconnect;

    private JComboBox         cboDisconnectType;

    private JComboBox         cboConnectType;

    private JTextField        txtDisconnectRequestProperties;

    private JTextField        txtDisconnectPostParams;



    private JTextField        txtRetries;

    private JTextField        txtConnectRequestProperties;

    private JTextField        txtConnectPostParams;

    private JTextField        txtWaitForIPCheck;

    private Configuration     configuration;

    SubPanelHTTPReconnect(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        this.configuration = configuration;
        initPanel();

        load();
    }

    /**
     * Lädt alle Informationen
     */
    public void load() {
        txtRouterName.setText(routerData.getRouterName());
        txtLogin.setText(routerData.getLogin());
        txtLoginPostParams.setText(routerData.getLoginPostParams());
        txtLogoff.setText(routerData.getLogoff());
        txtConnect.setText(routerData.getConnect());
        txtDisconnect.setText(routerData.getDisconnect());
        txtDisconnectPostParams.setText(routerData.getDisconnectPostParams());
        txtConnectPostParams.setText(routerData.getConnectPostParams());

        this.txtWaitForIPCheck.setText(configuration.getWaitForIPCheck() + "");
        this.txtRouterIP.setText(routerData.getRouterIP());
        this.txtRouterPort.setText(routerData.getRouterPort() + "");
        this.txtRetries.setText(configuration.getReconnectRetries() + "");
 
        if (routerData.getLoginType() == RouterData.TYPE_WEB_GET) {

            cboLoginType.setSelectedItem(GET);
        }
        else {
            cboLoginType.setSelectedItem(POST);
        }
        if (routerData.getDisconnectType() == RouterData.TYPE_WEB_GET) {
            cboDisconnectType.setSelectedItem(GET);

        }
        else {
            cboDisconnectType.setSelectedItem(POST);
        }
        if (routerData.getConnectType() == RouterData.TYPE_WEB_GET) {
            cboConnectType.setSelectedItem(GET);

        }
        else {
            cboConnectType.setSelectedItem(POST);
        }

        if (routerData.getLoginRequestProperties() != null)
            txtLoginRequestProperties.setText(mergeHashMap(routerData.getLoginRequestProperties()));
        else
            txtLoginRequestProperties.setText(null);
        if (routerData.getDisconnectRequestProperties() != null)
            txtDisconnectRequestProperties.setText(mergeHashMap(routerData.getDisconnectRequestProperties()));
        else
            txtDisconnectRequestProperties.setText(null);
        if (routerData.getConnectRequestProperties() != null)
            txtConnectRequestProperties.setText(mergeHashMap(routerData.getConnectRequestProperties()));
        else
            txtConnectRequestProperties.setText(null);

    }

    /**
     * Speichert alle Änderungen auf der Maske
     */
    public void save() {
        configuration.setRouterData(routerData);
        configuration.setRouterUsername(txtUsername.getText().trim());
        configuration.setRouterPassword(new String(txtPassword.getPassword()).trim());
        try {
            configuration.setReconnectRetries(Integer.parseInt(new String(txtRetries.getText()).trim()));
        }
        catch (NumberFormatException e) {
            logger.severe("Reconnect retries wrong");
        }
        ;
        try {
            configuration.setWaitForIPCheck(Integer.parseInt(new String(txtWaitForIPCheck.getText()).trim()));
        }
        catch (NumberFormatException e) {
            logger.severe("txtWaitForIPCheck wrong");
        }
        ;
        routerData.setRouterIP(txtRouterIP.getText().trim());
        routerData.setRouterName(txtRouterName.getText().trim());
        routerData.setLogin(txtLogin.getText().trim());
        routerData.setLoginPostParams(txtLoginPostParams.getText().trim());
        routerData.setLogoff(txtLogoff.getText().trim());
        routerData.setConnect(txtConnect.getText().trim());
        routerData.setDisconnect(txtDisconnect.getText().trim());
        routerData.setDisconnectPostParams(txtDisconnectPostParams.getText().trim());

        routerData.setConnectPostParams(txtConnectPostParams.getText().trim());

        try {
            routerData.setRouterPort(Integer.parseInt(txtRouterPort.getText()));
        }
        catch (NumberFormatException e) {
            logger.severe("routerPort wrong");
        }

        if (cboLoginType.getSelectedItem().equals(GET))
            routerData.setLoginType(RouterData.TYPE_WEB_GET);
        else
            routerData.setLoginType(RouterData.TYPE_WEB_POST);
        if (cboDisconnectType.getSelectedItem().equals(GET))
            routerData.setDisconnectType(RouterData.TYPE_WEB_GET);
        else
            routerData.setDisconnectType(RouterData.TYPE_WEB_POST);
     
        routerData.setLoginRequestProperties(splitString(txtLoginRequestProperties.getText().trim()));
        routerData.setDisconnectRequestProperties(splitString(txtDisconnectRequestProperties.getText().trim()));
        routerData.setConnectRequestProperties(splitString(txtConnectRequestProperties.getText().trim()));
    }

    /**
     * Gibt eine Hashmap als String zurück. Das Format ist so;
     * key1=value1;key2=value2;"key3;k"="value;value"...
     * 
     * @param map Die Map, die ausgegeben werden soll
     * @return Ein String, der die Werte der HashMap darstellt
     */
    private String mergeHashMap(HashMap<String, String> map) {
        StringBuffer buffer = new StringBuffer();
        Iterator<String> iterator = map.keySet().iterator();
        String key;
        while (iterator.hasNext()) {
            key = iterator.next();
            buffer.append(key);
            buffer.append("==");
            buffer.append(map.get(key));
            if (iterator.hasNext()) buffer.append(";;");
        }
        return buffer.toString();
    }

    private HashMap<String, String> splitString(String properties) {
        HashMap<String, String> map = new HashMap<String, String>();
        if (properties == null || properties.equals("")) return map;
        String[] items = properties.split(";;");
        String[] keyValuePair;
        for (int i = 0; i < items.length; i++) {
            keyValuePair = items[i].split("==");
            if (keyValuePair.length == 2) {
                map.put(keyValuePair[0], keyValuePair[1]);
            }
            else {
                logger.severe("something's wrong with the key/value pair." + keyValuePair[0]);
            }
        }
        return map;
    }

    /**
     * Initiiert die ComboBoxen
     */
    private void checkComboBoxes() {
        if (cboLoginType.getSelectedItem().equals(GET)) {
            lblLoginPostParams.setEnabled(false);
            txtLoginRequestProperties.setEnabled(false);
            txtLoginPostParams.setEnabled(false);
        }
        else {
            lblLoginPostParams.setEnabled(true);
            txtLoginRequestProperties.setEnabled(true);
            txtLoginPostParams.setEnabled(true);

        }
        if (cboDisconnectType.getSelectedItem().equals(GET)) {
            lblDisconnectPostParams.setEnabled(false);
            txtDisconnectRequestProperties.setEnabled(false);
            txtDisconnectPostParams.setEnabled(false);
        }
        else {
            lblDisconnectPostParams.setEnabled(true);
            txtDisconnectRequestProperties.setEnabled(true);
            txtDisconnectPostParams.setEnabled(true);

        }
        if (cboConnectType.getSelectedItem().equals(GET)) {
            lblConnectPostParams.setEnabled(false);
            txtConnectRequestProperties.setEnabled(false);
            txtConnectPostParams.setEnabled(false);
        }
        else {
            lblConnectPostParams.setEnabled(true);
            txtConnectRequestProperties.setEnabled(true);
            txtConnectPostParams.setEnabled(true);

        }
    }

    private void importFromRoutersDat() {

        File fileRoutersDat;
        Vector<RouterData> routerData;
        fileRoutersDat = JDUtilities.getResourceFile("jd/router/routerdata.xml");
        if (fileRoutersDat != null) {
            RouterParser parser = new RouterParser();

            routerData = parser.parseXMLFile(fileRoutersDat);

            Collections.sort(routerData, new Comparator<Object>() {
                public int compare(Object a, Object b) {
                    if (a instanceof RouterData && b instanceof RouterData) {

                        if (((RouterData) a).getRouterName().compareToIgnoreCase(((RouterData) b).getRouterName()) > 0) {
                            return 1;
                        }
                        else if (((RouterData) a).getRouterName().compareToIgnoreCase(((RouterData) b).getRouterName()) < 0) {
                            return -1;
                        }
                        else {
                            return 0;
                        }
                    }
                    return 0;
                }

            });
            Object selected = JOptionPane.showInputDialog(this, JDLocale.L("gui.config.httpreconnect.selectRouter","Bitte wähle deinen Router aus"), JDLocale.L("gui.config.httpreconnect.importRouter","Router importieren"), JOptionPane.INFORMATION_MESSAGE, null, routerData.toArray(), null);
            if (selected != null) {
                this.routerData = (RouterData) selected;
                load();
            }
        }
    }

    public void itemStateChanged(ItemEvent e) {
        checkComboBoxes();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnImport){
            importFromRoutersDat();
        }
 

    }

    @Override
    public void initPanel() {

        this.setLayout(new BorderLayout());

        this.routerData = configuration.getRouterData();
        tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        pnlControl = new JPanel(new GridBagLayout());
        pnlRouter = new JPanel(new GridBagLayout());
        pnlLogin = new JPanel(new GridBagLayout());
        pnlDisconnect = new JPanel(new GridBagLayout());
        pnlConnect = new JPanel(new GridBagLayout());
        pnlIpCheck = new JPanel(new GridBagLayout());
        String types[] = new String[] { GET, POST };

        lblImport = new JLabel(JDLocale.L("gui.config.httpreconnect.importRouterData","RouterDaten importieren"), JLabel.RIGHT);
        lblDisconnectTest = new JLabel(JDLocale.L("gui.config.httpreconnect.testreconnect","Router reconnecten"), JLabel.RIGHT);
        lblUsername = new JLabel(JDLocale.L("gui.config.httpreconnect.userName","Benutzername"), JLabel.RIGHT);
        lblPassword = new JLabel(JDLocale.L("gui.config.httpreconnect.password","Password"), JLabel.RIGHT);
        lblRouterIP = new JLabel(JDLocale.L("gui.config.httpreconnect.routerIp","Hostname/IP des Routers"), JLabel.RIGHT);
        lblRouterPort = new JLabel(JDLocale.L("gui.config.httpreconnect.routerPort","Port des Routers"), JLabel.RIGHT);
        lblRouterName = new JLabel(JDLocale.L("gui.config.httpreconnect.routerName","Name des Routers"), JLabel.RIGHT);
        lblLogin = new JLabel(JDLocale.L("gui.config.httpreconnect.loginString","Anmeldestring"), JLabel.RIGHT);
        lblLoginType = new JLabel(JDLocale.L("gui.config.httpreconnect.loginRequestType","Art der Anmeldung"), JLabel.RIGHT);
        lblLoginRequestProperties = new JLabel(JDLocale.L("gui.config.httpreconnect.loginRequestProperties","RequestProperties fürs Login"), JLabel.RIGHT);
       
        lblLoginPostParams = new JLabel(JDLocale.L("gui.config.httpreconnect.loginPOSTParameter","POST Parameter fürs Login)"), JLabel.RIGHT);
        lblLogoff = new JLabel(JDLocale.L("gui.config.httpreconnect.logoffString","Abmeldestring"), JLabel.RIGHT);
        lblConnect = new JLabel(JDLocale.L("gui.config.httpreconnect.connection","Verbindungsaufbau"), JLabel.RIGHT);
        lblDisconnect = new JLabel(JDLocale.L("gui.config.httpreconnect.disconnect","Verbindungsabbruch"), JLabel.RIGHT);
        lblDisconnectType = new JLabel(JDLocale.L("gui.config.httpreconnect.disconnectType","Art des Verbindungsabbruchs"), JLabel.RIGHT);
        lblDisconnectRequestProperties = new JLabel(JDLocale.L("gui.config.httpreconnect.disconnectRequestProperties","RequestProperties für den Verbindungsabbruch"), JLabel.RIGHT);
        lblDisconnectPostParams = new JLabel(JDLocale.L("gui.config.httpreconnect.disconnectPOSTParameter","POST Parameter für den Verbindungsabbruch"), JLabel.RIGHT);

        lblRetries = new JLabel(JDLocale.L("gui.config.httpreconnect.retries","Versuche (0=unendlich)"), JLabel.RIGHT);
        lblConnectType = new JLabel(JDLocale.L("gui.config.httpreconnect.connectType","Art des Verbindungsaufbaus"), JLabel.RIGHT);
        lblConnectRequestProperties = new JLabel(JDLocale.L("gui.config.httpreconnect.connectRequestProperties","RequestProperties für den Verbindungsaufbau"), JLabel.RIGHT);
        lblConnectPostParams = new JLabel(JDLocale.L("gui.config.httpreconnect.connectPOSTParameter","POST Parameter für den Verbindungsaufbau"), JLabel.RIGHT);
        lblWaitForIPCheck = new JLabel(JDLocale.L("gui.config.httpreconnect.waitTimeForIPCheck","Wartezeit bis zum ersten IP-Check [sek]"), JLabel.RIGHT);

        txtUsername = new JTextField(configuration.getRouterUsername());
        txtPassword = new JPasswordField(configuration.getRouterPassword());
        txtRouterIP = new JTextField(50);
        txtRouterPort = new JTextField(50);
        txtRouterName = new JTextField(50);
        txtLogin = new JTextField(50);
        cboLoginType = new JComboBox(types);
        txtLoginRequestProperties = new JTextField(50);
        txtLoginPostParams = new JTextField(50);
        txtLogoff = new JTextField(50);
      
        txtConnect = new JTextField(50);

        txtDisconnect = new JTextField(50);
        cboDisconnectType = new JComboBox(types);
        cboConnectType = new JComboBox(types);
        txtDisconnectRequestProperties = new JTextField(50);
        txtDisconnectPostParams = new JTextField(50);

        txtRetries = new JTextField(10);
        txtWaitForIPCheck = new JTextField(10);
        txtConnectRequestProperties = new JTextField(50);
        txtConnectPostParams = new JTextField(50);

        btnImport = new JButton("Router auswählen");
       

    
        txtLoginRequestProperties.setToolTipText(JDLocale.L("gui.config.httpreconnect.loginrequestProperties.tooltip","<HTML>Die Werte werden folgendermaßen eingegeben:<BR>key1==value1;;key2==value2;;key3==value3.1=\"value3.2\"</HTML>"));
        txtDisconnectRequestProperties.setToolTipText(JDLocale.L("gui.config.httpreconnect.disconnectRequestPropertiesTooltip","<HTML>Die Werte werden folgendermaßen eingegeben:<BR>key1==value1;;key2==value2;;key3==value3.1=\"value3.2\"</HTML>"));

        cboLoginType.addItemListener(this);
        cboDisconnectType.addItemListener(this);
        cboConnectType.addItemListener(this);
        btnImport.addActionListener(this);
    
        Insets insets = new Insets(1, 5, 1, 5);
        int row = 0;
        tabbedPane.addTab(JDLocale.L("gui.config.httpreconnect.tab_control"), pnlControl);

        tabbedPane.addTab(JDLocale.L("gui.config.httpreconnect.tab_router"), pnlRouter);
        tabbedPane.addTab(JDLocale.L("gui.config.httpreconnect.tab_login"), pnlLogin);
        tabbedPane.addTab(JDLocale.L("gui.config.httpreconnect.tab_connect"), pnlConnect);
        tabbedPane.addTab(JDLocale.L("gui.config.httpreconnect.tab_disconnect"), pnlDisconnect);
        tabbedPane.addTab(JDLocale.L("gui.config.httpreconnect.tab_ipCheck"), pnlIpCheck);

        row = 0;

        // Controlbuttons
        JDUtilities.addToGridBag(pnlControl, lblImport, 0, row++, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(pnlControl, lblDisconnectTest, 0, row++, 1, 1, 0, 1, insets, GridBagConstraints.NONE, GridBagConstraints.NORTHEAST);
        row = 0;
        JDUtilities.addToGridBag(pnlControl, btnImport, 1, row++, 1, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
      
        // Router Login
        row = 0;
        JDUtilities.addToGridBag(pnlLogin, lblUsername, 0, row++, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(pnlLogin, lblPassword, 0, row++, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(pnlLogin, lblLogin, 0, row++, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(pnlLogin, lblLoginType, 0, row++, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(pnlLogin, lblLoginRequestProperties, 0, row++, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(pnlLogin, lblLoginPostParams, 0, row++, 1, 1, 0, 1, insets, GridBagConstraints.NONE, GridBagConstraints.NORTHEAST);
        row = 0;
        JDUtilities.addToGridBag(pnlLogin, txtUsername, 1, row++, 1, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(pnlLogin, txtPassword, 1, row++, 1, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(pnlLogin, txtLogin, 1, row++, 1, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(pnlLogin, cboLoginType, 1, row++, 1, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(pnlLogin, txtLoginRequestProperties, 1, row++, 1, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(pnlLogin, txtLoginPostParams, 1, row++, 1, 1, 1, 1, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTHWEST);

        // Routerdaten
        row = 0;
        JDUtilities.addToGridBag(pnlRouter, lblRouterName, 0, row++, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(pnlRouter, lblRouterIP, 0, row++, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(pnlRouter, lblRouterPort, 0, row++, 1, 1, 0, 1, insets, GridBagConstraints.NONE, GridBagConstraints.NORTHEAST);
        row = 0;
        JDUtilities.addToGridBag(pnlRouter, txtRouterName, 1, row++, 1, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(pnlRouter, txtRouterIP, 1, row++, 1, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(pnlRouter, txtRouterPort, 1, row++, 1, 1, 1, 1, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTHWEST);

        // Verbindungsaufbau
        row = 0;
        JDUtilities.addToGridBag(pnlConnect, lblLogoff, 0, row++, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(pnlConnect, lblConnect, 0, row++, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(pnlConnect, lblConnectType, 0, row++, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(pnlConnect, lblConnectRequestProperties, 0, row++, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(pnlConnect, lblConnectPostParams, 0, row++, 1, 1, 0, 1, insets, GridBagConstraints.NONE, GridBagConstraints.NORTHEAST);
        row = 0;
        JDUtilities.addToGridBag(pnlConnect, txtLogoff, 1, row++, 1, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(pnlConnect, txtConnect, 1, row++, 1, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(pnlConnect, cboConnectType, 1, row++, 1, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(pnlConnect, txtConnectRequestProperties, 1, row++, 1, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(pnlConnect, txtConnectPostParams, 1, row++, 1, 1, 1, 1, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTHWEST);

        // Verbindungsabbau
        row = 0;
        JDUtilities.addToGridBag(pnlDisconnect, lblDisconnect, 0, row++, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(pnlDisconnect, lblDisconnectType, 0, row++, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(pnlDisconnect, lblDisconnectRequestProperties, 0, row++, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(pnlDisconnect, lblDisconnectPostParams, 0, row++, 1, 1, 0, 1, insets, GridBagConstraints.NONE, GridBagConstraints.NORTHEAST);
        row = 0;
        JDUtilities.addToGridBag(pnlDisconnect, txtDisconnect, 1, row++, 1, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(pnlDisconnect, cboDisconnectType, 1, row++, 1, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(pnlDisconnect, txtDisconnectRequestProperties, 1, row++, 1, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(pnlDisconnect, txtDisconnectPostParams, 1, row++, 1, 1, 1, 1, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTHWEST);

        // Ipcheck
        row = 0;
      JDUtilities.addToGridBag(pnlIpCheck, lblRetries, 0, row++, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.NORTHEAST);
        JDUtilities.addToGridBag(pnlIpCheck, lblWaitForIPCheck, 0, row++, 1, 1, 0, 1, insets, GridBagConstraints.NONE, GridBagConstraints.NORTHEAST);

        row = 0;
      JDUtilities.addToGridBag(pnlIpCheck, txtRetries, 1, row++, 1, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTHWEST);
        JDUtilities.addToGridBag(pnlIpCheck, txtWaitForIPCheck, 1, row++, 1, 1, 1, 1, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTHWEST);

        add(tabbedPane, BorderLayout.CENTER);
        checkComboBoxes();

    }

    @Override
    public String getName() {

        return JDLocale.L("gui.config.httpreconnect.name","Router");
    }
}
