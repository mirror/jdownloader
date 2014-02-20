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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tusfiles.net" }, urls = { "http://(www\\.)?(tusfiles\\.net/go/[a-z0-9]{12}/|j\\-b\\.tusfil\\.es/[A-Z0-9]+)" }, flags = { 0 })
public class TusFilesNet extends PluginForDecrypt {

    public TusFilesNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_SHORT = "http://(www\\.)?j\\-b\\.tusfil\\.es/[A-Z0-9]+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (parameter.matches(TYPE_SHORT)) {
            final String finallink = br.getRegex("class=\"bttn button\"><a href=\"(http[^<>\"]*?)\"").getMatch(0);
            if (finallink == null || finallink.matches(TYPE_SHORT)) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            if (br.containsHTML(">No such folder<")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            final String folderid = new Regex(parameter, "([a-z0-9]{12})/$").getMatch(0);
            int maxPage = 1;
            final String[] pages = br.getRegex("href=\"/go/" + folderid + "/(\\d+)/\">\\d+</a>").getColumn(0);
            if (pages != null && pages.length != 0) {
                for (final String pagenum : pages) {
                    if (Integer.parseInt(pagenum) > maxPage) maxPage = Integer.parseInt(pagenum);
                }
            }
            for (int i = 1; i <= maxPage; i++) {
                logger.info("Decrypting page " + i + " of " + maxPage);
                try {
                    if (this.isAbort()) {
                        logger.info("Decryption aborted by user on page " + i + " of " + maxPage);
                    }
                } catch (final Throwable e) {
                    // Not available in old 0.9.581 Stable
                }
                if (i > 1) {
                    br.getPage("http://www.tusfiles.net/go/" + folderid + "/" + i + "/");
                    // Site bug
                    if (br.containsHTML(">No such folder<")) {
                        logger.info("Got all links, stopping...");
                        break;
                    }
                }
                final String[] folders = br.getRegex("\"(http://(www\\.)?tusfiles\\.net/go/[a-z0-9]{12}/)\"").getColumn(0);
                final String[] links = br.getRegex("\"(http://(www\\.)?tusfiles\\.net/[a-z0-9]{12})\"").getColumn(0);
                if ((links == null || links.length == 0) && (folders == null || folders.length == 0)) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                if (links != null && links.length != 0) {
                    for (final String singleLink : links)
                        decryptedLinks.add(createDownloadlink(singleLink));
                }
                if (folders != null && folders.length != 0) {
                    for (final String folder : folders)
                        decryptedLinks.add(createDownloadlink(folder));
                }
            }
        }
        return decryptedLinks;
    }

}
