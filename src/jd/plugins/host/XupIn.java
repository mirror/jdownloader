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

package jd.plugins.host;

import java.io.IOException;

import jd.PluginWrapper;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;

public class XupIn extends PluginForHost {

    private static final String AGB_LINK = "http://www.xup.in/terms/";

    public XupIn(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());

        String filename = br.getXPathElement("/html/body/div/div/div/form/div/fieldset[2]/legend/b").substring(10).trim();
        long size = Regex.getSize(br.getXPathElement("/html/body/div/div/div/form/div/div/fieldset/div/ul/li"));
        downloadLink.setDownloadSize(size);
        downloadLink.setName(filename);

        return true;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.setDebug(true);
        this.getFileInformation(downloadLink);

        Form download = br.getForm(0);

        if (download.getVars().containsKey("vpass")) {
            download.put("vpass", Plugin.getUserInput(JDLocale.L("plugins.host.enterlinkpassword", "Please enter Linkpassword"), downloadLink));
        }
        br.openDownload(downloadLink, download);

        if (!dl.getConnection().isContentDisposition()) {
            String page = br.loadConnection(dl.getConnection());
            if (page.contains("richtige Passwort erneut ein")) { throw new PluginException(LinkStatus.ERROR_RETRY, JDLocale.L("plugins.host.xup", "Password wrong")); }
            throw new PluginException(LinkStatus.ERROR_FATAL);
        }

        dl.startDownload();

    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {

    }

    @Override
    public void resetPluginGlobals() {
    }

}