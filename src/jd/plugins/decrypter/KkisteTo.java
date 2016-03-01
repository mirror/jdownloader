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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "kkiste.to" }, urls = { "http://(?:www\\.)?kkiste\\.to/[a-z0-9\\-]+\\.html" }, flags = { 0 })
public class KkisteTo extends PluginForDecrypt {

    public KkisteTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final DecimalFormat df = new DecimalFormat("00");
        final String ecostream_default_extension = jd.plugins.hoster.EcoStreamTv.default_extension;
        int counter = 1;
        final String url_name = new Regex(parameter, "kkiste\\.to/([a-z0-9\\-]+)\\.html").getMatch(0);
        String fpName = br.getRegex("<title>([^<>\"]*?) \\| Stream auf KKiste\\.to</title>").getMatch(0);
        if (fpName == null) {
            fpName = url_name;
        }
        fpName = Encoding.htmlDecode(fpName.trim());

        final String[] seasons = this.br.getRegex("<option value=\"\\d+\">Staffel (\\d+)</option>").getColumn(0);
        final String[] links = br.getRegex("class=\"free\"><a href=\"(http[^<>\"]*?)\"").getColumn(0);
        if (links != null && links.length > 0) {
            /* E..g. movies (no seasons & episodes!) */
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            for (final String singleLink : links) {
                final DownloadLink dl = createDownloadlink(singleLink);
                final String decrypterfilename = fpName + "_part_" + df.format(counter);
                dl.setProperty("decrypterfilename", decrypterfilename);
                dl.setName(decrypterfilename + ecostream_default_extension);
                dl._setFilePackage(fp);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                counter++;
            }
            return decryptedLinks;
        }
        if (seasons == null || seasons.length == 0) {
            if (!this.br.containsHTML("class=\"free\"") && !this.br.containsHTML("class=\"seasonselect\"")) {
                /* There is nothing to download - the content has not yet been published! */
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        /* E.g. series with multiple seasons and episodes */
        this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        for (final String season : seasons) {
            /* Reset counter as we re-use it for all episodes of every season ;) */
            counter = 1;
            final int season_int = Integer.parseInt(season);
            if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                return decryptedLinks;
            }
            this.br.postPage("/xhr/movies/episodes/" + url_name + "/", "season=" + season);
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
            final ArrayList<Object> ressourcelist = (ArrayList) entries.get("episodes");
            final FilePackage fp = FilePackage.getInstance();
            final String season_formatted = "S" + df.format(season_int);
            fp.setName(fpName + " " + season_formatted);
            for (final Object singleEpisode : ressourcelist) {
                entries = (LinkedHashMap<String, Object>) singleEpisode;
                String decrypterfilename = (String) entries.get("part");
                final String linkid = (String) entries.get("link");
                if (decrypterfilename == null || true) {
                    decrypterfilename = fpName + "_" + season_formatted + "E" + df.format(counter);
                }
                final DownloadLink dl = createDownloadlink("http://www.ecostream.tv/stream/" + linkid + ".html");
                dl.setProperty("decrypterfilename", decrypterfilename);
                dl.setName(decrypterfilename + ecostream_default_extension);
                dl._setFilePackage(fp);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                distribute(dl);
                counter++;
            }
        }

        return decryptedLinks;
    }

}
