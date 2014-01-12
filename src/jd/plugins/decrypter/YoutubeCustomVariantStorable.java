package jd.plugins.decrypter;

import jd.plugins.decrypter.YoutubeVariantInterface.DownloadType;
import jd.plugins.decrypter.YoutubeVariantInterface.VariantGroup;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.appwork.utils.StringUtils;

public class YoutubeCustomVariantStorable implements Storable {
    public static void main(String[] args) {
        System.out.println(JSonStorage.serializeToJson(new YoutubeCustomVariantStorable()));
    }

    private YoutubeCustomVariantStorable(/* storable */) {

    }

    private String uniqueID;

    public String getUniqueID() {
        return uniqueID;
    }

    public void setUniqueID(String uniqueID) {
        this.uniqueID = uniqueID;
    }

    public String getTypeID() {
        return typeID;
    }

    public void setTypeID(String typeID) {
        this.typeID = typeID;
    }

    public String getDownloadType() {
        return downloadType;
    }

    public void setDownloadType(String downloadType) {
        this.downloadType = downloadType;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getAudioTag() {
        return audioTag;
    }

    public void setAudioTag(String audioTag) {
        this.audioTag = audioTag;
    }

    public String getVideoTag() {
        return videoTag;
    }

    public void setVideoTag(String videoTag) {
        this.videoTag = videoTag;
    }

    public double getQualityRating() {
        return qualityRating;
    }

    public void setQualityRating(double qualityRating) {
        this.qualityRating = qualityRating;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameTag() {
        return nameTag;
    }

    public void setNameTag(String nameTag) {
        this.nameTag = nameTag;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getBinary() {
        return binary;
    }

    public void setBinary(String binary) {
        this.binary = binary;
    }

    public String[] getParameters() {
        return parameters;
    }

    public void setParameters(String[] parameters) {
        this.parameters = parameters;
    }

    private String   typeID;
    private String   downloadType;
    private String   group;
    private String   audioTag;
    private String   videoTag;
    private double   qualityRating;
    private String   name;
    private String   nameTag;
    private String   extension;
    private String   binary;
    private String[] parameters;

    public YoutubeVariantInterface toVariant() {
        YoutubeITAG audio = StringUtils.isEmpty(getAudioTag()) ? null : YoutubeITAG.valueOf(getAudioTag());
        YoutubeITAG video = StringUtils.isEmpty(getVideoTag()) ? null : YoutubeITAG.valueOf(getVideoTag());

        double qr = qualityRating;
        if (qualityRating <= 0) {
            qr = 0.001d;
            if (audio != null) qr += audio.getQualityRating();
            if (video != null) qr += video.getQualityRating();
        }

        return new YoutubeCustomConvertVariant(getUniqueID(), getTypeID(), DownloadType.valueOf(getDownloadType()), VariantGroup.valueOf(getGroup()), qr, getName(), getNameTag(), getExtension(), audio, video, null, new YoutubeExternConverter(getBinary(), getParameters()));

    }
}
