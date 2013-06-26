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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision: 20458 $", interfaceVersion = 2, names = { "rapidmoviez.com" }, urls = { "https?://(www\\.)?rapidmoviez\\.com/release/[a-z0-9\\-]+" }, flags = { 0 })
public class RpdMvzCm extends PluginForDecrypt {

    public RpdMvzCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    private Browser prepBrowser(Browser prepBr) {
        // define custom browser headers and language settings.
        JDUtilities.getPluginForHost("mediafire.com");
        prepBr.getHeaders().put("User-Agent", jd.plugins.hoster.MediafireCom.stringUserAgent());
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        prepBr.getHeaders().put("Accept-Charset", null);
        prepBr.getHeaders().put("Cache-Control", null);
        prepBr.getHeaders().put("Pragma", null);
        return prepBr;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // no https
        String parameter = param.toString().replace("https://", "http://");

        prepBrowser(br);

        br.getPage(parameter);

        if (br.containsHTML("<title>404 Page Not Found</title>")) {
            logger.info("Incorrect URL: " + parameter);
            return decryptedLinks;
        }

        String fpName = br.getRegex("<h2>(.*?)<br />").getMatch(0);

        String js = br.getRegex("<script[^\r\n]+src=\"(/j/\\d+[^\"]+\\.js)").getMatch(0);
        if (js == null) {
            logger.warning("Can not find 'js', Please report this to JDownloader Development Team : " + parameter);
            return null;
        }

        br.getHeaders().put("Accept", "*/*");
        br.getPage(js);

        String filter = br.getRegex("\\(([\\d,]+)\\)").getMatch(0);
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
                if (data == null) continue;
                for (String res : data) {
                    out.append(res);
                }
                decryptedLinks.add(createDownloadlink(out.toString()));
            }
        } else {
            logger.warning("Can not find 'results', Please report this to JDownloader Development Team : " + parameter);
            return null;
        }

        if (decryptedLinks.isEmpty()) {
            logger.warning("'decrptedLinks' isEmpty!, Please report this to JDownloader Development Team : " + parameter);
            return null;
        }

        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}