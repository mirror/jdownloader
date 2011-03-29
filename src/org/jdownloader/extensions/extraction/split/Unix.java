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
import java.util.Collections;
import java.util.List;

import jd.config.ConfigContainer;
import jd.gui.swing.jdgui.views.settings.panels.JSonWrapper;
import jd.plugins.DownloadLink;

import org.appwork.utils.Regex;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionControllerConstants;
import org.jdownloader.extensions.extraction.IExtraction;

/**
 * Joins Unixsplit files.
 * 
 * @author botzi
 * 
 */
public class Unix extends IExtraction {

    public Archive buildArchive(DownloadLink link) {
        String pattern = "^" + Regex.escape(link.getFileOutput().replaceAll("(?i)\\.[a-z][a-z]$", "")) + "\\.[a-z][a-z]$";
        Archive a = SplitUtil.buildArchive(link, pattern, ".*\\.aa$");
        a.setExtractor(this);
        return a;
    }

    @Override
    public Archive buildDummyArchive(String file) {
        Archive a = SplitUtil.buildDummyArchive(file, ".*\\.[a-z][a-z]$", ".*\\.aa$");
        a.setExtractor(this);
        return a;
    }

    @Override
    public boolean findPassword(String password) {
        return true;
    }

    @Override
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

    public void initConfig(ConfigContainer config, JSonWrapper subConfig) {
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

}