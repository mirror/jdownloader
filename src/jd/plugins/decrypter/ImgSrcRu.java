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
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imgsrc.ru" }, urls = { "http://(www\\.)?imgsrc\\.ru/.*?/[a-z0-9]+\\.html" }, flags = { 0 })
public class ImgSrcRu extends PluginForDecrypt {

    public ImgSrcRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String MAINPAGE = "http://imgsrc.ru/";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.setCookie(MAINPAGE, "lang", "en");
        br.getPage(parameter);
        if (br.containsHTML(">Search for better photos") || br.getURL().contains("imgsrc.ru/main/user.php")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        // Try to view all images on one page
        String viewAll = br.getRegex("cellpadding=3 align=center><tr><td align=center> <a href=\\'(/.*?)\\'").getMatch(0);
        if (viewAll == null) viewAll = br.getRegex("\\'(/main/pic_tape\\.php\\?ad=\\d+)").getMatch(0);
        if (viewAll != null) br.getPage(MAINPAGE + viewAll);
        String[][] picLinks = br.getRegex("class=big src=(http://[^\"\\']+) alt=\\'(.*?)\\'><br></a>").getMatches();
        if (picLinks == null || picLinks.length == 0) return null;
        for (String[] aLink : picLinks) {
            DownloadLink dlink = createDownloadlink(aLink[0]);
            dlink.setFinalFileName(aLink[1]);
            decryptedLinks.add(dlink);
        }
        return decryptedLinks;
    }

}
