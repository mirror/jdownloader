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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "dmax.de" }, urls = { "https?://(www\\.)?(dmax|tlc|animalplanet|discovery)\\.de/.+" })
public class DmaxDeDecrypter extends PluginForDecrypt {
    public DmaxDeDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Tags: Discovery Communications Inc */
    private static final String type_videoid    = "https?://.+/#\\d+$";
    private static final String DOMAIN          = "dmax.de";
    private String              apiTokenCurrent = null;

    /* Settings stuff */
    // private static final String FAST_LINKCHECK = "FAST_LINKCHECK";
    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        /* Load sister-host plugin */
        JDUtilities.getPluginForHost("dmax.de");
        final String parameter = param.toString();
        final String vid = new Regex(parameter, "(\\d+)$").getMatch(0);
        LinkedHashMap<String, Object> entries = null;
        ArrayList<Object> ressourcelist = null;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final LinkedHashMap<String, String[]> formats = jd.plugins.hoster.DmaxDe.formats;
        final String nicehost = new Regex(parameter, "https?://(?:www\\.)?([^/]+)").getMatch(0);
        final String nicehost_nicer = new Regex(parameter, "https=://(?:www\\.)?([^/]+)\\.de/").getMatch(0);
        final String decryptedhost = "http://" + nicehost + "decrypted";
        final SubConfiguration cfg = SubConfiguration.getConfig(DOMAIN);
        this.br.setFollowRedirects(false);
        if (parameter.matches(type_videoid)) {
            initAPI(nicehost);
            accessAPI("find_video_by_id", "video_id=" + vid + "&video_fields=name,renditions");
            if (br.getHttpConnection().getResponseCode() == 404 || br.toString().equals("null")) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            if (entries.get("error") != null) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            String date_formatted = "-";
            String title = (String) entries.get("name");
            /*
             * Most times we will have 10 entries available - sometimes less and sometimes also less http-urls but usually at least 2 of 4
             * http urls are available.
             */
            ressourcelist = (ArrayList) entries.get("renditions");
            if (title == null || ressourcelist == null) {
                return null;
            }
            title = encodeUnicode(title);
            FilePackage fp = null;
            for (final Object o : ressourcelist) {
                final LinkedHashMap<String, Object> vdata = (LinkedHashMap<String, Object>) o;
                String directlink = (String) vdata.get("url");
                final Object osize = vdata.get("size");
                final Object owidth = vdata.get("frameWidth");
                final Object oheight = vdata.get("frameHeight");
                final Object odate = vdata.get("uploadTimestampMillis");
                if (odate != null) {
                    date_formatted = formatDate(JavaScriptEngineFactory.toLong(odate, -1));
                }
                String width = null;
                String height = null;
                if (owidth != null && oheight != null) {
                    width = Long.toString(JavaScriptEngineFactory.toLong(owidth, -1));
                    height = Long.toString(JavaScriptEngineFactory.toLong(oheight, -1));
                }
                final boolean audioOnly = ((Boolean) vdata.get("audioOnly")).booleanValue();
                if (audioOnly) {
                    /* Should never happen */
                    logger.info("Skipping link because it is audio only");
                    continue;
                }
                final String vpath = new Regex(directlink, "(/byocdn/media/.+)").getMatch(0);
                if (vpath != null) {
                    directlink = "http://discoveryint1.edgeboss.net/download/discoveryint1/" + vpath;
                }
                if (fp == null) {
                    fp = FilePackage.getInstance();
                    fp.setName(date_formatted + "_" + nicehost_nicer + "_" + title);
                }
                if (width != null && height != null && osize != null && formats.containsKey(width) && cfg.getBooleanProperty(width, true)) {
                    final long filesize = JavaScriptEngineFactory.toLong(osize, -1);
                    final DownloadLink dl = createDownloadlink(decryptedhost + System.currentTimeMillis() + new Random().nextInt(1000000000));
                    final String[] vidinfo = formats.get(width);
                    String filename = date_formatted + "_" + nicehost_nicer + "_" + title + "_" + getFormatString(vidinfo, width + "x" + height);
                    filename += ".mp4";
                    try {
                        dl.setContentUrl(parameter);
                        dl.setLinkID(vid + filename);
                    } catch (final Throwable e) {
                        /* Not available in old 0.9.581 Stable */
                    }
                    dl._setFilePackage(fp);
                    dl.setProperty("format", width);
                    dl.setProperty("mainlink", parameter);
                    dl.setProperty("directlink", directlink);
                    dl.setProperty("directfilename", filename);
                    dl.setProperty("directsize", filesize);
                    dl.setProperty("videoid", vid);
                    dl.setDownloadSize(filesize);
                    dl.setFinalFileName(filename);
                    dl.setAvailable(true);
                    // if (fastLinkcheck) {
                    // dl.setAvailable(true);
                    // }
                    decryptedLinks.add(dl);
                } else if (width == null && height == null && osize == null) {
                    logger.info("Found single undefined quality --> Decrypting that");
                    /* Unknown format (usually only 1 quality available then --> Decrypt it regardless of the users' settings. */
                    final DownloadLink dl = createDownloadlink(decryptedhost + System.currentTimeMillis() + new Random().nextInt(1000000000));
                    final String filename = date_formatted + "_" + nicehost_nicer + "_" + title + ".mp4";
                    try {
                        dl.setContentUrl(parameter);
                        dl.setLinkID(vid);
                    } catch (final Throwable e) {
                        /* Not available in old 0.9.581 Stable */
                    }
                    dl._setFilePackage(fp);
                    dl.setProperty("mainlink", parameter);
                    dl.setProperty("directlink", directlink);
                    dl.setProperty("directfilename", filename);
                    dl.setProperty("videoid", vid);
                    dl.setFinalFileName(filename);
                    // if (fastLinkcheck) {
                    // dl.setAvailable(true);
                    // }
                    decryptedLinks.add(dl);
                    break;
                } else {
                    logger.warning("WTF");
                }
            }
        } else {
            /* Playlist with videoids --> Decrypt --> Goes back into the decrypter */
            br.getPage(parameter);
            if (!br.containsHTML("name=\"playerID\"")) {
                /* Whatever the user added - it is not a video. */
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            String[] videoids = null;
            final String playlist_html = br.getRegex("<ol class=\"numbers\">(.*?)</ol>").getMatch(0);
            final String single_video_id = br.getRegex("name=\"@videoPlayer\" value=\"(\\d+)\"").getMatch(0);
            if (playlist_html != null) {
                videoids = br.getRegex("data\\-guid=\"(\\d+)\"").getColumn(0);
            }
            if ((videoids == null || videoids.length == 0) && single_video_id == null) {
                return null;
            }
            if (videoids != null && videoids.length > 0) {
                /* Playlist / oner video split into multiple parts */
                for (final String videoid : videoids) {
                    final DownloadLink dl = createDownloadlink("http://" + nicehost + "/#" + videoid);
                    decryptedLinks.add(dl);
                }
            } else {
                /* Single video */
                final DownloadLink dl = createDownloadlink("http://" + nicehost + "/#" + single_video_id);
                decryptedLinks.add(dl);
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.info(DOMAIN + ": None of the selected formats were found or none were selected, decrypting done...");
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    private String getFormatString(final String[] formatinfo, final String videoResolution) {
        String formatString = "";
        final String videoCodec = formatinfo[0];
        final String videoBitrate = formatinfo[1];
        // final String videoResolution = formatinfo[2];
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

    private void initAPI(final String host) throws PluginException {
        if (host.equals("dmax.de")) {
            this.apiTokenCurrent = jd.plugins.hoster.DmaxDe.apiTokenDmax;
        } else if (host.equals("discovery.de")) {
            this.apiTokenCurrent = jd.plugins.hoster.DmaxDe.apiTokenDiscoveryDe;
        } else if (host.equalsIgnoreCase("tlc.de")) {
            this.apiTokenCurrent = jd.plugins.hoster.DmaxDe.apiTokenTlc;
        } else if (host.equals("animalplanet.de")) {
            this.apiTokenCurrent = jd.plugins.hoster.DmaxDe.apiTokenAnimalplanetDe;
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.br.getHeaders().put("User-Agent", "stagefright/1.2 (Linux;Android 4.4.2)");
    }

    private void accessAPI(final String command, final String params) throws IOException {
        // /* Request to get 'officially'-downloadable URLs (but lower quality) */
        // br.getPage("https://api.brightcove.com/services/library?command=find_video_by_id&video_fields=name%2CFLVURL%2CreferenceId%2CitemState%2Cid&media_delivery=http&video_id=2827406067001&token=XoVA15ecuocTY5wBbxNImXVFbQd72epyxxVcH3ZVmOA.");
        final String url = "https://api.brightcove.com/services/library?token=" + this.apiTokenCurrent + "&command=" + command + "&" + params;
        this.br.getPage(url);
    }

    private String formatDate(final long input) {
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        Date theDate = new Date(input);
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            formattedDate = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent input error killing plugin */
            formattedDate = Long.toString(input);
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