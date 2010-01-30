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
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "israbox.com" }, urls = { "http://[\\w\\.]*israbox\\.com/[0-9]+-.*?\\.html" }, flags = { 0 })
public class SrBoxCom extends PluginForDecrypt {

    public SrBoxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.containsHTML("(An error has occurred|The article cannot be found)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String fpName = br.getRegex("<title>(.*?)\\&raquo").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<b>Download Fast:(.*?)</b>").getMatch(0);
                if (fpName == null) {
                    fpName = br.getRegex("color=\"#ffffff\" size=\"1\">(.*?)free from rapidshare").getMatch(0);
                }
            }
        }
        String[] links = br.getRegex("leech_begin--><a href=\"(.*?)\"").getColumn(0);
        if (links == null || links.length == 0) links = br.getRegex("\"(http://www\\.israbox\\.com/engine/go\\.php\\?url=.*?)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String redirectlink : links) {
            br.getPage(redirectlink);
            String finallink = br.getRedirectLocation();
            if (finallink == null) return null;
            decryptedLinks.add(createDownloadlink(finallink));
            progress.increase(1);
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
