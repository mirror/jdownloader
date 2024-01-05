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

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.hoster.DefineBabeCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "definebabe.com" }, urls = { "https?://(?:www\\.)?definebabes?\\.com/video/([a-z0-9]+)/([a-z0-9\\-]+)/" })
public class DefinebabeComDecrypter extends PornEmbedParser {
    public DefinebabeComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    @Override
    protected boolean isOffline(final Browser br) {
        return DefineBabeCom.isOffline(this.br);
    }

    public static String getURLTitleCleaned(final String url) {
        String urlTitle = new Regex(url, "([a-z0-9\\-]+)/?$").getMatch(0);
        if (urlTitle != null) {
            urlTitle = urlTitle.replace("-", " ").trim();
            return urlTitle;
        } else {
            return null;
        }
    }

    @Override
    protected String getFileTitle(final CryptedLink param, final Browser br) {
        return getFileTitle(br);
    }

    @Override
    protected boolean isSelfhosted(final Browser br) {
        final String videoID = DefineBabeCom.getVideoID(br);
        if (videoID != null) {
            return true;
        } else {
            return false;
        }
    }

    public static String getFileTitle(final Browser br) {
        String title = br.getRegex("<div id=\"sp\">\\s*?<b>([^<>\"]+)</b>").getMatch(0);
        if (title == null) {
            /* 2022-08-23 */
            title = br.getRegex("property=\"og:title\" content=\"([^\"]+)").getMatch(0);
            if (title == null) {
                /* 2023-01-05 */
                title = br.getRegex("<title>([^<]+)</title>").getMatch(0);
            }
        }
        if (title == null) {
            /* Fallback */
            title = getURLTitleCleaned(br.getURL());
        }
        title = Encoding.htmlDecode(title).trim();
        return title;
    }
}
