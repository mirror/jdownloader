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

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "katholisch.de" }, urls = { "https?://(?:www\\.)?katholisch.de/video/\\d+[a-z0-9\\-]+" })
public class KatholischDe extends PluginForDecrypt {
    public KatholischDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("<title>\\s*(.*?)\\s*(-\\s*katholisch\\.de)?\\s*</").getMatch(0);
        if (title == null) {
            /* Fallback */
            title = br._getURL().getPath().replace("-", " ");
        }
        title = Encoding.htmlDecode(title).trim();
        final String m3u8 = br.getRegex("file\\s*:\\s*'(https?://[^']*\\.m3u8)'").getMatch(0);
        if (StringUtils.isEmpty(title) || StringUtils.isEmpty(m3u8)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Browser brc = br.cloneBrowser();
        brc.getPage(m3u8);
        final ArrayList<DownloadLink> ret = GenericM3u8Decrypter.parseM3U8(this, m3u8, brc, null, null, title);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        for (final DownloadLink result : ret) {
            // result.setContainerUrl(br.getURL());
            result._setFilePackage(fp);
        }
        return ret;
    }
}