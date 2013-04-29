package org.jdownloader.extensions;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.exceptions.WTFException;
import org.appwork.txtresource.TranslateInterface;
import org.jdownloader.actions.AppAction;

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

public abstract class ExtensionAction<T extends AbstractExtension<ConfigType, TranslationType>, ConfigType extends ExtensionConfigInterface, TranslationType extends TranslateInterface> extends AppAction {

    protected TranslationType _;
    private T                 extension;
    {

        try {
            ParameterizedTypeImpl sc = (ParameterizedTypeImpl) getClass().getGenericSuperclass();
            Class<T> clazz = (Class<T>) sc.getActualTypeArguments()[0];
            LazyExtension ex = ExtensionController.getInstance().getExtension(clazz);
            if (ex._isEnabled()) {
                extension = ((T) ex._getExtension());

                _ = extension.getTranslation();
            }

        } catch (Exception e) {
            throw new WTFException();

        }
        if (extension == null) {

        throw new WTFException();

        }
    }

    protected T _getExtension() {
        return extension;
    }
}
