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

import org.appwork.utils.IO;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.AbstractPastebinCrawler;
import jd.plugins.decrypter.AbstractPastebinCrawler.PastebinMetadata;

/** Use this for pastebin websites. A crawler plugin which extends AbstractPastebinCrawler is needed for this to work. */
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public abstract class AbstractPastebinHoster extends PluginForHost {
    public AbstractPastebinHoster(PluginWrapper wrapper) {
        super(wrapper);
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

    /** Returns unique ID for this DownloadLink. */
    protected String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        final PluginForDecrypt plg = this.getNewPluginForDecryptInstance(this.getHost());
        final DownloadLink plaintext = ((AbstractPastebinCrawler) plg).preProcessAndGetPlaintextDownloadLink(new CryptedLink(link.getPluginPatternMatcher(), link));
        link.setFinalFileName(plaintext.getName());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        final PluginForDecrypt plg = this.getNewPluginForDecryptInstance(this.getHost());
        final PastebinMetadata metadata = ((AbstractPastebinCrawler) plg).preProcessAndGetMetadata(new CryptedLink(link.getPluginPatternMatcher(), link));
        final String textToSave = metadata.getPastebinText();
        /* Write text to file */
        final File dest = new File(link.getFileOutput());
        IO.writeToFile(dest, textToSave.getBytes("UTF-8"), IO.SYNC.META_AND_DATA);
        /* Set filesize so user can see it in UI. */
        link.setVerifiedFileSize(dest.length());
        /* Set progress to finished - the "download" is complete. */
        link.getLinkStatus().setStatus(LinkStatus.FINISHED);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return false;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
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
    public void resetDownloadlink(final DownloadLink link) {
    }
}