package jd.plugins.decrypter;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.parser.UrlQuery;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "magentacloud.de" }, urls = { "https?://(www\\.)?magentacloud\\.de/(share|lnk)/[a-z0-9\\-]+" }) public class MagentaCloudDe extends PluginForDecrypt {

    public MagentaCloudDe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String param = parameter.getCryptedUrl();
        final String paramID = new Regex(param, "(?:share|lnk)/([a-z0-9\\-]+)").getMatch(0);
        br.setAllowedResponseCodes(new int[] { 401, 403 });
        if (param.contains("/lnk/")) {
            br.getPage("https://www.magentacloud.de/api/sharelink/info?id=" + paramID);
            Map<String, Object> info = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP, null);
            if (paramID.equals(info.get("id")) && !info.containsKey("code")) {
                if (Boolean.TRUE.equals(info.get("has_password"))) {
                    final String password = getUserInput("Password?", parameter);
                    if (password == null || "".equals(password)) {
                        throw new DecrypterException(DecrypterException.PASSWORD);
                    }
                    br.postPage("https://www.magentacloud.de/api/sharelink/info", UrlQuery.parse("id=" + Encoding.urlEncode(paramID) + "&fields=name%2Ctype%2Csize%2Cdownload_code&password=" + Encoding.urlEncode(password)));
                } else {
                    br.postPage("https://www.magentacloud.de/api/sharelink/info", UrlQuery.parse("id=" + Encoding.urlEncode(paramID) + "&fields=name%2Ctype%2Csize"));
                }
                if (br.getRequest().getHttpConnection().getResponseCode() == 401 || br.getRequest().getHttpConnection().getResponseCode() == 403) {
                    throw new DecrypterException(DecrypterException.PASSWORD);
                }
                info = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP, null);
                final String type = (String) info.get("type");
                if ("file".equals(type)) {
                    final String name = (String) info.get("name");
                    final String download_code = (String) info.get("download_code");
                    final Number size = (Number) info.get("size");
                    final DownloadLink link;
                    if (download_code != null) {
                        link = createDownloadlink("directhttp://https://www.magentacloud.de/api/sharelink/download?id=" + paramID + "&download_code=" + download_code);
                    } else {
                        link = createDownloadlink("directhttp://https://www.magentacloud.de/api/sharelink/download?id=" + paramID);
                    }
                    if (name != null) {
                        link.setFinalFileName(URLDecoder.decode(name, "UTF-8"));
                    }
                    if (size != null) {
                        link.setVerifiedFileSize(size.longValue());
                    }
                    link.setAvailable(true);
                    link.setLinkID("magentacloud.de://" + paramID);
                    link.setContentUrl(param);
                    ret.add(link);
                }
            }
        } else {
            br.getPage("https://www.magentacloud.de/api/share/info?id=" + paramID);
            Map<String, Object> info = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP, null);
            if (!info.containsKey("code")) {
                if (Boolean.TRUE.equals(info.get("has_password"))) {
                    final String password = getUserInput("Password?", parameter);
                    if (password == null || "".equals(password)) {
                        throw new DecrypterException(DecrypterException.PASSWORD);
                    }
                    br.postPage("https://www.magentacloud.de/api/share/token", UrlQuery.parse("id=" + Encoding.urlEncode(paramID) + "&password=" + Encoding.urlEncode(password)));
                } else {
                    br.postPage("https://www.magentacloud.de/api/share/token", UrlQuery.parse("id=" + Encoding.urlEncode(paramID)));
                }
                if (br.getRequest().getHttpConnection().getResponseCode() == 401 || br.getRequest().getHttpConnection().getResponseCode() == 403) {
                    throw new DecrypterException(DecrypterException.PASSWORD);
                }
                info = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP, null);
                final String root_name = (String) info.get("root_name");
                final String token_type = (String) info.get("token_type");
                final String access_token = (String) info.get("access_token");
                if ("Bearer".equals(token_type) && access_token != null) {
                    br.setHeader("Authorization", token_type + " " + Encoding.Base64Encode(access_token));
                    br.getPage("https://www.magentacloud.de/api/dir?path=/&fields=id%2Cpath%2Cwritable%2Cmembers.id%2Cmembers.parent_id%2Cmembers.name%2Cmembers.mtime%2Cmembers.path%2Cmembers.readable%2Cmembers.writable%2Cmembers.type%2Cmembers.image.width%2Cmembers.image.height%2Cmembers.size&members=all&limit=0%2C5000");
                    info = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP, null);
                    for (final Map<String, Object> member : (List<Map<String, Object>>) info.get("members")) {
                        final String type = (String) member.get("type");
                        if ("file".equals(type)) {
                            final String name = (String) member.get("name");
                            final String id = (String) member.get("id");
                            final Number size = (Number) member.get("size");
                            final DownloadLink link = createDownloadlink("directhttp://https://www.magentacloud.de/api/file?attachment=true&pid=" + id + "&access_token=" + access_token);
                            if (name != null) {
                                link.setFinalFileName(URLDecoder.decode(name, "UTF-8"));
                            }
                            if (size != null) {
                                link.setVerifiedFileSize(size.longValue());
                            }
                            link.setAvailable(true);
                            link.setLinkID("magentacloud.de://" + id);
                            link.setContainerUrl(param);
                            ret.add(link);
                        }
                    }
                }
                if (ret.size() > 1 && root_name != null) {
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(URLDecoder.decode(root_name, "UTF-8"));
                    fp.addLinks(ret);
                }
            }
        }
        return ret;
    }

}
