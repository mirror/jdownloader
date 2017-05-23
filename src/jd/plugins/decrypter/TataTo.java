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

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tata.to" }, urls = { "https?://(?:www\\.)?tata\\.to/film/[a-z0-9\\-]+" })
public class TataTo extends PluginForDecrypt {
    public TataTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final Regex linfo = new Regex(parameter, "film/([a-z0-9\\-]+)\\-([a-z0-9]+)$");
        final String fpName = linfo.getMatch(0);
        final String fid = linfo.getMatch(1);
        br.getPage("https://www." + this.getHost() + "/ajax/stream/" + fid);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        br.getRequest().setHtmlCode(Encoding.Base64Decode(this.br.toString()));
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("playinfo");
        for (final Object videoo : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) videoo;
            final String finallink = (String) entries.get("link_mp4");
            final String quality = Long.toString(JavaScriptEngineFactory.toLong(entries.get("quality"), 0));
            if (StringUtils.isEmpty(finallink) || !finallink.startsWith("http") || quality.equals("0")) {
                /* Skip bad objects */
                continue;
            }
            final DownloadLink dl = this.createDownloadlink(finallink);
            dl.setFinalFileName(fpName + "_" + quality + ".mp4");
            decryptedLinks.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
