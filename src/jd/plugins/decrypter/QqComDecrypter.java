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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "qq.com", "qqmusic.qq.com" }, urls = { "http://(?:www\\.)?(fenxiang\\.qq\\.com/((share|upload)/index\\.php/share/share_c/index(_v2)?/|x/)[A-Za-z0-9\\-_~]+|urlxf\\.qq\\.com/\\?[A-Za-z0-9]+)", "http://y\\.qq\\.com/#type=(?:album|singer)\\&mid=[A-Za-z0-9]+" }, flags = { 0, 0 })
public class QqComDecrypter extends PluginForDecrypt {

    public QqComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_SHORT  = "http://(www\\.)?urlxf\\.qq\\.com/\\?[A-Za-z0-9]+";
    private static final String TYPE_NORMAL = "http://(?:www\\.)?fenxiang\\.qq\\.com/(?:(?:share|upload)/index\\.php/share/share_c/index(?:_v2)?/|x/)[A-Za-z0-9\\-_~]+";
    private static final String TYPE_MUSIC  = "http://y\\.qq\\.com/#type=(album|singer)\\&mid=([A-Za-z0-9]+)";

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String fpName = null;
        br.setFollowRedirects(false);
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:27.0) Gecko/20100101 Firefox/27.0");
        br.getPage(parameter);

        if (parameter.matches(TYPE_SHORT)) {
            final String redirect = br.getRegex("window.location=\"(http[^\"]+)").getMatch(0);
            if (redirect == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            if (redirect.matches("http://(www\\.)?fenxiang\\.qq\\.com/share/index\\.php/share/share_c/index/[A-Za-z0_9]+")) {
                decryptedLinks.add(createDownloadlink(redirect));
                return decryptedLinks;
            }
            parameter = redirect;
            br.getPage(parameter);
        } else if (parameter.matches(TYPE_NORMAL)) {
            if (br.containsHTML(">很抱歉，此资源已被删除或包含敏感信息不能查看啦<")) {
                final DownloadLink offline = this.createOfflinelink(parameter);
                offline.setFinalFileName(new Regex(parameter, "([A-Za-z0-9\\-_~]+)$").getMatch(0));
                decryptedLinks.add(offline);
                return decryptedLinks;
            }

            fpName = br.getRegex("<h1>([^<>\"]*?)</h1>").getMatch(0);
            final String[] tableEntries = br.getRegex("<td class=\"td_c\">(.*?)</td>").getColumn(0);
            if (tableEntries == null || tableEntries.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String tableEntry : tableEntries) {
                final String qhref = new Regex(tableEntry, "qhref=\"(qqdl://[^<>\"]*?)\"").getMatch(0);
                final String filehash = new Regex(tableEntry, "filehash=\"([^<>\"]*?)\"").getMatch(0);
                final String filesize = new Regex(tableEntry, "filesize=\"([^<>\"]*?)\"").getMatch(0);
                final String title = new Regex(tableEntry, "title=\"([^<>\"]*?)\"").getMatch(0);
                final DownloadLink dl = createDownloadlink("http://qqdecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(1000000000));
                dl.setFinalFileName(Encoding.htmlDecode(title.trim()));
                dl.setDownloadSize(Long.parseLong(filesize));
                dl.setContentUrl(parameter);
                dl.setProperty("qhref", qhref);
                dl.setProperty("filehash", filehash);
                dl.setProperty("mainlink", parameter);
                dl.setProperty("plainfilename", title);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        } else {
            /* TYPE_MUSIC */
            String json = null;
            final String type = new Regex(parameter, TYPE_MUSIC).getMatch(0);
            final String albumid = new Regex(parameter, TYPE_MUSIC).getMatch(1);
            if (type.equals("singer")) {
                this.br.getPage("http://i.y.qq.com/v8/fcg-bin/fcg_v8_singer_detail_cp.fcg?tpl=20&singermid=" + albumid);
                json = this.br.getRegex("songlist[\t\n\r ]*?:[\t\n\r ]*?(\\[.*?\\}\\]),[\t\n\r ]+").getMatch(0);
            } else {
                this.br.getPage("http://i.y.qq.com/v8/fcg-bin/fcg_v8_album_detail_cp.fcg?tpl=20&albummid=" + albumid + "&play=0");
                // this.br.getPage("http://base.music.qq.com/fcgi-bin/fcg_musicexpress.fcg?json=3&guid=1755471437&g_tk=938407465&loginUin=0&hostUin=0&format=jsonp&inCharset=GB2312&outCharset=GB2312&notice=0&platform=yqq&jsonpCallback=jsonCallback&needNewCode=0");
                json = this.br.getRegex("mapSongInfo[\t\n\r ]*?:[\t\n\r ]*?\\{\"Disc\\d+_\":(\\[.*?\\}\\])\\}").getMatch(0);
            }
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            if (json == null) {
                return null;
            }
            LinkedHashMap<String, Object> entries = null;
            final ArrayList<Object> ressourcelist = (ArrayList) JavaScriptEngineFactory.jsonToJavaObject(json);
            for (final Object datao : ressourcelist) {
                entries = (LinkedHashMap<String, Object>) datao;
                final String albumname = encodeUnicode((String) entries.get("albumname"));
                final String songname = encodeUnicode((String) entries.get("songname"));
                final String songid = Long.toString(JavaScriptEngineFactory.toLong(entries.get("songid"), -1));
                final long filesize = JavaScriptEngineFactory.toLong(entries.get("size320"), -1);
                if (albumname == null || songname == null || "-1".equals(songid)) {
                    return null;
                }
                final DownloadLink dl = createDownloadlink("http://qqdecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(1000000000));
                dl.setProperty(jd.plugins.hoster.QqCom.PROPERTY_TYPE, jd.plugins.hoster.QqCom.TYPE_MUSIC);
                dl.setProperty(jd.plugins.hoster.QqCom.PROPERTY_MUSIC_SONGID, songid);
                dl.setProperty(jd.plugins.hoster.QqCom.PROPERTY_MUSIC_ALBUMID, albumid);
                dl.setLinkID(songid);
                dl.setFinalFileName(albumname + " - " + songname + ".mp3");
                dl.setDownloadSize(filesize);
                dl.setContentUrl(parameter);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                /* This might not be the best way to take the first existing albumname of */
                if (fpName == null) {
                    fpName = albumname;
                }
            }
            if (fpName == null) {
                fpName = albumid;
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}