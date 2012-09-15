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

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bitbonus.com" }, urls = { "http://(www\\.)?bitbonus\\.com/download/[A-Za-z0-9\\-]+" }, flags = { 0 })
public class BitBonusComDecrypt extends PluginForDecrypt {

    public BitBonusComDecrypt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static AtomicBoolean LIMITREACHED  = new AtomicBoolean(false);
    public static final String   FILENAMEREGEX = "Name:[\t\n\r ]+<strong>([^<>\"]+)</strong><br";

    /** Handling similar to FilesMonsterDecrypter */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString().replace("www.", "");
        final URLConnectionAdapter con = br.openGetConnection(parameter);
        if (con.getResponseCode() == 500) {
            DownloadLink finalOne = createDownloadlink(parameter.replace("bitbonus.com/", "bitbonusdecrypted.com/"));
            finalOne.setAvailable(false);
            decryptedLinks.add(finalOne);
            return decryptedLinks;
        }
        br.followConnection();
        /** File offline? */
        if (br.containsHTML("(>Internal Server Error|Please notify the webmaster if you believe there is a problem|<title>BitBonus\\.com</title>)")) {
            DownloadLink finalOne = createDownloadlink(parameter.replace("bitbonus.com/", "bitbonusdecrypted.com/"));
            finalOne.setAvailable(false);
            decryptedLinks.add(finalOne);
            return decryptedLinks;
        }
        String fname = br.getRegex(FILENAMEREGEX).getMatch(0);
        String fsize = getSize(br);
        String[] decryptedStuff = getTempLinks(br);
        if (LIMITREACHED.get() == false) {
            if (decryptedStuff == null || decryptedStuff.length == 0) {
                logger.warning("BitBonusComDecrypt failed while decrypting link:" + parameter);
                return null;
            }
            String theImportantPartOfTheMainLink = new Regex(parameter, "bitbonus\\.com/download/(.+)").getMatch(0);
            for (String fileInfo : decryptedStuff) {
                String filename = new Regex(fileInfo, "\"name\":\"(.*?)\"").getMatch(0);
                String filesize = new Regex(fileInfo, "\"size\":(\")?(\\d+)").getMatch(1);
                String filelinkPart = new Regex(fileInfo, "\"dlcode\":\"(.*?)\"").getMatch(0);
                if (filename == null || filesize == null || filelinkPart == null || filename.length() == 0 || filesize.length() == 0 || filelinkPart.length() == 0) {
                    logger.warning("BitBonusComDecrypt failed while decrypting link:" + parameter);
                    return null;
                }
                String dllink = "http://bitbonusdecrypted.com/download/" + theImportantPartOfTheMainLink + "/free/2/" + filelinkPart;
                DownloadLink finalOne = createDownloadlink(dllink);
                finalOne.setFinalFileName(Encoding.htmlDecode(filename));
                finalOne.setDownloadSize(Integer.parseInt(filesize));
                finalOne.setAvailable(true);
                finalOne.setProperty("origfilename", filename);
                finalOne.setProperty("origsize", filesize);
                finalOne.setProperty("mainlink", parameter);
                decryptedLinks.add(finalOne);
            }
        } else {
            logger.info("postThat is null, probably limit reached, adding only the premium link...");
        }
        DownloadLink thebigone = createDownloadlink(parameter.replace("bitbonus.com/", "bitbonusdecrypted.com/"));
        if (fname != null && fsize != null) {
            thebigone.setName(Encoding.htmlDecode(fname));
            thebigone.setDownloadSize(SizeFormatter.getSize(fsize));
            thebigone.setAvailable(true);
        }
        thebigone.setProperty("PREMIUMONLY", "true");
        decryptedLinks.add(thebigone);
        /** All those links belong to the same file so lets make a package */
        if (fname != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fname.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    public static String[] getTempLinks(Browser br) throws IOException {
        String[] decryptedStuff = null;
        String postThat = br.getRegex("\"(http://bitbonus\\.com/download/[A-Za-z0-9\\-_]+/free/1/[A-Za-z0-9\\-_]+/?)\"").getMatch(0);
        if (postThat == null) postThat = br.getRegex("<form id=\\'slowdownload\\' method=\"post\" action=\"(http://bitbonus\\.com/[^<>\"]+)\"").getMatch(0);
        if (postThat != null) {
            br.postPage(postThat, "");
            String findOtherLinks = br.getRegex("reserve_ticket\\(\\'(/download/rft/.*?)\\'\\)").getMatch(0);
            if (findOtherLinks != null) {
                br.getPage("http://bitbonus.com" + findOtherLinks);
                decryptedStuff = br.getRegex("\\{(.*?)\\}").getColumn(0);
            }
        } else {
            LIMITREACHED.set(true);
        }
        return decryptedStuff;
    }

    public static String getSize(Browser br) {
        String fsize = br.getRegex("Size: <strong>([^<>\"]+)</strong>").getMatch(0);
        if (fsize == null) fsize = br.getRegex("<span class=\"small\">([^<>\"]+)</span>").getMatch(0);
        return fsize;
    }
}
