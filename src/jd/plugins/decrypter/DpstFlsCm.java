//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "depositfiles.com" }, urls = { "https?://(www\\.)?(depositfiles\\.com|dfiles\\.(eu|ru))/([a-z]+/)?folders/.+" }, flags = { 0 })
public class DpstFlsCm extends PluginForDecrypt {

    private static String MAINPAGE = null;
    private static String DOMAINS  = null;

    public DpstFlsCm(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (MAINPAGE == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("depositfiles.com");
            MAINPAGE = jd.plugins.hoster.DepositFiles.MAINPAGE;
            DOMAINS = jd.plugins.hoster.DepositFiles.DOMAINS;
            if (MAINPAGE == null || DOMAINS == null) {
                logger.warning("Contant setters failed.");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }

        String url = parameter.toString();
        int pagecount = 1;
        String id = new Regex(url, "folders/(.+)").getMatch(0);
        url = MAINPAGE + "/de/folders/" + id;
        // Get Pagecount //
        if (url.contains("page")) url = url.split("\\?")[0];
        br.getPage(url);
        if (br.containsHTML("\\&gt;\\&gt;\\&gt;")) {
            final String[] pages = br.getRegex("\\?page=(\\d+)\">\\d+</a>").getColumn(0);
            if (pages != null && pages.length != 0) {
                for (final String currentPage : pages)
                    if (Integer.parseInt(currentPage) > pagecount) pagecount = Integer.parseInt(currentPage);
            }
        }
        for (int x = 1; x <= pagecount; x++) {
            br.getPage(url + "?page=" + x + "&format=text");
            String[] finalLinks = br.getRegex("(https?://" + DOMAINS + "/files/[0-9a-z]+)").getColumn(0);
            for (String data : finalLinks) {
                decryptedLinks.add(createDownloadlink(data));
            }
        }
        return decryptedLinks;
    }

}
