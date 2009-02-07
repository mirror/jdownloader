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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;

import jd.http.HTTPConnection;

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
        getFileInformation(downloadLink);
        br.setFollowRedirects(true);
        String link = br.getRegex(Pattern.compile("<DIV id=\"pnlLink1\">.*?href=\"(.*?)\".*?</DIV>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        br.getPage("http://www.axifile.com/javascript/donwload.js");
        br.setCookie("http://" + br.getHost(), "pv", br.getRegex("setCookie\\(\"pv\",\"(.*?)\"").getMatch(0));
        br.setCookie("http://" + br.getHost(), "flv", "y");
        HTTPConnection con = br.openGetConnection("http://www.axifile.com/flash/baner.swf");
        BufferedInputStream is = new BufferedInputStream(con.getInputStream());

        int i;
        char[] lastone = new char[11];
        ArrayList<Character> pool = new ArrayList<Character>();
        ArrayList<Short> ifs = new ArrayList<Short>();
        ArrayList<Short> poolindex = new ArrayList<Short>();
        while ((i = is.read()) != -1) {
            char[] backlast = lastone;
            for (int j = 1; j < backlast.length; j++) {
                lastone[j - 1] = backlast[j];
            }
            lastone[10] = (char) i;
            if (lastone[7] == 0x00 && lastone[6] == 0x02 && lastone[5] == 0x9D && lastone[4] == 0x49) ifs.add((short) lastone[0]);
            if (lastone[0] == 0x00 && lastone[2] == 0x00 && ("" + (char) lastone[1]).matches("[0-9a-f]")) pool.add((char) lastone[1]);
            if (lastone[0] == 0x00 && lastone[1] == 0x08 && lastone[2] == 0x00 && lastone[3] == 0x96 && lastone[4] == 0x04 && lastone[5] == 0x00 && lastone[6] == 0x08 && lastone[7] == 0x0F && lastone[8] == 0x08 && lastone[10] == 0x1D) poolindex.add((short) (lastone[9] - 0x11));

        }
        Collections.reverse(poolindex);

        char[] mycode = new char[32];
        for (int j = 0; j < mycode.length; j++) {
            mycode[ifs.get(j)] = pool.get(poolindex.get(j));
        }
        is.close();
        String code = new String(mycode);

        if (link == null && code != null) throw new PluginException(LinkStatus.ERROR_FATAL);
        dl = br.openDownload(downloadLink, link.replaceFirst(".*?axifile.com/.*?/", "http://dl.axifile.com/" + code + "/"), false, 3);
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
