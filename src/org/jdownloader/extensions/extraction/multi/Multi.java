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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.ISevenZipInArchive;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.impl.VolumedArchiveInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.Regex;
import org.appwork.utils.ReusableByteArrayOutputStream;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.StringFormatter;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.DummyArchiveFile;
import org.jdownloader.extensions.extraction.ExtSevenZipException;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionControllerConstants;
import org.jdownloader.extensions.extraction.ExtractionException;
import org.jdownloader.extensions.extraction.FileSignatures;
import org.jdownloader.extensions.extraction.IExtraction;
import org.jdownloader.extensions.extraction.Item;
import org.jdownloader.extensions.extraction.Signature;
import org.jdownloader.extensions.extraction.content.ContentView;
import org.jdownloader.extensions.extraction.content.PackedFile;
import org.jdownloader.extensions.extraction.gui.iffileexistsdialog.IfFileExistsDialog;
import org.jdownloader.settings.IfFileExistsAction;

/**
 * Extracts rar, zip, 7z. tar.gz, tar.bz2.
 * 
 * @author botzi
 * 
 */
public class Multi extends IExtraction {

    private static final String      REGEX_ZIP_PART_REPLACE                                      = "(?i)\\.(z\\d+|zip)$";

    private static final String      REGEX_MULTI_ZIP_PART                                        = "(?i).*(\\.z\\d+|zip)$";

    private static final String      Z_D_$                                                       = "(\\.z)(\\d+)$";

    /**
     * Helper for the passwordfinding method.
     * 
     * @author botzi
     * 
     */

    private static final String      _7Z_D                                                       = "(\\.7z)?\\.\\D?(\\d{1,4})$";
    private static final String      REGEX_ANY_7ZIP_PART                                         = "(?i).*(\\.7z)?\\.\\d{1,4}$";

    private static final String      _7Z$                                                        = "\\.7z$";
    private static final String      REGEX_SINGLE_7ZIP                                           = "(?i).*\\.7z$";

    private static final String      BZ2$                                                        = "\\.bz2$";
    private static final String      GZ$                                                         = "\\.gz$";
    private static final String      REGEX_FIRST_PART_7ZIP                                       = "(?i).*(\\.7z)?\\.001$";
    private static final String      REGEX_ZERO_PART_MULTI_RAR                                   = "(?i).*\\.pa?r?t?\\.?[0]*0\\.rar$";
    private static final String      REGEX_FIRST_PART_MULTI_RAR                                  = "(?i).*\\.pa?r?t?\\.?[0]*1.*?\\.rar$";
    private static final String      REGEX_ANY_MULTI_RAR_PART_FILE                               = "(?i).*\\.pa?r?t?\\.?[0-9]+.*?.rar$";
    private static final String      REGEX_ANY_DOT_R19_FILE                                      = "(?i).*\\.r\\d+$";
    private static final String      REGEX_ENDS_WITH_DOT_RAR                                     = "(?i).*\\.rar$";
    private static final String      PA_R_T_0_9_RAR$                                             = "\\.pa?r?t?\\.?[0-9]+.*?\\.rar$";
    private static final String      REGEX_FIND_PARTNUMBER_MULTIRAR                              = "\\.pa?r?t?\\.?(\\d+)\\.rar$";
    private static final String      REGEX_FIND_MULTIPARTRAR_FULL_PART_EXTENSION_AND_PART_NUMBER = "(\\.pa?r?t?\\.?)(\\d+)\\.";
    public static final String       PATTERN_RAR_MULTI                                           = "(?i).*\\.pa?r?t?\\.?\\d+.*?\\.rar$";
    private static final String      REGEX_FIND_PARTNUMBER                                       = "\\.r(\\d+)$";
    private static final String      REGEX_FIND_PART_EXTENSION_AND_PARTNUMBER                    = "(\\.r)(\\d+)$";

    private static final String      REGEX_EXTENSION_RAR                                         = "\\.rar$";
    private static final String      TAR_BZ2$                                                    = "\\.tar\\.bz2$";

    private static final String      TAR_GZ$                                                     = "(\\.tar\\.gz|\\.tgz)$";

                                                                                                                                          ;
    private static final String      TAR$                                                        = "\\.tar$";

    private static final String      REGEX_ZIP$                                                  = "(?i).*\\.zip$";
    private static final String      ZIP$                                                        = "\\.zip$";
    private int                      crack;
    private java.util.List<String>   filter                                                      = new ArrayList<String>();

    private ArchiveFormat            format;

    private ISevenZipInArchive       inArchive;
    /** For 7z */
    private MultiOpener              multiopener;

    /** For rar */
    private RarOpener                raropener;

    /** For all single files */
    private RandomAccessFileInStream stream;

    public Multi() {
        crack = 0;
        inArchive = null;
    }

    @Override
    public Archive buildArchive(ArchiveFactory link) throws ArchiveException {
        String file = link.getFilePath();
        Archive archive = link.createArchive();
        archive.setName(getArchiveName(link));
        String pattern = "";
        boolean canBeSingleType = true;
        if (file.matches(PATTERN_RAR_MULTI)) {
            pattern = "^" + Regex.escape(file.replaceAll("(?i)\\.pa?r?t?\\.?[0-9]+.*?\\.rar$", "")) + "\\.pa?r?t?\\.?[0-9]+.*?\\.rar$";
            archive.setArchiveFiles(link.createPartFileList(file, pattern));
            canBeSingleType = false;
        } else if (file.matches(REGEX_ENDS_WITH_DOT_RAR)) {
            pattern = "^" + Regex.escape(file.replaceAll("(?i)\\.rar$", "")) + REGEX_FIND_PART_EXTENSION_AND_PARTNUMBER;
            archive.setArchiveFiles(link.createPartFileList(file, pattern));
        } else if (file.matches(REGEX_ANY_DOT_R19_FILE)) {
            // matches.add(link);
            pattern = "^" + Regex.escape(file.replaceAll("(?i)\\.r\\d+$", "")) + "\\.(r\\d+|rar)$";
            archive.setArchiveFiles(link.createPartFileList(file, pattern));
            canBeSingleType = false;
        } else if (file.matches(REGEX_SINGLE_7ZIP)) {
            /* single part 7zip */
            pattern = "^" + Regex.escape(file.replaceAll("(?i)\\.7z$", "")) + _7Z$;
            archive.setArchiveFiles(link.createPartFileList(file, pattern));
            canBeSingleType = true;
        } else if (file.matches(REGEX_MULTI_ZIP_PART)) {
            pattern = "^" + Regex.escape(file.replaceAll(REGEX_ZIP_PART_REPLACE, "")) + "(\\.z\\d+$|\\.zip)";
            List<ArchiveFile> files = link.createPartFileList(file, pattern);
            if (files.size() == 1 && files.get(0).getName().matches(REGEX_ZIP$)) {
                // single zip
                archive.setArchiveFiles(files);
                canBeSingleType = true;
            } else {
                // multizip
                archive.setArchiveFiles(files);
                canBeSingleType = false;
            }
        } else if (file.matches(REGEX_ANY_7ZIP_PART)) {
            /* MUST BE LAST ONE! */
            /* multipart 7zip */
            pattern = "^" + Regex.escape(file.replaceAll("(?i)(\\.7z)?\\.\\d+$", "")) + _7Z_D;
            archive.setArchiveFiles(link.createPartFileList(file, pattern));
            canBeSingleType = false;
        } else {
            throw new ArchiveException("Unsupported Archive: " + link.getFilePath());
        }
        ArchiveFile firstArchiveFile = null;
        if (archive.getArchiveFiles().size() == 1 && canBeSingleType) {
            archive.setType(ArchiveType.SINGLE_FILE);
            firstArchiveFile = archive.getArchiveFiles().get(0);
        } else {
            for (ArchiveFile l : archive.getArchiveFiles()) {
                if ((archive.getType() == null || ArchiveType.MULTI_RAR == archive.getType()) && (l.getFilePath().matches(REGEX_FIRST_PART_MULTI_RAR) || l.getFilePath().matches(REGEX_ZERO_PART_MULTI_RAR))) {
                    if (firstArchiveFile == null) {
                        archive.setType(ArchiveType.MULTI_RAR);
                        firstArchiveFile = l;
                    } else {
                        /* find the part with lowest number */
                        String newPartNumber = new Regex(l.getFilePath(), REGEX_FIND_PARTNUMBER_MULTIRAR).getMatch(0);
                        String oldPartNumber = new Regex(firstArchiveFile.getFilePath(), REGEX_FIND_PARTNUMBER_MULTIRAR).getMatch(0);
                        if (newPartNumber == null || oldPartNumber == null) { throw new ArchiveException("Regex issue? Type:" + archive.getType() + "|File1:" + l.getFilePath() + "|File2:" + firstArchiveFile.getFilePath()); }
                        if (Integer.parseInt(newPartNumber) < Integer.parseInt(oldPartNumber)) {
                            firstArchiveFile = l;
                        }
                    }
                    if (l.isComplete() == false) {
                        /* this should help finding the link that got downloaded */
                        continue;
                    }
                } else if (l.getFilePath().matches(REGEX_ENDS_WITH_DOT_RAR) && !l.getFilePath().matches(REGEX_ANY_MULTI_RAR_PART_FILE)) {
                    if (archive.getArchiveFiles().size() == 1) {
                        archive.setType(ArchiveType.SINGLE_FILE);
                        firstArchiveFile = archive.getArchiveFiles().get(0);
                    } else {
                        archive.setType(ArchiveType.MULTI_RAR);
                        firstArchiveFile = l;
                    }
                    if (l.isComplete() == false) {
                        /* this should help finding the link that got downloaded */
                        continue;
                    }
                    break;
                } else if ((archive.getType() == null || ArchiveType.MULTI == archive.getType()) && l.getFilePath().matches(REGEX_FIRST_PART_7ZIP)) {
                    if (checkRARSignature(new File(l.getFilePath()))) {
                        /* rar archive with 001 ending */
                        archive.setType(ArchiveType.MULTI_RAR);
                        firstArchiveFile = l;
                        if (l.isComplete() == false) {
                            /* this should help finding the link that got downloaded */
                            continue;
                        }
                    } else if (check7ZSignature(new File(l.getFilePath()))) {
                        /* 7z archive with 001 ending */
                        archive.setType(ArchiveType.MULTI);
                        firstArchiveFile = l;
                        if (l.isComplete() == false) {
                            /* this should help finding the link that got downloaded */
                            continue;
                        }
                    }
                    break;
                } else if ((archive.getType() == null || ArchiveType.MULTI == archive.getType()) && l.getFilePath().matches(REGEX_ZIP$)) {
                    archive.setType(ArchiveType.MULTI_RAR);
                    firstArchiveFile = l;
                    if (l.isComplete() == false) {
                        /* this should help finding the link that got downloaded */
                        continue;
                    }
                    break;
                }
                // TODO several multipart archive types are missing in this loop
            }
            if (archive.getType() == null) {
                // maybe first part missing? try to get archive type without
                // first part

                for (ArchiveFile l : archive.getArchiveFiles()) {
                    if (l.getFilePath().matches(REGEX_ANY_MULTI_RAR_PART_FILE)) {
                        archive.setType(ArchiveType.MULTI_RAR);
                        if (l.isComplete() == false) {
                            /*
                             * this should help finding the link that got downloaded
                             */
                            continue;
                        }
                        break;
                    } else if (l.getFilePath().matches(REGEX_ANY_7ZIP_PART)) {
                        archive.setType(ArchiveType.MULTI);
                        if (l.isComplete() == false) {
                            /*
                             * this should help finding the link that got downloaded
                             */
                            continue;
                        }
                        break;
                    } else if (l.getFilePath().matches(REGEX_ANY_DOT_R19_FILE)) {
                        archive.setType(ArchiveType.MULTI_RAR);
                        if (l.isComplete() == false) {
                            /*
                             * this should help finding the link that got downloaded
                             */
                            continue;
                        }
                        break;
                    } else if (l.getFilePath().matches(REGEX_MULTI_ZIP_PART)) {
                        archive.setType(ArchiveType.MULTI_RAR);
                        if (l.isComplete() == false) {
                            /*
                             * this should help finding the link that got downloaded
                             */
                            continue;
                        }
                        break;
                    }
                    // TODO several multipart archive types are missing in this
                    // loop
                }
            }
        }
        archive.setFirstArchiveFile(firstArchiveFile);
        if (archive.getType() == null) { //
            throw new ArchiveException("Unsupported Archive: " + link.getFilePath());
        }
        return archive;
    }

    public static boolean checkRARSignature(File file) {
        Signature signature = null;
        try {
            String sig = FileSignatures.readFileSignature(file);
            signature = new FileSignatures().getSignature(sig);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (signature != null) {
            if ("RAR".equalsIgnoreCase(signature.getId())) return true;
        }
        return false;
    }

    public static boolean check7ZSignature(File file) {
        Signature signature = null;
        try {
            String sig = FileSignatures.readFileSignature(file);
            signature = new FileSignatures().getSignature(sig);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (signature != null) {
            if ("7Z".equalsIgnoreCase(signature.getId())) return true;
        }
        return false;
    }

    public void setLastModifiedDate(ISimpleInArchiveItem item, File extractTo) {
        // Set last write time
        try {
            if (config.isUseOriginalFileDate()) {
                Date date = item.getLastWriteTime();
                if (date != null && date.getTime() >= 0) {
                    if (!extractTo.setLastModified(date.getTime())) {
                        logger.warning("Could not set last write/modified time for " + item.getPath());
                        return;
                    }
                }
            } else {
                extractTo.setLastModified(System.currentTimeMillis());
            }
        } catch (final Throwable e) {
            logger.log(e);
        }
    }

    @Override
    public boolean checkCommand() {
        File tmp = null;
        String libID = null;
        try {
            String s = System.getProperty("os.arch");
            String s1 = System.getProperty("os.name").split(" ")[0];
            libID = new StringBuilder().append(s1).append("-").append(s).toString();
            logger.finer("Lib ID: " + libID);
            tmp = Application.getTempResource("7zip");
            try {
                org.appwork.utils.Files.deleteRecursiv(tmp);
            } catch (final Throwable e) {
            }
            logger.finer("Lib Path: " + tmp);
            tmp.mkdirs();
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
                    if (SevenZip.isInitializedSuccessfully()) return true;
                } catch (final Throwable e2) {
                    e2.printStackTrace();
                }
            }
            try {
                org.appwork.utils.Files.deleteRecursiv(tmp);
            } catch (final Throwable e1) {
            }
            logger.log(e);
            logger.warning("Could not initialize Multiunpacker #1");
            try {
                String s2 = System.getProperty("java.io.tmpdir");
                logger.finer("Lib Path: " + (tmp = new File(s2)));
                SevenZip.initSevenZipFromPlatformJAR(tmp);
            } catch (Throwable e2) {
                logger.log(e2);
                logger.warning("Could not initialize Multiunpacker #2");
                return false;
            }
        }
        return SevenZip.isInitializedSuccessfully();
    }

    @Override
    public DummyArchive checkComplete(Archive archive) throws CheckException {
        try {
            DummyArchive ret = new DummyArchive(archive);

            if (archive.getType() == ArchiveType.SINGLE_FILE) {
                ret.add(new DummyArchiveFile(archive.getArchiveFiles().get(0)));
                return ret;
            }

            int last = 0;
            int length = 0;
            int start = 2;
            String getpartid = "";

            String format = null;
            HashMap<Integer, ArchiveFile> erg = new HashMap<Integer, ArchiveFile>();
            switch (archive.getType()) {
            case MULTI:
                getpartid = _7Z_D;
                start = 0;
                format = archive.getArchiveFiles().get(0).getName().replace("%", "\\%").replace("$", "\\$").replaceAll("(\\.7z\\.?)(\\d+)$", "$1%s");
                length = new Regex(archive.getArchiveFiles().get(0).getName(), "(\\.7z)?\\.?(\\d{1,4})$").getMatch(1).length();
                // Just get partnumbers to speed up the checking.

                for (ArchiveFile l : archive.getArchiveFiles()) {
                    String e = "";
                    // String name = l.getName();
                    if ((e = new Regex(l.getFilePath(), getpartid).getMatch(1)) != null) {
                        int p = Integer.parseInt(e);
                        if (p > last) last = p;
                        if (p < start) start = p;
                        erg.put(p, l);
                    }
                }
                break;
            case MULTI_RAR:
                for (ArchiveFile af : archive.getArchiveFiles()) {
                    if (af.getFilePath().matches(REGEX_ANY_MULTI_RAR_PART_FILE)) {
                        getpartid = REGEX_FIND_PARTNUMBER_MULTIRAR;
                        start = 1;
                        format = af.getName().replace("%", "\\%").replace("$", "\\$").replaceAll(REGEX_FIND_MULTIPARTRAR_FULL_PART_EXTENSION_AND_PART_NUMBER, "$1%s.");
                        length = new Regex(af.getName(), "\\.pa?r?t?\\.?(\\d+).*?\\.").getMatch(0).length();
                        // Just get partnumbers to speed up the checking.

                        for (ArchiveFile l : archive.getArchiveFiles()) {
                            String e = "";
                            // String name = l.getName();
                            if ((e = new Regex(l.getFilePath(), getpartid).getMatch(0)) != null) {
                                int p = Integer.parseInt(e);
                                if (p > last) last = p;
                                if (p < start) start = p;
                                erg.put(p, l);
                            }
                        }
                        break;
                    } else if (af.getFilePath().matches(REGEX_ANY_DOT_R19_FILE)) {
                        start = 0;
                        getpartid = REGEX_FIND_PARTNUMBER;
                        format = af.getName().replace("%", "\\%").replace("$", "\\$").replaceAll(REGEX_FIND_PARTNUMBER, ".r%s");
                        length = new Regex(af.getName(), REGEX_FIND_PARTNUMBER).getMatch(0).length();
                        // Just get partnumbers to speed up the checking.

                        for (ArchiveFile l : archive.getArchiveFiles()) {
                            String e = "";
                            // String name = l.getName();
                            if ((e = new Regex(l.getFilePath(), getpartid).getMatch(0)) != null) {
                                int p = Integer.parseInt(e);
                                if (p > last) last = p;
                                if (p < start) start = p;
                                erg.put(p, l);

                            } else if (l.getFilePath().endsWith(".rar")) {
                                ret.add(new DummyArchiveFile(l));

                            }
                        }
                        if (!erg.containsKey(0)) start = 1;
                        if (ret.getSize() == 0) {
                            // .rar missing
                            ret.add(new DummyArchiveFile(archive.getName() + ".rar", archive.getFolder()));

                        }
                        break;
                    } else if (af.getFilePath().matches(REGEX_ANY_7ZIP_PART)) {
                        start = 0;
                        getpartid = _7Z_D;
                        format = archive.getArchiveFiles().get(0).getName().replace("%", "\\%").replace("$", "\\$").replaceAll("(\\.7z\\.?)(\\d+)$", "$1%s");
                        length = new Regex(archive.getArchiveFiles().get(0).getName(), "(\\.7z)?\\.?(\\d+)$").getMatch(1).length();
                        // Just get partnumbers to speed up the checking.

                        for (ArchiveFile l : archive.getArchiveFiles()) {
                            String e = "";
                            // String name = l.getName();
                            if ((e = new Regex(l.getFilePath(), getpartid).getMatch(1)) != null) {
                                int p = Integer.parseInt(e);
                                if (p > last) last = p;
                                if (p < start) start = p;
                                erg.put(p, l);
                            }
                        }
                        break;
                    } else if (af.getFilePath().matches(REGEX_MULTI_ZIP_PART)) {
                        start = 0;
                        getpartid = Z_D_$;
                        //
                        System.out.println(1);
                        format = archive.getArchiveFiles().get(0).getName().replace("%", "\\%").replace("$", "\\$");
                        format = format.replaceAll(REGEX_ZIP_PART_REPLACE, "%s");
                        for (ArchiveFile af1 : archive.getArchiveFiles()) {
                            String lenthstring = new Regex(af1.getName(), Z_D_$).getMatch(1);
                            if (lenthstring != null) {
                                length = lenthstring.length();
                                break;
                            }
                        }
                        if (length <= 0) length = 2;
                        // Just get partnumbers to speed up the checking.

                        for (ArchiveFile l : archive.getArchiveFiles()) {
                            String e = "";
                            // String name = l.getName();
                            if ((e = new Regex(l.getFilePath(), getpartid).getMatch(1)) != null) {
                                int p = Integer.parseInt(e);
                                if (p > last) last = p;
                                if (p < start) start = p;
                                erg.put(p, l);
                            } else {
                                erg.put(0, l);
                            }
                        }
                        break;
                    }

                }

            }
            if (format == null) throw new CheckException("Cannot check Archive " + archive.getName());

            boolean ZEROZEROZEROMissing = false;
            int missing = 0;
            for (int i = start; i <= last; i++) {

                ArchiveFile af = erg.get(i);

                if (af != null) {
                    // available file
                    ret.add(new DummyArchiveFile(af));
                } else {
                    missing++;
                    if (Z_D_$ == getpartid) {
                        if (i == 0) {
                            // .zip is missing
                            String part = String.format(format, ".zip");
                            ret.add(new DummyArchiveFile(part, archive.getFolder()));
                            continue;
                        } else {
                            String part = String.format(format, ".z" + StringFormatter.fillString(i + "", "0", "", length));
                            ret.add(new DummyArchiveFile(part, archive.getFolder()));
                            continue;
                        }
                    }
                    if (i == 0) {
                        ZEROZEROZEROMissing = true;
                        continue;
                    }

                    // missing file
                    String part = String.format(format, StringFormatter.fillString(i + "", "0", "", length));
                    ret.add(new DummyArchiveFile(part, archive.getFolder()));
                }

            }
            if (archive.getType() == ArchiveType.MULTI && missing == 0 && ZEROZEROZEROMissing) {

                logger.info("Workaround(may be wrong) for non existing .000");
            }
            return ret;
        } catch (Throwable e) {
            logger.log(e);
            throw new CheckException("Cannot check Archive " + archive.getName(), e);
        }
    }

    @Override
    public void close() {
        // Deleteing rar recovery volumes
        try {
            if (archive.getExitCode() == ExtractionControllerConstants.EXIT_CODE_SUCCESS && archive.getFirstArchiveFile().getFilePath().matches(PATTERN_RAR_MULTI)) {
                for (ArchiveFile link : archive.getArchiveFiles()) {
                    File f = archive.getFactory().toFile(link.getFilePath().replace(REGEX_EXTENSION_RAR, ".rev"));
                    if (f.exists() && f.getName().toLowerCase(Locale.ENGLISH).endsWith(".rev")) {
                        logger.info("Deleting rar recovery volume " + f.getAbsolutePath());
                        if (!f.delete()) {
                            logger.warning("Could not deleting rar recovery volume " + f.getAbsolutePath());
                        }
                    }
                }
            }
        } finally {
            try {
                raropener.close();
            } catch (final Throwable e) {
            }
            try {
                multiopener.close();
            } catch (final Throwable e) {
            }
            try {
                stream.close();
            } catch (final Throwable e) {
            }
            try {
                inArchive.close();
            } catch (final Throwable e) {
            }
        }

    }

    @Override
    public String createID(ArchiveFactory f) {

        String[] patterns = new String[] { PA_R_T_0_9_RAR$, REGEX_EXTENSION_RAR, ZIP$, _7Z$, TAR_GZ$, TAR_BZ2$, REGEX_FIND_PART_EXTENSION_AND_PARTNUMBER, TAR$, Z_D_$, _7Z_D };

        for (String p : patterns) {
            Pattern pattern = Pattern.compile(p, Pattern.CASE_INSENSITIVE);

            Matcher matcher = pattern.matcher(f.getName());
            if (matcher.find()) {
                //

                if (p.equals(REGEX_FIND_PART_EXTENSION_AND_PARTNUMBER)) {
                    // we cannot distingish between *.rar and *.rar -> *.r00
                    // archives when looking only at one single filename. that's
                    // why the get the same id
                    return matcher.replaceAll(REGEX_EXTENSION_RAR.replace("\\", "_").replace("$", "_"));
                } else if (p.equals(Z_D_$)) {
                    // we cannot distingish between *.zip and *.zip -> *.z01
                    // archives when looking only at one single filename. that's
                    // why the get the same id
                    return matcher.replaceAll(ZIP$.replace("\\", "_").replace("$", "_"));
                } else {
                    return matcher.replaceAll(p.replace("\\", "_").replace("$", "_"));
                }

            }
        }
        return null;
    }

    @Override
    public void extract(final ExtractionController ctrl) {
        try {
            ctrl.setCompleteBytes(archive.getContentView().getTotalSize());
            ctrl.setProcessedBytes(0);
            if (ArchiveFormat.SEVEN_ZIP == format) {
                ArrayList<Integer> allItems = new ArrayList<Integer>();
                for (int i = 0; i < inArchive.getNumberOfItems(); i++) {
                    final Boolean isFolder = (Boolean) inArchive.getProperty(i, PropID.IS_FOLDER);
                    if (Boolean.TRUE.equals(isFolder)) continue;
                    allItems.add(i);
                }
                final int maxItemsRound = 50;
                ArrayList<Integer> round = new ArrayList<Integer>();
                Iterator<Integer> it = allItems.iterator();
                while (it.hasNext()) {
                    Integer next = it.next();
                    round.add(next);
                    if (round.size() == maxItemsRound || it.hasNext() == false) {
                        int[] items = new int[round.size()];
                        int index = 0;
                        for (Integer item : round) {
                            items[index++] = item;
                        }
                        round.clear();
                        Seven7ExtractCallback callback = null;
                        try {
                            inArchive.extract(items, false, callback = new Seven7ExtractCallback(this, inArchive, ctrl, archive, config));
                        } catch (SevenZipException e) {
                            logger.log(e);
                            throw e;
                        } finally {
                            ctrl.setCurrentActiveItem(null);
                            if (callback != null) callback.close();
                        }
                        if (callback != null) {
                            if (callback.hasError()) throw new SevenZipException("callback encountered an error!");
                            if (callback.isResultMissing()) throw new SevenZipException("callback is missing results!");
                        }
                    }
                }
            } else {
                for (ISimpleInArchiveItem item : inArchive.getSimpleInterface().getArchiveItems()) {
                    // Skip folders
                    if (item == null || item.isFolder()) {
                        continue;
                    }
                    if (ctrl.gotKilled()) { throw new SevenZipException("Extraction has been aborted"); }
                    AtomicBoolean skipped = new AtomicBoolean(false);
                    File extractTo = getExtractFilePath(item, ctrl, skipped);
                    if (skipped.get()) {
                        /* file is skipped */
                        continue;
                    }
                    if (extractTo == null) {
                        /* error */
                        return;
                    }
                    archive.addExtractedFiles(extractTo);
                    ctrl.setCurrentActiveItem(new Item(item.getPath(), item.getSize(), extractTo));
                    try {
                        MultiCallback call = new MultiCallback(extractTo, controller, config, false) {

                            @Override
                            public int write(byte[] data) throws SevenZipException {
                                try {
                                    if (ctrl.gotKilled()) throw new SevenZipException("Extraction has been aborted");
                                    return super.write(data);
                                } finally {
                                    ctrl.addAndGetProcessedBytes(data.length);
                                }
                            }
                        };
                        ExtractOperationResult res;
                        try {
                            if (item.isEncrypted()) {
                                String pw = archive.getFinalPassword();
                                if (pw == null) { throw new IOException("Password is null!"); }
                                res = item.extractSlow(call, pw);
                            } else {
                                res = item.extractSlow(call);
                            }
                        } finally {
                            /* always close files, thats why its best in finally branch */
                            call.close();
                        }

                        setLastModifiedDate(item, extractTo);
                        if (item.getSize() != extractTo.length()) {
                            if (ExtractOperationResult.OK == res) {
                                logger.info("Size missmatch for " + item.getPath() + ", but Extraction returned OK?! Archive seems incomplete");
                                archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_INCOMPLETE_ERROR);
                                return;
                            }
                            logger.info("Size missmatch for " + item.getPath() + " is " + extractTo.length() + " but should be " + item.getSize());
                            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CRC_ERROR);
                            return;
                        }
                        switch (res) {
                        case OK:
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
            setException(e);
            logger.log(e);
            archive.setExitCode(e.getExitCode());
            return;
        } catch (ExtSevenZipException e) {
            setException(e);
            logger.log(e);
            archive.setExitCode(e.getExitCode());
        } catch (SevenZipException e) {
            setException(e);
            logger.log(e);
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
            return;
        } catch (IOException e) {
            setException(e);
            logger.log(e);
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
            return;
        }
        archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_SUCCESS);
    }

    /**
     * Checks if the file should be upacked.
     * 
     * @param file
     * @return
     */
    private boolean filter(String file) {
        file = "/" + file;
        for (String entry : filter) {
            if (file.contains(entry)) { return true; }
        }
        return false;
    }

    /**
     * Builds an DummyArchiveFile from an file.
     * 
     * @param file
     *            The file from the harddisk.
     * 
     * @return The ArchiveFile.
     */
    // private ArchiveFile buildArchiveFileFromFile(String file) {
    // File file0 = new File(file);
    // DummyArchiveFile link = new DummyArchiveFile(file0.getName());
    // link.setFile(file0);
    // return link;
    // }
    //

    public File getExtractFilePath(ISimpleInArchiveItem item, ExtractionController ctrl, AtomicBoolean skipped) throws SevenZipException {
        String path = item.getPath();
        if (StringUtils.isEmpty(path)) {
            // example: test.tar.gz contains a test.tar file, that has
            // NO name. we create a dummy name here.
            String archivename = archive.getFactory().toFile(archive.getFirstArchiveFile().getFilePath()).getName();
            int in = archivename.lastIndexOf(".");
            if (in > 0) {
                path = archivename.substring(0, in);
            }
        }
        if (filter(item.getPath())) {
            logger.info("Filtering file " + item.getPath() + " in " + archive.getFirstArchiveFile().getFilePath());
            ctrl.addAndGetProcessedBytes(item.getSize());
            skipped.set(true);
            return null;
        }
        if (CrossSystem.isWindows()) {
            String pathParts[] = path.split("\\" + File.separator);
            /* remove invalid path chars */
            for (int pathIndex = 0; pathIndex < pathParts.length - 1; pathIndex++) {
                pathParts[pathIndex] = CrossSystem.alleviatePathParts(pathParts[pathIndex]);
            }
            StringBuilder sb = new StringBuilder();
            for (String pathPartItem : pathParts) {
                if (sb.length() > 0) sb.append(File.separator);
                sb.append(pathPartItem);
            }
            path = sb.toString();
        }
        String filename = controller.getExtractToFolder().getAbsoluteFile() + File.separator + path;

        File extractTo = new File(filename);
        logger.info("Extract " + filename);
        if (extractTo.exists()) {
            /* file already exists */

            IfFileExistsAction action = controller.getIfFileExistsAction();
            while (action == null || action == IfFileExistsAction.ASK_FOR_EACH_FILE) {
                IfFileExistsDialog d = new IfFileExistsDialog(extractTo, new Item(path, item.getSize(), extractTo), archive);
                d.show();

                try {
                    d.throwCloseExceptions();
                } catch (Exception e) {
                    throw new SevenZipException(e);
                }
                action = d.getAction();
                if (action == null) throw new SevenZipException("Cannot handle if file exists");
                if (action == IfFileExistsAction.AUTO_RENAME) {
                    extractTo = new File(extractTo.getParentFile(), d.getNewName());
                    if (extractTo.exists()) {
                        action = IfFileExistsAction.ASK_FOR_EACH_FILE;
                    }
                }
            }
            switch (action) {
            case OVERWRITE_FILE:

                if (!FileCreationManager.getInstance().delete(extractTo, null)) {
                    setException(new Exception("Could not overwrite(delete) " + extractTo));
                    archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
                    return null;
                }
                break;

            case SKIP_FILE:
                /* skip file */
                archive.addExtractedFiles(extractTo);
                ctrl.addAndGetProcessedBytes(item.getSize());
                skipped.set(true);
                return null;
            case AUTO_RENAME:
                String extension = Files.getExtension(extractTo.getName());
                String name = StringUtils.isEmpty(extension) ? extractTo.getName() : extractTo.getName().substring(0, extractTo.getName().length() - extension.length() - 1);
                int i = 1;
                while (extractTo.exists()) {
                    if (StringUtils.isEmpty(extension)) {
                        extractTo = new File(extractTo.getParentFile(), name + "_" + i);

                    } else {
                        extractTo = new File(extractTo.getParentFile(), name + "_" + i + "." + extension);

                    }
                    i++;
                }

                break;

            }

        }
        if ((!extractTo.getParentFile().exists() && !extractTo.getParentFile().mkdirs())) {
            setException(new Exception("Could not create folder for File " + extractTo));
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
            return null;
        }
        String fixedFilename = null;
        while (true) {
            try {
                if (!extractTo.createNewFile()) {
                    setException(new Exception("Could not create File " + extractTo));
                    archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
                    return null;
                }
                extractTo.delete();
                break;
            } catch (final IOException e) {
                logger.log(e);
                if (fixedFilename == null) {
                    /* first try, we try again with lastTryFilename */
                    fixedFilename = filename;
                    extractTo = new File(fixedFilename);
                    continue;
                } else if (fixedFilename == filename) {
                    /* second try, we try with modified filename */
                    /* Invalid Chars could have occured, try to remove them */
                    logger.severe("Invalid Chars could have occured, try to remove them");
                    File parent = extractTo.getParentFile();
                    String brokenFilename = extractTo.getName();
                    /* new String so == returns false */
                    fixedFilename = new String(brokenFilename.replaceAll("[^\\w\\s\\.\\(\\)\\[\\],]", ""));
                    logger.severe("Replaced " + brokenFilename + " with " + fixedFilename);
                    extractTo = new File(parent, fixedFilename);
                    continue;
                } else {
                    setException(e);
                    archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
                    return null;
                }
            }
        }
        return extractTo;
    }

    @Override
    public boolean findPassword(final ExtractionController ctl, String password, boolean optimized) throws ExtractionException {
        crack++;
        if (StringUtils.isEmpty(password)) {
            /* This should never happen */
            password = "";
        }
        ReusableByteArrayOutputStream buffer = null;
        final AtomicBoolean passwordfound = new AtomicBoolean(false);
        try {
            buffer = new ReusableByteArrayOutputStream(64 * 1024);
            try {
                stream.close();
            } catch (final Throwable e) {
            }
            try {
                inArchive.close();
            } catch (Throwable e) {
            }
            try {
                multiopener.close();
            } catch (Throwable e) {
            }
            try {
                raropener.close();
            } catch (Throwable e) {
            }
            if (archive.getType() == ArchiveType.SINGLE_FILE) {
                /* close on inArchive also closes the underlying RandomAccessFileInStream, so we have to open new one */
                stream = new RandomAccessFileInStream(new RandomAccessFile(archive.getFirstArchiveFile().getFilePath(), "r"));
                inArchive = SevenZip.openInArchive(format, stream, password);
            } else if (archive.getType() == ArchiveType.MULTI) {
                multiopener = new MultiOpener(password);
                IInStream inStream = new VolumedArchiveInStream(archive.getFirstArchiveFile().getFilePath(), multiopener);
                inArchive = SevenZip.openInArchive(ArchiveFormat.SEVEN_ZIP, inStream);
            } else if (archive.getType() == ArchiveType.MULTI_RAR) {
                raropener = new RarOpener(archive, password);
                raropener.setLogger(logger);
                IInStream inStream = raropener.getStream(archive.getFirstArchiveFile());
                inArchive = SevenZip.openInArchive(ArchiveFormat.RAR, inStream, raropener);
            }

            HashSet<String> checkedExtensions = new HashSet<String>();

            if (ArchiveFormat.SEVEN_ZIP == format) {
                if (archive.isPasswordRequiredToOpen()) {
                    // archive is open. password seems to be ok.
                    passwordfound.set(true);
                    return true;
                }
                ArrayList<Integer> allItems = new ArrayList<Integer>();
                for (int i = 0; i < inArchive.getNumberOfItems(); i++) {
                    final Boolean isFolder = (Boolean) inArchive.getProperty(i, PropID.IS_FOLDER);
                    if (Boolean.TRUE.equals(isFolder)) continue;
                    allItems.add(i);
                }
                int[] items = new int[allItems.size()];
                int index = 0;
                for (Integer item : allItems) {
                    items[index++] = item;
                }
                try {
                    inArchive.extract(items, false, new Seven7PWCallback(inArchive, passwordfound, password, buffer, config.getMaxCheckedFileSizeDuringOptimizedPasswordFindingInBytes(), ctl.getFileSignatures(), optimized));
                } catch (SevenZipException e) {
                    e.printStackTrace();
                    // An error will be thrown if the write method
                    // returns
                    // 0.
                }
            } else {
                SignatureCheckingOutStream signatureOutStream = new SignatureCheckingOutStream(passwordfound, ctl.getFileSignatures(), buffer, config.getMaxCheckedFileSizeDuringOptimizedPasswordFindingInBytes(), optimized);
                ISimpleInArchiveItem[] items = inArchive.getSimpleInterface().getArchiveItems();
                // we found some rar archives, that throw an exception when we try to open it with no or an invalid password, but do not
                // throw any exceptions if we use - for example - their archive name as password.
                // in this case, the archive opens fine, but does not show any contents. let's catch this case here
                if (archive.isPasswordRequiredToOpen() && items != null && items.length > 0) {
                    // archive is open. password seems to be ok.
                    passwordfound.set(true);
                    return true;
                }
                for (final ISimpleInArchiveItem item : items) {
                    if (item.isFolder() || (item.getSize() == 0 && item.getPackedSize() == 0)) {
                        /*
                         * we also check for items with size ==0, they should have a packedsize>0
                         */
                        continue;
                    }
                    if (ctl.gotKilled()) {
                        /* extraction got aborted */
                        break;
                    }
                    final String path = item.getPath();
                    String ext = Files.getExtension(path);
                    if (checkedExtensions.add(ext) || !optimized) {
                        if (!passwordfound.get()) {
                            try {
                                signatureOutStream.reset();
                                signatureOutStream.setSignatureLength(path, item.getSize());
                                logger.fine("Try to crack " + path);
                                ExtractOperationResult result = item.extractSlow(signatureOutStream, password);
                                if (ExtractOperationResult.DATAERROR.equals(result)) {
                                    /*
                                     * 100% wrong password, DO NOT CONTINUE as unrar already might have cleaned up (nullpointer in native -> crash jvm)
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
            if (!passwordfound.get()) return false;

            return true;
        } catch (SevenZipException e) {
            // this happens if the archive has encrypted filenames as well and thus needs a password to open it
            if (e.getMessage().contains("HRESULT: 0x80004005") || e.getMessage().contains("HRESULT: 0x1 (FALSE)") || e.getMessage().contains("can't be opened") || e.getMessage().contains("No password was provided")) {
                /* password required */
                archive.setPasswordRequiredToOpen(true);
                return false;
            }
            throw new ExtractionException(e, null);
        } catch (Throwable e) {
            throw new ExtractionException(e, null);
        } finally {
            if (passwordfound.get()) {
                archive.setFinalPassword(password);
                updateContentView(inArchive.getSimpleInterface());
            }
        }
    }

    @Override
    public String getArchiveName(ArchiveFactory factory) {

        String[] patterns = new String[] { PA_R_T_0_9_RAR$, REGEX_EXTENSION_RAR, ZIP$, _7Z$, TAR_GZ$, TAR_BZ2$, REGEX_FIND_PART_EXTENSION_AND_PARTNUMBER, TAR$, Z_D_$, _7Z_D };

        for (String p : patterns) {
            Pattern pattern = Pattern.compile(p, Pattern.CASE_INSENSITIVE);

            Matcher matcher = pattern.matcher(factory.getName());
            if (matcher.find()) {
                //

                return matcher.replaceAll("");

            }
        }
        return null;
    }

    @Override
    public int getCrackProgress() {
        return crack;
    }

    @Override
    public boolean isArchivSupported(ArchiveFactory factory, boolean allowDeepInspection) {
        String[] patterns = new String[] { PA_R_T_0_9_RAR$, REGEX_EXTENSION_RAR, ZIP$, _7Z$, TAR_GZ$, TAR_BZ2$, REGEX_FIND_PART_EXTENSION_AND_PARTNUMBER, TAR$, Z_D_$, _7Z_D };
        String archiveName = factory.getName();
        for (String p : patterns) {
            Pattern pattern = Pattern.compile(p, Pattern.CASE_INSENSITIVE);

            Matcher matcher = pattern.matcher(archiveName);
            if (matcher.find()) {

                //
                //
                if (_7Z_D.equals(p)) {
                    if (!archiveName.contains("7z.") && !archiveName.contains("7Z.")) continue;
                }
                return true;

            }
        }

        for (String p : patterns) {
            Pattern pattern = Pattern.compile(".+?" + p, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(archiveName);
            if (matcher.find()) {
                //
                if (_7Z_D.equals(p)) {
                    if (!archiveName.contains("7z.") && !archiveName.contains("7Z.")) continue;
                }
                return true;
            }
        }
        return false;
    }

    private boolean matches(String filePath, String pattern) {
        return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(filePath).find();
    }

    @Override
    public boolean prepare() throws ExtractionException {
        try {
            // if (archive.getFirstArchiveFile() instanceof DummyArchiveFile) {
            // Archive a = buildArchive(archive.getFirstArchiveFile());
            // archive.setArchiveFiles(a.getArchiveFiles());
            // archive.setType(a.getType());
            // }
            String[] entries = config.getBlacklistPatterns();
            if (entries != null) {
                for (String entry : entries) {
                    if (entry.trim().length() != 0) {
                        filter.add(entry.trim());
                    }
                }
            }
            if (archive.getType() == ArchiveType.SINGLE_FILE) {
                if (matches(archive.getFirstArchiveFile().getFilePath(), REGEX_EXTENSION_RAR)) {
                    format = ArchiveFormat.RAR;
                } else if (matches(archive.getFirstArchiveFile().getFilePath(), _7Z$)) {
                    format = ArchiveFormat.SEVEN_ZIP;
                } else if (matches(archive.getFirstArchiveFile().getFilePath(), ZIP$)) {
                    format = ArchiveFormat.ZIP;
                } else if (matches(archive.getFirstArchiveFile().getFilePath(), GZ$)) {
                    format = ArchiveFormat.GZIP;
                } else if (matches(archive.getFirstArchiveFile().getFilePath(), BZ2$)) {
                    format = ArchiveFormat.BZIP2;
                }
                stream = new RandomAccessFileInStream(new RandomAccessFile(archive.getFirstArchiveFile().getFilePath(), "r"));
                inArchive = SevenZip.openInArchive(format, stream);
            } else if (archive.getType() == ArchiveType.MULTI) {
                multiopener = new MultiOpener();
                IInStream inStream = new ModdedVolumedArchiveInStream(archive.getFirstArchiveFile().getFilePath(), multiopener);
                inArchive = SevenZip.openInArchive(ArchiveFormat.SEVEN_ZIP, inStream);
            } else if (archive.getType() == ArchiveType.MULTI_RAR) {
                raropener = new RarOpener(archive);
                raropener.setLogger(logger);
                IInStream inStream = raropener.getStream(archive.getFirstArchiveFile());
                inArchive = SevenZip.openInArchive(ArchiveFormat.RAR, inStream, raropener);
            } else {
                return false;
            }

            for (ISimpleInArchiveItem item : inArchive.getSimpleInterface().getArchiveItems()) {
                if (item.isEncrypted()) {
                    archive.setProtected(true);
                    break;
                }
            }
            updateContentView(inArchive.getSimpleInterface());
        } catch (SevenZipException e) {

            if (e.getMessage().contains("HRESULT: 0x80004005") || e.getMessage().contains("HRESULT: 0x1 (FALSE)") || e.getMessage().contains("can't be opened") || e.getMessage().contains("No password was provided")) {
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

    private void updateContentView(ISimpleInArchive simpleInterface) {
        try {
            ContentView newView = new ContentView();
            for (ISimpleInArchiveItem item : simpleInterface.getArchiveItems()) {
                try {
                    if (item.getPath().trim().equals("") || filter(item.getPath())) continue;
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

    @Override
    public boolean isMultiPartArchive(ArchiveFactory factory) {
        // rememver *.rar archives may be the start of a multiarchive, too
        String[] patterns = new String[] { PA_R_T_0_9_RAR$, REGEX_EXTENSION_RAR, _7Z_D, REGEX_FIND_PART_EXTENSION_AND_PARTNUMBER, Z_D_$, ZIP$ };

        for (String p : patterns) {
            Pattern pattern = Pattern.compile(p, Pattern.CASE_INSENSITIVE);

            Matcher matcher = pattern.matcher(factory.getName());
            if (matcher.find()) { return true;

            }
        }
        return false;
    }
}