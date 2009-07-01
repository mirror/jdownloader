package jd.plugins;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention( value = RetentionPolicy.RUNTIME ) 
@Target( value = ElementType.TYPE ) 
public @interface HostPlugin {
    String name();
    String urls();
    int flags() default 0;
}
