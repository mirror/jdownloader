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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hightail.com" }, urls = { "http(s)?://(www\\.)?yousendit\\.com/download/[A-Za-z0-9]+" }, flags = { 0 })
public class HighTailComDecrypter extends PluginForDecrypt {

    public HighTailComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        final String[] linkInfo = br.getRegex("<div class=\"fileContainer grid\"(.*?)<div class=\"downloadFiletype grid\"").getColumn(0);
        if (linkInfo != null && linkInfo.length != 0) {
            // Multiple links
            for (final String singleLink : linkInfo) {
                final DownloadLink dl = createDownloadlink("http://yousenditdecrypted.com/download/" + System.currentTimeMillis() + new Random().nextInt(100000));
                final String filename = new Regex(singleLink, "class=\"downloadFilename grid\"><span>([^<>\"]*?)</span>").getMatch(0);
                final String filesize = new Regex(singleLink, "class=\"downloadFilesize grid\">([^<>\"]*?)</div>").getMatch(0);
                final String fileurl = new Regex(singleLink, "file_url=\"([A-Za-z0-9]+)\"").getMatch(0);
                if (filename == null || filesize == null || fileurl == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                dl.setName(Encoding.htmlDecode(filename.trim()));
                dl.setDownloadSize(SizeFormatter.getSize(filesize));
                dl.setProperty("directname", Encoding.htmlDecode(filename.trim()));
                dl.setProperty("directsize", filesize);
                dl.setProperty("fileurl", fileurl);
                dl.setProperty("mainlink", parameter);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        } else {
            // Single link
            final DownloadLink dl = createDownloadlink(parameter.replace("yousendit.com/", "yousenditdecrypted.com/"));
            dl.setProperty("mainlink", parameter);
            if (br.containsHTML("Download link is invalid|Download link is invalid|>Access has expired<")) {
                dl.setProperty("offline", true);
                dl.setAvailable(false);
            } else {
                final String filename = br.getRegex("id=\"downloadSingleFilename\">([^<>\"]*?)</span>").getMatch(0);
                final String filesize = br.getRegex("id=\"downloadSingleFilesize\">([^<>\"]*?)<span>").getMatch(0);
                if (filename != null && filesize != null) {
                    dl.setName(Encoding.htmlDecode(filename.trim()));
                    dl.setDownloadSize(SizeFormatter.getSize(filesize));
                    dl.setProperty("directname", Encoding.htmlDecode(filename.trim()));
                    dl.setProperty("directsize", filesize);
                    dl.setAvailable(true);
                }
            }
            decryptedLinks.add(dl);

        }

        return decryptedLinks;
    }

}
