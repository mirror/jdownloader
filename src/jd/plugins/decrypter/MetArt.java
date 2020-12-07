package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "metart.com", "sexart.com" }, urls = { "https?://members\\.(?:met\\-art|metart)\\.com/members/model/[A-Za-z0-9\\-\\_]+/movie/\\d+/[A-Za-z0-9\\-\\_]+/|https://(?:www\\.)?metart\\.com/model/[^/]+/gallery/\\d+/[A-Za-z0-9\\-_]+", "https?://members\\.sexart\\.com/members/model/[A-Za-z0-9\\-\\_]+/movie/\\d+/[A-Za-z0-9\\-\\_]+/|https://(?:www\\.)?sexart\\.com/model/[^/]+/gallery/\\d+/[A-Za-z0-9\\-_]+" })
public class MetArt extends PluginForDecrypt {
    public MetArt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_GALLERY = "https://[^/]+/model/([^/]+)/gallery/(\\d+)/([^/]+)";

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2020-12-07: Preventive measure */
        return 1;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String host = Browser.getHost(param.toString());
        final String host_for_url = "decrypted" + host.replaceAll("(\\.|\\-)", "") + "://";
        final ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts(this.getHost());
        Account useAcc = null;
        if (accounts != null && accounts.size() != 0) {
            Iterator<Account> it = accounts.iterator();
            while (it.hasNext()) {
                Account n = it.next();
                if (n.isEnabled() && n.isValid()) {
                    useAcc = n;
                    break;
                }
            }
        }
        if (useAcc == null) {
            throw new AccountRequiredException();
        }
        br.setFollowRedirects(true);
        final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
        plg.setBrowser(this.br);
        /* TODO: We should be able to login without verifying to speed-up this crawler! */
        ((jd.plugins.hoster.MetArtCom) plg).login(useAcc, true);
        if (param.getCryptedUrl().matches(TYPE_GALLERY)) {
            /* New 2020-12-07 */
            final Regex urlinfo = new Regex(param.getCryptedUrl(), TYPE_GALLERY);
            final String modelname = urlinfo.getMatch(0);
            final String date = urlinfo.getMatch(1);
            final String galleryname = urlinfo.getMatch(2);
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(modelname + " - " + date + " - " + galleryname);
            br.getPage("https://www." + this.getHost() + "/api/image?name=" + galleryname + "&date=" + date + "&order=5&mediaType=gallery");
            if (br.getHttpConnection().getResponseCode() == 404) {
                ret.add(this.createOfflinelink(param.getCryptedUrl()));
                return ret;
            }
            Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final List<Object> imagesO = (List<Object>) entries.get("media");
            for (final Object imageO : imagesO) {
                entries = (Map<String, Object>) imageO;
                final String url = (String) JavaScriptEngineFactory.walkJson(entries, "src_downloadable/high");
                if (StringUtils.isEmpty(url)) {
                    /* Skip invalid objects */
                    continue;
                }
                final String filenameURL = UrlQuery.parse(url).get("filename");
                final DownloadLink dl = this.createDownloadlink(url);
                dl.setAvailable(true);
                if (filenameURL != null) {
                    dl.setName(filenameURL);
                }
                dl._setFilePackage(fp);
                ret.add(dl);
            }
        } else {
            /* Old: Revision 36050 and before */
            final String links[] = br.getRegex("href=\"(https?://[^/]+/[^<>\"\\']+method=download)\"").getColumn(0);
            String title = br.getRegex("<title>(.*?)</title").getMatch(0);
            for (String link : links) {
                link = Encoding.htmlDecode(link);
                final DownloadLink dl = createDownloadlink(link.replaceAll("https?://", host_for_url));
                ret.add(dl);
            }
            if (title != null) {
                title = Encoding.htmlDecode(title);
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(title);
                fp.addLinks(ret);
            }
        }
        return ret;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}