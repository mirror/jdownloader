//jDownloader - Downloadmanager
//Copyright (C) 2014  JD-Team support@jdownloader.org
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

import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.DebugMode;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bato.to" }, urls = { "https?://bato\\.to/chapter/(\\d+)" })
public class BatoTo extends PluginForDecrypt {
    public BatoTo(PluginWrapper wrapper) {
        super(wrapper);
        /* Prevent server response 503! */
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 3000);
    }

    public int getMaxConcurrentProcessingInstances() {
        /* Prevent server response 503! */
        return 1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            /* 2021-06-16: Plugin is still under development */
            return null;
        }
        /* Login if possible */
        final PluginForHost hostPlugin = this.getNewPluginForHostInstance(this.getHost());
        final Account acc = AccountController.getInstance().getValidAccount(hostPlugin);
        if (acc != null) {
            ((jd.plugins.hoster.BatoTo) hostPlugin).login(acc, false);
        }
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(param.toString()));
            return decryptedLinks;
        }
        final String chapterID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final String batojs = br.getRegex("batojs = (.*?);").getMatch(0);
        String batojsEvaluated = null;
        final String serverCrypted = br.getRegex("const server = \"([^\"]+)").getMatch(0);
        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        final StringBuilder sb = new StringBuilder();
        sb.append("var batojs = " + batojs + ";");
        sb.append("var server = \"" + serverCrypted + "\";");
        /* TODO: Use Java decrypt function. */
        // sb.append("var res = JSON.parse(CryptoJS.AES.decrypt(server, batojs).toString(CryptoJS.enc.Utf8))");
        try {
            engine.eval(sb.toString());
            batojsEvaluated = engine.get("batojs").toString();
        } catch (final Exception e) {
            e.printStackTrace();
        }
        final String titleSeries = br.getRegex("<a href=\"/series/\\d+\">([^<]+)</a>").getMatch(0);
        final Regex chapterInfo = br.getRegex("property=\"og:title\"[^>]*content=\"([^>]*) - Chapter (\\d+)\"/>");
        final String titleChapter = chapterInfo.getMatch(0);
        final String chapterNumber = chapterInfo.getMatch(1);
        String imgsText = br.getRegex("const images = \\[(.*?);").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setProperty("CLEANUP_NAME", false);
        if (titleSeries == null || titleChapter == null) {
            logger.info(br.toString());
            logger.info(titleSeries + "|" + titleChapter);
            throw new DecrypterException("Decrypter broken for link: " + param);
        }
        fp.setName(titleSeries + " - Chapter " + chapterNumber + ": " + titleChapter);
        imgsText = imgsText.replace("\"", "");
        final String[] imgs = imgsText.split(",");
        int index = 0;
        final DecimalFormat df = new DecimalFormat("00");
        for (final String url : imgs) {
            final String pageNumberFormatted = df.format(index);
            final DownloadLink link = createDownloadlink("" + url);
            final String fname_without_ext = fp.getName() + " - Page " + pageNumberFormatted;
            link.setProperty("fname_without_ext", fname_without_ext);
            link.setName(fname_without_ext);
            link.setMimeHint(CompiledFiletypeFilter.ImageExtensions.BMP);
            link.setAvailable(true);
            link.setContentUrl("http://bato.to/reader#" + chapterID + "_" + pageNumberFormatted);
            fp.add(link);
            distribute(link);
            decryptedLinks.add(link);
            index++;
        }
        return decryptedLinks;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}