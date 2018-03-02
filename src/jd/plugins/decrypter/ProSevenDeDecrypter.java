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

import java.text.DecimalFormat;
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

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "prosieben.de", "prosiebenmaxx.de", "the-voice-of-germany.de", "kabeleins.de", "sat1.de", "sat1gold.de", "sixx.de", "7tv.de", "kabeleinsdoku.de" }, urls = { "https?://(?:www\\.)?prosieben\\.(?:de|at|ch)/.+", "https?://(?:www\\.)?prosiebenmaxx\\.(?:de|at|ch)/.+", "https?://(?:www\\.)?the\\-voice\\-of\\-germany\\.(?:de|at|ch)/.+", "https?://(?:www\\.)?kabeleins\\.(?:de|at|ch)/.+", "https?://(?:www\\.)?sat1\\.(?:de|at|ch)/.+", "https?://(?:www\\.)?sat1gold\\.(?:de|at|ch)/.+", "https?://(?:www\\.)?sixx\\.(?:de|at|ch)/.+", "https?://(?:www\\.)?7tv\\.(?:de|at|ch)/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+", "https?://(?:www\\.)?kabeleinsdoku\\.(?:de|at|ch)/.+" })
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
        String date = br.getRegex("property=\"og:published_time\" content=\"([^<>\"]*?)\"").getMatch(0);
        if ("1979-11-30T00:00:00+01:00".equals(date)) {
            /* Avoid serverside wrong date e.g. http://www.7tv.de/circus-halligalli/52-episode-2-staffel-5-ganze-folge */
            date = br.getRegex("property=\"og:modified_time\" content=\"([^<>\"]*?)\"").getMatch(0);
        }
        final String brand = new Regex(parameter, "https?://(?:www\\.)?([^<>\"/]*?)\\.(?:de|at|ch)/").getMatch(0);
        String json = this.br.getRegex("var contentResources?\\s*=\\s*(\\[.+\\]);").getMatch(0);
        if (json == null) {
            json = this.br.getRegex("\"contentResources?\"\\s*:\\s*(\\[.+\\])").getMatch(0);
        }
        String fpName = br.getRegex("itemprop=\"title\"><h1>([^<>\"]*?)</h1>").getMatch(0);
        if (fpName == null) {
            fpName = new Regex(parameter, "([^/]+)/?$").getMatch(0);
        }
        fpName = Encoding.htmlDecode(fpName.trim());
        if (json == null || date == null) {
            /* Probably this is not a video - return nothing! */
            return decryptedLinks;
        }
        final DecimalFormat df = new DecimalFormat("00");
        final String date_formatted = formatDate(date);
        final ArrayList<Object> ressources = (ArrayList) JavaScriptEngineFactory.jsonToJavaObject(json);
        if (ressources == null) {
            return null;
        }
        for (final Object video_o : ressources) {
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) video_o;
            /* E.g. skip invalid ContentType 'live'. */
            final String contentType = (String) entries.get("contentType");
            final String formatName = (String) entries.get("formatName");
            String title = (String) entries.get("title");
            final String videoid = Long.toString(JavaScriptEngineFactory.toLong(entries.get("id"), -1));
            if (contentType == null || title == null || videoid.equals("-1")) {
                continue;
            }
            String filename = date_formatted + "_" + brand + "_" + formatName;
            if (formatName != null && formatName.length() > 0) {
                filename += "_" + formatName;
            }
            /* E.g. http://www.7tv.de/big-brother/161-staffel-1-episode-61-big-brother-tag-60-teil-2-ganze-folge */
            Regex seriesinfo = new Regex(title, "(Staffel (\\d+) Episode (\\d+): )");
            String delete_me = seriesinfo.getMatch(0);
            String seasonnumber_str = seriesinfo.getMatch(1);
            String episodenumber_str = seriesinfo.getMatch(2);
            if (seasonnumber_str == null || episodenumber_str == null || delete_me == null) {
                /* E.g. http://www.7tv.de/circus-halligalli/52-episode-2-staffel-5-ganze-folge */
                seriesinfo = new Regex(title, "(Episode (\\d+) \\- Staffel (\\d+))");
                delete_me = seriesinfo.getMatch(0);
                seasonnumber_str = seriesinfo.getMatch(2);
                episodenumber_str = seriesinfo.getMatch(1);
            }
            if (seasonnumber_str == null || episodenumber_str == null || delete_me == null) {
                /* E.g. http://www.7tv.de/circus-halligalli/62-staffel-6-episode-2-ganze-folge */
                seriesinfo = new Regex(title, "(Staffel (\\d+) \\- Episode (\\d+))");
                delete_me = seriesinfo.getMatch(0);
                seasonnumber_str = seriesinfo.getMatch(1);
                episodenumber_str = seriesinfo.getMatch(2);
            }
            /* Add series information in case we: 1. Found them and 2. Have a FULL episode (check contentType) */
            if (seasonnumber_str != null && episodenumber_str != null && delete_me != null && contentType.equals("full")) {
                filename += "_S" + df.format(Short.parseShort(seasonnumber_str)) + "E" + df.format(Short.parseShort(episodenumber_str));
                title = title.replace(delete_me, "");
                /* Only try this if we already found season- and episodenumber! */
                delete_me = new Regex(title, "(Folge \\d+)").getMatch(0);
                if (delete_me != null) {
                    /* E.g. http://www.7tv.de/mila/131-staffel-1-episode-31-folge-31-ganze-folge */
                    title = title.replace(delete_me, "");
                }
            }
            if (!inValidate(title)) {
                /* In some rare cases title will be "" after we remove season- and episodenumbers from it ... */
                filename += " - " + title;
            }
            /* Even though the data comes from json it might be htmlencoded sometimes - let's fix that! */
            filename = Encoding.htmlDecode(filename);
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

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     */
    protected boolean inValidate(final String s) {
        if (s == null || s.matches("\\s+") || s.equals("")) {
            return true;
        } else {
            return false;
        }
    }
}
