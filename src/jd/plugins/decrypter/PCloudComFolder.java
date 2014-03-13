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
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pcloud.com" }, urls = { "https?://(www\\.)?(my\\.pcloud\\.com/#page=publink\\&code=|pc\\.cd/)[A-Za-z0-9]+" }, flags = { 0 })
public class PCloudComFolder extends PluginForDecrypt {

    public PCloudComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String code = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);
        final DownloadLink main = createDownloadlink("http://pclouddecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
        main.setProperty("plain_code", code);
        main.setProperty("mainlink", parameter);

        prepBR();
        br.getPage("http://api.pcloud.com/showpublink?code=" + getFID(parameter));
        if (br.containsHTML("\"error\": \"Invalid link")) {
            main.setFinalFileName(new Regex(parameter, "copy\\.com/(.+)").getMatch(0));
            main.setAvailable(false);
            main.setProperty("offline", true);
            decryptedLinks.add(main);
            return decryptedLinks;
        }

        String folderName = getJson("name", br.toString());
        final String linktext = br.getRegex("contents\": \\[(.*?)\\][\t\r\n ]+\\}").getMatch(0);
        if (linktext == null || folderName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        folderName = Encoding.htmlDecode(folderName.trim());

        final String[] links = linktext.split("\\},[\t\n\r ]+\\{");
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        long totalSize = 0;
        for (final String singleinfo : links) {
            final DownloadLink dl = createDownloadlink("http://pclouddecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
            final String filesize = getJson("size", singleinfo);
            String filename = getJson("name", singleinfo);
            final String fileid = getJson("fileid", singleinfo);
            if (filesize == null || filename == null || fileid == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            filename = Encoding.htmlDecode(filename.trim());
            final long cursize = Long.parseLong(filesize);
            dl.setDownloadSize(cursize);
            totalSize += cursize;
            dl.setFinalFileName(filename);
            dl.setProperty("plain_name", filename);
            dl.setProperty("plain_size", filesize);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("plain_fileid", fileid);
            dl.setProperty("plain_code", code);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }

        if (decryptedLinks.size() > 1) {
            /* = all files (links) of the folder as .zip archive */
            final String main_name = folderName + ".zip";
            main.setFinalFileName(folderName);
            main.setProperty("plain_name", main_name);
            main.setProperty("plain_size", Long.toString(totalSize));
            main.setProperty("complete_folder", true);
            main.setProperty("plain_code", code);
            decryptedLinks.add(main);
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(folderName);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    private String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?([0-9\\.]+)").getMatch(1);
        if (result == null) result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?\"([^<>\"]*?)\"").getMatch(1);
        return result;
    }

    private String getFID(final String link) {
        return new Regex(link, "([A-Za-z0-9]+)$").getMatch(0);
    }

    private void prepBR() {
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept-Encoding", "gzip");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
        br.getHeaders().put("Accept-Charset", null);
    }

}
