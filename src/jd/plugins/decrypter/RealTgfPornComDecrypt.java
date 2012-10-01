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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "realtgfporn.com" }, urls = { "http://(www\\.)?realgfporn\\.com/\\d+/.*?\\.html" }, flags = { 0 })
public class RealTgfPornComDecrypt extends PluginForDecrypt {

    public RealTgfPornComDecrypt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        br.getPage(parameter);
        String tempID = br.getRedirectLocation();
        if (tempID != null && tempID.equals("http://www.realgfporn.com/") || br.containsHTML("Internet Explorer Custom 404")) {
            final DownloadLink dl = createDownloadlink(parameter.replace("realgfporn.com/", "realgfporndecrypted.com/"));
            dl.setAvailable(false);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (tempID != null) {
            decryptedLinks.add(createDownloadlink(tempID));
            return decryptedLinks;
        }
        // Same regexes as used in hosterplugin
        String filename = br.getRegex("<h3 class=\"video_title\">(.*?)</h3>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        if (filename == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        filename = filename.trim() + ".flv";
        tempID = br.getRegex("bufferLength: \\d+,[\t\n\r ]+url: \\'(http://.*?)\\'").getMatch(0);
        if (tempID != null) {
            final DownloadLink dl = createDownloadlink("directhttp://" + tempID);
            decryptedLinks.add(dl);
            dl.setFinalFileName(filename);
            return decryptedLinks;
        }
        final DownloadLink dl = createDownloadlink(parameter.replace("realgfporn.com/", "realgfporndecrypted.com/"));
        decryptedLinks.add(dl);
        return decryptedLinks;
    }

}
