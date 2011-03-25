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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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

/**
 * Joins Unixsplit files.
 * 
 * @author botzi
 * 
 */
public class Unix implements IExtraction {
    private Archive              archive;
    private ExtractionController controller;
    SubConfiguration             config;

    public Archive buildArchive(DownloadLink link) {
        String pattern = "^" + Regex.escape(link.getFileOutput().replaceAll("(?i)\\.[a-z][a-z]$", "")) + "\\.[a-z][a-z]$";
        Archive a = SplitUtil.buildArchive(link, pattern, ".*\\.aa$");
        a.setExtractor(this);
        return a;
    }

    public Archive buildDummyArchive(String file) {
        Archive a = SplitUtil.buildDummyArchive(file, ".*\\.[a-z][a-z]$", ".*\\.aa$");
        a.setExtractor(this);
        return a;
    }

    public boolean findPassword(String password) {
        return true;
    }

    public void extract() {
        File f = new File(archive.getFirstDownloadLink().getFileOutput().replaceFirst("\\.[a-z][a-z]$", ""));
        String extension = SplitUtil.getCutKillerExtension(new File(archive.getFirstDownloadLink().getFileOutput()), archive.getDownloadLinks().size());
        boolean ret;

        if (extension != null) {
            f = new File(f.getAbsolutePath() + "." + extension);
            ret = SplitUtil.merge(controller, f, 8);
        } else {
            ret = SplitUtil.merge(controller, f, 0);
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

    public void initConfig(ConfigContainer config, SubConfiguration subConfig) {
    }

    public void setArchiv(Archive archive) {
        this.archive = archive;
    }

    public void setExtractionController(ExtractionController controller) {
        this.controller = controller;
    }

    public String getArchiveName(DownloadLink link) {
        return new File(link.getFileOutput()).getName().replaceFirst("\\.[a-z][a-z]$", "");
    }

    public boolean isArchivSupported(String file) {
        if (file.matches(".*\\.aa$")) return true;
        return false;
    }

    public boolean isArchivSupportedFileFilter(String file) {
        if (file.matches(".*\\.aa$")) return true;
        return false;
    }

    public void setConfig(SubConfiguration config) {
        this.config = config;
    }

    public void close() {
    }

    public List<String> checkComplete(Archive archive) {
        List<String> missing = new ArrayList<String>();
        List<String> files = new ArrayList<String>();

        for (DownloadLink l : archive.getDownloadLinks()) {
            files.add(l.getFileOutput());
        }

        Collections.sort(files);

        String suffix = "aa";

        String archivename = getArchiveName(archive.getFirstDownloadLink()) + ".";

        for (String f : files) {
            while (!f.endsWith(suffix)) {
                missing.add(archivename + suffix);
                if (suffix.equals("zz")) break;
                suffix = countupSuffix(suffix);
            }

            if (suffix.equals("zz")) break;

            suffix = countupSuffix(suffix);
        }

        return missing;
    }

    /**
     * Gets the extension for the next file.
     * 
     * @param suffix
     *            Old extension.
     * @return New extension.
     */
    private String countupSuffix(String suffix) {
        String newSuffix = "";

        char first = suffix.charAt(0);
        char last = suffix.charAt(1);

        if (last + 1 > 'z') {
            first = (char) (first + 1);
            last = 'a';
        } else {
            last = (char) (last + 1);
        }

        newSuffix = String.valueOf(first) + String.valueOf(last);

        return newSuffix;
    }

    public void setLogger(Logger logger) {
    }
}