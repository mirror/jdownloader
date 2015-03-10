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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "keeplinks.me", "safemy.link" }, urls = { "https?://(www\\.)?keeplinks\\.(me|eu)/(p|d)/[a-z0-9]+", "http://(www\\.)?safemy\\.link/(p|d)/[a-z0-9]+" }, flags = { 0, 0 })
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
        ArrayList<DownloadLink> decryptedLinks = super.decryptIt(param, progress);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

    @Override
    protected boolean supportsHTTPS() {
        return false;
    }

    @Override
    protected boolean enforcesHTTPS() {
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
    protected String correctLink(final String string) {
        final String s = string.replaceFirst("^https?://", enforcesHTTPS() && supportsHTTPS() ? "https://" : "http://").replaceFirst("keeplinks\\.me/", "keeplinks.eu/");
        return s;
    }

    @Override
    protected boolean confirmationCheck() {
        return !br.containsHTML("class=\"co_form_title\">Live Link") && !br.containsHTML("class=\"co_form_title\">Direct Link");
    }

    @Override
    protected String regexLinks() {
        return "<lable[^>]+class=\"num(?:live|direct) nodisplay\"[^>]*>(.*?)</a>(?:<br\\s*/>|</label>)";
    }

}