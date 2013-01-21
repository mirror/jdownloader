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

package jd.plugins.decrypter;

import java.io.CharArrayWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.logging.Level;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "metalinker.org" }, urls = { "http://[\\d\\w\\.:\\-@]*/.*?\\.metalink" }, flags = { 0 })
public class MtLnk extends PluginForDecrypt {

    private ArrayList<DownloadLink> decryptedLinks;

    /* we use identity as package name if available */
    private String                  packageName   = null;

    private String                  publisherName = null;

    private String                  publisherURL  = null;

    public MtLnk(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        String metalink = br.getPage(parameter.getCryptedUrl());
        return decryptString(metalink);
    }

    public boolean pluginAPI(String method, Object input, Object output) throws Exception {
        if ("decryptString".equalsIgnoreCase(method)) {
            ((ArrayList<DownloadLink>) output).addAll(decryptString((String) input));
            return true;
        }
        return false;
    }

    public ArrayList<DownloadLink> decryptString(String metalink) {
        decryptedLinks = new ArrayList<DownloadLink>();
        DefaultHandler handler = new MetalinkSAXHandler();
        // Use the default (non-validating) parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            // Parse the input
            SAXParser saxParser = factory.newSAXParser();
            StringReader input = new StringReader(metalink);
            saxParser.parse(new InputSource(input), handler);
        } catch (Throwable t) {
            logger.log(Level.SEVERE, t.getMessage(), t);
        }

        if (packageName != null) {
            FilePackage pgk = FilePackage.getInstance();
            pgk.setName(packageName);
            if (publisherName != null && publisherURL != null) {
                pgk.setComment(publisherName + " (" + publisherURL + ")");
            } else if (publisherName != null) {
                pgk.setComment(publisherName);
            } else if (publisherURL != null) {
                pgk.setComment(publisherURL);
            }
            pgk.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    public class MetalinkSAXHandler extends DefaultHandler {
        private CharArrayWriter text  = new CharArrayWriter();
        Attributes              atr;
        String                  path  = "";
        private DownloadLink    dLink = null;

        public MetalinkSAXHandler() {
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equals("file")) {
                /* new file begins */
                dLink = new DownloadLink(null, null, null, null, true);
            }
            path += "." + qName;
            this.atr = attributes;
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (path.equalsIgnoreCase(".metalink.identity")) {
                packageName = text.toString().trim();
            } else if (path.equalsIgnoreCase(".metalink.publisher.name")) {
                publisherName = text.toString().trim();
            } else if (path.equalsIgnoreCase(".metalink.publisher.url")) {
                publisherURL = text.toString().trim();
            } else if (path.equalsIgnoreCase(".metalink.files.file.size")) {
                dLink.setDownloadSize(Long.parseLong(text.toString().trim()));
            } else if (path.equalsIgnoreCase(".metalink.files.file.verification.hash")) {
                if (atr.getValue("type").equalsIgnoreCase("md5")) {
                    dLink.setMD5Hash(text.toString().trim());
                } else if (atr.getValue("type").equalsIgnoreCase("sha1")) {
                    dLink.setSha1Hash(text.toString().trim());
                }
            } else if (path.equalsIgnoreCase(".metalink.files.file.resources.url")) {
                DownloadLink downloadLink = createDownloadlink(text.toString().trim());
                try {
                    /* needed to avoid rename by plugin */
                    downloadLink.forceFileName(dLink.getFinalFileName());
                } catch (Throwable e) {
                    /* forceFileName not available in 0.957 public */
                }
                downloadLink.setFinalFileName(dLink.getFinalFileName());
                downloadLink.setDownloadSize(dLink.getDownloadSize());
                downloadLink.setMD5Hash(dLink.getMD5Hash());
                downloadLink.setSha1Hash(dLink.getSha1Hash());
                decryptedLinks.add(downloadLink);
            }
            path = path.substring(0, path.length() - qName.length() - 1);
            text.reset();
        }

        public void characters(char[] ch, int start, int length) {
            text.write(ch, start, length);
        }
    }
}
