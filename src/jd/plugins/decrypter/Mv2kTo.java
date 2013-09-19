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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "movie2k.to" }, urls = { "http://(www\\.)?movie(2|4)k\\.to/(?!movies\\-(all|genre)|tvshows\\-season)(tvshows\\-\\d+\\-[^<>\"/]*?\\.html|[^<>\"/]*\\-\\d+|\\d+\\-[^<>\"/]*?)(\\.html)?" }, flags = { 0 })
public class Mv2kTo extends PluginForDecrypt {

    public Mv2kTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String INVALIDLINKS = "http://(www\\.)?movie4k\\.to/[a-z0-9\\-_]+\\-all\\-\\d+\\.html";

    /**
     * Description of the regexes array: 1= nowvideo.co,streamcloud.com 2=flashx.tv,ginbig .com,vidbux.com,xvidstage.com,vidstream.in
     * ,flashstream.in,hostingbulk.com ,vreer.com,uploadc.com,allmyvideos .net,putlocker .com,vureel.com,watchfreeinhd.com and many others
     * 3=zalaa.com,sockshare.com 4=stream2k.com 5=flashx.tv, yesload.net
     */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("movie2k.to/", "movie4k.to/");
        String initalMirror = parameter.substring(parameter.lastIndexOf("/") + 1);
        br.setFollowRedirects(true);
        if (parameter.matches(INVALIDLINKS)) {
            logger.info("Link invalid: " + parameter);
            return decryptedLinks;
        }
        br.getPage(parameter);
        if (br.getURL().endsWith("/error404.php")) {
            logger.info("Invalid URL, or the URL doesn't exist any longer: " + parameter);
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>Watch ([^<>\"]*?) online \\- Watch Movies Online, Full Movies, Download</title>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<title>(.*?) online").getMatch(0);
        Browser br2 = br.cloneBrowser();

        int mirror = 1, part = 1, m = 0;
        String mirrors[] = br.getRegex("<OPTION value=\"([^\"]+)\"").getColumn(0);
        if (mirrors != null && mirrors.length > 1) mirror = mirrors.length;
        String parts[] = br.getRegex("<a href=\"(movie\\.php\\?id=\\d+\\&part=\\d)\">").getColumn(0);
        if (parts != null && parts.length > 1) part = parts.length;

        for (int i = 0; i <= mirror; i++) {
            m++;
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim() + (mirror > 1 ? "@Mirror " + i : "")));
            for (int j = 1; j <= part; j++) {
                final String[][] regexes = { { "width=\"\\d+\" height=\"\\d+\" frameborder=\"0\"( scrolling=\"no\")? src=\"(http://[^<>\"]*?)\"", "1" }, { "<a target=\"_blank\" href=\"((http://)?[^<>\"]*?)\"", "0" }, { "<IFRAME SRC=\"(http://[^<>\"]*?)\"", "0" }, { "<iframe width=\\d+% height=\\d+px frameborder=\"0\" scrolling=\"no\" src=\"(http://embed\\.stream2k\\.com/[^<>\"]*?)\"", "0" }, { "\"(http://flashx\\.tv/player/embed_player\\.php\\?vid=\\d+)", "0" }, { "\\'(http://(www\\.)?novamov\\.com/embed\\.php\\?v=[^<>\"/]*?)\\'", "0" }, { "\"(http://(www\\.)?video\\.google\\.com/googleplayer\\.swf\\?autoplay=1\\&fs=true\\&fs=true\\&docId=\\d+)", "0" }, { "(http://embed\\.yesload\\.net/[\\w\\?]+)", "0" }, { "\"(http://(www\\.)?videoweed\\.es/embed\\.php\\?v=[a-z0-9]+)\"", "0" } };
                for (String[] regex : regexes) {
                    String finallink = br.getRegex(Pattern.compile(regex[0], Pattern.CASE_INSENSITIVE)).getMatch(Integer.parseInt(regex[1]));
                    if (finallink != null) {
                        if (finallink.contains("facebook.com/")) {
                            continue;
                        } else if (finallink.matches("http://embed\\.stream2k\\.com/[^<>\"]+")) {
                            br2.getPage(finallink);
                            finallink = br2.getRegex("file: \\'(http://[^<>\"]*?)\\',").getMatch(0);
                            if (finallink == null) finallink = br2.getRegex("\\'(http://server\\d+\\.stream2k\\.com/dl\\d+/[^<>\"/]*?)\\'").getMatch(0);
                            if (finallink != null) finallink = "directhttp://" + finallink;
                        } else if (finallink.matches("http://flashx\\.tv/player/embed_player\\.php\\?vid=\\d+")) {
                            br2.setFollowRedirects(true);
                            br2.getPage(finallink);
                            finallink = br2.getRegex("\"(http://flashx\\.tv/video/[A-Z0-9]+/)").getMatch(0);
                        }
                        if (finallink != null) {
                            DownloadLink dl = createDownloadlink(finallink);
                            dl.setName(fpName + (mirror > 1 && part == 1 ? "__Mirror_" + m : "") + (part > 1 ? "__Part_" + j : ""));
                            dl.setProperty("MOVIE2K", true);
                            fp.add(dl);
                            if (!finallink.startsWith("directhttp://")) {
                                try {
                                    distribute(dl);
                                } catch (final Throwable e) {
                                    /* does not exist in 09581 */
                                }
                            }
                            decryptedLinks.add(dl);
                        }
                    }
                }
                if (j > 0 && j < parts.length) {
                    String nextPart = parts[j];
                    if (!nextPart.startsWith("/")) nextPart = "/" + nextPart;
                    br.getPage(nextPart);
                    br2 = br.cloneBrowser();
                }
                // No wait = stream2k links may fail
                this.sleep(2 * 1000l, param);
            }
            if (mirrors.length == 0) break;
            if (i < mirrors.length) {
                String next = mirrors[i];
                if (initalMirror.equalsIgnoreCase(next)) i++;
                if (i < mirrors.length) {
                    next = mirrors[i];
                    if (!next.startsWith("http://") || !next.startsWith("https://")) {
                        if (!next.startsWith("/")) next = "/" + next;
                    }
                    br.getPage(next);
                    br2 = br.cloneBrowser();
                    String mirrorParts[] = br.getRegex("<a href=\"(movie\\.php\\?id=\\d+\\&part=\\d)\">").getColumn(0);
                    if (mirrorParts != null && mirrorParts.length > 1) part = mirrorParts.length;
                    if (mirrorParts != null && mirrorParts.length > 0) System.arraycopy(mirrorParts, 0, parts, 0, parts.length);
                }
            }
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}