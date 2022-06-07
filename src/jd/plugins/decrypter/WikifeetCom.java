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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.WikifeetComConfig;
import org.jdownloader.plugins.components.config.WikifeetComConfig.AlbumPackagenameScheme;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "wikifeet.com" }, urls = { "https?://(?!pics\\.)(?:\\w+\\.)?wikifeetx?\\.com/(?!celebs|feetoftheyear)[^/]+" })
public class WikifeetCom extends PluginForDecrypt {
    public WikifeetCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // public static final String type_pic = "https?://(?:\\w+\\.)?pics\\.wikifeetx?\\.com";
    public static final String type_wikifeet = "https?://(?:\\w+\\.)?wikifeetx?\\.com/[^/]+";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        this.br.getPage(param.getCryptedUrl());
        if (isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // final String contentID = br.getRegex("messanger\\.cid = (\\d+);").getMatch(0);
        String modelName = this.br.getRegex("messanger\\.cfname = '(.*?)';").getMatch(0);
        if (modelName == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        modelName = Encoding.htmlDecode(modelName).trim();
        if (param.getCryptedUrl().matches(type_wikifeet)) {
            final String gData = this.br.getRegex("messanger\\['gdata'\\] = ([\\s\\S]*?);").getMatch(0);
            final List<Object> data = (List<Object>) JavaScriptEngineFactory.jsonToJavaObject(gData);
            if (data.size() == 0) {
                return decryptedLinks;
            }
            for (final Object entry : data) {
                final Map<String, Object> entryMap = (Map<String, Object>) entry;
                if (entryMap.containsKey("pid")) {
                    final String pid = String.valueOf(entryMap.get("pid"));
                    final String dlurl = "directhttp://https://pics.wikifeet.com/" + modelName + "-Feet-" + pid + ".jpg";
                    final DownloadLink dl = this.createDownloadlink(dlurl);
                    final String finalFilename = modelName + "_" + pid + ".jpg";
                    dl.setFinalFileName(finalFilename);
                    dl.setProperty(DirectHTTP.FIXNAME, finalFilename);
                    dl.setAvailable(true);
                    decryptedLinks.add(dl);
                }
            }
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String shoesize = br.getRegex("(?i)Shoe Size\\s*:\\s*<span[^>]*>([^<]+)<a").getMatch(0);
        if (shoesize != null) {
            shoesize = Encoding.htmlDecode(shoesize).trim();
        }
        String birthplace = br.getRegex("(?i)Birthplace\\s*:\\s*<span[^>]*>([^<]+)<a").getMatch(0);
        if (birthplace != null) {
            birthplace = Encoding.htmlDecode(birthplace).trim();
        }
        String birthdate = br.getRegex("(?i)Birth Date\\s*:\\s*<span[^>]*>([^<]+)<a").getMatch(0);
        if (birthdate != null) {
            birthdate = Encoding.htmlDecode(birthdate).trim();
        }
        final String imdbURL = br.getRegex("(http?://(?:www\\.)?imdb\\.com/name/nm\\d+)").getMatch(0);
        /* Generate packagename */
        final WikifeetComConfig cfg = PluginJsonConfig.get(getConfigInterface());
        final String customPackagenameScheme = cfg.getCustomPackagenameScheme();
        String packagename;
        if (!StringUtils.isEmpty(customPackagenameScheme) && cfg.getAlbumPackagenameScheme() == AlbumPackagenameScheme.CUSTOM) {
            packagename = customPackagenameScheme;
        } else {
            packagename = "*user*";
        }
        packagename = packagename.replace("*user*", modelName);
        packagename = packagename.replace("*birth_place*", birthplace != null ? birthplace : "");
        packagename = packagename.replace("*birth_date*", birthdate != null ? birthdate : "");
        packagename = packagename.replace("*shoe_size*", shoesize != null ? shoesize : "");
        packagename = packagename.replace("*imdb_url*", imdbURL != null ? imdbURL : "");
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(packagename);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    public static boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else if (!br.containsHTML("id=thepics")) {
            return false;
        } else {
            return false;
        }
    }

    @Override
    public Class<WikifeetComConfig> getConfigInterface() {
        return WikifeetComConfig.class;
    }
}
