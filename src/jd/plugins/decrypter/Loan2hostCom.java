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
import jd.http.Browser;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.parser.html.InputField;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class Loan2hostCom extends MightyScriptAdLinkFly {
    public Loan2hostCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "tei.ai", "loan2host.com", "tii.ai", "tii.la", "wishes2.com", "ckk.ai" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/[A-Za-z0-9]+/?");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        /* Default handling */
        return super.decryptIt(param, progress);
    }

    @Override
    protected void hookAfterCaptcha(final Browser br, Form form) throws Exception {
        /*
         * 2021-10-11: Special: Previous form will redirect to external website e.g. "makemoneywithurl.com" which will then redirect back to
         * the initial site.
         */
        Form ret = null;
        final Form[] forms = br.getForms();
        for (final Form search : forms) {
            if (search.containsHTML("(?i)Generating Link\\.\\.\\.")) {
                ret = search;
                break;
            }
        }
        if (ret == null) {
            final String getlink = br.getRegex("document\\.getElementById\\(\"getlink\"\\)\\.href\\s*=\\s*'(.*?)'").getMatch(0);
            if (getlink != null && form != null) {
                // loan2host
                ret = new Form();
                ret.setMethod(MethodType.POST);
                ret.setAction(getlink);
                final InputField token = form.getInputField("token");
                if (token != null) {
                    ret.put("token", token.getValue());
                }
                ret.put("_method", "POST");
                final InputField c_d = form.getInputField("c_d");
                if (c_d != null) {
                    ret.put("c_d", c_d.getValue());
                }
                final InputField c_t = form.getInputField("c_t");
                if (c_t != null) {
                    ret.put("c_t", c_t.getValue());
                }
                final InputField alias = form.getInputField("alias");
                if (alias != null) {
                    ret.put("alias", alias.getValue());
                }
            }
        }
        if (ret != null) {
            this.submitForm(ret);
        }
    }
}
