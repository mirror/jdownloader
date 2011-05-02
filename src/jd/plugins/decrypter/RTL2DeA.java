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

package jd.plugins.decrypter;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rtl2.de" }, urls = { "http://(www\\.)?rtl2\\.de/rvp/\\w+/(\\?v=[0-9a-f]+)?" }, flags = { PluginWrapper.DEBUG_ONLY })
public class RTL2DeA extends PluginForDecrypt {

    private Document doc;

    public RTL2DeA(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        setBrowserExclusive();
        br.getPage(parameter);
        final String vipyId = br.getRegex("vipy_id\\s+:\\s+(\\d+),").getMatch(0);
        if (vipyId == null) { return null; }
        final HashMap<String, String> parameterMap = new HashMap<String, String>();

        XPath xPath = xmlParser("http://www.rtl2.de/vipo/php/xml_player.php?vipy_id=" + vipyId);
        final NodeList vicoId = (NodeList) xPath.evaluate("/playlists/playlist/vico_id", doc, XPathConstants.NODESET);
        final NodeList label = (NodeList) xPath.evaluate("/playlists/playlist/label", doc, XPathConstants.NODESET);

        if (vicoId.getLength() == 0 || label.getLength() == 0) { return null; }

        for (int i = 0; i < vicoId.getLength(); i++) {
            parameterMap.put(vicoId.item(i).getTextContent(), label.item(i).getTextContent());
        }

        if (parameterMap != null) {
            progress.setRange(parameterMap.size());
            for (final Map.Entry<String, String> entry : parameterMap.entrySet()) {
                progress.increase(1);
                xPath = xmlParser("http://www.rtl2.de/vipo/php/xml_collection.php?vico_id=" + entry.getKey());
                final NodeList title = (NodeList) xPath.evaluate("/metadaten/video/titel", doc, XPathConstants.NODESET);
                final NodeList name = (NodeList) xPath.evaluate("/metadaten/video//name", doc, XPathConstants.NODESET);
                if (title.getLength() == 0 || name.getLength() == 0) {
                    progress.increase(1);
                    continue;
                }
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(xPath.evaluate("/metadaten/ad_level", doc) + "_" + entry.getValue());
                for (int i = 0; i < title.getLength(); i++) {
                    final DownloadLink dlLink = createDownloadlink(name.item(i).getTextContent());
                    dlLink.setName(title.item(i).getTextContent().replace("\"", "") + ".flv");
                    dlLink.setFilePackage(fp);
                    decryptedLinks.add(dlLink);
                }
            }
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) { return null; }
        return decryptedLinks;

    }

    private XPath xmlParser(final String linkurl) throws Exception {
        final URL url = new URL(linkurl);
        final InputStream stream = url.openStream();
        final DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final XPath xPath = XPathFactory.newInstance().newXPath();
        doc = parser.parse(stream);
        return xPath;
    }
}
