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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "baseshare.com" }, urls = { "http://(www\\.)?baseshare\\.com/[A-Za-z0-9\\-_]+/mixtapes/[A-Za-z0-9\\-_]+/\\d+/" }, flags = { 0 })
public class BaseShareCom extends PluginForDecrypt {

    public BaseShareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getURL().equals("http://baseshare.com/")) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0));
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        final String url_artist = new Regex(parameter, "baseshare\\.com/([A-Za-z0-9\\-_]+)/mixtapes").getMatch(0);
        String artist = br.getRegex("<h1>([^<>]*?)</h1>").getMatch(0);
        String title = br.getRegex("<h2>([^<>]*?)</h2>").getMatch(0);
        String fpName = null;
        if (artist != null && title != null) {
            artist = encodeUnicode(Encoding.htmlDecode(artist).trim());
            title = encodeUnicode(Encoding.htmlDecode(title).trim());
            fpName = artist + " - " + title;
        }
        final String jstext = br.getRegex("<div id=\"content\">[\t\n\r ]+<script>(.*?)</script>").getMatch(0);
        final String[] links = jstext.split("function ");
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            final String[][] linkinfo = new Regex(singleLink, "updateSong\\(\\'(http://baseshare\\.com/uploads[^<>\"]*?\\.mp3)\\', \\'/uploads/waves/[a-z0-9]+\\.png\\', \\'([^<>\"]*?)\\', \\'([^<>\"]*?)\\', (\\d+)\\);").getMatches();
            if (linkinfo != null && linkinfo.length == 1) {
                final String thisurl = linkinfo[0][0];
                final String thisartist = linkinfo[0][1];
                final String thistitle = linkinfo[0][2];
                final String thisid = linkinfo[0][3];
                final String thisartisturl = thisartist.replaceAll("(\\-|\\.|_)", "").replace(" ", "-");
                final String thistitleurl = thistitle.replaceAll("(\\-|\\.|_)", "").replace(" ", "-");
                final String songurl = "http://baseshare.com/" + url_artist + "/songs/" + thisartisturl + "-" + thistitleurl + "/" + thisid + "/";
                final DownloadLink dl = createDownloadlink(songurl);
                dl.setProperty("directlink", thisurl);
                dl.setName(thisartist + " - " + thistitle + ".mp3");
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
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

}
