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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.SiteType.SiteTemplate;

//When adding new domains here also add them to the hosterplugin (TurboBitNet)
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "turbobit.net" }, urls = { "https?://(?:www\\.)?(ifolder\\.com\\.ua|wayupload\\.com|turo-bit\\.net|depositfiles\\.com\\.ua|dlbit\\.net|hotshare\\.biz|sibit\\.net|turbobit\\.net|turbobit\\.ru|xrfiles\\.ru|turbabit\\.net|filedeluxe\\.com|filemaster\\.ru|файлообменник\\.рф|turboot\\.ru|kilofile\\.com|twobit\\.ru|forum\\.flacmania\\.ru|filhost\\.ru|fayloobmennik\\.com)/download/folder/\\d+" })
public class TurboBitNetFolder extends PluginForDecrypt {
    @Override
    public String[] siteSupportedNames() {
        return new String[] { "ifolder.com.ua", "wayupload.com", "turo-bit.net", "depositfiles.com.ua", "dlbit.net", "hotshare.biz", "sibit.net", "turbobit.net", "turbobit.ru", "xrfiles.ru", "turbabit.net", "filedeluxe.com", "filemaster.ru", "файлообменник.рф", "turboot.ru", "kilofile.com", "twobit.ru", "forum.flacmania.ru", "filhost.ru", "fayloobmennik.com" };
    }

    public TurboBitNetFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        String id = new Regex(parameter, "download/folder/(\\d+)").getMatch(0);
        if (id == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        // rows = 100 000 makes sure that we only get one page with all links
        // br.getPage("http://turbobit.net/downloadfolder/gridFile?id_folder=" + id + "&_search=false&nd=&rows=100000&page=1");
        br.getPage("http://turbobit.net/downloadfolder/gridFile?rootId=" + id + "?currentId=" + id + "&_search=false&nd=&rows=100000&page=1&sidx=file_type&sord=asc");
        if (br.containsHTML("\"records\":0,\"total\":0,\"")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String[] ids = br.getRegex("\\{\"id\":\"([a-z0-9]+)\"").getColumn(0);
        if (ids == null || ids.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String singleID : ids) {
            if (!singleID.equals(id)) {
                decryptedLinks.add(createDownloadlink("http://turbobit.net/" + singleID + ".html"));
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Turbobit_Turbobit;
    }
}