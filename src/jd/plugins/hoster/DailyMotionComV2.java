//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import org.jdownloader.controlling.ffmpeg.FFMpegProgress;
import org.jdownloader.controlling.ffmpeg.FFmpeg;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.translate._JDT;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.DailyMotionVariant;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "dailymotion.com" }, urls = { "https?://dailymotion\\.com/video/\\w+" })
public class DailyMotionComV2 extends DailyMotionCom {
    public DailyMotionComV2(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    // public boolean hasVariantToChooseFrom(DownloadLink downloadLink) {
    // return downloadLink.getProperty("VARIANTS") != null;
    //
    //
    // }
    public List<? extends LinkVariant> getVariantsByLink(DownloadLink downloadLink) {
        if (downloadLink.isGenericVariantSupport()) {
            return super.getVariantsByLink(downloadLink);
        }
        return downloadLink.getVariants(DailyMotionVariant.class);
    }

    @Override
    public LinkVariant getActiveVariantByLink(DownloadLink downloadLink) {
        if (downloadLink.isGenericVariantSupport()) {
            return super.getActiveVariantByLink(downloadLink);
        }
        return downloadLink.getVariant(DailyMotionVariant.class);
    }

    public void downloadDirect(final DownloadLink downloadLink) throws ParseException, Exception, PluginException, IOException {
        if (!downloadLink.hasVariantSupport()) {
            super.downloadDirect(downloadLink);
        } else {
            DailyMotionVariant var = downloadLink.getVariant(DailyMotionVariant.class);
            if (var == null || var.getConvertTo() == null) {
                super.downloadDirect(downloadLink);
            } else {
                if (var.getConvertTo() != null) {
                    downloadLink.setInternalTmpFilenameAppend(".tmp");
                }
                checkFFmpeg(downloadLink, _JDT.T.plugin_for_host_reason_for_ffmpeg_demux());
                super.downloadDirect(downloadLink);
                final FFMpegProgress set = new FFMpegProgress();
                try {
                    downloadLink.addPluginProgress(set);
                    File file = new File(downloadLink.getFileOutput());
                    FFmpeg ffmpeg = getFFmpeg(br.cloneBrowser(), downloadLink);
                    File finalFile = downloadLink.getDownloadLinkController().getFileOutput(false, true);
                    if ("aac".equals(var.getConvertTo())) {
                        if (!ffmpeg.demuxAAC(set, finalFile.getAbsolutePath(), file.getAbsolutePath())) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, _GUI.T.YoutubeDash_handleFree_error_());
                        }
                    } else {
                        if (!ffmpeg.demuxM4a(set, finalFile.getAbsolutePath(), file.getAbsolutePath())) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, _GUI.T.YoutubeDash_handleFree_error_());
                        }
                    }
                    file.delete();
                    downloadLink.setDownloadSize(finalFile.length());
                    downloadLink.setDownloadCurrent(finalFile.length());
                    try {
                        downloadLink.setInternalTmpFilenameAppend(null);
                        // downloadLink.setInternalTmpFilename(null);
                    } catch (final Throwable e) {
                    }
                } finally {
                    downloadLink.removePluginProgress(set);
                }
            }
        }
    }

    @Override
    protected void onNewDirectLink(DownloadLink dl, String dllink2) {
        if (dl.hasVariantSupport()) {
            List<? extends LinkVariant> variants = getVariantsByLink(dl);
            if (variants != null) {
                for (LinkVariant lv : variants) {
                    if (lv instanceof DailyMotionVariant) {
                        ((DailyMotionVariant) lv).setLink(dllink2);
                    }
                }
            }
            dl.setVariants(variants);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        return super.requestFileInformation(downloadLink);
    }

    public class NoAudioException extends PluginException {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public NoAudioException() {
            super(LinkStatus.ERROR_FILE_NOT_FOUND, "No Audio Stream available");
        }
    }

    public static void setActiveVariant(final DownloadLink link, final DailyMotionVariant dmv) {
        if (dmv == null) {
            return;
        }
        try {
            link.setProperty(DailyMotionCom.PROPERTY_DIRECTURL, dmv.getLink());
            link.setProperty(DailyMotionCom.PROPERTY_QUALITY_NAME, dmv.getqName());
            link.setProperty(DailyMotionCom.PROPERTY_QUALITY_HEIGHT, dmv.getVideoHeight());
            if (dmv.getConvertTo() != null) {
                link.setProperty("plain_ext", "." + dmv.getConvertTo());
            }
            final String formattedFilename = jd.plugins.hoster.DailyMotionCom.getFormattedFilename(link);
            link.setFinalFileName(formattedFilename);
            link.setVariant(dmv);
            if (dmv.getConvertTo() != null) {
                link.setInternalTmpFilenameAppend(".tmp");
            } else {
                link.setInternalTmpFilenameAppend(null);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setActiveVariantByLink(DownloadLink downloadLink, LinkVariant variant) {
        if (variant != null && variant instanceof DailyMotionVariant) {
            setActiveVariant(downloadLink, (DailyMotionVariant) variant);
        } else if (variant != null) {
            super.setActiveVariantByLink(downloadLink, variant);
        }
    }
}