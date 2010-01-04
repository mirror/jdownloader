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

package jd.plugins.hoster;

import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "4shared.com" }, urls = { "http://[\\w\\.]*?(4shared|4shared-china)\\.com/(file|get)/\\d+?/.*" }, flags = { 2 })
public class FourSharedCom extends PluginForHost {

    public FourSharedCom(PluginWrapper wrapper) {
        super(wrapper);
        // enablePremium("http://www.4shared.com/ref/14368016/1");
    }

    public String getAGBLink() {
        return "http://www.4shared.com/terms.jsp";
    }

    public void login(Account account) throws IOException, PluginException {
        setBrowserExclusive();
        br.getHeaders().put("4langcookie", "en");
        br.getPage("http://www.4shared.com/login.jsp");
        br.postPage("http://www.4shared.com/index.jsp", "afp=&afu=&df=&rdf=&cff=&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&openid=");
        String premlogin = br.getCookie("http://www.4shared.com", "premiumLogin");
        if (premlogin == null || !premlogin.contains("true")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (br.getCookie("http://www.4shared.com", "Password") == null || br.getCookie("http://www.4shared.com", "Login") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        try {
            this.setBrowserExclusive();
            br.getHeaders().put("4langcookie", "en");
            br.setFollowRedirects(true);
            br.getPage(downloadLink.getDownloadURL());
            if (br.containsHTML("enter a password to access")) {
                Form form = br.getFormbyProperty("name", "theForm");
                if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                if (downloadLink.getProperty("pass") != null) downloadLink.setDecrypterPassword(downloadLink.getProperty("pass").toString());
                if (downloadLink.getDecrypterPassword() == null) {
                    for (int retry = 1; retry <= 5; retry++) {
                        String pass = getUserInput("Password:", downloadLink);
                        form.put("userPass2", pass);
                        br.submitForm(form);
                        if (!br.containsHTML("enter a password to access")) {
                            downloadLink.setDecrypterPassword(pass);
                            break;
                        } else if (retry == 5) logger.severe("Wrong Password!");
                    }
                } else {
                    form.put("userPass2", downloadLink.getDecrypterPassword());
                    br.submitForm(form);
                }
            }
            String filename = br.getRegex(Pattern.compile("<title>4shared.com.*?download(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0).trim();
            String size = br.getRegex(Pattern.compile("<b>Size:</b></td>.*?<.*?>(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            if (filename == null || size == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            downloadLink.setName(filename.trim());
            downloadLink.setDownloadSize(Regex.getSize(size.replace(",", "")));
            return AvailableStatus.TRUE;
        } catch (Exception e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String url = br.getRegex("<a href=\"(http://www.4shared.com/get.*?)\" class=\".*?dbtn.*?\" tabindex=\"1\" onclick=\"return callPostDownload\\(\\);\">").getMatch(0);
        if (url == null) {
            /* maybe directdownload */
            url = br.getRegex("startDownload.*?window\\.location.*?(http://.*?)\"").getMatch(0);
            if (url == null) {
                /* maybe picture download */
                url = br.getRegex("<a href=\"(http://dc\\d+\\.4shared.com/download/.*?)\" class=\".*?dbtn.*?\" tabindex=\"1\" onclick=\"return callPostDownload\\(\\);\">").getMatch(0);
            }
            if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            br.getPage(url);
            url = br.getRegex("id=\\'divDLStart\\' >.*?<a href=\\'(.*?)\'  onclick=\"return callPostDownload\\(\\);\">Click here to download this file</a>.*?</div>").getMatch(0);
            if (url.contains("linkerror.jsp")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            sleep(Integer.parseInt(br.getRegex(" var c = (\\d+?);").getMatch(0)) * 1000l, downloadLink);
        }
        br.setDebug(true);

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, false, 1);

        String error = new Regex(dl.getConnection().getURL(), "\\?error(.*)").getMatch(0);
        if (error != null) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        }
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 10;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("red.com/get", "red.com/file"));
    }

}
