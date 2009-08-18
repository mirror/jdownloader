package jd.nrouter;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import jd.controlling.JDLogger;
import jd.http.Browser;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class UpnpDevice {

    private Browser deviceBrowser;
    private String location;

    private Document deviceDocument;

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
            String page = deviceBrowser.getPage(location);
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setNamespaceAware(true);
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            deviceDocument = builder.parse(new InputSource(new ByteArrayInputStream(page.getBytes())));

            JDLogger.getLogger().finest(page.toString());

        } catch (Exception e) {
            JDLogger.exception(e);
        }
    }

    public String getProperty(String string) {
        // TODO Auto-generated method stub
        return null;
    }

  

}
