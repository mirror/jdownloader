package org.jdownloader.extensions.streaming;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.Locale;

import javax.swing.ImageIcon;

import jd.Launcher;
import jd.nutils.Executer;
import jd.plugins.DownloadLink;

import org.appwork.utils.Application;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.jdownloader.actions.AppAction;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.streaming.rarstream.RarStreamer;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;

public class RarStreamAction extends AppAction {

    private Archive            archive;
    private StreamingExtension extension;

    public RarStreamAction(Archive archive, ExtractionExtension extractor, StreamingExtension extension) {

        setName(T._.unraraction());
        Image front = NewTheme.I().getImage("archive", 14, true);
        setSmallIcon(new ImageIcon(ImageProvider.merge(extension.getIcon(20).getImage(), front, 0, 0, 5, 5)));
        this.archive = archive;
        this.extension = extension;
    }

    public boolean isDirectPlaySupported(DownloadLink link) {
        if (link == null) return false;
        String filename = link.getName().toLowerCase(Locale.ENGLISH);
        if (filename.endsWith("rar")) return true;
        return false;
    }

    public void actionPerformed(ActionEvent e) {

        RarStreamer rs = new RarStreamer(archive, extension);
        rs.start();
        open(rs.getID());

    }

    protected void open(String ID) {
        Executer exec = new Executer(extension.getVLCBinary());
        exec.setLogger(LogController.CL());
        exec.addParameters(new String[] { "http://127.0.0.1:" + RemoteAPIController.getInstance().getApiPort() + "/vlcstreaming/play?id=" + ID });
        exec.setRunin(Application.getRoot(Launcher.class));
        exec.setWaitTimeout(0);
        exec.start();
    }
}
