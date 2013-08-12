package org.jdownloader.crosssystem.idlegetter;

import org.appwork.utils.os.CrossSystem;

/**
 * http://ochafik.com/blog/?p=98
 * 
 * @author Thomas
 * 
 */
public abstract class IdleGetter {

    private static final IdleGetter INSTANCE = create();

    public static IdleGetter getInstance() {
        return INSTANCE;
    }

    public static IdleGetter create() {
        switch (CrossSystem.getOS()) {
        case WINDOWS_2000:
        case WINDOWS_2003:
        case WINDOWS_7:
        case WINDOWS_8:
        case WINDOWS_SERVER_2008:
        case WINDOWS_VISTA:
            return new ModernWindowsJnaIdleGetter();

        default:
            return new BasicMousePointerIdleGetter();

        }

    }

    public abstract long getIdleTimeSinceLastUserInput();
}
