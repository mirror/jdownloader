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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import jd.Configuration;
import jd.JDUtilities;
import jd.controlling.interaction.HTTPReconnect;
import jd.gui.UIInterface;
import jd.router.RouterData;
import jd.router.RouterParser;

/**
 * Hier können Routereinstellungen vorgenommen werden
 * 
 * @author astaldo
 */
class ConfigPanelInteractionHTTPReconnect extends ConfigPanel implements ItemListener, ActionListener {
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

    private JButton           btnDisconnect;

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

    private JLabel            lblIPAddressRegEx;

    private JLabel            lblIPAddressOffline;

    private JLabel            lblIPAddressSite;

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

    private JTextField        txtIPAddressRegEx;

    private JTextField        txtIPAddressOffline;

    private JTextField        txtIPAddressSite;

    private JTextField        txtRetries;

    private JTextField        txtConnectRequestProperties;

    private JTextField        txtConnectPostParams;

    private JTextField        txtWaitForIPCheck;

    ConfigPanelInteractionHTTPReconnect(Configuration configuration, UIInterface uiinterface) {
        super(configuration, uiinterface);
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
        txtIPAddressSite.setText(routerData.getIpAddressSite());
        txtIPAddressOffline.setText(routerData.getIpAddressOffline());
        this.txtWaitForIPCheck.setText(configuration.getWaitForIPCheck() + "");
        this.txtRouterIP.setText(routerData.getRouterIP());
        this.txtRouterPort.setText(routerData.getRouterPort() + "");
        this.txtRetries.setText(configuration.getReconnectRetries() + "");
        if (routerData.getIpAddressRegEx() != null)
            txtIPAddressRegEx.setText(routerData.getIpAddressRegEx().toString());
        else
            txtIPAddressRegEx.setText(null);
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
        routerData.setIpAddressSite(txtIPAddressSite.getText().trim());
        routerData.setIpAddressOffline(txtIPAddressOffline.getText().trim());
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
        try {
            String txtForPattern = txtIPAddressRegEx.getText().trim();
            Pattern.compile(txtForPattern);
            routerData.setIpAddressRegEx(txtForPattern);
        }
        catch (Exception e) {
            logger.severe("RegEx for IPAddress wrong");
        }
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
            Object selected = JOptionPane.showInputDialog(this, "Bitte wähle deinen Router aus", "Router importieren", JOptionPane.INFORMATION_MESSAGE, null, routerData.toArray(), null);
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
        if (e.getSource() == btnImport)
            importFromRoutersDat();
        else if (e.getSource() == btnDisconnect) {
            int button = JOptionPane.showConfirmDialog(this, "Sollen die Daten gespeichert und der Router getrennt werden?", "Reconnect-Test", JOptionPane.YES_NO_OPTION);
            if (button == JOptionPane.YES_OPTION) {
                save();
                boolean success = new HTTPReconnect().interact(null);
                if (success)
                    JOptionPane.showMessageDialog(this, "RouterTrennung erfolgreich");
                else
                    JOptionPane.showMessageDialog(this, "RouterTrennung fehlgeschlagen");
            }
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

        lblImport = new JLabel("RouterDaten importieren", JLabel.RIGHT);
        lblDisconnectTest = new JLabel("Router reconnecten", JLabel.RIGHT);
        lblUsername = new JLabel("Benutzername", JLabel.RIGHT);
        lblPassword = new JLabel("Password", JLabel.RIGHT);
        lblRouterIP = new JLabel("Hostname/IP des Routers", JLabel.RIGHT);
        lblRouterPort = new JLabel("Port des Routers", JLabel.RIGHT);
        lblRouterName = new JLabel("Name des Routers", JLabel.RIGHT);
        lblLogin = new JLabel("Anmeldestring", JLabel.RIGHT);
        lblLoginType = new JLabel("Art der Anmeldung", JLabel.RIGHT);
        lblLoginRequestProperties = new JLabel("RequestProperties fürs Login", JLabel.RIGHT);
        lblIPAddressOffline = new JLabel("Offlinestring", JLabel.RIGHT);
        lblLoginPostParams = new JLabel("POST Parameter fürs Login)", JLabel.RIGHT);
        lblLogoff = new JLabel("Anmeldestring", JLabel.RIGHT);
        lblConnect = new JLabel("Verbindungsaufbau", JLabel.RIGHT);
        lblDisconnect = new JLabel("Verbindungsabbruch", JLabel.RIGHT);
        lblDisconnectType = new JLabel("Art des Verbindungsabbruchs", JLabel.RIGHT);
        lblDisconnectRequestProperties = new JLabel("RequestProperties für den Verbindungsabbruch", JLabel.RIGHT);
        lblDisconnectPostParams = new JLabel("POST Parameter für den Verbindungsabbruch", JLabel.RIGHT);
        lblIPAddressRegEx = new JLabel("RegEx ausdruck zum Lesen der IP", JLabel.RIGHT);
        lblIPAddressSite = new JLabel("RouterPage für die IPAdresse", JLabel.RIGHT);
        lblRetries = new JLabel("Versuche (0=unendlich)", JLabel.RIGHT);
        lblConnectType = new JLabel("Art des Verbindungsaufbaus", JLabel.RIGHT);
        lblConnectRequestProperties = new JLabel("RequestProperties für den Verbindungsaufbau", JLabel.RIGHT);
        lblConnectPostParams = new JLabel("POST Parameter für den Verbindungsaufbau", JLabel.RIGHT);
        lblWaitForIPCheck = new JLabel("Wartezeit bis zum ersten IP-Check [sek]", JLabel.RIGHT);

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
        txtIPAddressOffline = new JTextField(50);
        txtConnect = new JTextField(50);

        txtDisconnect = new JTextField(50);
        cboDisconnectType = new JComboBox(types);
        cboConnectType = new JComboBox(types);
        txtDisconnectRequestProperties = new JTextField(50);
        txtDisconnectPostParams = new JTextField(50);
        txtIPAddressRegEx = new JTextField(50);
        txtIPAddressSite = new JTextField(50);
        txtRetries = new JTextField(10);
        txtWaitForIPCheck = new JTextField(10);
        txtConnectRequestProperties = new JTextField(50);
        txtConnectPostParams = new JTextField(50);

        btnImport = new JButton("Import");
        btnDisconnect = new JButton("DisconnectTest");

        txtLogin.setToolTipText("Als Platzhalter für den Benutzernamen " + HTTPReconnect.VAR_USERNAME + " und für das Password " + HTTPReconnect.VAR_PASSWORD + " nehmen");
        txtConnect.setToolTipText("Hiermit wird die Verbindung wiederaufgebaut (zb http://www.google.de)");
        txtLoginRequestProperties.setToolTipText("<HTML>Die Werte werden folgendermaßen eingegeben:<BR>key1==value1;;key2==value2;;key3==value3.1=\"value3.2\"</HTML>");
        txtDisconnectRequestProperties.setToolTipText("<HTML>Die Werte werden folgendermaßen eingegeben:<BR>key1==value1;;key2==value2;;key3==value3.1=\"value3.2\"</HTML>");

        cboLoginType.addItemListener(this);
        cboDisconnectType.addItemListener(this);
        cboConnectType.addItemListener(this);
        btnImport.addActionListener(this);
        btnDisconnect.addActionListener(this);

        Insets insets = new Insets(1, 5, 1, 5);
        int row = 0;
        tabbedPane.addTab(JDUtilities.getResourceString("label.config.router_control"), pnlControl);

        tabbedPane.addTab(JDUtilities.getResourceString("label.config.router_router"), pnlRouter);
        tabbedPane.addTab(JDUtilities.getResourceString("label.config.router_login"), pnlLogin);
        tabbedPane.addTab(JDUtilities.getResourceString("label.config.router_connect"), pnlConnect);
        tabbedPane.addTab(JDUtilities.getResourceString("label.config.router_disconnect"), pnlDisconnect);
        tabbedPane.addTab(JDUtilities.getResourceString("label.config.router_ip_check"), pnlIpCheck);

        row = 0;

        // Controlbuttons
        JDUtilities.addToGridBag(pnlControl, lblImport, 0, row++, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(pnlControl, lblDisconnectTest, 0, row++, 1, 1, 0, 1, insets, GridBagConstraints.NONE, GridBagConstraints.NORTHEAST);
        row = 0;
        JDUtilities.addToGridBag(pnlControl, btnImport, 1, row++, 1, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(pnlControl, btnDisconnect, 1, row++, 1, 1, 1, 1, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTHWEST);

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
        JDUtilities.addToGridBag(pnlIpCheck, lblIPAddressSite, 0, row++, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(pnlIpCheck, lblIPAddressRegEx, 0, row++, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(pnlIpCheck, lblIPAddressOffline, 0, row++, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(pnlIpCheck, lblRetries, 0, row++, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.NORTHEAST);
        JDUtilities.addToGridBag(pnlIpCheck, lblWaitForIPCheck, 0, row++, 1, 1, 0, 1, insets, GridBagConstraints.NONE, GridBagConstraints.NORTHEAST);

        row = 0;
        JDUtilities.addToGridBag(pnlIpCheck, txtIPAddressSite, 1, row++, 1, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(pnlIpCheck, txtIPAddressRegEx, 1, row++, 1, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(pnlIpCheck, txtIPAddressOffline, 1, row++, 1, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(pnlIpCheck, txtRetries, 1, row++, 1, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTHWEST);
        JDUtilities.addToGridBag(pnlIpCheck, txtWaitForIPCheck, 1, row++, 1, 1, 1, 1, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTHWEST);

        add(tabbedPane, BorderLayout.CENTER);
        checkComboBoxes();

    }

    @Override
    public String getName() {

        return "Router";
    }
}
