//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fizy.com" }, urls = { "http://(((?!www).*)\\.)?fizy\\.com/?(#?(s|p|u)/(s/)?\\w+)?" }, flags = { 0 })
public class FizyComD extends PluginForDecrypt {

    private String  CLIPDATA;

    private boolean YT = true;

    public FizyComD(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private int[] changeReturnTypeToIntArray(String x, String[] y) {
        if (x == null) return null;
        String[] tmp = x.split("\r\n");
        int[] indices = new int[tmp.length];
        int j = 0;
        for (int i = 0; i < y.length; i++) {
            if (tmp[j].equals(y[i])) {
                indices[j] = i;
                if (j >= tmp.length - 1) break;
                j++;
            }
        }
        return indices;
    }

    private String decodeUnicode(final String s) {
        final Pattern p = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        String res = s;
        final Matcher m = p.matcher(res);
        while (m.find()) {
            res = res.replaceAll("\\" + m.group(0), Character.toString((char) Integer.parseInt(m.group(1), 16)));
        }
        return res;
    }

    private int[] letTheUserSelectPlayLists(String[][] pL) {
        int count = 0;
        int[] indices = null;
        StringBuilder sb = new StringBuilder();
        for (String[] s : pL) {
            count = new Regex(s[0], "order").count();
            if (count == 0) continue;
            sb.append(Encoding.htmlDecode(decodeUnicode(s[1].trim())) + " (" + String.valueOf(count) + ")");
            sb.append("\r\n");
        }

        String[] mirrors = sb.toString().split("\r\n");

        try {
            indices = UserIO.getInstance().requestMultiSelectionDialog(0, "Please select playlist", "Please select the desired playlist.", mirrors, null, null, null, null);
        } catch (Throwable e) {
            /* this function DOES NOT exist in 09581 stable */
            // TODO Get rid of this catch section once
            // MultiSelectionDialog
            // makes its way into stable
            String index = UserIO.getInstance().requestInputDialog(UserIO.STYLE_LARGE | UserIO.NO_COUNTDOWN, "Please remove unwanted playlists", sb.toString());
            indices = changeReturnTypeToIntArray(index, mirrors);
        }
        // Dialog wurde abgebrochen, Decrypterinstanz wird beendet.
        if (indices == null) indices = new int[] { -1 };
        return indices;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl();
        if (new Regex(parameter, Pattern.compile("http://fizy\\.com/?", Pattern.CASE_INSENSITIVE)).matches()) {
            logger.info("Mainpage added -> Invalid link");
            return decryptedLinks;
        }

        // processing plugin configuration
        final SubConfiguration cfg = SubConfiguration.getConfig("fizy.com");
        YT = cfg.getBooleanProperty("INCLUDING_YT", true);

        if (!parameter.matches("http://(www\\.)?fizy\\.com/(#?s/)?\\w+")) {
            String user = new Regex(parameter, "http://(.*?)\\.fizy\\.com").getMatch(0);
            user = user == null ? new Regex(parameter, "fizy\\.com/#u/(\\w+)").getMatch(0) : user;
            if (user == null) return null;
            String playLists = null;

            try {
                playLists = br.postPage("http://fizy.com/fizy::getProfile", "username=" + user);
            } catch (Throwable e) {
                logger.severe("Server error! Message: " + e);
                return null;
            }

            /*
             * TODO: parsing content(JSON) after 0.9xx through org.codehaus.jackson.JsonNode class
             */
            playLists = playLists.replaceAll(":\\s", ":");
            playLists = playLists.replaceAll(",\\s\"", ",\"");
            playLists = playLists.replaceAll("\"(is_shuffle_on|style|itemstyle|itemhoverstyle)\":.*?,", "");

            final HashMap<String, String> ret = new HashMap<String, String>();

            // alle Playlisten des Users
            String[][] playListsTmp = new Regex(playLists, "\"playlist\":\\[\\{\"title\":\"(.+?)\",\"order\":\\d\\.\\d,\"songs\":\\[(.+?)\\]\\},|\\{\"songs\":\\[(.+?)\\],\"order\":\\d+,\"title\":\"(.+?)\"\\}").getMatches();
            if (playListsTmp == null || playListsTmp.length == 0) return null;

            String[][] allPlayLists = new String[playListsTmp.length][2];
            for (int i = 0; i < playListsTmp.length; i++) {
                int z = 0;
                for (int j = 0; j < playListsTmp[i].length; j++) {
                    if (playListsTmp[i][j] == null) continue;
                    allPlayLists[i][z++] = playListsTmp[i][j];
                }
                // reverse Array
                if (!allPlayLists[i][0].startsWith("{\"title\":")) {
                    List<String> t = new LinkedList<String>(Arrays.asList(allPlayLists[i]));
                    Collections.reverse(t);
                    allPlayLists[i] = t.toArray(new String[0]);
                }
            }
            if (allPlayLists == null || allPlayLists.length == 0) return null;
            // selektiere bestimmte Playlisten
            int[] selectedPlayListIndices = null;
            if (allPlayLists.length > 1) {
                selectedPlayListIndices = letTheUserSelectPlayLists(allPlayLists);
                if (selectedPlayListIndices[0] == -1) {
                    logger.info("Aborted by user!");
                    return decryptedLinks;
                }
            }

            int i = 0, j = 0;
            for (final String[] playList : allPlayLists) {
                if (selectedPlayListIndices != null) {
                    if (j > selectedPlayListIndices.length - 1) break;
                    if (i != selectedPlayListIndices[j]) {
                        i++;
                        continue;
                    }
                    i++;
                    j++;
                }
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(user.toUpperCase(Locale.ENGLISH) + "s_Playlist__" + Encoding.htmlDecode(decodeUnicode(playList[1].trim())));
                // alle Titel in der Playlist
                for (String[] song : new Regex(playList[0], "\\{(.*?)\\}").getMatches()) {
                    for (String[] s : new Regex(song[0], "\"(.*?)\":\"?(.*?)\"?,").getMatches()) {
                        ret.put(s[0], s[1]);
                    }

                    final DownloadLink dlLink = getFinalLink("http://fizy.com/#s/" + ret.get("ID"));
                    // for (final Entry<String, String> next : ret.entrySet()) {
                    // dlLink.setProperty(next.getKey(), next.getValue());
                    // }
                    // final String filename = dlLink.getProperty("title",
                    // "UnknownTitle") + "." + dlLink.getProperty("type",
                    // "mp3");
                    // dlLink.setName(decodeUnicode(filename.trim()));
                    // try {
                    // dlLink.setDownloadSize(Integer.parseInt(dlLink.getStringProperty("duration"))
                    // * 16 * 1024);
                    // } catch (final Exception e) {
                    // }
                    if (dlLink == null) continue;
                    fp.add(dlLink);
                    try {
                        distribute(dlLink);
                    } catch (final Throwable e) {
                        /* does not exist in 09581 */
                    }
                    progress.increase(1);
                    decryptedLinks.add(dlLink);
                }
            }

        } else {
            decryptedLinks.add(getFinalLink(parameter));
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    private DownloadLink getFinalLink(final String link) throws IOException {
        final String sid = link.substring(link.lastIndexOf("/") + 1);
        try {
            CLIPDATA = br.postPage("http://fizy.com/fizy::getSong", "SID=" + sid);
        } catch (Throwable e) {
            logger.severe("Server error! Message: " + e);
            return null;
        }

        String error = getClipData("error");
        if (error != null) {
            logger.warning("fizy.com response error for provider \"" + error + "\"! Link: " + link);
            return null;
        }

        final String filename = getClipData("title");
        final String duration = getClipData("duration");
        final String providerId = getClipData("providerNumber");
        String ext = getClipData("type");
        String clipUrl = getClipData("source");
        if (providerId == null) { return null; }
        int pId = providerId.matches("\\d+") ? Integer.parseInt(providerId) : -1;

        switch (pId) {
        case 1:
            // youtube
            if (!YT) return null;
            if (!clipUrl.startsWith("http")) {
                clipUrl = "http://www.youtube.com/watch?v=" + clipUrl;
            }
            break;
        case 2:
            // daylimotion
            break;
        case 3:
            // wrzuta.pl
            break;
        case 4:
            // mu-yap
            break;
        case 6:
            // http
            break;
        case 7:
            // metacafe
            break;
        case 8:
            // soundcloud
            break;
        case 9:
            // http
            break;
        case 10:
            // grooveshark direkt stream links
            break;
        case 11:
            // http
            break;
        default:
            logger.info("New providerId: " + providerId + " --> Link: " + clipUrl + " !");
            break;
        }
        if (clipUrl == null || filename == null) { return null; }
        clipUrl = clipUrl.startsWith("rtmp") ? "rtmp" + link : clipUrl;
        clipUrl = clipUrl.replace("\\", "");
        ext = ext == null ? "mp3" : ext;
        ext = "audio".equals(ext) ? "mp3" : ext;
        ext = "video".equals(ext) ? "mp4" : ext;
        if (pId != 1) {
            clipUrl = "directhttp://" + clipUrl;
        }
        final DownloadLink tmp = createDownloadlink(clipUrl);
        tmp.setFinalFileName(Encoding.htmlDecode(decodeUnicode(filename.trim())) + "." + ext);
        if (duration != null) {
            tmp.setDownloadSize(SizeFormatter.getSize(duration) * 16 * 1024);
        }
        return tmp;
    }

    private String getClipData(final String tag) {
        return new Regex(CLIPDATA, "\"" + tag + "\"\\s?:\\s?\"?([^\"]+)\"?(,|\\})").getMatch(0);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}