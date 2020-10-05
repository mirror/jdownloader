package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "orf.at" }, urls = { "https?://[a-z0-9]+\\.orf\\.at/(?:player|programm)/\\d+/[a-zA-Z0-9]+|https?://radiothek\\.orf\\.at/[a-z0-9]+/\\d+/[a-zA-Z0-9]+" })
public class OrfAt extends PluginForDecrypt {
    public OrfAt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_OLD = "https?://([a-z0-9]+)\\.orf\\.at/(?:player|programm)/(\\d+)/([a-zA-Z0-9]+)";
    private static final String TYPE_NEW = "https?://radiothek\\.orf\\.at/([a-z0-9]+)/(\\d+)/([a-zA-Z0-9]+)";

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final String broadCastID;
        final String broadCastKey;
        final String domainID;
        if (parameter.getCryptedUrl().matches(TYPE_OLD)) {
            broadCastID = new Regex(parameter.getCryptedUrl(), TYPE_OLD).getMatch(2);
            broadCastKey = new Regex(parameter.getCryptedUrl(), TYPE_OLD).getMatch(1);
            domainID = new Regex(parameter.getCryptedUrl(), TYPE_OLD).getMatch(0);
        } else {
            broadCastID = new Regex(parameter.getCryptedUrl(), TYPE_NEW).getMatch(2);
            broadCastKey = new Regex(parameter.getCryptedUrl(), TYPE_NEW).getMatch(1);
            domainID = new Regex(parameter.getCryptedUrl(), TYPE_NEW).getMatch(0);
        }
        if (broadCastID == null || broadCastKey == null || domainID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage("https://audioapi.orf.at/" + domainID + "/api/json/current/broadcast/" + broadCastID + "/" + broadCastKey + "?_s=" + System.currentTimeMillis());
        final Map<String, Object> response = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final String broadCastDay = response.get("broadcastDay").toString();
        final String title = (String) response.get("title");
        final List<Map<String, Object>> streams = (List<Map<String, Object>>) response.get("streams");
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title + "_" + broadCastDay);
        int index = 0;
        final String userid = UUID.randomUUID().toString();
        for (Map<String, Object> stream : streams) {
            final String loopStreamId = (String) stream.get("loopStreamId");
            if (loopStreamId == null) {
                continue;
            }
            final DownloadLink link = createDownloadlink("directhttp://http://loopstream01.apa.at/?channel=" + domainID + "&shoutcast=0&player=" + domainID + "_v1&referer=" + domainID + ".orf.at&_=" + System.currentTimeMillis() + "&userid=" + userid + "&id=" + loopStreamId);
            if (streams.size() > 1) {
                link.setFinalFileName(title + "_" + broadCastDay + "_" + (++index) + ".mp3");
            } else {
                link.setFinalFileName(title + "_" + broadCastDay + ".mp3");
            }
            link.setProperty("requestType", "GET");
            link.setAvailable(true);
            link.setLinkID(domainID + ".orf.at://" + broadCastID + "/" + broadCastKey + "/" + index);
            ret.add(link);
            fp.add(link);
        }
        return ret;
    }
}
