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

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "nexusmods.com" }, urls = { "https?://(?:www\\.)?nexusmods\\.com/(?!contents)[^/]+/mods/\\d+/?" })
public class NexusmodsCom extends PluginForDecrypt {
    public NexusmodsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replaceFirst("^http://", "https://");
        final PluginForHost plugin = JDUtilities.getPluginForHost(this.getHost());
        ((jd.plugins.hoster.NexusmodsCom) plugin).setLogger(getLogger());
        ((jd.plugins.hoster.NexusmodsCom) plugin).setBrowser(br);
        final String fid = ((jd.plugins.hoster.NexusmodsCom) plugin).getFID(parameter);
        final Account account = AccountController.getInstance().getValidAccount(plugin.getHost());
        if (account != null) {
            try {
                ((jd.plugins.hoster.NexusmodsCom) plugin).login(account);
            } catch (PluginException e) {
                handleAccountException(account, e);
            }
        }
        ((jd.plugins.hoster.NexusmodsCom) plugin).getPage(br, parameter);
        if (((jd.plugins.hoster.NexusmodsCom) plugin).isOffline(br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (((jd.plugins.hoster.NexusmodsCom) plugin).isLoginRequired(br)) {
            throw new AccountRequiredException();
        } else if (br.containsHTML(">\\s*This mod contains adult content")) {
            /* 2019-10-02: Account required + setting has to be enabled in account to be able to see/download such content! */
            logger.info("Adult content: Enable it in your account settings to be able to download such files via JD: Profile --> Settings --> Content blocking --> Show adult content");
            throw new AccountRequiredException();
        }
        String fpName = br.getRegex("<title>([^>]+)</title>").getMatch(0);
        if (fpName == null) {
            /* Fallback */
            fpName = fid;
        }
        final Browser br2 = br.cloneBrowser();
        final String game_id = br.getRegex("game_id\\s*=\\s*(\\d+)").getMatch(0);
        if (game_id == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        ((jd.plugins.hoster.NexusmodsCom) plugin).getPage(br2, "/Core/Libs/Common/Widgets/ModFilesTab?id=" + fid + "&game_id=" + game_id);
        // final String[] downloadTypes = new String[] { "Main files", "Update files", "Optional files", "Miscellaneous files", "Old files"
        // };
        final String[] downloadTypesHTMLs = br2.getRegex("<div class=\"file-category-header\">\\s*<h2>[^<>]+</h2>\\s*<div>.*?</dd>\\s*</dl>\\s*</div>").getColumn(-1);
        int counter = 0;
        for (final String downnloadTypeHTML : downloadTypesHTMLs) {
            counter++;
            String currentCategory = new Regex(downnloadTypeHTML, "<h2>([^<>\"]+)</h2>").getMatch(0);
            if (currentCategory == null) {
                /* Fallback */
                currentCategory = "Unknown category " + counter;
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName + " - " + currentCategory);
            currentCategory = Encoding.htmlDecode(currentCategory).trim();
            final String currentPath = fpName + "/" + currentCategory;
            final String[][] downloads = new Regex(downnloadTypeHTML, "<span>([^<]*?)</span>.*?<li class=\"stat-filesize\">.*?class=\"stat\">(.*?)</.*?\"(/Core/Libs/Common/Widgets/DownloadPopUp?\\?id=\\d+.*?)\"").getMatches();
            if (downloads == null || downloads.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final String download[] : downloads) {
                final DownloadLink link = createDownloadlink(br2.getURL(download[2]).toString());
                final long size = SizeFormatter.getSize(download[1]);
                if (size > 0) {
                    link.setDownloadSize(size);
                }
                link.setName(Encoding.htmlOnlyDecode(download[0]));
                link.setAvailable(true);
                link.setMimeHint(CompiledFiletypeFilter.ArchiveExtensions.ZIP);
                link.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, currentPath);
                link._setFilePackage(fp);
                decryptedLinks.add(link);
            }
        }
        return decryptedLinks;
    }
}
