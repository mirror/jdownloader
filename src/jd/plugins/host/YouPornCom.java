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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;

public class YouPornCom extends PluginForHost {

    public YouPornCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://youporn.com/terms";
    }

    @Override
    public boolean getFileInformation(DownloadLink parameter) throws IOException {
        this.setBrowserExclusive();

        br.setFollowRedirects(true);
        br.openGetConnection(parameter.getDownloadURL());
        parameter.setName(Plugin.getFileNameFormHeader(br.getHttpConnection()));
        parameter.setDownloadSize(br.getHttpConnection().getContentLength());
        br.getHttpConnection().disconnect();
        return true;

    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {

        getFileInformation(link);

        br.openDownload(link, link.getDownloadURL()).startDownload();

    }

    public int getMaxSimultanFreeDownloadNum() {
        /* TODO: Wert prüfen */
        return 1;
    }

    @Override
    public void reset() {
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
