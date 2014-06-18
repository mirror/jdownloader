//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.HashMap;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.http.Browser.BrowserException;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "br.de" }, urls = { "http://(www\\.)?br\\.de/mediathek/video/(sendungen/[A-Za-z0-9\\-_]+/)?[A-Za-z0-9\\-_]+\\.html" }, flags = { 32 })
public class BrOnlineDeDecrypter extends PluginForDecrypt {

    private static final String Q_0          = "Q_0";
    private static final String Q_A          = "Q_A";
    private static final String Q_B          = "Q_B";
    private static final String Q_E          = "Q_E";
    private static final String Q_C          = "Q_C";
    private static final String Q_BEST       = "Q_BEST";
    private static final String Q_SUBTITLES  = "Q_SUBTITLES";

    private static final String TYPE_INVALID = "http://(www\\.)?br\\.de/mediathek/video/index\\.html";

    public BrOnlineDeDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        boolean offline = false;
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");

        try {
            br.getPage(parameter);
        } catch (final BrowserException e) {
            offline = true;
        }
        if (offline || br.getHttpConnection().getResponseCode() == 404 || parameter.matches(TYPE_INVALID)) {
            /* Add offline link so user can see it */
            final DownloadLink dl = createDownloadlink("http://brdecrypted-online.de/?format=mp4&quality=1x1&hash=" + JDHash.getMD5(parameter));
            dl.setAvailable(false);
            dl.setProperty("offline", true);
            String offline_name = new Regex(parameter, "br\\.de/(.+)\\.html").getMatch(0);
            if (offline_name == null) {
                offline_name = new Regex(parameter, "").getMatch(0);
            }
            dl.setFinalFileName(offline_name);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }

        ArrayList<DownloadLink> newRet = new ArrayList<DownloadLink>();
        HashMap<String, DownloadLink> best_map = new HashMap<String, DownloadLink>();
        final SubConfiguration cfg = SubConfiguration.getConfig("br-online.de");
        final boolean grab_subtitle = cfg.getBooleanProperty(Q_SUBTITLES, false);

        String player_link = br.getRegex("\\{dataURL:\\'(/mediathek/video/[^<>\"]*?)\\'\\}").getMatch(0);
        if (player_link == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        br.getPage("http://www.br.de" + player_link);
        String plain_name = this.getXML("shareTitle", br.toString());
        final String[] qualities = br.getRegex("<asset type=(.*?)</asset>").getColumn(0);
        if (qualities == null || qualities.length == 0 || plain_name == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        plain_name = encodeUnicode(Encoding.htmlDecode(plain_name).trim()).replace("\n", "");

        String subtitle_url = br.getRegex("<dataTimedText url=\"(/mediathek/video/untertitel[^<>\"/]+\\.xml)\"").getMatch(0);
        if (subtitle_url != null) {
            subtitle_url = "http://www.br.de" + Encoding.htmlDecode(subtitle_url);
        }

        for (final String qinfo : qualities) {
            final String final_url = this.getXML("downloadUrl", qinfo);
            /* Avoid HDS */
            if (final_url == null) {
                continue;
            }
            final String q_string = new Regex(final_url, "_(0|A|B|C|D|E)\\.mp4").getMatch(0);
            final String width = this.getXML("frameWidth", qinfo);
            final String height = this.getXML("frameHeight", qinfo);
            final String fsize = this.getXML("size", qinfo);
            final String resolution = width + "x" + height;
            final String final_video_name = plain_name + "_" + resolution + ".mp4";
            final DownloadLink dl_video = createDownloadlink("http://brdecrypted-online.de/?format=mp4&quality=" + resolution + "&hash=" + JDHash.getMD5(parameter));
            dl_video.setProperty("mainlink", parameter);
            dl_video.setProperty("direct_link", final_url);
            dl_video.setProperty("plain_filename", final_video_name);
            dl_video.setProperty("plain_resolution", resolution);
            dl_video.setFinalFileName(final_video_name);
            dl_video.setDownloadSize(Long.parseLong(fsize));
            dl_video.setAvailable(true);
            best_map.put(q_string, dl_video);
            newRet.add(dl_video);
        }

        if (newRet.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        ArrayList<String> selected_qualities = new ArrayList<String>();
        if (newRet.size() > 1 && cfg.getBooleanProperty(Q_BEST, false)) {
            /* only keep best quality */
            final String[] best_list = { "C", "E", "B", "A", "0" };
            for (final String current_quality : best_list) {
                final DownloadLink keep = best_map.get(current_quality);
                if (keep != null) {
                    selected_qualities.add(current_quality);
                    break;
                }
            }
        } else {
            boolean grab_0 = cfg.getBooleanProperty(Q_0, false);
            boolean grab_A = cfg.getBooleanProperty(Q_A, false);
            boolean grab_B = cfg.getBooleanProperty(Q_B, false);
            boolean grab_C = cfg.getBooleanProperty(Q_C, false);
            boolean grab_E = cfg.getBooleanProperty(Q_E, false);
            /* User deselected all --> Add all */
            if (!grab_0 && !grab_A && !grab_B && !grab_C && !grab_E) {
                grab_0 = true;
                grab_A = true;
                grab_B = true;
                grab_C = true;
                grab_E = true;
            }

            if (grab_0) {
                selected_qualities.add("0");
            }
            if (grab_A) {
                selected_qualities.add("A");
            }
            if (grab_B) {
                selected_qualities.add("B");
            }
            if (grab_C) {
                selected_qualities.add("C");
            }
            if (grab_E) {
                selected_qualities.add("E");
            }
        }
        for (final String selected_quality : selected_qualities) {
            final DownloadLink keep = best_map.get(selected_quality);
            if (keep != null) {
                /* Add subtitle link for every quality so players will automatically find it */
                if (grab_subtitle && subtitle_url != null) {
                    final String subtitle_filename = plain_name + "_" + keep.getStringProperty("plain_resolution", null) + ".xml";
                    final String resolution = keep.getStringProperty("plain_resolution", null);
                    final DownloadLink dl_subtitle = createDownloadlink("http://brdecrypted-online.de/?format=xml&quality=" + resolution + "&hash=" + JDHash.getMD5(parameter));
                    dl_subtitle.setProperty("mainlink", parameter);
                    dl_subtitle.setProperty("direct_link", subtitle_url);
                    dl_subtitle.setProperty("plain_filename", subtitle_filename);
                    dl_subtitle.setProperty("streamingType", "subtitle");
                    dl_subtitle.setAvailable(true);
                    dl_subtitle.setFinalFileName(subtitle_filename);
                    decryptedLinks.add(dl_subtitle);
                }
                decryptedLinks.add(keep);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(plain_name);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

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

    private String getXML(final String parameter, final String source) {
        return new Regex(source, "<" + parameter + "( type=\"[^<>\"/]*?\")?>([^<>]*?)</" + parameter + ">").getMatch(1);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}