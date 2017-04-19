//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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

import org.jdownloader.plugins.components.abstractSafeLinking;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "kprotector.com", "keeplinks.me" }, urls = { "https?://(www\\.)?kprotector\\.com/(p\\d*|d)/[a-z0-9]+", "https?://(www\\.)?keeplinks\\.(me|eu)/(p\\d*|d)/[a-z0-9]+" })
public class KeepLinksMe extends abstractSafeLinking {

    public KeepLinksMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected String regexSupportedDomains() {
        if (getHost().contains("keeplinks.me")) {
            return "keeplinks\\.(me|eu)";
        } else {
            return super.regexSupportedDomains();
        }
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = super.decryptIt_oldStyle(param, progress);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

    @Override
    protected boolean supportsHTTPS() {
        if (getHost().contains("keeplinks.me")) {
            return true;
        }
        if ("kprotector.com".equals(getHost())) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean enforcesHTTPS() {
        if (getHost().contains("keeplinks.me")) {
            return true;
        }
        if ("kprotector.com".equals(getHost())) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    @Override
    protected Form formProtected() {
        final Form f = br.getFormbyProperty("id", "frmprotect");
        return f;
    }

    @Override
    protected String regexCaptchaFancy() {
        if ("kprotector.com".equals(getHost())) {
            return "class=\"ajax-fc-container\"";
        }
        return super.regexCaptchaFancy();
    }

    @Override
    protected String getCaptchaFancyUrl() {
        if ("kprotector.com".equals(getHost())) {
            return "/fancycaptcha/captcha/captcha.php";
        }
        return super.getCaptchaFancyUrl();
    }

    @Override
    protected String getCaptchaFancyInputfieldName() {
        if ("kprotector.com".equals(getHost())) {
            return "captcha";
        }
        return super.getCaptchaFancyInputfieldName();
    }

    @Override
    protected String correctLink(final String string) {
        final String s = string.replaceFirst("^https?://", enforcesHTTPS() && supportsHTTPS() ? "https://" : "http://").replaceFirst("keeplinks\\.me/", "keeplinks.eu/");
        return s;
    }

    @Override
    protected boolean confirmationCheck() {
        if (getHost().equals("keeplinks.me")) {
            return !br.containsHTML("class=\"co_form_title\">Live Link") && !br.containsHTML("class=\"co_form_title\">Direct Link");
        } else {
            return !br.containsHTML(">Live Link</div>") && !br.containsHTML(">Direct Link</div>");
        }
    }

    @Override
    protected String regexLinks() {
        return "<lable[^>]+class=\"num(?:live|direct) nodisplay\"[^>]*>(.*?)</a>(?:<br\\s*/>|</label>)";
    }

    @Override
    protected String regexContainerDlc() {
        if ("kprotector.com".equals(getHost())) {
            return "/download/" + uid + "/dlc";
        }
        return super.regexContainerDlc();
    }

    @Override
    protected String regexContainerCcf() {
        if ("kprotector.com".equals(getHost())) {
            return "/download/" + uid + "/ccf";
        }
        return super.regexContainerCcf();
    }

    @Override
    protected String regexContainerRsdf() {
        if ("kprotector.com".equals(getHost())) {
            return "/download/" + uid + "/rsdf";
        }
        return super.regexContainerRsdf();
    }

    @Override
    protected String regexContainer(final String format) {
        if ("kprotector.com".equals(getHost())) {
            return "\"(https?://[^/]*" + regexSupportedDomains() + format + ")";
        }
        return super.regexContainer(format);
    }

    @Override
    protected boolean supportsContainers() {
        if ("kprotector.com".equals(getHost())) {
            return false;
        }
        return super.supportsContainers();
    }

    @Override
    protected String formPasswordInputKeyName() {
        return "password";
    }

    @Override
    protected boolean isCaptchaSkipable() {
        return true;
    }

}