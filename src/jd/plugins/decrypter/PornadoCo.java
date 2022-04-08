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

import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pornado.co", "tubesafari.com" }, urls = { "https?://(?:www\\.)?pornado\\.co/video\\?id=[a-z0-9\\-_]+(?:\\&d=.+)?", "https?://(?:www\\.)?tubesafari\\.com/video\\?id=[a-z0-9\\-_]+(?:\\&d=.+)?" })
public class PornadoCo extends PornEmbedParser {
    public PornadoCo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /** E.g. more domains: xvirgo.com */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final UrlQuery query = UrlQuery.parse(param.getCryptedUrl());
        final String id = query.get("id");
        final String d = query.get("d");
        if (id.matches("ph[a-f0-9]+") && d == null) {
            /* 2021-03-26 */
            decryptedLinks.add(this.createDownloadlink("https://www.pornhub.com/embed/" + id));
            return decryptedLinks;
        }
        br.getPage(param.getCryptedUrl());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return decryptedLinks;
        }
        String filename = br.getRegex("<title>([^<>\"]+) \\- [^<>\"/]+</title>").getMatch(0);
        if (filename == null) {
            filename = new Regex(param.getCryptedUrl(), "\\&d=(.+)$").getMatch(0);
        }
        decryptedLinks.addAll(findEmbedUrls(filename));
        return decryptedLinks;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}