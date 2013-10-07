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
import java.util.Map.Entry;
import java.util.Random;

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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "orfmediathek.at" }, urls = { "http://(www\\.)?tvthek\\.orf\\.at/programs/[\\w\\-]+" }, flags = { 0 })
public class ORFMediathekDecrypter extends PluginForDecrypt {

    private static final String Q_SUBTITLES = "Q_SUBTITLES";
    private static final String Q_BEST      = "Q_BEST";
    private static final String Q_LOW       = "Q_LOW";
    private static final String Q_MEDIUM    = "Q_MEDIUM";
    private static final String Q_HIGH      = "Q_HIGH";
    private boolean             BEST        = false;

    public ORFMediathekDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (isStableEnviroment()) return decryptedLinks;
        String parameter = param.toString();

        br.getPage(parameter);
        // Check...if offline, add to llinkgrabber so user can see it
        if (br.containsHTML("Keine aktuellen Sendungen vorhanden")) {
            final DownloadLink link = createDownloadlink(parameter.replace("http://", "decrypted://") + "&quality=high");
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
            logger.warning("Decrypter out of date for link: " + parameter);
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
            if (xmlData != null) {
                Document doc = JDUtilities.parseXmlString(Encoding.htmlDecode(xmlData), false);

                /* xmlData --> HashMap */
                // /PlayerConfig/PlayList/Items/Item... --> name, quality, rtmp stream url
                NodeList nl = doc.getElementsByTagName("Item");

                HashMap<String, HashMap<String, String>> MediaEntrys = new HashMap<String, HashMap<String, String>>();
                HashMap<String, String> MediaEntry = null;
                String quality = null, key = null;

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
                    MediaEntrys.put(MediaEntry.get("Title"), MediaEntry);
                }

                String fpName = getTitle(br);
                String extension = ".mp4";
                if (br.getRegex("new MediaCollection\\(\"audio\",").matches()) extension = ".mp3";

                ArrayList<DownloadLink> newRet = new ArrayList<DownloadLink>();
                ArrayList<DownloadLink> part = new ArrayList<DownloadLink>();
                HashMap<String, DownloadLink> bestMap = new HashMap<String, DownloadLink>();
                String lastQualityFMT = null;

                for (Entry<String, HashMap<String, String>> next : MediaEntrys.entrySet()) {
                    MediaEntry = new HashMap<String, String>(next.getValue());
                    String fileName = next.getKey();

                    for (Entry<String, String> stream : MediaEntry.entrySet()) {
                        String url = stream.getValue();
                        String fmt = stream.getKey();
                        if (!isEmpty(fmt)) fmt = fmt.toUpperCase(Locale.ENGLISH).trim();
                        if (!isEmpty(fmt)) {
                            fmt = humanReadableQualityIdentifier(fmt);
                            /* best selection is done at the end */
                            if ("LOW".equals(fmt)) {
                                if ((cfg.getBooleanProperty(Q_LOW, true) || BEST) == false) {
                                    continue;
                                } else {
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
                                if (unknownQuality(fmt)) {
                                    logger.info("ORFMediathek Decrypter: unknown quality --> " + fmt);
                                    logger.info("Link: " + data);
                                }
                                continue;
                            }
                        }
                        lastQualityFMT = fmt;
                        final String name = fileName + "@" + fmt + extension;
                        final DownloadLink link = createDownloadlink(data.replace("http://", "decrypted://") + "&quality=" + fmt + "&hash=" + JDHash.getMD5(name));
                        link.setAvailable(true);
                        link.setFinalFileName(name);
                        link.setBrowserUrl(data);
                        link.setProperty("directURL", url);
                        link.setProperty("directName", name);
                        link.setProperty("directQuality", fmt);
                        link.setProperty("streamingType", "rtmp");

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
                                lastQualityFMT = "MEDIUM";
                                keep = bestMap.get("MEDIUM");
                            }
                            if (keep == null) {
                                lastQualityFMT = "LOW";
                                keep = bestMap.get("LOW");
                            }
                            if (keep != null) {
                                part.clear();
                                part.add(keep);
                            }
                        }
                    }
                    if (cfg.getBooleanProperty(Q_SUBTITLES, false)) {
                        String subtitleUrl = MediaEntry.get("SubTitleUrl");
                        if (!isEmpty(subtitleUrl)) {
                            final String name = fileName + "@" + lastQualityFMT + ".srt";
                            final DownloadLink link = createDownloadlink("decrypted://tvthek.orf.at/subtitles/" + System.currentTimeMillis() + new Random().nextInt(1000000));
                            link.setAvailable(true);
                            link.setFinalFileName(name);
                            link.setProperty("directURL", subtitleUrl);
                            link.setProperty("directName", name);
                            link.setProperty("streamingType", "subtitle");
                            part.add(link);
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

    private boolean unknownQuality(String s) {
        if (s.matches("(DESCRIPTION|SMIL|SUBTITLEURL|DURATION|TRANSCRIPTURL|TITLE)")) return false;
        return true;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}