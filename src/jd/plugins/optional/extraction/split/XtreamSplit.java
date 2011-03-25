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

package jd.plugins.optional.extraction.split;

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
import java.util.logging.Logger;

import jd.config.ConfigContainer;
import jd.config.SubConfiguration;
import jd.plugins.DownloadLink;
import jd.plugins.optional.extraction.Archive;
import jd.plugins.optional.extraction.ExtractionController;
import jd.plugins.optional.extraction.ExtractionControllerConstants;
import jd.plugins.optional.extraction.IExtraction;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.formatter.StringFormatter;

/**
 * Joins XtreamSplit files.
 * 
 * @author botzi
 * 
 */
public class XtreamSplit implements IExtraction {
    private final static int        BUFFER = 2048;

    private Archive                 archive;
    private ExtractionController    controller;
    SubConfiguration                config;
    private boolean                 md5;
    private File                    file;
    private HashMap<String, String> hashes = new HashMap<String, String>();
    private List<String>            files  = new ArrayList<String>();
    private Logger                  logger;

    public Archive buildArchive(DownloadLink link) {
        String pattern = "^" + Regex.escape(link.getFileOutput().replaceAll("(?i)\\.[\\d]+\\.xtm$", "")) + "\\.[\\d]+\\.xtm$";
        Archive a = SplitUtil.buildArchive(link, pattern, ".*\\.001\\.xtm$");
        a.setExtractor(this);
        return a;
    }

    public Archive buildDummyArchive(String file) {
        Archive a = SplitUtil.buildDummyArchive(file, ".*\\.[\\d]+\\.xtm$", ".*\\.001\\.xtm$");
        a.setExtractor(this);
        return a;
    }

    public boolean findPassword(String password) {
        return true;
    }

    public void extract() {
        byte[] buffer = new byte[BUFFER];
        Archive archive = controller.getArchiv();

        BufferedOutputStream out = null;
        BufferedInputStream in = null;

        try {
            if (file.exists()) {
                if (archive.isOverwriteFiles()) {
                    if (!file.delete()) {
                        archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
                        return;
                    }
                } else {
                    archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_OUTPUTFILE_EXIST);
                    return;
                }
            }

            if ((!file.getParentFile().exists() && !file.getParentFile().mkdirs()) || !file.createNewFile()) {
                archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
                return;
            }

            archive.addExtractedFiles(file);

            out = new BufferedOutputStream(new FileOutputStream(file));
            MessageDigest md = null;

            for (int i = 0; i < files.size(); i++) {
                File source = new File(files.get(i));
                in = new BufferedInputStream(new FileInputStream(source));

                if (md5) md = MessageDigest.getInstance("md5");

                // Skip header
                long length = source.length();
                if (i == 0) {
                    in.skip(104);
                    length = length - 104;
                }

                long read = 0;

                // Skip md5 hashes
                if (md5 && i == (files.size() - 1)) {
                    length = length - 32 * files.size();
                }

                // Merge
                while (length > read) {
                    // Calaulate the read buffer for the remaining byte. Max
                    // BUFFER
                    int buf = ((length - read) < BUFFER) ? (int) (length - read) : BUFFER;

                    // Read data
                    int l = in.read(buffer, 0, buf);

                    // Write data
                    out.write(buffer, 0, l);
                    out.flush();

                    // Sum up bytes for control
                    archive.setExtracted(archive.getExtracted() + l);
                    read += l;

                    // Update MD5
                    if (md5) md.update(buffer, 0, l);
                }

                in.close();

                // Check MD5 hashes
                if (md5) {
                    if (hashes.get(source.getAbsolutePath()).equals(HexFormatter.byteArrayToHex(md.digest()).toUpperCase())) {
                        for (DownloadLink link : archive.getDownloadLinks()) {
                            if (link.getFileOutput().equals(source)) {
                                archive.addCrcError(link);
                                break;
                            }
                        }
                    }
                }
            }
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
                if (out != null) out.close();
                if (in != null) in.close();
            } catch (IOException e) {
                controller.setExeption(e);
                archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_WRITE_ERROR);
            }
        }

        if (archive.getCrcError().size() > 0) {
            if (!file.delete()) {
                logger.warning("Could not delete outputfile after crc error");
            }
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CRC_ERROR);
            return;
        }

        archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_SUCCESS);
    }

    public boolean checkCommand() {
        return true;
    }

    public int getCrackProgress() {
        return 100;
    }

    public boolean prepare() {
        for (DownloadLink l : archive.getDownloadLinks()) {
            files.add(l.getFileOutput());
        }

        Collections.sort(files);

        file = new File(archive.getFirstDownloadLink().getFileOutput().replaceFirst("\\.[\\d]+\\.xtm$", ""));
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(archive.getFirstDownloadLink().getFileOutput()));
            // Skip useless bytes
            in.skip(40);

            // Original filename
            byte[] buffer = new byte[in.read()];
            in.read(buffer);
            in.skip(50 - buffer.length);

            String filename = new String(buffer);

            // Set filename. If no filename was set, take the archivename
            if (filename != null && filename.trim() != "")
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

    public void initConfig(ConfigContainer config, SubConfiguration subConfig) {
    }

    public void setArchiv(Archive archive) {
        this.archive = archive;
    }

    public void setExtractionController(ExtractionController controller) {
        this.controller = controller;
    }

    public String getArchiveName(DownloadLink link) {
        return new File(link.getFileOutput()).getName().replaceFirst("\\.[\\d]+\\.xtm$", "");
    }

    public boolean isArchivSupported(String file) {
        if (file.matches(".*\\.[\\d]+\\.xtm$")) return true;
        return false;
    }

    public boolean isArchivSupportedFileFilter(String file) {
        if (file.matches(".*\\.001\\.xtm$")) return true;
        return false;
    }

    public void setConfig(SubConfiguration config) {
        this.config = config;
    }

    public void close() {
    }

    public List<String> checkComplete(Archive archive) {
        int last = 1;
        List<String> missing = new ArrayList<String>();
        List<Integer> erg = new ArrayList<Integer>();

        Regex r = new Regex(archive.getFirstDownloadLink().getFileOutput(), ".*\\.([\\d]+)\\.xtm$");
        int length = r.getMatch(0).length();
        String archivename = getArchiveName(archive.getFirstDownloadLink());

        for (DownloadLink l : archive.getDownloadLinks()) {
            String e = "";
            if ((e = new Regex(l.getFileOutput(), ".*\\.([\\d]+)\\.xtm$").getMatch(0)) != null) {
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

        return missing;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }
}