package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.requests.PostRequest;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fembed.com" }, urls = { "https?://(?:www\\.)?(?:fembed\\.com|there\\.to|gcloud\\.live|plycdn\\.xyz|hlsmp4\\.com|svpri\\.xyz|asianclub\\.nl|asianclub\\.tv|javcl\\.me|feurl\\.com|zidiplay\\.com|embed\\.media|javideo\\.pw|playvideo\\.best|ffem\\.club|dunbed\\.xyz|embed\\.casa|sexhd\\.co|fileone\\.tv|luxubu\\.review|anime789\\.com)/(?:f|v)/([a-zA-Z0-9_-]+)(#javclName=[a-fA-F0-9]+)?" })
public class FEmbedDecrypter extends PluginForDecrypt {
    public FEmbedDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String file_id = new Regex(parameter.getCryptedUrl(), "/(?:f|v|api/sources?)/([a-zA-Z0-9_-]+)").getMatch(0);
        String name = new Regex(parameter.getCryptedUrl(), "#javclName=([a-fA-F0-9]+)").getMatch(0);
        if (name != null) {
            name = new String(HexFormatter.hexToByteArray(name), "UTF-8");
        }
        String fembedHost = null;
        Form dlform = null;
        if (name == null) {
            br.getPage(parameter.getCryptedUrl());
            dlform = br.getForm(1);
            fembedHost = br.getHost();
            name = br.getRegex("<title>\\s*([^<]*?)\\s*(-\\s*Free\\s*download)?\\s*</title>").getMatch(0);
        }
        if (fembedHost == null) {
            fembedHost = Browser.getHost(parameter.getCryptedUrl());
        }
        final PostRequest postRequest = new PostRequest("https://" + fembedHost + "/api/source/" + file_id);
        br.getPage(postRequest);
        Map<String, Object> response = null;
        try {
            response = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        } catch (final Throwable e) {
        }
        if (response == null && dlform != null) {
            /* 2020-03-16: Fallback for sites which do not have an API e.g. for fileone.tv */
            br.setFollowRedirects(false);
            br.submitForm(dlform);
            final String dllink = br.getRedirectLocation();
            if (dllink == null) {
                return null;
            }
            ret.add(this.createDownloadlink("directhttp://" + dllink));
            return ret;
        }
        if (!Boolean.TRUE.equals(response.get("success"))) {
            final DownloadLink link = createDownloadlink(parameter.getCryptedUrl().replaceAll("https?://", "decryptedforFEmbedHosterPlugin://"));
            link.setAvailable(false);
            ret.add(link);
            return ret;
        }
        final List<Map<String, Object>> videos;
        if (response.get("data") instanceof String) {
            videos = (List<Map<String, Object>>) JSonStorage.restoreFromString((String) response.get("data"), TypeRef.OBJECT);
        } else {
            videos = (List<Map<String, Object>>) response.get("data");
        }
        for (final Map<String, Object> video : videos) {
            final DownloadLink link = createDownloadlink(parameter.getCryptedUrl().replaceAll("https?://", "decryptedforFEmbedHosterPlugin://"));
            final String label = (String) video.get("label");
            final String type = (String) video.get("type");
            link.setProperty("label", label);
            link.setProperty("fembedid", file_id);
            link.setProperty("fembedHost", fembedHost);
            if (!StringUtils.isEmpty(name)) {
                link.setFinalFileName(name + "-" + label + "." + type);
            } else {
                link.setName(file_id + "-" + label + "." + type);
            }
            link.setAvailable(true);
            ret.add(link);
        }
        if (videos.size() > 1) {
            final FilePackage filePackage = FilePackage.getInstance();
            final String title;
            if (!StringUtils.isEmpty(name)) {
                title = name;
            } else {
                title = file_id;
            }
            filePackage.setName(title);
            filePackage.addLinks(ret);
        }
        return ret;
    }
}
