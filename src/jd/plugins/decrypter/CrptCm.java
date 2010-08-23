//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "youcrypt.com" }, urls = { "http://[\\w\\.]*?youcrypt\\.com/[0-9a-zA-Z]+\\.html" }, flags = { 0 })
public class CrptCm extends PluginForDecrypt {

    public CrptCm(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(parameter.toString());
        int state = 0;
        String captcha = null;
        String password = null;
        boolean valid = true;
        Form[] forms = br.getForms();

        if (br.containsHTML("Captcha:") && br.containsHTML("Passwort:")) {
            captcha = get_easy_Captcha();
            password = getUserInput(null, parameter);
            state = 1;
        } else if (br.containsHTML("Captcha:")) {
            captcha = get_easy_Captcha();

            state = 2;
        } else if (br.containsHTML("Passwort:")) {
            password = getUserInput(null, parameter);
            state = 3;
        }

        switch (state) {
        case 1:
            forms[0].put("captcha", captcha);
            forms[0].put("pw", password);
            br.submitForm(forms[0]);
            if (br.containsHTML("Sie haben ein falsches Passwort eingegeben!")) {
                valid = false;
            }
            break;
        case 2:
            forms[0].put("captcha", captcha);
            br.submitForm(forms[0]);
            break;
        case 3:
            forms[0].put("pw", password);
            br.submitForm(forms[0]);
            if (br.containsHTML("Sie haben ein falsches Passwort eingegeben!")) {
                valid = false;
            }
            break;
        default:
            // Unknown security-method or page-change ?!?!
            logger.fine("CrptCm: Unknown security-method or page-change");
            break;
        }

        if (valid == false) throw new DecrypterException(DecrypterException.PASSWORD);

        if (valid == true) {
            // If we have a Container we will use it
            if (br.containsHTML("Container:")) {
                String containerlink = null;
                String folder_id = br.getRegex("<b>Container:</b>.*?<a href=\"http://youcrypt.com/container/dl.php\\?type=.*?&id=(.*?)\">").getMatch(0);

                if (containerlink == null && br.containsHTML("<img src=\"images/dlc.png\" border=\"0\">")) {
                    containerlink = "http://youcrypt.com/container/dl.php?type=dlc&id=" + folder_id;
                    File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
                    Browser.download(container, br.cloneBrowser().openGetConnection(containerlink));
                    for (DownloadLink dLink : JDUtilities.getController().getContainerLinks(container)) {
                        decryptedLinks.add(dLink);
                    }
                    container.delete();
                }
                if (containerlink == null && br.containsHTML("<img src=\"images/rsdf.png\" border=\"0\">")) {
                    containerlink = "http://youcrypt.com/container/dl.php?type=rsdf&id=" + folder_id;
                    File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".rsdf");
                    Browser.download(container, br.cloneBrowser().openGetConnection(containerlink));
                    for (DownloadLink dLink : JDUtilities.getController().getContainerLinks(container)) {
                        decryptedLinks.add(dLink);
                    }
                    container.delete();
                }
                if (containerlink == null && br.containsHTML("<img src=\"images/ccf.png\" border=\"0\">")) {
                    containerlink = "http://youcrypt.com/container/dl.php?type=ccf&id=" + folder_id;
                    File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".ccf");
                    Browser.download(container, br.cloneBrowser().openGetConnection(containerlink));
                    for (DownloadLink dLink : JDUtilities.getController().getContainerLinks(container)) {
                        decryptedLinks.add(dLink);
                    }
                    container.delete();
                }
                if (containerlink != null) return decryptedLinks;
            }
            String folder_id = br.getRegex("<b>Seiten:</b>.*?<a href=\"http://youcrypt.com/index.php\\?action=show&folder_id=(.*?)&p=1\">1</a>").getMatch(0);
            int page_count = Integer.parseInt(br.getRegex("&nbsp;&nbsp;<a .*?>(..?)</a>&nbsp;&nbsp;<br><br>").getMatch(0));
            String[][] links = null;
            progress.setRange(15 * page_count);
            for (int x = 1; x <= page_count; x++) {
                br.getPage("http://youcrypt.com/index.php?action=show&folder_id=" + folder_id + "&p=" + x);
                links = br.getRegex("<tr style=\"cursor:pointer;\" onclick=\"window.open\\(\\'(.*?)\\'\\);\">").getMatches();
                br.setFollowRedirects(false);
                for (String data[] : links) {
                    br.getPage(data[0]);
                    DownloadLink l = this.createDownloadlink(br.getRedirectLocation());
                    l.setSourcePluginComment("YouCrypt.com");
                    decryptedLinks.add(l);
                    progress.increase(1);
                }

            }

        }
        return decryptedLinks;
    }

    private String get_easy_Captcha() {
        String captcha = null;
        captcha = br.getRegex("<input type=\"hidden\" name=\"captcha_2\" value=\"(.*?)\">").getMatch(0);
        return captcha;
    }
}
