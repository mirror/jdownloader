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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sealed.in" }, urls = { "http://[\\w\\.]*?sealed\\.in/[a-zA-Z]-[\\w]+" }, flags = { 0 })
public class Sldn extends PluginForDecrypt {

    public Sldn(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        String page = br.getPage(parameter);
        // Redirector link handling
        if (br.getRedirectLocation() != null) {
            decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
            return decryptedLinks;
        }
        String link = br.getRegex("refresh\" content=\"\\d+; URL=(.*?)\"").getMatch(0);
        if (link != null) {
            decryptedLinks.add(createDownloadlink(link));
            return decryptedLinks;
        }

        Form f = br.getForm(0);
        boolean do_continue = false;

        if (f != null) {
            for (int retrycnt = 1; retrycnt <= 5; retrycnt++) {

                /* Captcha */
                if (f.hasInputFieldByName("captcha")) {
                    String url = br.getRegex("(captcha\\-.*?\\.gif)").getMatch(0);
                    String captchaCode = getCaptchaCode(url, param);
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
            if (!getContainer(page, "dlc", decryptedLinks)) {
                if (!getContainer(page, "ccf", decryptedLinks)) {
                    getContainer(page, "rsdf", decryptedLinks);
                }
            }
            /* No Container */
            if (decryptedLinks.size() == 0) {
                String[] ids = br.getRegex("startDownload\\(\\'(.*?)\\'\\)").getColumn(0);
                if (ids.length > 0) {
                    progress.setRange(ids.length);
                    for (String id : ids) {
                        br.getPage("http://www.sealed.in/pl-" + id);
                        System.out.println(br.toString());
                        if (br.containsHTML("<title>RapidShare")) {
                            Form form = br.getFormbyProperty("name", "downloadForm");
                            if (form != null) decryptedLinks.add(createDownloadlink(form.getAction()));
                        } else {
                            decryptedLinks.add(createDownloadlink(DecodeLink(br.getPage("http://www.sealed.in/encd_" + id))));
                        }

                        progress.increase(1);

                    }
                    progress.doFinalize();
                }
            }
        } else {
            throw new DecrypterException(DecrypterException.CAPTCHA);
        }

        return decryptedLinks;
    }

    private boolean getContainer(String page, String containerFormat, ArrayList<DownloadLink> decryptedLinks) throws IOException {
        String container_link = new Regex(page, "href=\"(" + containerFormat + "/[a-z0-9]+)\"").getMatch(0);
        if (container_link != null) {
            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + "." + containerFormat);
            Browser browser = br.cloneBrowser();
            browser.getDownload(container, "http://www.sealed.in/" + Encoding.htmlDecode(container_link));
            decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
            container.delete();
            return true;
        }
        return false;
    }

    /**
     * 
     * Nachbau der Javascript Entschlüsselung auf xink.it (u.a.
     * http.xink.it/lcv1.js - http.sealed.in/lcv1.js)
     * 
     * @param source
     *            codierte Zeichenkette
     * @return entschlüsselter Link
     * 
     */
    private String DecodeLink(String source) {

        // implementiert von js vorlage http.xink.it/lcv1.js
        // l001011l10110101l11010101l101l01l( decodiert Base64
        // TODO: hier bitte die JS lib nutzen
        String evalCode = Encoding.Base64Decode(source);

        String l010 = Encoding.Base64Decode(new Regex(evalCode, "l010 \\= l001011l10110101l11010101l101l01l\\(\"(.*?)\"\\);").getMatch(0));
        String gt = new Regex(evalCode, "gt\\=\"(.*?)\";").getMatch(0);
        String l011 = Encoding.Base64Decode(new Regex(evalCode, "l011 \\= l001011l10110101l11010101l101l01l\\(\"(.*?)\"\\);").getMatch(0));
        String l012 = Encoding.Base64Decode(gt);

        String r = l012;
        String ar = r;
        String re = "";

        for (int a = 2; a < r.length(); a = a + 4) {

            String temp1 = "";

            int temp2 = a;
            if (temp2 > ar.length()) {
                temp2 = ar.length();
            } else if (temp2 < 0) {
                temp2 = 0;
            }

            int temp3 = a + 2;
            if (temp3 > ar.length()) {
                temp3 = ar.length();
            }

            int temp4 = a + 2 + ar.length() - a;
            if (temp4 > ar.length()) {
                temp4 = ar.length();
            } else if (temp4 < 0) {
                temp4 = temp3;
            }

            temp1 += ar.substring(0, temp2);
            temp1 += ar.substring(temp3, temp4);

            ar = temp1;

        }

        for (int a = 0; a < ar.length(); a = a + 2) {

            for (int i = 0; i < l011.length(); i = i + 2) {

                int temp5 = a + 2;
                if (temp5 > ar.length()) {
                    temp5 = ar.length();
                }

                int temp6 = i + 2;
                if (temp6 > l011.length()) {
                    temp6 = l011.length();
                }

                if (ar.substring(a, temp5).equals(l011.substring(i, temp6))) {

                    re += l010.substring((int) Math.floor(i / 2), (int) Math.floor(i / 2) + 1);

                }

            }

        }

        return re;

    }

    // @Override

}
