package org.jdownloader.controlling.linkcrawler;

import java.io.File;
import java.util.List;

import javax.swing.Icon;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.ExceptionRunnable;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Files;
import org.jdownloader.controlling.ffmpeg.FFMpegProgress;
import org.jdownloader.controlling.ffmpeg.FFmpeg;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.translate._JDT;

public enum GenericVariants implements LinkVariant {
    ORIGINAL(null, new AbstractIcon(IconKey.ICON_VIDEO, 16)) {

        @Override
        public String _getName() {
            return _JDT._.GenericVariants_ORIGINAL();
        }

        public void runPostDownload(PluginForHost pluginForHost, DownloadLink downloadLink, Account account) throws Exception {

        }

        @Override
        public void runPreDownload(PluginForHost pluginForHost, DownloadLink downloadLink, Account account) throws Exception {

        }
    },
    DEMUX_MP3("mp3", new AbstractIcon(IconKey.ICON_AUDIO, 16)) {
        @Override
        public String _getName() {
            return _JDT._.GenericVariants_FLV_TO_MP3_();
        }

        @Override
        public void runPreDownload(PluginForHost pluginForHost, DownloadLink downloadLink, Account account) throws SkipReasonException, InterruptedException {
            pluginForHost.checkFFmpeg(downloadLink, _JDT._.plugin_for_host_reason_for_ffmpeg_demux());
            downloadLink.setInternalTmpFilenameAppend("." + name() + ".tmp");
        }

        public boolean ffmpeg(DownloadLink link, File file, FFMpegProgress set, FFmpeg ffmpeg, File finalFile) throws Exception {
            return ffmpeg.demuxMp3(set, finalFile.getAbsolutePath(), file.getAbsolutePath());
        }
    },

    DEMUX_AAC("acc", new AbstractIcon(IconKey.ICON_AUDIO, 16)) {
        @Override
        public String _getName() {
            return _JDT._.GenericVariants_DEMUX_AAC();
        }

        @Override
        public void runPreDownload(PluginForHost pluginForHost, DownloadLink downloadLink, Account account) throws SkipReasonException, InterruptedException {
            pluginForHost.checkFFmpeg(downloadLink, _JDT._.plugin_for_host_reason_for_ffmpeg_demux());
            downloadLink.setInternalTmpFilenameAppend("." + name() + ".tmp");
        }

        public boolean ffmpeg(DownloadLink link, File file, FFMpegProgress set, FFmpeg ffmpeg, File finalFile) throws Exception {
            return ffmpeg.demuxAAC(set, finalFile.getAbsolutePath(), file.getAbsolutePath());
        }
    },
    DEMUX_M4A("m4a", new AbstractIcon(IconKey.ICON_AUDIO, 16)) {
        @Override
        public String _getName() {
            return _JDT._.GenericVariants_DEMUX_M4A();
        }

        @Override
        public void runPreDownload(PluginForHost pluginForHost, DownloadLink downloadLink, Account account) throws SkipReasonException, InterruptedException {
            pluginForHost.checkFFmpeg(downloadLink, _JDT._.plugin_for_host_reason_for_ffmpeg_demux());
            downloadLink.setInternalTmpFilenameAppend("." + name() + ".tmp");
        }

        public boolean ffmpeg(DownloadLink link, File file, FFMpegProgress set, FFmpeg ffmpeg, File finalFile) throws Exception {
            return ffmpeg.demuxM4a(set, finalFile.getAbsolutePath(), file.getAbsolutePath());
        }
    },
    DEMUX_GENERIC_AUDIO("audio", new AbstractIcon(IconKey.ICON_AUDIO, 16)) {
        @Override
        public String _getName() {
            return _JDT._.GenericVariants_DEMUX_GENERIC_AUDIO();
        }

        @Override
        public void runPreDownload(PluginForHost pluginForHost, DownloadLink downloadLink, Account account) throws Exception {
            pluginForHost.checkFFmpeg(downloadLink, _JDT._.plugin_for_host_reason_for_ffmpeg_demux());
            downloadLink.setInternalTmpFilenameAppend("." + name() + ".tmp");
        }

        public boolean ffmpeg(final DownloadLink link, final File file, FFMpegProgress set, FFmpeg ffmpeg, File finalFile) throws Exception {
            final List<File> ret = ffmpeg.demuxAudio(set, finalFile.getAbsolutePath(), file.getAbsolutePath());
            if (ret != null) {
                String base = finalFile.getAbsolutePath().substring(0, finalFile.getAbsolutePath().length() - ("." + Files.getExtension(finalFile.getAbsolutePath())).length());
                for (int i = 0; i < ret.size(); i++) {
                    final File out = ret.get(i);

                    String extension = Files.getExtension(out.getAbsolutePath());

                    if (i == 0) {
                        final File newFile = new File(base + "." + extension);
                        link.setCustomExtension(extension);
                        link.setVerifiedFileSize(newFile.length());
                        if (!out.renameTo(newFile)) {
                            String before = link.getInternalTmpFilenameAppend();
                            final String forcedBefore = link.getForcedFileName();
                            try {
                                link.setInternalTmpFilenameAppend(null);
                                DownloadWatchDog.getInstance().localFileCheck(link.getDownloadLinkController(), new ExceptionRunnable() {

                                    @Override
                                    public void run() throws Exception {
                                        String after = link.getForcedFileName();
                                        if (after != null && !after.equals(forcedBefore)) {
                                            // rename choosen
                                            out.renameTo(new File(newFile.getParentFile(), after));
                                        } else {
                                            out.renameTo(newFile);
                                        }
                                    }
                                }, new ExceptionRunnable() {

                                    @Override
                                    public void run() throws Exception {
                                        file.delete();
                                        for (File f : ret) {
                                            f.delete();
                                        }

                                    }
                                });
                            } finally {
                                link.setInternalTmpFilenameAppend(before);
                            }
                            // throw new PluginException(LinkStatus.ERROR_ALREADYEXISTS);
                        }

                    } else {

                        while (out.exists() && !out.renameTo(new File(base + "." + (i + 1) + "." + extension))) {
                            i++;
                        }

                    }

                }
            }
            return ret != null;
        }
    };
    private Icon   icon = null;
    private String extension;

    private GenericVariants(String extension, Icon icon) {
        this.icon = icon;
        this.extension = extension;
    }

    @Override
    public String _getUniqueId() {
        return name();
    }

    @Override
    public abstract String _getName();

    @Override
    public Icon _getIcon() {
        return icon;
    }

    @Override
    public String _getExtendedName() {
        return _getName() + " [" + _getUniqueId() + "]";
    }

    public String getExtension() {
        return extension;
    }

    public void runPreDownload(PluginForHost pluginForHost, DownloadLink downloadLink, Account account) throws Exception {
    }

    public void runPostDownload(PluginForHost pluginForHost, DownloadLink downloadLink, Account account) throws Exception {
        File file = new File(downloadLink.getFileOutput());
        FFMpegProgress set = new FFMpegProgress();
        try {
            downloadLink.addPluginProgress(set);

            FFmpeg ffmpeg = new FFmpeg();

            File finalFile = downloadLink.getDownloadLinkController().getFileOutput(false, true);
            boolean res = ffmpeg(downloadLink, file, set, ffmpeg, finalFile);
            if (!res) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, _JDT._.PluginForHost_handle_ffmpeg_conversion_failed());
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

    protected boolean ffmpeg(DownloadLink link, File file, FFMpegProgress set, FFmpeg ffmpeg, File finalFile) throws Exception {
        return false;
    }

}
