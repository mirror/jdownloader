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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision: 18813 $", interfaceVersion = 2, names = { "video.uni-erlangen.de" }, urls = { "http://(www\\.)?video\\.uni\\-erlangen\\.de/(clip|course)/id/\\d+\\.html|http://(www\\.)?video\\.uni\\-erlangen\\.de/get/file/\\d+\\.html\\?src=download" }, flags = { 0 })
public class Vdnrlngn extends PluginForDecrypt {

    public Vdnrlngn(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (param.getCryptedUrl().toLowerCase(Locale.ENGLISH).contains("/course/")) {
            String[][] matches = br.getRegex("<td colspan=\"\\d\" class=\"cliptitel\">.*?<a href=\"(/clip/id/\\d+\\.html)\">(\\d+)\\s+\\-\\s+(.*?)</a>.*?</td>").getMatches();
            for (String[] m : matches) {

                String u = "http://www.video.uni-erlangen.de" + m[0];

                decryptedLinks.addAll(parseClip(u, m[1] + " - " + m[2]));
            }
        } else if (param.getCryptedUrl().toLowerCase(Locale.ENGLISH).contains("/clip/")) {
            decryptedLinks.addAll(parseClip(param.getCryptedUrl(), null));

        } else if (param.getCryptedUrl().toLowerCase(Locale.ENGLISH).contains("/get/file/")) {

            DownloadLink dl = createDownloadlink("directhttp://" + param.getCryptedUrl());
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }

    private Collection<? extends DownloadLink> parseClip(String u, String packagename) throws IOException {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        br.getPage(u);
        if (packagename == null) {
            packagename = br.getRegex(" <td colspan=\"3\" class=\"clipdetailtitel\">(.*?)</td>").getMatch(0).trim();
        }
        FilePackage fp = FilePackage.getInstance();
        fp.setName(packagename);

        String[] resources = br.getRegex("<div class=\"resource\">(.*?)</div>").getColumn(0);
        for (String res : resources) {
            // String type = new Regex(res,
            // "<th class=\"detail_head\">Inhalt:</th>\\s+<td class=\"detail_field\">(.*?)</td>").getMatch(0).trim();
            // String duration = new Regex(res,
            // "<th class=\"detail_head\">Dauer:</th>\\s+<td class=\"detail_field\">(.*?)</td>").getMatch(0).trim();
            String size = new Regex(res, "<th class=\"detail_head\">Gr\\&ouml\\;\\&szlig\\;e:</th>\\s+<td class=\"detail_field\">(.*?)</td>").getMatch(0).trim();
            // String resolution = new Regex(res,
            // "<th class=\"detail_head\">Aufl\\&ouml\\;sung:</th>\\s+<td class=\"detail_field\">(.*?)</td>").getMatch(0).trim();
            String download = new Regex(res, "<a class=\"download_link\" href=\"(.*?)\">").getMatch(0);
            String url = "directhttp://http://video.uni-erlangen.de" + download;
            DownloadLink dl = createDownloadlink(url);
            dl.setDownloadSize(SizeFormatter.getSize(size));

            fp.add(dl);
            decryptedLinks.add(dl);

        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}