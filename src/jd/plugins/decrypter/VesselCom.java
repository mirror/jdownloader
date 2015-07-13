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
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vessel.com" }, urls = { "https?://www\\.vessel\\.com/news/videos/[A-Za-z0-9]+" }, flags = { 0 })
public class VesselCom extends PluginForDecrypt {

    @SuppressWarnings("deprecation")
    public VesselCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String DOMAIN         = "vessel.com";
    /* Settings stuff */
    private static final String FAST_LINKCHECK = "FAST_LINKCHECK";

    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        /* Load sister-host plugin and get account */
        final Account aa = AccountController.getInstance().getValidAccount(JDUtilities.getPluginForHost(DOMAIN));
        if (aa == null) {
            logger.info("Cannot decrypt anything without account");
            return decryptedLinks;
        }
        jd.plugins.hoster.VesselCom.login(this.br, aa, false);
        final String parameter = param.toString();
        String vid = null;
        String title = null;
        String description = null;
        String cdn_server = null;
        ArrayList<Object> ressourcelist = null;
        LinkedHashMap<String, Object> entries = null;
        final LinkedHashMap<String, String[]> formats = jd.plugins.hoster.VesselCom.formats;
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
        vid = br.getRegex("\"id\":(\\d+)").getMatch(0);
        if (vid == null) {
            return null;
        }
        br.postPageRaw("https://www.vessel.com/api/view/items/" + vid, "{\"client\":\"web\",\"refresh\":true}");
        entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
        ressourcelist = (ArrayList) jd.plugins.hoster.DummyScriptEnginePlugin.walkJson(entries, "assets/{1}/sources");
        title = (String) entries.get("title");
        title = encodeUnicode(title);
        cdn_server = new Regex((String) entries.get("contentLoc"), "(http://[^<>\"]*)/m/.+").getMatch(0);
        description = (String) entries.get("short_description");
        if (title == null || cdn_server == null) {
            return null;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        for (final Object videoo : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) videoo;
            final String format_name = (String) entries.get("name");
            if (format_name.equals("hls-index")) {
                /* We don't want hls formats */
                continue;
            }
            String url = (String) entries.get("location");

            final String videoBitrate = Long.toString(jd.plugins.hoster.DummyScriptEnginePlugin.toLong(entries.get("bitrate"), -1));
            final String width = Long.toString(jd.plugins.hoster.DummyScriptEnginePlugin.toLong(entries.get("width"), -1));
            final String height = Long.toString(jd.plugins.hoster.DummyScriptEnginePlugin.toLong(entries.get("height"), -1));
            final String videoresolution = width + "x" + height;

            if (cfg.getBooleanProperty(format_name, true)) {
                final DownloadLink dl = createDownloadlink(decryptedhost + System.currentTimeMillis() + new Random().nextInt(1000000000));
                final String[] formatString = formats.get(format_name);
                final String filename = title + "_" + getFormatString(formatString, videoBitrate, videoresolution) + ".mp4";

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
                dl.setProperty("format", format_name);
                dl.setProperty("mainlink", parameter);
                dl.setProperty("directlink", url);
                dl.setProperty("directfilename", filename);
                dl.setFinalFileName(filename);
                if (fastLinkcheck) {
                    dl.setAvailable(true);
                }
                decryptedLinks.add(dl);
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.info(DOMAIN + ": None of the selected formats were found or none were selected, decrypting done...");
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    private String getFormatString(final String[] formatinfo, final String videoBitrate, final String videoResolution) {
        String formatString = "";
        final String videoCodec = formatinfo[0];
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