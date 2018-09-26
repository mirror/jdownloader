//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
package org.jdownloader.extensions.extraction.split;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.download.raf.FileBytesCache;
import jd.plugins.download.raf.FileBytesCacheFlusher;
import net.sf.sevenzipjbinding.SevenZipException;

import org.appwork.uio.CloseReason;
import org.appwork.utils.Files;
import org.appwork.utils.IO;
import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.CPUPriority;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.DummyArchiveFile;
import org.jdownloader.extensions.extraction.ExtractionConfig;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionControllerConstants;
import org.jdownloader.extensions.extraction.ExtractionControllerException;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.Item;
import org.jdownloader.extensions.extraction.MissingArchiveFile;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
import org.jdownloader.extensions.extraction.content.PackedFile;
import org.jdownloader.extensions.extraction.gui.iffileexistsdialog.IfFileExistsDialog;
import org.jdownloader.extensions.extraction.multi.CheckException;
import org.jdownloader.settings.IfFileExistsAction;

class SplitUtil {
    protected static void checkComplete(ExtractionExtension extension, Archive archive, DummyArchive dummyArchive) throws CheckException {
        final SplitType splitType = archive.getSplitType();
        if (dummyArchive.isComplete() && splitType != null) {
            final ArchiveFile lastArchiveFile = archive.getLastArchiveFile();
            if (lastArchiveFile != null) {
                final DownloadLinkArchiveFactory factory;
                if (archive.getFactory() instanceof DownloadLinkArchiveFactory) {
                    factory = (DownloadLinkArchiveFactory) archive.getFactory();
                } else if (archive.getParentArchive() != null && archive.getParentArchive().getFactory() instanceof DownloadLinkArchiveFactory) {
                    factory = (DownloadLinkArchiveFactory) archive.getParentArchive().getFactory();
                } else {
                    factory = null;
                }
                if (factory != null) {
                    final int nextIndex = splitType.getPartNumber(splitType.getPartNumberString(lastArchiveFile.getFilePath())) + 1;
                    final List<ArchiveFile> maybeMissingArchiveFiles = SplitType.getMissingArchiveFiles(archive, splitType, nextIndex);
                    if (maybeMissingArchiveFiles.size() > 0) {
                        final Set<String> archiveIDs = new HashSet<String>();
                        factoryLoop: for (final DownloadLink downloadLink : factory.getDownloadLinks()) {
                            final FilePackage fp = downloadLink.getFilePackage();
                            final boolean readL = fp.getModifyLock().readLock();
                            try {
                                final List<Archive> searchArchives = extension.getArchivesFromPackageChildren(fp.getChildren(), archiveIDs, -1);
                                if (searchArchives != null) {
                                    for (final Archive searchArchive : searchArchives) {
                                        if (archiveIDs.add(searchArchive.getArchiveID())) {
                                            for (final ArchiveFile maybeMissingArchiveFile : maybeMissingArchiveFiles) {
                                                if (StringUtils.equals(maybeMissingArchiveFile.getName(), searchArchive.getName())) {
                                                    dummyArchive.add(new DummyArchiveFile(new MissingArchiveFile(searchArchive, maybeMissingArchiveFile.getFilePath())));
                                                    break factoryLoop;
                                                }
                                            }
                                        }
                                    }
                                }
                            } finally {
                                fp.getModifyLock().readUnlock(readL);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Merges the files from the archive. The filepaths need to be sortable.
     *
     * @param controller
     * @param destination
     *            The outputfile.
     * @param skipBytesFirstPart
     *            The amount of bytes that should be skipped.
     * @return
     * @throws SevenZipException
     */
    protected static boolean merge(final ExtractionController controller, String fileName, final int skipBytesFirstPart, ExtractionConfig config) throws ExtractionControllerException {
        CPUPriority priority = config.getCPUPriority();
        if (priority == null || CPUPriority.HIGH.equals(priority)) {
            priority = null;
        }
        final Archive archive = controller.getArchive();
        long size = -skipBytesFirstPart;
        for (ArchiveFile l : archive.getArchiveFiles()) {
            size += l.getFileSize();
        }
        final String mergeTo = controller.getExtractToFolder().getAbsoluteFile() + File.separator + fileName;
        File destination = new File(mergeTo);
        controller.getArchive().getContentView().add(new PackedFile(false, archive.getName(), size));
        controller.setCompleteBytes(size);
        controller.setProcessedBytes(0);
        final FileBytesCache cache = controller.getFileBytesCache();
        RandomAccessFile fos = null;
        FileBytesCacheFlusher flusher = null;
        final AtomicBoolean fileOpen = new AtomicBoolean(false);
        try {
            /*
             * write buffer, use same as downloadbuffer, so we have a pool of same sized buffers
             */
            if (destination.exists()) {
                /* file already exists */
                IfFileExistsAction action = controller.getIfFileExistsAction();
                while (action == null || action == IfFileExistsAction.ASK_FOR_EACH_FILE) {
                    IfFileExistsDialog d = new IfFileExistsDialog(destination, controller.getCurrentActiveItem(), archive);
                    d.show();
                    if (d.getCloseReason() != CloseReason.OK) {
                        throw new ExtractionControllerException(ExtractionControllerConstants.EXIT_CODE_USER_BREAK);
                    }
                    action = d.getAction();
                    if (action == null) {
                        throw new ExtractionControllerException(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
                    }
                    if (action == IfFileExistsAction.AUTO_RENAME) {
                        destination = new File(destination.getParentFile(), d.getNewName());
                        if (destination.exists()) {
                            action = IfFileExistsAction.ASK_FOR_EACH_FILE;
                        }
                    }
                }
                switch (action) {
                case OVERWRITE_FILE:
                    if (!FileCreationManager.getInstance().delete(destination, null)) {
                        throw new ExtractionControllerException(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR, "Could not overwrite(delete) " + destination);
                    }
                    break;
                case SKIP_FILE:
                    /* skip file */
                    archive.addSkippedFiles(destination);
                    return true;
                case AUTO_RENAME:
                    String extension = Files.getExtension(destination.getName());
                    String name = StringUtils.isEmpty(extension) ? destination.getName() : destination.getName().substring(0, destination.getName().length() - extension.length() - 1);
                    int i = 1;
                    while (destination.exists()) {
                        if (StringUtils.isEmpty(extension)) {
                            destination = new File(destination.getParentFile(), name + "_" + i);
                        } else {
                            destination = new File(destination.getParentFile(), name + "_" + i + "." + extension);
                        }
                        i++;
                    }
                    break;
                }
            }
            if ((!destination.getParentFile().exists() && !FileCreationManager.getInstance().mkdir(destination.getParentFile())) || !destination.createNewFile()) {
                archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
                return false;
            }
            controller.setCurrentActiveItem(new Item(destination.getName(), size, destination));
            fos = IO.open(destination, "rw");
            archive.addExtractedFiles(destination);
            fileOpen.set(true);
            final RandomAccessFile ffos = fos;
            final NullsafeAtomicReference<IOException> ioException = new NullsafeAtomicReference<IOException>(null);
            flusher = new FileBytesCacheFlusher() {
                @Override
                public void flushed() {
                }

                @Override
                public void flush(byte[] writeCache, int writeCachePosition, int length, long fileWritePosition) {
                    if (fileOpen.get()) {
                        try {
                            ffos.seek(fileWritePosition);
                            ffos.write(writeCache, writeCachePosition, length);
                        } catch (final IOException e) {
                            ioException.set(e);
                            fileOpen.set(false);
                        }
                    }
                }
            };
            final byte[] buffer = new byte[32767];
            long fileWritePosition = 0;
            for (int i = 0; i < archive.getArchiveFiles().size(); i++) {
                final File source = new File(archive.getArchiveFiles().get(i).getFilePath());
                FileInputStream in = null;
                try {
                    in = new FileInputStream(source);
                    if (i == 0 && skipBytesFirstPart > 0) {
                        in.skip(skipBytesFirstPart);
                    }
                    int read = 0;
                    while ((read = in.read(buffer)) >= 0) {
                        if (read == 0) {
                            /* nothing read, we wait a moment and see again */
                            Thread.yield();
                            continue;
                        }
                        cache.write(flusher, fileWritePosition, buffer, read);
                        fileWritePosition += read;
                        controller.addProcessedBytesAndPauseIfNeeded(read);
                        if (controller.gotKilled()) {
                            throw new IOException("Extraction has been aborted!");
                        }
                        if (ioException.get() != null) {
                            throw ioException.get();
                        }
                        if (priority != null && !CPUPriority.HIGH.equals(priority)) {
                            try {
                                Thread.sleep(priority.getTime());
                            } catch (InterruptedException e) {
                                throw new IOException(e);
                            }
                        }
                    }
                } finally {
                    try {
                        in.close();
                    } catch (final Throwable e) {
                    }
                }
            }
        } catch (IOException e) {
            controller.setExeption(e);
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_WRITE_ERROR);
            return false;
        } finally {
            final FileBytesCacheFlusher fflusher = flusher;
            cache.execute(new Runnable() {
                @Override
                public void run() {
                    if (fileOpen.get()) {
                        try {
                            if (fflusher != null) {
                                cache.flushIfContains(fflusher);
                            }
                        } finally {
                            fileOpen.set(false);
                        }
                    }
                }
            });
            try {
                try {
                    if (fos != null) {
                        fos.getChannel().force(true);
                    }
                } finally {
                    if (fos != null) {
                        fos.close();
                    }
                }
            } catch (Throwable e) {
            }
        }
        return true;
    }
}