//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "extabit.com" }, urls = { "http://[\\w\\.]*?extabit\\.com/(folder/\\d+|folder\\.jsp\\?id=\\d+)" }, flags = { 0 })
public class ExtabitComFolder extends PluginForDecrypt {

    public ExtabitComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (param.getCryptedUrl().contains("folder.jsp")) {
            String correctedLink = new Regex(param.getCryptedUrl(), "id=(\\d+)").getMatch(0);
            if (correctedLink != null)
                param.setCryptedUrl("http://extabit.com/folder/" + correctedLink);
            else
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String parameter = param.toString();
        br.setCookie("http://extabit.com", "language", "en");
        br.getPage(parameter);
        String id = new Regex(parameter, "extabit\\.com/folder/(\\d+)").getMatch(0);
        if (br.containsHTML("(>Folder doesn&#039;t exist\\.<|p>Unfortunatelly we didn&#039;t find requested folder\\.<|>Maybe folder was deleted by copyright owner\\.<)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the folder no longer exists."));
        String fpName = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<li class=\"folder_view_path_first\"><a href=\"/folder/" + id + ">(.*?)</a></li>").getMatch(0);
        parsePage(decryptedLinks, id);
        parseNextPage(decryptedLinks, id);
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private void parsePage(ArrayList<DownloadLink> ret, String id) throws PluginException {
        // filter the page, reduce false positives
        String filter = br.getRegex("<div id=\"folder_contents\">(.*?)<div id=\"pager\">").getMatch(0);
        if (filter == null) {
            logger.warning("Can't filter folder content, Please report issue to JDownloader Development Team");
            return;
        }
        String[] links = new Regex(filter, "\"(/file/[a-z0-9]+(/)?)\"").getColumn(0);
        // not sure on this, haven't found a folder to test with
        String[] folders = new Regex(filter, "\"(/folder/\\d+(/)?)\"").getColumn(0);
        if ((links == null || links.length == 0) && (folders == null || folders.length == 0)) return;
        if (links != null && links.length != 0) {
            for (String dl : links)
                ret.add(createDownloadlink("http://extabit.com" + dl));
        }
        // same as above, not tested. left over from template...
        if (folders != null && folders.length != 0) {
            logger.warning("Sub folders feature has never been implemented. Please inform JDownloader Development Team and they will fix!");
            // for (String aFolder : folders)
            // if (!aFolder.contains(id)) ret.add(createDownloadlink(aFolder));
        }
    }

    private boolean parseNextPage(ArrayList<DownloadLink> ret, String id) throws IOException, PluginException {
        String nextPage = br.getRegex("<a href=\"(/folder\\.jsp\\?id=" + id + "&page=\\d+)\"><img src=\"[^>]+alt=\"next\">").getMatch(0);
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