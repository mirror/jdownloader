package jd.plugins.decrypter;

import java.io.File;
import java.util.ArrayList;

import javax.swing.Icon;

import jd.plugins.DownloadLink;
import jd.plugins.PluginProgress;
import jd.utils.JDUtilities;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.images.AbstractIcon;

public class YoutubeExternConverter implements YoutubeConverter {

    private String   binary;
    private String[] parameters;

    public YoutubeExternConverter(String string, String[] strings) {
        this.binary = string;
        this.parameters = strings;
    }

    @Override
    public void run(DownloadLink downloadLink) {

        try {
            downloadLink.setPluginProgress(new PluginProgress(0, 100, null) {
                {
                    setIcon(new AbstractIcon(IconKey.ICON_RUN, 18));

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
                    return "Convert";
                }
            });
            File file = new File(downloadLink.getFileOutput());

            // String[] commands = new String[] { "-i", "%input", "-acodec", "mp3", "-ac", "2", "-f", "mp3", "-ab", "128", "%output" };
            ArrayList<String> cmds = new ArrayList<String>();
            File finalFile = new File(downloadLink.getFileOutput(false, true));
            for (String s : parameters) {
                cmds.add(s.replace("%input", file.getAbsolutePath()).replace("%output", finalFile.getAbsolutePath()));
            }
            System.out.println(JDUtilities.runCommand(binary, cmds.toArray(new String[] {}), null, -1));

            file.delete();
            downloadLink.setDownloadSize(finalFile.length());
            downloadLink.setDownloadCurrent(finalFile.length());
            try {
                downloadLink.setFinalFileOutput(finalFile.getAbsolutePath());
                downloadLink.setCustomFileOutputFilenameAppend(null);
                downloadLink.setCustomFileOutputFilename(null);
            } catch (final Throwable e) {
            }
        } finally {
            downloadLink.setPluginProgress(null);
        }
    }

}
