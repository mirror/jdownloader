package jd.plugins.components;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.IO.SYNC;
import org.appwork.utils.StringUtils;
import org.appwork.utils.ide.IDEUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.ffmpeg.FFprobe;
import org.jdownloader.controlling.ffmpeg.json.Stream;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.plugins.components.youtube.AudioCodec;
import org.jdownloader.plugins.components.youtube.MediaQualityInterface;
import org.jdownloader.plugins.components.youtube.MediaTagsVarious;
import org.jdownloader.plugins.components.youtube.VideoContainer;
import org.jdownloader.plugins.components.youtube.YoutubeClipData;
import org.jdownloader.plugins.components.youtube.YoutubeITAG;
import org.jdownloader.plugins.components.youtube.YoutubeVariant;
import org.jdownloader.plugins.components.youtube.YoutubeVariantInterface;
import org.jdownloader.plugins.components.youtube.YoutubeVariantInterface.DownloadType;
import org.jdownloader.plugins.components.youtube.YoutubeVariantInterface.VariantGroup;
import org.jdownloader.plugins.components.youtube.YoutubeVariantOld;

import jd.http.Browser;
import jd.http.QueryInfo;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;

public class ItagHelper {
    private static HashSet<String> dupes;
    private static HashSet<String> EXCLUDES;

    public static void main2(String[] args) throws IOException {
        Application.setApplication(".jd_home");
        for (YoutubeVariantOld vo : YoutubeVariantOld.values()) {
            if (YoutubeVariant.COMPATIBILITY_MAP.containsKey(vo.name())) {
                continue;
            }
            System.out.println(vo);
            int i = 0;
            for (YoutubeVariant v : YoutubeVariant.values()) {
                if (vo.getiTagAudio() == v.getiTagAudio()) {
                    if (vo.getiTagData() == v.getiTagData()) {
                        if (vo.getiTagVideo() == v.getiTagVideo()) {
                            if (vo.getGroup() == v.getGroup()) {

                                if (StringUtils.equalsIgnoreCase(vo.getFileExtension(), v.getFileExtension())) {
                                    System.out.println("Match" + vo + " - " + v);
                                    appendToSrc("COMPATIBILITY_MAP.put(\"" + vo.name() + "\"," + v.name() + ");", "// ###APPEND_COMPATIBILITY_MAP###");
                                    i++;
                                }
                            }
                        }
                    }
                }
            }
            if (i > 1) {
                System.out.println("WARN");
            }
        }
    }

    public static void main(String[] args) throws DialogClosedException, DialogCanceledException, IOException {
        Application.setApplication(".jd_home");
        main2(args);
        EXCLUDES = new HashSet<String>();
        EXCLUDES.add("DEMUX_AMRNB_.*");
        EXCLUDES.add("DEMUX_.*_THREEGP.*");
        EXCLUDES.add("DEMUX_VORBIS_.*");
        List<YoutubeITAG> dashVideos = new ArrayList<YoutubeITAG>();
        List<YoutubeITAG> dashAudios = new ArrayList<YoutubeITAG>();
        List<YoutubeITAG> nonDash = new ArrayList<YoutubeITAG>();
        loop: for (YoutubeITAG tag : YoutubeITAG.values()) {
            switch (tag) {
            case SUBTITLE:
            case IMAGE_HQ:
            case IMAGE_LQ:
            case IMAGE_MAX:
            case IMAGE_MQ:
                continue loop;
            }

            if (tag.getCodecAudio() == null) {
                dashVideos.add(tag);
            } else if (tag.getCodecVideo() == null) {
                dashAudios.add(tag);
            } else {
                nonDash.add(tag);
            }
        }

        dupes = new HashSet<String>();
        for (YoutubeVariant v : YoutubeVariant.values()) {
            dupes.add(v.name());
        }

        for (YoutubeITAG itag : nonDash) {
            handleItag(itag);

        }

        audioLoop: for (YoutubeITAG dashAudio : dashAudios) {
            videoLoop: for (YoutubeITAG dashVideo : dashVideos) {

                String videoCodec = codecToName(dashVideo.getCodecVideo());
                String audioCodec = codecToName(dashAudio.getCodecAudio());
                if (!validCombination(videoCodec, audioCodec)) {
                    continue;
                }

                handleDashCombination(dashVideo, dashAudio);

            }

        }
        // if (false) {
        // audioLoop: for (YoutubeITAG dashAudio : dashAudios) {
        // for (YoutubeVariant v : YoutubeVariant.values()) {
        // if (v.getiTagAudio() == dashAudio && v.getiTagVideo() == null) {
        // break audioLoop;
        // }
        // }
        //
        // getBitrateByItag(dashAudio);
        //
        // String audioCodec = dashAudio.getCodecAudio();
        // // String audioCodec = new Regex(dashAudio.name(), "(opus|aac)").getMatch(0);
        // String name = "DASH_AUDIO_" + audioCodec + "_" + bitrate + "KBIT";
        // // AAC_128(null, YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "aac", null,
        // // YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null) {
        // // @Override
        // // public String _getName() {
        // // return _GUI.T.YoutubeVariant_name_AAC_128();
        // // }
        // //
        // // @Override
        // // public String getQualityExtension() {
        // // return _GUI.T.YoutubeVariant_filenametag_AAC_128();
        // // }
        // // },
        // name = name.toUpperCase(Locale.ENGLISH);
        // String orgname = name;
        // int i = 2;
        // while (!dupes.add(name)) {
        // name = orgname + "_" + (i++);
        // }
        // sb.append(name).append("(");
        // sb.append("null");
        // sb.append(", ");
        // sb.append("YoutubeVariantInterface.VariantGroup.AUDIO");
        // sb.append(", ");
        // sb.append("YoutubeVariantInterface.DownloadType.DASH_AUDIO");
        // sb.append(", ");
        // sb.append("\"").append(getExtension(dashAudio, true).toString().toLowerCase(Locale.ENGLISH)).append("\"");
        // sb.append(", ");
        // sb.append("null");
        // sb.append(", ");
        // sb.append("YoutubeITAG." + dashAudio);
        // sb.append(", ");
        // sb.append("null");
        // sb.append(", ");
        // sb.append("null");
        // sb.append(", ");
        // sb.append("null");
        // sb.append(")");
        // sb.append("{\r\n");
        // sb.append("@Override\r\n");
        // sb.append("public String _getName() {\r\n");
        // sb.append(" return _GUI.T.YoutubeVariant_name_generic_audio(\"" + bitrate + "kbit\",\"" + getExtension(dashAudio, true) +
        // "\");\r\n");
        // sb.append("}\r\n\r\n");
        // sb.append("@Override\r\n");
        // sb.append("public String getQualityExtension() {\r\n");
        // sb.append(" return _GUI.T.YoutubeVariant_nametag_generic_audio(\"" + bitrate + "kbit\",\"" + getExtension(dashAudio, true) +
        // "\");\r\n");
        // sb.append("}\r\n");
        // sb.append("},\r\n");
        // }
        // }
        // if (false) {
        // videoLoop: for (YoutubeITAG dashVideo : dashVideos) {
        // audioLoop: for (YoutubeITAG dashAudio : dashAudios) {
        // for (YoutubeVariant v : YoutubeVariant.values()) {
        // if (v.getiTagAudio() == dashAudio && v.getiTagVideo() == dashVideo) {
        // break audioLoop;
        // }
        // }
        // // unknown variant
        // // DASH_VP9_2160P_30FPS_OPUS_64KBIT("_VP9_2160P_30FPS", YoutubeVariantInterface.VariantGroup.VIDEO,
        // // YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_2160P_30FPS,
        // // YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null) {
        // // @Override
        // // public String _getName() {
        // //
        // // return _GUI.T.YoutubeVariant_name_generic_video("720p","WebM");
        // // }
        // //
        // // @Override
        // // public String getQualityExtension() {
        // // return _GUI.T.YoutubeVariant_name_generic_video("720p","WebM");
        // // }
        // // },
        // String fps = new Regex(dashVideo.name(), "(\\d+fps)").getMatch(0);
        // if (fps == null) {
        // fps = "30FPS";
        // }
        // String resolution = getResolutionByItag(dashVideo);
        // getBitrateByItag(dashAudio);
        // String videoCodec = dashVideo.getCodecVideo();
        // // String videoCodec = new Regex(dashVideo.name(), "(h264|vp9)").getMatch(0);
        // String audioCodec = dashAudio.getCodecAudio();
        // if (!validCombination(videoCodec, audioCodec)) {
        // continue;
        // }
        // // String audioCodec = new Regex(dashAudio.name(), "(opus|aac)").getMatch(0);
        // String name = "DASH_" + videoCodec + "_" + resolution + "P_" + fps + "_" + audioCodec + "_" + bitrate + "KBIT";
        // name = name.toUpperCase(Locale.ENGLISH);
        // String orgname = name;
        // int i = 2;
        // while (!dupes.add(name)) {
        // name = orgname + "_" + (i++);
        // }
        // sb.append(name).append("(");
        // sb.append("\"").append(resolution + "P_" + getExtension(dashVideo, false)).append("\"");
        // sb.append(", ");
        // sb.append("YoutubeVariantInterface.VariantGroup.VIDEO");
        // sb.append(", ");
        // sb.append("YoutubeVariantInterface.DownloadType.DASH_VIDEO");
        // sb.append(", ");
        // sb.append("\"").append(getExtension(dashVideo, false).toString().toLowerCase(Locale.ENGLISH)).append("\"");
        // sb.append(", ");
        // sb.append("YoutubeITAG." + dashVideo);
        // sb.append(", ");
        // sb.append("YoutubeITAG." + dashAudio);
        // sb.append(", ");
        // sb.append("null");
        // sb.append(", ");
        // sb.append("null");
        // sb.append(", ");
        // sb.append("null");
        // sb.append(")");
        // sb.append("{\r\n");
        // sb.append("@Override\r\n");
        // sb.append("public String _getName() {\r\n");
        // sb.append(" return _GUI.T.YoutubeVariant_name_generic_video(\"" + resolution + "p\",\"" + getExtension(dashVideo, false) +
        // "\");\r\n");
        // sb.append("}\r\n\r\n");
        // sb.append("@Override\r\n");
        // sb.append("public String getQualityExtension() {\r\n");
        // sb.append(" return _GUI.T.YoutubeVariant_name_generic_video(\"" + resolution + "p\",\"" + getExtension(dashVideo, false) +
        // "\");\r\n");
        // sb.append("}\r\n");
        // sb.append("},\r\n");
        //
        // }
        // }
        //
        // }
        // videoLoop: for (YoutubeITAG dashVideo : nonDash) {
        //
        // for (YoutubeVariant v : YoutubeVariant.values()) {
        // if (v.getiTagVideo() == dashVideo) {
        // continue videoLoop;
        // }
        // }
        // // MP4_360("MP4_360", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4",
        // // YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC, null, null, null, null) {
        // // @Override
        // // public String _getName() {
        // // return _GUI.T.YoutubeVariant_name_MP4_360();
        // // }
        // //
        // // @Override
        // // public String getQualityExtension() {
        // // return _GUI.T.YoutubeVariant_filenametag_MP4_360();
        // // }
        // // },
        // String fps = new Regex(dashVideo.name(), "(\\d+fps)").getMatch(0);
        // if (fps == null) {
        // fps = "30FPS";
        // }
        // String resolution = new Regex(dashVideo.getQualityVideo(), "(\\d+)p").getMatch(0);
        // if (resolution == null) {
        // resolution = "" + (int) dashVideo.getQualityRating();
        // }
        // String bitrate = new Regex(dashVideo.getQualityAudio(), "(\\d+)k").getMatch(0);
        // String videoCodec = dashVideo.getCodecVideo();
        // // String videoCodec = new Regex(dashVideo.name(), "(h264|vp9)").getMatch(0);
        // String audioCodec = dashVideo.getCodecAudio();
        // if (!validCombination(videoCodec, audioCodec)) {
        // continue;
        // }
        // // String audioCodec = new Regex(dashAudio.name(), "(opus|aac)").getMatch(0);
        // String name = "" + videoCodec + "_" + resolution + "P_" + fps + "_" + audioCodec + "_" + bitrate + "KBIT";
        // name = name.toUpperCase(Locale.ENGLISH);
        // String orgname = name;
        // int i = 2;
        // while (!dupes.add(name)) {
        // name = orgname + "_" + (i++);
        // }
        // sb.append(name).append("(");
        // sb.append("\"").append(resolution + "P_" + getExtension(dashVideo, false)).append("\"");
        // sb.append(", ");
        // sb.append("YoutubeVariantInterface.VariantGroup.VIDEO");
        // sb.append(", ");
        // sb.append("YoutubeVariantInterface.DownloadType.VIDEO");
        // sb.append(", ");
        // sb.append("\"").append(getExtension(dashVideo, false).toString().toLowerCase(Locale.ENGLISH)).append("\"");
        // sb.append(", ");
        // sb.append("YoutubeITAG." + dashVideo);
        // sb.append(", ");
        // sb.append("null");
        // sb.append(", ");
        // sb.append("null");
        // sb.append(", ");
        // sb.append("null");
        // sb.append(", ");
        // sb.append("null");
        // sb.append(")");
        // sb.append("{\r\n");
        // sb.append("@Override\r\n");
        // sb.append("public String _getName() {\r\n");
        // sb.append(" return _GUI.T.YoutubeVariant_name_generic_video(\"" + resolution + "p\",\"" + getExtension(dashVideo, false) +
        // "\");\r\n");
        // sb.append("}\r\n\r\n");
        // sb.append("@Override\r\n");
        // sb.append("public String getQualityExtension() {\r\n");
        // sb.append(" return _GUI.T.YoutubeVariant_name_generic_video(\"" + resolution + "p\",\"" + getExtension(dashVideo, false) +
        // "\");\r\n");
        // sb.append("}\r\n");
        // sb.append("},\r\n");
        //
        // }

    }

    private static void handleDashCombination(YoutubeITAG dashVideo, YoutubeITAG dashAudio) throws IOException {
        if (dashVideo.name().contains("HLS")) {
            throw new WTFException("DASH HLS");

        }

        if (dashAudio.name().contains("HLS")) {
            throw new WTFException("DASH HLS");

        }
        String resolution = getResolutionByItag(dashVideo);
        String bitrate = getBitrateByItag(dashAudio);

        VideoContainer container = getVideoContainerByITag(dashVideo);
        String audioContainer = getAudioContainerByITag(dashAudio);
        boolean is3DItag = dashVideo.name().contains("3D");

        String fps = fpsByItag(dashVideo);

        String videoCodec = dashVideo.getCodecVideo();
        String audioCodec = dashAudio.getCodecAudio();
        String videoExtension = getExtension(dashVideo, false);

        String name = container + "_" + codecToName(videoCodec) + "_" + resolution + "P_" + fps + "_" + codecToName(audioCodec) + "_" + bitrate + "KBIT" + "_DASH";
        name = name.toUpperCase(Locale.ENGLISH);

        VariantInfo vi = new VariantInfo(name);
        vi.fps = fps;
        vi.id = resolution + "P_" + fps + "_" + videoExtension;
        vi.group = YoutubeVariantInterface.VariantGroup.VIDEO;
        vi.downloadType = DownloadType.DASH_VIDEO;
        vi.fileExtension = videoExtension.toLowerCase(Locale.ENGLISH);

        vi.videoItag = dashVideo;
        vi.audioItag = dashAudio;
        vi.dataItag = null;
        // vi.getNameMethod = "_GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(), getVideoCodec(), getAudioQuality(),
        // getAudioCodec())";
        // vi.getQualityExtensionMethod = "_GUI.T.YoutubeVariant_nametag_generic_video(\"" + resolution + "p\")";
        if (!is3DItag) {
            addVi(vi);
        }

        name = container + "_" + codecToName(videoCodec) + "_3D_" + resolution + "P_" + fps + "_" + codecToName(audioCodec) + "_" + bitrate + "KBIT_DASH";
        name = name.toUpperCase(Locale.ENGLISH);

        vi.baseName = name;
        vi.group = VariantGroup.VIDEO_3D;
        vi.id = resolution + "P_" + fps + "_" + videoExtension + "_3D";
        // vi.getQualityExtensionMethod = "_GUI.T.YoutubeVariant_nametag_generic_video(\"" + resolution + "p 3D\")";
        if (is3DItag || container != VideoContainer.MP4) {
            addVi(vi);
        }
        // audio
        if (!dupes.add(dashAudio.name())) {
            return;
        }
        if (StringUtils.equalsIgnoreCase(audioContainer, codecToName(audioCodec))) {
            name = audioContainer + "_" + bitrate + "KBIT" + "_DASH";
        } else {
            name = audioContainer + "_" + codecToName(audioCodec) + "_" + bitrate + "KBIT" + "_DASH";
        }

        name = name.toUpperCase(Locale.ENGLISH);
        vi.id = codecToName(audioCodec).toUpperCase(Locale.ENGLISH) + "_" + bitrate;
        vi.baseName = name;
        vi.group = VariantGroup.AUDIO;
        vi.downloadType = DownloadType.DASH_AUDIO;
        vi.fileExtension = audioContainer.toLowerCase(Locale.ENGLISH);
        vi.videoItag = null;
        // vi.getQualityExtensionMethod = "_GUI.T.YoutubeVariant_nametag_generic_audio(getAudioQuality())";
        // vi.getNameMethod = "_GUI.T.YoutubeVariant_name_generic_audio(getAudioQuality(), getAudioCodec())";

        addVi(vi);
        if (StringUtils.equalsIgnoreCase("aac", vi.fileExtension)) {
            audioContainer = "M4A";
            if (StringUtils.equalsIgnoreCase(audioContainer, codecToName(audioCodec))) {
                name = audioContainer + "_" + bitrate + "KBIT" + "_DASH";
            } else {
                name = audioContainer + "_" + codecToName(audioCodec) + "_" + bitrate + "KBIT" + "_DASH";
            }
            name = name.toUpperCase(Locale.ENGLISH);
            vi.baseName = name;
            vi.fileExtension = audioContainer.toLowerCase(Locale.ENGLISH);

            vi.id = "M4A_" + bitrate;
            vi.extender = generateMethod("@Override", "double getQualityRating()", "", "AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_" + bitrate + ".getRating()");

            // vi.getNameMethod = "_GUI.T.YoutubeVariant_name_generic_audio(getAudioQuality(), \"M4A\")";
            addVi(vi);

        }

        // if ("AAC".equals(audioCodec)) {
        // name = container + "_" + codecToName(videoCodec) + "_" + resolution + "P_" + fps + "_" + codecToName(audioCodec) + "_" + bitrate
        // + "KBIT";
        // name = name.toUpperCase(Locale.ENGLISH);
        // if (itag.name().contains("HLS")) {
        // name = "HLS_" + name;
        // }
        // name = "DEMUX_AAC_" + name;
        // vi.fileExtension = "aac";
        // vi.baseName = name;
        // vi.group = VariantGroup.AUDIO;
        // vi.id = "AAC_" + bitrate;
        // vi.getQualityExtensionMethod = "_GUI.T.YoutubeVariant_nametag_generic_audio(getAudioQuality())";
        // vi.getNameMethod = "_GUI.T.YoutubeVariant_name_generic_audio(getAudioQuality(), getAudioCodec())";
        // vi.converter = "YoutubeMp4ToAACAudio.getInstance()";
        // vi.extender = generateMethod("@Override", "double getQualityRating()", "", "AudioCodec.AAC.getRating() + AudioBitrate.KBIT_" +
        // bitrate + ".getRating() - 0.00000" + resolution);
        //
        // // sb.append("@Override\r\n");
        // // sb.append("public String getQualityExtension() {\r\n");
        // // sb.append(" return " + getQualityExtensionMethod + ";\r\n");
        // // sb.append("}\r\n");
        // addVi(vi);
        // name = container + "_" + codecToName(videoCodec) + "_" + resolution + "P_" + fps + "_" + codecToName(audioCodec) + "_" + bitrate
        // + "KBIT";
        // name = name.toUpperCase(Locale.ENGLISH);
        // if (itag.name().contains("HLS")) {
        // name = "HLS_" + name;
        // }
        // name = "DEMUX_M4A_" + name;
        // vi.converter = "YoutubeMp4ToM4aAudio.getInstance()";
        // vi.extender = generateMethod("@Override", "double getQualityRating()", "", "AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_"
        // + bitrate + ".getRating() - 0.00000" + resolution);
        // vi.fileExtension = "m4a";
        // vi.baseName = name;
        // vi.id = "M4A_" + bitrate;
        // vi.getNameMethod = "_GUI.T.YoutubeVariant_name_generic_audio(getAudioQuality(), \"M4A\")";
        // addVi(vi);
        // }
        //
        System.out.println(1);
    }

    private static String getAudioContainerByITag(YoutubeITAG dashAudio) {
        for (MediaQualityInterface t : dashAudio.getQualityTags()) {

            if (t instanceof AudioCodec) {
                switch ((AudioCodec) t) {
                case AAC:
                    return "AAC";
                case AAC_M4A:
                    return "M4A";
                case AMRNB:
                    return "AMR";
                case MP3:
                    return "MP3";
                case OPUS:
                    return "Ogg";
                case VORBIS:
                    return "Ogg";

                }
            }
        }
        throw new WTFException("Unknown AUdio Codec");
    }

    private static String getBitrateByItag(YoutubeITAG dashAudio) {
        return new Regex(dashAudio.getQualityAudio(), "(\\d+)k").getMatch(0);
    }

    private static String fpsByItag(YoutubeITAG dashVideo) {

        for (MediaQualityInterface t : dashVideo.getQualityTags()) {
            if (t == MediaTagsVarious.VIDEO_FPS_15) {
                return "15FPS";
            }
            if (t == MediaTagsVarious.VIDEO_FPS_6) {
                return "6FPS";
            }
            if (t == MediaTagsVarious.VIDEO_FPS_60) {
                return "60FPS";
            }
        }
        String fps = new Regex(dashVideo.name(), "(\\d+fps)").getMatch(0);
        if (fps == null) {
            fps = new Regex(dashVideo.name(), "fps(\\d+)").getMatch(0);
            if (fps == null) {
                fps = "30FPS";
            } else {
                fps = fps + "FPS";
            }
        }
        return fps;
    }

    private static class VariantInfo {

        public String       fps;
        private String      baseName;
        public String       id;
        public VariantGroup group;
        public DownloadType downloadType;
        public String       fileExtension;
        public YoutubeITAG  videoItag;
        public YoutubeITAG  audioItag;
        public YoutubeITAG  dataItag;

        public String       converter;
        public String       extender;

        public VariantInfo(String name) {
            this.baseName = name;
        }

        public String generateVariantSource(HashSet<String> dupes) {

            String name = createName();
            int i = 2;
            while (dupes.contains(name)) {
                name = createName() + "_" + (i++);
            }
            dupes.add(name);
            StringBuilder sb = new StringBuilder();
            sb.append(",\r\n");
            sb.append(name).append("(");
            sb.append("\"").append(id.toUpperCase(Locale.ENGLISH)).append("\"");
            sb.append(", ");
            sb.append("YoutubeVariantInterface.VariantGroup." + group.name());
            sb.append(", ");
            sb.append("YoutubeVariantInterface.DownloadType." + downloadType.name());
            sb.append(", ");
            sb.append("\"").append(fileExtension).append("\"");
            sb.append(", ");

            sb.append(videoItag == null ? "null" : ("YoutubeITAG." + videoItag));
            sb.append(", ");
            sb.append(audioItag == null ? "null" : ("YoutubeITAG." + audioItag));
            sb.append(", ");
            sb.append(dataItag == null ? "null" : ("YoutubeITAG." + dataItag));
            sb.append(", ");
            sb.append("null");
            sb.append(", ");
            sb.append(converter);
            sb.append(")");
            if (extender != null) {
                sb.append("{\r\n");

                sb.append(extender);
                sb.append("}");
            }

            return sb.toString();
        }

        private String createName() {
            return baseName;
        }

    }

    private static void handleItag(YoutubeITAG itag) throws IOException {
        String fps = fpsByItag(itag);

        String resolution = getResolutionByItag(itag);
        String bitrate = getBitrateByItag(itag);
        String videoCodec = itag.getCodecVideo();
        VideoContainer container = getVideoContainerByITag(itag);
        String audioCodec = itag.getCodecAudio();
        String audioContainer = getAudioContainerByITag(itag);

        boolean is3DItag = itag.name().contains("3D");
        // String audioCodec = new Regex(dashAudio.name(), "(opus|aac)").getMatch(0);
        String name = container + "_" + codecToName(videoCodec) + "_" + resolution + "P_" + fps + "_" + codecToName(audioCodec) + "_" + bitrate + "KBIT";
        name = name.toUpperCase(Locale.ENGLISH);
        if (itag.name().contains("HLS")) {
            name = "HLS_" + name;
        }
        VariantInfo vi = new VariantInfo(name);
        vi.fps = fps;
        vi.id = resolution + "P_" + fps + "_" + getExtension(itag, false);
        vi.group = YoutubeVariantInterface.VariantGroup.VIDEO;
        vi.downloadType = DownloadType.VIDEO;
        vi.fileExtension = getExtension(itag, false).toString().toLowerCase(Locale.ENGLISH);
        if (itag.name().contains("HLS")) {
            vi.downloadType = DownloadType.HLS_VIDEO;

        }
        vi.videoItag = itag;
        vi.audioItag = null;
        vi.dataItag = null;
        // vi.getNameMethod = "_GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(), getVideoCodec(), getAudioQuality(),
        // getAudioCodec())";
        // vi.getQualityExtensionMethod = "_GUI.T.YoutubeVariant_nametag_generic_video(\"" + resolution + "p\")";
        if (!is3DItag) {
            addVi(vi);
        }

        name = container + "_" + codecToName(videoCodec) + "_3D_" + resolution + "P_" + fps + "_" + codecToName(audioCodec) + "_" + bitrate + "KBIT";
        name = name.toUpperCase(Locale.ENGLISH);
        if (itag.name().contains("HLS")) {
            name = "HLS_" + name;
        }
        vi.baseName = name;
        vi.group = VariantGroup.VIDEO_3D;
        vi.id = resolution + "P_" + fps + "_" + getExtension(itag, false) + "_3D";
        // vi.getQualityExtensionMethod = "_GUI.T.YoutubeVariant_nametag_generic_video(\"" + resolution + "p 3D\")";
        if (is3DItag || container != VideoContainer.MP4) {
            addVi(vi);
        }
        name = container + "_" + codecToName(videoCodec) + "_" + resolution + "P_" + fps + "_" + codecToName(audioCodec) + "_" + bitrate + "KBIT";
        name = name.toUpperCase(Locale.ENGLISH);
        if (itag.name().contains("HLS")) {
            name = "HLS_" + name;
        }
        name = "DEMUX_" + codecToName(audioCodec).toUpperCase(Locale.ENGLISH) + "_" + name;
        vi.fileExtension = codecToName(audioCodec).toLowerCase(Locale.ENGLISH);
        vi.baseName = name;
        vi.group = VariantGroup.AUDIO;
        vi.id = codecToName(audioCodec).toUpperCase(Locale.ENGLISH) + "_" + bitrate;
        // vi.getQualityExtensionMethod = "_GUI.T.YoutubeVariant_nametag_generic_audio(getAudioQuality())";
        // vi.getNameMethod = "_GUI.T.YoutubeVariant_name_generic_audio(getAudioQuality(), getAudioCodec())";
        vi.converter = "YoutubeConverter" + container + "To" + audioContainer + "Audio.getInstance()";
        vi.extender = generateMethod("@Override", "double getQualityRating()", "", "AudioCodec." + codecToName(audioCodec).toUpperCase(Locale.ENGLISH) + ".getRating() + AudioBitrate.KBIT_" + bitrate + ".getRating() - 0.00000" + resolution);

        // sb.append("@Override\r\n");
        // sb.append("public String getQualityExtension() {\r\n");
        // sb.append(" return " + getQualityExtensionMethod + ";\r\n");
        // sb.append("}\r\n");
        addVi(vi);
        if ("AAC".equals(audioCodec)) {

            name = container + "_" + codecToName(videoCodec) + "_" + resolution + "P_" + fps + "_" + codecToName(audioCodec) + "_" + bitrate + "KBIT";
            name = name.toUpperCase(Locale.ENGLISH);
            if (itag.name().contains("HLS")) {
                name = "HLS_" + name;
            }
            name = "DEMUX_M4A_" + name;
            vi.converter = "Youtube" + container + "To" + "M4A" + "Audio.getInstance()";
            vi.extender = generateMethod("@Override", "double getQualityRating()", "", "AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_" + bitrate + ".getRating() - 0.00000" + resolution);
            vi.fileExtension = "m4a";
            vi.baseName = name;
            vi.id = "M4A_" + bitrate;
            // vi.getNameMethod = "_GUI.T.YoutubeVariant_name_generic_audio(getAudioQuality(), \"M4A\")";
            addVi(vi);
        }

        System.out.println(1);
        // // String audioCodec = new Regex(dashAudio.name(), "(opus|aac)").getMatch(0);
        // String name3D = container + "_" + codecToName(videoCodec) + "_" + resolution + "P_" + "3D_" + fps + "_" + codecToName(audioCodec)
        // + "_" + bitrate + "KBIT";
        //
        // String nameDemux = "DEMUX_"+container + "_" + codecToName(videoCodec) + "_" + resolution + "P_" + "3D_" + fps + "_" +
        // codecToName(audioCodec) + "_" + bitrate + "KBIT";
        //
        //
        //
        // if (itag.name().contains("HLS")) {
        // download = DownloadType.HLS_VIDEO;
        // name3D = "HLS_" + name3D;
        // name = "HLS_" + name;
        // }
        // name3D = name3D.toUpperCase(Locale.ENGLISH);
        // String orgname = name;
        // int i = 2;
        // while (dupes.contains(name)) {
        // name = orgname + "_" + (i++);
        // }
        // String orgname3D = name3D;
        // i = 2;
        // while (dupes.contains(name3D)) {
        // name3D = orgname3D + "_" + (i++);
        // }
        // // System.out.println("Create Dupe unique: " + name);
        //
        // for (YoutubeVariant v : YoutubeVariant.values()) {
        // if (itag == v.getiTagVideo() && v.getiTagData() == null && v.getiTagAudio() == null) {
        // if (v.getGroup() == VariantGroup.VIDEO) {
        //
        // if (!v.name().matches(Pattern.quote(orgname) + "(_\\d)?")) {
        //// if (!YoutubeVariant.COMPATIBILITY_MAP.containsKey(v.name())) {
        //// System.out.println("COMPATIBILITY_MAP.put(\"" + v.name() + "\"," + name + ");");
        //// // Dialog.I().showMessageDialog("Replace " + v.name());
        //// }
        //
        // } else {
        // typeNormal = true;
        // }
        // }
        // if (v.getGroup() == VariantGroup.VIDEO_3D) {
        // if (!v.name().matches(Pattern.quote(orgname3D) + "(_\\d)?")) {
        //// if (!YoutubeVariant.COMPATIBILITY_MAP.containsKey(v.name())) {
        //// System.out.println("COMPATIBILITY_MAP.put(\"" + v.name() + "\"," + name3D + ");");
        //// // Dialog.I().showMessageDialog("Replace " + v.name());
        //// }
        //
        // } else {
        // type3D = true;
        // }
        // }
        // }
        // if (type3D && typeNormal) {
        // break;
        // }
        // }
        //
        // if (!typeNormal) {
        // // System.out.println("Add dupe: " + name);
        // dupes.add(name);
        //
        // sb.append(name).append("(");
        // sb.append("\"").append(resolution + "P_" + getExtension(itag, false)).append("\"");
        // sb.append(", ");
        // sb.append("YoutubeVariantInterface.VariantGroup.VIDEO");
        // sb.append(", ");
        // sb.append("YoutubeVariantInterface.DownloadType." + download.name());
        // sb.append(", ");
        // sb.append("\"").append(getExtension(itag, false).toString().toLowerCase(Locale.ENGLISH)).append("\"");
        // sb.append(", ");
        // sb.append("YoutubeITAG." + itag);
        // sb.append(", ");
        // sb.append("null");
        // sb.append(", ");
        // sb.append("null");
        // sb.append(", ");
        // sb.append("null");
        // sb.append(", ");
        // sb.append("null");
        // sb.append(")");
        // sb.append("{\r\n");
        // sb.append("@Override\r\n");
        // sb.append("public String _getName() {\r\n");
        // sb.append(" return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(), getVideoCodec(), getAudioQuality(),
        // getAudioCodec());\r\n");
        //
        // sb.append("}\r\n\r\n");
        // sb.append("@Override\r\n");
        // sb.append("public String getQualityExtension() {\r\n");
        // sb.append(" return _GUI.T.YoutubeVariant_name_generic_video(\"" + resolution + "p\",\"" + getExtension(itag, false) +
        // "\");\r\n");
        // sb.append("}\r\n");
        // sb.append("},\r\n");
        //
        // }
        // if (!type3D) {
        // dupes.add(name3D);
        //
        // sb.append(name3D).append("(");
        // sb.append("\"").append(resolution + "P_" + getExtension(itag, false)).append("\"");
        // sb.append(", ");
        // sb.append("YoutubeVariantInterface.VariantGroup.VIDEO_3D");
        // sb.append(", ");
        // sb.append("YoutubeVariantInterface.DownloadType." + download.name());
        // sb.append(", ");
        // sb.append("\"").append(getExtension(itag, false).toString().toLowerCase(Locale.ENGLISH)).append("\"");
        // sb.append(", ");
        // sb.append("YoutubeITAG." + itag);
        // sb.append(", ");
        // sb.append("null");
        // sb.append(", ");
        // sb.append("null");
        // sb.append(", ");
        // sb.append("null");
        // sb.append(", ");
        // sb.append("null");
        // sb.append(")");
        // sb.append("{\r\n");
        // sb.append("@Override\r\n");
        // sb.append("public String _getName() {\r\n");
        // sb.append(" return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(), getVideoCodec(), getAudioQuality(),
        // getAudioCodec());\r\n");
        // sb.append("}\r\n\r\n");
        // sb.append("@Override\r\n");
        // sb.append("public String getQualityExtension() {\r\n");
        // sb.append(" return _GUI.T.YoutubeVariant_name_generic_video(\"" + resolution + "p 3D\",\"" + getExtension(itag, false) +
        // "\");\r\n");
        // sb.append("}\r\n");
        // sb.append("},\r\n");
        // }
    }

    private static String getResolutionByItag(YoutubeITAG itag) {
        String resolution = new Regex(itag.getQualityVideo(), "(\\d+)p").getMatch(0);
        if (resolution == null) {
            resolution = "" + (int) itag.getQualityRating();
        }
        return resolution;
    }

    private static VideoContainer getVideoContainerByITag(YoutubeITAG itag) {
        VideoContainer container = null;

        for (MediaQualityInterface qt : itag.getQualityTags()) {
            if (qt instanceof VideoContainer) {
                container = (VideoContainer) qt;

            }
        }
        if (container == null) {
            throw new WTFException("VideoContainer missing for " + itag);
        }
        return container;
    }

    private static String generateMethod(String annotation, String header, String extra, String ret) {
        StringBuilder sb = new StringBuilder();
        sb.append(annotation);
        sb.append("\r\n");
        sb.append("public " + header + " {\r\n");
        if (extra != null) {
            sb.append("\r\n").append(extra).append("\r\n");
        }
        sb.append(" return " + ret + ";\r\n");
        sb.append("}\r\n");
        return sb.toString();
    }

    private static void addVi(VariantInfo vi) throws IOException {
        for (String regex : EXCLUDES) {
            if (vi.baseName.matches(regex)) {
                System.out.println("Ignore " + vi + "(" + regex + ")");
                return;
            }
        }

        YoutubeVariant existingVariant = getExistingVariant(vi);
        if (existingVariant == null) {
            System.out.println("Write");
            appendToSrc(vi.generateVariantSource(dupes), "// ###APPEND###");
        } else {
            String existingBaseName = existingVariant.name().replaceAll("_\\d+$", "");
            if (StringUtils.equals(existingBaseName, vi.baseName)) {
                // ok
            } else {
                System.out.println("Write");
                appendToSrc(vi.generateVariantSource(dupes), "// ###APPEND###");
                removeFromSrc("\r\n\\s*" + existingVariant.name() + "\\(.*?\\}\\s*[,;]", "");
                appendToSrc("COMPATIBILITY_MAP.put(\"" + existingVariant.name() + "\"," + vi.createName() + ");", "// ###APPEND_COMPATIBILITY_MAP###");
            }
        }
    }

    private static void removeFromSrc(String search, String replace) throws IOException {
        File project = IDEUtils.getProjectFolder(YoutubeVariant.class);
        File variantSourceFile = new File(new File(project, "src"), YoutubeVariant.class.getName().replace(".", "/") + ".java");

        String src = IO.readFileToString(variantSourceFile);
        // String found = new Regex(src, search).getMatch(-1);
        src = Pattern.compile(search, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(src).replaceAll(replace);

        variantSourceFile.delete();
        IO.writeStringToFile(variantSourceFile, src, false, SYNC.META_AND_DATA);
    }

    private static void appendToSrc(String newSrc, String tag) throws IOException {
        File project = IDEUtils.getProjectFolder(YoutubeVariant.class);
        File variantSourceFile = new File(new File(project, "src"), YoutubeVariant.class.getName().replace(".", "/") + ".java");

        String src = IO.readFileToString(variantSourceFile);

        src = src.replace(tag, newSrc + "\r\n" + tag);
        variantSourceFile.delete();
        IO.writeStringToFile(variantSourceFile, src, false, SYNC.META_AND_DATA);
    }

    private static YoutubeVariant getExistingVariant(VariantInfo vi) {
        // YoutubeVariant[] values = YoutubeVariant.values();
        for (YoutubeVariant v : YoutubeVariant.values()) {
            if (vi.group == v.getGroup() && vi.videoItag == v.getiTagVideo() && vi.audioItag == v.getiTagAudio() && vi.dataItag == v.getiTagData()) {
                String baseName = v.name().replaceAll("_\\d+$", "");
                if (StringUtils.equals(baseName, vi.baseName)) {
                    return v;
                }
            }

        }

        for (YoutubeVariant v : YoutubeVariant.values()) {
            if (vi.group == v.getGroup() && vi.videoItag == v.getiTagVideo() && vi.audioItag == v.getiTagAudio() && vi.dataItag == v.getiTagData()) {
                return v;
            }

        }
        return null;
    }

    private static String codecToName(String codec) {
        if ("H264".equals(codec)) {
            return codec;
        }
        if ("AAC".equals(codec)) {
            return codec;
        }
        if ("Sorenson H.263".equals(codec)) {
            return "H263";
        }
        if ("MP3".equals(codec)) {
            return codec;
        }
        if ("h263".equals(codec)) {
            return "H263";
        }
        if ("AMRNB".equals(codec)) {
            return codec;
        }
        if ("MPEG-4 Visual".equals(codec)) {
            return "MPEG4";
        }
        if ("VP8".equals(codec)) {
            return "VP8";
        }
        if ("VP9".equals(codec)) {
            return "VP9";
        }
        if ("Opus".equals(codec)) {
            return "OPUS";
        }
        if ("txt".equals(codec)) {
            return "TXT";
        }
        if ("Vorbis".equals(codec)) {
            return "VORBIS";
        }
        if ("vp9 Low Quality Profile".equals(codec)) {
            return "VP9_LQ";
        }
        if ("vp9 High Quality Profile".equals(codec)) {
            return "VP9_HQ";
        }
        if ("vp9 Higher Quality Profile".equals(codec)) {
            return "VP9_HQP";
        }
        return codec;
    }

    private static boolean validCombination(String videoCodec, String audioCodec) {
        if (videoCodec.toLowerCase(Locale.ENGLISH).contains("vp9") || videoCodec.toLowerCase(Locale.ENGLISH).contains("vp8")) {
            if ("Opus".equalsIgnoreCase(audioCodec)) {
                return true;
            }
            if ("Vorbis".equalsIgnoreCase(audioCodec)) {
                return true;
            }
            if ("aac".equalsIgnoreCase(audioCodec)) {
                return false;
            }
        }
        if (videoCodec.toLowerCase(Locale.ENGLISH).contains("h264") || videoCodec.toLowerCase(Locale.ENGLISH).contains("h263")) {
            if ("Opus".equalsIgnoreCase(audioCodec)) {
                return false;
            }
            if ("Vorbis".equalsIgnoreCase(audioCodec)) {
                return false;
            }
            if ("aac".equalsIgnoreCase(audioCodec)) {
                return true;
            }
        }
        return false;
    }

    private static String getExtension(YoutubeITAG dashVideo, boolean audio) {
        if (audio) {
            if ("Opus".equalsIgnoreCase(dashVideo.getCodecAudio())) {
                return "Ogg";
            }

        } else {

            VideoContainer container = null;

            for (MediaQualityInterface qt : dashVideo.getQualityTags()) {
                if (qt instanceof VideoContainer) {
                    container = (VideoContainer) qt;

                }
            }
            if (container != null) {
                switch (container) {
                case THREEGP:
                    return "3gp";
                default:
                    return container.name().toLowerCase(Locale.ENGLISH);
                }
            }
            if ("vp9".equalsIgnoreCase(dashVideo.getCodecVideo())) {
                return "WebM";
            }
            if ("H264".equalsIgnoreCase(dashVideo.getCodecVideo())) {
                return "Mp4";
            }

            if ("Sorenson H.263".equalsIgnoreCase(dashVideo.getCodecVideo())) {
                return "Flv";
            }
            if (dashVideo.name().startsWith("THREE")) {
                return "3gp";
            }

            if (dashVideo.getCodecVideo().contains("VP8")) {
                return "WebM";
            }
        }
        return null;
    }

    private QueryInfo               query;
    private String                  url;
    private String                  itag;
    private Browser                 br;
    private StreamInfo              streamInfo;
    private HashMap<Object, String> mapping;
    private YoutubeClipData         vid;
    private File                    file;

    public ItagHelper(YoutubeClipData vid, Browser br, QueryInfo query, String url) {
        this.query = query;
        this.url = url;
        this.itag = query.get("itag");
        this.br = br;
        this.vid = vid;
        mapping = new HashMap<Object, String>();
        mapping.put("mp42", "MP4");
        mapping.put("H.264 / AVC / MPEG-4 AVC / MPEG-4 part 10", "H264");
        mapping.put("AAC (Advanced Audio Coding)", "AAC");
        mapping.put("h264", "H264");
        mapping.put("aac", "AAC");
        mapping.put("Google VP9", "VP9");
        mapping.put("Google VP8", "VP9");
        mapping.put("H.263 / H.263-1996, H.263+ / H.263-1998 / H.263 version 2", "H263");

        mapping.put("mp3", "MP3");
        mapping.put("Opus", "Opus");
        mapping.put("MP3 (MPEG audio layer 3)", "MP3");
        mapping.put("flv", "FLV");
        mapping.put("mpeg4", "Mpeg-4 Visual");
        mapping.put("MPEG-4 part 2", "Mpeg-4 Visual");

        mapping.put("3gp6", "3GP");
        mapping.put("FLV (Flash Video)", "FLV");
        mapping.put("FLV / Sorenson Spark / Sorenson H.263 (Flash Video)", "Sorenson H263");
        mapping.put("AMR-NB (Adaptive Multi-Rate NarrowBand)", "AMRNB");
    }

    public Object get(Object name) {
        String ret = mapping.get(name);
        if (ret != null) {
            return ret;
        }
        if (name instanceof Integer) {
            return name;
        }
        return name;
    }

    public void run() throws IOException {
        if (!Application.isJared(null)) {
            loadStreamInfo();

            try {
                System.out.println(JSonStorage.serializeToJson(query) + "\r\n" + JSonStorage.serializeToJson(streamInfo));
                String itagName = "";
                String itagID = itag;
                if (streamInfo.getStreams().size() < 2) {
                    // dash
                    String quality = "";
                    if ("VIDEO".equals(streamInfo.getStreams().get(0).getCodec_type().toUpperCase(Locale.ENGLISH))) {
                        Stream video = streamInfo.getStreams().get(0);
                        String res = getVideoResolution(video);
                        int fps = getFPS(video);
                        itagName = "DASH_VIDEO";

                        itagName += "_ITAG" + itagID;
                        itagName += "_" + get(video.getCodec_long_name());
                        itagName += "_" + getVideoResolution(video);
                        itagName += "P_" + get(fps) + "FPS";
                        // if (streamInfo.getStreams().get(0).getWidth() < streamInfo.getStreams().get(0).getHeight()) {
                        // itagName += "_PIVOT";
                        // }
                        quality = "YoutubeITAG.VIDEO_RESOLUTION_" + getVideoResolution(video) + "P + YoutubeITAG.VIDEO_CODEC_" + upper(get(video.getCodec_long_name()).toString());
                        itagName = upper(itagName);

                        notify(itagName + "(" + itagID + ",\"" + get(video.getCodec_long_name()) + "\",\"" + res + "p\",null,null," + quality + "),");
                        // DASH_WEBM_VIDEO_2160P_VP9(272, "VP9", "2160p", null, null, 2160.3),

                    } else {
                        Stream audio = streamInfo.getStreams().get(0);
                        int audioBitrate = getKBIT(audio);
                        itagName = "DASH_AUDIO";

                        itagName += "_" + get(audio.getCodec_long_name());
                        itagName += "_" + audioBitrate + "KBIT";
                        quality = " YoutubeITAG.AUDIO_CODEC_" + upper(get(audio.getCodec_long_name())) + "_" + get(audioBitrate);

                        itagName = upper(itagName);

                        notify(itagName + "(" + itagID + ",null,null,\"" + get(audio.getCodec_long_name()) + "\",\"" + get(audioBitrate) + "kbit\"," + quality + "),");

                    }

                } else {
                    Stream audio = null;
                    Stream video = null;

                    if (streamInfo.getStreams().get(0).getCodec_type().equalsIgnoreCase("video")) {
                        video = streamInfo.getStreams().get(0);
                        audio = streamInfo.getStreams().get(1);
                    } else {
                        video = streamInfo.getStreams().get(1);
                        audio = streamInfo.getStreams().get(2);
                    }
                    int fps = getFPS(video);
                    int audioBitrate = getKBIT(audio);
                    String res = getVideoResolution(video);
                    itagName = get(getContainerName()) + "";
                    ;
                    itagName += "_ITAG" + itagID;
                    itagName += "_" + get(video.getCodec_long_name());
                    itagName += "_" + getVideoResolution(video);
                    itagName += "P_" + get(fps) + "FPS";
                    // if (video.getWidth() < video.getHeight()) {
                    // itagName += "_PIVOT";
                    // }

                    itagName += "_" + get(audio.getCodec_long_name());
                    itagName += "_" + audioBitrate + "KBIT";
                    // MP4_VIDEO_720P_H264_AUDIO_AAC(22, "H264", "720p", "AAC", "192kbit", 720.4 + YoutubeITAG.AAC_192),
                    itagName = upper(itagName);

                    // YoutubeITAG.VIDEO_RESOLUTION_144P + YoutubeITAG.VIDEO_CODEC_H263 + YoutubeITAG.AUDIO_CODEC_AMRNB_12
                    String quality = "YoutubeITAG.VIDEO_RESOLUTION_" + getVideoResolution(video) + "P + YoutubeITAG.VIDEO_CODEC_" + upper(get(video.getCodec_long_name()).toString()) + " + YoutubeITAG.AUDIO_CODEC_" + upper(get(audio.getCodec_long_name())) + "_" + get(audioBitrate);
                    String not = itagName + "(" + itagID + ",\"" + get(video.getCodec_long_name()) + "\",\"" + res + "p\",\"" + get(audio.getCodec_long_name()) + "\",\"" + get(audioBitrate) + "kbit\",";
                    // VideoResolution.P_240, VideoContainer.FLV, VideoCodec.H263, AudioCodec.MP3, AudioBitrate.KBIT_64
                    not += "VideoResolution.P_" + getVideoResolution(video) + ", ";
                    not += "VideoContainer." + upper(getContainerName()) + ", ";
                    not += "VideoCodec." + upper(get(video.getCodec_long_name()).toString()) + ", ";
                    not += "AudioCodec." + upper(get(audio.getCodec_long_name())) + ", ";
                    not += "AudioBitrate.KBIT_" + audioBitrate;

                    not += "),";
                    notify(not);
                    // Dialog.getInstance().showInputDialog(Dialog.STYLE_LARGE, "New Youtube ITag Found!",
                    // JSonStorage.serializeToJson(query) + "\r\n" + JSonStorage.serializeToJson(streamInfo));

                }

            } catch (Throwable e) {
                e.printStackTrace();
            }
            System.out.println("StreamInfo");
        }
    }

    private static void notify(String string) {
        try {
            Dialog.getInstance().showInputDialog(0, "New Youtube ITAG. add it to " + YoutubeITAG.class, string);
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
    }

    public int getKBIT(Stream audio) {
        int bps = -1;
        try {
            bps = (Integer.parseInt(audio.getBit_rate()));
        } catch (Throwable e) {
            bps = (int) ((file.length() / Double.parseDouble(streamInfo.getFormat().getDuration())) * 8);
        }
        return bps / 1000;
    }

    public int getFPS(Stream video) {
        String[] d = new Regex(video.getAvg_frame_rate(), "(\\d+)/(\\d+)").getRow(0);
        return (int) Math.ceil(Integer.parseInt(d[0]) / (double) Integer.parseInt(d[1]));
    }

    public String getVideoResolution(Stream video) {
        if (true) {
            return video.getHeight() + "";
        }
        return Math.min(video.getHeight(), video.getWidth()) + "";

    }

    private String upper(Object string) {
        String str = string.toString();
        if (Character.isDigit(str.charAt(0))) {
            switch (str.charAt(0)) {
            case '0':
                str = "zero" + str.substring(1);
                break;
            case '1':
                str = "one" + str.substring(1);
                break;
            case '2':
                str = "two" + str.substring(1);
                break;
            case '3':
                str = "three" + str.substring(1);
                break;
            case '4':
                str = "fou_" + str.substring(1);
                break;
            case '5':
                str = "five" + str.substring(1);
                break;
            case '6':
                str = "six" + str.substring(1);
                break;
            case '7':
                str = "seven" + str.substring(1);
                break;
            case '8':
                str = "eight" + str.substring(1);
                break;
            case 9:
                str = "nine" + str.substring(1);
                break;
            }
        }
        return str.toUpperCase(Locale.ENGLISH).replaceAll("[\\s\\-]+", "_");
    }

    public String getContainerName() {

        String ret = streamInfo.getFormat().getTags().getMajor_brand();
        if (ret == null) {

            ret = streamInfo.getFormat().getFormat_long_name();
        }

        return get(ret) + "";
    }

    public void loadStreamInfo() throws IOException {
        final FFprobe ffprobe = new FFprobe();
        file = Application.getResource("tmp/ytdev/stream_" + itag + "_" + vid.videoID + ".dat");
        if (!file.exists()) {
            final URLConnectionAdapter con = br.openGetConnection(url);
            FileOutputStream fos = null;
            try {
                file.getParentFile().mkdirs();
                fos = new FileOutputStream(file);
                IO.readStreamToOutputStream(-1, con.getInputStream(), fos, true);
            } finally {
                try {
                    fos.close();
                } catch (final Throwable ignore) {
                }
                try {
                    if (con != null) {
                        con.disconnect();
                    }
                } catch (final Throwable ignore) {
                }
            }
        }
        streamInfo = ffprobe.getStreamInfo(file);
    }
}
