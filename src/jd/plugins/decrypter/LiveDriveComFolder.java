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
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "livedrive.com" }, urls = { "http://[a-z0-9]+\\.livedrive\\.com/(I|i)tem/\\d+" }, flags = { 0 })
public class LiveDriveComFolder extends PluginForDecrypt {

    public LiveDriveComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        // Single link or folder
        if (parameter.matches("http://[a-z0-9]+\\.livedrive\\.com/item/[a-z0-9]{32}")) {
            decryptedLinks.add(createDownloadlink(parameter.replace("livedrive.com/", "livedrivedecrypter.com/")));
        } else {
            br.getPage(parameter);
            String liveDriveUrlUserPart = new Regex(parameter, "(.*?)\\.livedrive\\.com").getMatch(0);
            liveDriveUrlUserPart = liveDriveUrlUserPart.replaceAll("(http://|www\\.)", "");
            if (br.containsHTML("Item not found</span>")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            final String[][] folders = br.getRegex("<div class=\"file\\-item\\-container\" name=\"([^<>\"]*?)\" data=\"([a-z0-9]{32})\"").getMatches();
            for (final String[] folderinfo : folders) {
                Browser br2 = br.cloneBrowser();
                br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br2.getPage("http://" + liveDriveUrlUserPart + ".livedrive.com/Files/FileList?fileId=" + folderinfo[1] + "&pageNo=1&viewMode=1&_=" + System.currentTimeMillis());
                // Regex those information
                String[][] fileInformation = br2.getRegex("name=\"([^<>\"]*?)\" data=\"([a-z0-9]{32})\"").getMatches();
                if (fileInformation == null || fileInformation.length == 0) {
                    logger.warning("fileInformation not found for link: " + parameter);
                    return null;
                }
                final FilePackage thisFoldername = FilePackage.getInstance();
                thisFoldername.setName(folderinfo[0]);
                for (final String dlinfo[] : fileInformation) {
                    final String ID = dlinfo[1];
                    final String filename = dlinfo[0];
                    final DownloadLink theFinalLink = createDownloadlink("http://" + liveDriveUrlUserPart + ".livedrivedecrypted.com/item/" + ID);
                    theFinalLink.setAvailable(true);
                    theFinalLink.setFinalFileName(filename);
                    theFinalLink._setFilePackage(thisFoldername);
                    decryptedLinks.add(theFinalLink);
                }
            }
        }
        return decryptedLinks;
    }

}
