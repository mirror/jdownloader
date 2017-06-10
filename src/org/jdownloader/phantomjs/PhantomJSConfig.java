package org.jdownloader.phantomjs;

import java.io.File;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.AbstractValidator;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.ValidatorFactory;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;

public interface PhantomJSConfig extends ConfigInterface {

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
                }
            }
        }
    }

    @AboutConfig
    @ValidatorFactory(BinayPathValidator.class)
    @DescriptionForConfigEntry("Set a custom absolute Path to PhantomJS Binaries")
    public String getCustomBinaryPath();

    public void setCustomBinaryPath(String path);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("A way to enable/disable the use of the PhantomJS module")
    public Boolean getEnabled();

    void setEnabled(boolean b);

}
