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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import net.sf.sevenzipjbinding.SevenZipException;

import org.appwork.utils.Files;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.formatter.StringFormatter;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.ExtSevenZipException;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionControllerConstants;
import org.jdownloader.extensions.extraction.IExtraction;
import org.jdownloader.extensions.extraction.Item;
import org.jdownloader.extensions.extraction.content.PackedFile;
import org.jdownloader.extensions.extraction.gui.iffileexistsdialog.IfFileExistsDialog;
import org.jdownloader.settings.IfFileExistsAction;

/**
 * Joins XtreamSplit files.
 * 
 * @author botzi
 * 
 */
public class XtreamSplit extends IExtraction {

    private final static int        HEADER_SIZE = 104;
    private final static int        BUFFER_SIZE = 2048;

    private boolean                 md5;
    private File                    file;
    private HashMap<String, String> hashes      = new HashMap<String, String>();
    private List<String>            files       = new ArrayList<String>();

    public Archive buildArchive(ArchiveFactory link) {
        String pattern = "^" + Regex.escape(link.getFilePath().replaceAll("(?i)\\.[\\d]+\\.xtm$", "")) + "\\.[\\d]+\\.xtm$";
        Archive a = SplitUtil.buildArchive(link, pattern, ".*\\.001\\.xtm$");
        a.setName(getArchiveName(link));
        return a;
    }

    @Override
    public boolean isMultiPartArchive(ArchiveFactory factory) {
        return true;
    }

    // @Override
    // public Archive buildDummyArchive(String file) {
    // Archive a = SplitUtil.buildDummyArchive(file, ".*\\.[\\d]+\\.xtm$",
    // ".*\\.001\\.xtm$");
    // a.setExtractor(this);
    // return a;
    // }

    @Override
    public boolean findPassword(ExtractionController controller, String password, boolean optimized) {
        return true;
    }

    @Override
    public void extract(ExtractionController ctrl) {
        byte[] buffer = new byte[BUFFER_SIZE];
        Archive archive = controller.getArchiv();
        BufferedOutputStream out = null;
        BufferedInputStream in = null;
        try {
            /* file already exists */
            if (file.exists()) {
                IfFileExistsAction action = controller.getIfFileExistsAction();
                while (action == null || action == IfFileExistsAction.ASK_FOR_EACH_FILE) {
                    IfFileExistsDialog d = new IfFileExistsDialog(file, controller.getCurrentActiveItem(), archive);
                    d.show();
                    try {
                        d.throwCloseExceptions();
                    } catch (Exception e) {
                        throw new SevenZipException(e);
                    }
                    action = d.getAction();
                    if (action == null) throw new SevenZipException("Cannot handle if file exists");
                    if (action == IfFileExistsAction.AUTO_RENAME) {
                        file = new File(file.getParentFile(), d.getNewName());
                        if (file.exists()) {
                            action = IfFileExistsAction.ASK_FOR_EACH_FILE;
                        }
                    }
                }
                switch (action) {
                case OVERWRITE_FILE:
                    if (!FileCreationManager.getInstance().delete(file, null)) { throw new ExtSevenZipException(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR, "Could not overwrite(delete) " + file); }
                    break;
                case SKIP_FILE:
                    /* skip file */
                    archive.addExtractedFiles(file);
                    throw new ExtSevenZipException(ExtractionControllerConstants.EXIT_CODE_OUTPUTFILE_EXIST, "Outputfile exists: " + file);
                case AUTO_RENAME:
                    String extension = Files.getExtension(file.getName());
                    String name = StringUtils.isEmpty(extension) ? file.getName() : file.getName().substring(0, file.getName().length() - extension.length() - 1);
                    int i = 1;
                    while (file.exists()) {
                        if (StringUtils.isEmpty(extension)) {
                            file = new File(file.getParentFile(), name + "_" + i);
                        } else {
                            file = new File(file.getParentFile(), name + "_" + i + "." + extension);
                        }
                        i++;
                    }
                    break;
                }
            }
            if ((!file.getParentFile().exists() && !FileCreationManager.getInstance().mkdir(file.getParentFile())) || !file.createNewFile()) {
                archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
                return;
            }
            long size = 0l;
            for (String l : files) {
                size += new File(l).length() - HEADER_SIZE;
            }
            controller.getArchiv().getContentView().add(new PackedFile(false, archive.getName(), size));
            controller.setCompleteBytes(size);
            controller.setProcessedBytes(0);
            archive.addExtractedFiles(file);
            out = new BufferedOutputStream(new FileOutputStream(file));
            controller.setCurrentActiveItem(new Item(file.getName(), size, file));
            MessageDigest md = null;
            for (int i = 0; i < files.size(); i++) {
                File source = new File(files.get(i));
                try {
                    in = new BufferedInputStream(new FileInputStream(source));
                    if (md5) md = MessageDigest.getInstance("md5");
                    // Skip header in case of the first file
                    // can't call in.skip(), because the header counts
                    // the towards md5 hash.
                    long length = source.length();
                    if (i == 0) {
                        int alreadySkipped = 0;
                        final int toBeSkipped = HEADER_SIZE;
                        while (alreadySkipped < toBeSkipped) {
                            // Read data
                            int l = in.read(buffer, 0, toBeSkipped - alreadySkipped);
                            alreadySkipped += l;
                            // Update MD5
                            if (md5) md.update(buffer, 0, l);
                        }
                        length = length - toBeSkipped;
                    }

                    long read = 0;
                    // Skip md5 hashes at the end if it's the last file
                    boolean isItTheLastFile = i == (files.size() - 1);
                    if (md5 && isItTheLastFile) {
                        length = length - 32 * files.size();
                    }
                    // Merge
                    while (length > read) {
                        // Calculate the read buffer for the remaining byte. Max
                        // BUFFER
                        int buf = ((length - read) < BUFFER_SIZE) ? (int) (length - read) : BUFFER_SIZE;
                        // Read data
                        int read2 = in.read(buffer, 0, buf);
                        // Write data
                        if (read2 > 0) {
                            out.write(buffer, 0, read2);
                            if (controller.gotKilled()) throw new IOException("Extraction has been aborted!");
                            // Sum up bytes for control
                            controller.addAndGetProcessedBytes(read2);
                            read += read2;
                            // Update MD5
                            if (md5) md.update(buffer, 0, read2);
                        } else if (read2 < 0) throw new IOException("EOF during extraction");
                    }

                    // Check MD5 hashes
                    if (md5) {
                        final String calculatedHash = HexFormatter.byteArrayToHex(md.digest()).toUpperCase();
                        final String hashStoredWithinFiles = hashes.get(source.getAbsolutePath());
                        if (hashStoredWithinFiles != null && !hashStoredWithinFiles.equals(calculatedHash)) {
                            for (ArchiveFile link : archive.getArchiveFiles()) {
                                if (link.getFilePath().equals(source.getAbsolutePath())) {
                                    archive.addCrcError(link);
                                    break;
                                }
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
        } catch (ExtSevenZipException e) {
            setException(e);
            logger.log(e);
            archive.setExitCode(e.getExitCode());
        } catch (SevenZipException e) {
            setException(e);
            logger.log(e);
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
            return;
        } catch (FileNotFoundException e) {
            controller.setExeption(e);
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_WRITE_ERROR);
            return;
        } catch (IOException e) {
            controller.setExeption(e);
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_WRITE_ERROR);
            return;
        } catch (NoSuchAlgorithmException e) {
            controller.setExeption(e);
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
            return;
        } finally {
            try {
                out.flush();
            } catch (Throwable e) {
            }
            try {
                out.close();
            } catch (Throwable e) {
            }
        }

        if (archive.getCrcError().size() > 0) {
            if (!FileCreationManager.getInstance().delete(file, null)) {
                logger.warning("Could not delete outputfile after crc error");
            }
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CRC_ERROR);
            return;
        }

        archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_SUCCESS);
    }

    @Override
    public boolean checkCommand() {
        return true;
    }

    @Override
    public int getCrackProgress() {
        return 100;
    }

    @Override
    public boolean prepare() {
        for (ArchiveFile l : archive.getArchiveFiles()) {
            files.add(l.getFilePath());
        }

        Collections.sort(files);

        file = new File(archive.getFirstArchiveFile().getFilePath().replaceFirst("\\.[\\d]+\\.xtm$", ""));
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(archive.getFirstArchiveFile().getFilePath()));
            // Skip useless bytes
            in.skip(40);

            // Original filename
            byte[] buffer = new byte[in.read()];
            in.read(buffer);
            in.skip(50 - buffer.length);

            String filename = new String(buffer, "UTF-8");

            // Set filename. If no filename was set, take the archivename
            if (filename != null && filename.trim().length() != 0)
                file = new File(file.getAbsolutePath().replace(file.getName(), filename));
            else {
                logger.warning("Could not read from XtremSplit file " + file.getAbsolutePath());
            }

            // MD5 Hashes are present
            md5 = in.read() == 1 ? true : false;

            // Get MD5 Hashes
            if (md5) {
                in.close();

                File f = new File(files.get(files.size() - 1));
                in = new BufferedInputStream(new FileInputStream(f));
                in.skip(f.length() - (32 * files.size()));
                buffer = new byte[32];

                for (int i = 0; i < files.size(); i++) {
                    in.read(buffer);
                    hashes.put(files.get(i), new String(buffer));
                }
            }
        } catch (IOException e) {
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
            return false;
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException e) {
                archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
                return false;
            }
        }
        return true;
    }

    public String getArchiveName(ArchiveFactory factory) {
        return createID(factory);
    }

    public boolean isArchivSupported(ArchiveFactory factory, boolean allowDeepInspection) {
        if (factory.getName().matches(".*\\.[\\d]+\\.xtm$")) return true;
        return false;
    }

    // public boolean isArchivSupportedFileFilter(String file) {
    // if (file.matches(".*\\.001\\.xtm$")) return true;
    // return false;
    // }

    public void close() {
    }

    public DummyArchive checkComplete(Archive archive) {
        int last = 1;
        List<String> missing = new ArrayList<String>();
        List<Integer> erg = new ArrayList<Integer>();

        Regex r = new Regex(archive.getFirstArchiveFile().getFilePath(), ".*\\.([\\d]+)\\.xtm$");
        int length = r.getMatch(0).length();
        String archivename = archive.getName();

        for (ArchiveFile l : archive.getArchiveFiles()) {
            String e = "";
            if ((e = new Regex(l.getFilePath(), ".*\\.([\\d]+)\\.xtm$").getMatch(0)) != null) {
                int p = Integer.parseInt(e);

                if (p > last) last = p;

                erg.add(p);
            }
        }

        for (int i = 1; i <= last; i++) {
            if (!erg.contains(i)) {
                String part = archivename + "." + StringFormatter.fillString(i + "", "0", "", length) + ".xtm";
                missing.add(part);
            }
        }

        return DummyArchive.create(archive, missing);
    }

    @Override
    public String createID(ArchiveFactory factory) {
        return factory.getName().replaceFirst("\\.[\\d]+\\.xtm$", "");
    }

}