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

import jd.parser.HTMLParser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class UCMS extends PluginForDecrypt {
    static private final String host = "Underground CMS";
    private Pattern PAT_CAPTCHA = Pattern.compile("<IMG SRC=\"/gfx/secure/", Pattern.CASE_INSENSITIVE);

    private Pattern PAT_NO_CAPTCHA = Pattern.compile("(<INPUT TYPE=\"SUBMIT\" CLASS=\"BUTTON\" VALUE=\"Zum Download\" onClick=\"if)|(<INPUT TYPE=\"SUBMIT\" CLASS=\"BUTTON\" VALUE=\"Download\" onClick=\"if)", Pattern.CASE_INSENSITIVE);

    private Pattern patternSupported = Pattern.compile("(http://[\\w\\.]*?filefox\\.in(/\\?id=.+|/category/.+/.+\\.html))" + "|(http://[\\w\\.]*?alphawarez\\.us/\\?id=.+)" + "|(http://[\\w\\.]*?pirate-loads\\.com/\\?id=.+)" + "|(http://[\\w\\.]*?fettrap\\.com/\\?id=.+)" + "|(http://[\\w\\.]*?omega-music\\.com(/\\?id=.+|/download/.+/.+\\.html))" + "|(http://[\\w\\.]*?hardcoremetal\\.biz/\\?id=.+)" + "|(http://[\\w\\.]*?flashload\\.org/\\?id=.+)" + "|(http://[\\w\\.]*?twin-warez\\.com/\\?id=.+)" + "|(http://[\\w\\.]*?oneload\\.org/\\?id=.+)" + "|(http://[\\w\\.]*?steelwarez\\.com/\\?id=.+)" + "|(http://[\\w\\.]*?fullstreams\\.info/\\?id=.+)" + "|(http://[\\w\\.]*?lionwarez\\.com/\\?id=.+)" + "|(http://[\\w\\.]*?1dl\\.in/\\?id=.+)" + "|(http://[\\w\\.]*?chrome-database\\.com/\\?id=.+)" + "|(http://[\\w\\.]*?oneload\\.org/\\?id=.+)" + "|(http://[\\w\\.]*?youwarez\\.biz/\\?id=.+)"
            + "|(http://[\\w\\.]*?saugking\\.net/\\?id=.+)" + "|(http://[\\w\\.]*?leetpornz\\.com/\\?id=.+)" + "|(http://[\\w\\.]*?freefiles4u\\.com/\\?id=.+)" + "|(http://[\\w\\.]*?dark-load\\.net/\\?id=.+)" + "|(http://[\\w\\.]*?crimeland\\.de/\\?id=.+)" + "|(http://[\\w\\.]*?get-warez\\.in/\\?id=.+)" + "|(http://[\\w\\.]*?meinsound\\.com/\\?id=.+)" + "|(http://[\\w\\.]*?projekt-tempel-news\\.de.vu/\\?id=.+)" + "|(http://[\\w\\.]*?datensau\\.org/\\?id=.+)" + "|(http://[\\w\\.]*?musik\\.am(/\\?id=.+|/download/.+/.+\\.html))" + "|(http://[\\w\\.]*?spreaded\\.net(/\\?id=.+|/download/.+/.+\\.html))" + "|(http://[\\w\\.]*?relfreaks\\.com(/\\?id=.+|/download/.+/.+\\.html))" + "|(http://[\\w\\.]*?babevidz\\.com(/\\?id=.+|/category/.+/.+\\.html))" + "|(http://[\\w\\.]*?serien24\\.com(/\\?id=.+|/download/.+/.+\\.html))"
            + "|(http://[\\w\\.]*?porn-freaks\\.net(/\\?id=.+|/category/.+/.+\\.html))" + "|(http://[\\w\\.]*?xxx-4-free\\.net(/\\?id=.+|/category/.+/.+\\.html))" + "|(http://[\\w\\.]*?xxx-reactor\\.net(/\\?id=.+|/category/.+/.+\\.html))" + "|(http://[\\w\\.]*?porn-traffic\\.net(/\\?id=.+|/category/.+/.+\\.html))" + "|(http://[\\w\\.]*?chili-warez\\.net(/\\?id=.+|/.+/.+\\.html))" + "|(http://[\\w\\.]*?game-freaks\\.net(/\\?id=.+|/download/.+/.+\\.html))" + "|(http://[\\w\\.]*?isos\\.at(/\\?id=.+|/download/.+/.+\\.html))" + "|(http://[\\w\\.]*?your-load\\.com(/\\?id=.+|/download/.+/.+\\.html))" + "|(http://[\\w\\.]*?mov-world\\.net(/\\?id=.+|/category/.+/.+\\.html))" + "|(http://[\\w\\.]*?xtreme-warez\\.net(/\\?id=.+|/category/.+/.+\\.html))" + "|(http://[\\w\\.]*?sceneload\\.to(/\\?id=.+|/download/.+/.+\\.html))"
            + "|(http://[\\w\\.]*?oxygen-warez\\.com(/\\?id=.+|/category/.+/.+\\.html))" + "|(http://[\\w\\.]*?epicspeedload\\.in/\\?id=.+)" + "|(http://[\\w\\.]*?serienfreaks\\.to(/\\?id=.+|/category/.+/.+\\.html))" + "|(http://[\\w\\.]*?serienfreaks\\.in(/\\?id=.+|/category/.+/.+\\.html))" + "|(http://[\\w\\.]*?warez-load\\.com(/\\?id=.+|/download/.+/.+\\.html))" + "|(http://[\\w\\.]*?ddl-scene\\.com(/\\?id=.+|/category/.+/.+\\.html))" + "|(http://[\\w\\.]*?mp3king\\.cinipac-hosting\\.biz/\\?id=.+)", Pattern.CASE_INSENSITIVE);
    // private String version = "1.0.0.0";

    public UCMS() {
        super();
    }

    
    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = new URL(parameter);

            RequestInfo reqinfo = HTTP.getRequest(url);
            File captchaFile = null;
            String capTxt = "";
            String host = url.getHost();

            if (!host.startsWith("http")) host = "http://" + host;

            String pass = new Regex(reqinfo.getHtmlCode(), Pattern.compile("CopyToClipboard\\(this\\)\\; return\\(false\\)\\;\">(.*?)<\\/a>", Pattern.CASE_INSENSITIVE)).getFirstMatch();
            if (pass != null) {
                if (!pass.equals("n/a") && !pass.equals("-") && !pass.equals("-kein Passwort-")) this.default_password.add(pass);
            }
            String forms[][] = new Regex(reqinfo.getHtmlCode(), Pattern.compile("<FORM ACTION=\"([^\"]*)\" ENCTYPE=\"multipart/form-data\" METHOD=\"POST\" NAME=\"(mirror|download)[^\"]*\"(.*?)</FORM>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatches();
            for (int i = 0; i < forms.length; i++) {
                for (int retry = 0; retry < 5; retry++) {
                    Matcher matcher = PAT_CAPTCHA.matcher(forms[i][2]);

                    if (matcher.find()) {
                        if (captchaFile != null && capTxt != null) {
                            JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, false);
                        }

                        logger.finest("Captcha Protected");
                        String captchaAdress = host + new Regex(forms[i][2], Pattern.compile("<IMG SRC=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getFirstMatch();
                        captchaFile = getLocalCaptchaFile(this);
                        JDUtilities.download(captchaFile, captchaAdress);
                        capTxt = JDUtilities.getCaptcha(this, "hardcoremetal.biz", captchaFile, false);
                        String posthelp = HTMLParser.getFormInputHidden(forms[i][2]);
                        if (forms[i][0].startsWith("http")) {
                            reqinfo = HTTP.postRequest(new URL(forms[i][0]), posthelp + "&code=" + capTxt);
                        } else {
                            reqinfo = HTTP.postRequest(new URL(host + forms[i][0]), posthelp + "&code=" + capTxt);
                        }
                    } else {
                        if (captchaFile != null && capTxt != null) {
                            JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, true);
                        }

                        Matcher matcher_no = PAT_NO_CAPTCHA.matcher(forms[i][2]);
                        if (matcher_no.find()) {
                            logger.finest("Not Captcha protected");
                            String posthelp = HTMLParser.getFormInputHidden(forms[i][2]);
                            if (forms[i][0].startsWith("http")) {
                                reqinfo = HTTP.postRequest(new URL(forms[i][0]), posthelp);
                            } else {
                                reqinfo = HTTP.postRequest(new URL(host + forms[i][0]), posthelp);
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
                    if (reqinfo.getConnection().getURL().toString().equals(host + forms[i][0])) break;
                }
                String links[][] = null;
                if (reqinfo.containsHTML("unescape")) {
                    String temp = JDUtilities.htmlDecode(JDUtilities.htmlDecode(JDUtilities.htmlDecode(new Regex(reqinfo.getHtmlCode(), Pattern.compile("unescape\\(unescape\\(\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getFirstMatch())));
                    links = new Regex(temp, Pattern.compile("ACTION=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatches();
                } else {
                    links = new Regex(reqinfo.getHtmlCode(), Pattern.compile("ACTION=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatches();
                }
                for (int j = 0; j < links.length; j++) {
                    decryptedLinks.add(this.createDownloadlink(JDUtilities.htmlDecode(links[j][0])));
                }
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
       String ret=new Regex("$Revision$","\\$Revision: ([\\d]*?) \\$").getFirstMatch();return ret==null?"0.0":ret;
    }
}