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
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.RGhostNet;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rghost.net" }, urls = { "https?://(?:[a-z0-9]+\\.)?(?:rghost\\.(?:net|ru)|rgho\\.st)/(.+)" })
public class RGhostNetCrawler extends PluginForDecrypt {
    public RGhostNetCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        RGhostNet.prepBR(this.br);
        final String contenturl = RGhostNet.correctAddedURL(param.getCryptedUrl());
        br.getPage(contenturl);
        /* Check if the file is hosted on this website (selfhosted) or on an external website such as Google Drive. */
        final String externallyHostedFileURL = br.getRegex("href=\"(https?://drive\\.google\\.com/[^\"]+)\"><i class=\"fab fa-google-drive\"").getMatch(0);
        if (externallyHostedFileURL != null) {
            final DownloadLink externallyHosted = this.createDownloadlink(externallyHostedFileURL);
            ret.add(externallyHosted);
        } else {
            final DownloadLink selfhosted = this.createDownloadlink(contenturl);
            RGhostNet.parseFileInfo(selfhosted, br);
            ret.add(selfhosted);
        }
        return ret;
    }
}
