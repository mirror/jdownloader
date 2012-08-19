package org.jdownloader.extensions.streaming;

import java.awt.Image;
import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import jd.plugins.DownloadLink;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.streaming.upnp.PlayToDevice;
import org.jdownloader.images.NewTheme;

public class DirectPlayToAction extends AppAction {
    DownloadLink               link             = null;

    private StreamingExtension extension;

    private PlayToDevice       device;

    private static final long  serialVersionUID = 1375146705091555054L;

    public DirectPlayToAction(StreamingExtension streamingExtension, PlayToDevice d, DownloadLink link) {

        this.link = link;
        device = d;
        extension = streamingExtension;
        setName(T._.playto(device.getDisplayName()));
        Image front = NewTheme.I().getImage("media-playback-start", 20, true);
        setSmallIcon(new ImageIcon(ImageProvider.merge(streamingExtension.getIcon(20).getImage(), front, 0, 0, 5, 5)));

    }

    public boolean isDirectPlaySupported(DownloadLink link) {
        if (link == null) return false;

        return true;
    }

    public void actionPerformed(ActionEvent e) {

        device.play(link, link.getUniqueID().toString(), null);

    }

}
