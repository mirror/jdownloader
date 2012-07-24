package org.jdownloader.extensions.vlcstreaming.rarstream;

import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.ISevenZipInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

import org.appwork.utils.logging2.LogSource;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionException;
import org.jdownloader.extensions.extraction.content.ContentView;
import org.jdownloader.extensions.extraction.content.PackedFile;
import org.jdownloader.logging.LogController;

public class RarStreamer implements Runnable {

    private Archive            archive;
    private ISevenZipInArchive rarArchive;
    private LogSource          logger;

    public RarStreamer(Archive archive) {
        this.archive = archive;
        logger = LogController.CL();
    }

    public void start() {
        new Thread(this, "RarStreamer").start();
    }

    @Override
    public void run() {
        try {
            open();
        } catch (ExtractionException e) {
            logger.log(e);
        }
    }

    private void updateContentView(ISimpleInArchive simpleInterface) {
        try {
            ContentView newView = new ContentView();
            for (ISimpleInArchiveItem item : simpleInterface.getArchiveItems()) {
                try {
                    if (item.getPath().trim().equals("")) continue;
                    newView.add(new PackedFile(item.isFolder(), item.getPath(), item.getSize()));
                } catch (SevenZipException e) {
                    logger.log(e);
                }
            }
            archive.setContentView(newView);
        } catch (SevenZipException e) {
            logger.log(e);
        }
    }

    private void open() throws ExtractionException {
        RarStreamProvider streamProvider = new RarStreamProvider(archive);
        try {

            IInStream rarStream = streamProvider.getStream(archive.getFirstArchiveFile());
            rarArchive = SevenZip.openInArchive(ArchiveFormat.RAR, rarStream, streamProvider);
            for (ISimpleInArchiveItem item : rarArchive.getSimpleInterface().getArchiveItems()) {
                if (item.isEncrypted()) {
                    archive.setProtected(true);
                    break;
                }
            }
            updateContentView(rarArchive.getSimpleInterface());
            System.out.println(archive.getContentView().getChildren());
            rarArchive.close();
        } catch (SevenZipException e) {
            logger.log(e);
            if (e.getMessage().contains("HRESULT: 0x80004005") || e.getMessage().contains("No password was provided")) {

                // file password protected: net.sf.sevenzipjbinding.SevenZipException: HRESULT: 0x80004005 (Fail). Archive file (format:
                // Rar)
                // can't be opened
                // There are password protected multipart rar files
                archive.setProtected(true);

            } else {
                throw new ExtractionException(e, streamProvider != null ? streamProvider.getLatestAccessedStream().getArchiveFile() : null);
            }
        } catch (Throwable e) {
            logger.log(e);
            throw new ExtractionException(e, streamProvider != null ? streamProvider.getLatestAccessedStream().getArchiveFile() : null);
        }

    }

}
