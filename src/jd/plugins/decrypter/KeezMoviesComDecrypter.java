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

import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "keezmovies.com" }, urls = { "https?://(www\\.)?keezmovies\\.com/(video|embed)/[\\w\\-]+" })
public class KeezMoviesComDecrypter extends PornEmbedParser {
    public KeezMoviesComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("/embed/", "/video/");
        final DownloadLink decryptedMainlink = createDownloadlink(parameter.replace("keezmovies.com/", "keezmoviesdecrypted.com/"));
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        String filename = br.getRegex("<span class=\"fn\" style=\"display:none\">([^<>\"]*?)</span>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>(.*?) \\- KeezMovies\\.com</title>").getMatch(0);
        }
        if (filename != null) {
            // cleanup
            filename = StringUtils.startsWithCaseInsensitive(StringUtils.trim(filename), "http") ? null : filename;
        }
        decryptedLinks.addAll(findEmbedUrls(filename));
        if (!decryptedLinks.isEmpty()) {
            return decryptedLinks;
        }
        /* No external url found --> Video must be selfhosted. */
        decryptedLinks.add(decryptedMainlink);
        return decryptedLinks;
    }
}
