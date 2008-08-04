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
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.plugins.CRequest.CaptchaInfo;
import jd.utils.JDUtilities;

public class Stealth extends PluginForDecrypt {
    static private final String host = "Stealth.to";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?stealth\\.to/(\\?id\\=[a-zA-Z0-9]+|index\\.php\\?id\\=[a-zA-Z0-9]+)", Pattern.CASE_INSENSITIVE);

    public Stealth() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            CaptchaInfo<File, String> captchaInfo = null;
            request.getRequest(parameter);
            for (int i = 0; i < 5; i++) {
                if (request.toString().contains("captcha_img.php")) {

                    String sessid = new Regex(request.getCookie(), "PHPSESSID=([a-zA-Z0-9]*)").getFirstMatch();
                    if (sessid == null) {
                        logger.severe("Error sessionid: " + request.getCookie());
                        return null;
                    }
                    logger.finest("Captcha Protected");
                    String captchaAdress = "http://stealth.to/captcha_img.php?PHPSESSID=" + sessid;
                    captchaInfo = request.getCaptchaCode(this, captchaAdress);
                    Form form = request.getForm();
                    form.put("txtCode", captchaInfo.captchaCode);
                    request.setRequestInfo(form);
                } else {
                    break;
                }
            }

            RequestInfo reqhelp = HTTP.postRequest(new URL("http://stealth.to/ajax.php"), null, parameter, null, "id=" + new Regex(request.getHtmlCode(), Pattern.compile("<div align=\"center\"><a id=\"(.*?)\" href=\"", Pattern.CASE_INSENSITIVE)).getFirstMatch() + "&typ=hit", true);
            String[] links = new Regex(request.getHtmlCode(), Pattern.compile("dl = window\\.open\\(\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatches(1);
            progress.setRange(links.length);

            for (String element : links) {
                reqhelp = HTTP.getRequest(new URL("http://stealth.to/" + element));
                String[] decLinks = new Regex(reqhelp.getHtmlCode(), Pattern.compile("iframe src=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatches(1);
                decryptedLinks.add(createDownloadlink(JDUtilities.htmlDecode(decLinks[1])));
                progress.increase(1);
            }
        } catch (IOException e) {
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
