//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "euroshare.eu" }, urls = { "http://(www\\.)?euroshare\\.(eu|sk)/folder/\\d+/[^\"<>\\' ]+" }, flags = { 0 })
public class EuroShareEuFolder extends PluginForDecrypt {
    
    public EuroShareEuFolder(PluginWrapper wrapper) {
        super(wrapper);
    }
    
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("euroshare.sk/", "euroshare.eu/");
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.getPage(parameter);
        String id = new Regex(parameter, "euroshare\\.eu/folder/(\\d+)/").getMatch(0);
        if (br.containsHTML(">error 404<")) return null;
        String fpName = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
        String[] links = br.getRegex("href=\"(/file/\\d+/[^\"]+)").getColumn(0);
        if (links == null || links.length == 0) links = br.getRegex("\"(https?://euroshare\\.eu/file/[a-zA-Z0-9]+/[^\"]+").getColumn(0);
        if (links == null || links.length == 0) return null;
        if (links != null && links.length != 0) {
            for (String dl : links) {
                if (dl.matches("/file/[a-zA-Z0-9]+/[^\"]+"))
                    decryptedLinks.add(createDownloadlink("http://euroshare.eu" + dl));
                else
                    decryptedLinks.add(createDownloadlink(dl));
            }
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
    
    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
    
}