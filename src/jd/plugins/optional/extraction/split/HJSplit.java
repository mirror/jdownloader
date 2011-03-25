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
import org.appwork.utils.formatter.StringFormatter;

/**
 * Joins HJSplit files.
 * 
 * @author botzi
 * 
 */
public class HJSplit implements IExtraction {
    private Archive              archive;
    private ExtractionController controller;
    SubConfiguration             config;

    public Archive buildArchive(DownloadLink link) {
        String pattern = "^" + Regex.escape(link.getFileOutput().replaceAll("(?i)\\.[\\d]+$", "")) + "\\.[\\d]+$";
        Archive a = SplitUtil.buildArchive(link, pattern, ".*\\.001$");
        a.setExtractor(this);
        return a;
    }

    public Archive buildDummyArchive(String file) {
        Archive a = SplitUtil.buildDummyArchive(file, ".*\\.[\\d]+$", ".*\\.001$");
        a.setExtractor(this);
        return a;
    }

    public boolean findPassword(String password) {
        return true;
    }

    public void extract() {
        File f = new File(archive.getFirstDownloadLink().getFileOutput().replaceFirst("\\.[\\d]+$", ""));
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
        return new File(link.getFileOutput()).getName().replaceFirst("\\.[\\d]+$", "");
    }

    public boolean isArchivSupported(String file) {
        if (file.matches(".*\\.[\\d]+$")) {
            if (file.matches("(?i).*\\.7z\\.\\d+$")) {
                return false;
            } else {
                Archive a = buildDummyArchive(file);
                if (a.getFirstDownloadLink() == null || a.getDownloadLinks().size() <= 1) {
                    return false;
                } else {
                    return true;
                }
            }
        }
        return false;
    }

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

    public void setConfig(SubConfiguration config) {
        this.config = config;
    }

    public void close() {
    }

    public List<String> checkComplete(Archive archive) {
        int last = 1;
        List<String> missing = new ArrayList<String>();
        List<Integer> erg = new ArrayList<Integer>();

        Regex r = new Regex(archive.getFirstDownloadLink().getFileOutput(), ".*\\.([\\d]+)$");
        int length = r.getMatch(0).length();
        String archivename = getArchiveName(archive.getFirstDownloadLink());

        for (DownloadLink l : archive.getDownloadLinks()) {
            String e = "";
            if ((e = new Regex(l.getFileOutput(), ".*\\.([\\d]+)$").getMatch(0)) != null) {
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

    public void setLogger(Logger logger) {
    }
}