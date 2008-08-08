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
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

public class ShareOnAll extends PluginForDecrypt {
    final static String host = "shareonall.com";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?shareonall\\.com/(.*?)\\.htm", Pattern.CASE_INSENSITIVE);

    public ShareOnAll() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        String cryptedLink = parameter;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            Browser.clearCookies(host);
            String id = new Regex(cryptedLink, patternSupported).getFirstMatch();
            String url = "http://www.shareonall.com/showlinks.php?f=" + id + ".htm";
         
            br.getPage(url);
            boolean do_continue = false;
            Form form;
            for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {
                if (br.containsHTML("<img src='code")) {
                    form = br.getForm(0);
                    String captchaAddress = br.getRegex( Pattern.compile("src='code/(.*?)'", Pattern.CASE_INSENSITIVE)).getFirstMatch();
                    captchaAddress = "http://www.shareonall.com/code/" + captchaAddress;
                    
          
                    File captchaFile = this.getLocalCaptchaFile(this);
                    if (!Browser.download(captchaFile, br.openGetConnection(captchaAddress)) || !captchaFile.exists()) {
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
                    form.put("c", captchaCode);
                    br.submitForm(form);
                 
                } else {
                    do_continue = true;
                    break;
                }
            }
            if (do_continue == true) {
                // Links herausfiltern
                String links[][] = br.getRegex( Pattern.compile("<a href=\'(.*?)\' target='_blank'>", Pattern.CASE_INSENSITIVE)).getMatches();
                progress.setRange(links.length);
                for (String[] element : links) {
                    decryptedLinks.add(createDownloadlink(element[0]));
                    progress.increase(1);
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