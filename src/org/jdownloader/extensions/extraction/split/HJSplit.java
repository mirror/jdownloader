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

import jd.controlling.linkcrawler.CrawledLink;
import jd.plugins.DownloadLink;
import jd.utils.JDHexUtils;

import org.appwork.utils.Regex;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ExtensionsFilterInterface;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.DummyArchiveFile;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionControllerConstants;
import org.jdownloader.extensions.extraction.ExtractionControllerException;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.FileSignatures;
import org.jdownloader.extensions.extraction.IExtraction;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkFactory;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
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

    private final ExtractionExtension extension;

    public HJSplit(ExtractionExtension extension) {
        this.extension = extension;
    }

    @Override
    public void extract(ExtractionController ctrl) {
        final Archive archive = getExtractionController().getArchive();
        final ArchiveFile firstArchiveFile = archive.getArchiveFiles().get(0);
        final String matches[] = splitType.getMatches(firstArchiveFile.getName());
        if (matches != null) {
            try {
                final String fileName;
                final int skipBytes;
                final String signature = JDHexUtils.toString(FileSignatures.readFileSignature(new File(firstArchiveFile.getFilePath())));
                if (new Regex(signature, "^[\\w]{3}  \\d{3}").matches()) {
                    final String extension = new Regex(signature, "^([\\w]{3})").getMatch(0);
                    fileName = matches[0] + "." + extension;
                    skipBytes = 8;
                } else {
                    skipBytes = 0;
                    fileName = matches[0];
                }
                if (SplitUtil.merge(getExtractionController(), fileName, skipBytes, getConfig())) {
                    archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_SUCCESS);
                } else {
                    if (archive.getExitCode() == -1) {
                        archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
                    }
                }
                return;
            } catch (ExtractionControllerException e) {
                setException(e);
                archive.setExitCode(e.getExitCode());
            } catch (IOException e) {
                setException(e);
                archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
            }
        } else {
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
        }
    }

    public boolean isAvailable(ExtractionExtension extractionExtension) {
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
                final DummyArchive dummyArchive = new DummyArchive(archive, splitType);
                for (final ArchiveFile archiveFile : archive.getArchiveFiles()) {
                    dummyArchive.add(new DummyArchiveFile(archiveFile));
                }
                if (dummyArchive.isComplete()) {
                    final ArchiveFile firstFile = archive.getArchiveFiles().get(0);
                    final String firstArchiveFile = firstFile.getFilePath();
                    final String partNumberOfFirstArchiveFile = splitType.getPartNumberString(firstArchiveFile);
                    if (splitType.getFirstPartIndex() != splitType.getPartNumber(partNumberOfFirstArchiveFile)) {
                        throw new CheckException("Wrong firstArchiveFile(" + firstArchiveFile + ") for Archive(" + archive.getName() + ")");
                    }
                    if (firstFile.exists()) {
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
                                    dummyArchive.add(new DummyArchiveFile(missingArchiveFile));
                                }
                            }
                            if (dummyArchive.getSize() < numberOfParts) {
                                throw new CheckException("Missing archiveParts(" + numberOfParts + "!=" + dummyArchive.getSize() + ") for Archive(" + archive.getName() + ")");
                            } else if (dummyArchive.getSize() > numberOfParts) {
                                throw new CheckException("Too many archiveParts(" + numberOfParts + "!=" + dummyArchive.getSize() + ") for Archive(" + archive.getName() + ")");
                            }
                        } else {
                            SplitUtil.checkComplete(extension, archive, dummyArchive);
                        }
                    }
                }
                return dummyArchive;
            } catch (CheckException e) {
                throw e;
            } catch (Throwable e) {
                throw new CheckException("Cannot check Archive(" + archive.getName() + ")", e);
            }
        }
        return null;
    }

    @Override
    public Boolean isSupported(final ArchiveFactory factory, final boolean allowDeepInspection) {
        if (splitType.matches(factory.getFilePath())) {
            if (factory instanceof DownloadLinkArchiveFactory) {
                for (final DownloadLink link : ((DownloadLinkArchiveFactory) factory).getDownloadLinks()) {
                    final ExtensionsFilterInterface hint = CompiledFiletypeFilter.getExtensionsFilterInterface(link.getMimeHint());
                    if (hint != null && !hint.isSameExtensionGroup(CompiledFiletypeFilter.ArchiveExtensions.NUM)) {
                        return false;
                    }
                }
            } else if (factory instanceof CrawledLinkFactory) {
                for (final CrawledLink link : ((CrawledLinkFactory) factory).getLinks()) {
                    final DownloadLink dlLink = link.getDownloadLink();
                    if (dlLink != null) {
                        final ExtensionsFilterInterface hint = CompiledFiletypeFilter.getExtensionsFilterInterface(dlLink.getMimeHint());
                        if (hint != null && !hint.isSameExtensionGroup(CompiledFiletypeFilter.ArchiveExtensions.NUM)) {
                            return false;
                        }
                    }
                }
            }
            if (allowDeepInspection) {
                try {
                    return SplitType.createArchive(factory, splitType, allowDeepInspection) != null;
                } catch (ArchiveException e) {
                    getLogger().log(e);
                }
            } else {
                return true;
            }
        }
        return false;
    }
}