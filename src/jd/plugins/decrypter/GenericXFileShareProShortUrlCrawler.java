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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.components.SiteType.SiteTemplate;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class GenericXFileShareProShortUrlCrawler extends antiDDoSForDecrypt {
    public GenericXFileShareProShortUrlCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        ret.add(new String[] { "ddownload.com", "ddl.to", "api.ddl.to", "esimpurcuesc.ddownload.com" });
        ret.add(new String[] { "deimos.click", "phobos.click" });
        ret.add(new String[] { "file.al", "1f.al" });
        ret.add(new String[] { "daofile.com", "dsht.link" });
        /* 2021-01-27: Not sure whether filecheck.link and container.cool actually belong to hotlink.cc */
        ret.add(new String[] { "hotlink.cc", "redirect.codes", "filecheck.link", "container.cool" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/d/([A-Za-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String fid_short = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || !br.getURL().contains(fid_short)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        /* Check for direct redirect */
        if (br.getURL().matches("https?://[^/]+/[a-z0-9]{12}")) {
            decryptedLinks.add(createDownloadlink(this.br.getURL()));
            return decryptedLinks;
        }
        String fid = null;
        final Form form = br.getFormbyProperty("name", "F1");
        if (form != null) {
            fid = form.getInputFieldByName("id").getValue();
        }
        if (fid == null || !fid.matches("[a-z0-9]{12}")) {
            /* Assume that URL is offline */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String host;
        if (!StringUtils.isEmpty(form.getAction())) {
            host = Browser.getHost(form.getAction());
        } else {
            host = this.getHost();
        }
        decryptedLinks.add(createDownloadlink("https://" + host + "/" + fid));
        return decryptedLinks;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.SibSoft_XFileShare;
    }
}
