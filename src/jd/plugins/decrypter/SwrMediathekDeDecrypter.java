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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "swrmediathek.de" }, urls = { "https?://(?:www\\.)?swrmediathek\\.de/player\\.htm\\?show=[a-z0-9\\-]+" })
public class SwrMediathekDeDecrypter extends PluginForDecrypt {

    public SwrMediathekDeDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /** Settings stuff */
    private static final String           FASTLINKCHECK        = "FASTLINKCHECK";
    private static final String           Q_SUBTITLES          = "Q_SUBTITLES";
    private static final String           Q_BEST               = "Q_BEST";
    private static final String           ALLOW_720p           = "ALLOW_720p";
    private static final String           ALLOW_544p           = "ALLOW_544p";
    private static final String           ALLOW_288p           = "ALLOW_288p";
    private static final String           ALLOW_180p           = "ALLOW_180p";
    /* Constants */
    private static final String           DOMAIN               = "swrmediathek.de";

    /* Variables */
    private LinkedHashMap<String, String> FOUNDQUALITIES       = new LinkedHashMap<String, String>();
    private String                        TITLE                = null;
    private String                        DATE                 = null;
    private String                        DATE_FORMATTED       = null;
    private String                        PARAMETER            = null;

    private static Object                 ctrlLock             = new Object();
    private static AtomicBoolean          pluginLoaded         = new AtomicBoolean(false);

    private String                        VIDEOID              = null;
    private String                        SUBTITLE_URL         = null;
    private boolean                       FASTLINKCHECK_active = false;

    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        FASTLINKCHECK_active = SubConfiguration.getConfig(DOMAIN).getBooleanProperty(FASTLINKCHECK, false);
        final FilePackage fp = FilePackage.getInstance();
        final SubConfiguration cfg = SubConfiguration.getConfig(DOMAIN);
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final ArrayList<String> selectedQualities = new ArrayList<String>();
        final boolean grabsubtitles = cfg.getBooleanProperty(Q_SUBTITLES, false);
        PARAMETER = param.toString().replace("/lq/", "/");
        br.setFollowRedirects(true);
        synchronized (ctrlLock) {
            if (!pluginLoaded.get()) {
                // load host plugin!
                JDUtilities.getPluginForHost(DOMAIN);
                pluginLoaded.set(true);
            }
            VIDEOID = new Regex(PARAMETER, "show=([a-z0-9\\-]+)$").getMatch(0);
            if (!VIDEOID.contains("-")) {
                final DownloadLink dl = createDownloadlink("directhttp://" + PARAMETER);
                dl.setFinalFileName(VIDEOID);
                dl.setProperty("offline", true);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            /* Decrypt start */
            /* Decrypt qualities START */
            /*
             * Example of a link which is by default not available via http:
             * http://swrmediathek.de/player.htm?show=3229e410-166d-11e4-9894-0026b975f2e6
             */
            br.getPage("http://swrmediathek.de/AjaxEntry?ekey=" + VIDEOID);
            if (br.containsHTML("<h1>HTTP Status 403") || br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404 || br.toString().length() < 100) {
                final DownloadLink dl = createDownloadlink("directhttp://" + PARAMETER);
                dl.setFinalFileName(VIDEOID);
                dl.setProperty("offline", true);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            final LinkedHashMap<String, Object> attr_main = (LinkedHashMap<String, Object>) entries.get("attr");
            final String description = (String) attr_main.get("entry_descl");
            final boolean rtmpExists = br.containsHTML("rtmp://");
            TITLE = (String) attr_main.get("entry_title");
            TITLE = encodeUnicode(TITLE);
            DATE = (String) attr_main.get("entry_pdateh");
            DATE_FORMATTED = formatDate(DATE);
            fp.setName(DATE_FORMATTED + "_swr_" + TITLE);
            final ArrayList<Object> sub = (ArrayList) entries.get("sub");
            for (final Object o : sub) {
                final LinkedHashMap<String, Object> media_info = (LinkedHashMap<String, Object>) o;
                final String media_type = (String) media_info.get("name");
                /* Skip thumbnails and other stuff */
                if (media_type == null || !media_type.equals("entry_media")) {
                    continue;
                }
                final LinkedHashMap<String, Object> attr = (LinkedHashMap<String, Object>) media_info.get("attr");
                // final String media_qualitynumber = (String) attr.get("val1");
                final String media_codec = (String) attr.get("val0");
                String media_url = (String) attr.get("val2");
                /* Skip hds/hls mobile stuff */
                if (media_url == null || media_codec == null || !media_codec.equals("h264")) {
                    continue;
                }
                /*
                 * If we got rtmp + http urls, the http urls will be broken so we have to take the rtmp urls and convert them to working
                 * http urls (what a logic...)
                 */
                if (rtmpExists && !media_url.startsWith("rtmp")) {
                    continue;
                }
                /* Avoid rtmp */
                if (media_url.matches("rtmp://fm-ondemand\\.[a-z0-9\\.]+/ondemand/.+")) {
                    media_url = "http://pd-ondemand." + new Regex(media_url, "rtmp://fm-ondemand\\.(.+)").getMatch(0);
                    media_url = media_url.replace("/ondemand/", "/");
                } else if (media_url.matches("rtmp://fc\\-ondemand\\.swr\\.de/.+/\\d+\\.[a-z]+\\.mp4")) {
                    media_url = "http://pd-ondemand.swr.de/" + new Regex(media_url, "rtmp://fc\\-ondemand\\.swr\\.de/[^/]+/[^/]+/(.+)").getMatch(0);
                }
                if (media_url.contains("s.mp4")) {
                    FOUNDQUALITIES.put("180p", media_url);
                } else if (media_url.contains("m.mp4")) {
                    FOUNDQUALITIES.put("288p", media_url);
                } else if (media_url.contains("xl.mp4")) {
                    FOUNDQUALITIES.put("720p", media_url);
                } else if (media_url.contains("l.mp4")) {
                    FOUNDQUALITIES.put("544p", media_url);
                }
            }
            SUBTITLE_URL = (String) entries.get("entry_capuri");

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
                boolean q180p = cfg.getBooleanProperty(ALLOW_180p, false);
                boolean q288p = cfg.getBooleanProperty(ALLOW_288p, false);
                boolean q544p = cfg.getBooleanProperty(ALLOW_544p, false);
                boolean q720p = cfg.getBooleanProperty(ALLOW_720p, false);
                if (q288p == false && q544p == false && q720p == false) {
                    q288p = true;
                    q544p = true;
                    q720p = true;
                }
                if (q180p) {
                    selectedQualities.add("180p");
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
                        try {
                            subtitlelink.setContentUrl(PARAMETER);
                            if (description != null) {
                                subtitlelink.setComment(description);
                            }
                        } catch (final Throwable e) {
                            /* Not available in 0.9.581 Stable */
                        }

                        fp.add(subtitlelink);
                        decryptedLinks.add(subtitlelink);
                    }
                    try {
                        dl.setContentUrl(PARAMETER);
                        if (description != null) {
                            dl.setComment(description);
                        }
                    } catch (final Throwable e) {
                        /* Not available in 0.9.581 Stable */
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

    @SuppressWarnings("deprecation")
    private DownloadLink getVideoDownloadlink(final String plain_qualityname) throws ParseException {
        DownloadLink dl = null;
        final String directlink = FOUNDQUALITIES.get(plain_qualityname);
        if (directlink != null) {
            final String ext = getFileNameExtensionFromString(directlink);
            final String ftitle = DATE_FORMATTED + "_swr_" + TITLE + "_" + plain_qualityname + ext;
            dl = createDownloadlink("http://swrmediathekdecrypted.de/" + System.currentTimeMillis() + new Random().nextInt(10000));
            dl.setProperty("directlink", directlink);
            dl.setProperty("plain_qualityname", plain_qualityname);
            dl.setProperty("mainlink", PARAMETER);
            dl.setProperty("plain_filename", ftitle);
            dl.setProperty("plain_linkid", VIDEOID);
            dl.setProperty("plain_ext", ext);
            dl.setProperty("LINKDUPEID", DOMAIN + "_" + ftitle);
            dl.setName(ftitle);
            if (FASTLINKCHECK_active) {
                dl.setAvailable(true);
            }

            dl.setContainerUrl(PARAMETER);

            return dl;
        }
        return dl;
    }

    @SuppressWarnings("deprecation")
    private DownloadLink getSubtitleDownloadlink(final DownloadLink vidlink) throws ParseException {
        final String plain_qualityname = vidlink.getStringProperty("plain_qualityname", null);
        final String ext = ".xml";
        final String ftitle = DATE_FORMATTED + "_swr_" + TITLE + "_" + plain_qualityname + ext;
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
        dl.setContainerUrl(PARAMETER);
        dl.setAvailable(true);
        return dl;
    }

    private String formatDate(final String input) {
        final long date = TimeFormatter.getMilliSeconds(input, "dd.MM.yyyy, HH.mm", Locale.GERMAN);
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        Date theDate = new Date(date);
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            formattedDate = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent input error killing plugin */
            formattedDate = input;
        }
        return formattedDate;
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