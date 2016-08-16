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
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "huaban.com" }, urls = { "https?://(?:www\\.)?huaban\\.com/boards/\\d+" }, flags = { 0 })
public class HuabanComDecrypter extends PluginForDecrypt {

    public HuabanComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private ArrayList<DownloadLink> decryptedLinks                      = null;
    private String                  parameter                           = null;
    private FilePackage             fp                                  = null;
    private boolean                 enable_description_inside_filenames = jd.plugins.hoster.HuabanCom.defaultENABLE_DESCRIPTION_IN_FILENAMES;

    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br = new Browser();
        decryptedLinks = new ArrayList<DownloadLink>();
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
            decryptedLinks.add(getOffline(parameter));
            return decryptedLinks;
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
        LinkedHashMap<String, Object> entries = null;
        do {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user: " + parameter);
                return decryptedLinks;
            }

            if (page == 0) {
                json_source = br.getRegex("app\\.page\\[\"board\"\\] = (\\{.*?\\});[\t\n\r]+").getMatch(0);
                if (json_source == null) {
                    break;
                }
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json_source);
                lnumberof_pins = JavaScriptEngineFactory.toLong(entries.get("pin_count"), 0);
                if (lnumberof_pins == 0) {
                    decryptedLinks.add(getOffline(parameter));
                    return decryptedLinks;
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
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json_source);
                entries = (LinkedHashMap<String, Object>) entries.get("board");
            }
            final ArrayList<Object> resource_data_list = (ArrayList) entries.get("pins");
            for (final Object pint : resource_data_list) {
                final LinkedHashMap<String, Object> single_pin_data = (LinkedHashMap<String, Object>) pint;

                final String pin_directlink = jd.plugins.hoster.HuabanCom.getDirectlinkFromJson(single_pin_data);
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
                filename = encodeUnicode(filename);

                dl.setContentUrl(content_url);
                dl.setLinkID("huabancom://" + pin_id);
                if (pin_directlink != null) {
                    dl.setProperty("free_directlink", pin_directlink);
                }
                dl.setProperty("boardid", boardid);
                dl.setProperty("username", username);
                dl.setProperty("decryptedfilename", filename);
                dl.setName(filename);
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                distribute(dl);
                last_pin_id = pin_id;
            }
            logger.info("Decrypter " + decryptedLinks.size() + " of " + lnumberof_pins + " pins");
            page++;
        } while (last_pin_id != null && decryptedLinks.size() < lnumberof_pins);

        return decryptedLinks;
    }

    private DownloadLink getOffline(final String parameter) {
        final DownloadLink offline = this.createOfflinelink(parameter);
        offline.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0));
        return offline;
    }

    // /** Log in the account of the hostplugin */
    // @SuppressWarnings({ "deprecation", "static-access" })
    // private boolean getUserLogin(final boolean force) throws Exception {
    // final PluginForHost hostPlugin = JDUtilities.getPluginForHost("huaban.com");
    // final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
    // if (aa == null) {
    // logger.warning("There is no account available, stopping...");
    // return false;
    // }
    // try {
    // jd.plugins.hoster.HuabanCom.login(this.br, aa, false);
    // } catch (final PluginException e) {
    // aa.setValid(false);
    // return false;
    // }
    // return true;
    // }

}
