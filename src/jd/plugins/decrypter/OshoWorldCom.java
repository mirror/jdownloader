//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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
import jd.parser.Regex;
import jd.parser.html.InputField;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

/**
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "oshoworld.com" }, urls = { "https?://(?:www\\.)?oshoworld\\.com/[^/]+/.*?\\.asp\\?album_id=(\\d+)" }) 
public class OshoWorldCom extends PluginForDecrypt {

    public OshoWorldCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404) {
            return decryptedLinks;
        }
        String fpName = null;

        String[] results = br.getRegex("<tr>\\s*<td class=\"track_txt\".*?</td>\\s*</tr>").getColumn(-1);

        if (results != null) {
            for (String r : results) {
                final InputField i = InputField.parse(r);
                final String iFilename = i.getValue();
                final String filename = new Regex(r, "<td[^>]+>\\s*(.*?)</td>").getMatch(0);
                if (fpName == null) {
                    fpName = new Regex(filename, "(.*?)\\s*\\d+\\.mp3").getMatch(0);
                }
                final String filesize = new Regex(r, "<td[^>]+align=\"left\">\\s*(.*?)</td>").getMatch(0);
                final DownloadLink d = createDownloadlink(parameter.replaceFirst("oshoworld.com/", "oshoworlddecrypted.com/"));
                d.setName(iFilename);
                d.setDownloadSize(SizeFormatter.getSize(filesize + " MiB"));
                if (i.containsProperty("checked") && !"checked".equals(i.getProperty("checked", null))) {
                    i.putProperty("checked", "checked");
                }
                d.setProperty("iFilename", iFilename);
                d.setProperty("result", r);
                d.setLinkID(iFilename);
                d.setAvailableStatus(AvailableStatus.TRUE);
                decryptedLinks.add(d);
            }
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}