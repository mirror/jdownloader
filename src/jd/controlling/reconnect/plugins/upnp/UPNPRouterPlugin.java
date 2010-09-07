package jd.controlling.reconnect.plugins.upnp;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jd.config.Configuration;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.controlling.reconnect.plugins.GetIpException;
import jd.controlling.reconnect.plugins.ReconnectException;
import jd.controlling.reconnect.plugins.ReconnectPluginController;
import jd.controlling.reconnect.plugins.RouterPlugin;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.UserIO;
import jd.nrouter.IPCheck;
import jd.nutils.IPAddress;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.utils.Regex;
import org.appwork.utils.locale.Loc;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.lobobrowser.util.OS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class UPNPRouterPlugin extends RouterPlugin implements ActionListener, ControlListener {

    private static final String           EXTERNALIP     = "externalip";
    private static final String           CANCHECKIP     = "cancheckip";
    protected static final String         TRIED_AUTOFIND = "TRIED_AUTOFIND";

    private final Storage                 storage;
    private JButton                       find;
    private JTextField                    serviceTypeTxt;
    private JTextField                    controlURLTxt;
    private JLabel                        wanType;
    private JButton                       auto;
    protected ArrayList<UpnpRouterDevice> devices;

    public UPNPRouterPlugin() {
        this.storage = JSonStorage.getPlainStorage(this.getID());

        if (!UPNPRouterPlugin.this.storage.get(UPNPRouterPlugin.TRIED_AUTOFIND, false)) {
            // only listen to system if autofind has not been used
            JDController.getInstance().addControlListener(this);
        }

    }

    public void actionPerformed(final ActionEvent e) {
        // TODO: Do not use Appwork controller here
        // mixing two different Dialog controllers is not a good idea
        if (e.getSource() == this.auto) {
            final ProgressDialog dialog = new ProgressDialog(new ProgressGetter() {

                public int getProgress() {
                    // TODO Auto-generated method stub
                    return -1;
                }

                public String getString() {
                    // TODO Auto-generated method stub
                    return null;
                }

                public void run() throws Exception {
                    try {
                        UPNPRouterPlugin.this.devices = UPNPRouterPlugin.this.scanDevices();

                    } catch (final IOException e) {
                        UserIO.getInstance().requestMessageDialog("Could not find any UPNP Devices. Try Live Header Reconnect instead!");
                    }

                }

            }, 0, "Looking for routers", "Wait while JDownloader is looking for router interfaces", null);

            Dialog.getInstance().showDialog(dialog);
            if (this.devices != null && this.devices.size() > 0) {
                this.autoFind();
            }
        } else if (e.getSource() == this.find) {
            final ProgressDialog dialog = new ProgressDialog(new ProgressGetter() {

                public int getProgress() {
                    // TODO Auto-generated method stub
                    return -1;
                }

                public String getString() {
                    // TODO Auto-generated method stub
                    return null;
                }

                public void run() throws Exception {
                    try {
                        final ArrayList<UpnpRouterDevice> devices = UPNPRouterPlugin.this.scanDevices();
                        if (Thread.currentThread().isInterrupted()) { return; }
                        final int ret = UserIO.getInstance().requestComboDialog(UserIO.NO_COUNTDOWN, "Select Router", "Please select the router that handles your internet connection", devices.toArray(new HashMap[] {}), 0, null, null, null, new DefaultListCellRenderer() {

                            private static final long serialVersionUID = 3607383089555373774L;

                            @Override
                            public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
                                final JLabel label = (JLabel) super.getListCellRendererComponent(list, ((UpnpRouterDevice) value).getFriendlyname(), index, isSelected, cellHasFocus);

                                return label;
                            }
                        });
                        if (Thread.currentThread().isInterrupted()) { return; }
                        if (ret < 0) { return; }
                        UPNPRouterPlugin.this.setDevice(devices.get(ret));

                    } catch (final IOException e) {
                        UserIO.getInstance().requestMessageDialog("Could not find any UPNP Devices. Try Live Header Reconnect instead!");
                    }

                }

            }, 0, "Looking for routers", "Wait while JDownloader is looking for router interfaces", null);

            Dialog.getInstance().showDialog(dialog);
        }
    }

    private void autoFind() {
        // this thread is required because of the fucked up eventsender in
        // JDController. It can be removed as soon as we implemented a
        // nocnblocking one there

        Log.L.info("Find UPNP Routers START");
        new Thread("UPNPFinder") {
            public void run() {
                try {
                    if (UPNPRouterPlugin.this.devices == null || UPNPRouterPlugin.this.devices.size() == 0) {
                        UPNPRouterPlugin.this.devices = UPNPRouterPlugin.this.scanDevices();
                    }
                    if (UPNPRouterPlugin.this.devices.size() > 0) {
                        final ConfirmDialog cDialog = new ConfirmDialog(Dialog.LOGIC_COUNTDOWN | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, Loc.L("jd.controlling.reconnect.plugins.upnp.UPNPRouterPlugin.autoFind.dialog.title", "UPNP Router found"), Loc.LF("jd.controlling.reconnect.plugins.upnp.UPNPRouterPlugin.autoFind.dialog.message", "It seems that your reconnection is not setup perfectly. JDownloader found %s UPNPRouter device(s).\r\n\r\nShould JDownloader try to auto-configure your reconnect?", UPNPRouterPlugin.this.devices.size()), null, null, null);
                        cDialog.setLeftActions(new AbstractAction(Loc.L("jd.controlling.reconnect.plugins.upnp.UPNPRouterPlugin.autoFind.dialog.morebutton", " ... read more")) {

                            /**
                             * 
                             */
                            private static final long serialVersionUID = 1L;

                            public void actionPerformed(final ActionEvent e) {
                                try {
                                    OS.launchBrowser("http://board.jdownloader.org/showthread.php?t=16450");
                                } catch (final IOException e1) {
                                    // TODO Auto-generated catch block
                                    e1.printStackTrace();
                                }
                            }

                        });
                        cDialog.displayDialog();

                        if (Dialog.isOK(Dialog.getInstance().showDialog(cDialog))) {

                            // final ProgressGetter progressGetter, final int
                            // flags,
                            // final String title, final String message, final
                            // ImageIcon
                            // icon
                            final ProgressDialog dialog = new ProgressDialog(new ProgressGetter() {

                                private UpnpRouterDevice device;
                                private int              progress;

                                public int getProgress() {
                                    // TODO Auto-generated method stub
                                    return this.progress;
                                }

                                public String getString() {
                                    // TODO Auto-generated method stub
                                    return this.device != null ? this.device.getFriendlyname() : "";
                                }

                                public void run() throws Exception {
                                    // used for progresscontroll only
                                    int steps = 0;
                                    final int maxSteps = UPNPRouterPlugin.this.devices.size() * 22;
                                    int deviceCount = 0;
                                    loop: for (final UpnpRouterDevice device : UPNPRouterPlugin.this.devices) {
                                        steps++;
                                        this.progress = steps * 100 / maxSteps;
                                        try {
                                            this.device = device;
                                            UPNPRouterPlugin.this.setDevice(device);
                                            // get real ext IP:

                                            final String ipBefore = IPCheck.getIPAddress();
                                            // TODO what if ipBefore is null
                                            String ipAfter;
                                            UPNPRouterPlugin.this.doReconnect(null);
                                            // wait max 20 seconds
                                            for (int w = 0; w < 20; w++) {
                                                steps++;
                                                this.progress = steps * 100 / maxSteps;
                                                ipAfter = IPCheck.getIPAddress();

                                                if (!ipBefore.equals(ipAfter)) {
                                                    // success
                                                    break loop;
                                                }
                                            }
                                            steps++;
                                            this.progress = steps * 100 / maxSteps;
                                        } catch (final Exception e) {
                                            // TODO Auto-generated catch block
                                            e.printStackTrace();
                                        }
                                        deviceCount++;
                                        steps = deviceCount * 22;
                                        this.progress = steps * 100 / maxSteps;
                                        this.device = null;
                                        UPNPRouterPlugin.this.setDevice(null);
                                    }

                                }

                            }, 0, "Check Devices", "JDownloader now tests all found Router devices", null);

                            Dialog.getInstance().showDialog(dialog);

                            // test is done here. if we found a successfull
                            // device, it
                            // is already set

                            UPNPRouterPlugin.this.storage.put(UPNPRouterPlugin.TRIED_AUTOFIND, true);
                            if (UPNPRouterPlugin.this.storage.get(UpnpRouterDevice.CONTROLURL, null) != null) {

                                final ImageIcon icon = JDTheme.II("gui.images.ok", 32, 32);

                                Dialog.getInstance().showConfirmDialog(Dialog.BUTTONS_HIDE_CANCEL, JDL.L("jd.controlling.reconnect.plugins.upnp.UPNPRouterPlugin.autoFind.successdialog.title", "Successfull"), JDL.LF("jd.controlling.reconnect.plugins.upnp.UPNPRouterPlugin.autoFind.successdialog.message", "JD set up the reconnection settings successfully!\r\n\r\nYour Router is \r\n'%s'", UPNPRouterPlugin.this.storage.get(UpnpRouterDevice.FRIENDLYNAME, null)), icon, null, null);
                                ReconnectPluginController.getInstance().activatePluginReconnect(UPNPRouterPlugin.this);
                            }
                        }
                    }
                } catch (final IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }.start();

    }

    public void controlEvent(final ControlEvent event) {
        if (UPNPRouterPlugin.this.storage.get(UPNPRouterPlugin.TRIED_AUTOFIND, false)) { return; }
        final Configuration configuration = JDUtilities.getConfiguration();
        final boolean allowed = configuration.getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true);
        final boolean rcOK = configuration.getBooleanProperty(Configuration.PARAM_RECONNECT_OKAY, true);
        final int failCount = configuration.getIntegerProperty(Configuration.PARAM_RECONNECT_FAILED_COUNTER, 0);
        switch (event.getEventID()) {

        case ControlEvent.CONTROL_INIT_COMPLETE:

            // case: autoreconnect disable ddue to too many failed
            if (!allowed && !rcOK && failCount == 0) {
                // try to autoconfiger upnp router
                this.autoFind();
            } else if (allowed && failCount > 0) {
                // reconnect failed
                this.autoFind();
            }

            break;

        case ControlEvent.CONTROL_AFTER_RECONNECT:
            if (failCount > 0) {
                // last reconnect failed.
                this.autoFind();
            }
            break;

        }

    }

    @Override
    public void doReconnect(final ProgressController progress) throws ReconnectException {

        try {
            this.runCommand(this.storage.get(UpnpRouterDevice.SERVICETYPE, ""), this.storage.get(UpnpRouterDevice.CONTROLURL, ""), "ForceTermination");
            Thread.sleep(2000);
            this.runCommand(this.storage.get(UpnpRouterDevice.SERVICETYPE, ""), this.storage.get(UpnpRouterDevice.CONTROLURL, ""), "RequestConnection");

        } catch (final Throwable e) {
            throw new ReconnectException(e);
        }

    }

    @Override
    public String getExternalIP() throws GetIpException {
        try {
            final String ip = this.getIP(this.storage.get(UpnpRouterDevice.SERVICETYPE, ""), this.storage.get(UpnpRouterDevice.CONTROLURL, ""));
            this.storage.put(UPNPRouterPlugin.EXTERNALIP, ip);
            return ip;
        } catch (final GetIpException e) {
            // failed. disable ipcheck for this upnp device

            throw e;
        }
    }

    @Override
    public JComponent getGUI() {
        final JPanel p = new JPanel(new MigLayout("ins 10,wrap 3", "[][][grow,fill]", "[]"));
        this.find = new JButton("Find Routers");
        this.find.addActionListener(this);
        this.auto = new JButton("Setup Wizard");
        this.auto.addActionListener(this);
        this.serviceTypeTxt = new JTextField();
        this.controlURLTxt = new JTextField();
        this.wanType = new JLabel();
        p.add(this.auto, "aligny top,gapright 15");
        p.add(new JLabel("Router"), "");
        p.add(this.wanType, "spanx");
        p.add(this.find, "aligny top,gapright 15,newline");
        p.add(new JLabel("Service Type"), "");
        p.add(this.serviceTypeTxt);
        p.add(new JLabel("Control URL"), "newline,skip");
        p.add(this.controlURLTxt);
        p.add(Box.createGlue(), "pushy,growy");
        this.updateGUI();
        this.serviceTypeTxt.getDocument().addDocumentListener(new DocumentListener() {

            public void changedUpdate(final DocumentEvent e) {
                this.update();

            }

            public void insertUpdate(final DocumentEvent e) {
                this.update();

            }

            public void removeUpdate(final DocumentEvent e) {
                this.update();

            }

            private void update() {
                UPNPRouterPlugin.this.storage.put(UpnpRouterDevice.SERVICETYPE, UPNPRouterPlugin.this.serviceTypeTxt.getText());
                UPNPRouterPlugin.this.setCanCheckIP(true);
            }

        });
        this.controlURLTxt.getDocument().addDocumentListener(new DocumentListener() {

            public void changedUpdate(final DocumentEvent e) {
                this.update();

            }

            public void insertUpdate(final DocumentEvent e) {
                this.update();

            }

            public void removeUpdate(final DocumentEvent e) {
                this.update();

            }

            private void update() {
                UPNPRouterPlugin.this.storage.put(UpnpRouterDevice.CONTROLURL, UPNPRouterPlugin.this.controlURLTxt.getText());
                UPNPRouterPlugin.this.setCanCheckIP(true);
            }

        });

        return p;
    }

    @Override
    public String getID() {
        // TODO Auto-generated method stub
        return "SIMPLEUPNP";
    }

    private String getIP(final String serviceType, final String controlURL) throws GetIpException {
        String ipxml;

        try {
            ipxml = this.runCommand(serviceType, controlURL, "GetExternalIPAddress");

        } catch (final Exception e) {
            // this.storage.put(UPNPRouterPlugin.CANCHECKIP, false);
            throw new GetIpException("GetExternalIPAddress failed", e);
        }

        final Matcher ipm = Pattern.compile("<\\s*NewExternalIPAddress\\s*>\\s*(.*)\\s*<\\s*/\\s*NewExternalIPAddress\\s*>", Pattern.CASE_INSENSITIVE).matcher(ipxml);
        if (ipm.find()) {
            final String ip = ipm.group(1);
            if ("0.0.0.0".equals(ip)) {
                // Fritzbox returns 0.0.0.0 in offline mode

                return RouterPlugin.OFFLINE;

            }
            if (ip.matches(IPAddress.IP_PATTERN)) {

                return ip;
            } else {
                // TODO: differ between NA and OFFLINE
                return RouterPlugin.NOT_AVAILABLE;

            }
        }
        // this.storage.put(UPNPRouterPlugin.CANCHECKIP, false);
        throw new GetIpException("Could not get IP/No GetExternalIPAddress");
    }

    public int getIpCheckInterval() {
        // if ipcheck is done over upnp, we do not have to use long intervals
        return 1;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return "UPNP - Universal Plug & Play (Fritzbox,...)";
    }

    public int getWaittimeBeforeFirstIPCheck() {
        // if ipcheck is done over upnp, we do not have to use long intervals
        return 1;
    }

    @Override
    public boolean isIPCheckEnabled() {
        return this.storage.get(UPNPRouterPlugin.CANCHECKIP, true);
    }

    @Override
    public boolean isReconnectionEnabled() {
        return true;
    }

    private String runCommand(final String serviceType, final String controlUrl, final String command) throws IOException {
        final String data = "<?xml version='1.0' encoding='utf-8'?> <s:Envelope s:encodingStyle='http://schemas.xmlsoap.org/soap/encoding/' xmlns:s='http://schemas.xmlsoap.org/soap/envelope/'> <s:Body> <u:" + command + " xmlns:u='" + serviceType + "' /> </s:Body> </s:Envelope>";
        // this works for fritz box.
        // old code did NOT work:

        /*
         * 
         * final String data = "<?xml version=\"1.0\"?>\n" +
         * "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"
         * + " <s:Body>\n" + "  <m:" + command + " xmlns:m=\"" + serviceType +
         * "\"></m:" + command + ">\n" + " </s:Body>\n" + "</s:Envelope>"; try {
         * final URL url = new URL(controlUrl); final URLConnection conn =
         * url.openConnection(); conn.setDoOutput(true);
         * conn.addRequestProperty("Content-Type",
         * "text/xml; charset=\"utf-8\""); conn.addRequestProperty("SOAPAction",
         * serviceType + "#" + command + "\"");
         */
        final URL url = new URL(controlUrl);
        final URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        conn.addRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
        conn.addRequestProperty("SoapAction", serviceType + "#" + command);
        OutputStreamWriter wr = null;
        BufferedReader rd = null;
        try {
            wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String xmlstr = "";
            String nextln;
            while ((nextln = rd.readLine()) != null) {
                xmlstr += nextln.trim();
            }
            return xmlstr;

        } finally {
            if (wr != null) {
                wr.close();
            }
            if (rd != null) {
                rd.close();
            }
        }

    }

    protected ArrayList<UpnpRouterDevice> scanDevices() throws IOException {
        final String msg = "M-SEARCH * HTTP/1.1\r\n" + "HOST: 239.255.255.250:1900\r\n" + "ST: urn:schemas-upnp-org:device:InternetGatewayDevice:1\r\n" + "MAN: \"ssdp:discover\"\r\n" + "MX: 3\r\n\r\n";
        /*
         * TODO (NOT IMPORTANT) To simplify will not make a request for every
         * network interface, let java decide the network interface.
         */
        final MulticastSocket socket = new MulticastSocket();
        socket.setSoTimeout(5000);
        DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.length(), InetAddress.getByName("239.255.255.250"), 1900);
        socket.send(packet);
        // we need a bigger receive buffer.
        final byte[] buffer = new byte[500 * 1024];

        packet = new DatagramPacket(buffer, buffer.length);
        String response = null;
        final ArrayList<UpnpRouterDevice> foundDevices = new ArrayList<UpnpRouterDevice>();
        while (true) {
            try {
                socket.receive(packet);
            } catch (final Exception e) {
                break;
            }

            response = new String(packet.getData());

            Log.L.fine(response);
            URL url;
            for (final String location : new Regex(response, "LOCATION\\s*:\\s*([^\\s]+)").getColumn(0)) {
                try {
                    final UpnpRouterDevice device = new UpnpRouterDevice();
                    url = new URL(location);
                    device.setLocation(location);
                    // put(UPNPRouterPlugin.LOCATION, location);
                    final BufferedReader stream = new BufferedReader(new InputStreamReader(url.openStream()));
                    String xmlStr = "";
                    String ln;
                    while ((ln = stream.readLine()) != null) {
                        xmlStr += ln;
                    }
                    stream.close();
                    final Matcher m = Pattern.compile("(<.*?>)").matcher(xmlStr);
                    // lowercase all tags
                    final StringBuilder sb = new StringBuilder();
                    int last = 0;
                    while (m.find()) {
                        sb.append(xmlStr.substring(last, m.start()));
                        sb.append(m.group(0).toLowerCase());
                        last = m.end();
                    }
                    sb.append(xmlStr.substring(last));
                    xmlStr = sb.toString();
                    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder;

                    builder = factory.newDocumentBuilder();

                    final Document document = builder.parse(new InputSource(new StringReader(xmlStr)));
                    // ############## urlbase ##############
                    final Node base = document.getElementsByTagName(UpnpRouterDevice.URLBASE).item(0);
                    String urlbase = null;

                    if (base != null) {
                        urlbase = base.getTextContent().trim();
                        if (urlbase != null) {
                            urlbase = urlbase.trim();
                        }
                    }
                    if (urlbase == null) {
                        urlbase = url.getProtocol() + "://" + url.getHost();
                        if (url.getPort() != -1) {
                            urlbase += ":" + url.getPort();
                        }
                    } else if (urlbase.endsWith("/")) {
                        urlbase = urlbase.substring(0, urlbase.length() - 1);
                    }
                    device.setUrlBase(urlbase);
                    // .put(UPNPRouterPlugin.URLBASE, urlbase);
                    // ############## friendlyname ##############
                    Element el = null;
                    String friendlyname = null;
                    NodeList nodes = document.getElementsByTagName("devicetype");
                    for (int i = 0; i < nodes.getLength(); i++) {
                        if (nodes.item(i).getTextContent().toLowerCase().matches("urn:schemas-upnp-org:device:internetgatewaydevice:1")) {
                            el = (Element) nodes.item(i).getParentNode();
                            final NodeList nodes2 = nodes.item(i).getParentNode().getChildNodes();
                            for (int j = 0; j < nodes2.getLength(); j++) {
                                if (nodes2.item(j).getNodeName().matches(UpnpRouterDevice.FRIENDLYNAME)) {
                                    friendlyname = nodes2.item(j).getTextContent();
                                    // no sure why we do not use break here
                                    // device.put(UPNPRouterPlugin.FRIENDLYNAME,
                                    // friendlyname);
                                    device.setFriendlyname(friendlyname);
                                }
                            }
                        }
                    }
                    // ############## controlurl ##############
                    // ############## servicetype #############
                    // ################# wanip ################
                    nodes = el.getElementsByTagName(UpnpRouterDevice.SERVICETYPE);
                    for (int i = 0; i < nodes.getLength(); i++) {
                        // TODO (NOT IMPORTANT) To simplify will return
                        // after
                        // the
                        // first service matching
                        // "urn:schemas-upnp-org:service:wan[i|p]pp?connection:1"
                        if (nodes.item(i).getTextContent().toLowerCase().matches("urn:schemas-upnp-org:service:wan[i|p]pp?connection:1")) {
                            final String servicetype = nodes.item(i).getTextContent();
                            device.setServiceType(servicetype);
                            // .put(UPNPRouterPlugin.SERVICETYPE, servicetype);
                            final NodeList nodes2 = nodes.item(i).getParentNode().getChildNodes();
                            for (int j = 0; j < nodes2.getLength(); j++) {
                                if (nodes2.item(j).getNodeName().matches(UpnpRouterDevice.CONTROLURL)) {
                                    String controlurl = nodes2.item(j).getTextContent().trim();
                                    if (!controlurl.startsWith("/")) {
                                        controlurl = "/" + controlurl;
                                    }
                                    controlurl = urlbase + controlurl;
                                    device.setHost(url.getHost());
                                    // .put(UPNPRouterPlugin.HOST,
                                    // url.getHost());
                                    device.setControlURL(controlurl);
                                    // device.put(UPNPRouterPlugin.CONTROLURL,
                                    // controlurl);

                                    final Matcher service = Pattern.compile("wan([i|p]pp?)connection", Pattern.CASE_INSENSITIVE).matcher(servicetype);
                                    if (service.find()) {
                                        if (service.group(1).toLowerCase().equals("ppp")) {
                                            device.setWanservice(JDL.L("interaction.UpnpReconnect.wanservice.ppp", "Point-to-Point Protocol"));

                                        } else {
                                            device.setWanservice(JDL.L("interaction.UpnpReconnect.wanservice.ip", "Internet Protocol"));

                                        }
                                    }
                                    break;

                                }
                            }

                            foundDevices.add(device);

                        }
                    }
                } catch (final ParserConfigurationException e) {
                    Log.exception(e);
                } catch (final SAXException e) {
                    // TODO Auto-generated catch block
                    Log.exception(e);
                }

            }
        }
        socket.close();
        return foundDevices;
    }

    @Override
    public void setCanCheckIP(final boolean b) {
        this.storage.put(UPNPRouterPlugin.CANCHECKIP, b);

    }

    private void setDevice(final UpnpRouterDevice upnpRouterDevice) {
        if (upnpRouterDevice == null) {
            this.storage.clear();
        } else {
            JDLogger.getLogger().info(upnpRouterDevice + "");
            for (final Iterator<Entry<String, String>> it = upnpRouterDevice.entrySet().iterator(); it.hasNext();) {
                final Entry<String, String> next = it.next();
                this.storage.put(next.getKey(), next.getValue());
            }
            this.setCanCheckIP(true);
            try {
                final String ip = this.getIP(upnpRouterDevice.getServiceType(), upnpRouterDevice.getControlURL());
                this.storage.put(UPNPRouterPlugin.EXTERNALIP, ip);
            } catch (final GetIpException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            this.updateGUI();
        }
    }

    private void updateGUI() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {

                if (UPNPRouterPlugin.this.wanType != null) {
                    UPNPRouterPlugin.this.wanType.setText(UPNPRouterPlugin.this.storage.get(UpnpRouterDevice.FRIENDLYNAME, "") + " (" + UPNPRouterPlugin.this.storage.get(UpnpRouterDevice.WANSERVICE, "") + ")");

                    UPNPRouterPlugin.this.serviceTypeTxt.setText(UPNPRouterPlugin.this.storage.get(UpnpRouterDevice.SERVICETYPE, ""));
                    UPNPRouterPlugin.this.controlURLTxt.setText(UPNPRouterPlugin.this.storage.get(UpnpRouterDevice.CONTROLURL, ""));
                }
            }

        };
    }
}
