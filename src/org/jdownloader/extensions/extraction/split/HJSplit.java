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
import java.io.IOException;
import java.util.List;

import jd.utils.JDHexUtils;

import org.appwork.utils.Regex;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.DummyArchiveFile;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionControllerConstants;
import org.jdownloader.extensions.extraction.ExtractionControllerException;
import org.jdownloader.extensions.extraction.FileSignatures;
import org.jdownloader.extensions.extraction.IExtraction;
import org.jdownloader.extensions.extraction.MissingArchiveFile;
import org.jdownloader.extensions.extraction.multi.ArchiveException;
import org.jdownloader.extensions.extraction.multi.CheckException;

/**
 * Joins HJSplit files.
 *
 * @author botzi
 *
 */
public class HJSplit extends IExtraction {

    private final SplitType splitType = SplitType.HJ_SPLIT;

    public Archive buildArchive(ArchiveFactory link, boolean allowDeepInspection) throws ArchiveException {
        return SplitType.createArchive(link, splitType, allowDeepInspection);
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
                final String fileName;
                final int skipBytes;
                final String signature = JDHexUtils.toString(FileSignatures.readFileSignature(new File(archive.getFirstArchiveFile().getFilePath())));
                if (new Regex(signature, "^[\\w]{3}  \\d{3}").matches()) {
                    final String extension = new Regex(signature, "^([\\w]{3})").getMatch(0);
                    fileName = matches[0] + "." + extension;
                    skipBytes = 8;
                } else {
                    skipBytes = 0;
                    fileName = matches[0];
                }
                if (SplitUtil.merge(controller, fileName, skipBytes, getConfig())) {
                    archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_SUCCESS);
                } else {
                    if (archive.getExitCode() == -1) {
                        archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
                    }
                }
                return;
            } catch (ExtractionControllerException e) {
                archive.setExitCode(e.getExitCode());
            } catch (IOException e) {
                archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
            }
        } else {
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
        }
    }

    public boolean isAvailable() {
        return true;
    }

    public int getCrackProgress() {
        return 100;
    }

    public boolean prepare() {
        return true;
    }

    public void close() {
    }

    public DummyArchive checkComplete(Archive archive) throws CheckException {
        if (archive.getSplitType() == splitType) {
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
                    if (archive.getFirstArchiveFile().exists()) {
                        final String signature = JDHexUtils.toString(FileSignatures.readFileSignature(new File(firstArchiveFile)));
                        if (new Regex(signature, "^[\\w]{3}  \\d{3}").matches()) {
                            /**
                             * cutkiller header: extension and number of files
                             */
                            final String numberOfPartsString = new Regex(signature, "^[\\w]{3}  (\\d{3})").getMatch(0);
                            final int numberOfParts = Integer.parseInt(numberOfPartsString);
                            final List<ArchiveFile> missingArchiveFiles = SplitType.getMissingArchiveFiles(archive, splitType, numberOfParts);
                            if (missingArchiveFiles != null) {
                                for (ArchiveFile missingArchiveFile : missingArchiveFiles) {
                                    ret.add(new DummyArchiveFile(missingArchiveFile));
                                }
                            }
                            if (ret.getSize() < numberOfParts) {
                                throw new CheckException("Missing archiveParts(" + numberOfParts + "!=" + ret.getSize() + ") for Archive(" + archive.getName() + ")");
                            } else if (ret.getSize() > numberOfParts) {
                                throw new CheckException("Too many archiveParts(" + numberOfParts + "!=" + ret.getSize() + ") for Archive(" + archive.getName() + ")");
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

    @Override
    public Boolean isSupported(ArchiveFactory factory, boolean allowDeepInspection) {
        if (allowDeepInspection) {
            try {
                return SplitType.createArchive(factory, splitType, allowDeepInspection) != null;
            } catch (ArchiveException e) {
                getLogger().log(e);
                return false;
            }
        } else {
            return splitType.matches(factory.getFilePath());
        }
    }

}