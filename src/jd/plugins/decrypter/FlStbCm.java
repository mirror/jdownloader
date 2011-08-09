//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filestube.com" }, urls = { "http://[\\w\\.]*?filestube\\.com/(?!source).+\\.html" }, flags = { 0 })
public class FlStbCm extends PluginForDecrypt {

    public FlStbCm(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        FilePackage fp = FilePackage.getInstance();
        br.getPage(parameter.toString());
        if (br.containsHTML("(Requested file was not found|Error 404 -)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        if (parameter.toString().contains("/go.html")) {
            String finallink = br.getRegex("<noframes> <br /> <a href=\"(.*?)\"").getMatch(0);
            if (finallink == null) finallink = br.getRegex("<iframe style=\".*?\" src=\"(.*?)\"").getMatch(0);
            if (finallink == null) return null;
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            String fpName = br.getRegex("<title>(.*?)- Download").getMatch(0);
            // Hmm this plugin should always have a name with that mass of
            // alternative ways to get the name
            if (fpName == null) {
                fpName = br.getRegex("content=\"Download(.*?)from").getMatch(0);
                if (fpName == null) {
                    fpName = br.getRegex("\">Download:(.*?)</h2>").getMatch(0);
                    if (fpName == null) {
                        fpName = br.getRegex("widgetTitle: '(.*?)',").getMatch(0);
                        if (fpName == null) {
                            fpName = br.getRegex("&quot;\\](.*?)\\[/url\\]\"").getMatch(0);
                        }
                    }
                }
            }
            String alternatives = br.getRegex("Alternatives\\('(http:.*?)'").getMatch(0);
            String alterID = br.getRegex("getAlternatives\\('(.*?)'").getMatch(0);
            String alterID2 = br.getRegex("getAlternatives\\('.*?', '(.*?)'").getMatch(0);
            String pagePiece = br.getRegex(Pattern.compile("id=\"copy_paste_links\" style=\".*?\">(.*?)</pre>", Pattern.DOTALL)).getMatch(0);
            if (pagePiece == null) return null;
            String temp[] = pagePiece.split("\r\n");
            if (temp == null) return null;
            if (temp == null || temp.length == 0) return null;
            for (String data : temp)
                decryptedLinks.add(createDownloadlink(data));
            if (alternatives != null && alterID != null && alterID2 != null) {
                Browser br2 = br.cloneBrowser();
                br2.getPage(alternatives + "/get/" + alterID + "/" + alterID2 + "?callback=jsonp" + System.currentTimeMillis());
                String alts[] = br2.getRegex("'t':'(.*?)'").getColumn(0);
                if (alts != null) {
                    for (String link : alts) {
                        Browser br3 = br.cloneBrowser();
                        br3.getPage("http://www.filestube.com/" + link + "/go.html");
                        String finallink = br3.getRegex("<noframes> <br /> <a href=\"(.*?)\"").getMatch(0);
                        if (finallink == null) finallink = br3.getRegex("<iframe style=\".*?\" src=\"(.*?)\"").getMatch(0);
                        if (finallink != null) decryptedLinks.add(createDownloadlink(finallink));
                    }
                }
            }

            if (fpName != null) {
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }

}
