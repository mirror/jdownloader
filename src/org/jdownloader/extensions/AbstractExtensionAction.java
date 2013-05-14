package org.jdownloader.extensions;

import java.lang.reflect.Type;

import org.appwork.exceptions.WTFException;
import org.jdownloader.actions.AppAction;

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

public abstract class AbstractExtensionAction<T extends AbstractExtension<?, ?>> extends AppAction {

    private T extension;
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
