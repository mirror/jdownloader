package jd.controlling.reconnect.pluginsinc.upnp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jd.controlling.reconnect.pluginsinc.upnp.translate.T;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.Regex;
import org.appwork.utils.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class UPNPScanner implements Runnable {
    private java.util.List<UpnpRouterDevice> foundDevices;

    private UPNPScanner() {
        // ThreadGroup grp = new
        // ThreadGroup(Thread.currentThread().getThreadGroup(), "UPNPScanner");

    }

    final static String MSG = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nST: urn:schemas-upnp-org:device:InternetGatewayDevice:1\r\nMAN: \"ssdp:discover\"\r\nMX: 3\r\n\r\n";

    public synchronized java.util.List<UpnpRouterDevice> scan() throws InterruptedException {

        foundDevices = new ArrayList<UpnpRouterDevice>();
        /*
         * TODO (NOT IMPORTANT) To simplify will not make a request for every network interface, let java decide the network interface.
         */

        Thread th = new Thread(this, "UPNPScanner");
        th.start();
        try {
            th.join();
        } catch (InterruptedException e) {
            close();
            throw e;
        }
        return foundDevices;

    }

    private void close() {
        try {
            socket.close();

        } catch (Throwable e1) {

        }
    }

    private MulticastSocket socket;

    public void run() {

        try {
            socket = new MulticastSocket();
            socket.setSoTimeout(5000);
            DatagramPacket packet = new DatagramPacket(MSG.getBytes("UTF-8"), MSG.length(), InetAddress.getByName("239.255.255.250"), 1900);
            socket.send(packet);
            // we need a bigger receive buffer.
            final byte[] buffer = new byte[500 * 1024];

            packet = new DatagramPacket(buffer, buffer.length);
            String response = null;
            while (true) {

                socket.receive(packet);

                response = new String(packet.getData(), "UTF-8");

                Log.L.fine(response);
                URL url;
                for (final String location : new Regex(response, "LOCATION\\s*:\\s*([^\\s]+)").getColumn(0)) {
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
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
                            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                            if (nodes.item(i).getTextContent().toLowerCase().matches("urn:schemas-upnp-org:device:internetgatewaydevice:1")) {
                                el = (Element) nodes.item(i).getParentNode();
                                final NodeList nodes2 = nodes.item(i).getParentNode().getChildNodes();
                                for (int j = 0; j < nodes2.getLength(); j++) {
                                    if (nodes2.item(j).getNodeName().matches(UpnpRouterDevice.FRIENDLYNAME)) {
                                        friendname = nodes2.item(j).getTextContent();
                                        // no sure why we do not use
                                        // break here
                                        // device.put(UPNPRouterPlugin.ModelNAME,
                                        // Modelname);
                                    }
                                    if (nodes2.item(j).getNodeName().matches(UpnpRouterDevice.MODELNAME)) {
                                        modelname = nodes2.item(j).getTextContent();
                                        // no sure why we do not use
                                        // break here
                                        // device.put(UPNPRouterPlugin.ModelNAME,
                                        // Modelname);
                                    }
                                    if (nodes2.item(j).getNodeName().matches(UpnpRouterDevice.MANUFACTOR)) {
                                        manufactor = nodes2.item(j).getTextContent();
                                        // no sure why we do not use
                                        // break here
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
                                // .put(UPNPRouterPlugin.SERVICETYPE,
                                // servicetype);
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
        } catch (IOException e) {
            Log.exception(e);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    public static void main(String[] args) {
        try {
            Log.L.setLevel(Level.ALL);
            System.out.println(JSonStorage.toString(INSTANCE.scan()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private static UPNPScanner INSTANCE = new UPNPScanner();

    public static java.util.List<UpnpRouterDevice> scanDevices() throws InterruptedException {
        return INSTANCE.scan();
    }

}
