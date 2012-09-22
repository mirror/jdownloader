//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fernsehkritik.tv" }, urls = { "http://(www\\.)?fernsehkritik\\.tv/folge\\-\\d+" }, flags = { 0 })
public class FernsehkritikTvA extends PluginForDecrypt {

    public FernsehkritikTvA(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        FilePackage fp;
        final String parameter = param.toString();
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.getPage(parameter);
        final String episode = new Regex(parameter, "folge-(\\d+)").getMatch(0);
        if (episode == null) { return null; }
        if (Integer.valueOf(episode) < 69) {
            final String[] finallinks = br.getRegex("\n\\s+<a href=\"(.*?)\">.*?").getColumn(0);
            final String title = br.getRegex("<a id=\"eptitle\".*?>(.*?)<").getMatch(0);
            if (finallinks == null || finallinks.length == 0 || title == null) { return null; }
            fp = FilePackage.getInstance();
            fp.setName(title);
            for (final String finallink : finallinks) {
                // mms not supported
                if (finallink.startsWith("mms")) {
                    continue;
                }
                final DownloadLink dlLink = createDownloadlink("directhttp://" + finallink);
                if (title != null) {
                    dlLink.setFinalFileName(title + fileExtension(finallink));
                }
                fp.add(dlLink);
                decryptedLinks.add(dlLink);
            }

        } else {
            br.getPage(parameter + "/Start/");
            final String fpName = br.getRegex("var flattr_tle = \\'(.*?)\\'").getMatch(0);
            final String[] jumps = br.getRegex("url: base \\+ \\'100\\-(\\d+)\\.flv\\'").getColumn(0);
            if (jumps == null || jumps.length == 0) {
                logger.warning("FATAL error, no parts found for link: " + parameter);
                return null;
            }
            ArrayList<String> parts = new ArrayList<String>();
            parts.add("1");
            for (String jump : jumps)
                if (!parts.contains(jump)) parts.add(jump);
            fp = FilePackage.getInstance();
            fp.setName(fpName);
            for (final String part : parts) {
                String partname = null;
                if (part.equals("1"))
                    partname = episode + ".flv";
                else
                    partname = episode + "-" + part + ".flv";
                final DownloadLink dlLink = createDownloadlink("http://fernsehkritik.tv/jdownloaderfolge" + partname);
                dlLink.setFinalFileName(fpName + "_Teil" + part + ".flv");
                fp.add(dlLink);
                dlLink.setAvailable(true);
                decryptedLinks.add(dlLink);
            }
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) { return null; }
        return decryptedLinks;
    }

    private String fileExtension(final String arg) {
        String ext = arg.substring(arg.lastIndexOf("."));
        ext = ext == null ? ".flv" : ext;
        return ext;
    }

}
