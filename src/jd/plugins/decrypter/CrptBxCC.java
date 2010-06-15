//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cryptbox.cc" }, urls = { "http://[\\w\\.]*?.cryptbox\\.cc/ordner/[0-9a-zA-z]+" }, flags = { 0 })
public class CrptBxCC extends PluginForDecrypt {

    private static final String CAPTCHA_PATTERN = "api\\.recaptcha\\.net";
    private static final String CAPTCHA_WRONG_PATTERN = "Sie haben einen falschen Sicherheitscode eingegeben\\.";
    private static final String DOWNLINK_PATTERN = "<br><a href=\"(.*?)\"";
    private static final String DOWNLINK_PATTERN2 = "id=\"ff\" action=\"(.*?)\"";
    private static final String FIRSTALLURLS_PATTERN = "<table class=\"download\">[\r\n\t ]+<tr>[\r\n\t ]+<td>[\r\n\t ]+<a href='(http://.*?)'";
    private static final String SECONDALLURLS_PATTERN = "'(http://www\\.cryptbox\\.cc/go/.*?)'";
    private static final String FOLDER_NOT_FOUND_PATTERN = "<h3>Fehler!</h3>Dieser Ordner ex[ei]stiert nicht.";

    public CrptBxCC(PluginWrapper wrapper) {
        super(wrapper);
    }

    private ArrayList<DownloadLink> linksGoHere = new ArrayList<DownloadLink>();

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(parameter.toString());
        if (br.containsHTML(FOLDER_NOT_FOUND_PATTERN)) {
            logger.fine("No such folder.");
            return decryptedLinks;
        }
        boolean failed = true;
        if (br.containsHTML(CAPTCHA_PATTERN)) {
            for (int i = 0; i <= 3; i++) {
                PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                rc.parse();
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, parameter);
                rc.setCode(c);
                if (!br.containsHTML(CAPTCHA_WRONG_PATTERN)) {
                    failed = false;
                    break;
                }
                br.getPage(parameter.toString());
            }
            if (failed) throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        String folderId = new Regex(parameter.toString(), "cryptbox.cc/ordner/(.+)").getMatch(0);

        /*
         * TODO: Container Handling rechecked?
         */
        if (br.containsHTML(">DLC-Container")) {
            decryptedLinks = loadContainer("http://www.cryptbox.cc/container.php?ordner=" + folderId + "&type=dlc");
            if (decryptedLinks != null && !decryptedLinks.isEmpty()) return decryptedLinks;
        }
        br.getPage(parameter.toString());

        String[] pages = br.getRegex("seite=(\\d+)\"").getColumn(0);
        if (pages != null) {
            decryptTheStuff(pages.length, progress, folderId);
        } else {
            decryptTheStuff(0, progress, folderId);
        }
        return linksGoHere;
    }

    private void decryptTheStuff(int pages, ProgressController progress, String folderId) throws Exception {
        logger.info("Found " + pages + " pages.");
        int defectLinksCounter = 1;
        for (int i = 0; pages <= pages; i++) {
            logger.info("Working on page " + (i + 1) + " of " + pages);
            if (i != 0) br.getPage("http://www.cryptbox.cc/ordner/" + folderId + "&seite=" + (i + 1));
            String[] links = br.getRegex(FIRSTALLURLS_PATTERN).getColumn(0);
            if (links == null || links.length == 0) {
                links = br.getRegex(SECONDALLURLS_PATTERN).getColumn(0);
            }
            if (links == null || links.length == 0) throw new DecrypterException("Decrypter broken!");
            progress.setRange(links.length);
            for (String link : links) {
                br.getPage(link);
                String finallink = br.getRegex(DOWNLINK_PATTERN).getMatch(0);
                if (finallink == null) finallink = br.getRegex(DOWNLINK_PATTERN2).getMatch(0);
                if (finallink == null) {
                    if (br.containsHTML("<frame src=\"")) {
                        logger.info("Found " + defectLinksCounter + " defect links.");
                        defectLinksCounter++;
                        progress.increase(1);
                        continue;
                    }
                    throw new DecrypterException("Decrypter broken!");
                }
                linksGoHere.add(createDownloadlink(finallink));
                progress.increase(1);
            }
        }
    }

    private ArrayList<DownloadLink> loadContainer(String dlclink) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = null;
        Browser brc = br.cloneBrowser();
        URLConnectionAdapter con = null;

        try {
            con = brc.openGetConnection(dlclink);
            if (con.getResponseCode() == 200) {
                File file = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
                brc.downloadConnection(file, con);
                if (file != null && file.exists() && file.length() > 100) {
                    decryptedLinks = JDUtilities.getController().getContainerLinks(file);
                }
            }
        } finally {
            if (con != null) con.disconnect();
        }

        return decryptedLinks;
    }
}
