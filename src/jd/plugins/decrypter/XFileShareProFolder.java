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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 15695 $", interfaceVersion = 2, names = { "XFileShareProFolder" }, urls = { "http://(www\\.)?(mojofile\\.com|fileduct\\.com)/(users/[a-z0-9_]+/.+|folder/\\d+/.+)" }, flags = { 0 })
public class XFileShareProFolder extends PluginForDecrypt {

    // DEV NOTES
    // other: keep last /.+ for fpName. Not needed otherwise.
    // TODO: add spanning folders + page support, at this stage it's not
    // important.
    // TODO: remove old xfileshare folder plugins after next major update.

    private String HOST = "";

    public XFileShareProFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        HOST = new Regex(parameter, "https?://([^/]+)").getMatch(0);
        if (HOST == null) {
            logger.warning("Failure finding HOST : " + parameter);
            return null;
        }
        br.setFollowRedirects(true);
        br.setCookie("http://" + HOST, "lang", "english");
        br.getPage(parameter);
        if (br.containsHTML("No such user exist")) {
            logger.warning("Incorrect URL or Invalid user : " + parameter);
            return null;
        }
        String[] links = br.getRegex("<div class=\"link\"><a href=\"(.+?" + HOST + "[^\"]+)").getColumn(0);
        if (links == null || links.length == 0) links = br.getRegex("<b>Filename: </b><a href=\"(.+?" + HOST + "[^\"]+)").getColumn(0);
        if (links == null || links.length == 0) return null;
        for (String dl : links)
            decryptedLinks.add(createDownloadlink(dl));
        // name isn't needed, other than than text output for fpName.
        String fpName = new Regex(parameter, "folder/\\d+/.+/(.+)").getMatch(0); // name
        if (fpName == null) {
            fpName = new Regex(parameter, "folder/\\d+/(.+)").getMatch(0); // id
            if (fpName == null) {
                fpName = new Regex(parameter, "users/[a-z0-9_]+/.+/(.+)").getMatch(0); // name
                if (fpName == null) {
                    fpName = new Regex(parameter, "users/[a-z0-9_]+/(.+)").getMatch(0); // id
                }
            }
        }
        if (fpName != null) {
            fpName = "Folder - " + (Encoding.urlDecode(fpName, false));
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        HOST = "";
        return decryptedLinks;
    }
}