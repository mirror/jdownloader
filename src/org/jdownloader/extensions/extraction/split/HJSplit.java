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
import java.util.ArrayList;
import java.util.List;

import jd.config.ConfigContainer;
import jd.controlling.JSonWrapper;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.StringFormatter;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.ExtractionControllerConstants;
import org.jdownloader.extensions.extraction.IExtraction;
import org.jdownloader.extensions.extraction.bindings.file.FileArchiveFactory;

/**
 * Joins HJSplit files.
 * 
 * @author botzi
 * 
 */
public class HJSplit extends IExtraction {

    public Archive buildArchive(ArchiveFactory link) {
        String pattern = "^" + Regex.escape(link.getFilePath().replaceAll("(?i)\\.[\\d]+$", "")) + "\\.[\\d]+$";
        Archive a = SplitUtil.buildArchive(link, pattern, ".*\\.001$");
        a.setExtractor(this);
        a.setName(getArchiveName(new File(link.getFilePath()).getName()));
        return a;
    }

    // @Override
    // public Archive buildDummyArchive(String file) {
    // Archive a = SplitUtil.buildDummyArchive(file, ".*\\.[\\d]+$",
    // ".*\\.001$");
    // a.setExtractor(this);
    // return a;
    // }

    @Override
    public boolean findPassword(String password) {
        return true;
    }

    @Override
    public void extract() {
        File f = archive.getFactory().toFile(archive.getFirstArchiveFile().getFilePath().replaceFirst("\\.[\\d]+$", ""));
        String extension = SplitUtil.getCutKillerExtension(archive.getFactory().toFile(archive.getFirstArchiveFile().getFilePath()), archive.getArchiveFiles().size());
        boolean ret;

        if (extension != null) {
            f = new File(f.getAbsolutePath() + "." + extension);
            ret = SplitUtil.merge(controller, f, 8, config);
        } else {
            ret = SplitUtil.merge(controller, f, 0, config);
        }

        if (ret) {
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_SUCCESS);
        }
    }

    public boolean checkCommand() {
        return true;
    }

    public int getCrackProgress() {
        return 100;
    }

    public boolean prepare() {
        return true;
    }

    public void initConfig(ConfigContainer config, JSonWrapper subConfig) {
    }

    public String getArchiveName(String filename) {
        return createID(filename);

    }

    public boolean isArchivSupported(String file) {
        if (file.matches(".*\\.[\\d]+$")) {
            if (file.matches("(?i).*\\.7z\\.\\d+$")) {
                return false;
            } else {
                // TODO

                Archive a = buildArchive(new FileArchiveFactory(archive.getFactory().toFile(file)));
                if (a.getFirstArchiveFile() == null || a.getArchiveFiles().size() <= 1) {
                    return false;
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isArchivSupportedFileFilter(String file) {
        if (file.matches(".*\\.001$")) {
            if (file.matches("(?i).*\\.7z\\.001$")) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    public void close() {
    }

    public List<String> checkComplete(Archive archive) {
        int last = 1;
        List<String> missing = new ArrayList<String>();
        List<Integer> erg = new ArrayList<Integer>();

        Regex r = new Regex(archive.getFirstArchiveFile().getFilePath(), ".*\\.([\\d]+)$");
        int length = r.getMatch(0).length();
        String archivename = archive.getName();

        for (ArchiveFile l : archive.getArchiveFiles()) {
            String e = "";
            if ((e = new Regex(l.getFilePath(), ".*\\.([\\d]+)$").getMatch(0)) != null) {
                int p = Integer.parseInt(e);

                if (p > last) last = p;

                erg.add(p);
            }
        }

        for (int i = 1; i <= last; i++) {
            if (!erg.contains(i)) {
                String part = archivename + "." + StringFormatter.fillString(i + "", "0", "", length);
                missing.add(part);
            }
        }

        return missing;
    }

    @Override
    public String createID(String filename) {
        return filename.replaceFirst("\\.[\\d]+$", "");
    }

    @Override
    public boolean isMultiPartArchive(String filename) {
        return true;
    }

}