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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.HostPlugin;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision="$Revision", interfaceVersion=1, names = { "4shared.com"}, urls ={ "http://[\\w\\.]*?4shared.com/file/\\d+?/.*"}, flags = {0})
public class FourSharedCom extends PluginForHost {

    private static int COUNTER = 0;

    public FourSharedCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public String getAGBLink() {
        return "http://www.4shared.com/terms.jsp";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        try {
            this.setBrowserExclusive();
            br.setFollowRedirects(true);
            br.getPage(downloadLink.getDownloadURL());
            if (br.containsHTML("enter a password to access")) {
                Form form = br.getFormbyProperty("name", "theForm");
                if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
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

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    public static synchronized void increaseCounter() {
        COUNTER++;
    }

    public static synchronized void decreaseCounter() {
        COUNTER--;
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        try {
            handleFree0(downloadLink);
            // decreaseCounter();
        } catch (Exception e) {
            // decreaseCounter();

            throw e;
        }

    }

    public void handleFree0(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String url = br.getRegex("<a href=\"(http://www.4shared.com/get.*?)\" class=\".*?dbtn.*?\" tabindex=\"1\">").getMatch(0);

        br.getPage(url);
        url = br.getRegex("id=\\'divDLStart\\' >.*?<a href=\\'(.*?)\'>Click here to download this file</a>.*?</div>").getMatch(0);
        if (url.contains("linkerror.jsp")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        this.sleep(Integer.parseInt(br.getRegex(" var c = (\\d+?);").getMatch(0)) * 1000l, downloadLink);
        downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.4sharedcom.waiting", "Waiting..."));
        downloadLink.requestGuiUpdate();
        // Das wartesystem lässt link b warten während link a lädt
        // while (COUNTER > 0) {
        // Thread.sleep(100);
        // }
        // increaseCounter();
        br.setDebug(true);
        dl = br.openDownload(downloadLink, url, false, 1);

        String error = new Regex(dl.getConnection().getURL(), "\\?error(.*)").getMatch(0);
        if (error != null) { throw new PluginException(LinkStatus.ERROR_RETRY, error); }

        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 10;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }

}
