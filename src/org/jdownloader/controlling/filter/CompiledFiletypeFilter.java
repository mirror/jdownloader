package org.jdownloader.controlling.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.FiletypeFilter.TypeMatchType;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

import jd.plugins.LinkInfo;

public class CompiledFiletypeFilter {
    private final Pattern[]                   list;
    private final ExtensionsFilterInterface[] filterInterfaces;
    private final TypeMatchType               matchType;

    public TypeMatchType getMatchType() {
        return matchType;
    }

    public static interface CompiledFiletypeExtension extends ExtensionsFilterInterface {
        public boolean matchesMimeType(final String mimeType);

        public String getExtensionFromMimeType(final String mimeType);

        public boolean isValidExtension(String extension);

        public String name();
    }

    // https://www.htmlstrip.com/mime-file-type-checker
    public static interface ExtensionsFilterInterface {
        public Pattern compiledAllPattern();

        public String getDesc();

        public String getIconID();

        public Pattern getPattern();

        public String name();

        public boolean isSameExtensionGroup(ExtensionsFilterInterface extension);

        public ExtensionsFilterInterface[] listSameGroup();

        public ExtensionsFilterInterface getSource();
    }

    private static List<CompiledFiletypeExtension> EXTENSIONSFILTERINTERFACES = init();

    private static List<CompiledFiletypeExtension> init() {
        final List<CompiledFiletypeExtension> ret = new ArrayList<CompiledFiletypeExtension>();
        ret.addAll(Arrays.asList(VideoExtensions.values()));
        ret.addAll(Arrays.asList(AudioExtensions.values()));
        ret.addAll(Arrays.asList(ExecutableExtensions.values()));
        ret.addAll(Arrays.asList(HashExtensions.values()));
        ret.addAll(Arrays.asList(DocumentExtensions.values()));
        ret.addAll(Arrays.asList(ImageExtensions.values()));
        ret.addAll(Arrays.asList(SubtitleExtensions.values()));
        ret.addAll(Arrays.asList(ArchiveExtensions.values()));
        return ret;
    }

    public static CompiledFiletypeExtension getExtensionsFilterInterface(String fileExtension) {
        if (fileExtension == null) {
            return null;
        }
        /* Correct given file-extension if it starts with dot. */
        if (fileExtension.startsWith(".")) {
            fileExtension = fileExtension.substring(1);
        }
        for (final CompiledFiletypeExtension extension : EXTENSIONSFILTERINTERFACES) {
            final Pattern pattern = extension.getPattern();
            if (pattern != null && pattern.matcher(fileExtension).matches()) {
                return extension;
            }
        }
        return null;
    }

    public static List<CompiledFiletypeExtension> getByMimeType(final String mimeType) {
        if (mimeType == null) {
            return null;
        }
        final List<CompiledFiletypeExtension> ret = new ArrayList<CompiledFiletypeExtension>();
        for (final CompiledFiletypeExtension extension : EXTENSIONSFILTERINTERFACES) {
            if (extension.matchesMimeType(mimeType)) {
                ret.add(extension);
            }
        }
        return ret;
    }

    public static enum HashExtensions implements CompiledFiletypeExtension {
        SFV,
        MD5,
        SHA,
        SHA256,
        SHA512,
        PAR2("(vol\\d+\\.par2|vol\\d+\\+\\d+\\.par2|par2)"),
        PAR("(p\\d+|par)");

        private final Pattern  pattern;
        private static Pattern allPattern;

        public Pattern getPattern() {
            return pattern;
        }

        public ExtensionsFilterInterface getSource() {
            return this;
        }

        private HashExtensions() {
            pattern = Pattern.compile(name(), Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }

        private HashExtensions(String id) {
            this.pattern = Pattern.compile(id, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }

        public String getDesc() {
            return _GUI.T.FilterRuleDialog_createTypeFilter_mime_checksums();
        }

        public String getIconID() {
            return IconKey.ICON_HASHSUM;
        }

        public boolean isValidExtension(String extension) {
            return CompiledFiletypeFilter.isValidExtension(extension, this);
        }

        public Pattern compiledAllPattern() {
            if (allPattern == null) {
                allPattern = compileAllPattern(HashExtensions.values());
            }
            return allPattern;
        }

        @Override
        public boolean isSameExtensionGroup(ExtensionsFilterInterface extension) {
            return extension != null && extension instanceof HashExtensions;
        }

        @Override
        public ExtensionsFilterInterface[] listSameGroup() {
            return values();
        }

        @Override
        public boolean matchesMimeType(String mimeType) {
            return false;
        }

        @Override
        public String getExtensionFromMimeType(String mimeType) {
            return CompiledFiletypeFilter.getExtensionFromMimeType(mimeType, this);
        }
    }

    public static enum ExecutableExtensions implements CompiledFiletypeExtension {
        BAT,
        EXE,
        MSI,
        JAR,
        VBS,
        APK,
        APP,
        BIN,
        RUN,
        PS1,
        CMD;

        private final Pattern  pattern;
        private static Pattern allPattern;

        public Pattern getPattern() {
            return pattern;
        }

        public ExtensionsFilterInterface getSource() {
            return this;
        }

        private ExecutableExtensions() {
            pattern = Pattern.compile(name(), Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }

        private ExecutableExtensions(String id) {
            this.pattern = Pattern.compile(id, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }

        public String getDesc() {
            return _GUI.T.FilterRuleDialog_createTypeFilter_mime_executable();
        }

        public String getIconID() {
            return IconKey.ICON_DESKTOP;
        }

        public Pattern compiledAllPattern() {
            if (allPattern == null) {
                allPattern = compileAllPattern(ExecutableExtensions.values());
            }
            return allPattern;
        }

        @Override
        public boolean isSameExtensionGroup(ExtensionsFilterInterface extension) {
            return extension != null && extension instanceof ExecutableExtensions;
        }

        public boolean isValidExtension(String extension) {
            return CompiledFiletypeFilter.isValidExtension(extension, this);
        }

        @Override
        public ExtensionsFilterInterface[] listSameGroup() {
            return values();
        }

        @Override
        public boolean matchesMimeType(String mimeType) {
            return false;
        }

        @Override
        public String getExtensionFromMimeType(String mimeType) {
            return CompiledFiletypeFilter.getExtensionFromMimeType(mimeType, this);
        }
    }

    public static enum SubtitleExtensions implements CompiledFiletypeExtension {
        SRT, // SubRip
        SSF, // Structured Subtitle Format
        SSA, // SubStation Alpha
        ASS, // SubStation Alpha
        IDX, // VobSub
        TTXT, // MPEG-4 Timed Text
        TTML, // Timed Text Markup Language
        SMI, // SAMI
        VTT, // WebVTT
        SUB;// VobSub

        private final Pattern  pattern;
        private static Pattern allPattern;

        public Pattern getPattern() {
            return pattern;
        }

        public ExtensionsFilterInterface getSource() {
            return this;
        }

        private SubtitleExtensions() {
            pattern = Pattern.compile(name(), Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }

        private SubtitleExtensions(String id) {
            this.pattern = Pattern.compile(id, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }

        public String getDesc() {
            return _GUI.T.FilterRuleDialog_createTypeFilter_mime_subtitle();
        }

        public String getIconID() {
            return IconKey.ICON_LANGUAGE;
        }

        public Pattern compiledAllPattern() {
            if (allPattern == null) {
                allPattern = compileAllPattern(SubtitleExtensions.values());
            }
            return allPattern;
        }

        public boolean isValidExtension(String extension) {
            return CompiledFiletypeFilter.isValidExtension(extension, this);
        }

        @Override
        public boolean isSameExtensionGroup(ExtensionsFilterInterface extension) {
            return extension != null && extension instanceof SubtitleExtensions;
        }

        @Override
        public ExtensionsFilterInterface[] listSameGroup() {
            return values();
        }

        @Override
        public boolean matchesMimeType(String mimeType) {
            return false;
        }

        @Override
        public String getExtensionFromMimeType(String mimeType) {
            return CompiledFiletypeFilter.getExtensionFromMimeType(mimeType, this);
        }
    }

    public static enum DocumentExtensions implements CompiledFiletypeExtension {
        CSS {
            private final Pattern pattern = Pattern.compile("(?i)text/css");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }
        },
        TXT {
            private final Pattern pattern = Pattern.compile("(?i)text/plain");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }
        },
        HTML("(html?)") {
            private final Pattern pattern = Pattern.compile("(?i)text/html");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }
        },
        PHP,
        JSP,
        JAVA,
        JS {
            private final Pattern pattern = Pattern.compile("(?i)text/javascript");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }
        },
        JSON {
            private final Pattern pattern = Pattern.compile("(?i)application/json");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }
        },
        DOC("(doc(m)?|dot(x|m)?)"),
        DOCX {
            private final Pattern pattern = Pattern.compile("(?i)application/vnd.openxmlformats-officedocument.wordprocessingml.document");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }
        },
        DOCM {
            private final Pattern pattern = Pattern.compile("(?i)application/vnd.ms-word.document.macroEnabled.12");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }

            public boolean isValidExtension(String extension) {
                return super.isValidExtension(extension) || CompiledFiletypeFilter.isValidExtension(extension, DOC);
            }
        },
        DOTX {
            private final Pattern pattern = Pattern.compile("(?i)application/vnd.openxmlformats-officedocument.wordprocessingml.template");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }

            public boolean isValidExtension(String extension) {
                return super.isValidExtension(extension) || CompiledFiletypeFilter.isValidExtension(extension, DOC);
            }
        },
        DOCT {
            private final Pattern pattern = Pattern.compile("(?i)application/vnd.ms-word.template.macroEnabled.12");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }
        },
        EPUB,
        PDB,
        README,
        MOBI("mobi|prc"),
        XML {
            private final Pattern pattern = Pattern.compile("(?i)(text/xml|application/xml)");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }
        },
        CHM,
        CSV,
        RTF,
        PDF {
            private final Pattern pattern = Pattern.compile("(?i)application/pdf");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }
        },
        NFO,
        USF;

        private final Pattern  pattern;
        private static Pattern allPattern;

        public Pattern getPattern() {
            return pattern;
        }

        public ExtensionsFilterInterface getSource() {
            return this;
        }

        private DocumentExtensions() {
            pattern = Pattern.compile(name(), Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }

        private DocumentExtensions(String id) {
            this.pattern = Pattern.compile(id, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }

        public String getDesc() {
            return _GUI.T.FilterRuleDialog_createTypeFilter_mime_document();
        }

        public String getIconID() {
            return IconKey.ICON_TEXT;
        }

        public boolean isValidExtension(String extension) {
            return CompiledFiletypeFilter.isValidExtension(extension, this);
        }

        public Pattern compiledAllPattern() {
            if (allPattern == null) {
                allPattern = compileAllPattern(DocumentExtensions.values());
            }
            return allPattern;
        }

        @Override
        public boolean isSameExtensionGroup(ExtensionsFilterInterface extension) {
            return extension != null && extension instanceof DocumentExtensions;
        }

        @Override
        public ExtensionsFilterInterface[] listSameGroup() {
            return values();
        }

        @Override
        public boolean matchesMimeType(String mimeType) {
            return false;
        }

        @Override
        public String getExtensionFromMimeType(String mimeType) {
            return CompiledFiletypeFilter.getExtensionFromMimeType(mimeType, this);
        }
    }

    private static String getExtensionFromMimeType(String mimeType, CompiledFiletypeExtension fileTypeExtension) {
        return fileTypeExtension.matchesMimeType(mimeType) ? fileTypeExtension.name().toLowerCase(Locale.ENGLISH) : null;
    }

    private static Boolean isValidExtension(String extension, CompiledFiletypeExtension fileTypeExtension) {
        if (extension != null) {
            return fileTypeExtension.getPattern().matcher(extension).matches();
        } else {
            return null;
        }
    }

    public static enum AudioExtensions implements CompiledFiletypeExtension {
        AC3,
        MP3 {
            private final Pattern pattern = Pattern.compile("(?i)audio/(mpeg|mp3)");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }
        },
        MO3, // proprietary format developed by "Un4seen Developments": https://www.un4seen.com/mo3.html
        WMA,
        AAC,
        WAV {
            private final Pattern pattern = Pattern.compile("(?i)audio/wav");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }
        },
        FLAC {
            private final Pattern pattern = Pattern.compile("(?i)audio/x-flac");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }
        },
        MID,
        MOD,
        OGG {
            private final Pattern pattern = Pattern.compile("(?i)audio/ogg");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }

            public boolean isValidExtension(String extension) {
                return super.isValidExtension(extension) || CompiledFiletypeFilter.isValidExtension(extension, OGA);
            }
        },
        OGA {
            // ogg audio media
            private final Pattern pattern = Pattern.compile("(?i)audio/ogg");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }

            public boolean isValidExtension(String extension) {
                return super.isValidExtension(extension) || CompiledFiletypeFilter.isValidExtension(extension, OGG);
            }
        },
        OPUS {
            private final Pattern pattern = Pattern.compile("(?i)audio/opus");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }
        },
        S3M,
        FourMP("4MP") {
            @Override
            public String getExtensionFromMimeType(String mimeType) {
                return matchesMimeType(mimeType) ? "4mp" : null;
            }
        },
        AIF,
        AIFF,
        AU,
        M3U,
        M4a {
            // mpeg 4 audio, eg aac
            private final Pattern pattern = Pattern.compile("(?i)audio/(mp4|x-m4a)");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }
        },
        M4b, // mpeg 4 audiobook
        M4P,
        MKA,
        MP1,
        MP2,
        MPA,
        MIDI("midi?"),
        OMG,
        OMF,
        SND,
        SPX, // Speex
        NSF;// NES Sound Format, https://wiki.nesdev.com/w/index.php/NSF

        private final Pattern  pattern;
        private static Pattern allPattern;

        public Pattern getPattern() {
            return pattern;
        }

        public ExtensionsFilterInterface getSource() {
            return this;
        }

        private AudioExtensions() {
            pattern = Pattern.compile(name(), Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }

        private AudioExtensions(String id) {
            this.pattern = Pattern.compile(id, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }

        public String getDesc() {
            return _GUI.T.FilterRuleDialog_createTypeFilter_mime_audio();
        }

        public String getIconID() {
            return IconKey.ICON_AUDIO;
        }

        public boolean isValidExtension(String extension) {
            return CompiledFiletypeFilter.isValidExtension(extension, this);
        }

        public Pattern compiledAllPattern() {
            if (allPattern == null) {
                allPattern = compileAllPattern(AudioExtensions.values());
            }
            return allPattern;
        }

        @Override
        public boolean isSameExtensionGroup(ExtensionsFilterInterface extension) {
            return extension != null && extension instanceof AudioExtensions;
        }

        @Override
        public ExtensionsFilterInterface[] listSameGroup() {
            return values();
        }

        @Override
        public boolean matchesMimeType(String mimeType) {
            return false;
        }

        @Override
        public String getExtensionFromMimeType(String mimeType) {
            return CompiledFiletypeFilter.getExtensionFromMimeType(mimeType, this);
        }
    }

    public static enum VideoExtensions implements CompiledFiletypeExtension {
        ThreeGP("3GP") {
            @Override
            public String getExtensionFromMimeType(String mimeType) {
                return matchesMimeType(mimeType) ? "3gp" : null;
            }
        },
        ASF,
        AVI,
        DIVX,
        XVID,
        FLV,
        MP4 {
            private final Pattern pattern = Pattern.compile("(?i)video/mp4");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }
        },
        H264,
        H265,
        M2TS("m2ts|m2t|mts"),
        MP2T("tsv|tsa|ts"),
        M4U,
        M4V,
        MOV,
        MKV,
        MPEG,
        MPEG4,
        MPG,
        OGM,
        OGV,
        VOB,
        WMV,
        GP3,
        WEBM {
            private final Pattern pattern = Pattern.compile("(?i)video/webm");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }
        },
        APNG {
            private final Pattern pattern = Pattern.compile("(?i)image/apng");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }
        };

        private final Pattern  pattern;
        private static Pattern allPattern;

        private VideoExtensions() {
            pattern = Pattern.compile(name(), Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }

        public Pattern getPattern() {
            return pattern;
        }

        public ExtensionsFilterInterface getSource() {
            return this;
        }

        @Override
        public boolean isSameExtensionGroup(ExtensionsFilterInterface extension) {
            return extension != null && extension instanceof VideoExtensions;
        }

        public boolean isValidExtension(String extension) {
            return CompiledFiletypeFilter.isValidExtension(extension, this);
        }

        private VideoExtensions(String id) {
            this.pattern = Pattern.compile(id, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }

        public String getDesc() {
            return _GUI.T.FilterRuleDialog_createTypeFilter_mime_video();
        }

        public Pattern compiledAllPattern() {
            if (allPattern == null) {
                allPattern = compileAllPattern(VideoExtensions.values());
            }
            return allPattern;
        }

        public String getIconID() {
            return IconKey.ICON_VIDEO;
        }

        @Override
        public ExtensionsFilterInterface[] listSameGroup() {
            return values();
        }

        @Override
        public boolean matchesMimeType(String mimeType) {
            return false;
        }

        @Override
        public String getExtensionFromMimeType(String mimeType) {
            return CompiledFiletypeFilter.getExtensionFromMimeType(mimeType, this);
        }
    }

    private static Pattern compileAllPattern(ExtensionsFilterInterface[] filters) {
        final StringBuilder sb = new StringBuilder();
        sb.append("(");
        boolean or = false;
        for (ExtensionsFilterInterface value : filters) {
            if (or) {
                sb.append("|");
            }
            final Pattern pattern = value.getPattern();
            if (pattern != null) {
                sb.append(pattern);
            }
            or = true;
        }
        sb.append(")");
        return Pattern.compile(sb.toString(), Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    }

    public static enum ArchiveExtensions implements CompiledFiletypeExtension {
        REV,
        RAR,
        ZIP {
            private final Pattern pattern = Pattern.compile("(?i)application/zip");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }
        },
        R_NUM("[r-z]\\d{2}"),
        NUM("\\d{1,4}"),
        MultiZip("z\\d{1,4}"),
        ACE("(ace|c\\d{2,4})") {
            @Override
            public String getExtensionFromMimeType(String mimeType) {
                return matchesMimeType(mimeType) ? "ace" : null;
            }
        },
        TAR,
        GZ {
            private final Pattern pattern = Pattern.compile("(?i)application/gzip");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }
        },
        AR,
        BZ2,
        ARJ,
        CPIO,
        SevenZ("(7z|7zip)") {
            private final Pattern pattern = Pattern.compile("(?i)application/x-7z-compressed");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }

            @Override
            public String getExtensionFromMimeType(String mimeType) {
                return matchesMimeType(mimeType) ? "7z" : null;
            }
        },
        S7Z,
        DMG,
        SFX,
        XZ {
            private final Pattern pattern = Pattern.compile("(?i)application/x-xz");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }
        },
        TXZ, // tar.xz
        TGZ, // tar.gz
        LZH,
        LHA,
        AA("[a-z]{2}");

        private final Pattern  pattern;
        private static Pattern allPattern;

        public Pattern getPattern() {
            return pattern;
        }

        public ExtensionsFilterInterface getSource() {
            return this;
        }

        private ArchiveExtensions() {
            pattern = Pattern.compile(name(), Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }

        private ArchiveExtensions(String id) {
            this.pattern = Pattern.compile(id, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }

        public boolean isValidExtension(String extension) {
            return CompiledFiletypeFilter.isValidExtension(extension, this);
        }

        public String getDesc() {
            return _GUI.T.FilterRuleDialog_createTypeFilter_mime_archives();
        }

        public Pattern compiledAllPattern() {
            if (allPattern == null) {
                allPattern = compileAllPattern(ArchiveExtensions.values());
            }
            return allPattern;
        }

        @Override
        public ExtensionsFilterInterface[] listSameGroup() {
            return values();
        }

        @Override
        public boolean isSameExtensionGroup(ExtensionsFilterInterface extension) {
            return extension != null && extension instanceof ArchiveExtensions;
        }

        public String getIconID() {
            return IconKey.ICON_EXTRACT;
        }

        @Override
        public boolean matchesMimeType(String mimeType) {
            return false;
        }

        @Override
        public String getExtensionFromMimeType(String mimeType) {
            return CompiledFiletypeFilter.getExtensionFromMimeType(mimeType, this);
        }
    }

    public static enum ImageExtensions implements CompiledFiletypeExtension {
        JPG("(jpe|jpe?g|jfif)") {
            private final Pattern pattern = Pattern.compile("(?i)image/jpe?g");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }
        },
        JP2("(jp2|j2k|jpf|jpg2|jpx|jpm|mj2|mjp2)"),
        AVIF,
        GIF {
            private final Pattern pattern = Pattern.compile("(?i)image/gif");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }
        },
        DNG,
        GPR,
        EPS,
        PNG {
            private final Pattern pattern = Pattern.compile("(?i)image/png");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }
        },
        BMP,
        TIF,
        TIFF {
            private final Pattern pattern = Pattern.compile("(?i)image/tiff");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }
        },
        RAW,
        SVG,
        ICO,
        CUR,
        WEBP {
            private final Pattern pattern = Pattern.compile("(?i)image/webp");

            @Override
            public boolean matchesMimeType(String mimeType) {
                return pattern.matcher(mimeType).find();
            }
        },
        MVIEW;

        private final Pattern  pattern;
        private static Pattern allPattern;

        public Pattern getPattern() {
            return pattern;
        }

        private ImageExtensions() {
            pattern = Pattern.compile(name(), Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }

        private ImageExtensions(String id) {
            this.pattern = Pattern.compile(id, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }

        public String getDesc() {
            return _GUI.T.FilterRuleDialog_createTypeFilter_mime_images();
        }

        public ExtensionsFilterInterface getSource() {
            return this;
        }

        public boolean isValidExtension(String extension) {
            return CompiledFiletypeFilter.isValidExtension(extension, this);
        }

        public Pattern compiledAllPattern() {
            if (allPattern == null) {
                allPattern = compileAllPattern(ImageExtensions.values());
            }
            return allPattern;
        }

        @Override
        public ExtensionsFilterInterface[] listSameGroup() {
            return values();
        }

        @Override
        public boolean isSameExtensionGroup(ExtensionsFilterInterface extension) {
            return extension != null && extension instanceof ImageExtensions;
        }

        public String getIconID() {
            return IconKey.ICON_IMAGE;
        }

        @Override
        public boolean matchesMimeType(String mimeType) {
            return false;
        }

        @Override
        public String getExtensionFromMimeType(String mimeType) {
            return CompiledFiletypeFilter.getExtensionFromMimeType(mimeType, this);
        }
    }

    public CompiledFiletypeFilter(FiletypeFilter filetypeFilter) {
        final List<Pattern> list = new ArrayList<Pattern>();
        final List<ExtensionsFilterInterface> filterInterfaces = new ArrayList<ExtensionsFilterInterface>();
        if (filetypeFilter.isArchivesEnabled()) {
            filterInterfaces.add(ArchiveExtensions.ACE);
        }
        if (filetypeFilter.isHashEnabled()) {
            filterInterfaces.add(HashExtensions.MD5);
        }
        if (filetypeFilter.isAudioFilesEnabled()) {
            filterInterfaces.add(AudioExtensions.AAC);
        }
        if (filetypeFilter.isImagesEnabled()) {
            filterInterfaces.add(ImageExtensions.BMP);
        }
        if (filetypeFilter.isVideoFilesEnabled()) {
            filterInterfaces.add(VideoExtensions.ASF);
        }
        if (filetypeFilter.isDocFilesEnabled()) {
            filterInterfaces.add(DocumentExtensions.TXT);
        }
        if (filetypeFilter.isSubFilesEnabled()) {
            filterInterfaces.add(SubtitleExtensions.SRT);
        }
        if (filetypeFilter.isExeFilesEnabled()) {
            filterInterfaces.add(ExecutableExtensions.EXE);
        }
        matchType = filetypeFilter.getMatchType();
        try {
            if (filetypeFilter.getCustoms() != null) {
                if (filetypeFilter.isUseRegex()) {
                    list.add(Pattern.compile(filetypeFilter.getCustoms(), Pattern.DOTALL | Pattern.CASE_INSENSITIVE));
                } else {
                    for (String s : filetypeFilter.getCustoms().split("\\,")) {
                        list.add(LinkgrabberFilterRuleWrapper.createPattern(s, false, RuleWrapper.AUTO_PATTERN_MODE.MATCHES));
                    }
                }
            }
        } catch (final Throwable e) {
            /* custom regex may contain errors */
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        }
        this.list = list.toArray(new Pattern[list.size()]);
        this.filterInterfaces = filterInterfaces.toArray(new ExtensionsFilterInterface[filterInterfaces.size()]);
    }

    public boolean matches(final String extension, final LinkInfo linkInfo) {
        boolean matches = false;
        final String ext;
        if (StringUtils.isNotEmpty(extension)) {
            ext = extension;
        } else {
            ext = linkInfo.getExtension().name();
        }
        for (final ExtensionsFilterInterface filterInterfaces : this.filterInterfaces) {
            if (!matches) {
                if (linkInfo.getExtension().isSameExtensionGroup(filterInterfaces)) {
                    matches = true;
                    break;
                } else {
                    for (final ExtensionsFilterInterface filterInterface : filterInterfaces.listSameGroup()) {
                        final Pattern pattern = filterInterface.getPattern();
                        try {
                            if (pattern != null && pattern.matcher(ext).matches()) {
                                matches = true;
                                break;
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                break;
            }
        }
        if (matches == false) {
            for (final Pattern pattern : this.list) {
                try {
                    if (pattern != null && pattern.matcher(ext).matches()) {
                        matches = true;
                        break;
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        switch (matchType) {
        case IS:
            return matches;
        case IS_NOT:
            return !matches;
        }
        return false;
    }

    public Pattern[] getList() {
        final List<Pattern> ret = new ArrayList<Pattern>();
        ret.addAll(Arrays.asList(this.list));
        for (final ExtensionsFilterInterface filterInterfaces : this.filterInterfaces) {
            for (final ExtensionsFilterInterface filterInterface : filterInterfaces.listSameGroup()) {
                final Pattern pattern = filterInterface.getPattern();
                if (pattern != null) {
                    ret.add(pattern);
                }
            }
        }
        return ret.toArray(new Pattern[ret.size()]);
    }
}
