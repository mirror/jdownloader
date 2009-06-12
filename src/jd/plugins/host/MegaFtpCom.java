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

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDLocale;

public class MegaFtpCom extends PluginForHost {

    public MegaFtpCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public String getAGBLink() {
        return "http://upload.megaftp.com/faq.php";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception, PluginException, InterruptedException {
        this.setBrowserExclusive();

        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if(br.containsHTML("404 Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        String filename = br.getRegex("<font color=\"#FC8622\" size=\"4\">(.*?)</font>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        br.setFollowRedirects(false);        
        return AvailableStatus.TRUE;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision 6156$");
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

        //Datei hat Passwortschutz?
        if(br.containsHTML("This file is password-protected")) {
            String passCode;
            DownloadLink link = downloadLink;
            Form form = br.getFormbyProperty("name", "pswcheck");
            if (link.getStringProperty("pass", null) == null) {
                /* Usereingabe */
                passCode = Plugin.getUserInput(null, link);
            } else {
                /* gespeicherten PassCode holen */
                passCode = link.getStringProperty("pass", null);
            }

            /* Passwort übergeben */
            form.put("psw", passCode);
            br.submitForm(form);

            form = br.getFormbyProperty("name", "pswcheck");
            if (form != null && br.containsHTML("Invalid Password")) {
                link.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_FATAL, JDLocale.L("plugins.errors.wrongpassword", "Password wrong"));
            } else {
                link.setProperty("pass", passCode);
            }
        }

        Form downloadForm = br.getFormbyProperty("name", "download");
        downloadForm.put("download", Encoding.urlEncode("Click Here to Download"));

        dl = br.openDownload(downloadLink, downloadForm, false, 0);
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    // @Override
    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }
}