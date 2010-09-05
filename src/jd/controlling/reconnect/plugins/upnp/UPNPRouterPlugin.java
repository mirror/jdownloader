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

import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
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

import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.controlling.reconnect.plugins.RouterPlugin;
import jd.controlling.reconnect.plugins.RouterPluginException;
import jd.gui.UserIO;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.utils.Regex;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class UPNPRouterPlugin extends RouterPlugin implements ActionListener {
    private static final String LOCATION     = "location";
    private static final String URLBASE      = "urlbase";
    private static final String FRIENDLYNAME = "friendlyname";
    private static final String HOST         = "host";
    private static final String CONTROLURL   = "controlurl";
    private static final String WANSERVICE   = "wanservice";
    private static final String SERVICETYPE  = "servicetype";

    private static final String EXTERNALIP   = "externalip";
    private static final String CANCHECKIP   = "cancheckip";

    private final Storage       storage;
    private JButton             button;
    private JTextField          serviceTypeTxt;
    private JTextField          controlURLTxt;
    private JLabel              wanType;

    public UPNPRouterPlugin() {
        this.storage = JSonStorage.getPlainStorage(this.getID());
    }

    public void actionPerformed(final ActionEvent e) {
        // TODO: Do not use Appwork controller here
        // mixing two different Dialog controllers is not a good idea

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
                    final ArrayList<HashMap<String, String>> devices = UPNPRouterPlugin.this.scanDevices();
                    if (Thread.currentThread().isInterrupted()) { return; }
                    final int ret = UserIO.getInstance().requestComboDialog(0, "Select Router", "Please select the router that handles your internet connection", devices.toArray(new HashMap[] {}), 0, null, null, null, new DefaultListCellRenderer() {

                        private static final long serialVersionUID = 3607383089555373774L;

                        @SuppressWarnings("unchecked")
                        @Override
                        public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
                            final JLabel label = (JLabel) super.getListCellRendererComponent(list, ((HashMap<String, String>) value).get(UPNPRouterPlugin.FRIENDLYNAME), index, isSelected, cellHasFocus);

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

    @Override
    public boolean canCheckIP() {
        // TODO Auto-generated method stub
        return this.storage.get(UPNPRouterPlugin.CANCHECKIP, true);
    }

    @Override
    public boolean canRefreshIP() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean doReconnect(final ProgressController progress) {

        try {
            this.runCommand(this.storage.get(UPNPRouterPlugin.SERVICETYPE, ""), this.storage.get(UPNPRouterPlugin.CONTROLURL, ""), "ForceTermination");
            Thread.sleep(2000);
            this.runCommand(this.storage.get(UPNPRouterPlugin.SERVICETYPE, ""), this.storage.get(UPNPRouterPlugin.CONTROLURL, ""), "RequestConnection");
            return true;
        } catch (final Throwable e) {
            return false;
        }

    }

    @Override
    public String getExternalIP() throws RouterPluginException {

        String ipxml;

        try {
            ipxml = this.runCommand(this.storage.get(UPNPRouterPlugin.SERVICETYPE, ""), this.storage.get(UPNPRouterPlugin.CONTROLURL, ""), "GetExternalIPAddress");

        } catch (final Exception e) {
            this.storage.put(UPNPRouterPlugin.CANCHECKIP, false);
            throw new RouterPluginException(e);
        }

        final Matcher ipm = Pattern.compile("<\\s*NewExternalIPAddress\\s*>\\s*(.*)\\s*<\\s*/\\s*NewExternalIPAddress\\s*>", Pattern.CASE_INSENSITIVE).matcher(ipxml);
        if (ipm.find()) {
            final String ip = ipm.group(1);

            if (ip.matches("(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)")) {
                this.storage.put(UPNPRouterPlugin.EXTERNALIP, ip);

                return ip;
            } else {
                throw new RouterPluginException("Invalid IP");
            }
        }
        this.storage.put(UPNPRouterPlugin.CANCHECKIP, false);
        throw new RouterPluginException("Could not get IP/No GetExternalIPAddress");

    }

    @Override
    public JComponent getGUI() {
        final JPanel p = new JPanel(new MigLayout("ins 0,wrap 3", "[][][grow,fill]", "[]"));
        this.button = new JButton("Find Routers");
        this.button.addActionListener(this);
        this.serviceTypeTxt = new JTextField();
        this.controlURLTxt = new JTextField();
        this.wanType = new JLabel();
        p.add(this.button, "aligny top,gapright 15");
        p.add(new JLabel("Router"), "");
        p.add(this.wanType, "spanx");
        p.add(new JLabel("Service Type"), "newline,skip");
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
                UPNPRouterPlugin.this.storage.put(UPNPRouterPlugin.SERVICETYPE, UPNPRouterPlugin.this.serviceTypeTxt.getText());
                UPNPRouterPlugin.this.storage.put(UPNPRouterPlugin.CANCHECKIP, true);
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
                UPNPRouterPlugin.this.storage.put(UPNPRouterPlugin.CONTROLURL, UPNPRouterPlugin.this.controlURLTxt.getText());
                UPNPRouterPlugin.this.storage.put(UPNPRouterPlugin.CANCHECKIP, true);
            }

        });

        return p;
    }

    @Override
    public String getID() {
        // TODO Auto-generated method stub
        return "SIMPLEUPNP";
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return "UPNP - Universal Plug & Play (Fritzbox,...)";
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

    protected ArrayList<HashMap<String, String>> scanDevices() throws IOException {
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
        final ArrayList<HashMap<String, String>> foundDevices = new ArrayList<HashMap<String, String>>();
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
                    final HashMap<String, String> device = new HashMap<String, String>();
                    url = new URL(location);
                    device.put(UPNPRouterPlugin.LOCATION, location);
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
                    final Node base = document.getElementsByTagName(UPNPRouterPlugin.URLBASE).item(0);
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
                    device.put(UPNPRouterPlugin.URLBASE, urlbase);
                    // ############## friendlyname ##############
                    Element el = null;
                    String friendlyname = null;
                    NodeList nodes = document.getElementsByTagName("devicetype");
                    for (int i = 0; i < nodes.getLength(); i++) {
                        if (nodes.item(i).getTextContent().toLowerCase().matches("urn:schemas-upnp-org:device:internetgatewaydevice:1")) {
                            el = (Element) nodes.item(i).getParentNode();
                            final NodeList nodes2 = nodes.item(i).getParentNode().getChildNodes();
                            for (int j = 0; j < nodes2.getLength(); j++) {
                                if (nodes2.item(j).getNodeName().matches(UPNPRouterPlugin.FRIENDLYNAME)) {
                                    friendlyname = nodes2.item(j).getTextContent();
                                    // no sure why we do not use break here
                                    device.put(UPNPRouterPlugin.FRIENDLYNAME, friendlyname);
                                }
                            }
                        }
                    }
                    // ############## controlurl ##############
                    // ############## servicetype #############
                    // ################# wanip ################
                    nodes = el.getElementsByTagName(UPNPRouterPlugin.SERVICETYPE);
                    for (int i = 0; i < nodes.getLength(); i++) {
                        // TODO (NOT IMPORTANT) To simplify will return
                        // after
                        // the
                        // first service matching
                        // "urn:schemas-upnp-org:service:wan[i|p]pp?connection:1"
                        if (nodes.item(i).getTextContent().toLowerCase().matches("urn:schemas-upnp-org:service:wan[i|p]pp?connection:1")) {
                            final String servicetype = nodes.item(i).getTextContent();
                            device.put(UPNPRouterPlugin.SERVICETYPE, servicetype);
                            final NodeList nodes2 = nodes.item(i).getParentNode().getChildNodes();
                            for (int j = 0; j < nodes2.getLength(); j++) {
                                if (nodes2.item(j).getNodeName().matches(UPNPRouterPlugin.CONTROLURL)) {
                                    String controlurl = nodes2.item(j).getTextContent().trim();
                                    if (!controlurl.startsWith("/")) {
                                        controlurl = "/" + controlurl;
                                    }
                                    controlurl = urlbase + controlurl;
                                    device.put(UPNPRouterPlugin.HOST, url.getHost());
                                    device.put(UPNPRouterPlugin.CONTROLURL, controlurl);

                                    final Matcher service = Pattern.compile("wan([i|p]pp?)connection", Pattern.CASE_INSENSITIVE).matcher(servicetype);
                                    if (service.find()) {
                                        if (service.group(1).toLowerCase().equals("ppp")) {
                                            device.put(UPNPRouterPlugin.WANSERVICE, JDL.L("interaction.UpnpReconnect.wanservice.ppp", "Point-to-Point Protocol"));

                                        } else {
                                            device.put(UPNPRouterPlugin.WANSERVICE, JDL.L("interaction.UpnpReconnect.wanservice.ip", "Internet Protocol"));

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

    private void setDevice(final HashMap<String, String> hashMap) {
        JDLogger.getLogger().info(hashMap + "");
        for (final Iterator<Entry<String, String>> it = hashMap.entrySet().iterator(); it.hasNext();) {
            final Entry<String, String> next = it.next();
            this.storage.put(next.getKey(), next.getValue());
        }
        this.storage.put(UPNPRouterPlugin.CANCHECKIP, true);
        this.updateGUI();
    }

    private void updateGUI() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {

                UPNPRouterPlugin.this.wanType.setText(UPNPRouterPlugin.this.storage.get(UPNPRouterPlugin.FRIENDLYNAME, "") + " (" + UPNPRouterPlugin.this.storage.get(UPNPRouterPlugin.WANSERVICE, "") + ")");
                UPNPRouterPlugin.this.serviceTypeTxt.setText(UPNPRouterPlugin.this.storage.get(UPNPRouterPlugin.SERVICETYPE, ""));
                UPNPRouterPlugin.this.controlURLTxt.setText(UPNPRouterPlugin.this.storage.get(UPNPRouterPlugin.CONTROLURL, ""));
            }

        };
    }
}
