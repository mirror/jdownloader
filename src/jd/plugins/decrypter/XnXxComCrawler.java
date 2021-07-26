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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.XnXxCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "xnxx.com" }, urls = { "https?://[\\w\\.]*?xnxx\\.com/video-([a-z0-9\\-]+)(/[^/]+)?" })
public class XnXxComCrawler extends PluginForDecrypt {
    public XnXxComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        param.setCryptedUrl(XnXxCom.correctURL(param.getCryptedUrl()));
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (XnXxCom.isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* (Most of?) all content is hosted on xvideos.com and "double-embedded" on xnxx.com. */
        final String finallink = this.br.getRegex("(https?://(?:www\\.)?xvideos\\.com/embedframe/\\d+)").getMatch(0);
        if (finallink == null) {
            final DownloadLink selfhosted = this.createDownloadlink(param.getCryptedUrl());
            selfhosted.setAvailable(true);
            decryptedLinks.add(selfhosted);
        } else {
            decryptedLinks.add(createDownloadlink(finallink));
        }
        return decryptedLinks;
    }
}
