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

package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

public class RelinkUs extends PluginForDecrypt {

    ProgressController progress;

    public RelinkUs(PluginWrapper wrapper) {
        super(wrapper);
    }

    private boolean add_relinkus_container(String page, String cryptedLink, String containerFormat, ArrayList<DownloadLink> decryptedLinks) throws IOException {
        String container_link = new Regex(page, "(download\\.php\\?id=[a-zA-z0-9]+\\&amp\\;"+containerFormat+"=1)").getMatch(0);
        if (container_link != null) { 
            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + "." + containerFormat);
            Browser browser = br.cloneBrowser();           
            browser.getHeaders().put("Referer", cryptedLink);
            browser.getDownload(container, "http://relink.us/" + Encoding.htmlDecode(container_link));
            decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
            container.delete();
            return true;
        }
        return false;
    }

    private void add_relinkus_links(ArrayList<DownloadLink> decryptedLinks) throws IOException {
//        String links[] = new Regex(page, Pattern.compile("action=\\'([^\\']*?)\\' method=\\'post\\' target=\\'\\_blank\\'", Pattern.CASE_INSENSITIVE)).getColumn(0);
//        if (links.length == 0) {
//            links = new Regex(page, Pattern.compile("action=\"(.*?)\" method=\"post\" target=\"\\_blank\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
//        }
        Form[] forms = br.getForms();
        progress.addToMax(forms.length);
        for (Form link : forms) {
            try {
                br.submitForm(link);
        
            String dl_link = br.getRegex(Pattern.compile("iframe .*? src=\"[\n\r]*?(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(dl_link)));
            progress.increase(1);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        this.progress = progress;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        String page = br.getPage(parameter);
        boolean okay = true;
        for (int i = 0; i < 4; i++) {
            if (br.containsHTML("Dieser Ordner ist passwort")) {
                okay = false;
                Form form = br.getForm(0);
                String pw = getUserInput("Password?", param);
                form.put("passwort", pw);
                page = br.submitForm(form);
            } else {
                okay = true;
                break;
            }
        }
        if (okay == false) throw new DecrypterException(DecrypterException.CAPTCHA);
        progress.setRange(0);
        add_relinkus_links( decryptedLinks);
        String more_links[] = new Regex(page, Pattern.compile("<a href=\"(go\\.php\\?id=[a-zA-Z0-9]+\\&seite=\\d+)\">", Pattern.CASE_INSENSITIVE)).getColumn(0);
        for (String link : more_links) {
            br.getPage("http://relink.us/" + link);
            add_relinkus_links(decryptedLinks);
        }
      
        if (decryptedLinks.size() == 0) {
            if (!add_relinkus_container(page, parameter, "dlc", decryptedLinks)) {
                if (!add_relinkus_container(page, parameter, "ccf", decryptedLinks)) {
                    add_relinkus_container(page, parameter, "rsdf", decryptedLinks);
                }
            }
        }

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}