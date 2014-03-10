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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "putlocker.com" }, urls = { "http://(www\\.)?firedrive\\.com/share/(F_[A-Z0-9]+|[A-Za-z0-9\\-]+)" }, flags = { 0 })
public class PutLockerComFolder extends PluginForDecrypt {

    public PutLockerComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_REAL_FOLDER = "http://(www\\.)?firedrive\\.com/share/F_[A-Z0-9]+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        String fpName = null;
        String[] ids = null;
        String passCode = null;
        if (parameter.matches(TYPE_REAL_FOLDER)) {
            br.getPage(parameter);
            if (br.containsHTML("class=\"removed_folder_image\"")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            if (br.containsHTML("id=\"file_password_container\"")) {
                for (int i = 1; i <= 3; i++) {
                    passCode = getUserInput("Password?", param);
                    br.postPage(br.getURL(), "item_pass=" + Encoding.urlEncode(passCode));
                    if (br.containsHTML("id=\"file_password_container\"")) continue;
                    break;
                }
                if (br.containsHTML("id=\"file_password_container\"")) throw new DecrypterException(DecrypterException.PASSWORD);
            }
            fpName = br.getRegex("class=\"public_title_left\">([^<>\"]*?)</div>").getMatch(0);
            ids = br.getRegex("public=\\'([A-Z0-9]+)\\'").getColumn(0);
        } else {
            br.getPage(parameter);
            ids = new Regex(parameter, "firedrive\\.com/share/(.+)").getMatch(0).split("-");
        }
        if (ids == null || ids.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String fid : ids) {
            final Regex finfo = br.getRegex("public=\\'" + fid + "\\' data\\-name=\"([^<>\"]*?)\" data\\-type=\"([^<>\"]*?)\" data\\-size=\"(\\d+)\"");
            final String filename = finfo.getMatch(0);
            final String filesize = finfo.getMatch(2);
            final DownloadLink dl = createDownloadlink("http://www.firedrive.com/file/" + fid);
            if (filename == null || filesize == null) {
                dl.setName(fid);
                dl.setAvailable(false);
            } else {
                dl.setName(Encoding.htmlDecode(filename.trim()));
                dl.setDownloadSize(Long.parseLong(filesize));
                dl.setAvailable(true);
            }
            if (passCode != null) dl.setProperty("pass", passCode);
            decryptedLinks.add(dl);
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}