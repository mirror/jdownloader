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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;

import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.requests.PostRequest;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDHexUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "discovery.com", "tlc.com", "animalplanet.com" }, urls = { "http://www\\.discovery\\.com/tv\\-shows/[a-z0-9\\-]+/videos/[a-z0-9\\-]+/", "http://www\\.tlc\\.com/tv\\-shows/[a-z0-9\\-]+/videos/[a-z0-9\\-]+/", "http://www\\.animalplanet\\.com/tv\\-shows/[a-z0-9\\-]+/videos/[a-z0-9\\-]+/" })
public class DiscoveryComDecrypter extends PluginForDecrypt {
    public DiscoveryComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Tags: Discovery Communications Inc */
    /* Last checked: 18.04.15 */
    private static final String playerKey                  = "AQ~~,AAAAAAABkyc~,PFOoGLfSWhZ81Y0yRvH9oSpaitgzc4BX";
    private static final String playerID                   = "3688727558001";
    private static final String video_player_brightcove_id = "103207";
    /* Example brightcove player/AMFRequest needed: http://www.discovery.com/tv-shows/deadliest-catch/videos/best-of-catch-season-11/ */
    /* Example http: http://www.discovery.com/tv-shows/naked-and-afraid/videos/louisiana/ */
    /* Broken flv urls of source: http://discidevflash-f.akamaihd.net//digmed/dp/2015-04/10/879577/145609.002.01.001.flv */
    /*
     * Working HLS:
     * http://discidevflash-f.akamaihd.net/i/digmed/dp/2015-04/10/879577/145609.002.01.001-,110k,200k,400k,600k,800k,1500k,3500k
     * ,.mp4.csmil/master.m3u8
     */
    /*
     * Working http: http://discsmil.edgesuite.net/digmed/dp/2015-04/10/879577/145609.002.01.001-110k.mp4
     * http://discsmil.edgesuite.net/digmed/dp/2015-04/10/879577/145609.002.01.001-3500k.mp4
     */
    private static final String DOMAIN                     = "discovery.com";
    /* Settings stuff */
    private static final String FAST_LINKCHECK             = "FAST_LINKCHECK";
    private static final String GRAB_SUBTITLE              = "GRAB_SUBTITLE";
    private static final String ALLOW_110k                 = "110k";
    private static final String ALLOW_200k                 = "200k";
    private static final String ALLOW_400k                 = "400k";
    private static final String ALLOW_600k                 = "600k";
    private static final String ALLOW_800k                 = "800k";
    private static final String ALLOW_1500k                = "1500k";
    private static final String ALLOW_3500k                = "3500k";
    private static final String ALLOW_5000k                = "5000k";
    final String[]              qualities                  = { ALLOW_110k, ALLOW_200k, ALLOW_400k, ALLOW_600k, ALLOW_800k, ALLOW_1500k, ALLOW_3500k, ALLOW_5000k };

    /** TODO: Fix/implement subtitle support */
    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final String parameter = param.toString();
        String contentID = null;
        String thisvideoID = null;
        String show = null;
        String json = null;
        String videoplayerkey = null;
        String referenceID = null;
        String bitrate = null;
        LinkedHashMap<String, Object> entries = null;
        ArrayList<Object> ressourcelist = null;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final LinkedHashMap<String, String[]> formats = jd.plugins.hoster.DiscoveryCom.formats;
        final String host_nice = new Regex(parameter, "http://(?:www\\.)?([^/]+)").getMatch(0);
        final String host_plain = host_nice.replace(".com", "");
        final String decryptedhost = "http://" + host_nice + "decrypted";
        final SubConfiguration cfg = SubConfiguration.getConfig(DOMAIN);
        final boolean grabSubtitle = cfg.getBooleanProperty(GRAB_SUBTITLE, false);
        final boolean fastLinkcheck = cfg.getBooleanProperty(FAST_LINKCHECK, false);
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final Browser jsonBR = br.cloneBrowser();
        jsonBR.getPage(this.br.getURL() + "?flat=1");
        if (jsonBR.getHttpConnection().getResponseCode() == 403 || jsonBR.containsHTML("This site is not available in your region")) {
            decryptedLinks.add(this.createOfflinelink(parameter, "GEO_BLOCKED"));
            return decryptedLinks;
        }
        json = jsonBR.toString();
        if (json == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
        videoplayerkey = (String) entries.get("video_player_key");
        if (videoplayerkey == null) {
            videoplayerkey = playerKey;
        }
        ressourcelist = (ArrayList) entries.get("playlist");
        for (final Object plo : ressourcelist) {
            final LinkedHashMap<String, Object> playlist_entry = (LinkedHashMap<String, Object>) plo;
            final String thumbnail = (String) playlist_entry.get("thumbnailURL");
            final Object thisvideoIDo = playlist_entry.get("_id");
            String entryname = (String) playlist_entry.get("name");
            String description = (String) playlist_entry.get("description");
            contentID = (String) playlist_entry.get("contentId");
            if (thisvideoIDo instanceof Integer) {
                thisvideoID = Integer.toString(((Number) thisvideoIDo).intValue());
            } else {
                thisvideoID = (String) thisvideoIDo;
            }
            show = (String) playlist_entry.get("show");
            referenceID = (String) playlist_entry.get("referenceId");
            if (thisvideoID == null || show == null || entryname == null || thumbnail == null || referenceID == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            show = encodeUnicode(show);
            entryname = encodeUnicode(entryname);
            if (contentID != null && !contentID.equals("")) {
                /* contentID available --> Easy parsing */
                br.getPage("http://snagplayer.video.dp." + host_plain + ".com/" + contentID + "/snag-it-player.htm?auto=yes&_fwEnv=prod");
                json = br.getRegex("var videoListJSON = (\\{.*?\\});\n").getMatch(0);
                if (json == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                final HashMap<String, Object> videomap = (HashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
                final long videosCount = JavaScriptEngineFactory.toLong(videomap.get("count"), 0);
                if (videosCount <= 0) {
                    logger.info("Found no videos for contentID: " + contentID);
                    continue;
                }
                final ArrayList<Object> episodelist = (ArrayList) videomap.get("clips");
                for (final Object o : episodelist) {
                    String dllink = null;
                    final HashMap<String, Object> vinfo = (HashMap<String, Object>) o;
                    final ArrayList<Object> vurls = (ArrayList) vinfo.get("mp4");
                    String titleseries = (String) vinfo.get("programTitle");
                    String titleepisode = (String) vinfo.get("episodeTitle");
                    description = (String) vinfo.get("videoCaption");
                    final String subtitle = (String) vinfo.get("srtUrl");
                    if (titleseries == null || titleepisode == null) {
                        return null;
                    }
                    titleseries = encodeUnicode(titleseries);
                    titleepisode = encodeUnicode(titleepisode);
                    final String filenamepart = titleseries + " - " + titleepisode;
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(filenamepart);
                    DownloadLink dl = null;
                    for (final Object urlo : vurls) {
                        dl = createDownloadlink(decryptedhost + System.currentTimeMillis() + new Random().nextInt(1000000000));
                        final HashMap<String, Object> urlinfo = (HashMap<String, Object>) urlo;
                        bitrate = (String) urlinfo.get("bitrate");
                        dllink = (String) urlinfo.get("href");
                        try {
                            getFormatString(formats.get(bitrate));
                        } catch (final Throwable e) {
                            logger.info("currentBITRATE:" + bitrate);
                        }
                        final String filenamepart_two = filenamepart + "_" + getFormatString(formats.get(bitrate));
                        dl.setContentUrl(parameter);
                        if (description != null) {
                            dl.setComment(description);
                        }
                        dl._setFilePackage(fp);
                        dl.setProperty("bitrate", bitrate);
                        dl.setProperty("mainlink", parameter);
                        final String filenamevideo = filenamepart_two + ".mp4";
                        /* Video */
                        dl.setFinalFileName(filenamevideo);
                        dl.setProperty("directlink", dllink);
                        dl.setProperty("directfilename", filenamevideo);
                        dl.setLinkID(filenamevideo);
                        if (fastLinkcheck) {
                            dl.setAvailable(true);
                        }
                        if (cfg.getBooleanProperty(bitrate, false)) {
                            decryptedLinks.add(dl);
                        }
                        // final String filenameaudio = filenamepart_two + ".srt";
                        // /* Subtitle */
                        // dl.setUrlDownload(decryptedhost + System.currentTimeMillis() + new Random().nextInt(1000000000));
                        // dl.setFinalFileName(filenameaudio);
                        // dl.setProperty("directlink", subtitle);
                        // dl.setProperty("directfilename", filenameaudio);
                        // dl.setLinkID(filenameaudio);
                        // if (fastLinkcheck) {
                        // dl.setAvailable(true);
                        // }
                        // if (grabSubtitle && !inValidate(subtitle)) {
                        // decryptedLinks.add(dl);
                        // }
                    }
                }
            } else {
                /* contentID NOT available --> Brightcove handling needed --> Nope we can easily avoid this :) */
                /* AMF-Request */
                this.br.getHeaders().put("Content-Type", "application/x-amf");
                getAMFRequest(this.br, createAMFMessage("ca38f4f2cff16b53daa67436490b7d344d4f222c", playerID, referenceID, video_player_brightcove_id), videoplayerkey);
                final String filenamepart = show + " - " + entryname;
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(filenamepart);
                final String[] urls = br.getRegex("(http://discsmil\\.edgesuite\\.net/digmed/\\w+/(?:[\\d\\-]+/){1,}[A-Za-z0-9\\-_\\.]+\\-\\d+k\\.mp4)").getColumn(0);
                for (final String dllink : urls) {
                    final DownloadLink dl = createDownloadlink(decryptedhost + System.currentTimeMillis() + new Random().nextInt(1000000000));
                    bitrate = new Regex(dllink, "(\\d+k)\\.mp4$").getMatch(0);
                    final String filenamepart_two = filenamepart + "_" + getFormatString(formats.get(bitrate));
                    dl.setContentUrl(parameter);
                    if (description != null) {
                        dl.setComment(description);
                    }
                    dl._setFilePackage(fp);
                    dl.setProperty("bitrate", bitrate);
                    dl.setProperty("mainlink", parameter);
                    final String filenamevideo = filenamepart_two + ".mp4";
                    /* Video */
                    dl.setFinalFileName(filenamevideo);
                    dl.setProperty("directlink", dllink);
                    dl.setProperty("directfilename", filenamevideo);
                    dl.setLinkID(filenamevideo);
                    if (fastLinkcheck) {
                        dl.setAvailable(true);
                    }
                    if (cfg.getBooleanProperty(bitrate, false)) {
                        decryptedLinks.add(dl);
                    }
                }
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.info(DOMAIN + ": None of the selected formats were found or none were selected, decrypting done...");
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    private String getFormatString(final String[] formatinfo) {
        String formatString = "";
        final String videoCodec = formatinfo[0];
        final String videoBitrate = formatinfo[1];
        final String videoResolution = formatinfo[2];
        final String audioCodec = formatinfo[3];
        final String audioBitrate = formatinfo[4];
        if (videoCodec != null) {
            formatString += videoCodec + "_";
        }
        if (videoResolution != null) {
            formatString += videoResolution + "_";
        }
        if (videoBitrate != null) {
            formatString += videoBitrate + "_";
        }
        if (audioCodec != null) {
            formatString += audioCodec + "_";
        }
        if (audioBitrate != null) {
            formatString += audioBitrate;
        }
        if (formatString.endsWith("_")) {
            formatString = formatString.substring(0, formatString.lastIndexOf("_"));
        }
        return formatString;
    }

    private void getAMFRequest(final Browser amf, final byte[] b, String s) throws IOException {
        amf.getHeaders().put("Content-Type", "application/x-amf");
        amf.setKeepResponseContentBytes(true);
        PostRequest request = amf.createPostRequest("http://c.brightcove.com/services/messagebroker/amf?playerKey=" + s, (String) null);
        request.setPostBytes(b);
        amf.openRequestConnection(request);
        amf.loadConnection(null);
    }

    private byte[] createAMFMessage(String... s) {
        String data = "0003000000010046636f6d2e627269676874636f76652e706c617965722e72756e74696d652e506c617965724d656469614661636164652e66696e644d6564696142795265666572656e6365496400022f3100000";
        data += "06" + "50a00000004020028" + JDHexUtils.getHexString(s[0]); // 0x06(String marker) + length + String b
        data += "00428ad6ca5dbb8800020020";
        data += JDHexUtils.getHexString(s[2]);
        data += "0040f9327000000000";
        return JDHexUtils.getByteArray(data);
    }
    // private String getHexLength(final String s, boolean amf3) {
    // String result = Integer.toHexString(s.length() | 1);
    // if (amf3) {
    // result = "";
    // for (int i : getUInt29(s.length() << 1 | 1)) {
    // if (i == 0) {
    // break;
    // }
    // result += Integer.toHexString(i);
    // }
    // }
    // return result.length() % 2 > 0 ? "0" + result : result;
    // }

    // private int[] getUInt29(int ref) {
    // int[] buf = new int[4];
    // if (ref < 0x80) {
    // buf[0] = ref;
    // } else if (ref < 0x4000) {
    // buf[0] = (((ref >> 7) & 0x7F) | 0x80);
    // buf[1] = ref & 0x7F;
    // } else if (ref < 0x200000) {
    // buf[0] = (((ref >> 14) & 0x7F) | 0x80);
    // buf[1] = (((ref >> 7) & 0x7F) | 0x80);
    // buf[2] = ref & 0x7F;
    // } else if (ref < 0x40000000) {
    // buf[0] = (((ref >> 22) & 0x7F) | 0x80);
    // buf[1] = (((ref >> 15) & 0x7F) | 0x80);
    // buf[2] = (((ref >> 8) & 0x7F) | 0x80);
    // buf[3] = ref & 0xFF;
    // } else {
    // logger.warning("about.com(amf3): Integer out of range: " + ref);
    // }
    // return buf;
    // }
    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}