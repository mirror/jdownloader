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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class AdshortCo extends MightyScriptAdLinkFly {
    public AdshortCo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "adshort.co", "adsrt.com", "adsrt.me", "adshort.me", "adshort.im" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([A-Za-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    protected ArrayList<String> getDeadDomains() {
        final ArrayList<String> deadDomains = new ArrayList<String>();
        deadDomains.add("adshort.im"); // 2022-07-07
        deadDomains.add("adshort.me"); // 2022-07-07
        return deadDomains;
    }

    @Override
    protected ArrayList<DownloadLink> handlePreCrawlProcess(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        param.setCryptedUrl(param.getCryptedUrl().replaceFirst("http://", "https://"));
        if (param.getCryptedUrl().matches("https?://[^/]+/[a-z0-9]+")) {
            /**
             * Only lowercase contentID -> Invalid e.g.: </br>
             * https://adshort.co/anunciantes </br>
             * https://adshort.co/rates
             */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.setFollowRedirects(true);
        /* 2023-08-16: This referer skips captcha & two redirect-forms */
        br.getHeaders().put("Referer", "https://techgeek.digital/");
        getPage(param.getCryptedUrl());
        /* 2022-06-23: Extra step needed */
        Form preForm = br.getFormByInputFieldKeyValue("submit", "continue");
        if (preForm == null) {
            preForm = br.getFormbyProperty("id", "setc");
        }
        if (preForm != null) {
            this.submitForm(preForm);
        }
        final Form preform2 = br.getFormbyKey("FU4");
        if (preform2 != null) {
            logger.warning("Found preform2 which should be auto-skipped");
        }
        if (this.regexAppVars(this.br) == null) {
            logger.warning("Possible crawler failure...");
        }
        /* Now continue with parent class code (requires captcha + waittime) */
        return ret;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        return super.decryptIt(param, progress);
    }
}
