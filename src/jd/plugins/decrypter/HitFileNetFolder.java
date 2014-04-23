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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hitfile.net" }, urls = { "http://(www\\.)?hitfile\\.net/download/folder/\\d+" }, flags = { 0 })
public class HitFileNetFolder extends PluginForDecrypt {

    public HitFileNetFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getHeaders().put("Referer", parameter);
        br.getPage("http://hitfile.net/lang/en");
        if (!br.getURL().equals(parameter)) br.getPage(parameter);
        if (br.containsHTML(">There are no any files in this folder<|>Searching file\\.\\.\\.Please wait|>Please wait, searching file")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String fpName = br.getRegex("class=\\'folder\\-big\\'><img src=\\'/js/lib/grid/icon/folder\\.png\\'>([^<>\"\\']+)</div>").getMatch(0);
        final String fid = new Regex(parameter, "/folder/(\\d+)").getMatch(0);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getPage("http://hitfile.net/downloadfolder/gridFile?rootId=" + fid + "&currentId=" + fid + "&_search=false&nd=" + Math.random() + "&rows=2000&page=1&sidx=name&sord=asc");
        String[] ids = br.getRegex("\"id\":\"([A-Za-z0-9]+)\"").getColumn(0);
        if (ids == null || ids.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String id : ids)
            decryptedLinks.add(createDownloadlink("http://hitfile.net/" + id));
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}