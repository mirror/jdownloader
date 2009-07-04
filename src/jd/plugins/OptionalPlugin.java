package jd.plugins;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention( value = RetentionPolicy.RUNTIME ) 
@Target( value = ElementType.TYPE ) 
public @interface OptionalPlugin {
    String id();
    int interfaceversion();
    boolean defaultEnabled() default false;
    boolean linux() default true; 
    boolean windows() default true; 
    boolean mac() default true; 
    double minJVM() default 1.5;
    String rev();
}
