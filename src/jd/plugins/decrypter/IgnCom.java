//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ign.com" }, urls = { "http://(www\\.)?pc\\.ign\\.com/dor/objects/\\d+/[A-Za-z0-9_\\-]+/videos/.*?\\d+\\.html" }, flags = { 0 })
public class IgnCom extends PluginForDecrypt {

    public IgnCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML("No htmlCode read")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String fpName = br.getRegex("<meta property=\"og:title\" content=\"(.*?) \\- IGN\"").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<h1 class=\"grid_16 container hdr\\-video\\-title\">(.*?)</h1>").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("var disqus_title=\"(.*?)\";").getMatch(0);
                if (fpName == null) {
                    fpName = br.getRegex("<title>(.*?) Video \\- ").getMatch(0);
                }
            }
        }
        if (fpName == null) return null;
        fpName = fpName.trim();
        String configUrl = br.getRegex("\"config_episodic\":\"(http:.*?)\"").getMatch(0);
        if (configUrl == null) return null;
        configUrl = configUrl.replace("\\", "");
        br.getPage(configUrl);
        boolean failed = true;
        String regexes[] = { "<downloadable src=\"(http://.*?)\"", "format\\-height=\"\\d+\" mime-type=\"video/mp4\" src=\"(http://.*?)\"" };
        for (String regex : regexes) {
            String[] links = br.getRegex(regex).getColumn(0);
            if (links != null && links.length != 0) {
                failed = false;
                for (String singleLink : links) {
                    DownloadLink dlink = createDownloadlink("directhttp://" + singleLink);
                    dlink.setFinalFileName(fpName + singleLink.substring(singleLink.length() - 4, singleLink.length()));
                    decryptedLinks.add(dlink);
                }
            }
        }
        if (failed) return null;
        FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName.trim());
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

}
