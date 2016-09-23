package org.jdownloader.extensions;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.appwork.exceptions.WTFException;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;

public abstract class AbstractExtensionAction<T extends AbstractExtension<?, ?>> extends CustomizableAppAction {

    public AbstractExtensionAction() {
        super();
    }

    private T extension;
    {

        Class<?> myClass = getClass();
        main: while (myClass != null && extension == null) {
            try {
                Type supClass = myClass.getGenericSuperclass();
                if (supClass instanceof ParameterizedType) {

                    ParameterizedType sc = ((ParameterizedType) supClass);
                    for (Type t : sc.getActualTypeArguments()) {
                        if (t instanceof Class && AbstractExtension.class.isAssignableFrom((Class<?>) t)) {
                            Class<? extends AbstractExtension> clazz = (Class<? extends AbstractExtension>) t;
                            LazyExtension ex = ExtensionController.getInstance().getExtension(clazz);

                            extension = ((T) ex._getExtension());
                            break main;
                        }
                    }

                    myClass = (Class<?>) ((ParameterizedType) supClass).getRawType();

                } else if (supClass instanceof Class) {
                    myClass = (Class<?>) supClass;
                } else {
                    break;
                }

            } catch (Exception e) {
                e.printStackTrace();
                throw new WTFException(e);

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
