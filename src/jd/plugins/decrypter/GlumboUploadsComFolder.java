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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "glumbouploads.com" }, urls = { "http://(www\\.)?(glumbouploads|uploads\\.glumbo)\\.com/users/[a-z0-9_]+/\\d+" }, flags = { 0 })
public class GlumboUploadsComFolder extends PluginForDecrypt {

    public GlumboUploadsComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String HOST = "glumbouploads.com";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> pages = new ArrayList<String>();
        String parameter = param.toString();
        pages.add(parameter);
        br.setFollowRedirects(true);
        br.setCookie("http://" + HOST, "lang", "english");
        br.getPage(parameter);
        if (br.containsHTML("No such user exist")) return decryptedLinks;
        final String[] allpages = br.getRegex(">\\d+</a><a href=\\'(\\?fld_id=\\d+\\&amp;[^<>\"/]*?page=\\d+)\\'").getColumn(0);
        if (allpages != null && allpages.length != 0) {
            for (final String aPage : allpages)
                if (!pages.contains(aPage)) pages.add(aPage);
        }
        int counter = 0;
        for (final String currentPage : pages) {
            if (counter != 0) br.getPage("http://glumbouploads.com/" + Encoding.htmlDecode(currentPage));
            final String[] links = br.getRegex("<div class=\"link\"><a href=\"(http://(www\\.)?" + HOST + "/[a-z0-9]{12}.*?)\"").getColumn(0);
            if (links == null) return null;
            for (String dl : links)
                decryptedLinks.add(createDownloadlink(dl));
            counter++;
        }
        return decryptedLinks;
    }

}
