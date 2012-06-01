package org.jdownloader.gui.uiserio;



public class NewUIO {

    private static UserIOInterface USERIO = null;

    public static void setUserIO(UserIOInterface io) {
        USERIO = io;
    }

    public static UserIOInterface I() {
        return USERIO;
    }

}
