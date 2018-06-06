package org.jdownloader.settings.advanced;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface AdvancedValueEditorFactory {
    Class<? extends AdvandedValueEditor<?>> value();
}
