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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.HTMLParser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class UCMS extends PluginForDecrypt {

    private Pattern PAT_CAPTCHA = Pattern.compile("<IMG SRC=\".*?/gfx/secure/", Pattern.CASE_INSENSITIVE);

    private Pattern PAT_NO_CAPTCHA = Pattern.compile("(<INPUT TYPE=\"SUBMIT\" CLASS=\"BUTTON\" VALUE=\".*?Download.*?\".*?Click)", Pattern.CASE_INSENSITIVE);

    public UCMS(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        try {
            URL url = new URL(parameter);

            RequestInfo reqinfo = HTTP.getRequest(url);
            File captchaFile = null;
            String capTxt = "";
            String host = url.getHost();

            if (!host.startsWith("http")) {
                host = "http://" + host;
            }

            String pass = new Regex(reqinfo.getHtmlCode(), Pattern.compile("CopyToClipboard\\(this\\)\\; return\\(false\\)\\;\">(.*?)<\\/a>", Pattern.CASE_INSENSITIVE)).getMatch(0);
            if (pass != null) {
                if (pass.equals("kein pw") || pass.equals("N/A") || pass.equals("n/a") || pass.equals("-") || pass.equals("-kein Passwort-") || pass.equals("-No Pass-") || pass.equals("ohne PW")) {
                    pass = null;
                }
            }
            String forms[][] = new Regex(reqinfo.getHtmlCode(), Pattern.compile("<FORM ACTION=\"([^\"]*)\" ENCTYPE=\"multipart/form-data\" METHOD=\"POST\" NAME=\"(mirror|download)[^\"]*\"(.*?)</FORM>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatches();
            for (String[] element : forms) {
                for (int retry = 0; retry < 5; retry++) {
                    Matcher matcher = PAT_CAPTCHA.matcher(element[2]);

                    if (matcher.find()) {
                        if (captchaFile != null && capTxt != null) {
                            JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, false);
                        }

                        logger.finest("Captcha Protected");
                        String captchaAdress = host + new Regex(element[2], Pattern.compile("<IMG SRC=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
                        captchaFile = getLocalCaptchaFile(this);
                        Browser.download(captchaFile, captchaAdress);
                        capTxt = JDUtilities.getCaptcha(this, "hardcoremetal.biz", captchaFile, false, param);
                        if (capTxt == null) throw new DecrypterException(DecrypterException.CAPTCHA);
                        String posthelp = HTMLParser.getFormInputHidden(element[2]);
                        if (element[0].startsWith("http")) {
                            reqinfo = HTTP.postRequest(new URL(element[0]), posthelp + "&code=" + capTxt);
                        } else {
                            reqinfo = HTTP.postRequest(new URL(host + element[0]), posthelp + "&code=" + capTxt);
                        }
                    } else {
                        if (captchaFile != null && capTxt != null) {
                            JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, true);
                        }

                        Matcher matcher_no = PAT_NO_CAPTCHA.matcher(element[2]);
                        if (matcher_no.find()) {
                            logger.finest("Not Captcha protected");
                            String posthelp = HTMLParser.getFormInputHidden(element[2]);
                            if (element[0].startsWith("http")) {
                                reqinfo = HTTP.postRequest(new URL(element[0]), posthelp);
                            } else {
                                reqinfo = HTTP.postRequest(new URL(host + element[0]), posthelp);
                            }
                            break;
                        }
                    }
                    if (reqinfo.containsHTML("Der Sichheitscode wurde falsch eingeben")) {
                        logger.warning("Captcha Detection failed");
                        reqinfo = HTTP.getRequest(url);
                    } else {
                        break;
                    }
                    if (reqinfo.getConnection().getURL().toString().equals(host + element[0])) {
                        break;
                    }
                }
                String links[][] = null;
                if (reqinfo.containsHTML("unescape(unescape(unescape")) {
                    String temp[] = new Regex(reqinfo.getHtmlCode(), Pattern.compile("unescape\\(unescape\\(unescape\\(\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
                    String temp2 = Encoding.htmlDecode(Encoding.htmlDecode(Encoding.htmlDecode(temp[0])));
                    links = new Regex(temp2, Pattern.compile("ACTION=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatches();
                } else if (reqinfo.containsHTML("unescape(unescape")) {
                    String temp[] = new Regex(reqinfo.getHtmlCode(), Pattern.compile("unescape\\(unescape\\(\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
                    String temp2 = Encoding.htmlDecode(Encoding.htmlDecode(temp[0]));
                    links = new Regex(temp2, Pattern.compile("ACTION=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatches();
                } else {
                    links = new Regex(reqinfo.getHtmlCode(), Pattern.compile("ACTION=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatches();
                }
                for (String[] element2 : links) {
                    DownloadLink link = createDownloadlink(Encoding.htmlDecode(element2[0]));
                    link.addSourcePluginPassword(pass);
                    decryptedLinks.add(link);
                }
            }
        } catch (IOException e) {
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