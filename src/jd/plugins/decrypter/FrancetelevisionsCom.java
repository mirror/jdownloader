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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class FrancetelevisionsCom extends PluginForDecrypt {
    /**
     * Returns the annotations names array
     *
     * @return
     */
    public static String[] getAnnotationNames() {
        return new String[] { "france.tv", "france2.fr", "france4.fr", "france5.fr", "franceo.fr", "francetvinfo.fr", "pluzz.francetv.fr", "francetvsport.fr", "zoom.francetv.fr", "education.francetv.fr", "ludo.fr", "zouzous.fr" };
    }

    /**
     * returns the annotation pattern array
     *
     * @return
     */
    public static String[] getAnnotationUrls() {
        String[] a = new String[getAnnotationNames().length];
        int i = 0;
        for (final String domain : getAnnotationNames()) {
            a[i] = "https?://(?:www\\.)?" + Pattern.quote(domain) + "/.+";
            i++;
        }
        return a;
    }

    public FrancetelevisionsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        List<String> videoids = new ArrayList<String>();
        final String parameter = param.toString();
        final String videoid;
        if (parameter.matches("https?://(?:www\\.)?zouzous\\.fr/videos/\\d+")) {
            /* Special case - videoid is given inside url */
            videoid = new Regex(parameter, "(\\d+)$").getMatch(0);
            videoids.add(videoid + "@Zouzous_web");
        } else if (parameter.matches("https?://pluzz\\.francetv\\.fr/videos/[A-Za-z0-9\\-_]+,\\d+\\.html")) {
            /* Special case - videoid is given inside url */
            videoid = new Regex(parameter, "(\\d+)\\.html$").getMatch(0);
            videoids.add(videoid + "@Pluzz");
        } else if (parameter.matches(".+france\\.tv/.+")) {
            br.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            videoid = br.getRegex("data\\-main\\-video=\"(\\d+)\"").getMatch(0);
            /* 2017-05-10: The 'catalogue' parameter is not required anymore or not required for these URLs --> nullify that. */
            videoids.add(videoid + "@null");
        } else {
            /* Old code (or all other cases) */
            br.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            String[] links = br.getRegex("(NI_\\d+@[A-Za-z0-9\\-_]+)").getColumn(0);
            if (links == null || links.length == 0) {
                links = br.getRegex("videos\\.francetv\\.fr/video/(\\d+@[^<>\"/]+)\"").getColumn(0);
            }
            if (links != null) {
                videoids = Arrays.asList(links);
            }
        }
        for (final String singleid : videoids) {
            final DownloadLink dl = createDownloadlink("http://francetelevisionsdecrypted/" + singleid);
            dl.setContentUrl(parameter);
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }
}
