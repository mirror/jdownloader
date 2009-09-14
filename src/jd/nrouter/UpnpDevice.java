//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.nrouter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jd.controlling.JDLogger;
import jd.http.Browser;
import jd.http.RequestHeader;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class UpnpDevice {

    private Browser deviceBrowser;
    private String location;

    private Document deviceDocument;
    private String scpd;
    private String serviceType;
    private String serviceId;
    private String controlURL;
    private String eventSubURL;
    private String port;

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public UpnpDevice(String location) {
        try {
            this.location = location;

            deviceBrowser = new Browser();

            loadDevice();
            JDLogger.getLogger().finest(deviceBrowser.toString());

            String[] services = deviceBrowser.getRegex("<service>(.+?)</service>").getColumn(0);

            for (String service : services) {
                if (service.contains("WANIPConnection")) {
                    scpd = new Regex(service, "<SCPDURL>(.+?)</SCPDURL>").getMatch(0);
                    serviceType = new Regex(service, "<serviceType>(.+?)</serviceType>").getMatch(0);
                    serviceId = new Regex(service, "<serviceId>(.+?)</serviceId>").getMatch(0);
                    controlURL = new Regex(service, "<controlURL>(.+?)</controlURL>").getMatch(0);
                    eventSubURL = new Regex(service, "<eventSubURL>(.+?)</eventSubURL>").getMatch(0);

                    
                   
                    String ss = deviceBrowser.getPage(scpd);
                    JDLogger.getLogger().finest(deviceBrowser.toString());

                    JDLogger.getLogger().finest(call("GetExternalIPAddress"));
                   
                }
            }

        } catch (Exception e) {
            JDLogger.exception(e);
        }
    }
/**
 * loads the devicelocation into deviceBrowser.
 *
 * @throws IOException
 * @throws SAXException
 * @throws ParserConfigurationException
 */
    public void loadDevice() throws IOException, SAXException, ParserConfigurationException {
        String page = deviceBrowser.getPage(location);
//        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
//        domFactory.setNamespaceAware(true);
//        DocumentBuilder builder = domFactory.newDocumentBuilder();
//        deviceDocument = builder.parse(new InputSource(new ByteArrayInputStream(page.getBytes())));

    }

    public String call(String action) throws IOException {
      
        HashMap<String, String> h = new HashMap<String, String>();
        h.put("SoapAction", serviceType+"#"+action);
        h.put("CONTENT-TYPE", "text/xml; charset=\"utf-8\"");
        deviceBrowser.setHeaders(new RequestHeader(h));
        String con = deviceBrowser.postPage(controlURL, "<?xml version='1.0' encoding='utf-8'?> <s:Envelope s:encodingStyle='http://schemas.xmlsoap.org/soap/encoding/' xmlns:s='http://schemas.xmlsoap.org/soap/envelope/'> <s:Body> <u:GetStatusInfo xmlns:u='"+serviceType+"' /> </s:Body> </s:Envelope>");

        /*
         * 
         * HTTP/1.1 Host: %%%routerip%%%:49000 Content-Type: text/xml;
         * charset="utf-8"
         * SoapAction:urn:schemas-upnp-org:service:WANIPConnection
         * :1#ForceTermination
         * 
         * <?xml version='1.0' encoding='utf-8'?> <s:Envelope
         * s:encodingStyle='http://schemas.xmlsoap.org/soap/encoding/'
         * xmlns:s='http://schemas.xmlsoap.org/soap/envelope/'> <s:Body>
         * <u:ForceTermination
         * xmlns:u='urn:schemas-upnp-org:service:WANIPConnection:1' /> </s:Body>
         * </s:Envelope>
         */

        return con;
    }

    public String getProperty(String string) {
        // TODO Auto-generated method stub
        return null;
    }

}
