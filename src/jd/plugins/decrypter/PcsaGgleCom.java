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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "picasaweb.google.com" }, urls = { "http(s)?://(www\\.)?picasaweb\\.google\\.com/.*?/.*?(\\?feat=(featured#[0-9]+|featured#)|#[0-9]+|#|\\?authkey=[A-Za-z0-9\\-]+)" }, flags = { 0 })
public class PcsaGgleCom extends PluginForDecrypt {

    public PcsaGgleCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final Pattern singlelink = Pattern.compile(".*?#[0-9]+");

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("https://", "http://");
        br.getPage(parameter);
        /* Error handling */
        if (br.containsHTML("(Hier gibt es nichts zu sehen|Entweder haben Sie keinen Zugriff auf diese Fotos oder es gibt unter dieser Adresse keine)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        if (Regex.matches(parameter, singlelink)) {
            String picid = new Regex(parameter, ".*?#(\\d+)").getMatch(0);
            String directLinkRegex = "\"" + picid + "\",\"albumId\":\".*?\",\"access\":\"public\",.*?media\":\\{\"content\":\\[\\{\"url\":\"(http.*?)\"";
            String finallink = br.getRegex(directLinkRegex).getMatch(0);
            if (finallink == null) return null;
            DownloadLink dl = createDownloadlink("directhttp://" + finallink);
            decryptedLinks.add(dl);
        } else {
            String fpname = br.getRegex("title\" content=\"(.*?)\"").getMatch(0);
            if (fpname == null) {
                fpname = br.getRegex("title:\\'(.*?)\\'").getMatch(0);
                if (fpname == null) {
                    fpname = br.getRegex("album\\.title_wb = \"(.*?)\"").getMatch(0);
                }
            }
            // Make the regex to find alle the pic ids
            String galleryName = new Regex(parameter, "picasaweb\\.google\\.com/(.*?/.*?)(\\?feat|#)").getMatch(0);
            if (galleryName == null) galleryName = new Regex(parameter, "picasaweb\\.google\\.com/.*?\\?authkey=(.+)").getMatch(0);
            if (galleryName == null) return null;
            String theFinalRegex = "(" + galleryName + "#[0-9]+)\"";
            // Use the regex
            String[] picLinks = br.getRegex(theFinalRegex).getColumn(0);
            if (picLinks == null || picLinks.length == 0) return null;
            progress.setRange(picLinks.length);
            int i = 1;
            for (String piclink : picLinks) {
                String pictureoverview = null;
                if (parameter.contains("?authkey=")) {
                    pictureoverview = "http://picasaweb.google.com/" + new Regex(parameter, "picasaweb\\.google\\.com/(.*?\\?authkey=)").getMatch(0) + piclink;
                } else {
                    pictureoverview = "http://picasaweb.google.com/" + piclink;
                }
                br.getPage(pictureoverview);
                String picid = new Regex(piclink, "#(\\d+)").getMatch(0);
                String finallink = br.getRegex("\":\"" + picid + "\",\"albumId\":\"(\\d+)\",\"access\":\"(private|public)\",\"width\":\"\\d+\",\"height\":\"\\d+\",\"size\":\"\\d+\",\"commentingEnabled\":\"(true|false)\",\"allowNameTags\":\"(false|true)\",\"media\":\\{\"content\":\\[\\{\"url\":\"(http://.*?)\"").getMatch(4);
                DownloadLink dl = createDownloadlink("directhttp://" + finallink);
                String ending = new Regex(finallink, "\\.com/.*?/.{1,}(\\..{3,4})").getMatch(0);
                if (fpname != null && ending != null) {
                    String finalName = fpname.trim() + "_" + i + ending;
                    dl.setFinalFileName(finalName);
                }
                decryptedLinks.add(dl);
                progress.increase(1);
                i = i + 1;
            }
            if (fpname != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpname.trim());
                fp.addLinks(decryptedLinks);
            }
        }

        return decryptedLinks;
    }
}