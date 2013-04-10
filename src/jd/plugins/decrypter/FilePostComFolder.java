//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filepost.com" }, urls = { "https?://(www\\.)?(filepost\\.com|fp\\.io)/folder/[a-z0-9]+" }, flags = { 0 })
public class FilePostComFolder extends PluginForDecrypt {

    public FilePostComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("fp.io/", "filepost.com/");
        br.setCookie("http://filepost.com", "lang", "english");
        br.getPage(parameter);
        String id = new Regex(parameter, "filepost\\.com/folder/([a-z0-9]+)").getMatch(0);
        if (br.containsHTML(">This folder is empty<")) {
            logger.info("Link offline (folder empty): " + parameter);
            return decryptedLinks;
        }
        String fpName = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
        parsePage(decryptedLinks, id);
        parseNextPage(decryptedLinks, id);
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private void parsePage(ArrayList<DownloadLink> ret, String id) {
        String[] links = br.getRegex("<div class=\"file (.*?)\"><a href=\"(https?://.*?/files/.*?)/?\"").getColumn(1);
        String[] folders = br.getRegex("<div class=\"file folder\"><a href=\"(https?://filepost\\.com/folder/[a-z0-9]+)/?\"").getColumn(0);
        if (links == null || links.length == 0) links = br.getRegex("\"(https?://filepost\\.com/files/[a-z0-9]+/?\"").getColumn(0);
        if ((links == null || links.length == 0) && (folders == null || folders.length == 0)) return;
        if (links != null && links.length != 0) {
            for (String dl : links)
                ret.add(createDownloadlink(dl));
        }
        if (folders != null && folders.length != 0) {
            for (String aFolder : folders)
                if (!aFolder.contains(id)) ret.add(createDownloadlink(aFolder));
        }
    }

    private boolean parseNextPage(ArrayList<DownloadLink> ret, String id) throws IOException {
        String nextPage = br.getRegex("class=\"pager-link\" .*?(http://filepost.com/folder/" + id + "/\\d+/?)\">Next").getMatch(0);
        if (nextPage != null) {
            br.getPage(nextPage);
            parsePage(ret, id);
            parseNextPage(ret, id);
            return true;
        }
        return false;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}