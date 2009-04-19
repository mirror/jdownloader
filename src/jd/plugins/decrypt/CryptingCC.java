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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

public class CryptingCC extends PluginForDecrypt {

    public CryptingCC(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        String page = br.getPage(parameter);
        Form f = br.getForm(0);
        boolean do_continue = false;
        
        if (f != null) {           
            for (int retrycnt = 1; retrycnt <= 5; retrycnt++) {
                
                /* Captcha */                
                if (f.hasInputFieldByName("captcha")) {
                    String url = br.getRegex("(captcha\\-.*?\\.gif)").getMatch(0);
                    File captchaFile = this.getLocalCaptchaFile(this);
                    Browser.download(captchaFile, br.cloneBrowser().openGetConnection(url));
                    String captchaCode = Plugin.getCaptchaCode(this, "linkcrypt.com", captchaFile, false, param);
                    if (captchaCode == null) return null;
                    f.put("captcha", captchaCode);
                }
                
                /* Folder password */
                if (f.hasInputFieldByName("passw")) {
                    String pass = getUserInput(null, param);
                    f.put("passw", pass);
                }
                
                page = br.submitForm(f);
                
                if (!br.containsHTML("class=\"error\">")) {
                    do_continue = true;
                    break;
                }           
            }
        }
        
        
        if (do_continue || f == null) {
            /* Container */
            if (!getContainer(page, parameter, "dlc", decryptedLinks)) {
               if (!getContainer(page, parameter, "ccf", decryptedLinks)) {
                  if (getContainer(page, parameter, "rsdf", decryptedLinks)); 
               }
            }
            /* No Container */
            if (decryptedLinks.size() == 0) {
                String[] ids = br.getRegex("startDownload\\(\\'(.*?)\\'\\)").getColumn(0);
                if (ids.length > 0) {
                    Browser tab;
                    tab = br.cloneBrowser();
                    progress.setRange(ids.length);
                    for (String id : ids) {                    
                        tab.getPage("pl-" + id);
                        //decryptedLinks.addAll(new DistributeData(tab.toString()).findLinks(false));
                        /* Use next 2 lines instead of last line if decrypter uses iframes */
                        String link = tab.getRegex("<iframe .*? src=\"(.*?)\">").getMatch(0);
                        decryptedLinks.add(createDownloadlink(link));
                        progress.increase(1);

                    }
                    progress.finalize();
                }
            }
        } else {
            throw new DecrypterException(DecrypterException.CAPTCHA);
        }

        return decryptedLinks;
    }
    
    private boolean getContainer(String page, String cryptedLink, String containerFormat, ArrayList<DownloadLink> decryptedLinks) throws IOException {
        String container_link = new Regex(page, "href=\"(" + containerFormat + "/[a-z0-9]+)\"").getMatch(0);
        if (container_link != null) {
            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + "." + containerFormat);
            Browser browser = br.cloneBrowser();
            browser.getDownload(container, "http://crypting.cc/" + Encoding.htmlDecode(container_link));
            decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));            
            container.delete();
            return true;
        }
        return false;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}