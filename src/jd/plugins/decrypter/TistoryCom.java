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

import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tistory.com" }, urls = { "https?://[a-z0-9]+\\.tistory\\.com/\\d+" })
public class TistoryCom extends PluginForDecrypt {
    public TistoryCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String addedurl = param.toString();
        br.setFollowRedirects(true);
        br.getPage(addedurl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String fpName = br.getRegex("<title>[^\"/]+ :: ([^<>\"]+)</title>").getMatch(0);
        final String[] links = br.getRegex("class=\"imageblock\".*?<img src=\"(http[^<>\"]+)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter may be broken (not all links have pictures) for link: " + addedurl);
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        for (final String singleLink : links) {
            final DownloadLink dl = createDownloadlink(DirectHTTP.createURLForThisPlugin(singleLink));
            dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
            dl.setAvailable(true);
            ret.add(dl);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
            fp.addLinks(ret);
        }
        return ret;
    }
}
