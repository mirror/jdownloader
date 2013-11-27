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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "3sat.de" }, urls = { "http://(www\\.)?3sat\\.de/mediathek/(index\\.php)?(\\?display=\\d+\\&|\\?)mode=play(set)?\\&obj=\\d+" }, flags = { 32 })
public class DreiSatDe extends PluginForHost {

    private static final String Q_LOW      = "Q_LOW";
    private static final String Q_HIGH     = "Q_HIGH";
    private static final String Q_VERYHIGH = "Q_VERYHIGH";
    private static final String Q_HD       = "Q_HD";
    private static final String Q_BEST     = "Q_BEST";

    public DreiSatDe(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        /* only flash videos are supported */
        link.setUrlDownload(link.getDownloadURL().replaceAll("\\?display=\\d+\\&", "?display=1&"));
        link.setUrlDownload(link.getDownloadURL().replaceAll("\\?mode=play", "?display=1&mode=play"));
    }

    @Override
    public ArrayList<DownloadLink> getDownloadLinks(String data, FilePackage fp) {
        ArrayList<DownloadLink> ret = super.getDownloadLinks(data, fp);

        try {
            if (ret != null && ret.size() > 0) {
                /*
                 * we make sure only one result is in ret, thats the case for svn/next major version
                 */
                final DownloadLink sourceLink = ret.get(0);
                String ID = new Regex(sourceLink.getDownloadURL(), "\\&obj=(\\d+)").getMatch(0);
                // Flash: 1=auto; 11=low; 13=high; 14=veryhigh; 1?=hd --> rtmp://
                // Quicktime: 5=auto; 51=low; 53=high; 54=veryhigh; 5?=hd --> rtsp://
                // WindowsMedia: 2=auto; 21=low; 23=high; 24=veryhigh; 2?=hd --> mms://
                if (ID != null) {
                    Browser br = new Browser();
                    br.getPage(sourceLink.getDownloadURL());

                    String title = br.getRegex("<div class=\"MainBoxHeadline\">([^<]+)</").getMatch(0);
                    String titleUT = br.getRegex("<span class=\"BoxHeadlineUT\">([^<]+)</").getMatch(0);
                    if (title == null) title = br.getRegex("<title>3sat\\.online \\- Mediathek: ([^<]+)</title>").getMatch(0);
                    if (title != null) title = Encoding.htmlDecode(title + (titleUT != null ? "__" + titleUT.replaceAll(":$", "") : "").trim());
                    if (title == null) title = "UnknownTitle_" + System.currentTimeMillis();

                    if (br.containsHTML("<debuginfo>Kein Beitrag mit ID") || br.containsHTML("<statuscode>wrongParameter</statuscode>")) {
                        sourceLink.setAvailable(false);
                        ret.set(0, sourceLink);
                        return ret;
                    }
                    Map<String, HashMap<String, String>> MediaEntrys = new TreeMap<String, HashMap<String, String>>();
                    HashMap<String, String> MediaEntry = null;

                    Document doc = JDUtilities.parseXmlString(br.getPage("http://www.3sat.de/mediathek/xmlservice/web/beitragsDetails?ak=web&id=" + ID).toString(), false);

                    /* xmlData --> HashMap */
                    // /response/video/formitaeten/formitaet... --> name, quality, stream url
                    NodeList nl = doc.getElementsByTagName("formitaet");

                    for (int i = 0; i < nl.getLength(); i++) {
                        Node childNode = nl.item(i);
                        String mediaType = ((Element) childNode).getAttribute("basetype");
                        if (isEmpty(mediaType)) continue;
                        if (!(mediaType.contains("http_na_na") || mediaType.contains("rtmp_zdfmeta"))) continue;
                        NodeList t = childNode.getChildNodes();
                        MediaEntry = new HashMap<String, String>();
                        for (int j = 0; j < t.getLength(); j++) {
                            Node g = t.item(j);
                            if ("#text".equals(g.getNodeName())) continue;
                            MediaEntry.put(g.getNodeName(), g.getTextContent());
                        }
                        if (isEmpty(title)) continue;
                        MediaEntry.put("basetype", mediaType);
                        if (MediaEntry.get("url").contains("metafilegenerator.de")) continue;
                        MediaEntrys.put(title + "@" + mediaType + MediaEntry.get("quality") + MediaEntry.get("videoBitrate"), MediaEntry);
                    }

                    if (br.containsHTML("(>Dieser Beitrag ist leider.*?nicht \\(mehr\\) verf&uuml;gbar|>Das Video ist in diesem Format.*?aktuell leider nicht verf&uuml;gbar)")) {
                        sourceLink.setAvailable(false);
                        ret.set(0, sourceLink);
                        return ret;
                    }

                    /*
                     * little pause needed so the next call does not return trash
                     */
                    Thread.sleep(1000);

                    ArrayList<DownloadLink> newRet = new ArrayList<DownloadLink>();
                    HashMap<String, DownloadLink> bestMap = new HashMap<String, DownloadLink>();
                    for (Entry<String, HashMap<String, String>> next : MediaEntrys.entrySet()) {
                        MediaEntry = new HashMap<String, String>(next.getValue());

                        String name = next.getKey();
                        String protocol = MediaEntry.get("basetype").split("_")[3];
                        String url = MediaEntry.get("url");
                        String fmt = MediaEntry.get("quality");

                        if (fmt != null) fmt = fmt.toLowerCase(Locale.ENGLISH).trim();
                        if (fmt != null) {
                            /* best selection is done at the end */
                            if ("low".equals(fmt)) {
                                if (this.getPluginConfig().getBooleanProperty(Q_LOW, true) == false) {
                                    continue;
                                } else {
                                    fmt = "low";
                                }
                            } else if ("high".equals(fmt)) {
                                if (this.getPluginConfig().getBooleanProperty(Q_HIGH, true) == false) {
                                    continue;
                                } else {
                                    fmt = "high";
                                }
                            } else if ("veryhigh".equals(fmt)) {
                                if (this.getPluginConfig().getBooleanProperty(Q_VERYHIGH, true) == false) {
                                    continue;
                                } else {
                                    fmt = "veryhigh";
                                }
                            } else if ("hd".equals(fmt)) {
                                if (this.getPluginConfig().getBooleanProperty(Q_HD, true) == false) {
                                    continue;
                                } else {
                                    fmt = "hd";
                                }
                            }
                        }

                        String ext = MediaEntry.get("basetype").split("_")[2];
                        name = name.split("@")[0] + "__" + MediaEntry.get("quality") + "_" + protocol + "@" + MediaEntry.get("videoBitrate") + "bps." + ext;
                        final DownloadLink link = new DownloadLink(this, name, getHost(), sourceLink.getDownloadURL(), true);
                        link.setAvailable(true);
                        link.setFinalFileName(name);
                        link.setBrowserUrl(sourceLink.getBrowserUrl());
                        link.setProperty("directURL", url);
                        link.setProperty("directName", name);
                        link.setProperty("directQuality", fmt);
                        link.setProperty("streamingType", protocol);
                        link.setProperty("LINKDUPEID", "3sat" + JDHash.getMD5(ID + name + fmt + MediaEntry.get("videoBitrate") + System.nanoTime()));

                        try {
                            link.setDownloadSize(Long.parseLong(MediaEntry.get("filesize")));
                        } catch (Throwable e) {
                        }

                        DownloadLink best = bestMap.get(fmt);
                        if (best == null || link.getDownloadSize() > best.getDownloadSize()) {
                            bestMap.put(fmt, link);
                        }
                        newRet.add(link);
                    }
                    if (newRet.size() > 0) {
                        if (this.getPluginConfig().getBooleanProperty(Q_BEST, false)) {
                            /* only keep best quality */
                            DownloadLink keep = bestMap.get("hd");
                            if (keep == null) keep = bestMap.get("veryhigh");
                            if (keep == null) keep = bestMap.get("high");
                            if (keep == null) keep = bestMap.get("low");
                            if (keep != null) {
                                newRet.clear();
                                newRet.add(keep);
                            }
                        }
                        /*
                         * only replace original found links by new ones, when we have some
                         */
                        if (fp != null) {
                            fp.addLinks(newRet);
                            fp.remove(sourceLink);
                        } else if (newRet.size() > 1) {
                            fp = FilePackage.getInstance();
                            fp.setName(title);
                            fp.addLinks(newRet);
                        }
                        ret = newRet;
                    }
                } else {
                    /*
                     * no other qualities*&
                     */
                }
            }
        } catch (final Throwable e) {
            logger.severe(e.getMessage());
        }
        return ret;
    }

    @Override
    public String getAGBLink() {
        return "http://www.3sat.de/page/?source=/service/140013/index.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        download(downloadLink);
    }

    private void setupRTMPConnection(String dllink, DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        // rtmp.setPlayPath(stream[2]);
        rtmp.setUrl(dllink);
        // rtmp.setApp(stream[1]);
        rtmp.setResume(true);
        // rtmp.setSwfVfy("http://www.3sat.de/mediaplayer/5/EmbeddedPlayer.swf");
        rtmp.setRealTime();
        rtmp.setTimeOut(5);
    }

    private void download(final DownloadLink downloadLink) throws Exception {
        String dllink = downloadLink.getStringProperty("directURL", null);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if ("rtmp".equals(downloadLink.getStringProperty("streamingType", null))) {
            downloadLink.setProperty("FLVFIXER", true);
            if (!(dllink.startsWith("http") && dllink.endsWith(".meta"))) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getPage(dllink);
            dllink = br.getRegex("<default\\-stream\\-url>(.*?)</default\\-stream\\-url>").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            dl = new RTMPDownload(this, downloadLink, dllink);
            setupRTMPConnection(dllink, dl);
            if (!((RTMPDownload) dl).startDownload()) {
                if (downloadLink.getLinkStatus().getStatus() != 513) {
                    // downloadLink.setProperty("directURL", null);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }

        } else {
            br.setFollowRedirects(true);
            if (dllink.startsWith("mms")) throw new PluginException(LinkStatus.ERROR_FATAL, "Protocol (mms://) not supported!");
            if (dllink.startsWith("rtsp")) throw new PluginException(LinkStatus.ERROR_FATAL, "Protocol (rtsp://) not supported!");
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (dl.getConnection().getResponseCode() == 403) throw new PluginException(LinkStatus.ERROR_FATAL, "This Content is not longer available!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        if (downloadLink.getStringProperty("directURL", null) == null) {
            /* fetch fresh directURL */
            setBrowserExclusive();
            br.setFollowRedirects(true);
            br.getPage(downloadLink.getDownloadURL());
            String mediaUrl = br.getRegex("Flashvars\\.mediaURL\\s+?=\\s+?\"([^\"]+)\"").getMatch(0);

            if (br.containsHTML("(>Dieser Beitrag ist leider.*?nicht \\(mehr\\) verf&uuml;gbar|>Das Video ist in diesem Format.*?aktuell leider nicht verf&uuml;gbar)") || mediaUrl == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            br.getPage(mediaUrl);
            String app = br.getRegex("<param name=\"app\" value=\"([^\"]+)\"").getMatch(0);
            String host = br.getRegex("<param name=\"host\" value=\"([^\"]+)\"").getMatch(0);

            if (app == null && host == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (!host.startsWith("rtmp://")) host = "rtmp://" + host;
            String q = downloadLink.getStringProperty("directQuality", "high");
            String newUrl[] = br.getRegex("<video dur=\"([\\d:]+)\" paramGroup=\"gl\\-vod\\-rtmp\" src=\"([^\"]+)\" system\\-bitrate=\"(\\d+)\">[^<]+<param name=\"quality\" value=\"" + q + "\"").getRow(0);
            if (newUrl == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setProperty("directURL", host + "@" + app + "@" + newUrl[1]);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    private boolean isEmpty(String ip) {
        return ip == null || ip.trim().length() == 0;
    }

    private void setConfigElements() {
        final ConfigEntry bestonly = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_BEST, JDL.L("plugins.hoster.dreisat.best", "Load Best Version ONLY")).setDefaultValue(false);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(bestonly);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_LOW, JDL.L("plugins.hoster.dreisat.loadlow", "Load Low Version")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_HIGH, JDL.L("plugins.hoster.dreisat.loadhigh", "Load High Version")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_VERYHIGH, JDL.L("plugins.hoster.dreisat.loadveryhigh", "Load VeryHigh Version")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_HD, JDL.L("plugins.hoster.dreisat.loadhd", "Load HD Version")).setDefaultValue(false).setEnabled(false));
    }

}