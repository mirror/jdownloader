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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class AxiFileCom extends PluginForHost {
    /*
     * TODO: PW support, problem: filename und filesize sind ohne pw nicht zu
     * bekommen
     */
    public AxiFileCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.axifile.com/terms.php";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        br.setCookiesExclusive(true);
        br.getPage(downloadLink.getDownloadURL());

        String filesize = br.getRegex(Pattern.compile("You have request \".*?\" file \\((.*?)\\)<SCRIPT", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        String filename = br.getRegex("You have request \"(.*?)\".*?<SCRIPT").getMatch(0);
        if (filesize == null || filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        getFileInformation(downloadLink);
        sleep(1 * 60000l, downloadLink);/*
                                         * ne kleine pause damit auf timeouts am
                                         * server gewartet werden kann
                                         */
        br.setFollowRedirects(true);
        String link = br.getRegex(Pattern.compile("<DIV id=\"pnlLink1\">.*?href=\"(.*?)\".*?</DIV>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        sleep(35000l, downloadLink);
        if (link == null) throw new PluginException(LinkStatus.ERROR_FATAL);
        dl = br.openDownload(downloadLink, link, true, 3);
        /*
         * hoster supported wahlweise 3 files mit 1 chunk oder 1 file mit 3
         * chunks
         */
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

}
