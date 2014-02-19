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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

// http://tvthek,orf.at/live/... --> HDS
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "orfmediathek.at" }, urls = { "http://(www\\.)?tvthek\\.orf\\.at/(programs?|topic)/.+" }, flags = { 0 })
public class ORFMediathekDecrypter extends PluginForDecrypt {

    private static final String Q_SUBTITLES   = "Q_SUBTITLES";
    private static final String Q_BEST        = "Q_BEST";
    private static final String Q_LOW         = "Q_LOW";
    private static final String Q_MEDIUM      = "Q_MEDIUM";
    private static final String Q_HIGH        = "Q_HIGH";
    private static final String HTTP_STREAM   = "HTTP_STREAM";
    private boolean             BEST          = false;

    private static final String TYPE_TOPIC    = "http://(www\\.)?tvthek\\.orf\\.at/topic/.+";
    private static final String TYPE_PROGRAMM = "http://(www\\.)?tvthek\\.orf\\.at/programs?/.+";

    public ORFMediathekDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (isStableEnviroment()) return decryptedLinks;
        String parameter = param.toString();

        br.getPage(parameter);
        int status = br.getHttpConnection().getResponseCode();
        if (status == 301 || status == 302) {
            br.setFollowRedirects(true);
            if (br.getRedirectLocation() != null) {
                parameter = br.getRedirectLocation();
                br.getPage(parameter);
            }
        } else if (status != 200) {
            // Check...if offline, add to llinkgrabber so user can see it
            final DownloadLink link = createDownloadlink(parameter.replace("http://", "decrypted://") + "&quality=default&hash=default");
            link.setAvailable(false);
            link.setProperty("offline", true);
            decryptedLinks.add(link);
            return decryptedLinks;
        }
        // Check...if offline, add to llinkgrabber so user can see it
        if (br.containsHTML("(404 \\- Seite nicht gefunden\\.|area_headline error_message\">Keine Sendung vorhanden<)")) {
            final DownloadLink link = createDownloadlink(parameter.replace("http://", "decrypted://") + "&quality=default&hash=default");
            link.setAvailable(false);
            link.setProperty("offline", true);
            link.setName(new Regex(parameter, "tvthek\\.orf\\.at/programs/(.+)").getMatch(0));
            decryptedLinks.add(link);
            return decryptedLinks;
        }

        final SubConfiguration cfg = SubConfiguration.getConfig("orf.at");
        BEST = cfg.getBooleanProperty(Q_BEST, false);
        decryptedLinks.addAll(getDownloadLinks(parameter, cfg));

        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            if (parameter.matches(TYPE_TOPIC)) {
                logger.warning("MAYBE Decrypter out of date for link: " + parameter);
            } else {
                logger.warning("Decrypter out of date for link: " + parameter);
            }
            return null;
        }
        return decryptedLinks;
    }

    private boolean isStableEnviroment() {
        String prev = JDUtilities.getRevision();
        if (prev == null || prev.length() < 3) {
            prev = "0";
        } else {
            prev = prev.replaceAll(",|\\.", "");
        }
        final int rev = Integer.parseInt(prev);
        if (rev < 10000) { return true; }
        return false;
    }

    private ArrayList<DownloadLink> getDownloadLinks(final String data, final SubConfiguration cfg) {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();

        try {
            String xmlData = br.getRegex("ORF\\.flashXML\\s*=\\s*\'([^\']+)\';").getMatch(0);
            String jsonData = br.getRegex("\"segments\":\\[(\\{.*?\\})\\],\"is_forward_container\"").getMatch(0);

            if (xmlData != null || jsonData != null) {
                Map<String, HashMap<String, String>> MediaEntrys = new TreeMap<String, HashMap<String, String>>();
                HashMap<String, String> MediaEntry = null;
                String quality = null, key = null, title = null;

                if (xmlData != null) {
                    Document doc = JDUtilities.parseXmlString(Encoding.htmlDecode(xmlData), false);

                    /* xmlData --> HashMap */
                    // /PlayerConfig/PlayList/Items/Item... --> name, quality, rtmp stream url
                    NodeList nl = doc.getElementsByTagName("Item");

                    for (int i = 0; i < nl.getLength(); i++) {
                        Node childNode = nl.item(i);
                        NodeList t = childNode.getChildNodes();
                        MediaEntry = new HashMap<String, String>();
                        for (int j = 0; j < t.getLength(); j++) {
                            Node g = t.item(j);
                            if ("#text".equals(g.getNodeName())) continue;
                            quality = ((Element) g).getAttribute("quality");
                            key = g.getNodeName();
                            if (isEmpty(quality) && "VideoUrl".equalsIgnoreCase(key)) continue;
                            if ("VideoUrl".equalsIgnoreCase(key)) key = quality;
                            MediaEntry.put(key, g.getTextContent());
                        }
                        title = MediaEntry.get("Title");
                        if (isEmpty(title)) continue;
                        MediaEntrys.put(title, MediaEntry);
                    }
                } else {
                    /* jsonData --> HashMap */
                    HashMap<String, String> tmpMediaEntry = new HashMap<String, String>();
                    // if (!jsonData.endsWith("}]")) jsonData += "}]";
                    jsonData = decodeUnicode(jsonData);

                    String selector = "";
                    for (String segment : new Regex(jsonData, "\\{\"clickcounter_corrected\"(.*?)\"is_episode_one_segment_episode\":").getColumn(0)) {
                        segment = segment.replace("\\", "");
                        title = new Regex(segment, "\"title\":\"(.*?)\",").getMatch(0);
                        if (title == null) {
                            if (title == null) title = new Regex(segment, "\"title\":\"([^\"]+)\"").getMatch(0);
                            if (title == null) title = new Regex(segment, "\"description\":\"\\\\\"([^\\\\\"]+)\\\\\"").getMatch(0);
                            if (title == null) title = new Regex(segment, "\"description\":\"([^\"]+)\"").getMatch(0);
                            if (title != null && title.length() > 80) title = title.substring(0, 80);
                        }

                        String streams = new Regex(segment, "\"sources\":\\[(.*?)\\]").getMatch(0);
                        if (isEmpty(streams)) continue;
                        for (String stream : new Regex(streams, "\\{(.*?)\\}").getColumn(0)) {
                            MediaEntry = new HashMap<String, String>();
                            for (String[] sss : new Regex(stream, "\"([^\"]+)\":\"([^\"]+)\"").getMatches()) {
                                MediaEntry.put(sss[0], sss[1]);
                            }
                            if (isEmpty(title)) continue;
                            title = title.trim();
                            MediaEntry.put("title", title);
                            /* Backward compatibility with xml method */
                            String url = MediaEntry.get("src");
                            String q = MediaEntry.get("quality");
                            String p = MediaEntry.get("protocol");
                            String d = MediaEntry.get("delivery");
                            if (isEmpty(url) && isEmpty(q) && isEmpty(p) && isEmpty(d)) continue;
                            String subtitle = new Regex(segment, "\"subtitles\":\\[\\{\"src\":\"(http[^\"]+)\"").getMatch(0);
                            if (subtitle != null) MediaEntry.put("SubTitleUrl", subtitle.replace("\\", ""));
                            MediaEntry.remove("src");
                            MediaEntry.put(q, url);
                            if (isEmpty(selector)) selector = p + d;
                            if (!(p + d).equals(selector)) {
                                MediaEntrys.put(title + "@" + selector, tmpMediaEntry);
                                selector = p + d;
                                tmpMediaEntry = new HashMap<String, String>();
                                tmpMediaEntry.putAll(MediaEntry);
                            } else {
                                tmpMediaEntry.putAll(MediaEntry);
                            }
                        }
                        MediaEntrys.put(title + "@" + selector, tmpMediaEntry);
                    }
                }
                String fpName = getTitle(br);
                String extension = ".mp4";
                if (br.getRegex("new MediaCollection\\(\"audio\",").matches()) extension = ".mp3";

                ArrayList<DownloadLink> newRet = new ArrayList<DownloadLink>();
                ArrayList<DownloadLink> part = new ArrayList<DownloadLink>();
                HashMap<String, DownloadLink> bestMap = new HashMap<String, DownloadLink>();
                String vIdTemp = "";

                for (Entry<String, HashMap<String, String>> next : MediaEntrys.entrySet()) {
                    MediaEntry = new HashMap<String, String>(next.getValue());
                    String fileName = next.getKey();
                    fileName = fileName.replaceAll("\"", "");
                    fileName = fileName.replaceAll(":\\s|\\s\\|\\s", " - ").trim();
                    String protocol = MediaEntry.get("protocol");
                    String delivery = MediaEntry.get("delivery");
                    if (protocol != null && delivery != null) {
                        if (fileName.contains(protocol) && fileName.contains(delivery)) fileName = MediaEntry.get("title");
                    }

                    // available protocols: http, rtmp, rtsp, hds, hls
                    if (!("http".equals(protocol) && "progressive".equals(delivery))) {
                        if (!("rtmp".equals(protocol) && "streaming".equals(delivery))) continue;
                    }
                    if (cfg.getBooleanProperty(HTTP_STREAM, false) && "rtmp".equals(protocol)) continue;

                    boolean sub = true;
                    if (fileName.equals(vIdTemp)) sub = false;

                    for (Entry<String, String> stream : MediaEntry.entrySet()) {
                        String url = stream.getValue();
                        String fmt = stream.getKey();
                        if (!isEmpty(fmt)) {
                            fmt = humanReadableQualityIdentifier(fmt.toUpperCase(Locale.ENGLISH).trim());
                            /* best selection is done at the end */
                            if ("LOW".equals(fmt)) {
                                if ((cfg.getBooleanProperty(Q_LOW, true) || BEST) == false) {
                                    continue;
                                } else {
                                    if ("http".equals(protocol)) continue; // 404
                                    fmt = "LOW";
                                }
                            } else if ("MEDIUM".equals(fmt)) {
                                if ((cfg.getBooleanProperty(Q_MEDIUM, true) || BEST) == false) {
                                    continue;
                                } else {
                                    fmt = "MEDIUM";
                                }
                            } else if ("HIGH".equals(fmt)) {
                                if ((cfg.getBooleanProperty(Q_HIGH, true) || BEST) == false) {
                                    continue;
                                } else {
                                    fmt = "HIGH";
                                }
                            } else {
                                if (unknownQualityIdentifier(fmt)) {
                                    logger.info("ORFMediathek Decrypter: unknown quality identifier --> " + fmt);
                                    logger.info("Link: " + data);
                                }
                                continue;
                            }
                        }
                        String ext = url.substring(url.lastIndexOf("."));
                        if (ext.length() == 4) extension = ext;
                        final String name = fileName + "@" + fmt + (protocol != null ? "_" + protocol : "") + extension;
                        final DownloadLink link = createDownloadlink(data.replace("http://", "decrypted://") + "&quality=" + fmt + "&hash=" + JDHash.getMD5(name));

                        link.setFinalFileName(name);
                        link.setBrowserUrl(data);
                        link.setProperty("directURL", url);
                        link.setProperty("directName", name);
                        link.setProperty("directQuality", fmt);
                        if (protocol == null && delivery == null) {
                            link.setAvailable(true);
                            link.setProperty("streamingType", "rtmp");
                        } else {
                            link.setProperty("streamingType", protocol);
                            link.setProperty("delivery", delivery);
                            if (!"http".equals(protocol)) link.setAvailable(true);
                        }

                        DownloadLink best = bestMap.get(fmt);
                        if (best == null || link.getDownloadSize() > best.getDownloadSize()) {
                            bestMap.put(fmt, link);
                        }
                        part.add(link);
                    }
                    if (part.size() > 0) {
                        if (BEST) {
                            /* only keep best quality */
                            DownloadLink keep = bestMap.get("HIGH");
                            if (keep == null) {
                                keep = bestMap.get("MEDIUM");
                            }
                            if (keep == null) {
                                keep = bestMap.get("LOW");
                            }
                            if (keep != null) {
                                part.clear();
                                part.add(keep);
                            }
                        }
                    }
                    if (sub) {
                        if (cfg.getBooleanProperty(Q_SUBTITLES, false)) {
                            String subtitleUrl = MediaEntry.get("SubTitleUrl");
                            if (!isEmpty(subtitleUrl)) {
                                final String name = fileName + ".srt";
                                final DownloadLink link = createDownloadlink("decrypted://tvthek.orf.at/subtitles/" + System.currentTimeMillis() + new Random().nextInt(1000000));
                                link.setAvailable(true);
                                link.setFinalFileName(name);
                                link.setProperty("directURL", subtitleUrl);
                                link.setProperty("directName", name);
                                link.setProperty("streamingType", "subtitle");
                                part.add(link);
                                vIdTemp = fileName;
                            }
                        }
                    }
                    newRet.addAll(part);
                    part.clear();
                    bestMap.clear();
                }
                if (newRet.size() > 1) {
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(fpName);
                    fp.addLinks(newRet);
                }
                ret = newRet;
            }
        } catch (final Throwable e) {
            logger.severe(e.getMessage());
        }
        for (DownloadLink dl : ret) {
            try {
                distribute(dl);
            } catch (final Throwable e) {
                /* does not exist in 09581 */
            }
        }
        return ret;
    }

    private boolean isEmpty(String ip) {
        return ip == null || ip.trim().length() == 0;
    }

    private String getTitle(Browser br) {
        String title = br.getRegex("<title>(.*?)\\s*\\-\\s*ORF TVthek</title>").getMatch(0);
        String titleUT = br.getRegex("<span class=\"BoxHeadlineUT\">([^<]+)</").getMatch(0);
        if (title == null) title = br.getRegex("<meta property=\"og:title\" content=\"([^\"]+)\"").getMatch(0);
        if (title == null) title = br.getRegex("\'playerTitle\':\\s*\'([^\'])\'$").getMatch(0);
        if (title != null) title = Encoding.htmlDecode(title + (titleUT != null ? "__" + titleUT.replaceAll(":$", "") : "").trim());
        if (title == null) title = "UnknownTitle_" + System.currentTimeMillis();
        return title;
    }

    private String humanReadableQualityIdentifier(String s) {
        if ("Q1A".equals(s)) return "LOW";
        if ("Q4A".equals(s)) return "MEDIUM";
        if ("Q6A".equals(s)) return "HIGH";
        return s;
    }

    private boolean unknownQualityIdentifier(String s) {
        if (s.matches("(DESCRIPTION|SMIL|SUBTITLEURL|DURATION|TRANSCRIPTURL|TITLE|QUALITY|QUALITY_STRING|PROTOCOL|TYPE|DELIVERY)")) return false;
        return true;
    }

    private String decodeUnicode(String s) {
        Pattern p = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        String res = s;
        Matcher m = p.matcher(res);
        while (m.find()) {
            res = res.replaceAll("\\" + m.group(0), Character.toString((char) Integer.parseInt(m.group(1), 16)));
        }
        return res;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}