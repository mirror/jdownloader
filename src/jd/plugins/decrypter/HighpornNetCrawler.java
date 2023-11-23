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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "highporn.net", "tanix.net", "japanhub.net", "thatav.net" }, urls = { "https?://(?:www\\.)?highporn\\.net/video/(\\d+)(?:/[a-z0-9\\-]+)?", "https?://(?:www\\.)?tanix\\.net/video/(\\d+)(?:/[a-z0-9\\-]+)?", "https?://(?:www\\.)?japanhub\\.net/video/(\\d+)(?:/[a-z0-9\\-]+)?", "https?://(?:www\\.)?thatav\\.net/video/(\\d+)(?:/[a-z0-9\\-]+)?" })
public class HighpornNetCrawler extends PluginForDecrypt {
    public HighpornNetCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl().replaceFirst("(?i)www\\.", "");
        final String videoid = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        final String initialHost = Browser.getHost(parameter);
        br.setFollowRedirects(true);
        getPage(parameter);
        if (!br.getURL().contains(initialHost)) {
            logger.info("Redirect to external website");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getURL().contains(videoid)) {
            /* Offline --> Redirect to (external) ads page / search-page */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String fpName = getTitle(this.br, parameter);
        /* 2021-02-10: thatav.net */
        final String specialVideoURL = br.getRegex("\"file\":\\s*\"(https?://[^<>\"]+\\.mp4[^<>\"]+)").getMatch(0);
        if (specialVideoURL != null) {
            final DownloadLink dl = this.createDownloadlink("directhttp://" + specialVideoURL);
            dl.setAvailable(true);
            if (fpName != null) {
                dl.setFinalFileName(fpName + ".mp4");
            }
            ret.add(dl);
            return ret;
        }
        boolean singleVideo = false;
        final String videoLink = br.getRegex("data-src\\s*=\\s*\"(https?[^<>\"]+)\"").getMatch(0); // If single link, no videoID
        String[] videoIDs = br.getRegex("data-src\\s*=\\s*\"([^\"]+)\"").getColumn(0);
        if (videoIDs == null || videoIDs.length == 0) {
            if (videoLink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                videoIDs = new String[1];
                videoIDs[0] = (Long.toString(System.currentTimeMillis())); // dummy videoID
                singleVideo = true;
            }
        }
        final int padLength = StringUtils.getPadLength(videoIDs.length);
        int counter = 0;
        for (final String videoID : videoIDs) {
            if (isAbort()) {
                continue;
            }
            counter++;
            final String orderid_formatted = String.format(Locale.US, "%0" + padLength + "d", counter);
            final String filename = fpName + "_" + orderid_formatted + ".mp4";
            final DownloadLink dl = createDownloadlink("highporndecrypted://" + videoID);
            dl.setName(filename);
            dl.setProperty("decryptername", filename);
            dl.setProperty("mainlink", parameter);
            dl.setContentUrl(parameter);
            if (singleVideo) {
                dl.setProperty("singlevideo", true);
            } else {
                final PostRequest postRequest = new PostRequest("https://play.openhub.tv/playurl?random=" + (new Date().getTime() / 1000));
                postRequest.setContentType("application/x-www-form-urlencoded");
                postRequest.addVariable("v", videoID);
                postRequest.addVariable("source_play", "highporn");
                final Browser brc = br.cloneBrowser();
                final String file = brc.getPage(postRequest);
                URLConnectionAdapter con = null;
                try {
                    con = br.cloneBrowser().openHeadConnection(file);
                    // referer check
                    if (this.looksLikeDownloadableContent(con)) {
                        dl.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                } catch (final IOException e) {
                    logger.log(e);
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable ignore) {
                    }
                }
                dl.setAvailable(true);
            }
            ret.add(dl);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(ret);
        }
        return ret;
    }

    private void getPage(final String parameter) throws Exception {
        final PluginForHost plugin = JDUtilities.getPluginForHost("highporn.net");
        ((jd.plugins.hoster.HighpornNet) plugin).setBrowser(br);
        ((jd.plugins.hoster.HighpornNet) plugin).getPage(parameter);
    }

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404 || br.getURL().contains("/error/video_missing");
    }

    public static String getTitle(final Browser br, final String url) {
        String title = br.getRegex("property\\s*=\\s*\"og:title\" content=\"([^<>\"]+)\"").getMatch(0);
        if (title == null) {
            title = new Regex(url, "video/(.+)").getMatch(0);
        }
        return title;
    }

    public static void main(String[] args) throws InterruptedException, MalformedURLException, ScriptException {
        /* Simple method for decoding msg values obtained from real browser requests. */
        final StringBuilder sb = new StringBuilder();
        sb.append(
                "function _0x3bbf() {     var _0xad5001 = ['HTXyy', 'r_script_f', 'charAt', 'HnnWl', 'epUkB', 'ODkNs', 'HJKLNMPQRS', 'stateObjec', '__nightmar', 'uFXvr', 'Sequentum', 'sZsMg', 'aMAhK', 'RAXbe', 'setup', 'outerHeigh', 'hidden', 'IFIjP', 'yqpGp', 'aEpaf', 'JjWRM', '__webdrive', 'VRtul', 'DYuQh', 'RPLuu', 'aqyby', '305UmmDux', 'pjUOp', 'FUeub', 'eqMXg', 'wunnS', 'rn\\\\x20this\\\\x22)(', 'data-src', 'l?random=', 'removeClas', 'aBnPU', 'LpYgA', 'gger', 'MNUmC', 'mHyLy', 'ajax', 'QUrNW', 'XcGiF', 'FJJzZ', 'click', '2631284xNFRVw', 'zORJj', 'tcbBp', 'ode\\\\x20receiv', 'crKgh', 'ctor(\\\\x22retu', 'table\\\\x20inpu', 'DqDEz', 'vGDOh', '.playlist_', '__driver_e', 'highporn', 'POST', 'constructo', '4297965OrgDCf', 'length', 'LqxfT', 'djGCn', 'UWhHL', 'split', '550HElZhf', 'ACryd', '620MIDKMs', 'Ucmmm', 'zOYUa', 'Rkmzi', 'attr', 'UNyFV', 'languages', 'Vwrrd', 'qvBor', 'yXlDD', 'frameEleme', 'rqAlZ', 'QRzSU', 'BehIZ', '35361aWsESU', 'DgYmp', 'log', 'QXGAb', '__karma__', 'find', 'uOjJZ', 'prototype', 'ZcTJe', 'MTndT', 'Xuokx', 'uvfXT', 'contains', 'counter', 'nPMLV', '#playlist', 'cjjfn', '__proto__', 'lbPjz', 'plugins', 'Trident/', 'ljGtX', 'qAvlC', 'info', 'LaTIP', 'player_con', 'webdriver', 'documentEl', 'userAgent', 'n\\\\x20the\\\\x20Base', 'html5', 'pvEoP', 'tGPVt', '.fel-playc', 'callSeleni', 'mvWWW', 'r_evaluate', '99770kJDkRO', 'ECJuK', 'search', 'toString', '789ABCDEFG', 'jvKjv', '{}.constru', 'ASftc', '_unwrapped', 'qmJcu', 'qOxpX', 'YZabcdefgh', 'PChAd', 'edxjI', 'nHPMv', 'MPQRSTUVWX', 'Base58.dec', '__selenium', 'unction', 'yMmAN', 'FuHHs', 'call', 'NlPtp', 'aGMpG', 'cache_', '\\\\x27\\\\x20is\\\\x20not\\\\x20i', 'sFlMA', 'exception', 'VWetC', 'NhepH', 'vGiJz', 'push', 'a-zA-Z_$][', 'jGjUB', 'ReDji', 'FLUaV', 'getTime', 'addClass', 'OgBkp', 'YiJiU', 'Yjenp', 'JCbep', 'er\\\\x20\\\\x27', 'Jwjxu', 'nhdoi', '$]*)', 'AEXns', 'RHzFW', '_Selenium_', 'BQBaM', 'ay.openhub', 'map', 'lXNec', 'gxfbh', 'cyZOU', 'oMvLT', 'tMwZd', 'nuHep', 'WDYZM', '0-9a-zA-Z_', 'lsxpX', '58\\\\x20xx88aa.', 'function\\\\x20*', 'HYJUV', 'qJIVD', 'ffxQC', 'DnAwi', 'lose', '4|0|5|3|2|', 'TZWCh', 'waLxU', '_selenium', '__driver_u', 'external', 'reverse', 'uljFO', '__fxdriver', 'IULbh', 'XxIbu', 'string', 'vSYMw', 'VZuZV', '101862AioVqs', 'uugqQ', 'https://pl', 'jVAHt', 'AbmDO', 'yJmrW', '.tv/playur', 'eCyTU', 'join', 'YnPOk', 'ZBSpZ', '.mjs-close', 'hwAGu', 'jhuOt', '1820VmJHhg', 'kmXQS', 'NoQKi', 'pVxxv', 'charCodeAt', 'test', 'IZeNh', 'ement', 'indexOf', 'YOcVr', 'trace', 'PSWba', 'NnFeZ', 'rNTQu', 'apply', 'RUIDx', 'HmfGf', 'yPTvX', 'xwTFS', '23456789AB', '2128155ocIJBi', 'nCTsb', 'GgRPl', '_evaluate', 'VIyNq', 'fziBm', 'DZTuU', 'SJhJG', 'sngrE', 'tuvwxyz1', 'hsvvT', 'VHSeT', 'yGDgx', 'PeLsL', 'fromCharCo', 'match', 'tainer', 'mNoZY', 'OVySg', 'selenium', 'IKVgl', 'document', 'phEtg', 'pltge', 'lfwdH', 'rUANi', 'getAttribu', 'init', 'UiLFc', 'toLowerCas', 'console', 'IRvkj', 'arQmM', 'nction()\\\\x20', 'setInterva', 'uaSBl', 'cUAUQ', 'iHdCe', 'cdRzL', 'OaAlC', 'bWBha', 'play__butt', 'SxgEy', 'input', 'IaIJt', 'e)\\\\x20{}', 'PNUKw', 'scene', 'gtEde', 'uneHu', 'RQyij', 'icFHX', 'BRZrc', 'XIAgt', 'return\\\\x20(fu', 'JtkcZ', 'unc', 'cRwSK', '8LodgeK', 'cPjfo', 'pMuYp', 'zWVFG', 'bind', 'BbGil', 'debu', 'osRrv', 'uniform', 'cxsVt', 'VLNxj', 'nfdwg', 'while\\\\x20(tru', 'bioSP', 'pause', 'error', 't.\\\\x20Charact', 'zAKGh', 'ed\\\\x20unaccep', 'KuvKL', 'pxlDn', 'ZIymQ', 'kEJuW', 'Llram', '1|4|3|5|6', 'kZbad', 'callPhanto', 'bSmfp', 'xNWZs', 'tvSUy', 'done', 'rwrYT'];    _0x3bbf = function() {      return _0xad5001;   };  return _0x3bbf(); }");
        sb.append("var result = _0x3bbf();");
        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(null);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        String result = null;
        engine.eval(sb.toString());
        final Object resultO = engine.get("result");
        result = engine.get("result").toString();
        System.out.println("Result = ");
    }
}
