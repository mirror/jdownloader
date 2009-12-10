//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.router;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jd.controlling.JDLogger;
import jd.http.Browser;
import jd.nutils.Threader;
import jd.nutils.jobber.JDRunnable;
import jd.parser.Regex;

import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.device.SearchResponseListener;
import org.cybergarage.upnp.ssdp.SSDPPacket;
import org.hsqldb.lib.StringInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class UPnPInfo {

    private InetAddress host = null;
    private SSDPPacket ssdpP = null;
    public ArrayList<String> met = new ArrayList<String>();
    public HashMap<String, String> SCPDs = null;

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

    public UPnPInfo(InetAddress ipadress) {
        this(ipadress, 10000);
    }

    private static ArrayList<String> createUpnpReconnect(HashMap<String, String> SCPDs, String desc) throws ParserConfigurationException, SAXException, IOException {

        StringInputStream input = new StringInputStream(SCPDs.get(desc));
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(input);
        ArrayList<String> terminations = new ArrayList<String>();
        ArrayList<String> ret = new ArrayList<String>();
        for (Entry<String, String> ent : SCPDs.entrySet()) {
            if (ent.getValue().contains("<name>ForceTermination</name>")) {
                terminations.add(ent.getKey().replaceFirst(".*?\\:\\d+", ""));
            }

        }
        if (terminations.size() == 0) return ret;
        NodeList ndList = document.getElementsByTagName("SCPDURL");
        // printNodesFromList( ndList ); // printNodesFromList see below

        Node node = null;
        for (int j = 0; j < ndList.getLength(); j++) {
            node = ndList.item(j);
            if (node.getTextContent() != null) {
                for (String string : terminations) {
                    if (node.getFirstChild().getTextContent().contains(string)) {
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
                            continue;
                        }
                        String mett = "[[[HSRC]]]\r\n[[[STEP]]]\r\n[[[REQUEST]]]\r\n";
                        mett += "POST " + meth.get("controlURL") + " HTTP/1.1\r\n";
                        String hostport = new Regex(SCPDs.keySet().iterator().next(), ".*(\\:[\\d]+)").getMatch(0);
                        mett += "Host: %%%routerip%%%" + hostport + "\r\n";
                        mett += "Content-Type: text/xml; charset=\"utf-8\"\r\n";
                        mett += "SoapAction:" + meth.get("serviceType") + "#ForceTermination\r\n\r\n";
                        mett += "<?xml version='1.0' encoding='utf-8'?> <s:Envelope s:encodingStyle='http://schemas.xmlsoap.org/soap/encoding/' xmlns:s='http://schemas.xmlsoap.org/soap/envelope/'> <s:Body> <u:ForceTermination xmlns:u='" + meth.get("serviceType") + "' /> </s:Body> </s:Envelope>\r\n";
                        mett += "[[[/REQUEST]]]\r\n[[[/STEP]]]\r\n[[[/HSRC]]]";
                        ret.add(mett);
                    }
                }
            }

            node = null;
        }
        return ret;
    }

    public static ArrayList<String> createUpnpReconnect(HashMap<String, String> SCPDs) throws SAXException, IOException, ParserConfigurationException {
        for (Entry<String, String> ent : SCPDs.entrySet()) {
            if (ent.getValue().contains("</UDN>")) { return createUpnpReconnect(SCPDs, ent.getKey()); }
        }

        return null;
    }

    public UPnPInfo(InetAddress ipaddress, final long waittime) {
        this.host = ipaddress;
        if (host == null) host = RouterInfoCollector.getRouterIP();
        if (host == null) return;
        final ControlPoint c = new ControlPoint();
        c.start();
        final Threader th = new Threader();
        th.add(new JDRunnable() {

            public void go() throws Exception {
                try {
                    Thread.sleep(waittime);
                } catch (InterruptedException e) {
                }
                c.stop();
                th.interrupt();

            }
        });
        c.addSearchResponseListener(new SearchResponseListener() {

            public void deviceSearchResponseReceived(SSDPPacket ssdpPacket) {
                InetAddress ia = ssdpPacket.getRemoteInetAddress();
                if (ia.getHostAddress().equals(host.getHostAddress())) {
                    ssdpP = ssdpPacket;
                    c.stop();
                    th.interrupt();
                }
            }
        });
        th.startAndWait();
        if (ssdpP == null) return;
        try {
            // ---- Parse XML file ----
            if (ssdpP.getLocation() == null) return;

            getSCPDURLs(ssdpP.getLocation());
            met = createUpnpReconnect(SCPDs, ssdpP.getLocation());
            // ---- Error handling ----
        } catch (SAXParseException spe) {
            System.out.println("\n** Parsing error, line " + spe.getLineNumber() + ", uri " + spe.getSystemId());
            System.out.println("   " + spe.getMessage());
            Exception e = (spe.getException() != null) ? spe.getException() : spe;
            JDLogger.exception(e);
        } catch (SAXException sxe) {
            Exception e = (sxe.getException() != null) ? sxe.getException() : sxe;
            JDLogger.exception(e);
        } catch (ParserConfigurationException pce) {
            JDLogger.exception(pce);
        } catch (IOException ioe) {
            JDLogger.exception(ioe);
        }

    }

}
