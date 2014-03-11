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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "copy.com" }, urls = { "https?://(www\\.)?copy\\.com/s/[A-Za-z0-9]+(/[^<>\"/]+)?" }, flags = { 0 })
public class CopyComDecrypter extends PluginForDecrypt {

    public CopyComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String INVALIDLINKS = "https?://(www\\.)?copy\\.com/(price|about|barracuda|bigger|install|developer|browse|home|auth|signup|policies)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final DownloadLink offline = createDownloadlink("http://copydecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
        offline.setAvailable(false);
        offline.setProperty("offline", true);
        offline.setName(new Regex(parameter, "copy\\.com/(.+)").getMatch(0));
        if (parameter.matches(INVALIDLINKS)) {
            decryptedLinks.add(offline);
            return decryptedLinks;
        }

        br.getPage(parameter);
        if (br.containsHTML(">You\\&rsquo;ve found a page that doesn\\&rsquo;t exist")) {
            decryptedLinks.add(offline);
            return decryptedLinks;
        }

        final String linktext = br.getRegex("\"share\":null,\"children\":\\[(.*?)\\],\"children_count\":").getMatch(0);
        if (linktext == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String[] links = linktext.split("\\},\\{");
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleinfo : links) {
            final DownloadLink dl = createDownloadlink("http://copydecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
            final String filesize = getJson("size", singleinfo);
            String filename = getJson("name", singleinfo);
            String url = getJson("url", singleinfo);
            if (filesize == null || filename == null || url == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            filename = Encoding.htmlDecode(filename.trim());
            url = url.replace("\\", "");
            dl.setDownloadSize(Long.parseLong(filesize));
            dl.setFinalFileName(filename);
            dl.setProperty("plain_name", filename);
            dl.setProperty("plain_size", filesize);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("specified_link", url);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }

        return decryptedLinks;
    }

    private String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":([0-9\\.]+)").getMatch(0);
        if (result == null) result = new Regex(source, "\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        return result;
    }

}
