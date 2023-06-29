package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "thingiverse.com" }, urls = { "https?://(www\\.)?thingiverse\\.com/(thing:\\d+|make:\\d+|[^/]+/(about|designs|collections(/[^/]+)?|makes|likes|things)|groups/[^/]+(/(things|about))?)" })
public class ThingiverseCom extends antiDDoSForDecrypt {
    public ThingiverseCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String API_BASE = "https://api.thingiverse.com";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String fpName = null;
        if (new Regex(param.getCryptedUrl(), "/([^/]+/(about|designs|collections(/[^/]+)?|makes|likes|things)|groups/[^/]+(/(things|about))?)").matches()) {
            getPage(param.getCryptedUrl());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String[] links = getAPISearchLinks(br);
            if (links != null && links.length > 0) {
                for (String link : links) {
                    ret.add(createDownloadlink(br.getURL(link).toString()));
                }
            }
        } else if (StringUtils.containsIgnoreCase(param.getCryptedUrl(), "/thing:")) {
            // a thing
            getPage(param.getCryptedUrl());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            fpName = br.getRegex("<title>\\s*([^<]+?)\\s*-\\s*Thingiverse").getMatch(0);
            final String thingID = new Regex(br.getURL(), "(?i)thing:(\\d+).*").getMatch(0);
            if (thingID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final DownloadLink link = createDownloadlink(DirectHTTP.createURLForThisPlugin(String.format("https://www.thingiverse.com//thing:%s/zip", thingID)));
            if (fpName != null) {
                fpName = Encoding.htmlOnlyDecode(fpName);
                link.setFinalFileName(fpName + ".zip");
            }
            ret.add(link);
            // Images to see what we've downloaded (in case the label doesn't make much sense in hindsight).
            final String[] imageLinks = br.getRegex("<div class=\"gallery-photo\"[^>]*data-full=\"([^\"]+)\"[^>]*>").getColumn(0);
            if (imageLinks != null && imageLinks.length > 0) {
                for (String imageLink : imageLinks) {
                    imageLink = Encoding.htmlDecode(imageLink);
                    final DownloadLink imageDL = createDownloadlink(imageLink);
                    if (fpName != null) {
                        imageDL.setFinalFileName(fpName + "_" + imageLink.hashCode() + ".jpg");
                    }
                    ret.add(imageDL);
                }
            }
        } else if (StringUtils.containsIgnoreCase(param.getCryptedUrl(), "/make:")) {
            // a make
            final String contentID = new Regex(param.getCryptedUrl(), "(\\d+)$").getMatch(0);
            if (contentID == null) {
                /* Developer mistake */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String authtoken = this.getAuthToken(this.br);
            if (authtoken == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getHeaders().put("Authorization", "Bearer " + authtoken);
            getPage(API_BASE + "/copies/" + contentID);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final String thingURL = JavaScriptEngineFactory.walkJson(entries, "thing/public_url").toString();
            if (StringUtils.isEmpty(thingURL)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* This result will go back into this crawler to find the .zip files. */
            ret.add(createDownloadlink(thingURL));
        } else {
            /* Unsupported URL --> Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
            fp.addLinks(ret);
        }
        return ret;
    }

    private String getAuthToken(final Browser br) throws Exception {
        getPage(br, "https://cdn." + this.getHost() + "/site/js/app.bundle.js");
        final String authtoken = br.getRegex("u=\"([a-f0-9]{32})\"").getMatch(0);
        if (StringUtils.isEmpty(authtoken)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return authtoken;
    }

    private String[] getAPISearchLinks(Browser br) throws Exception {
        String[] results = null;
        String sourceData = PluginJSonUtils.getJsonNested(br, "data");
        sourceData = new Regex(sourceData, "(\\{[^\\}]+\\})").getMatch(0);
        if (sourceData != null) {
            HashMap<String, String> searchValues = JSonStorage.restoreFromString(sourceData, TypeRef.HASHMAP_STRING);
            if (searchValues != null && searchValues.keySet().size() > 0) {
                searchValues.put("page", "1");
                searchValues.put("per_page", "999999999");
                Browser br2 = br.cloneBrowser();
                String postURL = br2.getURL(searchValues.get("source")).toString();
                PostRequest post = new PostRequest(postURL);
                UrlQuery postQuery = new UrlQuery();
                for (String key : searchValues.keySet()) {
                    post.addVariable(key, String.valueOf(searchValues.get(key)));
                    postQuery.add(key, String.valueOf(searchValues.get(key)));
                }
                post.getHeaders().put("Origin", "https://www.thingiverse.com");
                post.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                post.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
                br2.setRequest(post);
                postPage(br2, postURL, postQuery.toString());
                results = br2.getRegex("a href=\"([^\"]+)\" class=\"card-img-holder").getColumn(0);
            }
        }
        return results;
    }
}