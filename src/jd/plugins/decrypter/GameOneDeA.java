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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.w3c.dom.Document;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gameone.de" }, urls = { "http://(www\\.)?gameone\\.de/tv/\\d+(\\?part=\\d+)?" }, flags = { PluginWrapper.DEBUG_ONLY })
public class GameOneDeA extends PluginForDecrypt {

    private Document doc;

    public GameOneDeA(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        setBrowserExclusive();
        br.setReadTimeout(60 * 1000);
        br.getPage(parameter);

        // http
        String dllink = br.getRegex("name=\"href\" value=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(http://cdn\\.riptide-mtvn\\.com/production/.*?)\"").getMatch(0);
        }
        String filename = br.getRegex("<title>(.*?) \\|").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<div id=\\'show-video\\'>[\t\n\r ]+<h2>(.*?)</h2>").getMatch(0);
        }
        if (filename != null && dllink != null) {
            filename += dllink.substring(dllink.length() - 4, dllink.length());
            final DownloadLink dlLink = createDownloadlink("directhttp://" + dllink);
            dlLink.setFinalFileName(filename);
            decryptedLinks.add(dlLink);
            return decryptedLinks;
        }

        // rtmp
        final String[] parts = br.getRegex("href='[/a-z0-9=?]+'\\srel='(.*?)'").getColumn(0);
        String fpName = br.getRegex("<title>(.*?) \\|").getMatch(0);
        if (parts == null || parts.length == 0 || fpName == null) { return null; }

        fpName = fpName.replaceAll(" - Teil \\d+", "");
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        final String partInfoUrl = "http://intl.esperanto.mtvi.com/www/xml/video.jhtml?uri=";
        final String dllinkInfoUrl = "http://intl.esperanto.mtvi.com/www/xml/media/mediaGen.jhtml?uri=";
        progress.setRange(parts.length);

        for (final String part : parts) {
            XPath xPath = xmlParser(partInfoUrl + Encoding.urlEncode(part) + "&version=as3&keyValues=brand=mtv;");
            filename = xPath.evaluate("/rss/channel/item/title", doc);
            xPath = xmlParser(dllinkInfoUrl + Encoding.urlEncode(part));
            dllink = xPath.evaluate("/package/video/item/rendition/src", doc);
            if (filename == null || dllink == null) {
                continue;
            }
            final DownloadLink dlLink = createDownloadlink(dllink);
            dlLink.setName(filename + ".flv");
            dlLink.setFilePackage(fp);
            decryptedLinks.add(dlLink);
            progress.increase(1);
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
