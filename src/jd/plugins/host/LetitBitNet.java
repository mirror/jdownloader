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

package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;

public class LetitBitNet extends PluginForHost {

    public LetitBitNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://letitbit.net/page/terms.php";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.getPage(downloadLink.getDownloadURL());
        String filename = null;
        String size = null;
        try {
            filename = br.getXPathElement("/html/body/div[2]/div[3]/div/h1[1]").trim();
            size = br.getXPathElement("/html/body/div[2]/div[3]/div/h1[2]").trim();
        } catch (Exception e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (filename == null || size == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(size));
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        br.setDebug(true);
        Form forms[] = br.getForms();
        String captchaurl = null;
        if (forms.length != 8) {
            //first trying to bypass block using webproxy:
            br.setFollowRedirects(true);
            String randomain = String.valueOf((int)(Math.random()*9+1));
            br.getPage("http://www.gur"+randomain+".info/index.php");
            br.postPage("http://www.gur"+randomain+".info/index.php", "q="+downloadLink.getDownloadURL()+"&hl[include_form]=0&hl[remove_scripts]=0&hl[accept_cookies]=1&hl[show_images]=1&hl[show_referer]=0&hl[strip_meta]=0&hl[strip_title]=0&hl[session_cookies]=0") ;  
            forms = br.getForms();
            captchaurl = br.getRegex(Pattern.compile("<div.*?class=\"cont.*?<img src=\"(.*?)\"", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
            
            //formaction =  forms[3].action;          
            if (captchaurl==null) throw new PluginException(LinkStatus.ERROR_FATAL, JDLocale.L("plugins.hoster.letitbitnet.errors.countryblock","Letitbit forbidden downloading this file in your country"));


        }
        else {
            String id = forms[3].getVarsMap().get("uid");
            captchaurl = "http://letitbit.net/cap.php?jpg=" + id + ".jpg";

        }
        Form down = br.getFormbyID("Premium");
        URLConnectionAdapter con = br.openGetConnection(captchaurl);
        File file = this.getLocalCaptchaFile(this);
        Browser.download(file, con);
        down.method = Form.METHOD_POST;
        down.put("frameset", "Download+file");
        String code = Plugin.getCaptchaCode(file, this, downloadLink);
        down.put("cap", code);
        down.put("fix", "1");
        br.getPage(downloadLink.getDownloadURL());
        down.action = "http://letitbit.net/download3.php";
        br.submitForm(down);
        if (!br.containsHTML("<frame")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        br.getPage(br.getRegex("<frame.*?src=\"(.*?)\"").getMatch(0));
        String url = br.getRegex("<div.*?id=\"links\".*?>\\s+<a\\s+href=\"(.*?)\"").getMatch(0);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        this.sleep(2000, downloadLink);
        dl = br.openDownload(downloadLink, url, true, 1);
        if (dl.getConnection().getResponseCode() == 404) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        }
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 10;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}