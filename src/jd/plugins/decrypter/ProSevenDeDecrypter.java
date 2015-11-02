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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DummyScriptEnginePlugin;

import org.appwork.utils.formatter.TimeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "prosieben.de", "prosiebenmaxx.de", "the-voice-of-germany.de", "kabeleins.de", "sat1.de", "sat1gold.de", "sixx.de", "7tv.de" }, urls = { "http://(?:www\\.)?prosieben\\.de/tv/[\\w\\-]+/video.+", "http://www\\.prosiebenmaxx\\.de/[^<>\"\\']*?video.+", "http://(?:www\\.)?the\\-voice\\-of\\-germany\\.de/video.+", "http://(?:www\\.)?kabeleins\\.de/tv/[\\w\\-]+/video.+", "http://(?:www\\.)?sat1\\.de/tv/[\\w\\-]+/video.+", "http://(?:www\\.)?sat1gold\\.de/tv/[\\w\\-]+/video.+", "http://(?:www\\.)?sixx\\.de/tv/.+", "http://(?:www\\.)?7tv\\.de/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+" }, flags = { 32, 32, 32, 32, 32, 32, 32, 32 })
public class ProSevenDeDecrypter extends PluginForDecrypt {

    public ProSevenDeDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        this.br.setFollowRedirects(true);
        br.getPage(parameter);
        if (jd.plugins.hoster.ProSevenDe.isOffline(this.br)) {
            /* Page offline */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String date = br.getRegex("property=\"og:published_time\" content=\"([^<>\"]*?)\"").getMatch(0);
        final String brand = new Regex(parameter, "https?://(?:www\\.)?([^<>\"/]*?)\\.de/").getMatch(0);
        final String json = this.br.getRegex("var contentResources = (\\[.+\\]);").getMatch(0);
        String fpName = br.getRegex("itemprop=\"title\"><h1>([^<>\"]*?)</h1>").getMatch(0);
        if (fpName == null) {
            fpName = new Regex(parameter, "([^/]+)$").getMatch(0);
        }
        fpName = Encoding.htmlDecode(fpName.trim());
        if (json == null || date == null) {
            /* Probably this is not a video */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String date_formatted = formatDate(date);
        final ArrayList<Object> ressources = (ArrayList) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(json);
        if (ressources == null) {
            return null;
        }

        for (final Object video_o : ressources) {
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) video_o;
            final String formatName = (String) entries.get("formatName");
            final String title = (String) entries.get("title");
            final String videoid = Long.toString(DummyScriptEnginePlugin.toLong(entries.get("id"), -1));
            if (formatName == null || title == null || videoid.equals("-1")) {
                return null;
            }
            String filename = date_formatted + "_" + brand + "_" + formatName + " - " + title;
            filename = encodeUnicode(filename) + ".mp4";
            final DownloadLink dl = this.createDownloadlink("http://7tvdecrypted.de/" + videoid);
            dl.setProperty("decrypter_filename", filename);
            dl.setProperty("mainlink", parameter);
            dl.setAvailable(true);
            dl.setFinalFileName(filename);
            dl.setContentUrl(parameter);
            decryptedLinks.add(dl);
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    private String formatDate(String input) {
        /* 2015-06-23T20:15:00.000+02:00 --> 2015-06-23T20:15:00.000+0200 */
        input = input.substring(0, input.lastIndexOf(":")) + "00";
        final long date = TimeFormatter.getMilliSeconds(input, "yyyy-MM-dd'T'HH:mm:ssZ", Locale.GERMAN);
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

    /** Avoid chars which are not allowed in filenames under certain OS' */
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

}
