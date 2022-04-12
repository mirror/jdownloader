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

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.DecrypterPlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "amateurdumper.com" }, urls = { "https?://(?:www\\.)?amateurdumper\\.com/[^/]{10,}/?" })
public class AmateurDumperCom extends PornEmbedParser {
    public AmateurDumperCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    @Override
    protected boolean returnRedirectToUnsupportedLinkAsResult() {
        return true;
    }

    @Override
    protected boolean isOffline(Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)>\\s*404 The page was not found")) {
            return true;
        } else if (br.getRedirectLocation() == null && br.getRequest().getHtmlCode().length() < 100) {
            return true;
        } else {
            return false;
        }
    }
}