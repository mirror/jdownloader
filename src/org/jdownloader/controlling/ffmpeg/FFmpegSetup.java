package org.jdownloader.controlling.ffmpeg;

import java.io.File;
import java.util.HashMap;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.AbstractValidator;
import org.appwork.storage.config.annotations.DefaultFactory;
import org.appwork.storage.config.annotations.DefaultStringArrayValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.ValidatorFactory;
import org.appwork.storage.config.defaults.AbstractDefaultFactory;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;

public interface FFmpegSetup extends ConfigInterface {

    class BinayPathValidator extends AbstractValidator<String> {

        @Override
        public void validate(String binaryPath) throws ValidationException {
            if (StringUtils.isNotEmpty(binaryPath)) {
                final File file = new File(binaryPath);
                if (!file.exists()) {
                    throw new ValidationException("Binary '" + binaryPath + "' does not exist!");
                } else if (file.isDirectory()) {
                    throw new ValidationException("Binary '" + binaryPath + "' must be a file!");
                } else if (CrossSystem.isUnix() && !file.canExecute()) {
                    throw new ValidationException("Binary '" + binaryPath + "' is not executable!");
                } else if (CrossSystem.isWindows() && !file.getName().endsWith(".exe")) {
                    throw new ValidationException("Binary '" + binaryPath + "' is not executable!");
                }
            }
        }
    }

    class DefaultFFMpegBinary extends AbstractDefaultFactory<String> {
        final String binary = "ffmpeg";

        @Override
        public String getDefaultValue() {
            if (CrossSystem.isLinux() || CrossSystem.isMac()) {
                final BinayPathValidator binaryPathValidator = new BinayPathValidator();
                for (final String path : new String[] { "/usr/bin/", "/usr/local/bin/" }) {
                    try {
                        final String binaryPath = path + binary;
                        binaryPathValidator.validate(binaryPath);
                        return binaryPath;
                    } catch (ValidationException ignore) {
                    }
                }
            }
            return null;
        }

    }

    class DefaultFFProbeBinary extends AbstractDefaultFactory<String> {
        final String binary = "ffprobe";

        @Override
        public String getDefaultValue() {
            if (CrossSystem.isLinux() || CrossSystem.isMac()) {
                final BinayPathValidator binaryPathValidator = new BinayPathValidator();
                for (final String path : new String[] { "/usr/bin/", "/usr/local/bin/" }) {
                    try {
                        final String binaryPath = path + binary;
                        binaryPathValidator.validate(binaryPath);
                        return binaryPath;
                    } catch (ValidationException ignore) {
                    }
                }
            }
            return null;
        }

    }

    @AboutConfig
    @ValidatorFactory(BinayPathValidator.class)
    @DefaultFactory(DefaultFFMpegBinary.class)
    @DescriptionForConfigEntry("full path (including binary filename) to ffmpeg")
    String getBinaryPath();

    void setBinaryPath(String path);

    @AboutConfig
    @ValidatorFactory(BinayPathValidator.class)
    @DefaultFactory(DefaultFFProbeBinary.class)
    @DescriptionForConfigEntry("full path (including binary filename) to ffprobe")
    String getBinaryPathProbe();

    void setBinaryPathProbe(String path);

    @AboutConfig
    @DefaultStringArrayValue({ "-i", "%video", "-i", "%audio", "-map", "0:0", "-c:v", "copy", "-map", "1:0", "-c:a", "copy", "-f", "mp4", "%out", "-y" })
    String[] getMuxToMp4Command();

    void setMuxToMp4Command(String[] command);

    @AboutConfig
    @DefaultStringArrayValue({ "-i", "%audio", "-f", "mp4", "-c:a", "copy", "%out", "-y" })
    String[] getDash2M4aCommand();

    void setDash2M4aCommand(String[] command);

    @AboutConfig
    @DefaultStringArrayValue({ "-i", "%audio", "-f", "adts", "-c:a", "copy", "%out", "-y" })
    String[] getDash2AacCommand();

    void setDash2AacCommand(String[] command);

    @AboutConfig
    @DefaultStringArrayValue({ "-i", "%audio", "-vn", "-f", "adts", "-c:a", "copy", "%out", "-y" })
    String[] getDemux2AacCommand();

    void setDemux2AacCommand(String[] command);

    @AboutConfig
    @DefaultStringArrayValue({ "-i", "%audio", "-vn", "-f", "mp4", "-c:a", "copy", "%out", "-y" })
    String[] getDemux2M4aCommand();

    void setDemux2M4aCommand(String[] command);

    @AboutConfig
    @DefaultStringArrayValue({ "-i", "%audio", "-vn", "-f", "mp3", "-c:a", "copy", "%out", "-y" })
    String[] getDemux2Mp3Command();

    void setDemux2Mp3Command(String[] command);

    @AboutConfig
    @DefaultStringArrayValue({ "-i", "%audio", "-vn", "-f", "ogg", "-acodec", "libvorbis", "-aq", "4", "%out", "-y" })
    String[] getDemuxAndConvert2Ogg();

    void setDemuxAndConvert2Ogg(String[] command);

    @AboutConfig
    @DefaultStringArrayValue({ "-i", "%audio", "%map", "-acodec", "copy", "-vn", "%out", "-y" })
    String[] getDemuxGenericCommand();

    void setDemuxGenericCommand(String[] command);

    @AboutConfig
    HashMap<String, String> getExtensionToFormatMap();

    void setExtensionToFormatMap(HashMap<String, String> map);

    @AboutConfig
    @DefaultStringArrayValue({ "-i", "%video", "-i", "%audio", "-map", "0:0", "-c:v", "copy", "-map", "1:0", "-c:a", "copy", "-f", "webm", "%out", "-y" })
    String[] getMuxToWebmCommand();

    void setMuxToWebmCommand(String[] command);

    @AboutConfig
    @DefaultStringArrayValue({ "-i", "%audio", "-f", "opus", "-c:a", "copy", "%out", "-y" })
    String[] getDash2OpusAudioCommand();

    void setDash2OpusAudioCommand(String[] command);

    @AboutConfig
    @DefaultStringArrayValue({ "-i", "%audio", "-f", "ogg", "-c:a", "copy", "%out", "-y" })
    String[] getDash2OggAudioCommand();

    void setDash2OggAudioCommand(String[] command);
}
