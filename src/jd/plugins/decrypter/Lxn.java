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
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "lix.in" }, urls = { "http://[\\w\\.]*?lix\\.in/[-]{0,1}[\\w]{6,10}" }, flags = { 0 })
public class Lxn extends PluginForDecrypt {

    static private Object LOCK = new Object(); /*
                                                * lixin checkt anhand der ip und der globalen phpsessionid, daher müssen parallel zugriffe
                                                * vermieden werden, sonst ist das captcha imme falsch
                                                */

    public Lxn(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        synchronized (LOCK) {
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
                    // Try to download the DLC first, continue if that doesn't
                    // work
                    ArrayList<DownloadLink> dlclinks = new ArrayList<DownloadLink>();
                    dlclinks = loadcontainer(parameter);
                    if (dlclinks != null && dlclinks.size() != 0) return dlclinks;
                    /* KEIN EinzelLink gefunden, evtl ist es ein Folder */
                    Form[] forms = br.getForms();
                    if (forms == null || forms.length == 0) {
                        logger.warning("Could not find forms");
                        return null;
                    }
                    int counter = 1;
                    int failcounter = 0;
                    progress.setRange(forms.length);
                    for (Form element : forms) {
                        if (element.containsHTML("Download")) continue;
                        element.put("submit", "Link+" + counter);
                        try {
                            br.submitForm(element);
                        } catch (Exception e) {
                            logger.warning("Failed on ID " + counter);
                            failcounter++;
                            continue;
                        }
                        /* EinzelLink gefunden */
                        link = br.getRegex("<iframe.*?src=\"(.+?)\"").getMatch(0);
                        if (link == null) {
                            logger.warning("Could not find the link in the iframe!");
                            return null;
                        }
                        decryptedLinks.add(createDownloadlink(link.trim()));
                        counter++;
                        progress.increase(1);
                    }
                    logger.info("Finished with " + failcounter + "failed links");
                }
            }
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> loadcontainer(String theLink) throws IOException, PluginException {
        ArrayList<DownloadLink> decryptedLinks = null;
        Browser brc = br.cloneBrowser();
        String id = new Regex(theLink, "lix\\.in/(.+)").getMatch(0);
        theLink = Encoding.htmlDecode(theLink);
        File file = null;
        URLConnectionAdapter con = brc.openPostConnection("http://lix.in/download/", "submit=Download+DLC&id=" + id);
        if (con.getResponseCode() == 200) {
            file = JDUtilities.getResourceFile("tmp/lixin/" + theLink.replaceAll("(:|/|=|\\?)", "") + ".dlc");
            if (file == null) return null;
            file.deleteOnExit();
            brc.downloadConnection(file, con);
            if (file != null && file.exists() && file.length() > 100) {
                decryptedLinks = JDUtilities.getController().getContainerLinks(file);
            }
        } else {
            con.disconnect();
            return null;
        }

        if (file != null && file.exists() && file.length() > 100) {
            if (decryptedLinks.size() > 0) return decryptedLinks;
        } else {
            return null;
        }
        return null;
    }

}
