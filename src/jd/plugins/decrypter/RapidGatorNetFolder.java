//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rapidgator.net" }, urls = { "https?://(?:www\\.)?(?:rapidgator\\.net|rg\\.to)/folder/\\d+/[^/]+\\.html" })
@SuppressWarnings("deprecation")
public class RapidGatorNetFolder extends PluginForDecrypt {

    private String parameter = null;
    private String uid       = null;

    public RapidGatorNetFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        parameter = param.toString();
        uid = new Regex(parameter, "/folder/(\\d+)").getMatch(0);
        // standardise browser configurations to avoid detection.
        /* we first have to load the plugin, before we can reference it */
        JDUtilities.getPluginForHost("rapidgator.net");
        jd.plugins.hoster.RapidGatorNet.prepareBrowser(br);
        // normal stuff
        br.setFollowRedirects(true);
        br.getPage(parameter);
        br.setFollowRedirects(false);

        if (br.containsHTML("E_FOLDERNOTFOUND") || this.br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String fpName = br.getRegex("Downloading:[\r\n\t ]+</strong>[\r\n\t ]+(.*?)[\r\n\t ]+</p>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<title>Download file (.*?)</title>").getMatch(0);
        }
        parsePage(decryptedLinks);
        String lastPage = null;
        while (true) {
            String nextPage = br.getRegex("<a href=\"(/folder/" + uid + "/[^>]+\\?page=\\d+)\">Next").getMatch(0);
            if (lastPage == null) {
                lastPage = br.getRegex("<a href=\"(/folder/" + uid + "/[^>]+\\?page=\\d+)\">Last").getMatch(0);
            }
            if (nextPage != null && (lastPage != null && !br.getURL().contains(lastPage))) {
                br.getPage(nextPage);
                parsePage(decryptedLinks);
            } else {
                break;
            }
        }

        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private void parsePage(ArrayList<DownloadLink> ret) {
        final String[] subfolders = this.br.getRegex("<td><a href=\"(/folder/\\d+/[^<>\"/]+\\.html)\">").getColumn(0);
        String[][] links = br.getRegex("\"(/file/([a-z0-9]{32}|\\d+)/([^\"]+))\".*?>([\\d\\.]+ (KB|MB|GB))").getMatches();

        if ((links == null || links.length == 0) && (subfolders == null || subfolders.length == 0)) {
            logger.warning("Empty folder, or possible plugin defect. Please confirm this issue within your browser, if the plugin is truely broken please report issue to JDownloader Development Team. " + parameter);
            return;
        }
        if (links != null && links.length != 0) {
            for (String[] dl : links) {
                DownloadLink link = createDownloadlink(Request.getLocation(dl[0], br.getRequest()));
                link.setName(dl[2].replaceFirst("\\.html$", ""));
                link.setDownloadSize(SizeFormatter.getSize(dl[3]));
                link.setAvailable(true);
                ret.add(link);
            }
        }

        if (subfolders != null && subfolders.length != 0) {
            for (final String folder : subfolders) {
                final DownloadLink link = createDownloadlink(Request.getLocation(folder, br.getRequest()));
                ret.add(link);
            }
        }

    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}