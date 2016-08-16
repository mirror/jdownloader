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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bloomberg.com" }, urls = { "http://www\\.bloomberg\\.com/news/videos/\\d{4}\\-\\d{2}\\-\\d{2}/[a-z0-9\\-]+" }, flags = { 0 })
public class BloombergComDecrypter extends PluginForDecrypt {

    @SuppressWarnings("deprecation")
    public BloombergComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /*
     * Thanks: https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/bloomberg.py AND
     * https://github.com/isync/www-video-download/blob/master/bloomberg-downloader.pl
     */

    /* Example main: http://www.bloomberg.com/news/videos/2015-04-27/your-future-office-space-might-be-on-wheels */
    /*
     * Example given http url:
     * http://cdn3.videos.bloomberg.com/m/NjM3OTczMA/UMIeosVobRQOoUNoqO5oEZVCtsRtSLiUgykdvG9izJgxODhh/8aa4cdbc-7f5e-440
     * a-9717-15c784b10035_150.mp4
     */
    /*
     * Example hds:
     * http://b5vod-vh.akamaihd.net/z/m/NjM3OTczMA/UMIeosVobRQOoUNoqO5oEZVCtsRtSLiUgykdvG9izJgxODhh/8aa4cdbc-7f5e-440a-9717-15c784b10035_
     * ,15,24,44,70,120,180,240,0.mp4.csmil/manifest.f4m?hdcore=1
     */

    private static final String DOMAIN         = "bloomberg.com";
    /* Settings stuff */
    private static final String FAST_LINKCHECK = "FAST_LINKCHECK";

    @SuppressWarnings({ "deprecation", "unchecked" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        /* Load sister-host plugin */
        JDUtilities.getPluginForHost(DOMAIN);
        final String parameter = param.toString();
        String vid = null;
        String title = null;
        String description = null;
        String cdn_server = null;
        String xmlsource = null;
        LinkedHashMap<String, Object> entries = null;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final LinkedHashMap<String, String[]> formats = jd.plugins.hoster.BloombergCom.formats;
        final String nicehost = new Regex(parameter, "http://(?:www\\.)?([^/]+)").getMatch(0);
        final String decryptedhost = "http://" + nicehost + "decrypted";
        final SubConfiguration cfg = SubConfiguration.getConfig(DOMAIN);
        final boolean fastLinkcheck = cfg.getBooleanProperty(FAST_LINKCHECK, false);
        this.br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
            final DownloadLink dl = this.createOfflinelink(parameter);
            dl.setFinalFileName(vid);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        vid = PluginJSonUtils.getJsonValue(br, "bmmrId");
        if (vid == null) {
            return null;
        }
        br.getPage("http://www.bloomberg.com/api/embed?id=" + vid + "&version=v0.8.14&idType=BMMR");
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        title = (String) entries.get("title");
        title = encodeUnicode(title);
        cdn_server = new Regex((String) entries.get("contentLoc"), "(http://[^<>\"]*)/m/.+").getMatch(0);
        description = (String) entries.get("description");
        xmlsource = (String) entries.get("xml");
        if (title == null || cdn_server == null || xmlsource == null) {
            return null;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        /* Text (no subtitle) and audio is also available. Usually we'll have 10 entries of which 9 get crawled if the user wants all. */
        final String[] xmllist = new Regex(xmlsource, "<bmContentFormat>(.*?)</bmContentFormat>").getColumn(0);
        for (final String xmldata : xmllist) {
            final String audioencodingdata = new Regex(xmldata, "<audioFormat>(.*?)</audioFormat>").getMatch(0);
            final String videoencodingdata = new Regex(xmldata, "<videoFormat>(.*?)</videoFormat>").getMatch(0);

            String ext = getXML(xmldata, "containerFormat");
            final String filesize = getXML(xmldata, "packageSize");
            final String type = getXML(xmldata, "mimeType");
            String url = getXML(xmldata, "file");

            final String audioCodec = getXML(audioencodingdata, "name");
            final String audioBitrate = getXML(audioencodingdata, "bitRate");

            final String videocodec = getXML(videoencodingdata, "name");
            final String videoBitrate = getXML(videoencodingdata, "bitRate");
            final String width = getXML(xmldata, "displayWidth");
            final String height = getXML(xmldata, "displayHeight");
            if (!"Video".equals(type)) {
                /* We don't want audio files */
                continue;
            }

            if (ext == null || filesize == null || url == null || audioCodec == null || audioBitrate == null || videocodec == null || videoBitrate == null || width == null || height == null) {
                return null;
            }
            if (!url.startsWith("origin://m/")) {
                logger.warning("Unexpected url format!");
            }
            /* Finally we have a full (downloadable) http url */
            url = url.replace("origin:/", cdn_server);
            ext = ext.toLowerCase();
            /* Small fix for mpeg formats (usually only 1 available) */
            if (ext.equals("mpg2")) {
                ext = "mpeg2";
            }
            final String videoresolution = width + "x" + height;

            /*
             * formatstring_half is e.g. needed for the mpg2 formats as their resolutions- and bitrates vary while the ones of the other
             * formats are always predictable.
             */
            final String formatstring_site_half = videocodec + "_" + videoresolution + "_" + audioCodec;
            String formatstring_site_full = videocodec + "_";
            formatstring_site_full += videoresolution + "_";
            formatstring_site_full += videoBitrate + "_";
            formatstring_site_full += audioCodec + "_";
            formatstring_site_full += audioBitrate;
            if (formatstring_site_full.endsWith("_")) {
                formatstring_site_full = formatstring_site_full.substring(0, formatstring_site_full.lastIndexOf("_"));
            }

            if ((formats.containsKey(formatstring_site_full) && cfg.getBooleanProperty(formatstring_site_full, true)) || (formats.containsKey(formatstring_site_half) && cfg.getBooleanProperty(formatstring_site_half, true))) {
                final DownloadLink dl = createDownloadlink(decryptedhost + System.currentTimeMillis() + new Random().nextInt(1000000000));
                final String[] realFormatString;
                if (formats.containsKey(formatstring_site_full)) {
                    realFormatString = formats.get(formatstring_site_full);
                } else {
                    realFormatString = formats.get(formatstring_site_half);
                }
                final String filename = title + "_" + getFormatString(realFormatString) + "." + ext;

                try {
                    dl.setContentUrl(parameter);
                    if (description != null) {
                        dl.setComment(description);
                    }
                    dl.setLinkID(vid + filename);
                } catch (final Throwable e) {
                    /* Not available in old 0.9.581 Stable */
                }
                dl._setFilePackage(fp);
                dl.setProperty("format", formatstring_site_full);
                dl.setProperty("format_half", formatstring_site_half);
                dl.setProperty("mainlink", parameter);
                dl.setProperty("directlink", url);
                dl.setProperty("directfilename", filename);
                dl.setFinalFileName(filename);
                /* Currently not needed as we get the filesize from the XML */
                // if (fastLinkcheck) {
                // dl.setAvailable(true);
                // }
                dl.setAvailable(true);
                dl.setDownloadSize(Long.parseLong(filesize));
                decryptedLinks.add(dl);
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

    private String getXML(final String source, final String parameter) {
        return new Regex(source, "<" + parameter + "( type=\"[^<>\"/]*?\")?>([^<>]*?)</" + parameter + ">").getMatch(1);
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