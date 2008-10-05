package jd.router;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.device.SearchResponseListener;
import org.cybergarage.upnp.ssdp.SSDPPacket;
import org.hsqldb.lib.StringInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import jd.parser.Regex;

import jd.http.Browser;

public class UPnPInfo {

    private String host=null;
    private SSDPPacket ssdpP =null;
    public String met = null;
    public HashMap<String, String> SCPDs = null;

    // protected String
    private void getSCPDURLs(String location) throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        String page = new Browser().getPage(location);
        StringInputStream input = new StringInputStream(page);
        Document document = builder.parse(input);

        if (SCPDs == null) SCPDs = new HashMap<String, String>();
        SCPDs.put(location, page);
        // ---- Get list of nodes to given element tag name ----
        NodeList ndList = document.getElementsByTagName("SCPDURL");
        for (int i = 0; i < ndList.getLength(); i++) {
            getSCPDURLs(ssdpP.getLocation().replaceFirst("(http://.*?)/.*", "$1/" + ndList.item(i).getTextContent().replaceFirst("^\\/", "")));

        }
    }

    public UPnPInfo(String ipadress) {
        this.host = ipadress;
        final ControlPoint c = new ControlPoint();
        c.start();
        c.addSearchResponseListener(new SearchResponseListener() {

            public void deviceSearchResponseReceived(SSDPPacket ssdpPacket) {
                InetAddress ia = ssdpPacket.getRemoteInetAddress();
                if (ia.getHostAddress().endsWith(host) || ia.getHostName().endsWith(host)) {
                    ssdpP = ssdpPacket;
                    c.stop();
                }
            }
        });
        int d = 0;
        while (ssdpP == null) {
            if (d++ == 1000) {
                c.stop();
                return;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try {
            // ---- Parse XML file ----
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            if (ssdpP.getLocation() == null) return;
            Document document = builder.parse(new Browser().openGetConnection(ssdpP.getLocation()).getInputStream());

            // ---- Get list of nodes to given element tag name ----
            NodeList ndList = document.getElementsByTagName("serviceType");
            // printNodesFromList( ndList ); // printNodesFromList see below
            Node node = null;
            for (int i = 0; i < ndList.getLength(); i++) {
                node = ndList.item(i);
                if (node.getFirstChild().getTextContent().contains("WANIPConnection")) break;
                node = null;
            }
            NodeList cl = node.getParentNode().getChildNodes();
            HashMap<String, String> meth = new HashMap<String, String>();
            // System.out.println(cl.getLength());
            for (int i = 0; i < cl.getLength(); i++) {
                Node cln = cl.item(i);
                if (cln.hasChildNodes()) {
                    meth.put(cln.getNodeName(), cln.getTextContent().trim());
                }
            }
            if (!meth.containsKey("serviceType") || !meth.containsKey("controlURL") || !meth.containsKey("SCPDURL")) {
                getSCPDURLs(ssdpP.getLocation());
                return;
            }
            if (!new Browser().getPage(ssdpP.getLocation().replaceFirst("(http://.*?)/.*", "$1/" + meth.get("SCPDURL").replaceFirst("^\\/", ""))).contains("ForceTermination")) {
                getSCPDURLs(ssdpP.getLocation());
                return;
            }
            met = "[[[HSRC]]]\r\n[[[STEP]]]\r\n[[[REQUEST]]]\r\n";
            met += "POST " + meth.get("controlURL") + " HTTP/1.1\r\n";
            String hostport = "";
            if (ssdpP.getLocation().matches(".*\\:[\\d]+.*")) hostport = new Regex(ssdpP.getLocation(), ".*(\\:[\\d]+)").getMatch(0);
            met += "Host: %%%routerip%%%" + hostport + "\r\n";
            met += "Content-Type: text/xml; charset=\"utf-8\"\r\n";
            met += "SoapAction:" + meth.get("serviceType") + "#ForceTermination\r\n";
            met += "<?xml version='1.0' encoding='utf-8'?> <s:Envelope s:encodingStyle='http://schemas.xmlsoap.org/soap/encoding/' xmlns:s='http://schemas.xmlsoap.org/soap/envelope/'> <s:Body> <u:ForceTermination xmlns:u='" + meth.get("serviceType") + "' /> </s:Body> </s:Envelope>\r\n";
            met += "[[[/REQUEST]]]\r\n[[[/STEP]]]\r\n[[[/HSRC]]]";

            //System.out.println(ndList.item(0).getParentNode().getChildNodes().
            // item(1).getFirstChild());
            // ---- Error handling ----
        } catch (SAXParseException spe) {
            System.out.println("\n** Parsing error, line " + spe.getLineNumber() + ", uri " + spe.getSystemId());
            System.out.println("   " + spe.getMessage());
            Exception e = (spe.getException() != null) ? spe.getException() : spe;
            e.printStackTrace();
        } catch (SAXException sxe) {
            Exception e = (sxe.getException() != null) ? sxe.getException() : sxe;
            e.printStackTrace();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }
}
