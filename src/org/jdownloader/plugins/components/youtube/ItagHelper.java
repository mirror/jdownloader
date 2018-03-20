package org.jdownloader.plugins.components.youtube;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.IO.SYNC;
import org.appwork.utils.StringUtils;
import org.appwork.utils.ide.IDEUtils;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.ffmpeg.FFprobe;
import org.jdownloader.controlling.ffmpeg.json.Stream;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.plugins.components.youtube.itag.AudioBitrate;
import org.jdownloader.plugins.components.youtube.itag.AudioCodec;
import org.jdownloader.plugins.components.youtube.itag.VideoCodec;
import org.jdownloader.plugins.components.youtube.itag.VideoFrameRate;
import org.jdownloader.plugins.components.youtube.itag.VideoResolution;
import org.jdownloader.plugins.components.youtube.itag.YoutubeITAG;
import org.jdownloader.plugins.components.youtube.keepForCompatibility.YoutubeVariantOld;
import org.jdownloader.plugins.components.youtube.variants.DownloadType;
import org.jdownloader.plugins.components.youtube.variants.FileContainer;
import org.jdownloader.plugins.components.youtube.variants.VariantBase;
import org.jdownloader.plugins.components.youtube.variants.VariantGroup;

import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;

public class ItagHelper {
    private static HashSet<String> dupes;
    private static HashSet<String> EXCLUDES;

    public static int fillCompatibility() throws IOException {
        Application.setApplication(".jd_home");
        if (dupes == null) {
            dupes = new HashSet<String>();
        }
        int i = 0;
        for (YoutubeVariantOld vo : YoutubeVariantOld.values()) {
            if (!VariantBase.COMPATIBILITY_MAP.containsKey(vo.name())) {
                System.out.println(vo);
                i = handleCompatibility(i, vo);
            }
            // if (vo.getId() != null && dupes.add(vo.getId()) && !VariantBase.COMPATIBILITY_MAP_ID.containsKey(vo.getId())) {
            //
            // System.out.println(vo);
            //
            // for (VariantBase v : VariantBase.values()) {
            // if (vo.getiTagAudio() == v.getiTagAudio()) {
            // if (vo.getiTagData() == v.getiTagData()) {
            // if (vo.getiTagVideo() == v.getiTagVideo()) {
            // if (vo.getGroup() == v.getGroup()) {
            //
            // if (StringUtils.equalsIgnoreCase(vo.getFileExtension(), v.getContainer().getExtension())) {
            // if (!StringUtils.equals(vo.getId(), AbstractVariant.get(v).getTypeId()) && AbstractVariant.get(v).getTypeId() != null) {
            // System.out.println("Match" + vo + " - " + v);
            // if (!StringUtils.equals(vo.getId(), AbstractVariant.get(v).getTypeId())) {
            // appendToSrc("COMPATIBILITY_MAP_ID.put(\"" + vo.getId() + "\",\"" + AbstractVariant.get(v).getTypeId() + "\");", "//
            // ###APPEND_COMPATIBILITY_MAP_ID###");
            // }
            // i++;
            // }
            // }
            // }
            // }
            // }
            // }
            // }
            // }
        }
        return i;
    }

    private static int handleCompatibility(int i, YoutubeVariantOld vo) throws IOException {
        boolean b = false;
        for (VariantBase v : VariantBase.values()) {
            if (vo.getiTagAudio() == v.getiTagAudio()) {
                if (vo.getiTagData() == v.getiTagData()) {
                    if (vo.getiTagVideo() == v.getiTagVideo()) {
                        if (vo.getGroup() == v.getGroup()) {
                            if (StringUtils.equalsIgnoreCase(vo.getFileExtension(), v.getContainer().getExtension())) {
                                System.out.println("Match" + vo + " - " + v);
                                if (!StringUtils.equals(vo.name(), v.name())) {
                                    appendToSrc("COMPATIBILITY_MAP.put(\"" + vo.name() + "\"," + v.name() + ");", "// ###APPEND_COMPATIBILITY_MAP###");
                                }
                                i++;
                                b = true;
                            }
                        }
                    }
                }
            }
        }
        if (!b) {
            System.out.println("Not Found");
        }
        return i;
    }

    public static void main(String[] args) throws DialogClosedException, DialogCanceledException, IOException {
        Application.setApplication(".jd_home");
        // fillCompatibility();
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
            case DESCRIPTION:
            case IMAGE_HQ:
            case IMAGE_LQ:
            case IMAGE_MAX:
            case IMAGE_MQ:
                continue loop;
            }
            if (tag.getAudioCodec() == null) {
                dashVideos.add(tag);
            } else if (tag.getVideoCodec() == null) {
                dashAudios.add(tag);
            } else {
                nonDash.add(tag);
            }
        }
        dupes = new HashSet<String>();
        for (VariantBase v : VariantBase.values()) {
            dupes.add(v.name());
        }
        for (YoutubeITAG itag : nonDash) {
            handleItag(itag);
        }
        for (YoutubeITAG dashAudio : dashAudios) {
            for (YoutubeITAG dashVideo : dashVideos) {
                VideoCodec videoCodec = dashVideo.getVideoCodec();
                AudioCodec audioCodec = dashAudio.getAudioCodec();
                if (!validCombination(videoCodec, audioCodec)) {
                    continue;
                }
                handleDashCombination(dashVideo, dashAudio);
            }
        }
    }

    private static Class<?> caller = ItagHelper.class;

    private static void handleDashCombination(YoutubeITAG dashVideo, YoutubeITAG dashAudio) throws IOException {
        if (dashVideo.name().contains("HLS")) {
            throw new WTFException("DASH HLS");
        }
        if (dashAudio.name().contains("HLS")) {
            throw new WTFException("DASH HLS");
        }
        VideoResolution resolution = dashVideo.getVideoResolution();
        AudioBitrate bitrate = dashAudio.getAudioBitrate();
        FileContainer videoContainer = getVideoContainer(dashVideo, dashAudio);
        FileContainer audioContainer2 = getAudioContainer(dashVideo, dashAudio);
        boolean is3DItag = dashVideo.name().contains("3D");
        VideoFrameRate fps = dashVideo.getVideoFrameRate();
        VideoCodec videoCodec = dashVideo.getVideoCodec();
        AudioCodec audioCodec = dashAudio.getAudioCodec();
        String baseName = videoContainer.name() + "_" + videoCodec.name() + "_" + resolution.getHeight() + "P_" + (int) Math.ceil(fps.getFps()) + "FPS_" + audioCodec.name() + "_" + bitrate.getKbit() + "KBIT" + "_DASH";
        baseName = baseName.toUpperCase(Locale.ENGLISH);
        VariantInfo vi = new VariantInfo(baseName);
        vi.fps = fps;
        String baseID;
        vi.group = VariantGroup.VIDEO;
        vi.downloadType = DownloadType.DASH_VIDEO;
        vi.container = videoContainer;
        vi.videoItag = dashVideo;
        vi.audioItag = dashAudio;
        vi.dataItag = null;
        if (!is3DItag) {
            addVi(vi);
        } else {
            vi.baseName = baseName + "_3D";
            if (is3DItag) {
                addVi(vi);
            }
        }
        // audio
        if (!dupes.add(dashAudio.name())) {
            return;
        }
        String audioName;
        if (StringUtils.equalsIgnoreCase(audioContainer2.name(), audioCodec.name())) {
            audioName = audioContainer2.name() + "_" + bitrate.getKbit() + "KBIT" + "_DASH";
        } else {
            audioName = audioContainer2.name() + "_" + audioCodec.name() + "_" + bitrate.getKbit() + "KBIT" + "_DASH";
        }
        audioName = audioName.toUpperCase(Locale.ENGLISH);
        vi.baseName = audioName;
        vi.group = VariantGroup.AUDIO;
        vi.downloadType = DownloadType.DASH_AUDIO;
        vi.container = audioContainer2;
        vi.videoItag = null;
        // vi.getQualityExtensionMethod = "_GUI.T.YoutubeVariant_nametag_generic_audio(getAudioQuality())";
        // vi.getNameMethod = "_GUI.T.YoutubeVariant_name_generic_audio(getAudioQuality(), getAudioCodec())";
        addVi(vi);
        switch (audioContainer2) {
        case AAC:
            audioContainer2 = FileContainer.M4A;
            vi.container = audioContainer2;
            audioName = audioContainer2.name() + "_" + audioCodec.name() + "_" + bitrate.getKbit() + "KBIT" + "_DASH";
            vi.baseName = audioName.toUpperCase(Locale.ENGLISH);
            vi.extender = null;
            // generateMethod("@Override", "double getQualityRating()", "", "AudioCodec.M4A.getRating() + AudioBitrate.KBIT_" +
            // bitrate.getKbit() + ".getRating()");
            addVi(vi);
        }
    }

    private static FileContainer getVideoContainer(YoutubeITAG dashVideo, YoutubeITAG dashAudio) {
        switch (dashVideo.getRawContainer()) {
        case DASH_VIDEO:
            switch (dashVideo.getVideoCodec()) {
            case H264:
                return FileContainer.MP4;
            case H263:
                throw new WTFException();
            default:
                switch (dashAudio.getAudioCodec()) {
                case AAC:
                case AAC_SPATIAL:
                case MP3:
                    return FileContainer.MKV;
                default:
                    return FileContainer.WEBM;
                }
            }
        case FLV:
            return FileContainer.FLV;
        case MP4:
            return FileContainer.MP4;
        case THREEGP:
            return FileContainer.THREEGP;
        case WEBM:
            return FileContainer.WEBM;
        default:
            throw new WTFException();
        }
    }

    private static FileContainer getAudioContainer(YoutubeITAG dashVideo, YoutubeITAG audioCodec) {
        switch (audioCodec.getRawContainer()) {
        case DASH_AUDIO:
            switch (audioCodec.getAudioCodec()) {
            case AAC:
            case AAC_SPATIAL:
                return FileContainer.AAC;
            default:
                return FileContainer.OGG;
            }
        default:
            switch (audioCodec.getAudioCodec()) {
            case AAC:
            case AAC_SPATIAL:
                return FileContainer.AAC;
            case MP3:
                return FileContainer.MP3;
            default:
                return FileContainer.OGG;
            }
        }
    }

    private static class VariantInfo {
        public VideoFrameRate fps;
        private String        baseName;
        public VariantGroup   group;
        public DownloadType   downloadType;
        public YoutubeITAG    videoItag;
        public YoutubeITAG    audioItag;
        public YoutubeITAG    dataItag;
        public FileContainer  container;
        public String         converter;
        public String         extender;

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
            sb.append("VariantGroup." + group.name());
            sb.append(", ");
            sb.append("DownloadType." + downloadType.name());
            sb.append(", ");
            sb.append("FileContainer.").append(container).append("");
            sb.append(", ");
            sb.append(videoItag == null ? "null" : ("YoutubeITAG." + videoItag));
            sb.append(", ");
            sb.append(audioItag == null ? "null" : ("YoutubeITAG." + audioItag));
            sb.append(", ");
            sb.append(dataItag == null ? "null" : ("YoutubeITAG." + dataItag));
            sb.append(", ");
            // sb.append("null");
            // sb.append(", ");
            sb.append(converter);
            sb.append(")");
            if (extender != null) {
                sb.append("{\r\n");
                sb.append(extender);
                sb.append("}");
            }
            System.out.println(sb);
            return sb.toString();
        }

        private String createName() {
            return baseName;
        }
    }

    private static void handleItag(YoutubeITAG itag) throws IOException {
        VideoFrameRate fps = itag.getVideoFrameRate();
        VideoResolution resolution = itag.getVideoResolution();
        AudioBitrate bitrate = itag.getAudioBitrate();
        VideoCodec videoCodec = itag.getVideoCodec();
        FileContainer container = getVideoContainer(itag, itag);
        AudioCodec audioCodec = itag.getAudioCodec();
        FileContainer audioContainer = getAudioContainer(itag, itag);
        boolean is3DItag = itag.name().contains("3D");
        // String audioCodec = new Regex(dashAudio.name(), "(opus|aac)").getMatch(0);
        String baseName = container.name() + "_" + videoCodec.name() + "_" + resolution.getHeight() + "P_" + (int) Math.ceil(fps.getFps()) + "FPS_" + audioCodec.name() + "_" + bitrate.getKbit() + "KBIT";
        baseName = baseName.toUpperCase(Locale.ENGLISH);
        if (itag.name().contains("HLS")) {
            baseName = "HLS_" + baseName;
        }
        VariantInfo vi = new VariantInfo(baseName);
        vi.fps = fps;
        String baseID;
        vi.group = VariantGroup.VIDEO;
        vi.downloadType = DownloadType.VIDEO;
        vi.container = container;
        if (itag.name().contains("HLS")) {
            vi.downloadType = DownloadType.HLS_VIDEO;
        }
        vi.videoItag = itag;
        vi.audioItag = null;
        vi.dataItag = null;
        if (!is3DItag) {
            addVi(vi);
        } else {
            vi.baseName = baseName + "_3D";
            addVi(vi);
        }
        vi.container = audioContainer;
        vi.baseName = "DEMUX_" + audioCodec.name() + "_" + baseName;
        vi.group = VariantGroup.AUDIO;
        vi.converter = "YoutubeConverter" + container + "To" + audioContainer + "Audio.getInstance()";
        vi.extender = null;
        // generateMethod("@Override", "double getQualityRating()", "", "AudioCodec." + audioCodec.name() + ".getRating() +
        // AudioBitrate.KBIT_" + bitrate.getKbit() + ".getRating() - 0.00000" + resolution.getHeight());
        addVi(vi);
        if (audioCodec == AudioCodec.AAC) {
            vi.converter = "YoutubeConverter" + container + "To" + "M4A" + "Audio.getInstance()";
            vi.extender = null;
            // generateMethod("@Override", "double getQualityRating()", "", "AudioCodec.M4A.getRating() + AudioBitrate.KBIT_" +
            // bitrate.getKbit() + ".getRating() - 0.00000" + resolution.getHeight());
            vi.container = FileContainer.M4A;
            vi.baseName = "DEMUX_M4A_" + baseName;
            addVi(vi);
        }
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
        VariantBase existingVariant = getExistingVariant(vi);
        if (existingVariant == null) {
            System.out.println("Write " + vi.baseName);
            appendToSrc(vi.generateVariantSource(dupes), "// ###APPEND###");
        } else {
            String existingBaseName = existingVariant.name().replaceAll("_\\d+$", "");
            if (StringUtils.equals(existingBaseName, vi.baseName)) {
                // ok
            } else {
                System.out.println("Write " + vi.baseName + " replace " + existingVariant);
                // appendToSrc(vi.generateVariantSource(dupes), "// ###APPEND###");
                // removeFromSrc("\r\n\\s+" + existingVariant.name() + "\\(.*?\\}\\s*[,;]", "");
                // appendToSrc("COMPATIBILITY_MAP.put(\"" + existingVariant.name() + "\"," + vi.createName() + ");", "//
                // ###APPEND_COMPATIBILITY_MAP###");
            }
        }
    }

    private static void removeFromSrc(String search, String replace) throws IOException {
        File project = IDEUtils.getProjectFolder(VariantBase.class);
        File variantSourceFile = new File(new File(project, "src"), VariantBase.class.getName().replace(".", "/") + ".java");
        String src = IO.readFileToString(variantSourceFile);
        // String found = new Regex(src, search).getMatch(-1);
        src = Pattern.compile(search, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(src).replaceAll(replace);
        variantSourceFile.delete();
        IO.writeStringToFile(variantSourceFile, src, false, SYNC.META_AND_DATA);
    }

    private static void appendToSrc(String newSrc, String tag) throws IOException {
        File project = IDEUtils.getProjectFolder(VariantBase.class);
        File variantSourceFile = new File(new File(project, "src"), VariantBase.class.getName().replace(".", "/") + ".java");
        String src = IO.readFileToString(variantSourceFile);
        src = src.replace(tag, newSrc + "\r\n" + tag);
        variantSourceFile.delete();
        IO.writeStringToFile(variantSourceFile, src, false, SYNC.META_AND_DATA);
    }

    private static VariantBase getExistingVariant(VariantInfo vi) {
        // YoutubeVariant[] values = YoutubeVariant.values();
        for (VariantBase v : VariantBase.values()) {
            if (StringUtils.equalsIgnoreCase(v.getContainer().getExtension(), vi.container.getExtension()) && vi.group == v.getGroup() && vi.videoItag == v.getiTagVideo() && vi.audioItag == v.getiTagAudio() && vi.dataItag == v.getiTagData()) {
                String baseName = v.name().replaceAll("_\\d+$", "");
                if (StringUtils.equals(baseName, vi.baseName)) {
                    return v;
                }
            }
        }
        for (VariantBase v : VariantBase.values()) {
            if (StringUtils.equalsIgnoreCase(v.getContainer().getExtension(), vi.container.getExtension()) && vi.group == v.getGroup() && vi.videoItag == v.getiTagVideo() && vi.audioItag == v.getiTagAudio() && vi.dataItag == v.getiTagData()) {
                return v;
            }
        }
        return null;
    }

    private static boolean validCombination(VideoCodec videoCodec, AudioCodec audioCodec) {
        switch (videoCodec) {
        case H263:
        case H264:
            switch (audioCodec) {
            case AAC:
            case AAC_SPATIAL:
                return true;
            default:
                return false;
            }
        case VP8:
        case VP9:
        case VP9_HDR:
        case VP9_BETTER_PROFILE_1:
        case VP9_BETTER_PROFILE_2:
        case VP9_WORSE_PROFILE_1:
            switch (audioCodec) {
            case AAC:
                return true;
            case AAC_SPATIAL:
            case MP3:
                return false;
            default:
                return true;
            }
        }
        return false;
    }

    private UrlQuery        query;
    private String          url;
    private String          itag;
    private Browser         br;
    private StreamInfo      streamInfo;
    // private HashMap<Object, String> mapping;
    private YoutubeClipData vid;
    private File            file;

    public ItagHelper(YoutubeClipData vid, Browser br, UrlQuery query, String url) {
        this.query = query;
        this.url = url;
        this.itag = query.get("itag");
        this.br = br;
        this.vid = vid;
    }
    // public Object get(Object name) {
    // String ret = mapping.get(name);
    // if (ret != null) {
    // return ret;
    // }
    // if (name instanceof Integer) {
    // return name;
    // }
    // return name;
    // }
    // public void run() throws IOException {
    // if (!Application.isJared(null)) {
    // loadStreamInfo();
    //
    // try {
    // System.out.println(JSonStorage.serializeToJson(query) + "\r\n" + JSonStorage.serializeToJson(streamInfo));
    // String itagName = "";
    // String itagID = itag;
    // if (streamInfo.getStreams().size() < 2) {
    // // dash
    // String quality = "";
    // if ("VIDEO".equals(streamInfo.getStreams().get(0).getCodec_type().toUpperCase(Locale.ENGLISH))) {
    // Stream video = streamInfo.getStreams().get(0);
    // String res = getVideoResolution(video);
    // int fps = getFPS(video);
    // itagName = "DASH_VIDEO";
    //
    // itagName += "_ITAG" + itagID;
    // itagName += "_" + get(video.getCodec_long_name());
    // itagName += "_" + getVideoResolution(video);
    // itagName += "P_" + get(fps) + "FPS";
    // // if (streamInfo.getStreams().get(0).getWidth() < streamInfo.getStreams().get(0).getHeight()) {
    // // itagName += "_PIVOT";
    // // }
    // quality = "YoutubeITAG.VIDEO_RESOLUTION_" + getVideoResolution(video) + "P + YoutubeITAG.VIDEO_CODEC_" +
    // upper(get(video.getCodec_long_name()).toString());
    // itagName = upper(itagName);
    //
    // notify(itagName + "(" + itagID + ",\"" + get(video.getCodec_long_name()) + "\",\"" + res + "p\",null,null," + quality + "),");
    // // DASH_WEBM_VIDEO_2160P_VP9(272, "VP9", "2160p", null, null, 2160.3),
    //
    // } else {
    // Stream audio = streamInfo.getStreams().get(0);
    // int audioBitrate = getKBIT(audio);
    // itagName = "DASH_AUDIO";
    //
    // itagName += "_" + get(audio.getCodec_long_name());
    // itagName += "_" + audioBitrate + "KBIT";
    // quality = " YoutubeITAG.AUDIO_CODEC_" + upper(get(audio.getCodec_long_name())) + "_" + get(audioBitrate);
    //
    // itagName = upper(itagName);
    //
    // notify(itagName + "(" + itagID + ",null,null,\"" + get(audio.getCodec_long_name()) + "\",\"" + get(audioBitrate) + "kbit\"," +
    // quality + "),");
    //
    // }
    //
    // } else {
    // Stream audio = null;
    // Stream video = null;
    //
    // if (streamInfo.getStreams().get(0).getCodec_type().equalsIgnoreCase("video")) {
    // video = streamInfo.getStreams().get(0);
    // audio = streamInfo.getStreams().get(1);
    // } else {
    // video = streamInfo.getStreams().get(1);
    // audio = streamInfo.getStreams().get(2);
    // }
    // int fps = getFPS(video);
    // int audioBitrate = getKBIT(audio);
    // String res = getVideoResolution(video);
    // itagName = get(getContainerName()) + "";
    // ;
    // itagName += "_ITAG" + itagID;
    // itagName += "_" + get(video.getCodec_long_name());
    // itagName += "_" + getVideoResolution(video);
    // itagName += "P_" + get(fps) + "FPS";
    // // if (video.getWidth() < video.getHeight()) {
    // // itagName += "_PIVOT";
    // // }
    //
    // itagName += "_" + get(audio.getCodec_long_name());
    // itagName += "_" + audioBitrate + "KBIT";
    // // MP4_VIDEO_720P_H264_AUDIO_AAC(22, "H264", "720p", "AAC", "192kbit", 720.4 + YoutubeITAG.AAC_192),
    // itagName = upper(itagName);
    //
    // // YoutubeITAG.VIDEO_RESOLUTION_144P + YoutubeITAG.VIDEO_CODEC_H263 + YoutubeITAG.AUDIO_CODEC_AMRNB_12
    // String quality = "YoutubeITAG.VIDEO_RESOLUTION_" + getVideoResolution(video) + "P + YoutubeITAG.VIDEO_CODEC_" +
    // upper(get(video.getCodec_long_name()).toString()) + " + YoutubeITAG.AUDIO_CODEC_" + upper(get(audio.getCodec_long_name())) + "_" +
    // get(audioBitrate);
    // String not = itagName + "(" + itagID + ",\"" + get(video.getCodec_long_name()) + "\",\"" + res + "p\",\"" +
    // get(audio.getCodec_long_name()) + "\",\"" + get(audioBitrate) + "kbit\",";
    // // VideoResolution.P_240, VideoContainer.FLV, VideoCodec.H263, AudioCodec.MP3, AudioBitrate.KBIT_64
    // not += "VideoResolution.P_" + getVideoResolution(video) + ", ";
    // not += "VideoContainer." + upper(getContainerName()) + ", ";
    // not += "VideoCodec." + upper(get(video.getCodec_long_name()).toString()) + ", ";
    // not += "AudioCodec." + upper(get(audio.getCodec_long_name())) + ", ";
    // not += "AudioBitrate.KBIT_" + audioBitrate;
    //
    // not += "),";
    // notify(not);
    // // Dialog.getInstance().showInputDialog(Dialog.STYLE_LARGE, "New Youtube ITag Found!",
    // // JSonStorage.serializeToJson(query) + "\r\n" + JSonStorage.serializeToJson(streamInfo));
    //
    // }
    //
    // } catch (Throwable e) {
    // e.printStackTrace();
    // }
    // System.out.println("StreamInfo");
    // }
    // }

    // private static void notify(String string) {
    // try {
    // Dialog.getInstance().showInputDialog(0, "New Youtube ITAG. add it to " + YoutubeITAG.class, string);
    // } catch (DialogClosedException e) {
    // e.printStackTrace();
    // } catch (DialogCanceledException e) {
    // e.printStackTrace();
    // }
    // }
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

    // public String getContainerName() {
    //
    // String ret = streamInfo.getFormat().getTags().getMajor_brand();
    // if (ret == null) {
    //
    // ret = streamInfo.getFormat().getFormat_long_name();
    // }
    //
    // return get(ret) + "";
    // }
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

    public void run() {
        Dialog.getInstance().showMessageDialog("Unknown ITag found: " + query + "\r\nAsk Coalado to Update the ItagHelper for Video ID: " + vid.videoID);
    }
}
