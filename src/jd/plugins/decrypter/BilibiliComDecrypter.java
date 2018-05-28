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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.JDHash;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bilibili.com" }, urls = { "https?://(?:www\\.)?bilibili\\.com/(?:mobile/)?video/av\\d+" })
public class BilibiliComDecrypter extends PluginForDecrypt {
    public BilibiliComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final long   SUCCESS_OK        = 200l;
    private static final String API_URL           = "//interface.bilibili.com/v2/playurl?";
    private static final String API_QUERY_FORMAT1 = "appkey=%s&cid=%s&otype=json";
    private static final String API_QUERY_FORMAT2 = "%s&qn=%s&quality=%s&type=";
    private static final String API_QUERY_FORMAT3 = "%s%s&sign=%s";
    private static final String APP_KEY           = "84956560bc028eb7";
    private static final String SEC_KEY           = "94aba54af9065f71de72f5508f1cd42e";

    @SuppressWarnings("unchecked")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        final String jsonString = br.getRegex("window\\.__INITIAL_STATE__=(\\{.+?\\});").getMatch(0);
        Map<String, Object> entries = null;
        long errCode = SUCCESS_OK;
        if (jsonString != null) {
            entries = JavaScriptEngineFactory.jsonToJavaMap(jsonString);
            errCode = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "error/code"), SUCCESS_OK);
        }
        if (errCode == 404 || entries == null) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String aid = (String) entries.get("aid");
        if (parameter.contains("/bangumi/")) {
        } else {
            String title = (String) JavaScriptEngineFactory.walkJson(entries, "videoData/title");
            List<Map<String, Object>> pages = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "videoData/pages");
            int pageCnt = 1;
            for (Map<String, Object> p : pages) {
                // default
                String cid = String.valueOf(p.get("cid"));
                Number page = (Number) p.get("page");
                String part = (String) p.get("part");
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
                String ext = (String) entries3.get("format");
                List<Map> durls = (List<Map>) entries3.get("durl");
                int partCnt = 1;
                for (Map durl : durls) {
                    // There are multiple possibilities, in that case need to join with ffmpeg.
                    DownloadLink dl = createDownloadlink("https://bilibilidecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(10000));
                    dl.setProperty("mainLink", durl.get("url"));
                    StringBuilder fileName = new StringBuilder(title);
                    if (StringUtils.isNotEmpty(part)) {
                        fileName.append(String.format(" P%d %s", pageCnt, part));
                    }
                    if (durls.size() > 2) {
                        fileName.append(".part");
                        fileName.append(String.valueOf(partCnt));
                        partCnt++;
                    }
                    fileName.append(".");
                    fileName.append(ext);
                    dl.setFinalFileName(fileName.toString());
                    dl.setDownloadSize(JavaScriptEngineFactory.toLong(durl.get("size"), -1));
                    decryptedLinks.add(dl);
                }
                pageCnt++;
            }
        }
        return decryptedLinks;
    }
}
