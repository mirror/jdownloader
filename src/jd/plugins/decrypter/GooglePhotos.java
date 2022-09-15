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
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "photos.google.com" }, urls = { "https?://photos\\.google\\.com/share/[A-Za-z0-9\\-_]+\\?key=[A-Za-z0-9\\-_]+|https?://photos\\.app\\.goo\\.gl/[A-Za-z0-9]+" })
public class GooglePhotos extends PluginForDecrypt {
    public GooglePhotos(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final boolean fastlinkcheck = SubConfiguration.getConfig(this.getHost()).getBooleanProperty(jd.plugins.hoster.GooglePhotos.FAST_LINKCHECK, true);
        // use english not german!
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">Album is empty<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex urlinfo = new Regex(br.getURL(), "/share/([A-Za-z0-9\\-_]+)\\?key=([A-Za-z0-9\\-_]+)");
        final String idMAIN = urlinfo.getMatch(0);
        final String key = urlinfo.getMatch(1);
        if (idMAIN == null || key == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String[] ids = br.getRegex("\\[\"([A-Za-z0-9\\-_]{22,})\",\\[\"https").getColumn(0);
        String fpName = br.getRegex("(?i)<title>(?:Shared album\\s*-\\s*)?[A-Za-z0-9 ]+ – ([^<>\"]+)\\s*-\\s*Google (?:Ph|F)otos</title>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("property=\"og:title\" content=\"([^\"]+)").getMatch(0);
        }
        if (fpName == null) {
            fpName = idMAIN;
        }
        if (ids == null || ids.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String idSINGLE : ids) {
            final String finallink = "https://photos.google.com/share/" + idMAIN + "/photo/" + idSINGLE + "?key=" + key;
            final DownloadLink dl = createDownloadlink(finallink);
            dl.setName(idSINGLE);
            /* 2020-08-28: Don't do this, it could be either a photo or a video */
            // dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
            if (fastlinkcheck) {
                dl.setAvailable(true);
            }
            ret.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName).trim());
        fp.addLinks(ret);
        return ret;
    }
}
