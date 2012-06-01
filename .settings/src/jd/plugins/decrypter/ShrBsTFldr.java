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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision: 9022 $", interfaceVersion = 2, names = { "sharebase.to folder" }, urls = { "http://[\\w\\.]*?sharebase\\.to/3,[A-Z0-9]+\\.html" }, flags = { 0 })
public class ShrBsTFldr extends PluginForDecrypt {

    public ShrBsTFldr(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        String url = param.getCryptedUrl();
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        br.getPage(url);
        String check = br.getRedirectLocation();
        if (br.containsHTML(">Dieser Ordner existiert nicht") || check != null) {
            logger.warning("Wrong link");
            logger.warning(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            return decryptedLinks;
        }
        String[] files = br.getRegex("ng>DL:</strong>(.*?)<br").getColumn(0);
        if (files == null || files.length == 0) files = br.getRegex("\"a2\" href=\"(.*?)\"").getColumn(0);
        if (files == null || files.length == 0) return null;
        for (String file : files) {
            decryptedLinks.add(createDownloadlink(file));
        }
        return decryptedLinks;
    }
}
