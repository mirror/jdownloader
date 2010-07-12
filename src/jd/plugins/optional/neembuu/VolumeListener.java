
package jd.plugins.optional.neembuu;

import java.util.logging.Level;
import jpfm.FormatterEvent;
import jpfm.UnderprivilegedFormatterListener;

/**
 *
 * @author Shashank Tulsyan
 */
public class VolumeListener implements UnderprivilegedFormatterListener {
    private final Neembuu neembuu;
    private final VirtualFolderManager virtuaFolderManager;
    /*package private*/ VolumeListener(
            final Neembuu neembuu,
            final VirtualFolderManager folderManager) {
        this.neembuu = neembuu;
        this.virtuaFolderManager = folderManager;
    }

    public void eventOccurred(FormatterEvent event) {
        Level level;
        switch(event.getEventType()){
            case DETACHED: // means unmounted using external
                //pismo utility or dur to jdownloader exit
                //OR due to invocatoin on unmount
            case SUCCESSFULLY_MOUNTED:
            case OTHER:
                level = Level.FINEST;
                break;
            case MESSAGE:
                level = Level.FINE;
                break;
            case ERROR:
            case WARNING:
            case INCORRECT_IMPLEMENTATION: // this is most useful
                // kind of message. It gives detailed information
                // about the kind of mistake the programmer is doing.
                level = Level.WARNING;
                break;
            case MOUNT_CREATE_FAILED:  // this is a major issue.
                // it means something really bad.
                // Developer of JPfm and/or Pfm might have to be consulted
                level = Level.SEVERE;
                break;
            default:
                level = Level.FINEST;
        }

        neembuu.getLogger().log(level, event.getMessage(), event.getException());
        if(event.getEventType()==FormatterEvent.EVENT.DETACHED){
            virtuaFolderManager.detached(this);
        }
    }

}
