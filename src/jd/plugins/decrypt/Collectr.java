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
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

public class Collectr extends PluginForDecrypt {
    private static final Pattern PAT_SUPPORTED_OUT    = Pattern.compile("http://[\\w\\.]*?collectr\\.net/out/(\\d+/)?\\d+",Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_AB_18        = Pattern.compile("Hast du das 18 Lebensjahr bereits abgeschlossen\\?.*");
    
    private static final Pattern PAT_SUPPORTED_FOLDER = Pattern.compile("http://[\\w\\.]*?collectr\\.net/links/(\\w+)",Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_GETLINK      = Pattern.compile("<a href=\"javascript:getLink\\(lnk\\[(\\d+)\\]\\)\">(.+?)  #\\d+</a>");
    private static final Pattern PATTERN_SAPCHA       = Pattern.compile("useSaptcha\\s+=\\s+(\\d+);");
    private static final Pattern PATTERN_FOLDERNAME   = Pattern.compile("<span id=\"title\">(.+?)</span>");
    private static final Pattern PATTERN_DURL         = Pattern.compile("<key>(.+?)</key>");
    private static final String  JAMES_GETLINK        = "http://collectr.net/james.php?do=getLink";
    private static final String  JAMES_SAPTCHA        = "http://collectr.net/james.php?do=saptcha";
    public Collectr(PluginWrapper wrapper) {
        super(wrapper);
    }
    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String url = param.toString();
        String page = br.getPage(url);
        if(new Regex(url,PAT_SUPPORTED_OUT).matches()){
            Form[] forms = br.getForms();
    
            if (Regex.matches(page, PATTERN_AB_18)) {
                forms[0].put("o18", "o18=true");
                br.submitForm(forms[0]);
            }
    
            String links[] = br.getRegex("<iframe id=\"displayPage\" src=\"(.*?)\" name=\"displayPage\"").getColumn(0);
            progress.setRange(links.length);
    
            for (String element : links) {
                decryptedLinks.add(createDownloadlink(element));
                progress.increase(1);
            }
        }else if(new Regex(url,PAT_SUPPORTED_FOLDER).matches()){
            String saptcha = new Regex(page, PATTERN_SAPCHA).getMatch(0);
            String ordner = new Regex(url,PAT_SUPPORTED_FOLDER).getMatch(0);
            FilePackage fp = new FilePackage();
            fp.setName(new Regex(page,PATTERN_FOLDERNAME).getMatch(0));
            if(saptcha!=null){
                //Captcha on
                File file = this.getLocalCaptchaFile(this);
                Browser c = br.cloneBrowser();
                Browser.download(file, c.openGetConnection("http://collectr.net/img/saptcha"+saptcha+".gif"));
                String captchaCode = Plugin.getCaptchaCode(file, this, param);
                HashMap<String, String> post = new HashMap<String, String>();
                post.put("saptcha", captchaCode);
                post.put("id"     , saptcha );
                post.put("ordner" , ordner );
                br.postPage(JAMES_SAPTCHA, post);
            }
            for(String link:new Regex(page,PATTERN_GETLINK).getColumn(0)){
                HashMap<String, String> post = new HashMap<String, String>();
                post.put("id"    , link);
                post.put("ordner", ordner);
                String dUrl = new Regex(br.postPage(JAMES_GETLINK, post),PATTERN_DURL).getMatch(0);
                DownloadLink dLink = createDownloadlink(dUrl);
                dLink.setFilePackage(fp);
                decryptedLinks.add(dLink);
            }
        }
        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}