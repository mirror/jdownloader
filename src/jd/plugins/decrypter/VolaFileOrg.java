package jd.plugins.decrypter;

import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.websocket.ReadWebSocketFrame;
import org.appwork.utils.net.websocket.WebSocketFrameHeader;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.websocket.WebSocketClient;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "volafile.org" }, urls = { "https?://(?:www\\.)?volafile\\.(?:org|io)/r/[A-Za-z0-9\\-_]+" })
public class VolaFileOrg extends PluginForDecrypt {
    public VolaFileOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(parameter.getCryptedUrl().replace(".io/", ".org/"));
        if (br.getHttpConnection().getResponseCode() == 404) {
            ret.add(this.createOfflinelink(parameter.getCryptedUrl()));
            return ret;
        }
        br.followRedirect();
        br.setCookie(getHost(), "allow-download", "1");
        final String checksum = PluginJSonUtils.getJson(br, "checksum2");
        String room = br.getRegex("\"room_id\"\\s*:\\s*\"(.*?)\"").getMatch(0);
        if (room == null) {
            room = new Regex(parameter.getCryptedUrl(), "/r/([A-Za-z0-9\\-_]+)").getMatch(0);
        }
        if (StringUtils.isEmpty(checksum)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final UrlQuery query = new UrlQuery();
        query.add("room", URLEncoder.encode(room, "UTF-8"));
        query.add("cs", URLEncoder.encode(checksum, "UTF-8"));
        query.add("nick", "Alke");
        query.add("EIO", "3");
        query.add("transport", "websocket");
        WebSocketClient wsc = null;
        String passCode = null;
        do {
            try {
                wsc = new WebSocketClient(br, new URL("https://volafile.org/api/?" + query.toString()));
                wsc.connect();
                ReadWebSocketFrame frame = wsc.readNextFrame();// sid
                frame = wsc.readNextFrame();// session
                frame = wsc.readNextFrame();// subscription
                if (WebSocketFrameHeader.OP_CODE.UTF8_TEXT.equals(frame.getOpCode()) && frame.isFin()) {
                    String string = new String(frame.getPayload(), "UTF-8");
                    string = string.replaceFirst("^\\d+", "");
                    if (string.startsWith("[-1") && !string.contains("\"files\"")) {
                        /* Password protected content */
                        if (passCode != null) {
                            /* Wrong password - Don't allow 2nd try. */
                            throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                        }
                        passCode = getUserInput("Password?", parameter);
                        query.add("password", passCode);
                        continue;
                    }
                    final List<Object> list = JSonStorage.restoreFromString(string, TypeRef.LIST);
                    List<Object> files = (List<Object>) ((Map<String, Object>) ((List<Object>) ((List<Object>) ((List<Object>) list.get(6)).get(0)).get(1)).get(1)).get("files");
                    if (files == null) {
                        files = (List<Object>) ((Map<String, Object>) ((List<Object>) ((List<Object>) ((List<Object>) list.get(7)).get(0)).get(1)).get(1)).get("files");
                    }
                    if (files == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else {
                        for (final Object file : files) {
                            final List<Object> fileInfo = (List<Object>) file;
                            final String fileName = String.valueOf(fileInfo.get(1));
                            final Number fileSize = ((Number) fileInfo.get(3));
                            final DownloadLink link = createDownloadlink("https://volafile.org/download/" + fileInfo.get(0) + "/" + URLEncoder.encode(fileName, "UTF-8"));
                            link.setFinalFileName(fileName);
                            link.setVerifiedFileSize(fileSize.longValue());
                            link.setAvailable(true);
                            if (passCode != null) {
                                link.setDownloadPassword(passCode);
                            }
                            ret.add(link);
                        }
                        break;
                    }
                } else {
                    logger.severe("Unsupported:" + frame);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } finally {
                wsc.close();
            }
        } while (!StringUtils.isEmpty(passCode));
        return ret;
    }
}
