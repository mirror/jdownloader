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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "duckload.com" }, urls = { "http://(www\\.)?duckload\\.com/folder/[a-z0-9]+" }, flags = { 0 })
public class DuckLoadComFolder extends PluginForDecrypt {

    public DuckLoadComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> realPages = new ArrayList<String>();
        realPages.add("0");
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("This folder could not be found\\.")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String fpName = br.getRegex("ico_folder\\.png\" alt=\"\" style=\"margin-bottom:-3px;\" /> (.*?) \\[<strong>").getMatch(0);
        // Find all pages and remove double entries
        String[] pages = br.getRegex("folder_start=(\\d+)\\'").getColumn(0);
        if (pages != null && pages.length != 0) for (String aPage : pages)
            if (!realPages.contains(aPage)) realPages.add(aPage);
        String folderID = new Regex(parameter, "duckload\\.com/folder/(.+)").getMatch(0);
        progress.setRange(realPages.size());
        for (String currentPage : realPages) {
            if (!currentPage.equals("0")) br.getPage("http://www.duckload.com/Folder/" + folderID + "&folder_start=" + currentPage);
            String[] links = br.getRegex("\\'(http://(www\\.)?duckload\\.com/(download/\\d+/.*?|play/[A-Z0-9]+))\\'").getColumn(0);
            if (links == null || links.length == 0) return null;
            for (String dl : links)
                decryptedLinks.add(createDownloadlink(dl));
            progress.increase(1);
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
