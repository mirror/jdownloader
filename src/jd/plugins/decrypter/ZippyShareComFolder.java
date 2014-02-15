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
import jd.http.RandomUserAgent;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zippyshare.com" }, urls = { "http://(www\\.)?zippyshare\\.com/[a-z0-9\\-_%]+/[a-z0-9\\-_%]+/dir\\.html" }, flags = { 0 })
public class ZippyShareComFolder extends PluginForDecrypt {

    public ZippyShareComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(parameter);
        if (!br.containsHTML("class=\"filerow even\">")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String[] links = br.getRegex("\"(http://www\\d+\\.zippyshare\\.com/v/\\d+/file\\.html)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        // Over 50 links? Maybe there is more...
        if (links.length == 50) {
            final String user = new Regex(parameter, "zippyshare\\.com/([a-z0-9\\-_]+)/([a-z0-9\\-_]+)/").getMatch(0);
            final String dir = new Regex(parameter, "zippyshare\\.com/[a-z0-9\\-_]+/([a-z0-9\\-_]+)/").getMatch(0);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage("http://www.zippyshare.com/fragments/publicDir/filetable.jsp", "page=0&user=" + user + "&dir=" + dir + "&sort=nameasc&pageSize=250&search=&viewType=default");
            links = br.getRegex("\"(http://www\\d+\\.zippyshare\\.com/v/\\d+/file\\.html)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
        }
        for (String singleLink : links)
            decryptedLinks.add(createDownloadlink(singleLink));

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}