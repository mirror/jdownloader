package org.jdownloader.extensions.extraction.multi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import jd.controlling.downloadcontroller.IfFileExistsDialogInterface;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.sf.sevenzipjbinding.SevenZipException;

import org.appwork.utils.Files;
import org.appwork.utils.Regex;
import org.appwork.utils.ReusableByteArrayOutputStream;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.DummyArchiveFile;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionControllerConstants;
import org.jdownloader.extensions.extraction.ExtractionException;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.IExtraction;
import org.jdownloader.extensions.extraction.Item;
import org.jdownloader.extensions.extraction.MissingArchiveFile;
import org.jdownloader.extensions.extraction.Signature;
import org.jdownloader.extensions.extraction.content.ContentView;
import org.jdownloader.extensions.extraction.content.PackedFile;
import org.jdownloader.extensions.extraction.gui.iffileexistsdialog.IfFileExistsDialog;
import org.jdownloader.settings.IfFileExistsAction;

public class Zip4J extends IExtraction {
    private volatile int               crack                   = 0;
    private ZipFile                    zipFile                 = null;
    private static final ArchiveType[] SUPPORTED_ARCHIVE_TYPES = new ArchiveType[] { ArchiveType.ZIP_MULTI2 };

    @Override
    public Archive buildArchive(ArchiveFactory link, boolean allowDeepInspection) throws ArchiveException {
        return ArchiveType.createArchive(link, allowDeepInspection, SUPPORTED_ARCHIVE_TYPES);
    }

    @Override
    public boolean findPassword(ExtractionController ctl, String password, boolean optimized) throws ExtractionException {
        final Archive archive = getExtractionController().getArchive();
        crack++;
        if (StringUtils.isEmpty(password)) {
            /* This should never happen */
            password = "";
        }
        final AtomicReference<Signature> passwordFound = new AtomicReference<Signature>(null);
        try {
            final List<?> items = zipFile.getFileHeaders();
            final HashSet<String> checkedExtensions = new HashSet<String>();
            final ReusableByteArrayOutputStream buffer = new ReusableByteArrayOutputStream(64 * 1024);
            final SignatureCheckingOutStream signatureOutStream = new SignatureCheckingOutStream(ctl, passwordFound, ctl.getFileSignatures(), buffer, getConfig().getMaxCheckedFileSizeDuringOptimizedPasswordFindingInBytes(), optimized);
            final byte[] readBuffer = new byte[32767];
            for (int index = 0; index < items.size(); index++) {
                final FileHeader item = (FileHeader) items.get(index);
                // Skip folders
                if (item == null || item.isDirectory() || !item.isEncrypted()) {
                    continue;
                } else if (ctl.gotKilled()) {
                    /* extraction got aborted */
                    break;
                } else if (passwordFound.get() != null) {
                    break;
                }
                final String path = item.getFileName();
                final String ext = Files.getExtension(path);
                if (checkedExtensions.add(ext) || !optimized) {
                    if (passwordFound.get() == null) {
                        try {
                            long remaining = item.getUncompressedSize();
                            signatureOutStream.reset();
                            signatureOutStream.setSignatureLength(path, remaining);
                            logger.fine("Validating password: " + path + "|" + password);
                            zipFile.setPassword(password);
                            final InputStream is = zipFile.getInputStream(item);
                            try {
                                while (passwordFound.get() == null) {
                                    final int read = is.read(readBuffer);
                                    if (read == -1) {
                                        break;
                                    } else {
                                        final int write = signatureOutStream.write(readBuffer, 0, read);
                                        if (write == 0) {
                                            break;
                                        } else {
                                            remaining -= write;
                                        }
                                    }
                                }
                            } finally {
                                is.close();
                            }
                            if (remaining == 0) {
                                passwordFound.set(new Signature("UNKNOWN:Extraction:OK", null, null, ext));
                            }
                        } catch (SevenZipException e) {
                            logger.log(e);
                        } catch (IOException e) {
                            if (!StringUtils.containsIgnoreCase(e.getMessage(), "Wrong Password")) {
                                throw e;
                            } else {
                                logger.log(e);
                            }
                        } finally {
                            if (passwordFound.get() != null) {
                                logger.info("Verified Password:" + password + "|" + path + "|" + passwordFound.get());
                            }
                        }
                    } else {
                        /* pw found */
                        break;
                    }
                }
            }
            return passwordFound.get() != null;
        } catch (Throwable e) {
            throw new ExtractionException(e, null);
        } finally {
            if (passwordFound.get() != null) {
                archive.setFinalPassword(password);
            }
        }
    }

    private final ExtractionExtension extension;

    public Zip4J(ExtractionExtension extension) {
        crack = 0;
        this.extension = extension;
    }

    public File getExtractFilePath(final FileHeader item, final ExtractionController ctrl, final AtomicBoolean skipped) throws MultiSevenZipException, ZipException {
        final Archive archive = getExtractionController().getArchive();
        String itemPath = item.getFileName();
        final ArchiveFile firstArchiveFile = archive.getArchiveFiles().get(0);
        if (isFiltered(itemPath)) {
            logger.info("Filtering item:" + itemPath + " from " + firstArchiveFile);
            skipped.set(true);
            return null;
        }
        final Long size = item.getUncompressedSize();
        if (true || CrossSystem.isWindows()) {
            // always alleviate the path and filename
            final String itemPathParts[] = itemPath.split(Regex.escape(File.separator));
            final StringBuilder sb = new StringBuilder();
            for (final String pathPartItem : itemPathParts) {
                if (sb.length() > 0) {
                    sb.append(File.separator);
                }
                sb.append(CrossSystem.alleviatePathParts(pathPartItem));
            }
            itemPath = sb.toString();
        }
        final String extractToRoot = getExtractionController().getExtractToFolder().getAbsoluteFile() + File.separator;
        File extractToFile = new File(extractToRoot + itemPath);
        logger.info("Extract " + extractToFile);
        if (extractToFile.exists()) {
            /* file already exists */
            IfFileExistsAction action = getExtractionController().getIfFileExistsAction();
            while (action == null || action == IfFileExistsAction.ASK_FOR_EACH_FILE) {
                if (ctrl.gotKilled()) {
                    throw new MultiSevenZipException("Extraction has been aborted", ExtractionControllerConstants.EXIT_CODE_USER_BREAK);
                }
                final IfFileExistsDialog dialog = new IfFileExistsDialog(extractToFile, new Item(itemPath, size, extractToFile), archive);
                final IfFileExistsDialogInterface dialogInterface = dialog.show();
                try {
                    dialogInterface.throwCloseExceptions();
                } catch (DialogNoAnswerException e) {
                    throw new ZipException(e);
                }
                action = dialogInterface.getAction();
                if (action == null) {
                    throw new ZipException("cannot handle if file exists: " + extractToFile);
                }
                if (action == IfFileExistsAction.AUTO_RENAME) {
                    if (dialogInterface == dialog) {
                        // only exists in gui version
                        final String newName = dialog.getNewName();
                        if (!StringUtils.equals(extractToFile.getName(), newName)) {
                            extractToFile = new File(extractToFile.getParentFile(), newName);
                            if (extractToFile.exists()) {
                                action = IfFileExistsAction.ASK_FOR_EACH_FILE;
                            } else {
                                action = null;
                                break;
                            }
                        }
                    }
                }
            }
            if (extractToFile.exists() && action != null) {
                switch (action) {
                case OVERWRITE_FILE:
                    if (!FileCreationManager.getInstance().delete(extractToFile, null)) {
                        throw new MultiSevenZipException("Could not overwrite(delete) " + extractToFile, ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
                    }
                    break;
                case SKIP_FILE:
                    /* skip file */
                    archive.addSkippedFiles(extractToFile);
                    skipped.set(true);
                    return null;
                case AUTO_RENAME:
                    final String splitName[] = CrossSystem.splitFileName(extractToFile.getName());
                    final String fileRoot = extractToFile.getParent();
                    final String extension;
                    if (StringUtils.isEmpty(splitName[1])) {
                        extension = "";
                    } else {
                        extension = "." + splitName[1];
                    }
                    long duplicateFilenameCounter = 2;
                    final String alreadyDuplicated = new Regex(splitName[0], ".*_(\\d+)$").getMatch(0);
                    final String sourceFileName;
                    if (alreadyDuplicated != null) {
                        /* it seems the file already got auto renamed! */
                        duplicateFilenameCounter = Long.parseLong(alreadyDuplicated) + 1;
                        sourceFileName = new Regex(splitName[0], "(.*)_\\d+$").getMatch(0);
                    } else {
                        sourceFileName = splitName[0];
                    }
                    while (true) {
                        final String newFileName = sourceFileName + "_" + (duplicateFilenameCounter++) + extension;
                        final File checkFileExists = new File(fileRoot, newFileName);
                        if (!checkFileExists.exists()) {
                            extractToFile = checkFileExists;
                            break;
                        }
                        if (ctrl.gotKilled()) {
                            throw new MultiSevenZipException("Extraction has been aborted", ExtractionControllerConstants.EXIT_CODE_USER_BREAK);
                        }
                    }
                    break;
                }
            }
        }
        if ((!extractToFile.getParentFile().exists() && !extractToFile.getParentFile().mkdirs())) {
            throw new MultiSevenZipException("could not create folder for file:" + extractToFile, ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
        }
        while (true) {
            try {
                if (!extractToFile.createNewFile()) {
                    throw new MultiSevenZipException("could not create file:" + extractToFile, ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
                }
                extractToFile.delete();
                return extractToFile;
            } catch (final IOException e) {
                logger.log(e);
                final File parent = extractToFile.getParentFile();
                final String brokenFilename = extractToFile.getName();
                final String fixedFilename = new String(brokenFilename.replaceAll("[^\\w\\s\\.\\(\\)\\[\\],]", ""));
                if (!StringUtils.equals(brokenFilename, fixedFilename)) {
                    /* Invalid Chars could have occured, try to remove them */
                    logger.severe("Invalid Chars could have occured, try to remove them");
                    logger.severe("Replaced " + brokenFilename + " with " + fixedFilename);
                    extractToFile = new File(parent, fixedFilename);
                    continue;
                }
                throw new MultiSevenZipException(e, ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
            }
        }
    }

    @Override
    public void extract(final ExtractionController ctrl) {
        final Archive archive = getExtractionController().getArchive();
        try {
            ctrl.setCompleteBytes(archive.getContentView().getTotalSize());
            ctrl.setProcessedBytes(0);
            if (zipFile.isEncrypted()) {
                final String pw = archive.getFinalPassword();
                zipFile.setPassword(pw);
            }
            final List<?> items = zipFile.getFileHeaders();
            byte[] readBuffer = new byte[32767];
            for (int index = 0; index < items.size(); index++) {
                final FileHeader item = (FileHeader) items.get(index);
                // Skip folders
                if (item == null || item.isDirectory()) {
                    continue;
                }
                if (ctrl.gotKilled()) {
                    throw new MultiSevenZipException("Extraction has been aborted", ExtractionControllerConstants.EXIT_CODE_USER_BREAK);
                }
                final AtomicBoolean skippedFlag = new AtomicBoolean(false);
                final Long size = item.getUncompressedSize();
                final File extractTo = getExtractFilePath(item, ctrl, skippedFlag);
                if (skippedFlag.get()) {
                    if (size != null && size >= 0) {
                        ctrl.addProcessedBytesAndPauseIfNeeded(size);
                    }
                    continue;
                } else if (extractTo == null) {
                    /* error */
                    throw new ZipException("Extraction error, extractTo == null");
                }
                final String itemPath = item.getFileName();
                ctrl.setCurrentActiveItem(new Item(itemPath, size, extractTo));
                try {
                    final FilesBytesCacheWriter call = new FilesBytesCacheWriter(extractTo, getExtractionController(), getConfig()) {
                        @Override
                        public int write(byte[] data, int length) throws SevenZipException {
                            if (ctrl.gotKilled()) {
                                throw new MultiSevenZipException("Extraction has been aborted", ExtractionControllerConstants.EXIT_CODE_USER_BREAK);
                            }
                            final int ret = super.write(data, length);
                            ctrl.addProcessedBytesAndPauseIfNeeded(ret);
                            return ret;
                        }
                    };
                    archive.addExtractedFiles(extractTo);
                    ZipInputStream is = null;
                    try {
                        is = zipFile.getInputStream(item);
                        if (is == null) {
                            throw new IOException("no InputStream for " + item.getFileName());
                        }
                        int readLen = -1;
                        // Loop until End of File and write the contents to the output stream
                        while ((readLen = is.read(readBuffer)) != -1) {
                            call.write(readBuffer, readLen);
                        }
                        is.close();
                        is = null;
                    } finally {
                        call.close();
                        if (is != null) {
                            is.close(true);
                        }
                    }
                } finally {
                    ctrl.setCurrentActiveItem(null);
                }
            }
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_SUCCESS);
        } catch (MultiSevenZipException e) {
            logger.log(e);
            setException(e);
            archive.setExitCode(e.getExitCode());
        } catch (SevenZipException e) {
            logger.log(e);
            setException(e);
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
        } catch (ZipException e) {
            logger.log(e);
            setException(e);
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
        } catch (IOException e) {
            logger.log(e);
            setException(e);
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
        }
    }

    @Override
    public boolean isAvailable(ExtractionExtension extractionExtension) {
        return true;
    }

    @Override
    public int getCrackProgress() {
        return crack;
    }

    @Override
    public boolean prepare() throws ExtractionException {
        final Archive archive = getExtractionController().getArchive();
        final ArchiveFile firstArchiveFile = archive.getArchiveFiles().get(0);
        try {
            zipFile = new ZipFile(firstArchiveFile.getFilePath());
            archive.setProtected(zipFile.isEncrypted());
            updateContentView(zipFile);
        } catch (Throwable e) {
            logger.log(e);
            throw new ExtractionException(e, firstArchiveFile);
        }
        return true;
    }

    private void updateContentView(final ZipFile zipFile) {
        final Archive archive = getExtractionController().getArchive();
        try {
            if (archive != null) {
                initFilters();
                final ContentView newView = new ContentView();
                final List<?> fileHeaders = zipFile.getFileHeaders();
                for (int index = 0; index < fileHeaders.size(); index++) {
                    final FileHeader fileHeader = (FileHeader) fileHeaders.get(index);
                    if (fileHeader != null) {
                        final String itemPath = fileHeader.getFileName();
                        if (StringUtils.isEmpty(itemPath) || isFiltered(itemPath)) {
                            continue;
                        }
                        newView.add(new PackedFile(fileHeader.isDirectory(), itemPath, fileHeader.getUncompressedSize()));
                    }
                }
                archive.setContentView(newView);
            }
        } catch (ZipException e) {
            logger.log(e);
        }
    }

    @Override
    public Boolean isSupported(ArchiveFactory factory, boolean allowDeepInspection) {
        if (allowDeepInspection) {
            try {
                return buildArchive(factory, allowDeepInspection) != null;
            } catch (ArchiveException e) {
                getLogger().log(e);
                return false;
            }
        } else {
            for (final ArchiveType archiveType : SUPPORTED_ARCHIVE_TYPES) {
                if (archiveType.matches(factory.getFilePath())) {
                    return null;
                }
            }
            return false;
        }
    }

    @Override
    public void close() {
    }

    @Override
    public DummyArchive checkComplete(Archive archive) throws CheckException {
        if (archive.getArchiveType() != null) {
            try {
                final DummyArchive ret = new DummyArchive(archive, archive.getArchiveType());
                boolean hasMissingArchiveFiles = false;
                for (ArchiveFile archiveFile : archive.getArchiveFiles()) {
                    if (archiveFile instanceof MissingArchiveFile) {
                        hasMissingArchiveFiles = true;
                    }
                    ret.add(new DummyArchiveFile(archiveFile));
                }
                if (hasMissingArchiveFiles == false) {
                    final ArchiveType archiveType = archive.getArchiveType();
                    final String firstArchiveFile = archive.getArchiveFiles().get(0).getFilePath();
                    final String partNumberOfFirstArchiveFile = archiveType.getPartNumberString(firstArchiveFile);
                    if (archiveType.getFirstPartIndex() != archiveType.getPartNumber(partNumberOfFirstArchiveFile)) {
                        throw new CheckException("Wrong firstArchiveFile(" + firstArchiveFile + ") for Archive(" + archive.getName() + ")");
                    }
                }
                if (ret.isComplete()) {
                    final ZipFile zipFile = new ZipFile(archive.getArchiveFiles().get(0).getFilePath());
                    final ArrayList<String> splitZipFiles = zipFile.getSplitZipFiles();
                    if (splitZipFiles != null) {
                        for (String splitZipFile : splitZipFiles) {
                            if (splitZipFile.endsWith("z010")) {
                                // workaround for bug in Zip4jUtil.getSplitZipFiles, if i>9 must be i>9
                                splitZipFile = splitZipFile.replaceFirst("z010$", "z10");
                            }
                            if (archive.getArchiveFileByPath(splitZipFile) == null) {
                                final File missingFile = new File(splitZipFile);
                                ret.add(new DummyArchiveFile(new MissingArchiveFile(missingFile.getName(), splitZipFile)));
                            }
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
}
