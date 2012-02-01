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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 14836 $", interfaceVersion = 2, names = { "mukki.org" }, urls = { "http://(www\\.)?mukki\\.org/.+/.+\\-\\d+" }, flags = { 0 })
public class MkiOg extends PluginForDecrypt {

    public MkiOg(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final static String ua = RandomUserAgent.generate();

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("://www.", "://");
        br.setCookie("http://mukki.org", "lang", "english");
        br.getHeaders().put("User-Agent", ua);
        br.getPage(parameter);
        // error clauses
        if (br.containsHTML("(?i)(>Not Found</h1>|>Apologies\\, but the page you requested could not be found\\.)")) {
            logger.warning("Invalid URL: " + parameter);
            return null;
        }
        // find a respectable package name
        String fp = Encoding.htmlDecode(br.getRegex("(?i)</a> \\&raquo\\; ([^\"<>]+)").getMatch(0));
        if (fp == null) {
            fp = Encoding.htmlDecode(br.getRegex("(?i)\"></span>(.*?)</h1>").getMatch(0));
        }
        String fpName = null;
        if (fp != null)
            fpName = (fp + " - Mukki Site Links").trim();
        else
            fpName = "Mukki Site Links";
        FilePackage FP1 = FilePackage.getInstance();
        FP1.setName(fpName);
        // find and decode base64
        String SitePost = Encoding.Base64Decode(br.getRegex("(?i)\\(showmeass\\.decode\\(\"([a-zA-Z0-9\\+\\/]+[=]{0,2})\"\\)\\)").getMatch(0));
        String[] links = new Regex(SitePost, "(?i)href=\\'(.*?)\\'").getColumn(0);
        if (links == null || links.length == 0) links = new Regex(SitePost, "(?i)>(.*?)</a>").getColumn(0);
        if (links != null && links.length > 0) {
            for (String link : links) {
                DownloadLink thislink = this.createDownloadlink(link);
                FP1.add(thislink);
                decryptedLinks.add(thislink);
            }
        }
        // user comments
        if (fp != null)
            fpName = (fp + " - Mukki User Links").trim();
        else
            fpName = "Mukki User Links";
        FilePackage FP2 = FilePackage.getInstance();
        FP2.setName(fpName);
        String UserComments = br.getRegex("(?i)<div id=\"comments\">(.*?)</ol></div>").getMatch(0);
        String[] UserLinks = null;
        if (UserComments != null) {
            UserLinks = new Regex(UserComments, "(?i)>(https?://.*?)</a>").getColumn(0);
            if (UserLinks == null || UserLinks.length == 0) {
                UserLinks = new Regex(UserComments, "(?i)href=\"(.*?)\"").getColumn(0);
            }
        }
        if (UserLinks != null) {
            for (String link : UserLinks) {
                if (!link.contains("mukki.org")) {
                    DownloadLink thislink = this.createDownloadlink(link);
                    FP2.add(thislink);
                    decryptedLinks.add(thislink);
                }
            }
        }
        return decryptedLinks;
    }
}
