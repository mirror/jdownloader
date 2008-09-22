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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

public class Lixin extends PluginForDecrypt {

    static private final Pattern patternCaptcha = Pattern.compile("<img src=\"(.*?captcha.*?)\"");

    static private final Pattern patternIframe = Pattern.compile("<iframe.*src=\"(.+?)\"", Pattern.DOTALL);

    public Lixin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        try {

            boolean lix_continue = true;
            Matcher matcher;
            Form form;
            /* zuerst mal den evtl captcha abarbeiten */

            br.getPage(parameter);
            for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {
                form = br.getForm(0);
                if (form != null) {
                    matcher = patternCaptcha.matcher(form.getHtmlCode());
                    if (matcher.find()) {
                        lix_continue = false;
                        String captchaAddress = "http://" + getHost() + "/" + matcher.group(1);
                        File captchaFile = this.getLocalCaptchaFile(this);
                        Browser.download(captchaFile, captchaAddress);
                        String captchaCode = Plugin.getCaptchaCode(captchaFile, this, param);
                        captchaCode = captchaCode.toUpperCase();
                        form.put("capt", captchaCode);
                        br.submitForm(form);
                    } else {
                        if (form.hasSubmitValue("continue")) {
                            br.submitForm(form);
                        } else {
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
                matcher = patternIframe.matcher(br + "");
                if (matcher.find()) {
                    /* EinzelLink gefunden */
                    String link = matcher.group(1);
                    decryptedLinks.add(createDownloadlink(link));
                } else {
                    /* KEIN EinzelLink gefunden, evtl ist es ein Folder */
                    Form[] forms = br.getForms();
                    for (Form element : forms) {

                        br.submitForm(element);
                        matcher = patternIframe.matcher(br + "");
                        if (matcher.find()) {
                            /* EinzelLink gefunden */
                            String link = matcher.group(1);
                            decryptedLinks.add(createDownloadlink(link));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}
