//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.decrypt;

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

public class LinkShareOrg extends PluginForDecrypt {

    public LinkShareOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String containerId = new Regex(parameter, "\\?url=([a-zA-Z0-9]{32})").getMatch(0);

        // Prüfen ob Containerfiles vorhanden sind
        if (!br.getPage("http://www.link-share.org/container/download.php?id=" + containerId + ".dlc").toString().contains("Die Datei wurde nicht gefunden")) {
            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
            Browser.download(container, "http://www.link-share.org/container/download.php?id=" + containerId + ".dlc");
            decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
            container.delete();
        }
        if (decryptedLinks.size() == 0 && !br.getPage("http://www.link-share.org/container/download.php?id=" + containerId + ".ccf").toString().contains("Die Datei wurde nicht gefunden")) {
            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".ccf");
            Browser.download(container, "http://www.link-share.org/container/download.php?id=" + containerId + ".ccf");
            decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
            container.delete();
        }
        if (decryptedLinks.size() == 0 && !br.getPage("http://www.link-share.org/container/download.php?id=" + containerId + ".rsdf").toString().contains("Die Datei wurde nicht gefunden")) {
            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".rsdf");
            Browser.download(container, "http://www.link-share.org/container/download.php?id=" + containerId + ".rsdf");
            decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
            container.delete();
        }
        if (decryptedLinks.size() == 0) {
            // 150 Links können maximal verwendet werden
            // Hier läuft es durch und wartet bis encodedLink nichtmehr
            // vorhanden
            // ist und somit die Links "zuende"

            for (int i = 0; i < 150; i++) {
                String encodedLink = Encoding.htmlDecode(new Regex(br.getPage("http://link-share.org/show.php?url=" + containerId + "&id=" + i), "<iframe src=\"(.*?)\" allow").getMatch(0));
                if (encodedLink.equals("")) break;

                decryptedLinks.add(createDownloadlink(encodedLink));

            }
        }

        /*
         * Captcha if
         * (br.containsHTML("<input type='hidden' name='captcha_encode'")) {
         * String captchaCode =
         * br.getRegex("name='captcha_encode' value='(.*?)'><img").getMatch(0);
         * File file = this.getLocalCaptchaFile(this); Form form =
         * br.getForm(0); Browser.download(file,
         * br.cloneBrowser().openGetConnection
         * ("http://link-share.org/captcha.php?captcha_code=" + captchaCode));
         * String capTxt = Plugin.getCaptchaCode(file, this, param);
         * form.put("captcha_code", capTxt); br.submitForm(form);
         * 
         * if (br.containsHTML("<input type='hidden' name='captcha_encode'")) {
         * throw new DecrypterException(DecrypterException.CAPTCHA); } }
         */

        /*
         * if (br.containsHTML("<image src=\"images/dlc.gif\">")) { URL url;
         * String containerId = new Regex(parameter,
         * "\\?url=([a-zA-Z0-9]{32})").getMatch(0); url = new
         * URL("http://www.link-share.org/container/download.php?id=" +
         * containerId + ".dlc"); File container =
         * JDUtilities.getResourceFile("container/" + System.currentTimeMillis()
         * + ".dlc"); HTTPConnection dlc_con = new
         * HTTPConnection(url.openConnection()); Browser.download(container,
         * dlc_con);
         * decryptedLinks.addAll(JDUtilities.getController().getContainerLinks
         * (container)); container.delete(); } else if
         * (br.containsHTML("<image src=\"images/ccf.gif\">")) { URL url; String
         * containerId = new Regex(parameter,
         * "\\?url=([a-zA-Z0-9]{32})").getMatch(0); url = new
         * URL("http://www.link-share.org/container/download.php?id=" +
         * containerId + ".ccf"); File container =
         * JDUtilities.getResourceFile("container/" + System.currentTimeMillis()
         * + ".dlc"); HTTPConnection ccf_con = new
         * HTTPConnection(url.openConnection()); Browser.download(container,
         * ccf_con);
         * decryptedLinks.addAll(JDUtilities.getController().getContainerLinks
         * (container)); container.delete(); } else if
         * (br.containsHTML("<image src=\"images/rsdf.gif\">")) { URL url;
         * String containerId = new Regex(parameter,
         * "\\?url=([a-zA-Z0-9]{32})").getMatch(0); url = new
         * URL("http://www.link-share.org/container/download.php?id=" +
         * containerId + ".rsdf"); File container =
         * JDUtilities.getResourceFile("container/" + System.currentTimeMillis()
         * + ".dlc"); HTTPConnection rsdf_con = new
         * HTTPConnection(url.openConnection()); Browser.download(container,
         * rsdf_con);
         * decryptedLinks.addAll(JDUtilities.getController().getContainerLinks
         * (container)); container.delete(); }
         */

        /*
         * String links[] =
         * br.getRegex("<a href=\"show.php\\?url=(.*?)\" target=\"_blank\">"
         * ).getColumn(0);
         * 
         * 
         * progress.setRange(links.length); for (String element : links) {
         * String encodedLink = Encoding.htmlDecode(new
         * Regex(br.getPage("http://link-share.org/show.php?url=" + element),
         * "<iframe src=\"(.*?)\" allow").getMatch(0));
         * 
         * decryptedLinks.add(createDownloadlink(encodedLink));
         * progress.increase(1); }
         */
        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
