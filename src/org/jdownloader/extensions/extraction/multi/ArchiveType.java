package org.jdownloader.extensions.extraction.multi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import net.sf.sevenzipjbinding.ArchiveFormat;

import org.appwork.utils.Regex;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.FileSignatures;
import org.jdownloader.extensions.extraction.MissingArchiveFile;
import org.jdownloader.logging.LogController;

public enum ArchiveType {

    /**
     * DO NOT CHANGE ORDER: some archiveTypes share same extension!
     */

    /**
     * Multipart RAR Archive (.part01.rar, .part02.rar...)
     */
    RAR_MULTI {
        /**
         * naming, see http://www.win-rar.com/press/downloads/Split_Files.pdf and http://kb.winzip.com/kb/entry/154/
         */
        private final Pattern pattern = Pattern.compile("(?i)(.*)\\.(part|p)(\\.?)(\\d+)(.*?)\\.rar$");

        @Override
        public ArchiveFormat getArchiveFormat() {
            return ArchiveFormat.RAR;
        }

        @Override
        public boolean matches(String filePathOrName) {
            return filePathOrName != null && pattern.matcher(filePathOrName).matches();
        }

        @Override
        protected String buildIDPattern(String[] matches, final Boolean isMultiPart) {
            if (Boolean.FALSE.equals(isMultiPart)) {
                return null;
            } else {
                return "\\." + matches[1] + Regex.escape(matches[2]) + "\\d{" + matches[3].length() + "}" + Regex.escape(matches[4]) + "\\.rar";
            }
        }

        @Override
        public Pattern buildArchivePattern(String[] matches, final Boolean isMultiPart) {
            if (Boolean.FALSE.equals(isMultiPart)) {
                return null;
            } else {
                final String pattern = "^" + Regex.escape(matches[0]) + buildIDPattern(matches, isMultiPart) + "$";
                return Pattern.compile(pattern);
            }
        }

        @Override
        public String[] getMatches(String filePathOrName) {
            return filePathOrName != null ? new Regex(filePathOrName, pattern).getRow(0) : null;
        }

        @Override
        protected String getPartNumberString(String filePathOrName) {
            final String matches[] = getMatches(filePathOrName);
            return matches != null ? matches[3] : null;
        }

        @Override
        protected int getPartNumber(String partNumberString) {
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
            return matches[0] + "." + matches[1] + matches[2] + String.format(Locale.US, "%0" + partStringLength + "d", partIndex) + matches[4] + ".rar";
        }

        @Override
        protected Boolean isMultiPart(ArchiveFile archiveFile) {
            return RAR_SINGLE.isMultiPart(archiveFile);
        }

    },
    /**
     * Multipart RAR Archive (.000.rar, .001.rar...)
     */
    RAR_MULTI2 {

        private final Pattern pattern = Pattern.compile("(?i)(.*)\\.(\\d{3})\\.rar$");

        @Override
        public ArchiveFormat getArchiveFormat() {
            return ArchiveFormat.RAR;
        }

        @Override
        public boolean matches(String filePathOrName) {
            return filePathOrName != null && pattern.matcher(filePathOrName).matches();
        }

        @Override
        protected String buildIDPattern(String[] matches, final Boolean isMultiPart) {
            if (Boolean.FALSE.equals(isMultiPart)) {
                return null;
            } else {
                return "\\.\\d{3}\\.rar";
            }
        }

        @Override
        public Pattern buildArchivePattern(String[] matches, final Boolean isMultiPart) {
            if (Boolean.FALSE.equals(isMultiPart)) {
                return null;
            } else {
                final String pattern = "^" + Regex.escape(matches[0]) + buildIDPattern(matches, isMultiPart) + "$";
                return Pattern.compile(pattern);
            }
        }

        @Override
        public String[] getMatches(String filePathOrName) {
            return filePathOrName != null ? new Regex(filePathOrName, pattern).getRow(0) : null;
        }

        @Override
        protected String getPartNumberString(String filePathOrName) {
            final String matches[] = getMatches(filePathOrName);
            return matches != null ? matches[1] : null;
        }

        @Override
        protected int getPartNumber(String partNumberString) {
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
            return matches[0] + "." + String.format(Locale.US, "%0" + partStringLength + "d", partIndex) + ".rar";
        }

        @Override
        protected Boolean isMultiPart(ArchiveFile archiveFile) {
            return RAR_SINGLE.isMultiPart(archiveFile);
        }

    },
    /**
     * Multipart RAR Archive (.rar, .r00, .r01...)
     */
    RAR_MULTI3 {
        private final Pattern patternPart  = Pattern.compile("(?i)(.*)\\.r(\\d{2,})$");

        private final Pattern patternStart = Pattern.compile("(?i)(.*)\\.rar$");

        @Override
        public ArchiveFormat getArchiveFormat() {
            return ArchiveFormat.RAR;
        }

        @Override
        public boolean matches(String filePathOrName) {
            return filePathOrName != null && patternPart.matcher(filePathOrName).matches() || patternStart.matcher(filePathOrName).matches();
        }

        @Override
        protected String buildIDPattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.FALSE.equals(isMultiPart)) {
                return null;
            } else {
                return "\\.(r\\d{2,}|rar)";
            }
        }

        @Override
        public Pattern buildArchivePattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.FALSE.equals(isMultiPart)) {
                return null;
            } else {
                final String pattern = "^" + Regex.escape(matches[0]) + buildIDPattern(matches, isMultiPart) + "$";
                return Pattern.compile(pattern);
            }
        }

        @Override
        public String[] getMatches(String filePathOrName) {
            if (filePathOrName != null) {
                String matches[] = new Regex(filePathOrName, patternPart).getRow(0);
                if (matches == null) {
                    matches = new Regex(filePathOrName, patternStart).getRow(0);
                }
                return matches;
            }
            return null;
        }

        @Override
        protected String getPartNumberString(String filePathOrName) {
            final String matches[] = new Regex(filePathOrName, patternPart).getRow(0);
            return matches != null ? matches[1] : null;
        }

        @Override
        protected int getPartNumber(String partNumberString) {
            if (partNumberString == null) {
                return 0;
            } else {
                return Integer.parseInt(partNumberString) + 1;
            }
        }

        @Override
        protected int getFirstPartIndex() {
            return 0;
        }

        @Override
        protected int getMinimumNeededPartIndex() {
            return 0;
        }

        @Override
        protected String buildMissingPart(String[] matches, int partIndex, int partStringLength) {
            if (partIndex == 0) {
                return matches[0] + ".rar";
            } else {
                return matches[0] + ".r" + String.format(Locale.US, "%0" + partStringLength + "d", (partIndex - 1));
            }
        }

        @Override
        protected Boolean isMultiPart(ArchiveFile archiveFile) {
            return RAR_SINGLE.isMultiPart(archiveFile);
        }

    },

    /**
     * SinglePart RAR Archive (.rar) (.rar)
     */
    RAR_SINGLE {

        private final Pattern pattern = Pattern.compile("(?i)(.*)\\.rar$");

        @Override
        public ArchiveFormat getArchiveFormat() {
            return ArchiveFormat.RAR;
        }

        @Override
        public boolean matches(String filePathOrName) {
            return filePathOrName != null && pattern.matcher(filePathOrName).matches();
        }

        @Override
        protected String buildIDPattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.TRUE.equals(isMultiPart)) {
                return null;
            } else {
                return "\\.rar";
            }
        }

        @Override
        public Pattern buildArchivePattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.TRUE.equals(isMultiPart)) {
                return null;
            } else {
                final String pattern = "^" + Regex.escape(matches[0]) + buildIDPattern(matches, isMultiPart) + "$";
                return Pattern.compile(pattern);
            }
        }

        @Override
        public String[] getMatches(String filePathOrName) {
            return filePathOrName != null ? new Regex(filePathOrName, pattern).getRow(0) : null;
        }

        @Override
        protected String getPartNumberString(String filePathOrName) {
            return null;
        }

        @Override
        protected int getPartNumber(String partNumberString) {
            return 0;
        }

        @Override
        protected int getFirstPartIndex() {
            return 0;
        }

        @Override
        protected int getMinimumNeededPartIndex() {
            return 0;
        }

        @Override
        protected String buildMissingPart(String[] matches, int partIndex, int partStringLength) {
            return matches[0] + ".rar";
        }

        /**
         * http://www.forensicswiki.org/wiki/RAR
         *
         * http://www.rarlab.com/technote.htm#rarsign
         *
         * DOES ONLY CHECK VALID RAR FILE, NOT IF ITS A VOLUME FILE if partIndex >0
         */
        @Override
        protected Boolean isValidPart(int partIndex, ArchiveFile archiveFile) {
            if (archiveFile.exists()) {
                final String signatureString;
                try {
                    signatureString = FileSignatures.readFileSignature(new File(archiveFile.getFilePath()), 4);
                } catch (IOException e) {
                    LogController.CL().log(e);
                    return false;
                }
                if (signatureString.length() >= 8) {
                    return signatureString.startsWith("52617221");
                }
            }
            return null;
        }

        /**
         * http://www.forensicswiki.org/wiki/RAR
         *
         * http://www.rarlab.com/technote.htm#rarsign
         *
         * 0x24:23:22:21 indices (because of endianness)
         *
         * 0x0001 Volume/Archive
         *
         * 0x0010 New Volume naming scheme (.partN.rar)
         *
         * 0x0100 First Volume (only in RAR 3.0 and later)
         */
        @Override
        protected Boolean isMultiPart(ArchiveFile archiveFile) {
            if (archiveFile.exists()) {
                final String signatureString;
                try {
                    signatureString = FileSignatures.readFileSignature(new File(archiveFile.getFilePath()), 12);
                } catch (IOException e) {
                    LogController.CL().log(e);
                    return false;
                }
                if (signatureString.length() >= 8) {
                    if (signatureString.startsWith("52617221")) {
                        if (signatureString.length() >= 22) {
                            final int flag = Integer.parseInt(Character.toString(signatureString.charAt(21)), 16);
                            return flag > 0 && flag % 2 != 0;
                        }
                    }
                    return false;
                }
            }
            return null;
        }

    },

    /**
     * Multipart 7Zip Archive (.7z.001, 7z.002...)
     */
    SEVENZIP_PARTS {
        private final Pattern pattern = Pattern.compile("(?i)(.*)\\.7z\\.(\\d{1,4})$");

        @Override
        public ArchiveFormat getArchiveFormat() {
            return ArchiveFormat.SEVEN_ZIP;
        }

        @Override
        public boolean matches(String filePathOrName) {
            return filePathOrName != null && pattern.matcher(filePathOrName).matches();
        }

        @Override
        protected String buildIDPattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.FALSE.equals(isMultiPart)) {
                return null;
            } else {
                return "\\.7z\\.\\d{" + matches[1].length() + "}";
            }
        }

        @Override
        public Pattern buildArchivePattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.FALSE.equals(isMultiPart)) {
                return null;
            } else {
                final String pattern = "^" + Regex.escape(matches[0]) + buildIDPattern(matches, null) + "$";
                return Pattern.compile(pattern);
            }
        }

        @Override
        public String[] getMatches(String filePathOrName) {
            return filePathOrName != null ? new Regex(filePathOrName, pattern).getRow(0) : null;
        }

        @Override
        protected String getPartNumberString(String filePathOrName) {
            final String matches[] = getMatches(filePathOrName);
            return matches != null ? matches[1] : null;
        }

        @Override
        protected int getPartNumber(String partNumberString) {
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
            return matches[0] + ".7z." + String.format(Locale.US, "%0" + partStringLength + "d", partIndex);
        }
    },

    /**
     * Multipart Zip Archive (.zip.001, .zip.002...)
     */
    ZIP_MULTI {
        private final Pattern pattern = Pattern.compile("(?i)(.*)\\.zip\\.(\\d{1,4})$");

        @Override
        public ArchiveFormat getArchiveFormat() {
            return ArchiveFormat.ZIP;
        }

        @Override
        public boolean matches(String filePathOrName) {
            return filePathOrName != null && pattern.matcher(filePathOrName).matches();
        }

        @Override
        protected String buildIDPattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.FALSE.equals(isMultiPart)) {
                return null;
            } else {
                return "\\.zip\\.\\d{" + matches[1].length() + "}";
            }
        }

        @Override
        public Pattern buildArchivePattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.FALSE.equals(isMultiPart)) {
                return null;
            } else {
                final String pattern = "^" + Regex.escape(matches[0]) + buildIDPattern(matches, null) + "$";
                return Pattern.compile(pattern);
            }
        }

        @Override
        public String[] getMatches(String filePathOrName) {
            return filePathOrName != null ? new Regex(filePathOrName, pattern).getRow(0) : null;
        }

        @Override
        protected String getPartNumberString(String filePathOrName) {
            final String matches[] = getMatches(filePathOrName);
            return matches != null ? matches[1] : null;
        }

        @Override
        protected int getPartNumber(String partNumberString) {
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
            return matches[0] + ".zip." + String.format(Locale.US, "%0" + partStringLength + "d", partIndex);
        }
    },

    /**
     * SinglePart 7zip Archive (.7z)
     */
    SEVENZIP_SINGLE {
        private final Pattern pattern = Pattern.compile("(?i)(.*)\\.7z$");

        @Override
        public ArchiveFormat getArchiveFormat() {
            return ArchiveFormat.SEVEN_ZIP;
        }

        @Override
        public boolean matches(String filePathOrName) {
            return filePathOrName != null && pattern.matcher(filePathOrName).matches();
        }

        @Override
        protected String buildIDPattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.TRUE.equals(isMultiPart)) {
                return null;
            } else {
                return "\\.7z";
            }
        }

        @Override
        public Pattern buildArchivePattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.TRUE.equals(isMultiPart)) {
                return null;
            } else {
                final String pattern = "^" + Regex.escape(matches[0]) + buildIDPattern(matches, null) + "$";
                return Pattern.compile(pattern);
            }
        }

        @Override
        public String[] getMatches(String filePathOrName) {
            return filePathOrName != null ? new Regex(filePathOrName, pattern).getRow(0) : null;
        }

        @Override
        protected String getPartNumberString(String filePathOrName) {
            return null;
        }

        @Override
        protected int getPartNumber(String partNumberString) {
            return 0;
        }

        @Override
        protected int getFirstPartIndex() {
            return 0;
        }

        @Override
        protected int getMinimumNeededPartIndex() {
            return 0;
        }

        @Override
        protected String buildMissingPart(String[] matches, int partIndex, int partStringLength) {
            return matches[0] + ".7z";
        }

    },

    /**
     * SinglePart Zip Archive (.7z)
     */
    ZIP_SINGLE {
        private final Pattern pattern = Pattern.compile("(?i)(.*)\\.zip$");

        @Override
        public ArchiveFormat getArchiveFormat() {
            return ArchiveFormat.ZIP;
        }

        @Override
        public boolean matches(String filePathOrName) {
            return filePathOrName != null && pattern.matcher(filePathOrName).matches();
        }

        @Override
        protected String buildIDPattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.TRUE.equals(isMultiPart)) {
                return null;
            } else {
                return "\\.zip";
            }
        }

        @Override
        public Pattern buildArchivePattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.TRUE.equals(isMultiPart)) {
                return null;
            } else {
                final String pattern = "^" + Regex.escape(matches[0]) + buildIDPattern(matches, null) + "$";
                return Pattern.compile(pattern);
            }
        }

        @Override
        public String[] getMatches(String filePathOrName) {
            return filePathOrName != null ? new Regex(filePathOrName, pattern).getRow(0) : null;
        }

        @Override
        protected String getPartNumberString(String filePathOrName) {
            return null;
        }

        @Override
        protected int getPartNumber(String partNumberString) {
            return 0;
        }

        @Override
        protected int getFirstPartIndex() {
            return 0;
        }

        @Override
        protected int getMinimumNeededPartIndex() {
            return 0;
        }

        @Override
        protected String buildMissingPart(String[] matches, int partIndex, int partStringLength) {
            return matches[0] + ".zip";
        }
    },

    /**
     * SinglePart LZH Archive (.lzh or .lha)
     */
    LZH_SINGLE {
        private final Pattern pattern = Pattern.compile("(?i)(.*)\\.(lha|lzh)$");

        @Override
        public ArchiveFormat getArchiveFormat() {
            return ArchiveFormat.LZH;
        }

        @Override
        public boolean matches(String filePathOrName) {
            return filePathOrName != null && pattern.matcher(filePathOrName).matches();
        }

        @Override
        protected String buildIDPattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.TRUE.equals(isMultiPart)) {
                return null;
            } else {
                return "\\." + matches[1];
            }
        }

        @Override
        public Pattern buildArchivePattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.TRUE.equals(isMultiPart)) {
                return null;
            } else {
                final String pattern = "^" + Regex.escape(matches[0]) + buildIDPattern(matches, null) + "$";
                return Pattern.compile(pattern);
            }
        }

        @Override
        public String[] getMatches(String filePathOrName) {
            return filePathOrName != null ? new Regex(filePathOrName, pattern).getRow(0) : null;
        }

        @Override
        protected String getPartNumberString(String filePathOrName) {
            return null;
        }

        @Override
        protected int getPartNumber(String partNumberString) {
            return 0;
        }

        @Override
        protected int getFirstPartIndex() {
            return 0;
        }

        @Override
        protected int getMinimumNeededPartIndex() {
            return 0;
        }

        @Override
        protected String buildMissingPart(String[] matches, int partIndex, int partStringLength) {
            return matches[0] + "." + matches[1];
        }
    },

    /**
     * SinglePart LZH Archive (.tar)
     */
    TAR_SINGLE {
        private final Pattern pattern = Pattern.compile("(?i)(.*)\\.tar$");

        @Override
        public ArchiveFormat getArchiveFormat() {
            return ArchiveFormat.TAR;
        }

        @Override
        public boolean matches(String filePathOrName) {
            return filePathOrName != null && pattern.matcher(filePathOrName).matches();
        }

        @Override
        protected String buildIDPattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.TRUE.equals(isMultiPart)) {
                return null;
            } else {
                return "\\.tar";
            }
        }

        @Override
        public Pattern buildArchivePattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.TRUE.equals(isMultiPart)) {
                return null;
            } else {
                final String pattern = "^" + Regex.escape(matches[0]) + buildIDPattern(matches, null) + "$";
                return Pattern.compile(pattern);
            }
        }

        @Override
        public String[] getMatches(String filePathOrName) {
            return filePathOrName != null ? new Regex(filePathOrName, pattern).getRow(0) : null;
        }

        @Override
        protected String getPartNumberString(String filePathOrName) {
            return null;
        }

        @Override
        protected int getPartNumber(String partNumberString) {
            return 0;
        }

        @Override
        protected int getFirstPartIndex() {
            return 0;
        }

        @Override
        protected int getMinimumNeededPartIndex() {
            return 0;
        }

        @Override
        protected String buildMissingPart(String[] matches, int partIndex, int partStringLength) {
            return matches[0] + ".tar";
        }
    },

    /**
     * SinglePart ARJ Archive (.arj)
     */
    ARJ_SINGLE {
        private final Pattern pattern = Pattern.compile("(?i)(.*)\\.arj$");

        @Override
        public ArchiveFormat getArchiveFormat() {
            return ArchiveFormat.ARJ;
        }

        @Override
        public boolean matches(String filePathOrName) {
            return filePathOrName != null && pattern.matcher(filePathOrName).matches();
        }

        @Override
        protected String buildIDPattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.TRUE.equals(isMultiPart)) {
                return null;
            } else {
                return "\\.arj";
            }
        }

        @Override
        public Pattern buildArchivePattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.TRUE.equals(isMultiPart)) {
                return null;
            } else {
                final String pattern = "^" + Regex.escape(matches[0]) + buildIDPattern(matches, null) + "$";
                return Pattern.compile(pattern);
            }
        }

        @Override
        public String[] getMatches(String filePathOrName) {
            return filePathOrName != null ? new Regex(filePathOrName, pattern).getRow(0) : null;
        }

        @Override
        protected String getPartNumberString(String filePathOrName) {
            return null;
        }

        @Override
        protected int getPartNumber(String partNumberString) {
            return 0;
        }

        @Override
        protected int getFirstPartIndex() {
            return 0;
        }

        @Override
        protected int getMinimumNeededPartIndex() {
            return 0;
        }

        @Override
        protected String buildMissingPart(String[] matches, int partIndex, int partStringLength) {
            return matches[0] + ".arj";
        }
    },
    /**
     * SinglePart CPIO Archive (.cpio)
     */
    CPIO_SINGLE {
        private final Pattern pattern = Pattern.compile("(?i)(.*)\\.cpio$");

        @Override
        public ArchiveFormat getArchiveFormat() {
            return ArchiveFormat.CPIO;
        }

        @Override
        public boolean matches(String filePathOrName) {
            return filePathOrName != null && pattern.matcher(filePathOrName).matches();
        }

        @Override
        protected String buildIDPattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.TRUE.equals(isMultiPart)) {
                return null;
            } else {
                return "\\.cpio";
            }
        }

        @Override
        public Pattern buildArchivePattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.TRUE.equals(isMultiPart)) {
                return null;
            } else {
                final String pattern = "^" + Regex.escape(matches[0]) + buildIDPattern(matches, null) + "$";
                return Pattern.compile(pattern);
            }
        }

        @Override
        public String[] getMatches(String filePathOrName) {
            return filePathOrName != null ? new Regex(filePathOrName, pattern).getRow(0) : null;
        }

        @Override
        protected String getPartNumberString(String filePathOrName) {
            return null;
        }

        @Override
        protected int getPartNumber(String partNumberString) {
            return 0;
        }

        @Override
        protected int getFirstPartIndex() {
            return 0;
        }

        @Override
        protected int getMinimumNeededPartIndex() {
            return 0;
        }

        @Override
        protected String buildMissingPart(String[] matches, int partIndex, int partStringLength) {
            return matches[0] + ".cpio";
        }
    },
    /**
     * SinglePart Tar.GZ Archive (.tgz)
     */
    TGZ_SINGLE {
        private final Pattern pattern = Pattern.compile("(?i)(.*)\\.tgz$");

        @Override
        public ArchiveFormat getArchiveFormat() {
            return ArchiveFormat.GZIP;
        }

        @Override
        public boolean matches(String filePathOrName) {
            return filePathOrName != null && pattern.matcher(filePathOrName).matches();
        }

        @Override
        protected String buildIDPattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.TRUE.equals(isMultiPart)) {
                return null;
            } else {
                return "\\.tgz";
            }
        }

        @Override
        public Pattern buildArchivePattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.TRUE.equals(isMultiPart)) {
                return null;
            } else {
                final String pattern = "^" + Regex.escape(matches[0]) + buildIDPattern(matches, null) + "$";
                return Pattern.compile(pattern);
            }
        }

        @Override
        public String[] getMatches(String filePathOrName) {
            return filePathOrName != null ? new Regex(filePathOrName, pattern).getRow(0) : null;
        }

        @Override
        protected String getPartNumberString(String filePathOrName) {
            return null;
        }

        @Override
        protected int getPartNumber(String partNumberString) {
            return 0;
        }

        @Override
        protected int getFirstPartIndex() {
            return 0;
        }

        @Override
        protected int getMinimumNeededPartIndex() {
            return 0;
        }

        @Override
        protected String buildMissingPart(String[] matches, int partIndex, int partStringLength) {
            return matches[0] + ".tgz";
        }
    },
    /**
     * SinglePart GZIP Archive (.gz)
     */
    GZIP_SINGLE {
        private final Pattern pattern = Pattern.compile("(?i)(.*)\\.gz$");

        @Override
        public ArchiveFormat getArchiveFormat() {
            return ArchiveFormat.GZIP;
        }

        @Override
        public boolean matches(String filePathOrName) {
            return filePathOrName != null && pattern.matcher(filePathOrName).matches();
        }

        @Override
        protected String buildIDPattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.TRUE.equals(isMultiPart)) {
                return null;
            } else {
                return "\\.gz";
            }
        }

        @Override
        public Pattern buildArchivePattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.TRUE.equals(isMultiPart)) {
                return null;
            } else {
                final String pattern = "^" + Regex.escape(matches[0]) + buildIDPattern(matches, null) + "$";
                return Pattern.compile(pattern);
            }
        }

        @Override
        public String[] getMatches(String filePathOrName) {
            return filePathOrName != null ? new Regex(filePathOrName, pattern).getRow(0) : null;
        }

        @Override
        protected String getPartNumberString(String filePathOrName) {
            return null;
        }

        @Override
        protected int getPartNumber(String partNumberString) {
            return 0;
        }

        @Override
        protected int getFirstPartIndex() {
            return 0;
        }

        @Override
        protected int getMinimumNeededPartIndex() {
            return 0;
        }

        @Override
        protected String buildMissingPart(String[] matches, int partIndex, int partStringLength) {
            return matches[0] + ".gz";
        }
    },
    /**
     * SinglePart BZIP2 Archive (.bz2)
     */
    BZIP2_SINGLE {
        private final Pattern pattern = Pattern.compile("(?i)(.*)\\.bz2$");

        @Override
        public ArchiveFormat getArchiveFormat() {
            return ArchiveFormat.BZIP2;
        }

        @Override
        public boolean matches(String filePathOrName) {
            return filePathOrName != null && pattern.matcher(filePathOrName).matches();
        }

        @Override
        protected String buildIDPattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.TRUE.equals(isMultiPart)) {
                return null;
            } else {
                return "\\.bz2";
            }
        }

        @Override
        public Pattern buildArchivePattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.TRUE.equals(isMultiPart)) {
                return null;
            } else {
                final String pattern = "^" + Regex.escape(matches[0]) + buildIDPattern(matches, null) + "$";
                return Pattern.compile(pattern);
            }
        }

        @Override
        public String[] getMatches(String filePathOrName) {
            return filePathOrName != null ? new Regex(filePathOrName, pattern).getRow(0) : null;
        }

        @Override
        protected String getPartNumberString(String filePathOrName) {
            return null;
        }

        @Override
        protected int getPartNumber(String partNumberString) {
            return 0;
        }

        @Override
        protected int getFirstPartIndex() {
            return 0;
        }

        @Override
        protected int getMinimumNeededPartIndex() {
            return 0;
        }

        @Override
        protected String buildMissingPart(String[] matches, int partIndex, int partStringLength) {
            return matches[0] + ".bz2";
        }
    },

    /**
     * Multipart RAR Archive Archive (.001, .002 ...) MUST BE LAST ONE!! DO NOT CHANGE ORDER
     */
    RAR_MULTI4 {
        private final Pattern pattern = Pattern.compile("(?i)(.*)\\.([0-9]{3})$");

        @Override
        public boolean matches(String filePathOrName) {
            return filePathOrName != null && pattern.matcher(filePathOrName).matches();
        }

        @Override
        protected String buildIDPattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.FALSE.equals(isMultiPart)) {
                return null;
            } else {
                return "\\.[0-9]{3}";
            }
        }

        @Override
        public Pattern buildArchivePattern(String[] matches, Boolean isMultiPart) {
            if (Boolean.FALSE.equals(isMultiPart)) {
                return null;
            } else {
                final String pattern = "^" + Regex.escape(matches[0]) + buildIDPattern(matches, null) + "$";
                return Pattern.compile(pattern);
            }
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
        public ArchiveFormat getArchiveFormat() {
            return ArchiveFormat.RAR;
        }

        @Override
        protected Boolean isValidPart(int partIndex, ArchiveFile archiveFile) {
            return RAR_SINGLE.isValidPart(partIndex, archiveFile);
        }

    };

    public abstract ArchiveFormat getArchiveFormat();

    public abstract boolean matches(final String filePathOrName);

    public abstract String[] getMatches(final String filePathOrName);

    public abstract Pattern buildArchivePattern(String[] matches, Boolean isMultiPart);

    protected abstract String buildIDPattern(String[] matches, Boolean isMultiPart);

    protected abstract String getPartNumberString(final String filePathOrName);

    protected abstract int getPartNumber(final String partNumberString);

    protected abstract int getFirstPartIndex();

    protected abstract int getMinimumNeededPartIndex();

    protected abstract String buildMissingPart(String[] matches, int partIndex, int partStringLength);

    protected boolean looksLikeAnArchive(BitSet bitset) {
        return bitset.size() != 0;
    }

    protected Boolean isValidPart(int partIndex, ArchiveFile archiveFile) {
        return null;
    }

    protected Boolean isMultiPart(final ArchiveFile archiveFile) {
        return null;
    }

    public static List<ArchiveFile> getMissingArchiveFiles(Archive archive, ArchiveType archiveType, int numberOfParts) {
        final ArchiveFile link = archive.getFirstArchiveFile();
        if (link != null) {
            final String linkPath = link.getFilePath();
            final String[] filePathParts = archiveType.getMatches(linkPath);
            if (filePathParts != null) {
                final BitSet availableParts = new BitSet();
                int partStringLength = 1;
                for (final ArchiveFile archiveFile : archive.getArchiveFiles()) {
                    final String fileName = archiveFile.getName();
                    final String partNumberString = archiveType.getPartNumberString(fileName);
                    final int partNumber = archiveType.getPartNumber(partNumberString);
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
                final int minimumParts = Math.max(archiveType.getMinimumNeededPartIndex(), numberOfParts) - (1 - archiveType.getFirstPartIndex());
                for (int partIndex = archiveType.getFirstPartIndex(); partIndex <= minimumParts; partIndex++) {
                    if (availableParts.get(partIndex) == false) {
                        final File missingFile = new File(archiveType.buildMissingPart(filePathParts, partIndex, partStringLength));
                        ret.add(new MissingArchiveFile(missingFile.getName(), missingFile.getAbsolutePath()));
                    }
                }
                return ret;
            }
        }
        return null;
    }

    public static Archive createArchive(final ArchiveFactory link, final boolean allowDeepInspection) throws ArchiveException {
        final String linkPath = link.getFilePath();
        archiveTypeLoop: for (final ArchiveType archiveType : values()) {
            final String[] filePathParts = archiveType.getMatches(linkPath);
            if (filePathParts != null) {
                final Boolean isMultiPart;
                if (allowDeepInspection) {
                    isMultiPart = archiveType.isMultiPart(link);
                } else {
                    isMultiPart = null;
                }
                final Pattern pattern = archiveType.buildArchivePattern(filePathParts, isMultiPart);
                if (pattern == null) {
                    continue archiveTypeLoop;
                }
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
                    final String partNumberString = archiveType.getPartNumberString(fileName);
                    final int partNumber = archiveType.getPartNumber(partNumberString);
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
                            lowestPartNumber = partNumber;
                        }
                        if (partNumber > highestPartNumber) {
                            highestPartNumber = partNumber;
                        }
                    }
                }
                if (archiveType.looksLikeAnArchive(availableParts)) {
                    final String[] fileNameParts = archiveType.getMatches(link.getName());
                    final Archive archive = link.createArchive();
                    archive.setName(fileNameParts[0]);
                    archive.setArchiveType(archiveType);
                    archive.setArchiveID(archiveType.name() + fileNameParts[0] + archiveType.buildIDPattern(fileNameParts, isMultiPart));
                    final ArrayList<ArchiveFile> sortedArchiveFiles = new ArrayList<ArchiveFile>();
                    final int minimumParts = Math.max(archiveType.getMinimumNeededPartIndex(), highestPartNumber);
                    ArchiveFile firstArchiveFile = null;
                    for (int partIndex = archiveType.getFirstPartIndex(); partIndex <= minimumParts; partIndex++) {
                        if (availableParts.get(partIndex) == false) {
                            final File missingFile = new File(archiveType.buildMissingPart(filePathParts, partIndex, partStringLength));
                            sortedArchiveFiles.add(new MissingArchiveFile(missingFile.getName(), missingFile.getAbsolutePath()));
                        } else {
                            if (firstArchiveFile == null) {
                                firstArchiveFile = archiveFiles[partIndex];
                            }
                            if (allowDeepInspection && Boolean.FALSE.equals(archiveType.isValidPart(partIndex, archiveFiles[partIndex]))) {
                                continue archiveTypeLoop;
                            }
                            sortedArchiveFiles.add(archiveFiles[partIndex]);
                        }
                    }
                    archive.setArchiveFiles(sortedArchiveFiles);
                    archive.setFirstArchiveFile(firstArchiveFile);
                    return archive;
                } else {
                    return null;
                }
            }
        }
        return null;
    }

}
