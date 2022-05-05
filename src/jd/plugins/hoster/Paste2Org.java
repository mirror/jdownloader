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
package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.appwork.utils.IO;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { jd.plugins.decrypter.Paste2Org.class })
public class Paste2Org extends PluginForHost {
    public Paste2Org(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.PASTEBIN };
    }

    @Override
    public String getAGBLink() {
        return "https://paste2.org/";
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(jd.plugins.decrypter.Paste2Org.getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(jd.plugins.decrypter.Paste2Org.getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return jd.plugins.decrypter.Paste2Org.getAnnotationUrls();
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            /* Fallback */
            link.setName(this.getFID(link) + ".txt");
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        scanInfo(link, br);
        return AvailableStatus.TRUE;
    }

    protected void scanInfo(final DownloadLink link, final Browser br) throws PluginException {
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String textToSave = getPastebinText(br);
        if (textToSave != null) {
            try {
                link.setDownloadSize(textToSave.getBytes("UTF-8").length);
            } catch (final UnsupportedEncodingException ignore) {
                ignore.printStackTrace();
            }
        }
        link.setFinalFileName(getFilename(link));
        final String description = br.getRegex("class=\"desc\"[^>]*>\\s*<p>([^<]+)</p>").getMatch(0);
        if (description != null && link.getComment() == null) {
            link.setComment(description);
        }
    }

    public static final String getPastebinText(final Browser br) {
        return br.getRegex("(?i)<ol class='highlight code'>(.*?)</div></li></ol>").getMatch(0);
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        final String textToSave = getPastebinText(this.br);
        /* Write text to file */
        final File dest = new File(link.getFileOutput());
        IO.writeToFile(dest, textToSave.getBytes("UTF-8"), IO.SYNC.META_AND_DATA);
        /* Set filesize so user can see it in UI. */
        link.setVerifiedFileSize(dest.length());
        /* Set progress to finished - the "download" is complete. */
        link.getLinkStatus().setStatus(LinkStatus.FINISHED);
    }

    public String getFilename(final DownloadLink link) {
        return this.getFID(link) + ".txt";
    }

    @Override
    public boolean isResumeable(DownloadLink link, final Account account) {
        return false;
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}