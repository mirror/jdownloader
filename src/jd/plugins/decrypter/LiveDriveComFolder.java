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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "livedrive.com" }, urls = { "https?://([a-z0-9]+\\.livedrive\\.com/((I|i)tem|files)/\\d+|[a-z0-9]+\\.livedrivefolderlink\\.com/[a-z0-9]{32})" }, flags = { 0 })
public class LiveDriveComFolder extends PluginForDecrypt {

    public LiveDriveComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String FOLDERLINK = "http://[a-z0-9]+\\.livedrivefolderlink\\.com/[a-z0-9]{32}";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // Prefer http for Stable compatibility
        String parameter = param.toString().replace("https://", "http://");
        if (parameter.matches(FOLDERLINK)) {
            parameter = parameter.replace("livedrivefolderlink.com/", "livedrive.com/");
            final Regex paraminfo = new Regex(parameter, "http://([a-z0-9]+)\\.livedrive\\.com/([a-z0-9]{32})");
            // br.getPage("http://zenpharaohs.livedrive.com/Item/2390130");
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getPage("http://" + paraminfo.getMatch(0) + ".livedrive.com/Files/FileList?fileId=" + paraminfo.getMatch(1) + "&pageNo=1&viewMode=1&_=" + System.currentTimeMillis());
        } else {
            parameter = parameter.replace("/files/", "/item/");
            br.getPage(parameter);
        }
        // Single link or folder
        if (parameter.matches("http://[a-z0-9]+\\.livedrive\\.com/item/[a-z0-9]{32}")) {
            decryptedLinks.add(createDownloadlink(parameter.replace("livedrive.com/", "livedrivedecrypter.com/")));
        } else {
            String liveDriveUrlUserPart = new Regex(parameter, "(.*?)\\.livedrive\\.com").getMatch(0);
            liveDriveUrlUserPart = liveDriveUrlUserPart.replaceAll("(http://|www\\.)", "");
            if (br.containsHTML("Item not found</span>")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            final String[][] folders = br.getRegex("<div class=\"file\\-item\\-container\" name=\"([^<>\"]*?)\" data=\"([a-z0-9]{32})\" aid=\"\\d+\" ondblclick=\"Spinner\\(\\);\\$\\(\\'#FileList\\'\\)\\.load").getMatches();
            final String[][] files = br.getRegex("<div class=\"file\\-item\\-container\" name=\"([^<>\"]*?)\" data=\"([a-z0-9]{32})\" aid=\"\\d+\" rel=\"/Files/ToolTipView\\?fileId=").getMatches();
            if ((folders == null || folders.length == 0) && (files == null || files.length == 0)) {
                logger.warning("Decrypter broken for link: " + parameter);
                return decryptedLinks;
            }
            if (files != null && files.length != 0) {
                for (final String finfo[] : files) {
                    final String filename = finfo[0];
                    final String ID = finfo[1];
                    final DownloadLink theFinalLink = createDownloadlink("http://" + liveDriveUrlUserPart + ".livedrivedecrypted.com/item/" + ID);
                    theFinalLink.setAvailable(true);
                    theFinalLink.setFinalFileName(Encoding.htmlDecode(filename.trim()));
                    decryptedLinks.add(theFinalLink);
                }
            }
            if (folders != null && folders.length != 0) {
                for (final String[] folderinfo : folders) {
                    final String ID = folderinfo[1];
                    final DownloadLink theFinalLink = createDownloadlink("http://" + liveDriveUrlUserPart + ".livedrivefolderlink.com/" + ID);
                    decryptedLinks.add(theFinalLink);
                }
            }
            if (decryptedLinks == null || decryptedLinks.size() == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}