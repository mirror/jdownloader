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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "share.cx" }, urls = { "http://[\\w\\.]*?share.cx/users/.*/.*/.*" }, flags = { 0 })
public class ShareCxFolder extends PluginForDecrypt {

    public ShareCxFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        String parameter = param.toString();
        br.getPage(parameter);
        String dir = new Regex(parameter, "share\\.cx/users/.*?/(.*?)/").getMatch(0);
        String usr = br.getRegex("usr: \\'(\\d+)\\'").getMatch(0);
        if (dir == null || usr == null) return null;
        br.postPage("http://www.share.cx/jqueryFileTree.php", "dir=" + dir + "&usr=" + usr);
        if (br.toString().trim().equals("<ul class=\"jqueryFileTree\" id=\"filebrowser_list\" style=\"display: none;\"></ul>")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String[] links = br.getRegex("class=\"filebrowser_link\" href=\"(http://.*?)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        for (String dl : links)
            decryptedLinks.add(createDownloadlink(dl));

        return decryptedLinks;
    }
}
