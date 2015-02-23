package org.jdownloader.extensions.extraction.split;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.appwork.utils.Regex;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.FileSignatures;
import org.jdownloader.extensions.extraction.MissingArchiveFile;
import org.jdownloader.extensions.extraction.Signature;
import org.jdownloader.extensions.extraction.multi.ArchiveException;
import org.jdownloader.logging.LogController;

public enum SplitType {

    /**
     * Multipart XtremSplit Archive (.001.xtm, .002.xtm ...)
     */
    XTREMSPLIT {
        private final Pattern pattern = Pattern.compile("(?i)(.*)\\.(\\d+)\\.xtm$");

        @Override
        public boolean matches(String filePathOrName) {
            return filePathOrName != null && pattern.matcher(filePathOrName).matches();
        }

        @Override
        protected String buildIDPattern(String[] matches) {
            return "\\.\\d{" + matches[1].length() + "}\\.xtm";
        }

        @Override
        public Pattern buildArchivePattern(String[] matches) {
            final String pattern = "^" + Regex.escape(matches[0]) + buildIDPattern(matches) + "$";
            return Pattern.compile(pattern);
        }

        @Override
        public String[] getMatches(String filePathOrName) {
            return filePathOrName != null ? new Regex(filePathOrName, pattern).getRow(0) : null;
        }

        @Override
        public String getPartNumberString(String filePathOrName) {
            final String matches[] = getMatches(filePathOrName);
            return matches != null ? matches[1] : null;
        }

        @Override
        public int getPartNumber(String partNumberString) {
            return Integer.parseInt(partNumberString);
        }

        @Override
        protected int getFirstPartIndex() {
            return 1;
        }

        @Override
        protected int getMinimumNeededPartIndex() {
            return 2;
        }

        @Override
        protected String buildMissingPart(String[] matches, int partIndex, int partStringLength) {
            return matches[0] + "." + String.format(Locale.US, "%0" + partStringLength + "d", partIndex) + ".xtm";
        }

    },

    /**
     * Multipart Unix-Split Archive (.aa, .ab ...)
     */
    UNIX_SPLIT {
        private final Pattern pattern = Pattern.compile("(?i)(.*)\\.((?-i)[a-z][a-z])$");

        private int parseIndex(final String index) {
            final int x1 = index.charAt(0) - 'a';
            final int x2 = index.charAt(1) - 'a';
            return x1 * 26 + x2;
        }

        private String createIndex(final int index) {
            final char ret[] = new char[2];
            ret[0] = (char) ('a' + (index / 26));
            ret[1] = (char) ('a' + (index % 26));
            return String.copyValueOf(ret);
        }

        @Override
        public boolean matches(String filePathOrName) {
            return filePathOrName != null && pattern.matcher(filePathOrName).matches();
        }

        @Override
        protected String buildIDPattern(String[] matches) {
            return "\\.(?-i)[a-z][a-z]";
        }

        @Override
        public Pattern buildArchivePattern(String[] matches) {
            final String pattern = "^" + Regex.escape(matches[0]) + buildIDPattern(matches) + "$";
            return Pattern.compile(pattern);
        }

        @Override
        public String[] getMatches(String filePathOrName) {
            return filePathOrName != null ? new Regex(filePathOrName, pattern).getRow(0) : null;
        }

        @Override
        public String getPartNumberString(String filePathOrName) {
            final String matches[] = getMatches(filePathOrName);
            return matches != null ? matches[1] : null;
        }

        @Override
        public int getPartNumber(String partNumberString) {
            return parseIndex(partNumberString);
        }

        @Override
        protected int getFirstPartIndex() {
            return 0;
        }

        @Override
        protected int getMinimumNeededPartIndex() {
            return 1;
        }

        @Override
        protected String buildMissingPart(String[] matches, int partIndex, int partStringLength) {
            return matches[0] + "." + createIndex(partIndex);
        }

        @Override
        protected boolean looksLikeAnArchive(BitSet bitset) {
            int count = 0;
            final int js = parseIndex("js");
            final int xz = parseIndex("xz");
            for (int index = 0; index < bitset.length(); index++) {
                if (index == js || index == xz) {
                    /* exclude js,xz for validation */
                    continue;
                }
                if (bitset.get(index)) {
                    if (++count == 2) {
                        return true;
                    }
                } else {
                    count = 0;
                }
            }
            return false;
        }

    },

    /**
     * Multipart HJ-Split Archive (.001, .002 ...)
     */
    HJ_SPLIT {
        private final Pattern pattern = Pattern.compile("(?i)(.*)\\.([0-9]{3})$");

        @Override
        public boolean matches(String filePathOrName) {
            return filePathOrName != null && pattern.matcher(filePathOrName).matches();
        }

        @Override
        protected String buildIDPattern(String[] matches) {
            return "\\.[0-9]{3}";
        }

        @Override
        public Pattern buildArchivePattern(String[] matches) {
            final String pattern = "^" + Regex.escape(matches[0]) + buildIDPattern(matches) + "$";
            return Pattern.compile(pattern);
        }

        @Override
        public String[] getMatches(String filePathOrName) {
            return filePathOrName != null ? new Regex(filePathOrName, pattern).getRow(0) : null;
        }

        @Override
        public String getPartNumberString(String filePathOrName) {
            final String matches[] = getMatches(filePathOrName);
            return matches != null ? matches[1] : null;
        }

        @Override
        public int getPartNumber(String partNumberString) {
            return Integer.parseInt(partNumberString);
        }

        @Override
        protected int getFirstPartIndex() {
            return 1;
        }

        @Override
        protected int getMinimumNeededPartIndex() {
            return 2;
        }

        @Override
        protected String buildMissingPart(String[] matches, int partIndex, int partStringLength) {
            return matches[0] + "." + String.format(Locale.US, "%0" + partStringLength + "d", partIndex);
        }

        @Override
        protected boolean looksLikeAnArchive(BitSet bitset) {
            int count = 0;
            for (int index = 0; index < bitset.length(); index++) {
                if (bitset.get(index)) {
                    if (++count == 2) {
                        return true;
                    }
                } else {
                    count = 0;
                }
            }
            return false;
        }

        @Override
        protected boolean isValidPart(int partIndex, ArchiveFile archiveFile) {
            if (archiveFile.exists()) {
                final String signatureString;
                try {
                    signatureString = FileSignatures.readFileSignature(new File(archiveFile.getFilePath()));
                } catch (IOException e) {
                    LogController.CL().log(e);
                    return false;
                }
                final Signature signature = new FileSignatures().getSignature(signatureString);
                if (partIndex == 1) {
                    if (signature != null) {
                        if ("7Z".equalsIgnoreCase(signature.getId())) {
                            /**
                             * ArchiveType.SEVENZIP_PARTS
                             */
                            return false;
                        } else if ("ZIP".equalsIgnoreCase(signature.getId())) {
                            /**
                             * ArchiveType.ZIP_MULTI
                             */
                            return false;
                        }
                    }
                } else {
                    if (signature != null && "RAR".equalsIgnoreCase(signature.getId())) {
                        /**
                         * ArchiveType.RAR_MULTI4
                         */
                        return false;
                    }
                }
            }
            return true;
        }
    },

    /**
     * Multipart Hacha-Split Archive (.0, .1 ...)
     */
    HACHA_SPLIT {
        private final Pattern pattern = Pattern.compile("(?i)(.*)\\.(\\d+)$");

        @Override
        public boolean matches(String filePathOrName) {
            return filePathOrName != null && pattern.matcher(filePathOrName).matches();
        }

        @Override
        protected String buildIDPattern(String[] matches) {
            return "\\.\\d+";
        }

        @Override
        public Pattern buildArchivePattern(String[] matches) {
            final String pattern = "^" + Regex.escape(matches[0]) + buildIDPattern(matches) + "$";
            return Pattern.compile(pattern);
        }

        @Override
        public String[] getMatches(String filePathOrName) {
            return filePathOrName != null ? new Regex(filePathOrName, pattern).getRow(0) : null;
        }

        @Override
        public String getPartNumberString(String filePathOrName) {
            final String matches[] = getMatches(filePathOrName);
            return matches != null ? matches[1] : null;
        }

        @Override
        public int getPartNumber(String partNumberString) {
            if (partNumberString.startsWith("0") && partNumberString.length() > 1) {
                return -1;
            }
            return Integer.parseInt(partNumberString);
        }

        @Override
        protected int getFirstPartIndex() {
            return 0;
        }

        @Override
        protected int getMinimumNeededPartIndex() {
            return 1;
        }

        @Override
        protected String buildMissingPart(String[] matches, int partIndex, int partStringLength) {
            return matches[0] + "." + partIndex;
        }

        @Override
        protected boolean looksLikeAnArchive(BitSet bitset) {
            int count = 0;
            for (int index = 0; index < bitset.length(); index++) {
                if (bitset.get(index)) {
                    if (++count == 2) {
                        return true;
                    }
                } else {
                    count = 0;
                }
            }
            return false;
        }

        @Override
        protected boolean isValidPart(int partIndex, ArchiveFile archiveFile) {
            if (partIndex == 0 && archiveFile.exists()) {
                return HachaSplit.parseHachaHeader(archiveFile) != null;
            }
            return true;
        }
    };

    public abstract boolean matches(final String filePathOrName);

    public abstract String[] getMatches(final String filePathOrName);

    public abstract Pattern buildArchivePattern(String[] matches);

    protected abstract String buildIDPattern(String[] matches);

    public abstract String getPartNumberString(final String filePathOrName);

    public abstract int getPartNumber(final String partNumberString);

    protected abstract int getFirstPartIndex();

    protected abstract int getMinimumNeededPartIndex();

    protected abstract String buildMissingPart(String[] matches, int partIndex, int partStringLength);

    protected boolean looksLikeAnArchive(BitSet bitset) {
        return bitset.size() != 0;
    }

    protected boolean isValidPart(int partIndex, ArchiveFile archiveFile) {
        return true;
    }

    public static String createArchiveName(ArchiveFactory factory, SplitType splitType) {
        final String name = factory.getName();
        final String[] matches = splitType.getMatches(name);
        if (matches != null) {
            return matches[0];
        }
        return null;
    }

    public static String createArchiveID(ArchiveFactory factory, SplitType splitType) {
        String name = factory.getName();
        final String[] matches = splitType.getMatches(name);
        if (matches != null) {
            name = matches[0];
            return "^" + Regex.escape(name) + splitType.buildIDPattern(matches).concat("$");
        }
        return null;
    }

    public static List<ArchiveFile> getMissingArchiveFiles(Archive archive, SplitType splitType, int numberOfParts) {
        final ArchiveFile link = archive.getFirstArchiveFile();
        if (link != null) {
            final String linkPath = link.getFilePath();
            final String[] filePathParts = splitType.getMatches(linkPath);
            if (filePathParts != null) {
                final BitSet availableParts = new BitSet();
                int partStringLength = 1;
                for (final ArchiveFile archiveFile : archive.getArchiveFiles()) {
                    final String fileName = archiveFile.getName();
                    final String partNumberString = splitType.getPartNumberString(fileName);
                    final int partNumber = splitType.getPartNumber(partNumberString);
                    if (partNumberString != null) {
                        partStringLength = Math.max(partStringLength, partNumberString.length());
                    }
                    if (partNumber >= 0) {
                        availableParts.set(partNumber);
                    }
                }
                final List<ArchiveFile> ret = new ArrayList<ArchiveFile>();
                /**
                 * some archives start at 0 (0...x-1)
                 * 
                 * some archives start at 1 (1....x)
                 */
                final int minimumParts = Math.max(splitType.getMinimumNeededPartIndex(), numberOfParts) - (1 - splitType.getFirstPartIndex());
                for (int partIndex = splitType.getFirstPartIndex(); partIndex <= minimumParts; partIndex++) {
                    if (availableParts.get(partIndex) == false) {
                        final File missingFile = new File(splitType.buildMissingPart(filePathParts, partIndex, partStringLength));
                        ret.add(new MissingArchiveFile(missingFile.getName(), missingFile.getAbsolutePath()));
                    }
                }
                return ret;
            }
        }
        return null;
    }

    public static Archive createArchive(ArchiveFactory link, SplitType splitType, boolean allowDeepInspection) throws ArchiveException {
        final String linkPath = link.getFilePath();
        final String[] filePathParts = splitType.getMatches(linkPath);
        if (filePathParts != null) {
            final Pattern pattern = splitType.buildArchivePattern(filePathParts);
            final List<ArchiveFile> foundArchiveFiles = link.createPartFileList(linkPath, pattern.pattern());
            if (foundArchiveFiles == null || foundArchiveFiles.size() == 0) {
                throw new ArchiveException("Broken archive support: " + link.getFilePath());
            }
            final BitSet availableParts = new BitSet();
            int lowestPartNumber = Integer.MAX_VALUE;
            int partStringLength = 1;
            int highestPartNumber = Integer.MIN_VALUE;
            final int archiveFilesGrow = 128;
            ArchiveFile[] archiveFiles = new ArchiveFile[archiveFilesGrow];
            for (final ArchiveFile archiveFile : foundArchiveFiles) {
                final String fileName = archiveFile.getName();
                final String partNumberString = splitType.getPartNumberString(fileName);
                final int partNumber = splitType.getPartNumber(partNumberString);
                if (partNumber >= 0) {
                    if (partNumberString != null) {
                        partStringLength = Math.max(partStringLength, partNumberString.length());
                    }
                    availableParts.set(partNumber);
                    if (partNumber >= archiveFiles.length) {
                        archiveFiles = Arrays.copyOf(archiveFiles, Math.max(archiveFiles.length + archiveFilesGrow, partNumber + 1));
                    }
                    archiveFiles[partNumber] = archiveFile;
                    if (partNumber < lowestPartNumber) {
                        /* lower part found, clear list and add to list */
                        lowestPartNumber = partNumber;
                    }
                    if (partNumber > highestPartNumber) {
                        highestPartNumber = partNumber;
                    }
                }
            }
            if (splitType.looksLikeAnArchive(availableParts)) {
                final String[] fileNameParts = splitType.getMatches(link.getName());
                final Archive archive = link.createArchive();
                archive.setName(fileNameParts[0]);
                final ArrayList<ArchiveFile> sortedArchiveFiles = new ArrayList<ArchiveFile>();
                final int minimumParts = Math.max(splitType.getMinimumNeededPartIndex(), highestPartNumber);
                ArchiveFile firstArchiveFile = null;
                for (int partIndex = splitType.getFirstPartIndex(); partIndex <= minimumParts; partIndex++) {
                    if (availableParts.get(partIndex) == false) {
                        final File missingFile = new File(splitType.buildMissingPart(filePathParts, partIndex, partStringLength));
                        sortedArchiveFiles.add(new MissingArchiveFile(missingFile.getName(), missingFile.getAbsolutePath()));
                    } else {
                        if (firstArchiveFile == null) {
                            firstArchiveFile = archiveFiles[partIndex];
                        }
                        if (allowDeepInspection && splitType.isValidPart(partIndex, archiveFiles[partIndex]) == false) {
                            return null;
                        }
                        sortedArchiveFiles.add(archiveFiles[partIndex]);
                    }
                }
                archive.setArchiveFiles(sortedArchiveFiles);
                archive.setFirstArchiveFile(firstArchiveFile);
                return archive;
            }
        }
        return null;
    }

}
