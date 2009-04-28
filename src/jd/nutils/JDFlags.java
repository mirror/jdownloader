package jd.nutils;

public class JDFlags {

    /**
     * checks wether status has all following flags
     * 
     * @param status
     * @param flags
     * @return
     */
    public static boolean hasAllFlags(int status, int... flags) {
        for (int i : flags) {
            if ((status & i) == 0) return false;

        }
        return true;
    }

    public static boolean hasNoFlags(int status, int... flags) {
        for (int i : flags) {
            if ((status & i) > 0) return false;
        }
        return true;
    }

}
