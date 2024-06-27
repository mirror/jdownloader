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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.plugins.controller.LazyPlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "free-loops.com" }, urls = { "https?://(www\\.)?free\\-loops\\.com/\\d+[a-z0-9\\-]+\\.html" })
public class FreeLoopsCom extends PluginForDecrypt {
    public FreeLoopsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.AUDIO_STREAMING };
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String fileid = new Regex(parameter, "free\\-loops\\.com/(\\d+)").getMatch(0);
        final String finallink = "https://free-loops.com/force-audio.php?id=" + fileid;
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(finallink);
            if (!looksLikeDownloadableContent(con)) {
                br.followConnection(true);
                if (br.containsHTML("The file doesn't seem to be here") || br.containsHTML("Go back and try another file")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                con.disconnect();
                final DownloadLink l = createDownloadlink("directhttp://" + finallink);
                l.setContentUrl(parameter);
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(true);
                brc.getPage(parameter);
                final String fileName = brc.getRegex("File:\\s*(.*?)\\s*<br/>").getMatch(0);
                final String serverFileName = Plugin.getFileNameFromConnection(con);
                if (fileName != null) {
                    l.setFinalFileName(fileName + getFileNameExtensionFromString(serverFileName, ".wav"));
                } else {
                    l.setFinalFileName(serverFileName);
                }
                if (con.getCompleteContentLength() > 0) {
                    l.setVerifiedFileSize(con.getCompleteContentLength());
                }
                decryptedLinks.add(l);
            }
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}