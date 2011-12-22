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

@DecrypterPlugin(revision = "$Revision: 14953 $", interfaceVersion = 2, names = { "u.115.com" }, urls = { "http://(www\\.)?(u\\.)?115\\.com/folder/[a-z0-9]{11}" }, flags = { 0 })
public class U115ComFolder extends PluginForDecrypt {

    public U115ComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        parameter = parameter.replace("u.115.com/", "115.com/");
        br.setReadTimeout(2 * 60 * 1000);
        br.setCookie("http://u.115.com/", "lang", "en");
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(false);
        br.setCookiesExclusive(true);
        br.getPage(parameter);
        String id = new Regex(parameter, "115\\.com/folder/([a-z0-9]{11})").getMatch(0);
        if (br.containsHTML(">文件夹提取码不存在<")) {
            logger.warning("Invalid URL: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML(">该文件夹下暂时没有分享文件。<")) {
            logger.warning("Empty folder: " + parameter);
            return decryptedLinks;
        }
        // Set package name and prevent null field from creating plugin errors
        String fpName = br.getRegex("<i class=\"file\\-type tp\\-folder\"></i><span class=\"file\\-name\">(.*?)</span>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("desc:\\'分享好资源\\|   (.*?) http://").getMatch(0);
        if (fpName == null) fpName = "Untitled";

        parsePage(decryptedLinks, id);
        parseNextPage(decryptedLinks, id);

        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private void parsePage(ArrayList<DownloadLink> ret, String id) {
        String[] links = br.getRegex("<span class=\"file\\-name\"><a title=\".*?\" href=\"(http://(www\\.)?(u\\.)?115\\.com/file/[a-z0-9]+)\" target=\"\\_blank\">").getColumn(0);
        if (links == null || links.length == 0) return;
        if (links != null && links.length != 0) {
            for (String dl : links)
                ret.add(createDownloadlink(dl));
        }
    }

    private boolean parseNextPage(ArrayList<DownloadLink> ret, String id) throws IOException {
        String nextPage = br.getRegex("<a href=\\'(http://(www\\.)?(u\\.)?115\\.com/folder/" + id + "/(\\d+))\\'><b class=\\'next-page\\'>").getMatch(0);
        if (nextPage != null) {
            br.getPage(nextPage);
            parsePage(ret, id);
            parseNextPage(ret, id);
            return true;
        }
        return false;
    }
}