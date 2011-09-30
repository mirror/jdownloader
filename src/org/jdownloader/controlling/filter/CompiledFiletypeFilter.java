package org.jdownloader.controlling.filter;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.appwork.storage.config.JsonConfig;

public class CompiledFiletypeFilter {
    private ArrayList<Pattern> list = new ArrayList<Pattern>();

    public static enum AudioExtensions {
        MP3, WMA, ACC, WAV, FLAC, MID, MOD, OGG, S3M, FourMP("4MP"), AA, AIF, AIFF, AU, M3U, M4a, M4b, M4P, MKA, MP1, MP2, MPA, OMG, OMF, SND;

        private Pattern pattern;

        public Pattern getPattern() {
            return pattern;
        }

        private AudioExtensions() {
            pattern = Pattern.compile(name(), Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }

        private AudioExtensions(String id) {
            this.pattern = Pattern.compile(id, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }
    }

    public static enum VideoExtensions {
        ASF, AVI, DIVX, XVID, FLV, MP4, H264, M4U, M4V, MOV, MKV, MPEG, MPEG4, MPG, OGM, OGV, VOB, WMV;

        private Pattern pattern;

        private VideoExtensions() {
            pattern = Pattern.compile(name(), Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }

        public Pattern getPattern() {
            return pattern;
        }

        private VideoExtensions(String id) {
            this.pattern = Pattern.compile(id, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }
    }

    public static enum ArchiveExtensions {
        RAR, ZIP, SevenZIP("7ZIP"), R_NUM("r\\d+"), NUM("\\d+"), ACE, TAR, GZ, AR, BZ2, SevenZ("7Z"), S7Z, DMG, SFX, TGZ;

        private Pattern pattern;

        public Pattern getPattern() {
            return pattern;
        }

        private ArchiveExtensions() {
            pattern = Pattern.compile(name(), Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }

        private ArchiveExtensions(String id) {
            this.pattern = Pattern.compile(id, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }
    }

    public static enum ImageExtensions {
        JPG, JPEG, GIF, PNG, BMP, TIFF, RAW, SVG;

        private Pattern pattern;

        public Pattern getPattern() {
            return pattern;
        }

        private ImageExtensions() {
            pattern = Pattern.compile(name(), Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }

        private ImageExtensions(String id) {
            this.pattern = Pattern.compile(id, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }
    }

    public CompiledFiletypeFilter(FiletypeFilter filetypeFilter) {
        if (filetypeFilter.isArchivesEnabled()) {
            for (ArchiveExtensions ae : ArchiveExtensions.values()) {
                list.add(ae.getPattern());
            }
        }

        if (filetypeFilter.isAudioFilesEnabled()) {
            for (AudioExtensions ae : AudioExtensions.values()) {
                list.add(ae.getPattern());
            }
        }

        if (filetypeFilter.isImagesEnabled()) {
            for (ImageExtensions ae : ImageExtensions.values()) {
                list.add(ae.getPattern());
            }
        }
        if (filetypeFilter.isVideoFilesEnabled()) {
            for (VideoExtensions ae : VideoExtensions.values()) {
                list.add(ae.getPattern());
            }
        }
        if (filetypeFilter.getCustoms() != null) {
            if (JsonConfig.create(LinkFilterSettings.class).isRuleconditionsRegexEnabled()) {
                list.add(Pattern.compile(filetypeFilter.getCustoms(), Pattern.DOTALL | Pattern.CASE_INSENSITIVE));
            } else {

                for (String s : filetypeFilter.getCustoms().split("\\,")) {
                    list.add(LinkgrabberFilterRuleWrapper.createPattern(s));
                }
            }
        }

    }

    public boolean matches(String extension) {
        System.out.println(1);
        try {
            for (Pattern o : list) {
                if (o.matcher(extension).matches()) return true;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

}
