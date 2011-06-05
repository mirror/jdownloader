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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fernsehkritik.tv" }, urls = { "http://(www\\.)?fernsehkritik\\.tv/folge-\\d+(/Start/)?" }, flags = { PluginWrapper.DEBUG_ONLY })
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
        try {
            br.getPage(parameter);
        } catch (final Exception e) {
            logger.warning("Server temporary unavailable!");
        }
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
                String ext = finallink.substring(finallink.lastIndexOf("."), finallink.length());
                ext = ext == null ? ".vlc" : ext;
                final DownloadLink dlLink = createDownloadlink("directhttp://" + finallink);
                if (title != null) {
                    dlLink.setFinalFileName(title + ext);
                }
                fp.add(dlLink);
                decryptedLinks.add(dlLink);
            }

        } else {
            final String fpName = br.getRegex("var flattr_tle = '(.*?)'").getMatch(0);
            final String fpStart = br.getRegex("<a onclick=\"jump\\('(.*?)'\\);\" href=\"#\" id=\"start_flash_player_link\">Flash-Player starten</a>").getMatch(0);
            if (fpStart != null && !parameter.equals(fpStart)) {
                br.getPage(parameter + "/" + fpStart);
            } else {
                return null;
            }
            final String[] parts = br.getRegex("part: '(\\d+)'").getColumn(0);
            final boolean api = br.getRegex("config=/swf/ncfg\\.php\\?ep=").matches();
            if (parts == null || parts.length == 0 || !api) { return null; }
            progress.setRange(parts.length);
            fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            for (final String part : parts) {
                br.getPage("http://fernsehkritik.tv/swf/ncfg.php?ep=" + episode + "-" + part);
                final String apiR = br.toString().replaceAll("\\s", "");
                final String host = new Regex(apiR, "baseURL\":\"(.*?)\"").getMatch(0);
                final String file = new Regex(apiR, "file\":\"(.*?)\"").getMatch(0);
                if (host == null || file == null) {
                    continue;
                }
                progress.increase(1);
                final DownloadLink dlLink = createDownloadlink(host + file);
                fp.add(dlLink);
                decryptedLinks.add(dlLink);
            }
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) { return null; }
        return decryptedLinks;
    }
}
