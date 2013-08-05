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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "idrivesync.com" }, urls = { "https?://(www\\.)?idrivesync\\.com/share/view\\?k=[A-Za-z0-9]+" }, flags = { 0 })
public class IDriveSyncCom extends PluginForDecrypt {

    public IDriveSyncCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML(">Oops\\! Your Page cannot be found")) {
            final DownloadLink dl = createDownloadlink("http://idrivesyncdecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
            dl.setName(new Regex(parameter, "([A-Za-z0-9]+)§").getMatch(0));
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        final String fList = br.getRegex("\"file_list\":\\[(.*?)\\],").getMatch(0);
        if (fList == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String[] links = fList.split("\\}");
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String linkInfo : links) {
            final String filename = getJson("name", linkInfo);
            final String filesize = getJson("size", linkInfo);
            final String dlurl = getJson("url", linkInfo);
            if (filename == null || filesize == null || dlurl == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DownloadLink dl = createDownloadlink("http://idrivesyncdecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
            dl.setFinalFileName(Encoding.htmlDecode(filename.trim()));
            dl.setProperty("directfilename", Encoding.htmlDecode(filename.trim()));
            dl.setDownloadSize(SizeFormatter.getSize(filesize));
            dl.setProperty("directfilesize", filesize);
            dl.setProperty("dllink", "https://www.idrivesync.com" + dlurl.replace("\\", ""));
            dl.setProperty("mainlink", parameter);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }

    private String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) result = new Regex(source, "\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        return result;
    }
}
