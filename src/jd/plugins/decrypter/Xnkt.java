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
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xink.it" }, urls = { "http://[\\w\\.]*?xink\\.it/f-[\\w]+" }, flags = { 0 })
public class Xnkt extends PluginForDecrypt {

    public Xnkt(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);

        boolean do_continue = false;
        for (int retry = 1; retry < 5; retry++) {

            if (br.containsHTML("captcha_send")) {

                String captchaAdress = "http://xink.it/captcha-" + br.getRegex("src=\"captcha-(.*?)\"").getMatch(0);

                String captchaCode = getCaptchaCode(captchaAdress, param);

                Form captchaForm = br.getForm(0);
                captchaForm.put("captcha", captchaCode);
                br.submitForm(captchaForm);

            } else {
                do_continue = true;
                break;
            }
        }
        if (do_continue) {
            String ids[] = br.getRegex("startDownload\\('(.*?)'\\);").getColumn(0);
            progress.setRange(ids.length);
            for (String element : ids) {
                decryptedLinks.add(createDownloadlink(XnktDecodeLink(br.getPage("http://xink.it/encd_" + element))));
                progress.increase(1);
            }
        }

        return decryptedLinks;

    }

    // @Override

    /**
     * 
     * Nachbau der Javascript Entschlüsselung auf xink.it (u.a.
     * http.xink.it/lcv1.js)
     * 
     * @param source
     *            codierte Zeichenkette
     * @return entschlüsselter Link
     * 
     */
    private String XnktDecodeLink(String source) {

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

}
