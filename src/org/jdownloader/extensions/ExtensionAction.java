package org.jdownloader.extensions;

import java.lang.reflect.Type;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.exceptions.WTFException;
import org.appwork.txtresource.TranslateInterface;
import org.jdownloader.actions.AppAction;

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

public abstract class ExtensionAction<T extends AbstractExtension<ConfigType, TranslationType>, ConfigType extends ExtensionConfigInterface, TranslationType extends TranslateInterface> extends AppAction {

    protected TranslationType _;
    private T                 extension;
    {

        Class<?> myClass = getClass();
        while (true) {
            try {
                Type supClass = myClass.getGenericSuperclass();
                if (supClass instanceof ParameterizedTypeImpl) {
                    ParameterizedTypeImpl sc = ((ParameterizedTypeImpl) supClass);
                    Class<T> clazz = (Class<T>) sc.getActualTypeArguments()[0];
                    LazyExtension ex = ExtensionController.getInstance().getExtension(clazz);

                    extension = ((T) ex._getExtension());
                    if (extension != null) {
                        _ = extension.getTranslation();

                    }
                    break;
                } else if (supClass instanceof Class) {
                    myClass = (Class<?>) supClass;
                } else {
                    break;
                }

            } catch (Exception e) {
                throw new WTFException();

            }
        }
        if (extension == null) {

        throw new WTFException();

        }
    }

    protected T _getExtension() {
        return extension;
    }
}
