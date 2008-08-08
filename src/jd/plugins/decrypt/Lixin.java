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

import jd.http.Browser;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

public class Lixin extends PluginForDecrypt {

    static private final String host = "lix.in";

    static private final Pattern patternCaptcha = Pattern.compile("<img src=\"(.*?captcha.*?)\"");

    static private final Pattern patternIframe = Pattern.compile("<iframe.*src=\"(.+?)\"", Pattern.DOTALL);
    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?lix\\.in/[-]{0,1}[a-zA-Z0-9]{6,10}", Pattern.CASE_INSENSITIVE);

    public Lixin() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        String cryptedLink = parameter;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
          
            
            boolean lix_continue = false;
            Matcher matcher;
            Form form;
            /* zuerst mal den evtl captcha abarbeiten */
           
            br.getPage(cryptedLink);
            for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {
                matcher = patternCaptcha.matcher(br+"");
                if (matcher.find()) {
                    form = br.getForm(0);
                    
                    String captchaAddress = "http://" + getHost() + "/" + matcher.group(1);
                    File captchaFile = this.getLocalCaptchaFile(this);
                    if (!Browser.download(captchaFile, captchaAddress) || !captchaFile.exists()) {
                        /* Fehler beim Captcha */
                        logger.severe("Captcha Download fehlgeschlagen: " + captchaAddress);
                        return null;
                    }
                    String captchaCode = Plugin.getCaptchaCode(captchaFile, this);
                    if (captchaCode == null) {
                        /* abbruch geklickt */
                        return null;
                    }
                    captchaCode = captchaCode.toUpperCase();
                    form.put("capt", captchaCode);
                    br.submitForm(form);
                } else {
                    lix_continue = true;
                    break;
                }
            }
            if (lix_continue == true) {
                /* EinzelLink filtern */
                matcher = patternIframe.matcher(br+"");
                if (matcher.find()) {
                    /* EinzelLink gefunden */
                    String link = matcher.group(1);
                    decryptedLinks.add(createDownloadlink(link));
                } else {
                    /* KEIN EinzelLink gefunden, evtl ist es ein Folder */
                    Form[] forms = br.getForms();
                    for (Form element : forms) {
                       
                        br.submitForm(element);
                        matcher = patternIframe.matcher(br+"");
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
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }
}
