package org.jdownloader.controlling.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import jd.plugins.LinkInfo;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.FiletypeFilter.TypeMatchType;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class CompiledFiletypeFilter {
    private final Pattern[]                   list;
    private final ExtensionsFilterInterface[] filterInterfaces;
    private final TypeMatchType               matchType;

    public TypeMatchType getMatchType() {
        return matchType;
    }

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

    public static ExtensionsFilterInterface getExtensionsFilterInterface(final String fileExtension) {
        if (fileExtension != null) {
            for (final ExtensionsFilterInterface[] extensions : new ExtensionsFilterInterface[][] { ExecutableExtensions.values(), HashExtensions.values(), AudioExtensions.values(), DocumentExtensions.values(), ArchiveExtensions.values(), ImageExtensions.values(), VideoExtensions.values(), SubtitleExtensions.values() }) {
                for (final ExtensionsFilterInterface extension : extensions) {
                    final Pattern pattern = extension.getPattern();
                    if (pattern != null && pattern.matcher(fileExtension).matches()) {
                        return extension;
                    }
                }
            }
        }
        return null;
    }

    public static enum HashExtensions implements ExtensionsFilterInterface {
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
    }

    public static enum ExecutableExtensions implements ExtensionsFilterInterface {
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

        @Override
        public ExtensionsFilterInterface[] listSameGroup() {
            return values();
        }
    }

    public static enum SubtitleExtensions implements ExtensionsFilterInterface {
        SRT, // SubRip
        SSF, // Structured Subtitle Format
        SSA, // SubStation Alpha
        ASS, // SubStation Alpha
        IDX, // VobSub
        TTXT, // MPEG-4 Timed Text
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

        @Override
        public boolean isSameExtensionGroup(ExtensionsFilterInterface extension) {
            return extension != null && extension instanceof SubtitleExtensions;
        }

        @Override
        public ExtensionsFilterInterface[] listSameGroup() {
            return values();
        }
    }

    public static enum DocumentExtensions implements ExtensionsFilterInterface {
        TXT,
        HTML("(html?)"),
        PHP,
        JSP,
        JAVA,
        JS,
        DOC("(doc(x|m)?|dot(x|m)?)"),
        EPUB,
        PDB,
        README,
        MOBI("mobi|prc"),
        XML,
        CHM,
        CSV,
        RTF,
        PDF,
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
    }

    public static enum AudioExtensions implements ExtensionsFilterInterface {
        MP3,
        WMA,
        AAC,
        WAV,
        FLAC,
        MID,
        MOD,
        OGG,
        S3M,
        FourMP("4MP"),
        AIF,
        AIFF,
        AU,
        M3U,
        M4a,
        M4b,
        M4P,
        MKA,
        MP1,
        MP2,
        MPA,
        OMG,
        OMF,
        SND,
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
    }

    public static enum VideoExtensions implements ExtensionsFilterInterface {
        ThreeGP("3GP"),
        ASF,
        AVI,
        DIVX,
        XVID,
        FLV,
        MP4,
        H264,
        H265,
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
        WEBM;
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

    public static enum ArchiveExtensions implements ExtensionsFilterInterface {
        REV,
        RAR,
        ZIP,
        SevenZIP("7ZIP"),
        R_NUM("[r-z]\\d{2}"),
        NUM("\\d{1,4}"),
        MultiZip("z\\d{1,4}"),
        ACE("(ace|c\\d{2,4})"),
        TAR,
        GZ,
        AR,
        BZ2,
        ARJ,
        CPIO,
        SevenZ("7Z"),
        S7Z,
        DMG,
        SFX,
        XZ,
        TGZ,
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
    }

    public static enum ImageExtensions implements ExtensionsFilterInterface {
        JPG,
        JP2("(jp2|j2k|jpf|jpg2|jpx|jpm|mj2|mjp2)"),
        JPEG("(jpe|jpeg|jfif)"),
        GIF,
        EPS,
        PNG,
        BMP,
        TIF,
        TIFF,
        RAW,
        SVG,
        ICO,
        WEBP;
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
        try {
            if (filetypeFilter.getCustoms() != null) {
                if (filetypeFilter.isUseRegex()) {
                    list.add(Pattern.compile(filetypeFilter.getCustoms(), Pattern.DOTALL | Pattern.CASE_INSENSITIVE));
                } else {
                    for (String s : filetypeFilter.getCustoms().split("\\,")) {
                        list.add(LinkgrabberFilterRuleWrapper.createPattern(s, false));
                    }
                }
            }
        } catch (final Throwable e) {
            /* custom regex may contain errors */
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        }
        matchType = filetypeFilter.getMatchType();
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
