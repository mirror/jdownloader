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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "muslimass.com" }, urls = { "http://(www\\.)?muslimass\\.com/[a-z0-9\\-]+" }, flags = { 0 })
public class MuslimAssCom extends PluginForDecrypt {

    public MuslimAssCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // This is a site which shows embedded videos of other sites so we may have
    // to add regexes/handlings here
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML("(>Error 404 \\- Not Found<|<title>Nothing found for)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String filename = br.getRegex("<link rel=\"alternate\" type=\"application/rss\\+xml\" title=\"muslimass\\.com \\&raquo; (.*?) Comments Feed\" href=\"").getMatch(0);
        if (filename == null) filename = br.getRegex("title=\"Permanent Link to (.*?)\"").getMatch(0);
        if (filename == null) {
            logger.warning("hqmaturetube decrypter broken(filename regex) for link: " + parameter);
            return null;
        }
        filename = filename.trim();
        String externID = br.getRegex("<p style=\"text-align: center;\"><a href=\"(http://.*?)\"").getMatch(0);
        if (externID == null) externID = br.getRegex("\"(http://(www\\.)?xvideohost\\.com/video\\.php\\?id=[a-z0-9]+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(externID)));
            return decryptedLinks;
        }
        externID = br.getRegex("\"id_video=(\\d+)\"").getMatch(0);
        if (externID != null) {
            String finallink = "http://www.xvideos.com/video" + externID + "/";
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(finallink)));
            return decryptedLinks;
        }
        if (externID == null) {
            logger.warning("muslimass decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }
}
