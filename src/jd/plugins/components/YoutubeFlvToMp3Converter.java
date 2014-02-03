package jd.plugins.components;

import java.io.File;

import javax.swing.Icon;

import jd.plugins.DownloadLink;
import jd.plugins.PluginProgress;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.PluginTaskID;

import de.savemytube.flv.FLV;

public class YoutubeFlvToMp3Converter implements YoutubeConverter {
    private static final YoutubeFlvToMp3Converter INSTANCE = new YoutubeFlvToMp3Converter();

    /**
     * get the only existing instance of YoutubeFlvToMp3Converter. This is a singleton
     * 
     * @return
     */
    public static YoutubeFlvToMp3Converter getInstance() {
        return YoutubeFlvToMp3Converter.INSTANCE;
    }

    /**
     * Create a new instance of YoutubeFlvToMp3Converter. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */
    private YoutubeFlvToMp3Converter() {

    }

    @Override
    public void run(DownloadLink downloadLink) {
        PluginProgress old = null;
        PluginProgress set = null;
        try {
            old = downloadLink.setPluginProgress(set = new PluginProgress(0, 100, null) {
                {
                    setIcon(new AbstractIcon(IconKey.ICON_AUDIO, 18));

                }

                @Override
                public PluginTaskID getID() {
                    return PluginTaskID.CONVERT;
                }

                @Override
                public long getCurrent() {
                    return 95;
                }

                @Override
                public Icon getIcon(Object requestor) {
                    if (requestor instanceof ETAColumn) return null;
                    return super.getIcon(requestor);
                }

                @Override
                public String getMessage(Object requestor) {
                    if (requestor instanceof ETAColumn) return "";
                    return "Create Mp3";
                }
            });
            File file = new File(downloadLink.getFileOutput());

            new FLV(downloadLink.getFileOutput(), true, true);

            file.delete();
            File finalFile = new File(downloadLink.getFileOutput().replaceAll(".tmp$", ""));
            finalFile.delete();
            new File(downloadLink.getFileOutput().replaceAll(".tmp$", ".mp3")).renameTo(finalFile);
            new File(downloadLink.getFileOutput().replaceAll(".tmp$", ".avi")).delete();
            downloadLink.setDownloadSize(finalFile.length());
            downloadLink.setDownloadCurrent(finalFile.length());
            try {
                downloadLink.setFinalFileOutput(finalFile.getAbsolutePath());
                downloadLink.setCustomFileOutputFilenameAppend(null);
                downloadLink.setCustomFileOutputFilename(null);
            } catch (final Throwable e) {
            }
        } finally {
            downloadLink.compareAndSetPluginProgress(set, old);
        }
    }

}