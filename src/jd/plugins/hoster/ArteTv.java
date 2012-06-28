//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

package jd.plugins.hoster;

import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "arte.tv", "liveweb.arte.tv", "videos.arte.tv" }, urls = { "http://(www\\.)?arte\\.tv/[a-z]{2}/videos/.+", "http://liveweb\\.arte\\.tv/[a-z]{2}/videos?/.+", "http://videos\\.arte\\.tv/[a-z]{2}/videos/.+" }, flags = { PluginWrapper.DEBUG_ONLY, PluginWrapper.DEBUG_ONLY, PluginWrapper.DEBUG_ONLY })
public class ArteTv extends PluginForHost {

    private String   CLIPURL     = null;
    private String   EXPIRED     = null;
    private String   FLASHPLAYER = null;
    private String   clipData;

    private Document doc;

    public ArteTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    private boolean checkDateExpiration(String s) {
        if (s == null) { return false; }
        EXPIRED = s;
        SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
        try {
            Date date = df.parse(s);
            if (date.getTime() < System.currentTimeMillis()) { return true; }
            SimpleDateFormat dfto = new SimpleDateFormat("dd. MMM yyyy 'ab' HH:mm 'Uhr'");
            EXPIRED = dfto.format(date);
        } catch (Throwable e) {
            return false;
        }
        return false;
    }

    private void download(DownloadLink downloadLink) throws Exception {
        if (CLIPURL.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, CLIPURL);
            setupRTMPConnection(dl);
            ((RTMPDownload) dl).startDownload();
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public String getAGBLink() {
        return "http://www.arte.tv/de/Allgemeine-Nutzungsbedingungen/3664116.html";
    }

    private String getClipData(String tag) {
        return new Regex(clipData, "<" + tag + ">(.*?)</" + tag + ">").getMatch(0);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        download(downloadLink);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        String link = downloadLink.getDownloadURL();
        String lang = new Regex(link, "http://\\w+.arte.tv/(\\w+)/.+").getMatch(0);
        lang = lang != null && "de".equalsIgnoreCase(lang) ? "De" : lang;
        lang = lang != null && "fr".equalsIgnoreCase(lang) ? "Fr" : lang;

        String expiredBefore = null, expiredAfter = null, status = null, fileName = null;
        clipData = br.getPage(link);

        if (!"Error 404".equalsIgnoreCase(getClipData("title")) || lang == null) {
            HashMap<String, String> paras;

            if (link.startsWith("http://liveweb.arte.tv")) {
                paras = requestLivewebArte();
                expiredBefore = paras.get("dateEvent");
                expiredAfter = paras.get("dateExpiration");
                fileName = paras.get("name" + lang);
                CLIPURL = paras.get("urlHd");
                CLIPURL = CLIPURL == null ? paras.get("urlSd") : CLIPURL;
            } else {
                paras = requestVideosArte();
                expiredBefore = paras.get("dateVideo");
                expiredAfter = paras.get("dateExpiration");
                fileName = paras.get("name");
                CLIPURL = paras.get("hd");
                CLIPURL = CLIPURL == null ? paras.get("sd") : CLIPURL;
            }
        }

        if (expiredBefore != null && !checkDateExpiration(expiredBefore)) {
            if ("de".equalsIgnoreCase(lang)) {
                status = "Dieses Video steht erst ab dem " + EXPIRED + " zur Verfügung!";
            } else {
                status = "Cette vidéo est disponible uniquement à partir de " + EXPIRED + "!";
            }
        }
        if (checkDateExpiration(expiredAfter)) {
            if ("de".equalsIgnoreCase(lang)) {
                status = "Dieses Video ist seit dem " + EXPIRED + " nicht mehr verfügbar!";
            } else {
                status = "Cette vidéo n'est plus disponible depuis " + EXPIRED + "!";
            }
        }
        if (fileName.matches("(Video nicht verfügbar\\.|vidéo indisponible\\.)")) {
            if ("de".equalsIgnoreCase(lang)) {
                status = "Dieses Video ist zur Zeit nicht verfügbar!";
            } else {
                status = "Cette vidéo n'est plus actuellement pas disponible!";
            }
        }
        if (status != null) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, status); }

        if (fileName == null || CLIPURL == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }

        String ext = CLIPURL.substring(CLIPURL.lastIndexOf("."), CLIPURL.length());
        if (ext.length() > 4) {
            ext = new Regex(ext, Pattern.compile("\\w/(mp4):", Pattern.CASE_INSENSITIVE)).getMatch(0);
        }
        ext = ext == null ? ".flv" : "." + ext;
        if (fileName.endsWith(".")) {
            fileName = fileName.substring(0, fileName.length() - 1);
        }
        downloadLink.setFinalFileName(fileName.trim() + ext);
        return AvailableStatus.TRUE;
    }

    private HashMap<String, String> requestLivewebArte() throws Exception {
        HashMap<String, String> paras = new HashMap<String, String>();
        String eventId = br.getRegex("eventId=(\\d+)").getMatch(0);
        FLASHPLAYER = br.getRegex("(http://[^\\?\"]+player.swf)").getMatch(0);

        XPath xPath = xmlParser("http://arte.vo.llnwd.net/o21/liveweb/events/event-" + eventId + ".xml?" + System.currentTimeMillis());
        NodeList modules = (NodeList) xPath.evaluate("//event[@id=" + eventId + "]/*|//event/video[@id]/*", doc, XPathConstants.NODESET);

        for (int i = 0; i < modules.getLength(); i++) {
            Node node = modules.item(i);
            if ("postrolls".equalsIgnoreCase(node.getNodeName()) || "categories".equalsIgnoreCase(node.getNodeName())) {
                continue;
            }
            paras.put(node.getNodeName(), node.getTextContent());
        }
        return paras;
    }

    private HashMap<String, String> requestVideosArte() throws Exception {
        HashMap<String, String> paras = new HashMap<String, String>();
        String tmpUrl = br.getRegex("ajaxUrl:\'([^<>]+view,)").getMatch(0);
        FLASHPLAYER = br.getRegex("<param name=\"movie\" value=\"(.*?)\\?").getMatch(0);
        if (FLASHPLAYER == null || tmpUrl == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }

        XPath xPath = xmlParser("http://videos.arte.tv" + tmpUrl + "strmVideoAsPlayerXml.xml");
        NodeList modules = (NodeList) xPath.evaluate("/video/*|//urls/*", doc, XPathConstants.NODESET);

        for (int i = 0; i < modules.getLength(); i++) {
            Node node = modules.item(i);
            if (node.hasAttributes()) {
                paras.put(node.getAttributes().item(0).getTextContent(), node.getTextContent());
            } else {
                paras.put(node.getNodeName(), node.getTextContent());
            }
        }
        return paras;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    private void setupRTMPConnection(DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        /* liveweb */
        String[] uri = CLIPURL.split("(?i)/MP4:liveweb");
        if (uri != null && uri.length == 2) {
            rtmp.setPlayPath("MP4:liveweb" + uri[1]);
            rtmp.setUrl(uri[0]);
            rtmp.setApp(new Regex(uri[0], "rtmp.?://[^/]+/(.*?)$").getMatch(0));
        } else {
            rtmp.setUrl(CLIPURL);
        }
        rtmp.setSwfVfy(FLASHPLAYER);
        rtmp.setResume(true);
    }

    private XPath xmlParser(String linkurl) throws Exception {
        InputStream stream = null;
        try {
            URL url = new URL(linkurl);
            stream = url.openStream();
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            XPath xPath = XPathFactory.newInstance().newXPath();
            doc = parser.parse(stream);
            return xPath;
        } catch (Throwable e2) {
            return null;
        } finally {
            try {
                stream.close();
            } catch (Throwable e) {
            }
        }
    }

}