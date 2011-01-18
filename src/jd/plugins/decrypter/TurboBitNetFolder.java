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

import org.appwork.utils.Regex;

//When adding new domains here also add them to the hosterplugin (TurboBitNet)
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "turbobit.net" }, urls = { "http://[\\w\\.]*?(filesmail\\.ru|hotshare\\.biz|bluetooths\\.pp\\.ru|speed-file\\.ru|sharezoid\\.com|turbobit\\.pl|dz-files\\.ru|file\\.alexforum\\.ws|file\\.grad\\.by|file\\.krut-warez\\.ru|filebit\\.org|files\\.best-trainings\\.org\\.ua|files\\.wzor\\.ws|gdefile\\.ru|letitshare\\.ru|mnogofiles\\.com|share\\.uz|sibit\\.net|turbo-bit\\.ru|turbobit\\.net|upload\\.mskvn\\.by|vipbit\\.ru|files\\.prime-speed\\.ru|filestore\\.net\\.ru|turbobit\\.ru|upload\\.dwmedia\\.ru|upload\\.uz|xrfiles\\.ru|unextfiles\\.com|e-flash\\.com\\.ua|turbobax\\.net|zharabit\\.net|download\\.uzhgorod\\.name|trium-club\\.ru|alfa-files\\.com)/download/folder/\\d+" }, flags = { 0 })
public class TurboBitNetFolder extends PluginForDecrypt {

    public TurboBitNetFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String id = new Regex(param.toString(), "download/folder/(\\d+)").getMatch(0);
        if (id == null) return null;
        // rows = 100 000 makes sure that we only get one page with all links
        br.getPage("http://turbobit.net/downloadfolder/gridFile?id_folder=" + id + "&_search=false&nd=&rows=100000&page=1");
        if (br.containsHTML("\"records\":0,\"total\":0,\"")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String[] ids = br.getRegex("\\{\"id\":\"([a-z0-9]+)\"").getColumn(0);
        if (ids == null || ids.length == 0) return null;
        for (String singleID : ids)
            decryptedLinks.add(createDownloadlink("http://turbobit.net/" + singleID + ".html"));

        return decryptedLinks;
    }

}
