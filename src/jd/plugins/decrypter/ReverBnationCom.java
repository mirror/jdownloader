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
import java.util.List;
import java.util.Map;

import org.appwork.utils.Regex;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.hoster.ReverBnationComHoster;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "reverbnation.com" }, urls = { "https?://(?:www\\.)?reverbnation\\.com/([^/]+)/songs" })
public class ReverBnationCom extends antiDDoSForDecrypt {
    public ReverBnationCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String artist = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String json = br.getRegex("var config\\s*=\\s*(\\{.*?\\});").getMatch(0);
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(json);
        /* TODO: Add pagination */
        final List<Map<String, Object>> songs = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "SONGS_WITH_PAGINATION/results");
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(artist);
        int songPosition = 1;
        for (final Map<String, Object> song : songs) {
            final DownloadLink link = this.createDownloadlink(song.get("homepage_url").toString());
            ReverBnationComHoster.parseFileInfo(link, song);
            link.setProperty(ReverBnationComHoster.PROPERTY_POSITION, songPosition);
            link._setFilePackage(fp);
            ret.add(link);
            songPosition += 1;
        }
        return ret;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}