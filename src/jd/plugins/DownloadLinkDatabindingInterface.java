package jd.plugins;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface DownloadLinkDatabindingInterface {
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD })
    public static @interface Key {

        String value();

    }
}
