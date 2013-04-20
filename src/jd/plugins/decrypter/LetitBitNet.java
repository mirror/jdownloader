//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "letitbit.net" }, urls = { "http://(www\\.)?letitbit\\.net/{1,2}(folder/\\d+/\\d+|page/folder/\\d+(\\||%7C)[^<>\"\\'/]+)" }, flags = { 0 })
public class LetitBitNet extends PluginForDecrypt {

    public LetitBitNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public Browser prepBr(Browser newBr) {
        newBr.setCookie("http://letitbit.net/", "lang", "en");
        return newBr;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("www.", "");
        prepBr(br);
        br.getPage(parameter);
        if (!br.containsHTML("<td width=\"700\" colspan=\"2\">") || br.containsHTML("<h2>Owner: </h2>")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        // only this link format contains spanning pages. pages/folder/ doesn't seem to!
        String uid = new Regex(parameter, "(/folder/\\d+/\\d+)").getMatch(0);
        // first page
        parsePage(decryptedLinks);
        // lets scan for additional pages
        String[] pages = br.getRegex("<li><a href=\"(" + uid + "/\\d+)\">\\d+</a></li>").getColumn(0);
        if (pages != null && pages.length != 0) {
            for (String page : pages) {
                try {
                    br.getPage(page);
                } catch (Exception e) {
                    if (br.getRequest().getHttpConnection().getResponseCode() == 400) {
                        // nginx config/setup == lame, detects or limit the amount of page requests via cookie session. Approx ~205
                        Thread.sleep(2000);
                        Browser br = new Browser();
                        prepBr(br);
                        br.getPage(this.br.getURL());
                        this.br = br;
                    } else {
                        throw e;
                    }
                }
                parsePage(decryptedLinks);
            }
        }

        return decryptedLinks;
    }

    private void parsePage(ArrayList<DownloadLink> ret) {
        String[] linkInfo = br.getRegex("<td width=\"700\" colspan=\"2\">(.*?)<hr />").getColumn(0);
        if (linkInfo == null || linkInfo.length == 0) {
            logger.warning("Could not find 'linkInfo' Regex, Please inform JDownloader Development Team!");
            return;
        }
        for (String singleInfo : linkInfo) {
            String downloadUrl = new Regex(singleInfo, "\"(http://letitbit\\.net/download/.*?)\"").getMatch(0);
            if (downloadUrl == null) {
                new Regex(singleInfo, "(http://letitbit\\.net/download/[a-z0-9\\.]+/.*?\\.html)<br").getMatch(0);
                if (downloadUrl == null) {
                    logger.warning("Could not find 'downloadUrl', Please inform JDownloader Development Team!");
                    return;
                }
            }

            DownloadLink dl = createDownloadlink(downloadUrl);
            String filename = new Regex(singleInfo, "target=\"_blank\"><font size=\"4\">(.*?)</font>").getMatch(0);
            if (filename != null) dl.setFinalFileName(filename.trim());
            String filesize = new Regex(singleInfo, "<b></b>(.*?)<br />").getMatch(0);
            if (filesize != null && !filesize.equals(" ")) dl.setDownloadSize(SizeFormatter.getSize(filesize));
            if (singleInfo.contains(">OK</font>")) {
                dl.setAvailable(true);
            } else if (singleInfo.contains("<b>BAD</b>")) {
                dl.setAvailable(false);
            }
            ret.add(dl);
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}