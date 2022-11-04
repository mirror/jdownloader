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
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rokfin.com" }, urls = { "https?://(www\\.)?rokfin.com/(post|stream)/\\d+" })
public class RokfinCom extends PluginForDecrypt {
    public RokfinCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br.getPage(param.getCryptedUrl());
        br.followRedirect();
        final String postID = new Regex(param.getCryptedUrl(), "/post/(\\d+)").getMatch(0);
        final String streamID = new Regex(param.getCryptedUrl(), "/stream/(\\d+)").getMatch(0);
        Browser brc = br.cloneBrowser();
        final Map<String, Object> map;
        if (postID != null) {
            // free posts with without authentication
            map = JSonStorage.restoreFromString(brc.getPage("https://prod-api-v2.production.rokfin.com/api/v2/public/post/" + postID), TypeRef.HASHMAP);
            final String type = (String) JavaScriptEngineFactory.walkJson(map, "content/contentType");
            if (!"video".equalsIgnoreCase(type) && !"audio".equalsIgnoreCase(type)) {
                logger.info("Unsupported type:" + type);
                return new ArrayList<DownloadLink>();
            } else {
                final String title = (String) JavaScriptEngineFactory.walkJson(map, "content/contentTitle");
                if (StringUtils.isEmpty(title)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String premiumPlan = StringUtils.valueOfOrNull(JavaScriptEngineFactory.walkJson(map, "premiumPlan"));
                if ("true".equalsIgnoreCase(premiumPlan) || "1".equalsIgnoreCase(premiumPlan)) {
                    throw new AccountRequiredException(title);
                }
                final String m3u8 = (String) JavaScriptEngineFactory.walkJson(map, "content/contentUrl");
                if (StringUtils.isEmpty(m3u8) || StringUtils.equalsIgnoreCase("fake.m3u8", m3u8)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                brc = br.cloneBrowser();
                brc.getPage(m3u8);
                final ArrayList<DownloadLink> ret = GenericM3u8Decrypter.parseM3U8(this, m3u8, brc, null, null, title);
                if (ret.size() > 1) {
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(title);
                    fp.addLinks(ret);
                }
                return ret;
            }
        } else {
            // may require authentication
            map = JSonStorage.restoreFromString(brc.getPage("https://prod-api-v2.production.rokfin.com/api/v2/public/stream/" + streamID), TypeRef.HASHMAP);
            final String title = (String) JavaScriptEngineFactory.walkJson(map, "title");
            if (StringUtils.isEmpty(title)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String premium = StringUtils.valueOfOrNull(JavaScriptEngineFactory.walkJson(map, "premium"));
            if ("true".equalsIgnoreCase(premium) || "1".equalsIgnoreCase(premium)) {
                throw new AccountRequiredException(title);
            }
            final String m3u8 = (String) JavaScriptEngineFactory.walkJson(map, "vodUrl");
            if (StringUtils.isEmpty(m3u8)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            brc = br.cloneBrowser();
            brc.getPage(m3u8);
            final ArrayList<DownloadLink> ret = GenericM3u8Decrypter.parseM3U8(this, m3u8, brc, null, null, title);
            if (ret.size() > 1) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(title);
                fp.addLinks(ret);
            }
            return ret;
        }
    }
}