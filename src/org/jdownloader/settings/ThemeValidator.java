package org.jdownloader.settings;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.annotations.AbstractValidator;
import org.appwork.utils.Application;

public class ThemeValidator extends AbstractValidator<String> {

    @Override
    public void validate(String themeID) throws ValidationException {
        if (!Application.getResource("themes/" + themeID).exists()) {
            throw new ValidationException(Application.getResource("themes/" + themeID) + " must exist");
        } else if (!Application.getResource("themes/" + themeID).isDirectory()) { throw new ValidationException(Application.getResource("themes/" + themeID) + " must be a directory"); }
    }

}
