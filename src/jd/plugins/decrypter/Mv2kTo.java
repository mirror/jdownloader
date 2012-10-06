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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "movie2k.to" }, urls = { "http://(www\\.)?movie2k\\.to/([^<>\"/]*?\\-\\d+|\\d+\\-[^<>\"/]*?\\.html)\\.html" }, flags = { 0 })
public class Mv2kTo extends PluginForDecrypt {

    public Mv2kTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * Description of the regexes array: 1= nowvideo.co,streamcloud.com
     * 2=flashx.tv,veervid.com,ginbig
     * .com,vidbux.com,xvidstage.com,xvidstream.net
     * ,flashstream.in,hostingbulk.com ,vreer.com,uploadc.com,allmyvideos
     * .net,vidreel.com,putlocker
     * .com,vureel.com,vidbox.netdeditv.com,watchfreeinhd.com and many others
     * 3=zalaa.com,sockshare.com 4=stream2k.com 5=flashx.tv
     */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        final String fpName = br.getRegex("<title>Watch ([^<>\"]*?) online \\- Watch Movies Online, Full Movies, Download</title>").getMatch(0);
        Browser br2 = br.cloneBrowser();
        final String[][] regexes = { { "width=\"\\d+\" height=\"\\d+\" frameborder=\"0\"( scrolling=\"no\")? src=\"(http://[^<>\"]*?)\"", "1" }, { "<a target=\"_blank\" href=\"(http://[^<>\"]*?)\"", "0" }, { "<IFRAME SRC=\"(http://[^<>\"]*?)\"", "0" }, { "<iframe width=\\d+% height=\\d+px frameborder=\"0\" scrolling=\"no\" src=\"(http://embed\\.stream2k\\.com/[^<>\"]*?)\"", "0" }, { "\"(http://flashx\\.tv/player/embed_player\\.php\\?vid=\\d+)", "0" }, { "\\'(http://(www\\.)?novamov\\.com/embed\\.php\\?v=[^<>\"/]*?)\\'", "0" }, { "\"(http://(www\\.)?video\\.google\\.com/googleplayer\\.swf\\?autoplay=1\\&fs=true\\&fs=true\\&docId=\\d+)", "0" } };
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
                if (finallink != null) decryptedLinks.add(createDownloadlink(finallink));
            }
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
