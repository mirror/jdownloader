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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import jd.controlling.JDLogger;
import jd.http.Browser;

import org.w3c.dom.Document;
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
