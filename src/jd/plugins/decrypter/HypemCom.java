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
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hypem.com" }, urls = { "https?://(?:www\\.)?hypem\\.com/((track|item)/[a-z0-9]+|go/[a-z0-9]+/[A-Za-z0-9]+)" })
public class HypemCom extends PluginForDecrypt {
    public HypemCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String type_redirect = "http://(www\\.)?hypem\\.com/go/[a-z0-9]+/[A-Za-z0-9]+";

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl().replaceFirst("(?i)http://", "https://");
        if (parameter.matches(type_redirect)) {
            br.setFollowRedirects(false);
            br.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String finallink = br.getRedirectLocation();
            if (finallink == null || finallink.contains("hypem.com/")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            ret.add(createDownloadlink(finallink));
        } else {
            br.setFollowRedirects(true);
            br.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String js = br.getRegex("id=\"displayList\\-data\">\\s*(.*?)\\s*</script").getMatch(0);
            Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(js);
            final List<Object> ressourcelist = (List) entries.get("tracks");
            entries = (Map<String, Object>) ressourcelist.get(0);
            final String fid = new Regex(parameter, "([a-z0-9]+)$").getMatch(0);
            final String title = (String) entries.get("song");
            final String artist = (String) entries.get("artist");
            final String key = (String) entries.get("key");
            if (title == null || artist == null || key == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage("https://hypem.com/serve/source/" + fid + "/" + key + "?_=" + System.currentTimeMillis());
            entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.getRequest().getHtmlCode());
            String finallink = (String) entries.get("url");
            if (StringUtils.isEmpty(finallink)) {
                /* Some items can't be played e.g.: https://hypem.com/track/1xv55/Clean+Bandit+-+Rihanna+feat.+Noonie+Bao */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!finallink.contains("soundcloud.com/")) {
                finallink = DirectHTTP.createURLForThisPlugin(finallink);
            }
            final DownloadLink dl = createDownloadlink(finallink);
            dl.setFinalFileName(artist + " - " + title + ".mp3");
            ret.add(dl);
        }
        return ret;
    }
}
