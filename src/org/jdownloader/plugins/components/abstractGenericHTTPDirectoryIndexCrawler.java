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
package org.jdownloader.plugins.components;

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DownloadLink;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.SiteType.SiteTemplate;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public abstract class abstractGenericHTTPDirectoryIndexCrawler extends PluginForDecrypt {
    protected abstractGenericHTTPDirectoryIndexCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public enum DirectoryListingMode {
        NGINX,
        APACHE
    }

    public abstract ArrayList<DownloadLink> parseHTTPDirectory(final CryptedLink param, final Browser br) throws IOException, PluginException, DecrypterRetryException;

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.GenericHTTPDirectoryIndex;
    }
}
