//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fizy.com" }, urls = { "http://\\w+\\.fizy\\.com/p/[0-9a-z]+" }, flags = { 0 })
public class FizyComD extends PluginForDecrypt {

    public FizyComD(final PluginWrapper wrapper) {
        super(wrapper);
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

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl();

        br.getPage(parameter);
        String fpName = br.getRegex("<h2>(.*?)</h2>").getMatch(0);
        fpName = fpName == null ? new Regex(parameter, "http://(.*?)\\..+").getMatch(0) : fpName;
        fpName = fpName == null ? "Fizy.com Playlist" : fpName;

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);

        final ArrayList<String> allSongs = new ArrayList<String>();
        for (final String song : br.getRegex("<a class=\"song\" name=\"\\w+\" href=\"(.*?)\">\\[\\d+:\\d+\\] .*?</a>").getColumn(0)) {
            allSongs.add(song);
        }

        final Iterator<String> link = allSongs.iterator();
        while (link.hasNext()) {
            final DownloadLink dl = getFinalLink(link.next());
            if (dl == null) {
                continue;
            }
            fp.add(dl);
            try {
                distribute(dl);
            } catch (final Throwable e) {
                /* does not exist in 09581 */
            }
            decryptedLinks.add(dl);
        }

        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    private DownloadLink getFinalLink(final String link) throws IOException {
        final String sid = link.substring(link.lastIndexOf("/") + 1);
        br.postPage("http://fizy.com/fizy::getSong", "SID=" + sid);
        final String filename = br.getRegex("title\":\"(.*?)\"").getMatch(0).trim();
        final String duration = br.getRegex("duration\":\"(\\d+)\"").getMatch(0);
        final String providerId = br.getRegex("providerNumber\":\"(\\d+)\"").getMatch(0);
        String ext = br.getRegex("type\":\"(.*?)\"").getMatch(0);
        String clipUrl = br.getRegex("source\":\"(.*?)\"").getMatch(0);
        if (providerId == null) { return null; }

        switch (Integer.parseInt(providerId)) {
        case 1:
            // youtube
            if (!clipUrl.startsWith("http")) {
                clipUrl = "http://www.youtube.com/watch?v=" + clipUrl;
            }
            break;
        case 3:
            // http
            clipUrl = clipUrl.replace("\\", "");
            break;
        case 4:
            // rtmp
            clipUrl = link;
            break;
        case 6:
            // rtmp
            clipUrl = link;
            break;
        case 9:
            // http
            clipUrl = clipUrl.replace("\\", "");
            break;
        case 10:
            // grooveshark direkt stream links
            clipUrl = clipUrl.replace("\\", "");
            break;
        default:
            logger.warning("ProviderId: " + providerId + " --> Link: " + clipUrl + "not supported!");
            clipUrl = null;
            break;
        }
        if (clipUrl == null || filename == null) { return null; }
        ext = ext == null ? "m4a" : ext;
        final DownloadLink tmp = createDownloadlink(clipUrl);
        tmp.setFinalFileName(Encoding.htmlDecode(decodeUnicode(filename)) + "." + ext);
        if (providerId.equals("4") || providerId.equals("6")) {
            tmp.setProperty("isRTMP", true);
        }
        if (duration != null) {
            tmp.setDownloadSize(Integer.parseInt(duration) * 16 * 1024);
        }
        return tmp;
    }

}