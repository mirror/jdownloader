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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

/**
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision: 37318 $", interfaceVersion = 2, names = { "emuparadise.me" }, urls = { "https?://(www\\.)?emuparadise\\.me/[^<>/]+/[^<>/]+/\\d{4,}" })
public class EmuParadiseMe extends PluginForDecrypt {

    public EmuParadiseMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final PluginForHost plugin = JDUtilities.getPluginForHost("emuparadise.me");
        if (plugin == null) {
            throw new IllegalStateException("emuparadise plugin not found!");
        }
        // set cross browser support
        ((jd.plugins.hoster.EmuParadiseMe) plugin).setBrowser(br);
        ((jd.plugins.hoster.EmuParadiseMe) plugin).prepBrowser();
        ((jd.plugins.hoster.EmuParadiseMe) plugin).setCookies();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (((jd.plugins.hoster.EmuParadiseMe) plugin).offlineCheck()) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        // many links? single link?
        final String[] links = br.getRegex(((jd.plugins.hoster.EmuParadiseMe) plugin).subURL).getColumn(-1);
        if (links != null && links.length > 0) {
            // many
            final String fpName = PluginJSonUtils.getJson(br, "name");
            final FilePackage fp;
            if (fpName != null) {
                fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            } else {
                fp = null;
            }
            for (final String link : links) {
                final String url = Request.getLocation(link, br.getRequest());
                final DownloadLink dl = createDownloadlink(url);
                final Regex s = new Regex(br, "<a href=\"" + Pattern.quote(link) + "\" title=\"Download (.*?) ISO for.*?\".*?</a>\\s*\\((\\d+(?:\\.\\d+)?[KMG]{1}[B]{0,1})\\)");
                final String name = s.getMatch(0);
                String size = s.getMatch(1);
                dl.setName(name);
                dl.setDownloadSize(SizeFormatter.getSize(((jd.plugins.hoster.EmuParadiseMe) plugin).correctFilesize(size)));
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                if (fp != null) {
                    fp.add(dl);
                }
            }
        } else {
            // single link
            final DownloadLink singlink = createDownloadlink(br.getURL());
            final String filename = ((jd.plugins.hoster.EmuParadiseMe) plugin).getFileName();
            final String filesize = ((jd.plugins.hoster.EmuParadiseMe) plugin).correctFilesize(((jd.plugins.hoster.EmuParadiseMe) plugin).getFileSize());
            if (filename != null) {
                singlink.setName(Encoding.htmlDecode(filename));
            }
            if (filesize != null) {
                singlink.setDownloadSize(SizeFormatter.getSize(filesize));
            }
            singlink.setAvailable(true);
            decryptedLinks.add(singlink);
        }
        return decryptedLinks;
    }
}
