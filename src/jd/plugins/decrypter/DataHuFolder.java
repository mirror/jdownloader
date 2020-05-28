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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "data.hu" }, urls = { "https?://[\\w\\.]*?data\\.hu/dir/([0-9a-z]+)" })
public class DataHuFolder extends PluginForDecrypt {
    public DataHuFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.setCookie("http://data.hu", "lang", "en");
        br.getPage(parameter);
        if (br.containsHTML("class=\"error alert alert-danger\"|>Sajnos ez a megosztás már megszűnt")) {
            logger.info("Link offline: " + parameter);
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        /** Password protected folders --> 2020-05-28: This doesn't work anymore */
        // if (br.containsHTML("Kérlek add meg a jelszót\\!<")) {
        // for (int i = 0; i <= 3; i++) {
        // final String passCode = Plugin.getUserInput("Enter password for: " + parameter, param);
        // br.postPage(parameter, "mappa_pass=" + Encoding.urlEncode(passCode));
        // if (br.containsHTML(">Hibás jelszó")) {
        // continue;
        // }
        // break;
        // }
        // if (br.containsHTML(">Hibás jelszó")) {
        // throw new DecrypterException(DecrypterException.PASSWORD);
        // }
        // }
        String nextpage = null;
        do {
            String[] links = br.getRegex("(https?://[\\w\\.]*?data\\.hu/get/\\d+/[^<>\"\\']+)").getColumn(0);
            String[] folders = br.getRegex("(https?://[\\w\\.]*?data\\.hu/dir/[0-9a-z]+)").getColumn(0);
            if ((links == null || links.length == 0) && (folders == null || folders.length == 0)) {
                break;
            }
            if (links != null && links.length != 0) {
                for (String dl : links) {
                    decryptedLinks.add(createDownloadlink(dl));
                }
            }
            final String currentFolderID = new Regex(parameter, "data\\.hu/(dir/.+)").getMatch(0);
            if (folders != null && folders.length != 0) {
                for (String folderlink : folders) {
                    if (!folderlink.contains(currentFolderID)) {
                        decryptedLinks.add(createDownloadlink(folderlink));
                    }
                }
            }
            nextpage = br.getRegex("class=\"next_page_link\" href=\"(\\?page=\\d+)\"").getMatch(0);
            if (nextpage != null) {
                br.getPage(nextpage);
                /* Decode json answer */
                br.getRequest().setHtmlCode(PluginJSonUtils.unescape(br.toString()));
            }
        } while (!this.isAbort() && nextpage != null);
        if (decryptedLinks.size() == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}