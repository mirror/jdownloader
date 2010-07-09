package jd.plugins.optional.neembuu;

import jpfm.JPfmMount;
import jpfm.JPfmReadOnlyFileSystem;
import jpfm.MountException;
import jpfm.MountFlags;
import jpfm.UnmountException;
import jpfm.VolumeVisibility;
import jpfm.fs.SimpleReadOnlyFileSystem;
import jpfm.volume.vector.VectorRootDirectory;

/**
 * This class is bascially an interface to jfpm.JPfmMount.
 * JPfmMount should not be used directly.
 * @author Shashank Tulsyan
 */
public final class VirtualFolderManager {
    private Object /*JPfmMount*/ mount = null;//if jpfm is not installed, we will get a
    // ClassNotFoundException here, so we initialize mount as an Object
    // so that this class loads safely in all cases
    private final Neembuu neembuu;
    private final JPfmReadOnlyFileSystem fileSystem;
    private final VectorRootDirectory rootDirectory;

    // todo : remove
    private static final String ASSUMED_MOUNT_LOCATION = "c:\\jd.jpfm.mountLoc";

    /**
     * Cached copy of mount location. Whenever the user changes, should be updated.
     * Changing when the volume is mounted will have no effect.
     */
    private String mountLocation = /*assuming for now*/  ASSUMED_MOUNT_LOCATION;

    // todo : remove
    static {
        try{
            new java.io.File (ASSUMED_MOUNT_LOCATION).createNewFile();
        }catch(Exception any){

        }
    }

    /*package private*/ VirtualFolderManager(Neembuu neembuu) {
        this.neembuu = neembuu;
        rootDirectory = new VectorRootDirectory();
        // we might replace simple ROFS with a custom implementation.
        // but for now, it serves all our requirements
        fileSystem = new SimpleReadOnlyFileSystem(rootDirectory);
    }

    /*package private*/ JPfmReadOnlyFileSystem getFileSystem() {
        return fileSystem;
    }

    /**
     * The virtual folder directory.
     * VectorRootDirectory can be used to go move around any where in the
     * virtual folder using the {@link jpfm.volume.vector.VectorFileContainer#get(java.lang.String)  }.
     * @return
     */
    public VectorRootDirectory getRootDirectory() {
        return rootDirectory;
    }

    public void unMount() throws UnmountException /*can be ignored*/ {
        try{
            ((JPfmMount)mount).unMount();
        }catch(Exception any){
            
        }
    }

    public boolean isMounted(){
        /*if(mount==null)return false;
        return ((JPfmMount)mount).isMounted();*/
        return mount==null;
    }

    /*package private*/ void mount(){
        if(mount!=null){
            //tis is to ensure that, virtual folders are not left
            // like zombies here and there.
            throw new IllegalStateException("Already mounted");
        }
        // ensuring thread safety is important
        // mount only one volume at a time
        synchronized(this){
         

            // todo:
            // ensure mountLocation validity and check if it exists
            // mounting on folders requires admin priveledges on windows
            // mounting on desktop requires admin priveledges on mac os
            // linux will make all mounts at /media/<name of the virtual folder>
            //       symlink /media/<name of the virtual folder> to a folder in desktop for ease

            // if mounting as a folder new MountFlags.Builder().setFolder is not compulsary as it is handled
            // internally
            // But in case of macos if mounting on desktop
            // setting new MountFlags.Builder().setDesktop() is compulsary.
            try{
               mount = JPfmMount.mount(
                       fileSystem,
                       mountLocation,
                       new MountFlags.Builder().build(), // we might like to
                         // add options to control this parameter using the gui
                       new VolumeListener(neembuu),
                       /*exit on unmount*/false,
                       VolumeVisibility.GLOBAL
                           //this is used to limit visibility to
                           // processes of our choice
                           // Global means, windows explorer/nautilus
                           // can see us and will make read requests
                           // for making thumnails which might cause
                           // unnessary downloads.
                           // for now, this problem is handler using
                           // offline and no index flags see
                           // {@link JDFile#getFileFlags() }
                           //
                           // in future we might want to limit
                           // visibility of the virtual folders to jdownloader jvm process
                           // We might want to add this option to the gui for advanced controlling

                   );
            }catch(MountException exception){
                //do something , show some error message

            }
        }
    }

}
