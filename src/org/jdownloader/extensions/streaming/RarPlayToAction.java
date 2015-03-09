package org.jdownloader.extensions.streaming;

import java.awt.event.ActionEvent;

import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.streaming.upnp.PlayToDevice;

public class RarPlayToAction extends AppAction {

    private StreamingExtension extension;

    private PlayToDevice       device;

    private Archive            archive;

    // private ArchiveItem item;

    private static final long  serialVersionUID = 1375146705091555054L;

    @Override
    public void actionPerformed(ActionEvent e) {
    }

    // public RarPlayToAction(StreamingExtension streamingExtension, PlayToDevice d, Archive archive, ExtractionExtension extractor,
    // ArchiveItem ai) {
    // this.archive = archive;
    // this.item = ai;
    // device = d;
    // extension = streamingExtension;
    // if (ai != null) {
    // setName(T._.playto2(ai.getPath(), device.getDisplayName()));
    // } else {
    // setName(T._.playto(device.getDisplayName()));
    // }
    // Image front = NewTheme.I().getImage("media-playback-start", 20, true);
    // setSmallIcon(new ImageIcon(ImageProvider.merge(IconIO.toBufferedImage(streamingExtension.getIcon(20)), front, 0, 0, 5, 5)));
    // }

    // public void actionPerformed(ActionEvent e) {
    // DownloadLink link = ((DownloadLinkArchiveFactory) archive.getFactory()).getDownloadLinks().get(0);
    // String id;
    //
    // device.play(link, link.getUniqueID().toString(), item == null ? null : item.getPath());
    //
    // }
}
