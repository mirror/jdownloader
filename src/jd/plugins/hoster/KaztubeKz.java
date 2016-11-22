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

package jd.plugins.hoster;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.jdownloader.downloader.hds.HDSDownloader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "kaztube.kz" }, urls = { "https?://(?:www\\.)?kaztube\\.kz/video/\\d+" })
public class KaztubeKz extends PluginForHost {

    public KaztubeKz(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:

    /* Extension which will be used if no correct extension is found */
    private static final String default_Extension = ".mp4";
    /* Connection stuff */
    private static final int    free_maxdownloads = -1;

    private String              dllink            = null;
    private boolean             server_issues     = false;

    @Override
    public String getAGBLink() {
        return "https://kaztube.kz/assets/media/soglashenie/cogl_ru.html";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String fid = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
        String filename = PluginJSonUtils.getJsonValue(this.br, "name");
        if (filename == null) {
            filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]+)\"").getMatch(0);
        }
        if (filename == null) {
            filename = fid;
        }
        dllink = br.getRegex("(https?://[^<>\"]+/manifest\\.f4m)").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        String ext;
        if (dllink != null) {
            ext = getFileNameExtensionFromString(dllink, default_Extension);
            if (ext.equalsIgnoreCase(".f4m")) {
                ext = default_Extension;
            }
        } else {
            ext = default_Extension;
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.br.getPage(dllink);
        String finallink = null;
        final DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final XPath xPath = XPathFactory.newInstance().newXPath();
        Document d = parser.parse(new ByteArrayInputStream(br.toString().getBytes("UTF-8")));
        NodeList mediaUrls = (NodeList) xPath.evaluate("/manifest/media", d, XPathConstants.NODESET);
        Node media;
        for (int j = 0; j < mediaUrls.getLength(); j++) {
            media = mediaUrls.item(j);

            String temp = getAttByNamedItem(media, "url");
            if (temp != null) {
                finallink = temp;
                break;
            }
        }
        if (!finallink.startsWith("http")) {
            finallink = dllink.replace("/manifest.f4m", "/") + finallink;
        }
        dl = new HDSDownloader(downloadLink, br, finallink);
        dl.startDownload();
    }

    /**
     * lets try and prevent possible NPE from killing the progress.
     *
     * @author raztoki
     * @param n
     * @param item
     * @return
     */
    private String getAttByNamedItem(final Node n, final String item) {
        final String t = n.getAttributes().getNamedItem(item).getTextContent();
        return (t != null ? t.trim() : null);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        if (link != null) {
            link.removeProperty(HDSDownloader.RESUME_FRAGMENT);
        }
    }
}
