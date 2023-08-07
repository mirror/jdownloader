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

import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "huaban.com" }, urls = { "https?://(?:www\\.)?huaban\\.com/boards/\\d+" })
public class HuabanComDecrypter extends PluginForDecrypt {
    public HuabanComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String      parameter                           = null;
    private FilePackage fp                                  = null;
    private boolean     enable_description_inside_filenames = jd.plugins.hoster.HuabanCom.defaultENABLE_DESCRIPTION_IN_FILENAMES;

    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        parameter = param.getCryptedUrl();
        final String boardid = new Regex(parameter, "(\\d+)").getMatch(0);
        enable_description_inside_filenames = SubConfiguration.getConfig("huaban.com").getBooleanProperty(jd.plugins.hoster.HuabanCom.ENABLE_DESCRIPTION_IN_FILENAMES, enable_description_inside_filenames);
        br.setFollowRedirects(true);
        /* Sometimes html can be very big */
        br.setLoadLimit(br.getLoadLimit() * 4);
        // final boolean loggedIN = getUserLogin(false);
        String fpName = null;
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        fpName = br.getRegex("<title>(.*?)</title>").getMatch(0);
        if (fpName == null) {
            fpName = boardid;
        }
        if (fpName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        int page = 0;
        long lnumberof_pins = 0;
        fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        String json_source = null;
        String last_pin_id = null;
        Map<String, Object> entries = null;
        do {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user: " + parameter);
                return ret;
            }
            if (page == 0) {
                json_source = br.getRegex("app\\.page\\[\"board\"\\] = (\\{.*?\\});[\t\n\r]+").getMatch(0);
                if (json_source == null) {
                    break;
                }
                entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json_source);
                lnumberof_pins = JavaScriptEngineFactory.toLong(entries.get("pin_count"), 0);
                if (lnumberof_pins == 0) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } else {
                this.br.getHeaders().put("Accept", "application/json");
                this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                this.br.getHeaders().put("X-Request", "JSON");
                this.br.getPage("/boards/" + boardid + "?max=" + last_pin_id + "&limit=20&wfl=1");
                json_source = this.br.toString();
                if (json_source == null) {
                    break;
                }
                entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json_source);
                entries = (Map<String, Object>) entries.get("board");
            }
            final List<Object> resource_data_list = (List) entries.get("pins");
            for (final Object pint : resource_data_list) {
                final Map<String, Object> single_pin_data = (Map<String, Object>) pint;
                final String pin_directlink = getDirectlinkFromJson(single_pin_data);
                final String pin_id = Long.toString(JavaScriptEngineFactory.toLong(single_pin_data.get("pin_id"), 0));
                // final String description =(String) single_pin_data.get("description");
                final String username = Long.toString(JavaScriptEngineFactory.toLong(single_pin_data.get("user_id"), 0));
                final String pinner_name = Long.toString(JavaScriptEngineFactory.toLong(single_pin_data.get("via_user_id"), 0));
                if (pin_id.equals("0") || (username.equals("0") && pinner_name.equals("0"))) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                String filename = pin_id;
                final String content_url = "http://huaban.com/pins/" + pin_id;
                final DownloadLink dl = createDownloadlink(content_url);
                // if (description != null) {
                // dl.setComment(description);
                // dl.setProperty("description", description);
                // if (enable_description_inside_filenames) {
                // filename += "_" + description;
                // }
                // }
                filename += jd.plugins.hoster.HuabanCom.default_extension;
                dl.setContentUrl(content_url);
                if (pin_directlink != null) {
                    dl.setProperty("free_directlink", pin_directlink);
                }
                dl.setProperty("boardid", boardid);
                dl.setProperty("username", username);
                dl.setProperty("decryptedfilename", filename);
                dl.setName(filename);
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                ret.add(dl);
                distribute(dl);
                last_pin_id = pin_id;
            }
            logger.info("Decrypter " + ret.size() + " of " + lnumberof_pins + " pins");
            page++;
        } while (last_pin_id != null && ret.size() < lnumberof_pins);
        return ret;
    }

    public static String getDirectlinkFromJson(final Map<String, Object> entries) {
        String directlink = null;
        final String key = (String) JavaScriptEngineFactory.walkJson(entries, "file/key");
        if (key != null) {
            directlink = "https://hbimg.huaban.com/" + key;
        }
        return directlink;
    }
}
