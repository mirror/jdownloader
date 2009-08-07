//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.http.Request;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "megaupload.com folder" }, urls = { "http://[\\w\\.]*?megaupload\\.com/.*?\\?f=[\\w]+"}, flags = { 0 })


public class MegauploadComFolder extends PluginForDecrypt {

    public MegauploadComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    //@Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        String id = Request.parseQuery(param.getCryptedUrl()).get("f");
        String url = "http://www.megaupload.com/xml/folderfiles.php?folderid=" + id;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        br.getPage(url);
        String[] files = br.getRegex("url=\"(.*?)\"").getColumn(0);
        for (String file : files) {
            decryptedLinks.add(createDownloadlink(file));
        }
        return decryptedLinks;
    }

    //@Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

}
