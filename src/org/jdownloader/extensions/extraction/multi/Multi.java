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
package org.jdownloader.extensions.extraction.multi;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.controlling.downloadcontroller.IfFileExistsDialogInterface;
import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IArchiveExtractCallback;
import net.sf.sevenzipjbinding.IArchiveOpenCallback;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

import org.appwork.utils.Application;
import org.appwork.utils.BinaryLogic;
import org.appwork.utils.Files;
import org.appwork.utils.Regex;
import org.appwork.utils.ReusableByteArrayOutputStream;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.CrossSystem.ARCHFamily;
import org.appwork.utils.os.CrossSystem.OperatingSystem;
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
import org.jdownloader.extensions.extraction.FileSignatures;
import org.jdownloader.extensions.extraction.IExtraction;
import org.jdownloader.extensions.extraction.Item;
import org.jdownloader.extensions.extraction.MissingArchiveFile;
import org.jdownloader.extensions.extraction.Signature;
import org.jdownloader.extensions.extraction.content.ContentView;
import org.jdownloader.extensions.extraction.content.PackedFile;
import org.jdownloader.extensions.extraction.gui.iffileexistsdialog.IfFileExistsDialog;
import org.jdownloader.settings.IfFileExistsAction;

public class Multi extends IExtraction {
    private volatile int               crack = 0;
    private SevenZipArchiveWrapper     inArchive;
    private IInStream                  inStream;
    private Closeable                  closable;
    private final static ArchiveType[] SUPPORTED_ARCHIVE_TYPES;
    static {
        final ArrayList<ArchiveType> archiveTypes = new ArrayList<ArchiveType>();
        for (final ArchiveType archiveType : ArchiveType.values()) {
            if (!archiveType.name().startsWith("ZIP_MULTI2")) {
                archiveTypes.add(archiveType);
            }
        }
        SUPPORTED_ARCHIVE_TYPES = archiveTypes.toArray(new ArchiveType[0]);
    }

    public Multi() {
        crack = 0;
        inArchive = null;
    }

    @Override
    public Archive buildArchive(ArchiveFactory link, boolean allowDeepInspection) throws ArchiveException {
        return ArchiveType.createArchive(link, allowDeepInspection, SUPPORTED_ARCHIVE_TYPES);
    }

    public static boolean checkRARSignature(File file) {
        try {
            final String sig = FileSignatures.readFileSignature(file);
            final Signature signature = new FileSignatures().getSignature(sig);
            if (signature != null) {
                if ("RAR".equalsIgnoreCase(signature.getId())) {
                    return true;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean check7ZSignature(File file) {
        try {
            final String sig = FileSignatures.readFileSignature(file);
            final Signature signature = new FileSignatures().getSignature(sig);
            if (signature != null) {
                if ("7Z".equalsIgnoreCase(signature.getId())) {
                    return true;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setPermissions(ISimpleInArchiveItem item, File extractTo) {
        if ((CrossSystem.isUnix() || CrossSystem.isMac())) {
            if (getConfig().isRestoreFilePermissions() && item != null && extractTo != null && extractTo.exists()) {
                try {
                    FilePermissionSet filePermissionSet = null;
                    final Integer attributesInteger = item.getAttributes();
                    final String hostOS = item.getHostOS();
                    if (attributesInteger != null) {
                        final int attributes = attributesInteger.intValue();
                        if (StringUtils.equalsIgnoreCase("Unix", hostOS) && attributes != 0) {
                            filePermissionSet = new FilePermissionSet();
                            int attributeIndex = 16;
                            filePermissionSet.setOtherExecute((attributes & 1 << attributeIndex++) != 0);
                            filePermissionSet.setOtherWrite((attributes & 1 << attributeIndex++) != 0);
                            filePermissionSet.setOtherRead((attributes & 1 << attributeIndex++) != 0);
                            filePermissionSet.setGroupExecute((attributes & 1 << attributeIndex++) != 0);
                            filePermissionSet.setGroupWrite((attributes & 1 << attributeIndex++) != 0);
                            filePermissionSet.setGroupRead((attributes & 1 << attributeIndex++) != 0);
                            filePermissionSet.setUserExecute((attributes & 1 << attributeIndex++) != 0);
                            filePermissionSet.setUserWrite((attributes & 1 << attributeIndex++) != 0);
                            filePermissionSet.setUserRead((attributes & 1 << attributeIndex++) != 0);
                        }
                    }
                    if (filePermissionSet != null) {
                        if (Application.getJavaVersion() >= Application.JAVA17) {
                            FilePermission17.setFilePermission(extractTo, filePermissionSet);
                        } else {
                            if (filePermissionSet.isUserExecute()) {
                                if (!extractTo.setExecutable(true, filePermissionSet.isOtherExecute() == false && filePermissionSet.isOtherExecute() == false)) {
                                    throw new IOException("Failed to set " + filePermissionSet + " to " + extractTo);
                                }
                            }
                        }
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                }
            }
        }
    }

    public void setLastModifiedDate(ISimpleInArchiveItem item, File extractTo) {
        // Set last write time
        try {
            final long modified;
            if (getConfig().isUseOriginalFileDate()) {
                final Date date = item.getLastWriteTime();
                if (date != null && date.getTime() >= 0) {
                    modified = date.getTime();
                } else {
                    modified = -1;
                }
            } else {
                modified = System.currentTimeMillis();
            }
            if (modified > 0 && !extractTo.setLastModified(modified)) {
                logger.warning("Could not set last write/modified time for " + item.getPath());
                return;
            }
        } catch (final Throwable e) {
            logger.log(e);
        }
    }

    private boolean initLibrary(final String libID) {
        File tmp = null;
        try {
            logger.finer("Try Lib ID: " + libID);
            tmp = Application.getTempResource("7zip");
            try {
                org.appwork.utils.Files.deleteRecursiv(tmp);
            } catch (final Throwable e) {
            }
            logger.finer("Try Lib Path: " + tmp + "->" + (tmp.exists() || tmp.mkdirs()));
            SevenZip.initSevenZipFromPlatformJAR(libID, tmp);
        } catch (Throwable e) {
            if (e instanceof UnsatisfiedLinkError && CrossSystem.isWindows()) {
                try {
                    /* workaround for sevenzipjbinding, missing dll imports */
                    String path = new Regex(e.getMessage(), "(.:.*?\\.dll)").getMatch(0);
                    logger.severe("Unsatisfied path " + path);
                    File root = new File(path).getParentFile();
                    System.load(new File(root, "mingwm10.dll").toString());
                    System.load(new File(root, "libgcc_s_dw2-1.dll").toString());
                    System.load(new File(root, "libstdc++-6.dll").toString());
                    SevenZip.initSevenZipFromPlatformJAR(libID, tmp);
                    if (SevenZip.isInitializedSuccessfully()) {
                        return true;
                    }
                } catch (final Throwable e2) {
                    e2.printStackTrace();
                }
            }
            try {
                org.appwork.utils.Files.deleteRecursiv(tmp);
            } catch (final Throwable e1) {
            }
            logger.warning("Could not initialize Multiunpacker:#1");
            logger.log(e);
            try {
                final String s2 = System.getProperty("java.io.tmpdir");
                tmp = new File(s2);
                logger.finer("Try Lib Path: " + tmp + "->" + tmp.exists());
                SevenZip.initSevenZipFromPlatformJAR(tmp);
            } catch (Throwable e2) {
                logger.warning("Could not initialize Multiunpacker:#2");
                logger.log(e2);
                return false;
            }
        }
        return SevenZip.isInitializedSuccessfully();
    }

    private boolean checkLibraries(final ExtractionExtension extractionExtension, final Set<String> libIDs) {
        logger.finer("Try Lib IDs: " + libIDs);
        for (final String libID : libIDs) {
            if (initLibrary(libID)) {
                extractionExtension.getSettings().setLastWorkingLibID(libID);
                return true;
            }
        }
        return false;
    }

    private Set<String> filter(Set<String> values) {
        final Set<String> ret = new LinkedHashSet<String>();
        for (final String value : values) {
            final OperatingSystem os = CrossSystem.getOS();
            switch (os.getFamily()) {
            case MAC:
                if (!StringUtils.startsWithCaseInsensitive(value, "Mac-")) {
                    continue;
                }
                break;
            case LINUX:
                if (!StringUtils.startsWithCaseInsensitive(value, "Linux-")) {
                    continue;
                }
                break;
            case WINDOWS:
                if (!StringUtils.startsWithCaseInsensitive(value, "Windows-")) {
                    continue;
                }
                break;
            default:
                break;
            }
            ret.add(value);
        }
        return ret;
    }

    private boolean hasLibrarySupport(final ExtractionExtension extractionExtension) {
        final String customLibID = System.getProperty("sevenzipLibID");
        if (StringUtils.isNotEmpty(customLibID)) {
            if (initLibrary(customLibID)) {
                if (CrossSystem.isLinux() && ARCHFamily.ARM.equals(CrossSystem.getARCHFamily())) {
                    extractionExtension.getSettings().setLastWorkingLibID(customLibID);
                }
                return true;
            }
            return false;
        } else {
            final Set<String> libIDs = new LinkedHashSet<String>();
            final String lastWorkingLibID = extractionExtension.getSettings().getLastWorkingLibID();
            if (StringUtils.isNotEmpty(lastWorkingLibID)) {
                libIDs.add(lastWorkingLibID);
                extractionExtension.getSettings().setLastWorkingLibID(null);
                extractionExtension.getSettings()._getStorageHandler().write();
            }
            final OperatingSystem os = CrossSystem.getOS();
            switch (os.getFamily()) {
            case BSD:
                switch (CrossSystem.getARCHFamily()) {
                case X86:
                    switch (os) {
                    case DRAGONFLYBSD:
                        if (Application.is64BitJvm()) {
                            libIDs.add("DragonFlyBSD-amd64");
                        }
                        break;
                    case FREEBSD:
                        if (Application.is64BitJvm()) {
                            libIDs.add("FreeBSD-amd64");
                            libIDs.remove("FreeBSD-i386");
                            // libIDs.add("FreeBSD-amd64-2");// untested
                        } else {
                            libIDs.add("FreeBSD-i386");
                            libIDs.remove("FreeBSD-amd64");
                        }
                        break;
                    default:
                        // Not supported
                        break;
                    }
                    break;
                default:
                    // Not supported
                    break;
                }
                break;
            case LINUX:
                switch (CrossSystem.getARCHFamily()) {
                case ARM:
                    if (Application.is64BitJvm()) {
                        libIDs.add("Linux-aarch64");
                    } else {
                        if (CrossSystem.isRaspberryPi()) {
                            libIDs.add("Linux-armpi");
                            libIDs.add("Linux-armpi2");
                        } else {
                            libIDs.add("Linux-arm2");
                            libIDs.add("Linux-arm");
                            libIDs.add("Linux-arm3");
                        }
                    }
                    break;
                case X86:
                    if (Application.is64BitJvm()) {
                        libIDs.add("Linux-amd64");
                        libIDs.remove("Linux-i386");
                    } else {
                        libIDs.add("Linux-i386");
                        libIDs.remove("Linux-amd64");
                    }
                    break;
                case PPC:
                    libIDs.add("Linux-ppc");
                    break;
                default:
                    // Not supported
                    break;
                }
                break;
            case MAC:
                if (Application.is64BitJvm()) {
                    libIDs.add("Mac-x86_64");
                    libIDs.remove("Mac-i386");
                } else {
                    libIDs.add("Mac-i386");
                    libIDs.remove("Mac-x86_64");
                }
                break;
            case WINDOWS:
                if (Application.is64BitJvm()) {
                    libIDs.add("Windows-amd64");
                    libIDs.remove("Windows-x86");
                } else {
                    libIDs.add("Windows-x86");
                    libIDs.remove("Windows-amd64");
                }
                break;
            default:
                // Not supported
                break;
            }
            return checkLibraries(extractionExtension, filter(libIDs));
        }
    }

    public static final String getSevenZipJBindingVersion() {
        try {
            final Method method = SevenZip.class.getMethod("getSevenZipJBindingVersion");
            return (String) method.invoke(null, new Object[0]);
        } catch (Throwable e) {
            return "4.65";
        }
    }

    public static boolean isRAR5Supported() {
        try {
            return ArchiveFormat.valueOf("RAR5") != null;
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    public boolean isAvailable(ExtractionExtension extractionExtension) {
        final boolean ret = hasLibrarySupport(extractionExtension);
        if (!ret) {
            logger.info("Unsupported SevenZipJBinding|CPU_ARCH=" + CrossSystem.getARCHFamily() + "|OS_FAM=" + CrossSystem.getOSFamily() + "|OS=" + CrossSystem.getOS() + "|64Bit_JVM=" + Application.is64BitJvm() + "|64Bit_ARCH=" + CrossSystem.is64BitArch());
        } else {
            logger.info("Supported SevenZipJBinding|Version=" + getSevenZipJBindingVersion() + "|RAR5=" + isRAR5Supported() + "|CPU_ARCH=" + CrossSystem.getARCHFamily() + "|OS_FAM=" + CrossSystem.getOSFamily() + "|OS=" + CrossSystem.getOS() + "|64Bit_JVM=" + Application.is64BitJvm() + "|64Bit_ARCH=" + CrossSystem.is64BitArch());
        }
        return ret;
    }

    @Override
    public DummyArchive checkComplete(Archive archive) throws CheckException {
        if (archive.getArchiveType() != null) {
            try {
                final DummyArchive ret = new DummyArchive(archive, archive.getArchiveType().name());
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
    public void close() {
        try {
            inArchive.close();
        } catch (final Throwable e) {
        }
        try {
            closable.close();
        } catch (final Throwable e) {
        }
    }

    @Override
    public void extract(final ExtractionController ctrl) {
        final Archive archive = getExtractionController().getArchive();
        try {
            final ArchiveFormat format = archive.getArchiveFormat();
            ctrl.setCompleteBytes(archive.getContentView().getTotalSize());
            ctrl.setProcessedBytes(0);
            final boolean isSolid = Boolean.TRUE.equals(inArchive.getArchiveProperty(PropID.SOLID));// solid archives are much much much
            // faster in stream extraction to avoid
            // seeking/extraction of previous data
            if (ArchiveFormat.SEVEN_ZIP == format || ArchiveFormat.BZIP2 == format || isSolid) {
                final ArrayList<Integer> allItems = new ArrayList<Integer>();
                for (int i = 0; i < inArchive.getNumberOfItems(); i++) {
                    final Boolean isFolder = inArchive.isFolder(i);
                    if (Boolean.TRUE.equals(isFolder)) {
                        continue;
                    }
                    allItems.add(i);
                }
                final int maxItemsRound = isSolid ? -1 : 50;
                final ArrayList<Integer> round = new ArrayList<Integer>();
                final Iterator<Integer> it = allItems.iterator();
                while (it.hasNext()) {
                    final Integer next = it.next();
                    round.add(next);
                    if (round.size() == maxItemsRound || it.hasNext() == false) {
                        final int[] items = new int[round.size()];
                        int index = 0;
                        for (Integer item : round) {
                            items[index++] = item;
                        }
                        round.clear();
                        final Seven7ExtractCallback callback = new Seven7ExtractCallback(this, inArchive, ctrl, archive, getConfig());
                        try {
                            inArchive.extract(items, false, callback);
                        } catch (SevenZipException e) {
                            logger.log(e);
                            throw e;
                        } finally {
                            callback.close();
                            ctrl.setCurrentActiveItem(null);
                        }
                        if (ctrl.gotKilled()) {
                            throw new MultiSevenZipException("Extraction has been aborted", ExtractionControllerConstants.EXIT_CODE_USER_BREAK);
                        }
                        if (callback.hasError()) {
                            throw new SevenZipException("callback encountered an error!", callback.getError());
                        }
                        if (callback.isResultMissing()) {
                            throw new SevenZipException("callback is missing results!");
                        }
                    }
                }
            } else {
                for (final ISimpleInArchiveItem item : inArchive.getSimpleInterface().getArchiveItems()) {
                    // Skip folders
                    if (item == null || item.isFolder()) {
                        continue;
                    }
                    if (ctrl.gotKilled()) {
                        throw new MultiSevenZipException("Extraction has been aborted", ExtractionControllerConstants.EXIT_CODE_USER_BREAK);
                    }
                    final AtomicBoolean skippedFlag = new AtomicBoolean(false);
                    final Long size = item.getSize();
                    final File extractTo = getExtractFilePath(item, ctrl, skippedFlag);
                    if (skippedFlag.get()) {
                        if (size != null && size >= 0) {
                            ctrl.addAndGetProcessedBytes(size);
                        }
                        continue;
                    } else if (extractTo == null) {
                        /* error */
                        throw new SevenZipException("Extraction error, extractTo == null");
                    }
                    ctrl.setCurrentActiveItem(new Item(item.getPath(), size, extractTo));
                    try {
                        final MultiCallback call = new MultiCallback(extractTo, getExtractionController(), getConfig()) {
                            @Override
                            public int write(final byte[] data) throws SevenZipException {
                                if (ctrl.gotKilled()) {
                                    throw new MultiSevenZipException("Extraction has been aborted", ExtractionControllerConstants.EXIT_CODE_USER_BREAK);
                                }
                                final int ret = super.write(data);
                                ctrl.addAndGetProcessedBytes(ret);
                                return ret;
                            }
                        };
                        archive.addExtractedFiles(extractTo);
                        final ExtractOperationResult res;
                        try {
                            if (item.isEncrypted()) {
                                String pw = archive.getFinalPassword();
                                if (pw == null) {
                                    throw new IOException("Password is null!");
                                }
                                res = item.extractSlow(call, pw);
                            } else {
                                res = item.extractSlow(call);
                            }
                        } finally {
                            /* always close files, thats why its best in finally branch */
                            call.close();
                        }
                        logger.info("Extracted:" + item.getPath() + "|BytesWritten:" + call.getWritten() + "|FileSize(OnDisk):" + extractTo.length() + "|FileSize(InArchive):" + size + "|Result:" + res);
                        setLastModifiedDate(item, extractTo);
                        setPermissions(item, extractTo);
                        switch (res) {
                        case OK:
                            if (size != null && size < extractTo.length()) {
                                if (extractTo.length() < size) {
                                    logger.info("Size missmatch for " + item.getPath() + "(" + extractTo.length() + "<" + size + ")");
                                    archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_INCOMPLETE_ERROR);
                                    return;
                                }
                                if (extractTo.length() > size) {
                                    logger.info("Ignore Size missmatch for " + item.getPath() + "(" + extractTo.length() + ">" + size + ")");
                                }
                            }
                            /* extraction successfully ,continue with next file */
                            break;
                        case CRCERROR:
                            logger.info("CRC Error for " + item.getPath());
                            writeCrashLog("CRC Error in " + item.getPath());
                            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CRC_ERROR);
                            return;
                        case UNSUPPORTEDMETHOD:
                            logger.info("Unsupported Method " + item.getMethod() + " in " + item.getPath());
                            writeCrashLog("Unsupported Method " + item.getMethod() + " in " + item.getPath());
                            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
                            return;
                        default:
                            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
                            return;
                        }
                    } finally {
                        ctrl.setCurrentActiveItem(null);
                    }
                }
            }
        } catch (MultiSevenZipException e) {
            logger.log(e);
            setException(e);
            archive.setExitCode(e.getExitCode());
            return;
        } catch (SevenZipException e) {
            logger.log(e);
            setException(e);
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
            return;
        } catch (IOException e) {
            logger.log(e);
            setException(e);
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
            return;
        }
        archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_SUCCESS);
    }

    public File getExtractFilePath(final ISimpleInArchiveItem item, final ExtractionController ctrl, final AtomicBoolean skipped) throws SevenZipException {
        final Archive archive = getExtractionController().getArchive();
        String itemPath = item.getPath();
        final ArchiveFile firstArchiveFile = archive.getArchiveFiles().get(0);
        if (StringUtils.isEmpty(itemPath)) {
            // example: test.tar.gz / test.tgz contains a test.tar file, that has
            // NO name. we create a dummy name here.
            final String firstPartFileName = firstArchiveFile.getName();
            final int in = firstPartFileName.lastIndexOf(".");
            final String newItemPath;
            if (in > 0) {
                newItemPath = firstPartFileName.substring(0, in);
            } else {
                if (StringUtils.isNotEmpty(firstPartFileName)) {
                    newItemPath = firstPartFileName;
                } else {
                    newItemPath = "UnknownExtractionFilename";
                }
            }
            if (ArchiveType.TGZ_SINGLE.equals(archive.getArchiveType()) && !StringUtils.endsWithCaseInsensitive(newItemPath, ".tar")) {
                itemPath = newItemPath + ".tar";
            } else {
                itemPath = newItemPath;
            }
        }
        if (isFiltered(itemPath)) {
            logger.info("Filtering item:" + itemPath + " from " + firstArchiveFile);
            skipped.set(true);
            return null;
        }
        final Long size = item.getSize();
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
                    throw new SevenZipException(e);
                }
                action = dialogInterface.getAction();
                if (action == null) {
                    throw new SevenZipException("cannot handle if file exists: " + extractToFile);
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
                            throw new SevenZipException("Extraction has been aborted");
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

    private SevenZipArchiveWrapper createSevenZipArchiveWrapper(ArchiveFormat format, IInStream inStream, IArchiveOpenCallback callBackformat) throws SevenZipException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        try {
            final Method createArchive = SevenZip.class.getMethod("openInArchive", new Class<?>[] { ArchiveFormat.class, IInStream.class, IArchiveOpenCallback.class });
            final Object archive = createArchive.invoke(null, new Object[] { format, inStream, callBackformat });
            final Method getArchiveFormat = archive.getClass().getMethod("getArchiveFormat");
            final Method getNumberOfItems = archive.getClass().getMethod("getNumberOfItems");
            final Method getArchiveProperty = archive.getClass().getMethod("getArchiveProperty", new Class[] { PropID.class });
            final Method getProperty = archive.getClass().getMethod("getProperty", new Class[] { int.class, PropID.class });
            final Method getSimpleInterface = archive.getClass().getMethod("getSimpleInterface");
            final Method close = archive.getClass().getMethod("close");
            final Object propIDLastWriteTime;
            PropID propID = null;
            try {
                // older sevenzipjbinding
                propID = PropID.valueOf("LAST_WRITE_TIME");
            } catch (final IllegalArgumentException e) {
                try {
                    // new sevenzipjbinding
                    propID = PropID.valueOf("LAST_MODIFICATION_TIME");
                } catch (final IllegalArgumentException e2) {
                }
            }
            propIDLastWriteTime = propID;
            final boolean slowDownWorkaroundNeeded = propID != null && !"LAST_MODIFICATION_TIME".equals(propID.name());
            final Method extract = archive.getClass().getMethod("extract", new Class[] { int[].class, boolean.class, IArchiveExtractCallback.class });
            return new SevenZipArchiveWrapper() {
                private final AtomicBoolean closedFlag = new AtomicBoolean(false);

                @Override
                public int getNumberOfItems() {
                    final Number ret = (Number) invoke(getNumberOfItems);
                    if (ret == null) {
                        return 0;
                    } else {
                        return ret.intValue();
                    }
                }

                @Override
                public ArchiveFormat getArchiveFormat() {
                    return (ArchiveFormat) invoke(getArchiveFormat);
                }

                @Override
                public Boolean isEncrypted(int index) {
                    return (Boolean) invoke(getProperty, index, PropID.ENCRYPTED);
                }

                protected Object invoke(Method method, Object... args) {
                    try {
                        return method.invoke(archive, args);
                    } catch (IllegalAccessException e) {
                        logger.log(e);
                    } catch (IllegalArgumentException e) {
                        logger.log(e);
                    } catch (InvocationTargetException e) {
                        logger.log(e);
                    }
                    return null;
                }

                @Override
                public Boolean isFolder(int index) {
                    return (Boolean) invoke(getProperty, index, PropID.IS_FOLDER);
                }

                @Override
                public Long getSize(int index) {
                    final Number ret = (Number) invoke(getProperty, index, PropID.SIZE);
                    if (ret != null) {
                        return ret.longValue();
                    } else {
                        return null;
                    }
                }

                @Override
                public Long getPackedSize(int index) {
                    final Number ret = (Number) invoke(getProperty, index, PropID.PACKED_SIZE);
                    if (ret != null) {
                        return ret.longValue();
                    } else {
                        return null;
                    }
                }

                @Override
                public String getPath(int index) {
                    return (String) invoke(getProperty, index, PropID.PATH);
                }

                @Override
                public String getMethod(int index) {
                    return (String) invoke(getProperty, index, PropID.METHOD);
                }

                @Override
                public Integer getAttributes(int index) {
                    final Number ret = (Number) invoke(getProperty, index, PropID.ATTRIBUTES);
                    if (ret != null) {
                        return ret.intValue();
                    } else {
                        return 0;
                    }
                }

                @Override
                public Date getLastWriteTime(int index) {
                    if (propIDLastWriteTime != null) {
                        return (Date) invoke(getProperty, index, propIDLastWriteTime);
                    } else {
                        return null;
                    }
                }

                @Override
                public void close() throws SevenZipException {
                    if (closedFlag.compareAndSet(false, true)) {
                        // it is important to call close only once, because new sevenzipjbinding will crash on second close call
                        try {
                            if (archive instanceof Closeable) {
                                ((Closeable) archive).close();
                            } else {
                                invoke(close);
                            }
                        } catch (IOException e) {
                            throw new SevenZipException(e);
                        }
                    }
                }

                @Override
                public void extract(int[] indices, boolean testMode, IArchiveExtractCallback extractCallback) throws SevenZipException {
                    invoke(extract, indices, testMode, extractCallback);
                }

                @Override
                public ISimpleInArchive getSimpleInterface() {
                    return (ISimpleInArchive) invoke(getSimpleInterface);
                }

                @Override
                public boolean isSlowDownWorkaroundNeeded() {
                    return slowDownWorkaroundNeeded && ArchiveFormat.SEVEN_ZIP == getArchiveFormat();
                }

                @Override
                public Object getArchiveProperty(PropID propID) {
                    return invoke(getArchiveProperty, propID);
                }
            };
        } catch (NoSuchMethodException e) {
            throw new SevenZipException(e);
        }
    }

    @Override
    public boolean findPassword(final ExtractionController ctl, String password, boolean optimized) throws ExtractionException {
        final Archive archive = getExtractionController().getArchive();
        crack++;
        if (StringUtils.isEmpty(password)) {
            /* This should never happen */
            password = "";
        }
        final AtomicBoolean passwordfound = new AtomicBoolean(false);
        try {
            final ArchiveFile firstArchiveFile = archive.getArchiveFiles().get(0);
            final ArchiveFormat format = archive.getArchiveFormat();
            final ReusableByteArrayOutputStream buffer = new ReusableByteArrayOutputStream(64 * 1024);
            try {
                if (inArchive != null) {
                    inArchive.close();
                    inArchive = null;
                }
            } catch (Throwable e) {
            }
            try {
                if (closable != null) {
                    closable.close();
                    closable = null;
                }
            } catch (final Throwable e) {
            }
            final IArchiveOpenCallback callBack;
            if (archive.getArchiveFiles().size() == 1) {
                final RandomAccessFile raf = new RandomAccessFile(firstArchiveFile.getFilePath(), "r");
                closable = raf;
                callBack = new DummyOpener(password);
                inStream = new RandomAccessFileInStream(raf);
            } else {
                switch (archive.getArchiveType()) {
                case RAR_MULTI:
                    final RarOpener rarOpener = new RarOpener(archive, password, logger);
                    closable = rarOpener;
                    callBack = rarOpener;
                    inStream = rarOpener.getStream(firstArchiveFile);
                    break;
                case SEVENZIP_PARTS:
                    final MultiOpener sevenZipPartsOpener = new MultiOpener(archive, password, getLogger());
                    closable = sevenZipPartsOpener;
                    callBack = sevenZipPartsOpener;
                    inStream = new ModdedVolumedArchiveInStream(firstArchiveFile.getFilePath(), sevenZipPartsOpener);
                    break;
                default:
                    final MultiOpener multiOpener = new MultiOpener(archive, password, getLogger());
                    closable = multiOpener;
                    callBack = multiOpener;
                    inStream = multiOpener.getStream(firstArchiveFile);
                    break;
                }
            }
            if (inStream == null) {
                logger.info("Failed to open Stream: " + firstArchiveFile);
            }
            if (inStream != null && closable != null) {
                try {
                    inArchive = createSevenZipArchiveWrapper(format, inStream, callBack);
                } catch (InvocationTargetException e) {
                    if (e.getTargetException() != null) {
                        throw e.getTargetException();
                    } else {
                        throw e;
                    }
                }
            } else {
                return false;
            }
            final HashSet<String> checkedExtensions = new HashSet<String>();
            if (ArchiveFormat.SEVEN_ZIP == format) {
                final int numberOfItems = inArchive.getNumberOfItems();
                if (archive.isPasswordRequiredToOpen() && numberOfItems > 0) {
                    // archive is open. password seems to be ok.
                    passwordfound.set(true);
                    return true;
                }
                final ArrayList<Integer> allItems = new ArrayList<Integer>();
                for (int i = 0; i < numberOfItems; i++) {
                    final Boolean isFolder = inArchive.isFolder(i);
                    final Boolean itemEncrypted = inArchive.isEncrypted(i);
                    final Long size = inArchive.getSize(i);
                    final Long packedSize = inArchive.getPackedSize(i);
                    if (!itemEncrypted || isFolder || size == null || (size == 0 && (packedSize == null || packedSize == 0))) {
                        /*
                         * we also check for items with size ==0, they should have a packedsize>0
                         */
                        continue;
                    }
                    allItems.add(i);
                }
                final int[] items = new int[allItems.size()];
                int index = 0;
                for (final Integer item : allItems) {
                    items[index++] = item;
                }
                try {
                    inArchive.extract(items, false, new Seven7PWCallback(ctl, inArchive, passwordfound, password, buffer, getConfig().getMaxCheckedFileSizeDuringOptimizedPasswordFindingInBytes(), ctl.getFileSignatures(), optimized));
                } catch (SevenZipException e) {
                    e.printStackTrace();
                    // An error will be thrown if the write method
                    // returns
                    // 0.
                }
            } else {
                final SignatureCheckingOutStream signatureOutStream = new SignatureCheckingOutStream(ctl, passwordfound, ctl.getFileSignatures(), buffer, getConfig().getMaxCheckedFileSizeDuringOptimizedPasswordFindingInBytes(), optimized);
                final ISimpleInArchiveItem[] items = inArchive.getSimpleInterface().getArchiveItems();
                // we found some rar archives, that throw an exception when we try to open it with no or an invalid password, but do not
                // throw any exceptions if we use - for example - their archive name as password.
                // in this case, the archive opens fine, but does not show any contents. let's catch this case here
                if (archive.isPasswordRequiredToOpen() && items != null && items.length > 0) {
                    // archive is open. password seems to be ok.
                    passwordfound.set(true);
                    return true;
                }
                for (final ISimpleInArchiveItem item : items) {
                    final Long size = item.getSize();
                    final Long packedSize = item.getPackedSize();
                    final boolean isEncrypted = item.isEncrypted();
                    if (isEncrypted == false || item.isFolder() || size == null || (size == 0 && (packedSize == null || packedSize == 0))) {
                        /*
                         * we also check for items with size ==0, they should have a packedsize>0
                         */
                        continue;
                    }
                    if (ctl.gotKilled()) {
                        /* extraction got aborted */
                        break;
                    } else if (passwordfound.get()) {
                        break;
                    }
                    final String path = item.getPath();
                    final String ext = Files.getExtension(path);
                    if (checkedExtensions.add(ext) || !optimized) {
                        if (!passwordfound.get()) {
                            try {
                                signatureOutStream.reset();
                                signatureOutStream.setSignatureLength(path, size);
                                logger.fine("Validating password: " + path);
                                final ExtractOperationResult result = item.extractSlow(signatureOutStream, password);
                                logger.fine("Validation result: " + path + "|" + result);
                                if (ExtractOperationResult.DATAERROR.equals(result)) {
                                    /*
                                     * 100% wrong password, DO NOT CONTINUE as unrar already might have cleaned up (nullpointer in native ->
                                     * crash jvm)
                                     */
                                    return false;
                                }
                                if (ExtractOperationResult.OK.equals(result)) {
                                    passwordfound.set(true);
                                }
                            } catch (SevenZipException e) {
                                e.printStackTrace();
                                // An error will be thrown if the write method
                                // returns
                                // 0.
                            }
                        } else {
                            /* pw found */
                            break;
                        }
                    }
                    // if (filter(item.getPath())) continue;
                }
            }
            return passwordfound.get();
        } catch (SevenZipException e) {
            // this happens if the archive has encrypted filenames as well and thus needs a password to open it
            if (e.getMessage().contains("HRESULT: 0x80004005") || e.getMessage().contains("HRESULT: 0x1 (FALSE)") || e.getMessage().contains("can't be opened") || e.getMessage().contains("No password was provided")) {
                /* password required */
                logger.info("SevenZipException: " + e.getMessage());
                archive.setProtected(true);
                archive.setPasswordRequiredToOpen(true);
                return false;
            }
            throw new ExtractionException(e, null);
        } catch (Throwable e) {
            throw new ExtractionException(e, null);
        } finally {
            if (passwordfound.get()) {
                archive.setFinalPassword(password);
                if (inArchive != null) {
                    updateContentView(inArchive.getSimpleInterface());
                }
            }
        }
    }

    @Override
    public int getCrackProgress() {
        return crack;
    }

    @Override
    public boolean prepare() throws ExtractionException {
        final Archive archive = getExtractionController().getArchive();
        try {
            final ArchiveFile firstArchiveFile = archive.getArchiveFiles().get(0);
            final ArchiveFormat format = archive.getArchiveFormat();
            if (format.name().startsWith("RAR")) {
                try {
                    final String signatureString = FileSignatures.readFileSignature(new File(firstArchiveFile.getFilePath()), 14);
                    if (signatureString.length() >= 16 && StringUtils.startsWithCaseInsensitive(signatureString, "526172211a070100")) {
                        /* check for rar5 */
                        if (isRAR5Supported()) {
                            logger.severe("RAR5 is supported:" + signatureString);
                        } else {
                            logger.severe("RAR5 is not supported:" + signatureString);
                            return false;
                        }
                    }
                    if (signatureString.length() >= 24) {
                        /*
                         * 0x0001 Volume attribute (archive volume)
                         *
                         * 0x0002 Archive comment present RAR 3.x uses the separate comment block and does not set this flag.
                         *
                         * 0x0004 Archive lock attribute
                         *
                         * 0x0008 Solid attribute (solid archive)
                         *
                         * 0x0010 New volume naming scheme ('volname.partN.rar')
                         *
                         * 0x0020 Authenticity information present RAR 3.x does not set this flag.
                         *
                         * 0x0040 Recovery record present
                         *
                         * 0x0080 Block headers are encrypted
                         */
                        final String headerBitFlags1 = "" + signatureString.charAt(20) + signatureString.charAt(21);
                        /*
                         * 0x0100 FIRST Volume
                         *
                         * 0x0200 EncryptedVerion
                         */
                        // final String headerBitFlags2 = "" + signatureString.charAt(22) + signatureString.charAt(23);
                        final int headerByte1 = Integer.parseInt(headerBitFlags1, 16);
                        // final int headerByte2 = Integer.parseInt(headerBitFlags2, 16);
                        if (BinaryLogic.containsAll(headerByte1, 1 << 7) && !format.name().startsWith("RAR5")) {
                            // TODO: RAR5 encryption detection
                            logger.severe("Encrypted RAR headers:" + signatureString);
                            archive.setProtected(true);
                            archive.setPasswordRequiredToOpen(true);
                            return true;
                        }
                    }
                } catch (IOException e) {
                    logger.log(e);
                }
            }
            try {
                final String sig = FileSignatures.readFileSignature(new File(firstArchiveFile.getFilePath()));
                final Signature signature = new FileSignatures().getSignature(sig);
                if (signature != null) {
                    final String signatureFormat;
                    if ("7Z".equalsIgnoreCase(signature.getId())) {
                        signatureFormat = ArchiveFormat.SEVEN_ZIP.name();
                    } else if ("RAR".equalsIgnoreCase(signature.getId())) {
                        signatureFormat = ArchiveFormat.RAR.name();
                    } else if ("ZIP".equalsIgnoreCase(signature.getId())) {
                        signatureFormat = ArchiveFormat.ZIP.name();
                    } else if ("GZ".equalsIgnoreCase(signature.getId())) {
                        signatureFormat = ArchiveFormat.GZIP.name();
                    } else if ("BZ2".equalsIgnoreCase(signature.getId())) {
                        signatureFormat = ArchiveFormat.BZIP2.name();
                    } else {
                        signatureFormat = null;
                    }
                    if (signatureFormat != null && !format.name().startsWith(signatureFormat)) {
                        logger.warning("Format missmatch:" + format + "!=" + signatureFormat);
                    }
                }
            } catch (Throwable e) {
                getLogger().log(e);
            }
            final IArchiveOpenCallback callBack;
            if (archive.getArchiveFiles().size() == 1) {
                final RandomAccessFile raf = new RandomAccessFile(firstArchiveFile.getFilePath(), "r");
                closable = raf;
                callBack = new DummyOpener();
                inStream = new RandomAccessFileInStream(raf);
            } else {
                switch (archive.getArchiveType()) {
                case RAR_MULTI:
                    final RarOpener rarOpener = new RarOpener(archive, logger);
                    closable = rarOpener;
                    callBack = rarOpener;
                    inStream = rarOpener.getStream(firstArchiveFile);
                    break;
                case SEVENZIP_PARTS:
                case ZIP_MULTI:
                    final MultiOpener sevenZipPartsOpener = new MultiOpener(archive, getLogger());
                    closable = sevenZipPartsOpener;
                    callBack = sevenZipPartsOpener;
                    inStream = new ModdedVolumedArchiveInStream(firstArchiveFile.getFilePath(), sevenZipPartsOpener);
                    break;
                default:
                    final MultiOpener multiOpener = new MultiOpener(archive, getLogger());
                    closable = multiOpener;
                    callBack = multiOpener;
                    inStream = multiOpener.getStream(firstArchiveFile);
                    break;
                }
            }
            if (inStream != null && closable != null) {
                try {
                    inArchive = createSevenZipArchiveWrapper(format, inStream, callBack);
                } catch (InvocationTargetException e) {
                    if (e.getTargetException() != null) {
                        throw e.getTargetException();
                    } else {
                        throw e;
                    }
                }
            } else {
                return false;
            }
            for (ISimpleInArchiveItem item : inArchive.getSimpleInterface().getArchiveItems()) {
                if (item.isEncrypted()) {
                    archive.setProtected(true);
                    break;
                }
            }
            if (inArchive.getNumberOfItems() == 0) {
                throw new SevenZipException("No Items found in \"" + firstArchiveFile.getFilePath() + "\"! Maybe unsupported archive type?");
            }
            updateContentView(inArchive.getSimpleInterface());
        } catch (SevenZipException e) {
            if (StringUtils.containsIgnoreCase(e.getMessage(), "can't be opened")) {
                try {
                    if (!validateArchiveParts(archive)) {
                        if (archive.getCrcError().size() > 0) {
                            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CRC_ERROR);
                        }
                        return false;
                    }
                } catch (final IOException e2) {
                    logger.log(e);
                    throw new ExtractionException(e2, null);
                }
            }
            if (e.getMessage() != null && (e.getMessage().contains("HRESULT: 0x80004005") || e.getMessage().contains("HRESULT: 0x1 (FALSE)") || e.getMessage().contains("can't be opened") || e.getMessage().contains("No password was provided"))) {
                /* password required */
                archive.setProtected(true);
                archive.setPasswordRequiredToOpen(true);
                return true;
            } else {
                logger.log(e);
                throw new ExtractionException(e, null);
            }
        } catch (Throwable e) {
            logger.log(e);
            throw new ExtractionException(e, null);
        }
        return true;
    }

    private boolean validateArchiveParts(final Archive archive) throws IOException {
        final ArchiveType type = archive.getArchiveType();
        switch (type) {
        case RAR_SINGLE:
        case RAR_MULTI:
        case RAR_MULTI2:
        case RAR_MULTI3:
        case RAR_MULTI4:
            for (final ArchiveFile archiveFile : archive.getArchiveFiles()) {
                if (!ArchiveType.RAR_SINGLE.isValidPart(0, archiveFile)) {
                    archive.addCrcError(archiveFile);
                    logger.severe("Missing/Broken" + type + " Signature: " + archiveFile);
                }
            }
            break;
        case ZIP_SINGLE:
            for (final ArchiveFile archiveFile : archive.getArchiveFiles()) {
                final String sig = FileSignatures.readFileSignature(new File(archiveFile.getFilePath()));
                final Signature signature = new FileSignatures().getSignature(sig);
                if (!"ZIP".equalsIgnoreCase(signature.getId())) {
                    archive.addCrcError(archiveFile);
                    logger.severe("Missing/Broken" + type + " Signature: " + archiveFile);
                }
            }
            break;
        case GZIP_SINGLE:
            for (final ArchiveFile archiveFile : archive.getArchiveFiles()) {
                final String sig = FileSignatures.readFileSignature(new File(archiveFile.getFilePath()));
                final Signature signature = new FileSignatures().getSignature(sig);
                if (!"GZ".equalsIgnoreCase(signature.getId())) {
                    archive.addCrcError(archiveFile);
                    logger.severe("Missing/Broken" + type + " Signature: " + archiveFile);
                }
            }
            break;
        case BZIP2_SINGLE:
            for (final ArchiveFile archiveFile : archive.getArchiveFiles()) {
                final String sig = FileSignatures.readFileSignature(new File(archiveFile.getFilePath()));
                final Signature signature = new FileSignatures().getSignature(sig);
                if (!"BZ2".equalsIgnoreCase(signature.getId())) {
                    archive.addCrcError(archiveFile);
                    logger.severe("Missing/Broken" + type + " Signature: " + archiveFile);
                }
            }
            break;
        case SEVENZIP_SINGLE:
            for (final ArchiveFile archiveFile : archive.getArchiveFiles()) {
                final String sig = FileSignatures.readFileSignature(new File(archiveFile.getFilePath()));
                final Signature signature = new FileSignatures().getSignature(sig);
                if (!"7Z".equalsIgnoreCase(signature.getId())) {
                    archive.addCrcError(archiveFile);
                    logger.severe("Missing/Broken" + type + " Signature: " + archiveFile);
                }
            }
            break;
        default:
            break;
        }
        return archive.getCrcError().size() == 0;
    }

    private void updateContentView(ISimpleInArchive simpleInterface) {
        final Archive archive = getExtractionController().getArchive();
        try {
            if (archive != null) {
                initFilters();
                final ContentView newView = new ContentView();
                for (ISimpleInArchiveItem item : simpleInterface.getArchiveItems()) {
                    try {
                        final String itemPath = item.getPath();
                        if (StringUtils.isEmpty(itemPath) || isFiltered(itemPath)) {
                            continue;
                        }
                        newView.add(new PackedFile(item.isFolder(), itemPath, item.getSize()));
                    } catch (SevenZipException e) {
                        getLogger().log(e);
                    }
                }
                archive.setContentView(newView);
            }
        } catch (SevenZipException e) {
            getLogger().log(e);
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
            for (final ArchiveType archiveType : ArchiveType.values()) {
                if (archiveType.matches(factory.getFilePath())) {
                    return null;
                }
            }
            return false;
        }
    }
}