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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "livedrive.com" }, urls = { "http://[\\w\\.]*?qriist\\.livedrive\\.com/(frameset\\.php\\?path=/)?files/\\d+" }, flags = { 0 })
public class LiveDriveComFolder extends PluginForDecrypt {

    public LiveDriveComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String DECRYPTEDLINKPART = "http://qriist.decryptedlivedrive.com/frameset.php?path=/files";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("/frameset.php?path=", "");
        br.getPage(parameter);
        if (br.containsHTML("Item not found</span>")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String fpName = br.getRegex("class=\"data_holder\">.*?'s Livedrive - (.*?)</span>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<title>.*?'s Livedrive - (.*?)</title>").getMatch(0);
        // Is it a single file or a folder ?
        if (br.containsHTML("onclick=\"fd\\.downloadItem\\(")) {
            String pathID = new Regex(parameter, "files/(\\d+)").getMatch(0);
            String ID = br.getRegex("Type=Query\\&Method=GetThumbnail\\&ID=(.*?)'\\)").getMatch(0);
            if (ID == null) {
                ID = br.getRegex("ParentId=(.*?)\"").getMatch(0);
            }
            if (ID == null || pathID == null) {
                logger.warning("Failed on single file, link = " + parameter);
                return null;
            }
            DownloadLink theFinalLink = createDownloadlink(DECRYPTEDLINKPART + "/" + pathID);
            theFinalLink.setProperty("DOWNLOADID", ID);
            decryptedLinks.add(theFinalLink);
        } else {
            String thereIsThisSecretID = br.getRegex("id=\"thisid\" class=\"data_holder\">(.*?)</span>").getMatch(0);
            if (thereIsThisSecretID == null) {
                logger.warning("Couldn't find the secret id for link: " + parameter);
                return null;
            }
            Browser br2 = br.cloneBrowser();
            br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br2.postPage("http://qriist.livedrive.com/WebService/AjaxHandler.ashx", "Method=GetFileList&Page=1&ParentID=" + thereIsThisSecretID);
            // Regex those information
            String[] fileInformation = br2.getRegex("(\\{\".*?\"\\})").getColumn(0);
            if (fileInformation == null || fileInformation.length == 0) {
                logger.warning("fileInformation not found for link: " + parameter);
                return null;
            }
            for (String dl : fileInformation) {
                String ID = new Regex(dl, "ID\":\"(.*?)\"").getMatch(0);
                String filename = new Regex(dl, "\"Name\":\"(.*?)\"").getMatch(0);
                String filesize = new Regex(dl, "\"Size\":\"(\\d+)\"").getMatch(0);
                String filetype = new Regex(dl, "\"FileType\":\"(.*?)\"").getMatch(0);
                String fileOrFolderPath = new Regex(dl, "\"Path\":\"(/\\d+)\"").getMatch(0);
                if (ID == null || filename == null || filetype == null || fileOrFolderPath == null) {
                    logger.warning("A part of the fileinformation couldn't be found for link: " + parameter);
                    return null;
                }
                // Do we have a folder ? Just add it.
                // Folders will go back into this decrypter so we don't change
                // the hostname to match the regex of the hosterplugin for
                // livedrive
                // Do we have a file ? Add it with all given information
                if (filetype.equals("Folder")) {
                    decryptedLinks.add(createDownloadlink("http://qriist.livedrive.com/frameset.php?path=/files" + fileOrFolderPath));
                } else {
                    DownloadLink theFinalLink = createDownloadlink(DECRYPTEDLINKPART + fileOrFolderPath);
                    theFinalLink.setName(filename);
                    theFinalLink.setDownloadSize(Long.parseLong(filesize));
                    theFinalLink.setProperty("DOWNLOADID", ID);
                    theFinalLink.setAvailable(true);
                    decryptedLinks.add(theFinalLink);
                }
            }
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
