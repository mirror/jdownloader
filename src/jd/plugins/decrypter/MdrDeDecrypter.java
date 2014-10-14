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
import java.util.LinkedHashMap;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mdr.de" }, urls = { "http://(www\\.)?mdr\\.de/[a-z0-9\\-]+/video\\d+[a-z0-9\\-_]*\\.html" }, flags = { 0 })
public class MdrDeDecrypter extends PluginForDecrypt {

    public MdrDeDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String[]                      QUALITIES            = { "720x576", "960x544", "640x360", "512x288", "480x272x384000", "480x272x240000", "256x144" };
    private static final String                 DOMAIN               = "mdr.de";

    private LinkedHashMap<String, DownloadLink> FOUNDQUALITIES       = new LinkedHashMap<String, DownloadLink>();
    /** Settings stuff */
    private static final String                 ALLOW_SUBTITLES      = "ALLOW_SUBTITLES";
    private static final String                 ALLOW_BEST           = "ALLOW_BEST";
    private static final String                 ALLOW_720x576        = "ALLOW_720x576";
    private static final String                 ALLOW_960x544        = "ALLOW_960x544";
    private static final String                 ALLOW_640x360        = "ALLOW_640x360";
    private static final String                 ALLOW_512x288        = "ALLOW_512x288";
    private static final String                 ALLOW_480x272_higher = "ALLOW_480x272_higher";
    private static final String                 ALLOW_480x272_lower  = "ALLOW_480x272_lower";
    private static final String                 ALLOW_256x144        = "ALLOW_256x144";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final SubConfiguration cfg = SubConfiguration.getConfig(DOMAIN);
        final boolean grab_subtitles = cfg.getBooleanProperty(ALLOW_SUBTITLES, false);

        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        final Regex clipinfo = new Regex(parameter, "mdr\\.de/([a-z0-9\\-]+)/video(\\d+)");
        final String url_clipname = clipinfo.getMatch(0);
        final String clip_id = clipinfo.getMatch(1);
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.getPage("http://www.mdr.de/" + url_clipname + "/video" + clip_id + "-avCustom.xml");
        if (br.getHttpConnection().getResponseCode() == 404) {
            final DownloadLink dl = createDownloadlink("directhttp://" + parameter);
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        final String subtitle_url = br.getRegex("<videoSubtitleUrl>(http://[^<>\"]*?\\.xml)</videoSubtitleUrl>").getMatch(0);

        /* Decrypt start */
        String title = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        if (title == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        title = Encoding.htmlDecode(title.trim());
        title = encodeUnicode(title);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);

        /** Decrypt qualities START */
        final String[] qualities = br.getRegex("<asset>(.*?)</asset>").getColumn(0);
        if (qualities == null || qualities.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String qualityinfo : qualities) {
            final String fsize = getXML(qualityinfo, "fileSize");
            final String url = getXML(qualityinfo, "progressiveDownloadUrl");
            final String sizewidth = getXML(qualityinfo, "frameWidth");
            final String sizeheight = getXML(qualityinfo, "frameHeight");
            final String bitrateVideo = getXML(qualityinfo, "bitrateVideo");
            if (url == null || fsize == null || sizewidth == null || sizeheight == null || bitrateVideo == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            String qualityString;
            if (sizewidth.equals("480")) {
                qualityString = sizewidth + "x" + sizeheight + "x" + bitrateVideo;
            } else {
                qualityString = sizewidth + "x" + sizeheight;
            }
            String filename = title;
            final String ext = ".mp4";
            filename += "_" + qualityString + ext;
            final DownloadLink fina = createDownloadlink("http://mdrdecrypted.de/" + System.currentTimeMillis() + new Random().nextInt(10000000));
            fina.setDownloadSize(Long.parseLong(fsize));
            fina.setAvailable(true);
            fina.setFinalFileName(filename);
            fina.setProperty("directlink", url);
            fina.setProperty("mainlink", parameter);
            fina.setProperty("plain_filename", filename);
            fina.setProperty("plain_filesize", fsize);
            fina.setProperty("plain_qualityString", qualityString);
            fina.setProperty("LINKDUPEID", "mdrde" + clip_id + "_" + qualityString);
            FOUNDQUALITIES.put(qualityString, fina);
        }

        if (FOUNDQUALITIES == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        /** Decrypt qualities END */
        /** Decrypt qualities, selected by the user */
        final ArrayList<String> selectedQualities = new ArrayList<String>();
        if (cfg.getBooleanProperty(ALLOW_BEST, false)) {
            for (final String quality : QUALITIES) {
                if (FOUNDQUALITIES.get(quality) != null) {
                    selectedQualities.add(quality);
                    break;
                }
            }
        } else {
            /** User selected nothing -> Decrypt everything */
            boolean q720x576 = cfg.getBooleanProperty(ALLOW_720x576, false);
            boolean q960x544 = cfg.getBooleanProperty(ALLOW_960x544, false);
            boolean q640x360 = cfg.getBooleanProperty(ALLOW_640x360, false);
            boolean q512x288 = cfg.getBooleanProperty(ALLOW_512x288, false);
            boolean q480x272_higher = cfg.getBooleanProperty(ALLOW_480x272_higher, false);
            boolean q480x272_lower = cfg.getBooleanProperty(ALLOW_480x272_lower, false);
            boolean q256x144 = cfg.getBooleanProperty(ALLOW_256x144, false);
            if (q720x576 == false && q960x544 == false && q640x360 == false && q512x288 == false && q480x272_higher == false && q480x272_lower == false && q256x144 == false) {
                q720x576 = true;
                q960x544 = true;
                q640x360 = true;
                q512x288 = true;
                q480x272_higher = true;
                q480x272_lower = true;
                q256x144 = true;
            }

            if (q720x576) {
                selectedQualities.add("720x576");
            }
            if (q960x544) {
                selectedQualities.add("960x544");
            }
            if (q640x360) {
                selectedQualities.add("640x360");
            }
            if (q512x288) {
                selectedQualities.add("512x288");
            }
            if (q480x272_higher) {
                selectedQualities.add("480x272x384000");
            }
            if (q480x272_lower) {
                selectedQualities.add("480x272x240000");
            }
            if (q256x144) {
                selectedQualities.add("256x144");
            }
        }
        for (final String selectedQualityValue : selectedQualities) {
            final DownloadLink dl = FOUNDQUALITIES.get(selectedQualityValue);
            if (dl != null) {
                if (grab_subtitles && subtitle_url != null) {
                    final DownloadLink stitle_dl = createDownloadlink("http://mdrdecrypted.de/" + System.currentTimeMillis() + new Random().nextInt(10000000));
                    final String video_qualitystring = dl.getStringProperty("plain_qualityString", null);
                    final String video_linkdupeid = dl.getStringProperty("LINKDUPEID", null) + "_subtitle";
                    final String subtitle_filename = title + "_" + video_qualitystring + ".xml";
                    stitle_dl.setProperty("mainlink", parameter);
                    stitle_dl.setProperty("directlink", subtitle_url);
                    stitle_dl.setProperty("LINKDUPEID", video_linkdupeid);
                    stitle_dl.setProperty("plain_qualityString", "subtitle");
                    stitle_dl.setProperty("plain_filename", subtitle_filename);
                    stitle_dl.setProperty("plain_filesize", "0");
                    fp.add(stitle_dl);
                    decryptedLinks.add(stitle_dl);
                }
                fp.add(dl);
                decryptedLinks.add(dl);
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.info(DOMAIN + ": None of the selected qualities were found, decrypting done...");
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    private String getXML(final String source, final String parameter) {
        return new Regex(source, "<" + parameter + "( type=\"[^<>\"/]*?\")?>([^<>]*?)</" + parameter + ">").getMatch(1);
    }

    /* Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

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