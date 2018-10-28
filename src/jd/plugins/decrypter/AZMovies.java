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
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 40019 $", interfaceVersion = 2, names = { "azmovies.xyz" }, urls = { "https?://(www\\.)?azmovies\\.xyz/watch.php?.+" })
public class AZMovies extends PluginForDecrypt {
    public AZMovies(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        String page = br.getPage(parameter);
        if (br.containsHTML("window.location.href")) {
            String[][] cookies = br.getRegex("document\\.cookie ?= \"?([^\"]+)\"").getMatches();
            String cookieString = "";
            String host = Browser.getHost(br.getURL());
            for (String[] cookie : cookies) {
                cookieString += ((cookieString.length() > 0 ? "&" : "") + cookie[0]);
            }
            br.setCookies(host, Cookies.parseCookies(cookieString, host, null));
            page = br.getPage(br.getRegex("window\\.location\\.href ?= \"?([^\"]+)\"").getMatch(0));
        }
        String fpName = br.getRegex("<meta (?:name|property)=\"description\" content=[\"'](?:Watch ?)([^<>\"]*?) full Movie for free").getMatch(0);
        //
        String[][] links = br.getRegex("<a href=\"([^\"]+)\" onclick=\"servernamechange").getMatches();
        for (String[] link : links) {
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link[0])));
        }
        //
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}