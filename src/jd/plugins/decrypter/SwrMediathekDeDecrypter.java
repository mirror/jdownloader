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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

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
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "swrmediathek.de" }, urls = { "http://(www\\.)?swrmediathek\\.de/player\\.htm\\?show=[a-z0-9\\-]+" }, flags = { 0 })
public class SwrMediathekDeDecrypter extends PluginForDecrypt {

    public SwrMediathekDeDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String           DOMAIN         = "swrmediathek.de";

    private LinkedHashMap<String, String> FOUNDQUALITIES = new LinkedHashMap<String, String>();
    private String                        FILENAME       = null;
    private String                        PARAMETER      = null;

    /** Settings stuff */
    private static final String           FASTLINKCHECK  = "FASTLINKCHECK";
    private static final String           Q_SUBTITLES    = "Q_SUBTITLES";
    private static final String           Q_BEST         = "Q_BEST";
    private static final String           ALLOW_720p     = "ALLOW_720p";
    private static final String           ALLOW_544p     = "ALLOW_544p";
    private static final String           ALLOW_288p     = "ALLOW_288p";

    private static Object                 ctrlLock       = new Object();
    private static AtomicBoolean          pluginLoaded   = new AtomicBoolean(false);

    private String                        VIDEOID        = null;
    private String                        SUBTITLE_URL   = null;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final SubConfiguration cfg = SubConfiguration.getConfig(DOMAIN);
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final ArrayList<String> selectedQualities = new ArrayList<String>();
        final boolean grabsubtitles = cfg.getBooleanProperty(Q_SUBTITLES, false);
        PARAMETER = param.toString().replace("/lq/", "/");
        VIDEOID = new Regex(PARAMETER, "([a-z0-9\\-]+)$").getMatch(0);
        br.setFollowRedirects(true);
        synchronized (ctrlLock) {
            if (!pluginLoaded.get()) {
                // load plugin!
                JDUtilities.getPluginForHost(DOMAIN);
                pluginLoaded.set(true);
            }
            br.getPage(PARAMETER);
            final String ekey = new Regex(PARAMETER, "show=(.+)").getMatch(0);
            if (ekey == null || !ekey.contains("-")) {
                final DownloadLink dl = createDownloadlink("directhttp://" + PARAMETER);
                dl.setProperty("offline", true);
                dl.setFinalFileName(VIDEOID);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            /* Decrypt start */
            final String title = br.getRegex("<p class=\"dachzeile\">([^<>\"]*?)</p>").getMatch(0);
            final String subtitle = br.getRegex("</p>[\t\n\r ]+<h4 class=\"headline\">([^<>\"]*?)<").getMatch(0);
            if (title == null || subtitle == null) {
                logger.warning("Decrypter broken for link: " + PARAMETER);
                return null;
            }
            FILENAME = Encoding.htmlDecode(title).trim() + " - " + Encoding.htmlDecode(subtitle).trim();
            FILENAME = encodeUnicode(FILENAME);
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(FILENAME);

            /* Decrypt qualities START */
            /*
             * Example of a link which does not seem to be available via http:
             * http://swrmediathek.de/player.htm?show=3229e410-166d-11e4-9894-0026b975f2e6
             */
            br.getPage("http://swrmediathek.de/AjaxEntry?ekey=" + ekey);
            SUBTITLE_URL = br.getRegex("\"entry_capuri\":\"(https?://[^<>\"]*?)\"").getMatch(0);
            br.getPage("http://swrmediathek.de/rtmpQuals/" + VIDEOID + "/clips.smil");
            final String[] qualities = br.getRegex("src=\"([^<>\"]*?\\.mp4)\"").getColumn(0);
            for (String directquality : qualities) {
                directquality = "http://pd-ondemand.swr.de/" + directquality;
                if (directquality.contains("m.mp4")) {
                    FOUNDQUALITIES.put("288p", directquality);
                } else if (directquality.contains("xl.mp4")) {
                    FOUNDQUALITIES.put("720p", directquality);
                } else if (directquality.contains("l.mp4")) {
                    FOUNDQUALITIES.put("544p", directquality);
                }
            }

            if (FOUNDQUALITIES == null) {
                logger.warning("Decrypter broken for link: " + PARAMETER);
                return null;
            }
            /* Decrypt qualities END */
            /* Decrypt qualities, selected by the user */
            if (cfg.getBooleanProperty(Q_BEST, false)) {
                final String[] quals = { "720p", "544p", "288p" };
                for (final String qualvalue : quals) {
                    if (FOUNDQUALITIES.get(qualvalue) != null) {
                        selectedQualities.add(qualvalue);
                        break;
                    }
                }
            } else {
                /** User selected nothing -> Decrypt everything */
                boolean q288p = cfg.getBooleanProperty(ALLOW_288p, false);
                boolean q544p = cfg.getBooleanProperty(ALLOW_544p, false);
                boolean q720p = cfg.getBooleanProperty(ALLOW_720p, false);
                if (q288p == false && q544p == false && q720p == false) {
                    q288p = true;
                    q544p = true;
                    q720p = true;
                }
                if (q288p) {
                    selectedQualities.add("288p");
                }
                if (q544p) {
                    selectedQualities.add("544p");
                }
                if (q720p) {
                    selectedQualities.add("720p");
                }
            }
            for (final String selectedQualityValue : selectedQualities) {
                final DownloadLink dl = getVideoDownloadlink(selectedQualityValue);
                if (dl != null) {
                    if (grabsubtitles && SUBTITLE_URL != null) {
                        final DownloadLink subtitlelink = getSubtitleDownloadlink(dl);
                        fp.add(subtitlelink);
                        decryptedLinks.add(subtitlelink);
                    }
                    fp.add(dl);
                    decryptedLinks.add(dl);
                }
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.info(DOMAIN + ": None of the selected qualities were found, decrypting done...");
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    private DownloadLink getVideoDownloadlink(final String plain_qualityname) throws ParseException {
        final String directlink = FOUNDQUALITIES.get(plain_qualityname);
        if (directlink != null) {
            final String ext = directlink.substring(directlink.lastIndexOf("."));
            final String ftitle = FILENAME + "_" + plain_qualityname + ext;
            final DownloadLink dl = createDownloadlink("http://swrmediathekdecrypted.de/" + System.currentTimeMillis() + new Random().nextInt(10000));
            dl.setProperty("directlink", directlink);
            dl.setProperty("plain_qualityname", plain_qualityname);
            dl.setProperty("mainlink", PARAMETER);
            dl.setProperty("plain_filename", ftitle);
            dl.setProperty("plain_linkid", VIDEOID);
            dl.setProperty("plain_ext", ext);
            dl.setProperty("LINKDUPEID", DOMAIN + "_" + ftitle);
            dl.setName(ftitle);
            if (SubConfiguration.getConfig(DOMAIN).getBooleanProperty(FASTLINKCHECK, false)) {
                dl.setAvailable(true);
            }
            return dl;
        } else {
            return null;
        }
    }

    private DownloadLink getSubtitleDownloadlink(final DownloadLink vidlink) throws ParseException {
        final String plain_qualityname = vidlink.getStringProperty("plain_qualityname", null);
        final String ext = ".xml";
        final String ftitle = FILENAME + "_" + plain_qualityname + ext;
        final DownloadLink dl = createDownloadlink("http://swrmediathekdecrypted.de/" + System.currentTimeMillis() + new Random().nextInt(10000));
        dl.setProperty("directlink", SUBTITLE_URL);
        dl.setProperty("plain_qualityname", plain_qualityname);
        dl.setProperty("streamingType", "subtitle");
        dl.setProperty("mainlink", PARAMETER);
        dl.setProperty("plain_filename", ftitle);
        dl.setProperty("plain_linkid", VIDEOID);
        dl.setProperty("plain_ext", ".srt");
        dl.setProperty("LINKDUPEID", DOMAIN + "_" + ftitle);
        dl.setName(ftitle);
        dl.setAvailable(true);
        return dl;
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