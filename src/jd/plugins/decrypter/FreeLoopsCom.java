//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.URLConnectionAdapter;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "free-loops.com" }, urls = { "http://[\\w\\.]*?free-loops\\.com/(download-free-loop-[0-9]+|download\\.php\\?id=[0-9]+|audio\\.php\\?term=((bass|drum kit|drum loop|instrument|midi|pad|sound effect|synth|vocal)&page=[0-9]+|(bass|drum kit|drum loop|instrument|midi|pad|sound effect|synth|vocal)))" }, flags = { 0 })
public class FreeLoopsCom extends PluginForDecrypt {

    public FreeLoopsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.contains("download-free-loop-") || parameter.contains("download.php?id=")) {
            String fileid = new Regex(parameter, "download-free-loop-(\\d+)").getMatch(0);
            if (fileid == null) fileid = new Regex(parameter, "download\\.php\\?id=(\\d+)").getMatch(0);
            String finallink = "http://free-loops.com/force-audio.php?id=" + fileid;
            URLConnectionAdapter con = br.openGetConnection(finallink);
            if ((con.getContentType().contains("html"))) {
                br.followConnection();
                if (br.containsHTML("The file doesn't seem to be here") || br.containsHTML("Go back and try another file")) {
                    logger.warning("The requested document was not found on this server.");
                    logger.warning(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
                    return new ArrayList<DownloadLink>();
                }
                return null;
            } else {
                DownloadLink l;
                decryptedLinks.add(l = createDownloadlink("directhttp://" + finallink));
                l.setFinalFileName(Plugin.getFileNameFromHeader(con));
                l.setDownloadSize(con.getLongContentLength());
                con.disconnect();
            }
        } else {
            br.getPage(parameter);
            String pagepiece = br.getRegex("<tr class=\"row-a\">(.*?)<td class=\"row-b\">").getMatch(0);
            if (pagepiece == null) return null;
            String[] links = new Regex(pagepiece, "href='(download-free-loop-[0-9]+).*?'").getColumn(0);
            if (links.length == 0) return null;
            progress.setRange(links.length);
            for (String dl : links) {
                String fileid = new Regex(dl, "download-free-loop-(\\d+)").getMatch(0);
                String finallink = "http://free-loops.com/force-audio.php?id=" + fileid;
                URLConnectionAdapter con = br.openGetConnection(finallink);
                if ((con.getContentType().contains("html"))) {
                    br.followConnection();
                    if (br.containsHTML("The file doesn't seem to be here") || br.containsHTML("Go back and try another file")) {
                        logger.warning("The requested document was not found on this server.");
                        logger.warning(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
                    }
                    progress.increase(1);
                } else {
                    DownloadLink l;
                    decryptedLinks.add(l = createDownloadlink("directhttp://" + finallink));
                    l.setFinalFileName(Plugin.getFileNameFromHeader(con));
                    l.setDownloadSize(con.getLongContentLength());
                    con.disconnect();
                    progress.increase(1);
                }
            }
        }
        return decryptedLinks;
    }
}
