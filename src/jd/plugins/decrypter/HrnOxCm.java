//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 17655 $", interfaceVersion = 2, names = { "hornoxe.com" }, urls = { "http://(www\\.)?hornoxe\\.com/(?!category)[a-z0-9\\-]+/" }, flags = { 0 })
public class HrnOxCm extends PluginForDecrypt {

    public HrnOxCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String INVALIDLINKS = "http://(www\\.)?hornoxe\\.com/(picdumps|sonstiges|eigener\\-content|comics\\-cartoons|amazon|witze|fun\\-clips|fun\\-bilder|sexy|kurzfilme|bastelstunde|games|fun\\-links|natur\\-technik|feed)/";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String parameter = param.toString();
        if (parameter.matches(INVALIDLINKS)) {
            logger.info("Link invalid: " + parameter);
            return decryptedLinks;
        }
        br.getPage(parameter);

        if (br.containsHTML(">Seite nicht gefunden<") || br.containsHTML("No htmlCode read") || br.containsHTML(">404 \\- Not Found<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String pageName = br.getRegex("og:title\" content=\"(.*?)\" />").getMatch(0);
        if (pageName == null) pageName = br.getRegex("<title>(.*?) \\- Hornoxe\\.com</title>").getMatch(0);
        if (pageName == null) {
            logger.warning("Decrypter failed for link: " + parameter);
            return null;
        }
        pageName = Encoding.htmlDecode(pageName.trim());

        // Check if there are embedded links
        String externID = br.getRegex("\"(//(www\\.)?youtube\\.com/embed/[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http:" + externID));
        }

        // Check if we have a single video
        final String file = br.getRegex("file\":\"(https?://videos\\.hornoxe\\.com/[^\"]+)").getMatch(0);
        if (file != null) {
            final DownloadLink vid = createDownloadlink(file.replace("hornoxe.com", "hornoxedecrypted.com"));
            vid.setFinalFileName(pageName + file.substring(file.lastIndexOf(".")));
            vid.setProperty("Referer", parameter);
            decryptedLinks.add(vid);
            return decryptedLinks;
        }

        // Check if we have a picdump
        String[] urls = null;
        if (parameter.contains("-gifdump")) {
            urls = br.getRegex("\\'(http://gifdumps\\.hornoxe\\.com/gifdump[^<>\"]*?)\\'").getColumn(0);
        } else {
            urls = br.getRegex("\"(http://(www\\.)?hornoxe\\.com/wp\\-content/picdumps/[^<>\"]*?)\"").getColumn(0);
            if (urls == null || urls.length == 0) urls = br.getRegex("\"(https?://(www\\.)hornoxe\\.com/wp\\-content/uploads/(?!thumb)[^<>\"]+)\"").getColumn(0);
        }
        if (urls != null && urls.length != 0) {
            String title = br.getRegex("<meta property=\"og\\:title\" content=\"(.*?)\" \\/>").getMatch(0);
            FilePackage fp = null;
            if (title != null) {
                fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(title.trim()));
                fp.addLinks(decryptedLinks);
            }

            add(decryptedLinks, urls, fp);
            String[] pageqs = br.getRegex("\"page-numbers\" href=\"(.*?nggpage\\=\\d+)").getColumn(0);

            for (String page : pageqs) {
                br.getPage(page);

                if (parameter.contains("-gifdump")) {
                    urls = br.getRegex("\\'(http://gifdumps\\.hornoxe\\.com/gifdump[^<>\"]*?)\\'").getColumn(0);
                } else {
                    urls = br.getRegex("\"(http://(www\\.)?hornoxe\\.com/wp\\-content/picdumps/[^<>\"]*?)\"").getColumn(0);
                    if (urls == null || urls.length == 0) urls = br.getRegex("\"(https?://(www\\.)hornoxe\\.com/wp\\-content/uploads[^<>\"]+)\"").getColumn(0);
                }
                add(decryptedLinks, urls, fp);
            }
            return decryptedLinks;
        }

        // Check if it's an image
        final String image = br.getRegex("\"(https?://(www\\.)hornoxe\\.com/wp\\-content/uploads[^<>\"]+)\"").getMatch(0);
        if (image != null) {
            final DownloadLink img = createDownloadlink("directhttp://" + image);
            img.setFinalFileName(pageName + image.substring(image.lastIndexOf(".")));
            decryptedLinks.add(img);
            return decryptedLinks;
        }

        return decryptedLinks;
    }

    private void add(ArrayList<DownloadLink> decryptedLinks, String[] urls, FilePackage fp) {

        for (final String url : urls) {
            if (url.contains("fliege.gif")) continue;
            DownloadLink link = createDownloadlink("directhttp://" + url);
            fp.add(link);
            decryptedLinks.add(link);
            try {
                distribute(link);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}