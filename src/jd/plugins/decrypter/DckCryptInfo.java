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
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "duckcrypt.info" }, urls = { "http://(www\\.)?duckcrypt\\.info/folder/[a-z0-9]+" }, flags = { 0 })
public class DckCryptInfo extends PluginForDecrypt {

    public DckCryptInfo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getURL().equals("http://duckcrypt.info/notfound.html")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        br.setFollowRedirects(false);
        br.getPage(parameter.replace("/folder/", "/wait/"));
        String continueLink = br.getRegex("url: \"(http://.*?)\"").getMatch(0);
        if (continueLink == null) continueLink = br.getRegex("\"(http://duckcrypt\\.info/ajax/auth\\.php\\?hash=[a-z0-9]+)\"").getMatch(0);
        if (continueLink == null) return null;
        Browser br2 = br.cloneBrowser();
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br2.getPage(continueLink);
        String folder = br2.getRegex("\"(http://duckcrypt\\.info/folder/[a-z0-9]+/[a-z0-9]+)\"").getMatch(0);
        if (folder == null) folder = br2.getRegex("<a href=\"(http://.*?)\"").getMatch(0);
        if (folder == null) return null;
        br.getPage(folder);
        String[] links = br.getRegex("<h2><a href=\"(http://.*?)\"").getColumn(0);
        if (links == null || links.length == 0) br.getRegex("\"(http://duckcrypt\\.info/link/[a-z0-9]+)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        for (String singleLink : links) {
            br.getPage(singleLink);
            String finalLink = br.getRegex("name=\"redirect_iframe\" src=\"(.*?)\"").getMatch(0);
            if (finalLink == null) finalLink = br.getRegex("aufrufen: <a href=\"(.*?)\"").getMatch(0);
            if (finalLink == null) return null;
            finalLink = Encoding.htmlDecode(finalLink);
            decryptedLinks.add(createDownloadlink(finalLink));
        }
        return decryptedLinks;
    }

}
