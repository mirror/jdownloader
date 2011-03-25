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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.RandomUserAgent;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "linkbee.com" }, urls = { "http://[\\w\\.]*?linkbee\\.com/[\\w]+" }, flags = { 0 })
public class LnkBCm extends PluginForDecrypt {

    private static final String ua = RandomUserAgent.generate();

    public LnkBCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getHeaders().put("User-Agent", ua);
        br.setFollowRedirects(false);
        String found = null;
        br.getPage(parameter);
        String id = new Regex(parameter, "\\.com/([\\w]+)").getMatch(0);
        String loc = br.getRedirectLocation();
        if (loc != null && loc.contains(id)) {
            br.getPage(loc);
        }
        if (br.getRedirectLocation() != null) {
            found = br.getRedirectLocation();
        } else {
            String[] lol = HTMLParser.getHttpLinks(br.toString(), "");
            String title = br.getRegex("<title>Linkbee:.*?(http://.*?/).*?</title>").getMatch(0);
            if (title != null) title = title.trim();
            for (String pwned : lol) {
                if (!pwned.equals(parameter) && !pwned.endsWith(".gif")) {
                    if (title != null && pwned.startsWith(title)) {
                        decryptedLinks.add(createDownloadlink(pwned));
                    }
                }
            }
            if (title == null) {
                String link = br.getRegex("skipBtn.*?urlholder' value='(http:/.*?)'").getMatch(0);
                if (link != null) {
                    decryptedLinks.add(createDownloadlink(link));
                }
            }
            return decryptedLinks;
        }
        if (found == null) return null;
        decryptedLinks.add(createDownloadlink(found));
        return decryptedLinks;
    }

}
