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

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.plugins.download.raf.FileBytesCache;
import jd.plugins.download.raf.FileBytesCacheFlusher;

import org.appwork.uio.CloseReason;
import org.appwork.utils.Files;
import org.appwork.utils.IO;
import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.StringUtils;
import org.appwork.utils.awfc.AWFCUtils;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.DummyArchiveFile;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionControllerConstants;
import org.jdownloader.extensions.extraction.ExtractionControllerException;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.IExtraction;
import org.jdownloader.extensions.extraction.Item;
import org.jdownloader.extensions.extraction.MissingArchiveFile;
import org.jdownloader.extensions.extraction.content.PackedFile;
import org.jdownloader.extensions.extraction.gui.iffileexistsdialog.IfFileExistsDialog;
import org.jdownloader.extensions.extraction.multi.ArchiveException;
import org.jdownloader.extensions.extraction.multi.CheckException;
import org.jdownloader.settings.IfFileExistsAction;

public class XtreamSplit extends IExtraction {

    private final static int                       HEADER_SIZE = 104;

    private boolean                                md5         = false;
    private File                                   outputFile;
    private final WeakHashMap<ArchiveFile, byte[]> hashes      = new WeakHashMap<ArchiveFile, byte[]>();

    private final SplitType                        splitType   = SplitType.XTREMSPLIT;

    public Archive buildArchive(ArchiveFactory link, boolean allowDeepInspection) throws ArchiveException {
        return SplitType.createArchive(link, splitType, allowDeepInspection);
    }

    @Override
    public boolean findPassword(ExtractionController controller, String password, boolean optimized) {
        return true;
    }

    @Override
    public void extract(ExtractionController ctrl) {
        final Archive archive = getExtractionController().getArchive();
        final FileBytesCache cache = getExtractionController().getFileBytesCache();
        RandomAccessFile fos = null;
        FileBytesCacheFlusher flusher = null;
        final AtomicBoolean fileOpen = new AtomicBoolean(false);
        try {
            long size = -HEADER_SIZE + (md5 ? (32 * archive.getArchiveFiles().size()) : 0);
            for (ArchiveFile l : archive.getArchiveFiles()) {
                size += l.getFileSize();
            }
            /* file already exists */
            if (outputFile.exists()) {
                IfFileExistsAction action = getExtractionController().getIfFileExistsAction();
                while (action == null || action == IfFileExistsAction.ASK_FOR_EACH_FILE) {
                    final IfFileExistsDialog d = new IfFileExistsDialog(outputFile, getExtractionController().getCurrentActiveItem(), archive);
                    d.show();
                    if (d.getCloseReason() != CloseReason.OK) {
                        throw new ExtractionControllerException(ExtractionControllerConstants.EXIT_CODE_USER_BREAK);
                    } else {
                        action = d.getAction();
                    }
                    if (action == null) {
                        throw new ExtractionControllerException(ExtractionControllerConstants.EXIT_CODE_USER_BREAK);
                    }
                    if (action == IfFileExistsAction.AUTO_RENAME) {
                        outputFile = new File(outputFile.getParentFile(), d.getNewName());
                        if (outputFile.exists()) {
                            action = IfFileExistsAction.ASK_FOR_EACH_FILE;
                        }
                    }
                }
                switch (action) {
                case OVERWRITE_FILE:
                    if (!FileCreationManager.getInstance().delete(outputFile, null)) {
                        throw new ExtractionControllerException(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR, "Could not overwrite(delete) " + outputFile);
                    }
                    break;
                case SKIP_FILE:
                    /* skip file */
                    archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_SUCCESS);
                    archive.addSkippedFiles(outputFile);
                    return;
                case AUTO_RENAME:
                    String extension = Files.getExtension(outputFile.getName());
                    String name = StringUtils.isEmpty(extension) ? outputFile.getName() : outputFile.getName().substring(0, outputFile.getName().length() - extension.length() - 1);
                    int i = 1;
                    final File parent = outputFile.getParentFile();
                    while (outputFile.exists()) {
                        if (StringUtils.isEmpty(extension)) {
                            outputFile = new File(parent, name + "_" + i++);
                        } else {
                            outputFile = new File(parent, name + "_" + (i++) + "." + extension);
                        }
                    }
                    break;
                }
            }
            if ((!outputFile.getParentFile().exists() && !FileCreationManager.getInstance().mkdir(outputFile.getParentFile())) || !outputFile.createNewFile()) {
                archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
                return;
            }
            getExtractionController().getArchive().getContentView().add(new PackedFile(false, archive.getName(), size));
            getExtractionController().setCompleteBytes(size);
            getExtractionController().setProcessedBytes(0);
            fos = IO.open(outputFile, "rw");
            archive.addExtractedFiles(outputFile);
            long fileWritePosition = 0;
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
            getExtractionController().setCurrentActiveItem(new Item(outputFile.getName(), size, outputFile));
            final byte[] buffer = new byte[32767];
            for (int partIndex = 0; partIndex < archive.getArchiveFiles().size(); partIndex++) {
                final File part = new File(archive.getArchiveFiles().get(partIndex).getFilePath());
                InputStream in = null;
                MessageDigest md = null;
                try {
                    in = new FileInputStream(part);
                    if (md5) {
                        md = MessageDigest.getInstance("md5");
                    }
                    // Skip header in case of the first file
                    // can't call in.skip(), because the header counts
                    // the towards md5 hash.
                    long partLength = part.length();
                    if (partIndex == 0) {
                        int alreadySkipped = 0;
                        final int toBeSkipped = HEADER_SIZE;
                        while (alreadySkipped < toBeSkipped) {
                            // Read data
                            int l = in.read(buffer, 0, toBeSkipped - alreadySkipped);
                            alreadySkipped += l;
                            if (md5) {
                                // Update MD5
                                md.update(buffer, 0, l);
                            }
                        }
                        partLength = partLength - toBeSkipped;
                    }

                    long partRead = 0;
                    // Skip md5 hashes at the end if it's the last file
                    final boolean isItTheLastFile = partIndex == (archive.getArchiveFiles().size() - 1);
                    if (md5 && isItTheLastFile) {
                        partLength = partLength - 32 * archive.getArchiveFiles().size();
                    }
                    // Merge
                    while (partLength > partRead) {
                        // Calculate the read buffer for the remaining byte.
                        final int readLength = ((partLength - partRead) < buffer.length) ? (int) (partLength - partRead) : buffer.length;
                        final int dataRead = in.read(buffer, 0, readLength);
                        if (dataRead > 0) {
                            cache.write(flusher, fileWritePosition, buffer, dataRead);
                            fileWritePosition += dataRead;
                            if (getExtractionController().gotKilled()) {
                                throw new ExtractionControllerException(ExtractionControllerConstants.EXIT_CODE_USER_BREAK, "Extraction has been aborted!");
                            }
                            if (ioException.get() != null) {
                                throw ioException.get();
                            }
                            // Sum up bytes for control
                            getExtractionController().addProcessedBytesAndPauseIfNeeded(dataRead);
                            partRead += dataRead;
                            if (md5) {
                                // Update MD5
                                md.update(buffer, 0, dataRead);
                            }
                        } else if (dataRead < 0) {
                            throw new EOFException("EOF during merge");
                        }
                    }

                    // Check MD5 hashes
                    if (md5) {
                        final byte[] calculatedHash = md.digest();
                        final byte[] knownHash = hashes.get(archive.getArchiveFiles().get(partIndex));
                        if (knownHash != null && !Arrays.equals(knownHash, calculatedHash)) {
                            archive.addCrcError(archive.getArchiveFiles().get(partIndex));
                            throw new ExtractionControllerException(ExtractionControllerConstants.EXIT_CODE_CRC_ERROR);
                        }
                    }
                } finally {
                    try {
                        in.close();
                    } catch (final Throwable e) {
                    }
                }
            }
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_SUCCESS);
            return;
        } catch (ExtractionControllerException e) {
            setException(e);
            archive.setExitCode(e.getExitCode());
        } catch (IOException e) {
            setException(e);
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_INCOMPLETE_ERROR);
        } catch (Exception e) {
            setException(e);
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
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
    }

    @Override
    public boolean isAvailable(ExtractionExtension extractionExtension) {
        return true;
    }

    @Override
    public int getCrackProgress() {
        return 100;
    }

    @Override
    public boolean prepare() {
        final Archive archive = getExtractionController().getArchive();
        final ArchiveFile firstArchiveFile = archive.getArchiveFiles().get(0);
        outputFile = new File(firstArchiveFile.getFilePath().replaceFirst("\\.[\\d]+\\.xtm$", ""));
        FileInputStream in = null;
        try {
            in = new FileInputStream(firstArchiveFile.getFilePath());
            AWFCUtils awfc = new AWFCUtils(in);
            in.skip(40);// Skip useless bytes
            byte[] buffer = new byte[awfc.ensureRead()]; // original fileName length
            awfc.ensureRead(buffer.length, buffer); // original fileName
            in.skip(50 - buffer.length);// skip rest
            final String filename = new String(buffer, "UTF-8");
            // Set filename. If no filename was set, take the archivename
            if (filename != null && filename.trim().length() != 0) {
                outputFile = new File(outputFile.getAbsolutePath().replace(outputFile.getName(), filename));
            } else {
                logger.warning("Could not read from XtremSplit file " + outputFile.getAbsolutePath());
            }
            md5 = awfc.ensureRead() == 1 ? true : false;// MD5 Hashes are present
            buffer = new byte[4];
            awfc.ensureRead(4, buffer);// numberOfParts
            final int numberOfFiles = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getInt();
            buffer = new byte[8];
            awfc.ensureRead(8, buffer);// fileLength
            in.close();
            // fileLength = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getLong();//my xtm example seems broken
            if (numberOfFiles > archive.getArchiveFiles().size()) {
                logger.warning("Archive incomplete: " + numberOfFiles + "!=" + archive.getArchiveFiles().size());
                archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_INCOMPLETE_ERROR);
                return false;
            }
            if (md5) {
                // Get MD5 Hashes
                final File lastFile = new File(archive.getArchiveFiles().get(numberOfFiles - 1).getFilePath());
                final RandomAccessFile raf = IO.open(lastFile, "r");
                try {
                    awfc = new AWFCUtils(new InputStream() {

                        @Override
                        public int read() throws IOException {
                            return raf.read();
                        }

                        @Override
                        public int read(byte[] b, int off, int len) throws IOException {
                            return raf.read(b, off, len);
                        }
                    });
                    raf.seek(lastFile.length() - (32 * numberOfFiles));
                    buffer = new byte[32];
                    for (int i = 0; i < numberOfFiles; i++) {
                        awfc.ensureRead(32, buffer);
                        hashes.put(archive.getArchiveFiles().get(i), buffer);
                    }
                } finally {
                    if (raf != null) {
                        raf.close();
                    }
                }
            }
            return true;
        } catch (Exception e) {
            setException(e);
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
            return false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
            }
        }
    }

    public void close() {
        hashes.clear();
    }

    public DummyArchive checkComplete(Archive archive) throws CheckException {
        if (archive.getSplitType() == splitType) {
            try {
                final DummyArchive ret = new DummyArchive(archive, splitType.name());
                boolean hasMissingArchiveFiles = false;
                for (ArchiveFile archiveFile : archive.getArchiveFiles()) {
                    if (archiveFile instanceof MissingArchiveFile) {
                        hasMissingArchiveFiles = true;
                    }
                    ret.add(new DummyArchiveFile(archiveFile));
                }
                final ArchiveFile firstFile = archive.getArchiveFiles().get(0);
                if (hasMissingArchiveFiles == false && firstFile.exists()) {
                    final String firstArchiveFile = firstFile.getFilePath();
                    final String partNumberOfFirstArchiveFile = splitType.getPartNumberString(firstArchiveFile);
                    if (splitType.getFirstPartIndex() != splitType.getPartNumber(partNumberOfFirstArchiveFile)) {
                        throw new CheckException("Wrong firstArchiveFile(" + firstArchiveFile + ") for Archive(" + archive.getName() + ")");
                    }
                    InputStream is = null;
                    try {
                        is = new FileInputStream(firstArchiveFile);
                        AWFCUtils awfc = new AWFCUtils(is);
                        is.skip(40);// Skip useless bytes
                        byte[] buffer = new byte[awfc.ensureRead()];// original fileName length
                        awfc.ensureRead(buffer.length, buffer); // original fileName
                        is.skip(50 - buffer.length);// skip rest
                        awfc.ensureRead();// md5
                        buffer = new byte[4];
                        awfc.ensureRead(4, buffer); // numberOfParts
                        final int numberOfParts = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getInt();
                        is.close();
                        final List<ArchiveFile> missingArchiveFiles = SplitType.getMissingArchiveFiles(archive, splitType, numberOfParts);
                        if (missingArchiveFiles != null) {
                            for (ArchiveFile missingArchiveFile : missingArchiveFiles) {
                                ret.add(new DummyArchiveFile(missingArchiveFile));
                            }
                        }
                        if (ret.getSize() < numberOfParts) {
                            throw new CheckException("Missing archiveParts(" + numberOfParts + "!=" + ret.getSize() + ") for Archive(" + archive.getName() + ")");
                        } else if (ret.getSize() > numberOfParts) {
                            throw new CheckException("Too many archiveParts(" + numberOfParts + "!=" + ret.getSize() + ") for Archive(" + archive.getName() + ")");
                        }
                    } finally {
                        if (is != null) {
                            is.close();
                        }
                    }
                }
                return ret;
            } catch (CheckException e) {
                throw e;
            } catch (Throwable e) {
                throw new CheckException("Cannot check Archive(" + archive.getName() + ")", e);
            }
        }
        return null;
    }

    @Override
    public Boolean isSupported(final ArchiveFactory factory, final boolean allowDeepInspection) {
        if (splitType.matches(factory.getFilePath())) {
            if (allowDeepInspection) {
                try {
                    return SplitType.createArchive(factory, splitType, allowDeepInspection) != null;
                } catch (ArchiveException e) {
                    getLogger().log(e);
                }
            } else {
                return true;
            }
        }
        return false;
    }

}