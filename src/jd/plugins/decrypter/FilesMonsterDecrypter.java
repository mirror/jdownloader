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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesmonster.comDecrypt" }, urls = { "http://(www\\.)?filesmonster\\.com/(download\\.php\\?id=[A-Za-z0-9_-]+|dl/.*?/free/)" }, flags = { 0 })
public class FilesMonsterDecrypter extends PluginForDecrypt {

    public FilesMonsterDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static final String FILENAMEREGEX = "\">File name:</td>[\t\n\r ]+<td>(.*?)</td>";
    public static final String FILESIZEREGEX = "\">File size:</td>[\t\n\r ]+<td>(.*?)</td>";
    private static String      FAILED        = null;

    /** Handling similar to BitBonusComDecrypt */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setReadTimeout(3 * 60 * 1000);
        br.setFollowRedirects(false);
        String parameter = param.toString();
        String fid = new Regex(parameter, "filesmonster\\.com/dl/(.*?)/free/").getMatch(0);
        if (fid != null) parameter = "http://filesmonster.com/download.php?id=" + fid;
        br.getPage(parameter);
        // Link offline
        if (br.containsHTML("(>File was deleted by owner or it was deleted for violation of copyrights<|>File not found<|>The link could not be decoded<)")) {
            final DownloadLink finalOne = createDownloadlink(parameter.replace("filesmonster.com", "filesmonsterdecrypted.com"));
            finalOne.setAvailable(false);
            decryptedLinks.add(finalOne);
            return decryptedLinks;
        }
        // Advertising link
        if (br.containsHTML(">the file can be accessed at the")) {
            final DownloadLink finalOne = createDownloadlink(parameter.replace("filesmonster.com", "filesmonsterdecrypted.com"));
            finalOne.setAvailable(false);
            decryptedLinks.add(finalOne);
            return decryptedLinks;
        }
        String fname = br.getRegex(FILENAMEREGEX).getMatch(0);
        String fsize = br.getRegex(FILESIZEREGEX).getMatch(0);

        String[] decryptedStuff = null;
        final String postThat = br.getRegex("\"(/dl/.*?)\"").getMatch(0);
        if (postThat != null) {
            br.postPage("http://filesmonster.com" + postThat, "");
            final String findOtherLinks = br.getRegex("'(/dl/rft/.*?)\\'").getMatch(0);
            if (findOtherLinks != null) {
                br.getPage("http://filesmonster.com" + findOtherLinks);
                decryptedStuff = br.getRegex("\\{(.*?)\\}").getColumn(0);
            }
        } else {
            FAILED = "Limit reached";
        }
        if (br.containsHTML(">You need Premium membership to download files larger than")) FAILED = "There are no free downloadlinks";

        if (FAILED == null) {
            if (decryptedStuff == null || decryptedStuff.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            String theImportantPartOfTheMainLink = new Regex(parameter, "filesmonster\\.com/download\\.php\\?id=(.+)").getMatch(0);
            for (String fileInfo : decryptedStuff) {
                String filename = new Regex(fileInfo, "\"name\":\"(.*?)\"").getMatch(0);
                String filesize = new Regex(fileInfo, "\"size\":(\")?(\\d+)").getMatch(1);
                String filelinkPart = new Regex(fileInfo, "\"dlcode\":\"(.*?)\"").getMatch(0);
                if (filename == null || filesize == null || filelinkPart == null || filename.length() == 0 || filesize.length() == 0 || filelinkPart.length() == 0) {
                    logger.warning("FilesMonsterDecrypter failed while decrypting link:" + parameter);
                    return null;
                }
                String dllink = "http://filesmonsterdecrypted.com/dl/" + theImportantPartOfTheMainLink + "/free/2/" + filelinkPart;
                final DownloadLink finalOne = createDownloadlink(dllink);
                finalOne.setFinalFileName(Encoding.htmlDecode(filename));
                finalOne.setDownloadSize(Integer.parseInt(filesize));
                finalOne.setAvailable(true);
                finalOne.setProperty("origfilename", filename);
                finalOne.setProperty("origsize", filesize);
                finalOne.setProperty("mainlink", parameter);
                decryptedLinks.add(finalOne);
            }
        } else {
            logger.info("Failed to get free links because: " + FAILED);
        }
        final DownloadLink thebigone = createDownloadlink(parameter.replace("filesmonster.com", "filesmonsterdecrypted.com"));
        if (fname != null && fsize != null) {
            thebigone.setFinalFileName(Encoding.htmlDecode(fname.trim()));
            thebigone.setDownloadSize(SizeFormatter.getSize(fsize));
            thebigone.setAvailable(true);
        }
        thebigone.getLinkStatus().setStatusText(JDL.L("plugins.hoster.filesmonstercom.only4premium", "Only downloadable via premium"));
        thebigone.setProperty("PREMIUMONLY", "true");
        decryptedLinks.add(thebigone);
        /** All those links belong to the same file so lets make a package */
        if (fname != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fname.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
