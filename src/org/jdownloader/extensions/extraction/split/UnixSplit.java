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

import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.DummyArchiveFile;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionControllerConstants;
import org.jdownloader.extensions.extraction.ExtractionControllerException;
import org.jdownloader.extensions.extraction.IExtraction;
import org.jdownloader.extensions.extraction.MissingArchiveFile;
import org.jdownloader.extensions.extraction.multi.ArchiveException;
import org.jdownloader.extensions.extraction.multi.CheckException;

public class UnixSplit extends IExtraction {

    private final SplitType splitType = SplitType.UNIX_SPLIT;

    public Archive buildArchive(ArchiveFactory link) throws ArchiveException {
        return SplitType.createArchive(link, splitType, false);
    }

    @Override
    public boolean findPassword(ExtractionController controller, String password, boolean optimized) {
        return true;
    }

    @Override
    public void extract(ExtractionController ctrl) {
        final Archive archive = getArchive();
        final String matches[] = splitType.getMatches(archive.getFirstArchiveFile().getName());
        if (matches != null) {
            try {
                if (SplitUtil.merge(controller, matches[0], 0, getConfig())) {
                    archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_SUCCESS);
                } else {
                    if (archive.getExitCode() == -1) {
                        archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
                    }
                }
                return;
            } catch (ExtractionControllerException e) {
                archive.setExitCode(e.getExitCode());
            }
        } else {
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
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

    public String getArchiveName(ArchiveFactory factory) {
        return SplitType.createArchiveName(factory, splitType);
    }

    public boolean isArchivSupported(ArchiveFactory factory, boolean allowDeepInspection) {
        try {
            return SplitType.createArchive(factory, splitType, allowDeepInspection) != null;
        } catch (ArchiveException e) {
            getLogger().log(e);
            return false;
        }
    }

    public void close() {
    }

    public DummyArchive checkComplete(Archive archive) throws CheckException {
        try {
            final DummyArchive ret = new DummyArchive(archive, splitType.name());
            boolean hasMissingArchiveFiles = false;
            for (ArchiveFile archiveFile : archive.getArchiveFiles()) {
                if (archiveFile instanceof MissingArchiveFile) {
                    hasMissingArchiveFiles = true;
                }
                ret.add(new DummyArchiveFile(archiveFile));
            }
            if (hasMissingArchiveFiles == false) {
                final String firstArchiveFile = archive.getFirstArchiveFile().getFilePath();
                final String partNumberOfFirstArchiveFile = splitType.getPartNumberString(firstArchiveFile);
                if (splitType.getFirstPartIndex() != splitType.getPartNumber(partNumberOfFirstArchiveFile)) {
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

    @Override
    public String createID(ArchiveFactory factory) {
        return SplitType.createArchiveID(factory, splitType);
    }

    @Override
    public boolean isMultiPartArchive(ArchiveFactory factory) {
        return true;
    }

    @Override
    public boolean isFileSupported(ArchiveFactory factory, boolean allowDeepInspection) {
        return splitType.matches(factory.getFilePath());
    }

}