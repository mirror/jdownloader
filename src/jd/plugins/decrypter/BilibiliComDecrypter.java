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
import java.util.Random;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.JDHash;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bilibili.com" }, urls = { "https?://(?:www\\.)?bilibili\\.com/(?:video/av\\d+(?:/\\?p=\\d+)?|bangumi/(?:play|media)/.+)" })
public class BilibiliComDecrypter extends PluginForDecrypt {
    public BilibiliComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final long   SUCCESS_OK                = 200l;
    private static final String API_URL                   = "//interface.bilibili.com/v2/playurl?";
    private static final String API_QUERY_FORMAT1         = "appkey=%s&cid=%s&otype=json";
    private static final String API_QUERY_FORMAT2         = "%s&qn=%s&quality=%s&type=";
    private static final String API_QUERY_FORMAT3         = "%s%s&sign=%s";
    private static final String BANGUMI_API_URL           = "//bangumi.bilibili.com/player/web_api/v2/playurl?";
    private static final String BANGUMI_API_QUERY_FORMAT1 = "appkey=%s&cid=%s&module=bangumi&otype=json";
    private static final String BANGUMI_API_QUERY_FORMAT2 = "%s&qn=%s&quality=%s&season_type=%s&type=";
    private static final String BANGUMI_API_QUERY_FORMAT3 = API_QUERY_FORMAT3;
    private static final String BANGUMI_API_GEO_CHECK     = "//bangumi.bilibili.com/view/web_api/season/user/status?season_id=%s&season_type=%s";
    //
    private static final String APP_KEY                   = "84956560bc028eb7";
    private static final String SEC_KEY                   = "94aba54af9065f71de72f5508f1cd42e";

    @SuppressWarnings("unchecked")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String jsonString = br.getRegex("window\\.__INITIAL_STATE__=(\\{.+?\\});").getMatch(0);
        Map<String, Object> entries = null;
        long errCode = SUCCESS_OK;
        if (jsonString != null) {
            entries = JavaScriptEngineFactory.jsonToJavaMap(jsonString);
            errCode = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "error/code"), SUCCESS_OK);
        }
        if (errCode == 404 || entries == null) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        String aid = (String) entries.get("aid");
        if (!parameter.contains("/bangumi/")) {
            String title = (String) JavaScriptEngineFactory.walkJson(entries, "videoData/title");
            FilePackage fp = FilePackage.getInstance();
            fp.setName(encodeUnicode(title.trim()));
            List<Map<String, Object>> pages = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "videoData/pages");
            String pageAssignment = new Regex(parameter, "/\\?p=(\\d+)").getMatch(0);
            if (pageAssignment != null && pages.size() > 1) {
                createDLLink(fp, decryptedLinks, pages.get(Integer.parseInt(pageAssignment) - 1), title, pages.size());
            } else {
                for (Map<String, Object> p : pages) {
                    createDLLink(fp, decryptedLinks, p, title, pages.size());
                }
            }
        } else {
            if (parameter.contains("/media/")) {
                String seasonId = String.valueOf(JavaScriptEngineFactory.walkJson(entries, "mediaInfo/param/season_id"));
                String seasonType = String.valueOf(JavaScriptEngineFactory.walkJson(entries, "mediaInfo/param/season_type"));
                if (!checkGeoBangumi(seasonId, seasonType)) {
                    logger.info("GEO-blocked");
                    decryptedLinks.add(createOfflinelink(parameter));
                } else {
                    String mt = (String) JavaScriptEngineFactory.walkJson(entries, "mediaInfo/chn_name");
                    FilePackage fp = FilePackage.getInstance();
                    fp.setName(encodeUnicode(mt.trim()));
                    List<Map<String, Object>> episodes = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "mediaInfo/episodes");
                    for (Map<String, Object> ep : episodes) {
                        String ei = (String) ep.get("index");
                        String et = (String) ep.get("index_title");
                        String title = String.format("%s EP%s %s", mt, ei, et);
                        String cid = String.valueOf(ep.get("cid"));
                        createDLLinkBangumi(fp, decryptedLinks, title, seasonType, cid);
                    }
                }
            } else {
                String seasonId = String.valueOf(JavaScriptEngineFactory.walkJson(entries, "mediaInfo/season_id"));
                String seasonType = String.valueOf(JavaScriptEngineFactory.walkJson(entries, "mediaInfo/season_type"));
                if (!checkGeoBangumi(seasonId, seasonType)) {
                    logger.info("GEO-blocked");
                    decryptedLinks.add(createOfflinelink(parameter));
                } else {
                    String mt = (String) JavaScriptEngineFactory.walkJson(entries, "mediaInfo/title");
                    String ei = (String) JavaScriptEngineFactory.walkJson(entries, "epInfo/index");
                    String et = (String) JavaScriptEngineFactory.walkJson(entries, "epInfo/index_title");
                    String title = String.format("%s EP%s %s", mt, ei, et);
                    String cid = String.valueOf(JavaScriptEngineFactory.walkJson(entries, "epInfo/cid"));
                    FilePackage fp = FilePackage.getInstance();
                    fp.setName(encodeUnicode(title.trim()));
                    createDLLinkBangumi(fp, decryptedLinks, title, seasonType, cid);
                }
            }
        }
        return decryptedLinks;
    }

    @SuppressWarnings("unchecked")
    private void createDLLink(final FilePackage fp, final ArrayList<DownloadLink> decryptedLinks, final Map<String, Object> page, final String title, final int pageSize) throws Exception {
        // default
        String cid = String.valueOf(page.get("cid"));
        String pageNo = String.valueOf(page.get("page"));
        String part = (String) page.get("part");
        final String query1 = String.format(API_QUERY_FORMAT1, APP_KEY, cid);
        String query2 = String.format(API_QUERY_FORMAT2, query1, "0", "0");
        String url = String.format(API_QUERY_FORMAT3, API_URL, query2, JDHash.getMD5(query2 + SEC_KEY));
        br.getPage(url);
        // best quality
        Map<String, Object> entries2 = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        List<Object> acceptQualityList = (List<Object>) entries2.get("accept_quality");
        String acceptQualityMax = String.valueOf(acceptQualityList.get(0));
        query2 = String.format(API_QUERY_FORMAT2, query1, acceptQualityMax, acceptQualityMax);
        url = String.format(API_QUERY_FORMAT3, API_URL, query2, JDHash.getMD5(query2 + SEC_KEY));
        br.getPage(url);
        // dllink
        Map<String, Object> entries3 = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        String ext = "flv"; // (String) entries3.get("format");
        List<Map<String, Object>> durls = (List<Map<String, Object>>) entries3.get("durl");
        for (Map<String, Object> durl : durls) {
            // There are multiple possibilities, in that case need to join with ffmpeg.
            DownloadLink dl = createDownloadlink("https://bilibilidecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(10000));
            dl.setProperty("mainLink", durl.get("url"));
            StringBuilder fileName = new StringBuilder(title);
            if (StringUtils.isNotEmpty(part) && pageSize > 1) {
                fileName.append(String.format(" P%s %s", pageNo, part));
            }
            if (durls.size() > 1) {
                fileName.append(".part");
                fileName.append(String.valueOf(durl.get("order")));
            }
            fileName.append(".");
            fileName.append(ext);
            dl.setFinalFileName(encodeUnicode(fileName.toString()));
            dl.setDownloadSize(JavaScriptEngineFactory.toLong(durl.get("size"), -1));
            fp.add(dl);
            decryptedLinks.add(dl);
        }
    }

    @SuppressWarnings("unchecked")
    private void createDLLinkBangumi(final FilePackage fp, final ArrayList<DownloadLink> decryptedLinks, final String title, final String seasonType, final String cid) throws Exception {
        // default
        final String query1 = String.format(BANGUMI_API_QUERY_FORMAT1, APP_KEY, cid);
        String query2 = String.format(BANGUMI_API_QUERY_FORMAT2, query1, "0", "0", seasonType);
        String url = String.format(BANGUMI_API_QUERY_FORMAT3, BANGUMI_API_URL, query2, JDHash.getMD5(query2 + SEC_KEY));
        br.getPage(url);
        // best quality
        Map<String, Object> entries2 = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        List<Object> acceptQualityList = (List<Object>) entries2.get("accept_quality");
        String acceptQualityMax = String.valueOf(acceptQualityList.get(0));
        query2 = String.format(BANGUMI_API_QUERY_FORMAT2, query1, acceptQualityMax, acceptQualityMax, seasonType);
        url = String.format(BANGUMI_API_QUERY_FORMAT3, BANGUMI_API_URL, query2, JDHash.getMD5(query2 + SEC_KEY));
        br.getPage(url);
        // dllink
        Map<String, Object> entries3 = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        String ext = "flv"; // (String) entries3.get("format");
        List<Map<String, Object>> durls = (List<Map<String, Object>>) entries3.get("durl");
        for (Map<String, Object> durl : durls) {
            // There are multiple possibilities, in that case need to join with ffmpeg.
            DownloadLink dl = createDownloadlink("https://bilibilidecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(10000));
            dl.setProperty("mainLink", durl.get("url"));
            StringBuilder fileName = new StringBuilder(title);
            if (durls.size() > 1) {
                fileName.append(".part");
                fileName.append(String.valueOf(durl.get("order")));
            }
            fileName.append(".");
            fileName.append(ext);
            dl.setFinalFileName(encodeUnicode(fileName.toString()));
            dl.setDownloadSize(JavaScriptEngineFactory.toLong(durl.get("size"), -1));
            fp.add(dl);
            decryptedLinks.add(dl);
        }
    }

    private boolean checkGeoBangumi(String seasonId, String seasonType) throws Exception {
        boolean result = false;
        br.getPage(String.format(BANGUMI_API_GEO_CHECK, seasonId, seasonType));
        String areaLimit = PluginJSonUtils.getJsonValue(br, "area_limit");
        if ("0".equals(areaLimit)) {
            result = true;
        }
        return result;
    }

    @Override
    public String encodeUnicode(final String input) {
        if (input != null) {
            String output = input;
            output = output.replace(":", "：");
            output = output.replace("|", "｜");
            output = output.replace("<", "＜");
            output = output.replace(">", "＞");
            output = output.replace("/", "⁄");
            output = output.replace("\\", "∖");
            output = output.replace("*", "＊");
            output = output.replace("?", "？");
            output = output.replace("!", "！");
            output = output.replace("\"", "”");
            return output;
        }
        return null;
    }
}
