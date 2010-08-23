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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hotfile.com" }, urls = { "http://[\\w\\.]*?hotfile\\.com/(list/\\d+/[\\w]+|links/\\d+/[a-z0-9]+/.+)" }, flags = { 0 })
public class HtflCm extends PluginForDecrypt {

    public HtflCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().equals("http://hotfile.com/")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            return null;
        }
        if (br.containsHTML("Empty Directory")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        if (parameter.contains("/list/")) {
            FilePackage fp = FilePackage.getInstance();
            String fpName = br.getRegex("-2px;\" />(.*?)/?</td>").getMatch(0).trim();
            if (fpName == null || fpName.length() == 0) fpName = "Hotfile.com folder";
            fp.setName(fpName);
            String[] links = br.getRegex("(http://hotfile\\.com/dl/\\d+/[0-9a-zA-Z]+/.*?)\"").getColumn(0);
            if (links == null || links.length == 0) return null;
            for (String link : links) {
                DownloadLink dlink = createDownloadlink(link);
                decryptedLinks.add(dlink);
                fp.add(dlink);
            }
        } else {
            String finallink = br.getRegex("name=\"url\" id=\"url\" class=\"textfield\" value=\"(.*?)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("name=\"forum\" id=\"forum\" class=\"textfield\" value=\"\\[URL=(http://hotfile\\.com/.*?)\\]http").getMatch(0);
                if (finallink == null) {
                    finallink = br.getRegex("\"(http://hotfile\\.com/dl/\\d+/[a-z0-9]+/.*?\\.html\"").getMatch(0);
                }
            }
            if (finallink == null) return null;
            decryptedLinks.add(createDownloadlink(finallink));
        }
        return decryptedLinks;
    }

}
