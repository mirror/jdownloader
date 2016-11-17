//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.GenericF4M;

import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@DecrypterPlugin(revision = "$Revision: 26321 $", interfaceVersion = 3, names = { "f4m" }, urls = { "https?://.+\\.f4m[^\\s<>\"']*" })
public class GenericF4MDecrypter extends PluginForDecrypt {

    @Override
    public Boolean siteTesterDisabled() {
        return Boolean.TRUE;
    }

    public GenericF4MDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        CrawledLink source = getCurrentLink();
        String referer = null;
        String cookiesString = null;
        while (source != null) {
            if (source.getDownloadLink() != null && StringUtils.equals(source.getURL(), param.getCryptedUrl())) {
                final DownloadLink downloadLink = source.getDownloadLink();
                cookiesString = downloadLink.getStringProperty("cookies", null);
                if (cookiesString != null) {
                    final String host = Browser.getHost(source.getURL());
                    br.setCookies(host, Cookies.parseCookies(cookiesString, host, null));
                }
            }
            if (!StringUtils.equals(source.getURL(), param.getCryptedUrl())) {
                if (source.getCryptedLink() != null) {
                    referer = source.getURL();
                    br.getPage(source.getURL());
                }
                break;
            } else {
                source = source.getSourceLink();
            }
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final URLConnectionAdapter con = br.openGetConnection(param.getCryptedUrl());
        if (!con.isOK() || con.getResponseCode() == 403 || con.getResponseCode() == 404) {
            con.disconnect();
            return ret;
        }
        final Document d;
        try {
            final DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            d = parser.parse(con.getInputStream());
        } finally {
            con.disconnect();
        }
        final String urlName = getFileNameFromURL(br._getURL());
        final String linkURL = "f4m" + param.getCryptedUrl().substring(4);
        final XPath xPath = XPathFactory.newInstance().newXPath();
        final NodeList mediaUrls = (NodeList) xPath.evaluate("/manifest/media", d, XPathConstants.NODESET);
        for (int j = 0; j < mediaUrls.getLength(); j++) {
            final Node media = mediaUrls.item(j);
            final String fragmentUrl = GenericF4M.getAttByNamedItem(media, "url");
            if (fragmentUrl != null) {
                final DownloadLink link = createDownloadlink(linkURL);
                String fileName = null;
                if (urlName == null) {
                    fileName = "Unknown";
                } else {
                    fileName = urlName.replaceAll("\\.f4m", "");
                }
                final String width = GenericF4M.getAttByNamedItem(media, "width");
                final String height = GenericF4M.getAttByNamedItem(media, "height");
                if (width != null && height != null) {
                    fileName += "_" + width + "x" + height;
                }
                final String bitrate = GenericF4M.getAttByNamedItem(media, "bitrate");
                if (bitrate != null) {
                    fileName += "_br" + bitrate;
                }
                link.setFinalFileName(fileName + ".mp4");
                link.setAvailable(true);
                final String streamId = GenericF4M.getAttByNamedItem(media, "streamId");
                // these propertes are used to find the correct url again
                link.setProperty("fragmentUrl", fragmentUrl);
                link.setProperty("streamId", streamId);
                link.setProperty("width", width);
                link.setProperty("height", height);
                link.setProperty("bitrate", bitrate);
                link.setLinkID("f4m://" + br.getHost() + "/" + Hash.getMD5(fileName));
                ret.add(link);
            }
        }
        if (ret.size() > 1) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(urlName);
            fp.addLinks(ret);
        }
        return ret;
    }
}