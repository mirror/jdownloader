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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pornziz.com" }, urls = { "https?://(?:www\\.)?pornziz\\.com/video/([a-z0-9\\-]+)\\-(\\d+)\\.html" })
public class PornzizCom extends PornEmbedParser {
    public PornzizCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (jd.plugins.hoster.UnknownPornScript8.isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String title = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0).replace("-", " ");
        decryptedLinks.addAll(findEmbedUrls(title));
        if (decryptedLinks.size() == 0) {
            /* Probably selfhosted content --> Pass to hostplugin */
            final DownloadLink selfhosted = this.createDownloadlink(param.getCryptedUrl());
            selfhosted.setAvailable(true);
            selfhosted.setName(title + ".mp4");
            decryptedLinks.add(selfhosted);
        }
        return decryptedLinks;
    }
}