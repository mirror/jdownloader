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
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.UnknownCrawledLinkHandler;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "metalinker.org" }, urls = { "http://[\\d\\w\\.:\\-@]*/.*?\\.(metalink|meta4)" })
public class MtLnk extends PluginForDecrypt {

    private ArrayList<DownloadLink>         decryptedLinks;

    /* we use identity as package name if available */
    private String                          packageName   = null;

    private String                          publisherName = null;

    private String                          publisherURL  = null;

    final private UnknownCrawledLinkHandler handler       = new UnknownCrawledLinkHandler() {

                                                              @Override
                                                              public void unhandledCrawledLink(CrawledLink link, LinkCrawler lc) {
                                                                  final DownloadLink dlLink = link.getDownloadLink();
                                                                  if (dlLink != null && !StringUtils.startsWithCaseInsensitive(dlLink.getPluginPatternMatcher(), "directhttp://")) {
                                                                      dlLink.setPluginPatternMatcher("directhttp://" + dlLink.getPluginPatternMatcher());
                                                                  }
                                                              }
                                                          };

    public MtLnk(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        String metalink = br.getPage(parameter.getCryptedUrl());
        return decryptString(metalink);
    }

    @Override
    public CrawledLink convert(DownloadLink link) {
        final CrawledLink ret = new CrawledLink(link);
        ret.setUnknownHandler(handler);
        return ret;
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
        final DefaultHandler handler = new MetalinkSAXHandler();
        // Use the default (non-validating) parser
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            // www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setValidating(false);
            // Parse the input
            final SAXParser saxParser = factory.newSAXParser();
            final StringReader input = new StringReader(metalink);
            saxParser.parse(new InputSource(input), handler);
        } catch (Throwable t) {
            logger.log(t);
        }

        if (packageName != null) {
            final FilePackage pgk = FilePackage.getInstance();
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
        private CharArrayWriter text   = new CharArrayWriter();
        Attributes              atr;
        String                  path   = "";
        private DownloadLink    dLink  = null;

        private String          md5    = null;
        private String          sha1   = null;
        private String          sha256 = null;

        public MetalinkSAXHandler() {
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws IOException, SAXException {
            // avoid parser to read external entities
            return new InputSource(new StringReader(""));
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equals("file")) {
                /* new file begins */
                md5 = null;
                sha1 = null;
                sha256 = null;
                dLink = new DownloadLink(null, null, null, null, true);
                final String fileName = attributes.getValue("name");
                if (StringUtils.isNotEmpty(fileName)) {
                    dLink.setFinalFileName(fileName);
                    dLink.setForcedFileName(fileName);
                }
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
            } else if (path.equalsIgnoreCase(".metalink.files.file.size") || path.equalsIgnoreCase(".metalink.file.size")) {
                /* V3 and V4 */
                dLink.setVerifiedFileSize(Long.parseLong(text.toString().trim()));
            } else if (path.equalsIgnoreCase(".metalink.files.file.verification.hash") || path.equalsIgnoreCase(".metalink.file.hash")) {
                final String hashType = atr.getValue("type");
                if ("md5".equalsIgnoreCase(hashType)) {
                    md5 = text.toString().trim();
                } else if ("sha1".equalsIgnoreCase(hashType)) {
                    sha1 = text.toString().trim();
                } else if ("sha-256".equalsIgnoreCase(hashType)) {
                    sha256 = text.toString().trim();
                }
            } else if (path.equalsIgnoreCase(".metalink.files.file.resources.url") || path.equalsIgnoreCase(".metalink.file.url")) {
                final DownloadLink downloadLink = createDownloadlink(text.toString().trim());
                downloadLink.setForcedFileName(dLink.getForcedFileName());
                downloadLink.setFinalFileName(dLink.getFinalFileName());
                downloadLink.setVerifiedFileSize(dLink.getVerifiedFileSize());
                if (sha256 != null) {
                    downloadLink.setSha256Hash(sha256);
                } else if (sha1 != null) {
                    downloadLink.setSha1Hash(sha1);
                } else if (md5 != null) {
                    downloadLink.setMD5Hash(md5);
                }
                decryptedLinks.add(downloadLink);
            }
            path = path.substring(0, path.length() - qName.length() - 1);
            text.reset();
        }

        public void characters(char[] ch, int start, int length) {
            text.write(ch, start, length);
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}