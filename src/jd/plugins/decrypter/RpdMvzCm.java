//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rapidmoviez.com" }, urls = { "https?://(www\\.)?(?:rapidmoviez\\.com|rmz\\.rezavn|rmz\\.cr|rapidmoviez\\.eu)/release/[a-z0-9\\-]+" })
public class RpdMvzCm extends antiDDoSForDecrypt {

    public RpdMvzCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            // define custom browser headers and language settings.
            prepBr.getHeaders().put("Cache-Control", null);
        }
        return prepBr;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // no https
        final String parameter = param.toString().replace("https://", "http://").replace("rapidmoviez.com/", "rmz.cr/").replace("rmz.rezavn.com/)", "rmz.cr/");

        getPage(parameter);

        if (br.containsHTML("<title>404 Page Not Found</title>")) {
            logger.info("Incorrect URL: " + parameter);
            return decryptedLinks;
        }

        final String fpName = br.getRegex("<h2>(.*?)<br />").getMatch(0);
        String filter = br.getRegex("<div class=\"fullsize\">Download</div>(.*?)<h5><").getMatch(0);

        final String links[] = HTMLParser.getHttpLinks(filter, "");
        if (links != null) {
            for (final String link : links) {
                decryptedLinks.add(createDownloadlink(link));
            }
        }
        if (decryptedLinks.isEmpty()) {

            String js = br.getRegex("<script[^\r\n]+src=\"(/j/\\d+[^\"]+\\.js)").getMatch(0);
            if (js == null) {
                logger.warning("Can not find 'js', Please report this to JDownloader Development Team : " + parameter);
                return null;
            }

            br.getHeaders().put("Accept", "*/*");
            getPage(js);

            filter = br.getRegex("\\(([\\d,]+)\\)").getMatch(0);
            if (filter == null) {
                logger.warning("Can not find 'filter', Please report this to JDownloader Development Team : " + parameter);
                return null;
            }
            String[] charset = filter.split(",");

            StringBuilder builder = new StringBuilder(charset.length);
            for (String chR : charset) {
                builder.append(Character.toChars(Integer.parseInt(chR)));
            }

            String[] results = new Regex(builder.toString(), "(txt \\+= 'http.*?txt \\+= \"\\\\n\";)").getColumn(0);
            if (results != null && results.length != 0) {
                for (String result : results) {
                    StringBuilder out = new StringBuilder(results.length);
                    String[] data = new Regex(result, "txt \\+= '([^']{1,})'").getColumn(0);
                    if (data == null) {
                        continue;
                    }
                    for (String res : data) {
                        out.append(res);
                    }
                    decryptedLinks.add(createDownloadlink(out.toString()));
                }
            } else {
                logger.warning("Can not find 'results', Please report this to JDownloader Development Team : " + parameter);
                return null;
            }
        }

        if (decryptedLinks.isEmpty()) {
            logger.warning("'decrptedLinks' isEmpty!, Please report this to JDownloader Development Team : " + parameter);
            return null;
        }

        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
            fp.setProperty("ALLOW_MERGE", true);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}