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
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.setCookie("https://" + this.getHost(), "lang", "en");
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)class=\"error alert alert-danger\"|>Sajnos ez a megosztás már megszűnt")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
        final String currentFolderID = new Regex(param.getCryptedUrl(), "(?i)data\\.hu/(dir/.+)").getMatch(0);
        String nextpage = null;
        String folderName = br.getRegex("id=\"main_big_content\">\\s*<h1>([^<]+)</h1>").getMatch(0);
        FilePackage fp = null;
        if (folderName != null) {
            folderName = Encoding.htmlDecode(folderName).trim();
            fp = FilePackage.getInstance();
            fp.setName(folderName);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        int page = 1;
        do {
            final String[] links = br.getRegex("(https?://[^/]+/get/\\d+/[^<>\"\\']+)").getColumn(0);
            final String[] folders = br.getRegex("(https?://[^/]+/dir/[0-9a-z]+)").getColumn(0);
            if (links.length == 0 && folders.length == 0) {
                break;
            }
            if (links.length > 0) {
                for (final String fileURL : links) {
                    final DownloadLink file = createDownloadlink(fileURL);
                    if (fp != null) {
                        file._setFilePackage(fp);
                    }
                    ret.add(file);
                    distribute(file);
                }
            }
            if (folders.length > 0) {
                for (final String folderURL : folders) {
                    if (!folderURL.contains(currentFolderID)) {
                        final DownloadLink subfolder = createDownloadlink(folderURL);
                        ret.add(subfolder);
                        distribute(subfolder);
                    }
                }
            }
            logger.info("Crawled page " + page + " | Found items so far: " + ret.size());
            page++;
            nextpage = br.getRegex("class=\"next_page_link\" href=\"(\\?page=" + page + ")\"").getMatch(0);
            if (nextpage == null) {
                logger.info("Stopping because: Reached last page");
                break;
            }
            br.getPage(nextpage);
            /* Small workaround: Decode json answer */
            br.getRequest().setHtmlCode(PluginJSonUtils.unescape(br.toString()));
        } while (!this.isAbort());
        if (ret.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}