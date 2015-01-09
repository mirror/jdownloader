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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fora.tv" }, urls = { "http://(www\\.)?fora\\.tv/\\d{4}/\\d{2}/\\d{2}/[A-Za-z0-9\\-_]+|http://library\\.fora\\.tv/program_landing\\.php\\?year=\\d{4}\\&month=\\d{2}\\&day=\\d{2}\\&title=[^<>\"/]+" }, flags = { 0 })
public class ForaTvDecrypt extends PluginForDecrypt {

    public ForaTvDecrypt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("There are no downloads for this program<")) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0));
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        String title = br.getRegex("var full_program_title = \"([^<>\"]*?)\";").getMatch(0);
        final String videoslist = br.getRegex("<ul id=\"downloads_list\">(.*?)</ul>").getMatch(0);
        if (title == null || videoslist == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String[] links = videoslist.split("</li>[\t\n\r ]+<li");
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        title = Encoding.htmlDecode(title).trim();
        int counter = 1;
        for (final String slinkinfo : links) {
            String downloadlink = new Regex(slinkinfo, "\"(/download[^<>\"]*?)\"").getMatch(0);
            final String type = new Regex(slinkinfo, "itemprop=\"name\">(Video|Audio)</span>").getMatch(0);
            final String size = new Regex(slinkinfo, "itemprop=\"contentSize\">([^<>\"]*?)</span>").getMatch(0);
            if (downloadlink == null || type == null || size == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            downloadlink = "directhttp://http://library.fora.tv/" + Encoding.htmlDecode(downloadlink);
            final DownloadLink dl = createDownloadlink(downloadlink);
            String ext;
            if (type.equals("Video")) {
                ext = ".mp4";
            } else {
                ext = ".mp3";
            }
            dl.setFinalFileName(title + "_" + counter + ext);
            /* This would only be a bad workaround for http://svn.jdownloader.org/issues/61512 */
            // dl.setForcedFileName(title + "_" + counter + ext);
            dl.setDownloadSize(SizeFormatter.getSize(size));
            dl.setAvailable(true);
            decryptedLinks.add(dl);
            counter++;
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

}
