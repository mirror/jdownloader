//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 14951 $", interfaceVersion = 2, names = { "unibytes.com" }, urls = { "http://(www\\.)?unibytes\\.com/folder/[a-zA-Z0-9\\-\\.\\_]{11}B" }, flags = { 0 })
public class UniBytesComFolder extends PluginForDecrypt {

    // https not currently available

    public UniBytesComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("http://unibytes", "http://www.unibytes");
        br.setCookie("http://www.unibtyes.com", "lang", "en");
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String id = new Regex(parameter, "\\.com/folder/([a-zA-Z0-9\\-\\.\\_]{12})").getMatch(0);
        if (br.getURL().contains("unibytes.com/upload")) {
            logger.warning("Invalid URL or the folder no longer exists: " + parameter);
            return decryptedLinks;
        }
        String fpName = br.getRegex("<h3 style=\"font\\-size: 2em\\;\">(.*?)</h3>").getMatch(0);
        if (fpName == null) br.getRegex("<title>Download (.*?) \\- Unibytes.com</title>").getMatch(0);
        parsePage(decryptedLinks, id);
        parseNextPage(decryptedLinks, id);
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private void parsePage(ArrayList<DownloadLink> ret, String id) {
        String[] links = br.getRegex("<li><a href=\"(http://(www\\.)?unibytes\\.com/[a-zA-Z0-9\\-\\.\\_ ]+)").getColumn(0);
        if (links == null || links.length == 0) return;
        if (links != null && links.length != 0) {
            for (String dl : links)
                ret.add(createDownloadlink(dl));
        }
    }

    private boolean parseNextPage(ArrayList<DownloadLink> ret, String id) throws IOException {
        String nextPage = br.getRegex("<a href=\"(/folder/" + id + "\\?page=\\d+)\">Следующая →</a>").getMatch(0);
        if (nextPage != null) {
            br.getPage("http://www.unibytes.com" + nextPage);
            parsePage(ret, id);
            parseNextPage(ret, id);
            return true;
        }
        return false;
    }
}
