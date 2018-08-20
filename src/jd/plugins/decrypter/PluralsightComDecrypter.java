package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.PluralsightCom;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.PluralsightComConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

@DecrypterPlugin(revision = "$Revision: $", interfaceVersion = 1, names = { "pluralsight.com" }, urls = { "https?://(app|www)?\\.pluralsight\\.com(\\/library)?\\/courses\\/[^/]+" })
public class PluralsightComDecrypter extends PluginForDecrypt {
    public PluralsightComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return PluralsightComConfig.class;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String course = new Regex(parameter.getCryptedUrl(), "(\\?|&)course=(.*?)(&|$)").getMatch(0);
        if (StringUtils.isEmpty(course)) {
            course = new Regex(parameter.getCryptedUrl(), "/courses/([^/]+)").getMatch(0);
        }
        if (StringUtils.isEmpty(course)) {
            return ret;
        }
        Account account = AccountController.getInstance().getValidAccount(this);
        if (account != null) {
            try {
                PluralsightCom.login(account, br, this, false);
            } catch (PluginException e) {
                handleAccountException(account, e);
                account = null;
            }
        }
        PluralsightCom.getRequest(br, this, br.createGetRequest(parameter.getCryptedUrl()));
        if (br.getHttpConnection().getResponseCode() == 200 || !br.containsHTML("You have reached the end of the internet")) {
            PluralsightCom.getRequest(br, this, br.createGetRequest("https://app.pluralsight.com/learner/content/courses/" + course));
            if (br.containsHTML("Not Found")) {
                return ret;
            }
            if (br.getRequest().getHttpConnection().getResponseCode() != 200) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Course or Data not found");
            }
            final Map<String, Object> map = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final ArrayList<DownloadLink> clips = PluralsightCom.getClips(this, br, map);
            if (clips != null) {
                final FilePackage fp = FilePackage.getInstance();
                final String title = (String) map.get("title");
                if (!StringUtils.isEmpty(title)) {
                    fp.setName(PluralsightCom.correctFileName(title));
                } else {
                    fp.setName(course);
                }
                fp.addLinks(clips);
                ret.addAll(clips);
                if (!PluginJsonConfig.get(PluralsightComConfig.class).isFastLinkCheckEnabled()) {
                    final Browser brc = br.cloneBrowser();
                    for (final DownloadLink clip : clips) {
                        if (clip.getKnownDownloadSize() < 0) {
                            final String streamURL = PluralsightCom.getStreamURL(br, this, clip);
                            if (streamURL != null) {
                                final Request checkStream = PluralsightCom.getRequest(brc, this, brc.createHeadRequest(streamURL));
                                final URLConnectionAdapter con = checkStream.getHttpConnection();
                                try {
                                    if (con.getResponseCode() == 200 && !StringUtils.containsIgnoreCase(con.getContentType(), "text") && con.getCompleteContentLength() > 0) {
                                        clip.setVerifiedFileSize(con.getCompleteContentLength());
                                        clip.setAvailable(true);
                                        distribute(clip);
                                    } else {
                                        clip.setAvailableStatus(AvailableStatus.UNCHECKED);
                                    }
                                } finally {
                                    con.disconnect();
                                }
                            } else {
                                clip.setAvailableStatus(AvailableStatus.UNCHECKED);
                            }
                        }
                    }
                }
                // TODO: add subtitles here, for each video add additional DownloadLink that represents subtitle, eg
                // link.setProperty("type", "srt");
            }
        }
        return ret;
    }
}
