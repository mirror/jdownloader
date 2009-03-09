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

import java.io.IOException;

import jd.PluginWrapper;
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
        parameter.setDownloadSize(br.getHttpConnection().getLongContentLength());
        br.getHttpConnection().disconnect();
        return true;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        getFileInformation(link);
        br.openDownload(link, link.getDownloadURL()).startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public String getVersion() {

        return getVersion("$Revision$");
    }
}
