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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "lix.in" }, urls = { "http://[\\w\\.]*?lix\\.in/[-]{0,1}[\\w]{6,10}"}, flags = { 0 })


public class Lixin extends PluginForDecrypt {

    static private Integer lock = 0; /*
                                      * lixin checkt anhand der ip und der
                                      * globalen phpsessionid, daher müssen
                                      * parallel zugriffe vermieden werden,
                                      * sonst ist das captcha imme falsch
                                      */

    public Lixin(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        synchronized (lock) {
            boolean lix_continue = true;
            Form form;
            /* zuerst mal den evtl captcha abarbeiten */
            br.setCookiesExclusive(false);
            br.getPage(parameter);
            for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {
                form = br.getForm(0);
                if (form != null) {
                    String capturl = form.getRegex("<img src=\"(.*?captcha.*?)\"").getMatch(0);
                    if (capturl != null) {
                        lix_continue = false;
                        String captchaCode = getCaptchaCode("http://" + getHost() + "/" + capturl, param);
                        captchaCode = captchaCode.toUpperCase();
                        form.put("capt", captchaCode);
                        br.submitForm(form);
                    } else {
                        try {
                            form.setPreferredSubmit("continue");
                            br.submitForm(form);
                        } catch (Exception e) {                            
                            lix_continue = true;
                            break;
                        }
                    }
                } else {
                    lix_continue = true;
                    break;
                }
            }
            if (lix_continue == true) {
                /* EinzelLink filtern */
                String link = br.getRegex("<iframe.*?src=\"(.+?)\"").getMatch(0);
                if (link != null) {
                    decryptedLinks.add(createDownloadlink(link));
                } else {
                    /* KEIN EinzelLink gefunden, evtl ist es ein Folder */
                    Form[] forms = br.getForms();
                    for (Form element : forms) {
                        if (element.containsHTML("Download")) continue;
                        br.submitForm(element);
                        /* EinzelLink gefunden */
                        link = br.getRegex("<iframe.*?src=\"(.+?)\"").getMatch(0);
                        if (link == null) return null;
                        decryptedLinks.add(createDownloadlink(link));
                    }
                }
            }
        }
        return decryptedLinks;
    }

    // @Override
    
}
