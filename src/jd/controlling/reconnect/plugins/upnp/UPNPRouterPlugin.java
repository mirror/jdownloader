package jd.controlling.reconnect.plugins.upnp;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jd.controlling.JDLogger;
import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.ReconnectInvoker;
import jd.controlling.reconnect.ReconnectResult;
import jd.controlling.reconnect.RouterPlugin;
import jd.controlling.reconnect.ipcheck.IP;
import jd.controlling.reconnect.ipcheck.IPCheckException;
import jd.controlling.reconnect.ipcheck.IPCheckProvider;
import jd.controlling.reconnect.ipcheck.InvalidIPRangeException;
import jd.controlling.reconnect.ipcheck.InvalidProviderException;
import jd.controlling.reconnect.plugins.upnp.translate.T;
import jd.gui.UserIO;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.Regex;
import org.appwork.utils.event.ProcessCallBack;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.advanced.AdvancedConfigManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class UPNPRouterPlugin extends RouterPlugin implements ActionListener, IPCheckProvider {

    public static final String            ID         = "SIMPLEUPNP";

    private static final String           CANCHECKIP = "cancheckip";

    private JButton                       find;
    private ExtTextField                  serviceTypeTxt;
    private ExtTextField                  controlURLTxt;
    private JLabel                        wanType;
    private JButton                       auto;
    protected ArrayList<UpnpRouterDevice> devices;

    private ImageIcon                     icon;

    private UPUPReconnectSettings         settings;

    public UPNPRouterPlugin() {
        super();
        icon = NewTheme.I().getIcon("upnp", 16);
        settings = JsonConfig.create(UPUPReconnectSettings.class);
        AdvancedConfigManager.getInstance().register(settings);
    }

    public void actionPerformed(final ActionEvent e) {
        // TODO: Do not use Appwork controller here
        // mixing two different Dialog controllers is not a good idea
        if (e.getSource() == this.auto) {
            final ProgressDialog dialog = new ProgressDialog(new ProgressGetter() {

                public int getProgress() {
                    return -1;
                }

                public String getString() {
                    return null;
                }

                public void run() throws Exception {
                    try {
                        UPNPRouterPlugin.this.devices = UPNPRouterPlugin.this.scanDevices();
                        if (devices.size() == 0) {
                            UserIO.getInstance().requestMessageDialog("Could not find any UPNP Devices. Try Live Header Reconnect instead!");
                        }
                    } catch (final IOException e) {
                        UserIO.getInstance().requestMessageDialog("Could not find any UPNP Devices. Try Live Header Reconnect instead!");
                    }

                }

            }, 0, JDL.L("jd.controlling.reconnect.plugins.upnp.UPNPRouterPlugin.actionPerformed.wizard.title", "UPNP Router Wizard"), JDL.L("jd.controlling.reconnect.plugins.upnp.UPNPRouterPlugin.actionPerformed.wizard.find.message", "Scanning all network interfaces"), null);
            dialog.setPreferredSize(new Dimension(500, 150));
            try {
                Dialog.getInstance().showDialog(dialog);
                if (this.devices != null && this.devices.size() > 0) {

                    runDetectionWizard(null);
                }
            } catch (DialogClosedException e1) {
                e1.printStackTrace();
            } catch (DialogCanceledException e1) {
                e1.printStackTrace();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }

        } else if (e.getSource() == this.find) {
            final ProgressDialog dialog = new ProgressDialog(new ProgressGetter() {

                public int getProgress() {
                    return -1;
                }

                public String getString() {
                    return null;
                }

                public void run() throws Exception {
                    try {
                        final ArrayList<UpnpRouterDevice> devices = UPNPRouterPlugin.this.scanDevices();
                        if (devices.size() == 0) {
                            UserIO.getInstance().requestMessageDialog("Could not find any UPNP Devices. Try Live Header Reconnect instead!");

                            return;
                        }
                        if (Thread.currentThread().isInterrupted()) { return; }
                        final int ret = UserIO.getInstance().requestComboDialog(UserIO.NO_COUNTDOWN, JDL.L("jd.controlling.reconnect.plugins.upnp.UPNPRouterPlugin.actionPerformed.wizard.title", "UPNP Router Wizard"), JDL.L("jd.controlling.reconnect.plugins.upnp.UPNPRouterPlugin.actionPerformed.wizard.find.message", "Scanning all network interfaces"), devices.toArray(new HashMap[] {}), 0, null, null, null, new DefaultListCellRenderer() {

                            private static final long serialVersionUID = 3607383089555373774L;

                            @Override
                            public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
                                final JLabel label = (JLabel) super.getListCellRendererComponent(list, ((UpnpRouterDevice) value).getModelname() + "(" + ((UpnpRouterDevice) value).getWanservice() + ")", index, isSelected, cellHasFocus);

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

            try {
                Dialog.getInstance().showDialog(dialog);
            } catch (DialogClosedException e1) {
                e1.printStackTrace();
            } catch (DialogCanceledException e1) {
                e1.printStackTrace();
            }
        }

    }

    /**
     * sets the correct router settings automatically
     * 
     * @throws InterruptedException
     */
    @Override
    public ArrayList<ReconnectResult> runDetectionWizard(ProcessCallBack processCallBack) throws InterruptedException {

        ArrayList<ReconnectResult> ret = new ArrayList<ReconnectResult>();
        if (devices == null) {
            try {
                this.devices = this.scanDevices();
            } catch (IOException e1) {
                Log.exception(e1);
                return null;
            }
        }
        for (final UpnpRouterDevice device : this.devices) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();

            ReconnectResult res;
            try {
                res = new UPNPReconnectInvoker(device.getServiceType(), device.getControlURL()).validate();
                if (res != null) ret.add(res);
            } catch (ReconnectException e) {
                e.printStackTrace();
            }

        }

        return ret;
    }

    public IP getExternalIP() throws IPCheckException {
        String ipxml;
        try {
            ipxml = UPNPReconnectInvoker.runCommand(settings.getServiceType(), settings.getControlURL(), "GetExternalIPAddress");
        } catch (final Exception e) {
            this.setCanCheckIP(false);

            throw new InvalidProviderException("UPNP Command Error");
        }
        try {
            final Matcher ipm = Pattern.compile("<\\s*NewExternalIPAddress\\s*>\\s*(.*)\\s*<\\s*/\\s*NewExternalIPAddress\\s*>", Pattern.CASE_INSENSITIVE).matcher(ipxml);
            if (ipm.find()) { return IP.getInstance(ipm.group(1)); }
        } catch (final InvalidIPRangeException e2) {
            throw new InvalidProviderException(e2);
        }
        this.setCanCheckIP(false);

        throw new InvalidProviderException("Unknown UPNP Response Error");
    }

    @Override
    public ImageIcon getIcon16() {
        return icon;
    }

    @Override
    public JComponent getGUI() {
        final JPanel p = new JPanel(new MigLayout("ins 0,wrap 3", "[][][grow,fill]", "[]"));
        this.find = new JButton(T._.literally_choose_router());
        this.find.addActionListener(this);
        this.auto = new JButton(T._.auto());
        this.auto.addActionListener(this);
        this.serviceTypeTxt = new ExtTextField();
        this.controlURLTxt = new ExtTextField();

        serviceTypeTxt.setHelpText(T._.servicetype_help());
        controlURLTxt.setHelpText(T._.controlURLTxt_help());

        this.wanType = new JLabel();
        p.add(this.auto, "aligny top,gapright 15,sg buttons");
        p.add(new JLabel(T._.literally_router()), "");
        p.add(this.wanType, "spanx");
        p.add(this.find, "aligny top,gapright 15,newline,sg buttons");
        p.add(new JLabel(T._.literally_service_type()), "");
        p.add(this.serviceTypeTxt);
        p.add(new JLabel(T._.literally_control_url()), "newline,skip");
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
                settings.setServiceType(UPNPRouterPlugin.this.serviceTypeTxt.getText());
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
                settings.setControlURL(UPNPRouterPlugin.this.controlURLTxt.getText());
                UPNPRouterPlugin.this.setCanCheckIP(true);
            }

        });

        return p;
    }

    @Override
    public String getID() {
        return UPNPRouterPlugin.ID;
    }

    public int getIpCheckInterval() {
        return 1;
    }

    public IPCheckProvider getIPCheckProvider() {
        if (!this.isIPCheckEnabled()) { return null; }
        return this;
    }

    @Override
    public String getName() {
        return "UPNP - Universal Plug & Play (Fritzbox,...)";
    }

    @Override
    public int getWaittimeBeforeFirstIPCheck() {
        // if ipcheck is done over upnp, we do not have to use long intervals
        return 0;
    }

    public boolean isIPCheckEnabled() {
        return settings.isIPCheckEnabled();
    }

    public ArrayList<UpnpRouterDevice> scanDevices() throws IOException {
        final String msg = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nST: urn:schemas-upnp-org:device:InternetGatewayDevice:1\r\nMAN: \"ssdp:discover\"\r\nMX: 3\r\n\r\n";
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
                    url = new URL(location);
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

                    // .put(UPNPRouterPlugin.URLBASE, urlbase);
                    // ############## Modelname ##############
                    Element el = null;
                    String friendname = null;
                    String modelname = null;
                    String manufactor = null;
                    NodeList nodes = document.getElementsByTagName("devicetype");
                    for (int i = 0; i < nodes.getLength(); i++) {
                        if (nodes.item(i).getTextContent().toLowerCase().matches("urn:schemas-upnp-org:device:internetgatewaydevice:1")) {
                            el = (Element) nodes.item(i).getParentNode();
                            final NodeList nodes2 = nodes.item(i).getParentNode().getChildNodes();
                            for (int j = 0; j < nodes2.getLength(); j++) {
                                if (nodes2.item(j).getNodeName().matches(UpnpRouterDevice.FRIENDLYNAME)) {
                                    friendname = nodes2.item(j).getTextContent();
                                    // no sure why we do not use break here
                                    // device.put(UPNPRouterPlugin.ModelNAME,
                                    // Modelname);
                                }
                                if (nodes2.item(j).getNodeName().matches(UpnpRouterDevice.MODELNAME)) {
                                    modelname = nodes2.item(j).getTextContent();
                                    // no sure why we do not use break here
                                    // device.put(UPNPRouterPlugin.ModelNAME,
                                    // Modelname);
                                }
                                if (nodes2.item(j).getNodeName().matches(UpnpRouterDevice.MANUFACTOR)) {
                                    manufactor = nodes2.item(j).getTextContent();
                                    // no sure why we do not use break here
                                    // device.put(UPNPRouterPlugin.ModelNAME,
                                    // Modelname);
                                }

                            }
                        }
                    }
                    // ############## controlurl ##############
                    // ############## servicetype #############
                    // ################# wanip ################
                    nodes = el.getElementsByTagName(UpnpRouterDevice.SERVICETYPE);
                    for (int i = 0; i < nodes.getLength(); i++) {
                        if (nodes.item(i).getTextContent().toLowerCase().matches("urn:schemas-upnp-org:service:wan[i|p]pp?connection:1")) {
                            /* add device to found list */
                            final UpnpRouterDevice device = new UpnpRouterDevice();
                            device.setUrlBase(urlbase);
                            device.setModelname(friendname);
                            device.setModelname(modelname);
                            device.setManufactor(manufactor);
                            device.setLocation(location);
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
                                            device.setWanservice(T._.interaction_UpnpReconnect_wanservice_ppp());

                                        } else {
                                            device.setWanservice(T._.interaction_UpnpReconnect_wanservice_ip());

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
                    Log.exception(e);
                }

            }
        }
        socket.close();
        return foundDevices;
    }

    public void setCanCheckIP(final boolean b) {
        settings.setIPCheckEnabled(b);

    }

    private void setDevice(final UpnpRouterDevice upnpRouterDevice) {
        if (upnpRouterDevice == null) {
            settings.setControlURL(null);
            settings.setModelName(null);
            settings.setIPCheckEnabled(false);
            settings.setServiceType(null);
            settings.setWANService(null);
        } else {
            JDLogger.getLogger().info(upnpRouterDevice + "");

            settings.setControlURL(upnpRouterDevice.getControlURL());
            settings.setModelName(upnpRouterDevice.getModelname());

            settings.setServiceType(upnpRouterDevice.getServiceType());
            settings.setWANService(upnpRouterDevice.getWanservice());

            this.setCanCheckIP(true);

            this.updateGUI();
        }
    }

    private void updateGUI() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {

                if (UPNPRouterPlugin.this.wanType != null) {
                    try {
                        UPNPRouterPlugin.this.wanType.setText(settings.getModelName() + (settings.getWANService().length() > 0 ? " (" + settings.getWANService() + ")" : ""));
                    } catch (final Throwable e) {
                    }
                    try {
                        UPNPRouterPlugin.this.serviceTypeTxt.setText(settings.getServiceType());
                    } catch (final Throwable e) {
                    }
                    try {
                        UPNPRouterPlugin.this.controlURLTxt.setText(settings.getControlURL());
                    } catch (final Throwable e) {
                    }
                    // final String ipcheckEnabled =
                    // UPNPRouterPlugin.this.isIPCheckEnabled() ?
                    // Loc.L("jd.controlling.reconnect.plugins.upnp.UPNPRouterPlugin.text.yes",
                    // "Yes") :
                    // Loc.L("jd.controlling.reconnect.plugins.upnp.UPNPRouterPlugin.text.no",
                    // "No");

                }
            }

        };
    }

    @Override
    public ReconnectInvoker getReconnectInvoker() {
        return new UPNPReconnectInvoker(settings.getServiceType(), settings.getControlURL());
    }
}