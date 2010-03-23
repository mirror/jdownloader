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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "motherless.com" }, urls = { "http://([\\w\\.]*?|members\\.)motherless\\.com/((?!movies|thumbs)\\w)\\w*" }, flags = { 0 })
public class MotherLessCom extends PluginForDecrypt {

    public MotherLessCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(parameter.toString());
        if (br.containsHTML("Not Available") || br.containsHTML("not found") || br.containsHTML("You will be redirected to")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String parm = parameter.toString();
        String filelink = br.getRegex("var __file_url = '([^']*)';").getMatch(0);
        if (filelink == null) return null;
        String matches = br.getRegex("s1.addParam\\('flashvars','file=([^)]*)").getMatch(0);
        if (matches == null) {
            matches = br.getRegex("(Not Available)").getMatch(0);
            if (matches == null) return null;
            logger.warning("The requested document was not found on this server.");
            logger.warning(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            return decryptedLinks;
        }

        filelink = rot13(filelink);

        String downloadlink = "http://members.motherless.com/movies/" + filelink + ".flv?start=0&id=player&client=FLASH%20WIN%2010,0,32,18&version=4.1.60";
        DownloadLink dlink = createDownloadlink(downloadlink);

        dlink.setBrowserUrl(parm);

        dlink.setFinalFileName(filelink.split("-")[0] + ".flv");

        decryptedLinks.add(dlink);

        return decryptedLinks;
    }

    private String rot13(String s) {
        String output = "";
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'a' && c <= 'm')
                c += 13;
            else if (c >= 'n' && c <= 'z')
                c -= 13;
            else if (c >= 'A' && c <= 'M')
                c += 13;
            else if (c >= 'A' && c <= 'Z') c -= 13;
            output += c;
        }
        return output;
    }

}
