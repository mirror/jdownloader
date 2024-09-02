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
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "girlsreleased.com" }, urls = { "https?://(?:www\\.)?girlsreleased\\.com/.*#?(set|site|model)s?/?.*" })
public class GirlsReleasedCom extends antiDDoSForDecrypt {
    public GirlsReleasedCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private enum PageType {
        GR_SET,
        GR_SITES,
        GR_SITE,
        GR_MODEL,
        GR_MODELS,
        GR_UNKNOWN;

        private final static PageType parse(final String url) {
            if (new Regex(url, PATTERN_SET).patternFind()) {
                return GR_SET;
            } else if (new Regex(url, PATTERN_SITE).patternFind()) {
                return GR_SITE;
            } else if (new Regex(url, PATTERN_SITES).patternFind()) {
                return GR_SITES;
            } else if (new Regex(url, PATTERN_MODEL).patternFind()) {
                return GR_MODEL;
            } else if (new Regex(url, PATTERN_MODELS).patternFind()) {
                return GR_MODELS;
            } else {
                return GR_UNKNOWN;
            }
        }
    }

    private final static Pattern PATTERN_SET    = Pattern.compile("(?i)https?://[^/]+/.*#?set/(\\d+)");
    private final static Pattern PATTERN_SITE   = Pattern.compile("(?i)https?://[^/]+/.*#?sites/(\\d+)");
    private final static Pattern PATTERN_SITES  = Pattern.compile("(?i)https?://[^/]+/.*#?sites/(\\d+)");
    private final static Pattern PATTERN_MODEL  = Pattern.compile("(?i)https?://[^/]+/.*#?model/(\\d+)(/([^/]+))?");
    private final static Pattern PATTERN_MODELS = Pattern.compile("(?i)https?://[^/]+/.*#?models/(\\d+)(/([^/]+))?");

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String parameter = param.getCryptedUrl();
        br.setFollowRedirects(true);
        final PageType pageType = PageType.parse(parameter);
        if (pageType == PageType.GR_UNKNOWN) {
            getLogger().warning("Unable to determine page type!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        getPage(parameter);
        // Build API payload
        String[][] idList = null;
        String payload = null;
        idList = new Regex(parameter, "#\\w+/([^$\\/\\?]+)").getMatches();
        String setID = null;
        if (pageType == PageType.GR_SET) {
            if (idList != null && idList.length > 0) {
                final String timestamp = br.getRegex("var w = \'([^\']+)\';").getMatch(0);
                if (timestamp != null && timestamp.length() > 0) {
                    setID = idList[0][0];
                    payload = "{\"tasks\":[\"getset\"],\"set\":{\"id\":\"" + setID + "\"},\"w\":\"" + timestamp + "\"}";
                }
            }
            if (setID == null) {
                setID = new Regex(param.getCryptedUrl(), PATTERN_SET).getMatch(0);
            }
        } else if (pageType == PageType.GR_SITE) {
            if (idList != null && idList.length > 0) {
                payload = "{\"tasks\":[\"getsets\"],\"sets\":{\"count\":999999999,\"site\":\"" + idList[0][0] + "\"}}";
            }
        } else if (pageType == PageType.GR_SITES) {
            payload = "{\"tasks\":[\"getsites\"],\"sites\":{\"page\":0,\"count\":999999999,\"model\":0,\"modelname\":null,\"search\":null,\"modelid\":null}}";
        } else if (pageType == PageType.GR_MODEL) {
            if (idList != null && idList.length > 0) {
                idList = new Regex(parameter, "/#\\w+/([^$\\/]+)/([^$\\/\\s\\?]+)").getMatches();
                if (idList != null && idList.length > 0 && idList[0].length > 1) {
                    payload = "{\"tasks\":[\"getsets\"],\"sets\":{\"count\":999999999,\"model\":\"" + idList[0][0] + "\"},\"modelname\":\"" + idList[0][1] + "\"}";
                }
            }
        } else if (pageType == PageType.GR_MODELS) {
            payload = "{\"tasks\":[\"getmodels\"],\"models\":{\"page\":0,\"count\":999999999,\"site\":null,\"sort\":null,\"search\":null}}";
        }
        if (pageType == PageType.GR_SET) {
            final Request request = br.createGetRequest("https://girlsreleased.com/api/0.1/set/" + setID);
            sendRequest(br, request);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final Map<String, Object> infomap = (Map<String, Object>) entries.get("set");
            final List<List<Object>> imgs = (List<List<Object>>) infomap.get("images");
            for (final List<Object> imgInfo : imgs) {
                String link = Encoding.htmlDecode(imgInfo.get(3).toString());
                if (link.startsWith("/")) {
                    link = br.getURL(link).toExternalForm();
                }
                if (this.canHandle(link)) {
                    continue;
                }
                ret.add(createDownloadlink(link));
            }
            final List<Object> modelInfo = (List<Object>) JavaScriptEngineFactory.walkJson(infomap, "models/{0}");
            final String fpName = infomap.get("site") + " - " + modelInfo.get(1) + " - " + "Set " + setID;
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
            fp.setAllowMerge(true);
            fp.addLinks(ret);
        } else if (pageType == PageType.GR_MODEL) {
            /* Crawl all sets of a model. */
            final String modelID = new Regex(param.getCryptedUrl(), PATTERN_MODEL).getMatch(0);
            final Request request = br.createGetRequest("https://girlsreleased.com/api/0.1/sets/model/" + modelID);
            sendRequest(br, request);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
            final List<List<Object>> sets = (List<List<Object>>) entries.get("sets");
            for (final List<Object> setInfo : sets) {
                final String url = "https://girlsreleased.com/set/" + setInfo.get(0).toString();
                ret.add(createDownloadlink(url));
            }
        } else {
            /* Old handling */
            // TODO timestamp/signature
            if (payload == null) {
                getLogger().warning("Unable to build payload!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String postURL = "https://girlsreleased.com/";
            final PostRequest post = new PostRequest(postURL);
            post.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            post.setContentType("application/json");
            post.setPostDataString(payload);
            br.setRequest(post);
            postPage(br, postURL, payload);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String apiResult = br.toString();
            apiResult = apiResult.replaceAll("\\\\/", "/");
            String[] links = null;
            if (pageType == PageType.GR_SITE || pageType == PageType.GR_MODELS) {
                String[][] rawLlinks = new Regex(apiResult, "\\[(\\d+),\"([^,\\]]+)\"").getMatches();
                links = new String[rawLlinks.length];
                for (int i = 0; i < rawLlinks.length; i++) {
                    links[i] = "https://girlsreleased.com/#model/" + rawLlinks[i][0] + "/" + rawLlinks[i][1];
                }
            } else if (pageType == PageType.GR_SITES) {
                String[] rawLlinks = new Regex(apiResult, "\\[[^,\\]]+,\"([^,\\]]+)\"").getColumn(0);
                links = new String[rawLlinks.length];
                for (int i = 0; i < rawLlinks.length; i++) {
                    links[i] = "https://girlsreleased.com/#set/" + rawLlinks[i];
                }
            }
            if (links != null && links.length > 0) {
                for (String link : links) {
                    link = Encoding.htmlDecode(link);
                    if (link.startsWith("/")) {
                        link = br.getURL(link).toString();
                    }
                    ret.add(createDownloadlink(link));
                }
            }
        }
        return ret;
    }
}