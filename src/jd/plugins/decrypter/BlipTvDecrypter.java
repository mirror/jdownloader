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
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "blip.tv" }, urls = { "http://(\\w+\\.)?blip\\.tv/[a-z0-9\\-]+/[a-z0-9\\-]+\\-\\d+" }, flags = { 0 })
public class BlipTvDecrypter extends PluginForDecrypt {

    public BlipTvDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String[]                      QUALITIES      = { "Source", "Blip HD 1080", "Blip HD 720", "Blip SD", "Blip LD" };
    private static final String                 DOMAIN         = "blip.tv";

    private LinkedHashMap<String, DownloadLink> FOUNDQUALITIES = new LinkedHashMap<String, DownloadLink>();
    /** Settings stuff */
    private static final String                 ALLOW_BEST     = "ALLOW_BEST";
    private static final String                 ALLOW_SOURCE   = "ALLOW_SOURCE";
    private static final String                 ALLOW_1080P    = "ALLOW_HQ";
    private static final String                 ALLOW_720P     = "ALLOW_LQ";
    private static final String                 ALLOW_SD       = "ALLOW_HQ";
    private static final String                 ALLOW_LD       = "ALLOW_HQ";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        final String fid = new Regex(parameter, "(\\d{4,})$").getMatch(0);
        if (fid == null) {
            final DownloadLink dl = createDownloadlink("directhttp://" + parameter);
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        try {
            br.getPage("http://blip.tv/rss/flash/" + fid);
        } catch (final BrowserException eb) {
            final long response = br.getRequest().getHttpConnection().getResponseCode();
            if (response == 404 || response == 410) {
                final DownloadLink dl = createDownloadlink("directhttp://" + parameter);
                dl.setProperty("offline", true);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            throw eb;
        }
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<blip:deleted>1</blip:deleted>")) {
            final DownloadLink dl = createDownloadlink("directhttp://" + parameter);
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        /* Decrypt start */
        String title = br.getRegex("<media:title>([^<>]*?)</media:title>").getMatch(0);
        if (title == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final FilePackage fp = FilePackage.getInstance();
        title = Encoding.htmlDecode(title).trim();
        title = encodeUnicode(title);
        fp.setName(title);

        /** Decrypt qualities START */
        final String[] qualities = br.getRegex("(<media:content url=\"http://.*?isDefault=)").getColumn(0);
        if (qualities == null || qualities.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String qualityinfo : qualities) {
            final String currentQuality = new Regex(qualityinfo, "blip:role=\"([^<>\"]*?)\"").getMatch(0);
            final String fsize = new Regex(qualityinfo, "fileSize=\"(\\d+)\"").getMatch(0);
            final String url = new Regex(qualityinfo, "url=\"(https?://blip\\.tv/[^<>\"]*?)\"").getMatch(0);
            if (url == null || fsize == null || currentQuality == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            String filename = title;
            String ext = url.substring(url.lastIndexOf("."), url.length());
            ext = ext == null ? ".flv" : ext;
            if (filename.endsWith(".")) {
                filename = filename.substring(0, filename.length() - 1);
            }
            filename += "_" + currentQuality + ext;
            final DownloadLink fina = createDownloadlink("http://blipdecrypted.tv/" + System.currentTimeMillis() + new Random().nextInt(10000000));
            fina.setDownloadSize(Long.parseLong(fsize));
            fina.setAvailable(true);
            fina.setFinalFileName(filename);
            fina.setProperty("directlink", url);
            fina.setProperty("mainlink", parameter);
            fina.setProperty("plain_filename", filename);
            fina.setProperty("plain_filesize", fsize);
            fina.setProperty("LINKDUPEID", "bliptv" + fid + "_" + currentQuality);
            try {
                fina.setContentUrl(parameter);
            } catch (final Throwable e) {
                /* Not available in 0.9.581 Stable */
            }
            FOUNDQUALITIES.put(currentQuality, fina);
        }

        if (FOUNDQUALITIES == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        /** Decrypt qualities END */
        /** Decrypt qualities, selected by the user */
        final ArrayList<String> selectedQualities = new ArrayList<String>();
        final SubConfiguration cfg = SubConfiguration.getConfig(DOMAIN);
        if (cfg.getBooleanProperty(ALLOW_BEST, false)) {
            for (final String quality : QUALITIES) {
                if (FOUNDQUALITIES.get(quality) != null) {
                    selectedQualities.add(quality);
                    break;
                }
            }
        } else {
            /** User selected nothing -> Decrypt everything */
            boolean qsource = cfg.getBooleanProperty(ALLOW_SOURCE, false);
            boolean q1080p = cfg.getBooleanProperty(ALLOW_1080P, false);
            boolean q720p = cfg.getBooleanProperty(ALLOW_720P, false);
            boolean qsd = cfg.getBooleanProperty(ALLOW_SD, false);
            boolean qld = cfg.getBooleanProperty(ALLOW_LD, false);
            if (qsource == false && q1080p == false && q720p == false && qsd == false && qld == false) {
                qsource = true;
                q1080p = true;
                q720p = true;
                qsd = true;
                qld = true;
            }

            if (qsource) {
                selectedQualities.add("Source");
            }
            if (q1080p) {
                selectedQualities.add("Blip HD 1080");
            }
            if (q720p) {
                selectedQualities.add("Blip HD 720");
            }
            if (qsd) {
                selectedQualities.add("Blip SD");
            }
            if (qld) {
                selectedQualities.add("Blip LD");
            }
        }
        for (final String selectedQualityValue : selectedQualities) {
            final DownloadLink dl = FOUNDQUALITIES.get(selectedQualityValue);
            if (dl != null) {
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