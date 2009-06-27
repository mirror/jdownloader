package jd;

import java.io.File;

import jd.event.MessageEvent;
import jd.event.MessageListener;
import jd.nutils.io.JDIO;
import jd.nutils.svn.Subversion;
import jd.utils.JDUtilities;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNRevision;

public class UpdateDevWorkspace {
    private static void updateSVN(String svnadr, String path) throws SVNException {
        Subversion svn = new Subversion(svnadr);

        File dir = new File(JDUtilities.getJDHomeDirectoryFromEnvironment(), path);
        try {
            svn.cleanUp(dir, true);
        } catch (SVNException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        svn.getBroadcaster().addListener(new MessageListener() {

            public void onMessage(MessageEvent event) {
                System.out.println(event.getMessage());

            }

        });
        
        try {
        svn.revert(dir);
        }catch(Exception e){
            e.printStackTrace();
            JDIO.removeDirectoryOrFile(dir);
            svn.update(dir, SVNRevision.HEAD);
        }

    }
    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            System.out.println("Update ressources at  " + JDUtilities.getJDHomeDirectoryFromEnvironment());
    
            updateSVN("svn://svn.jdownloader.org/jdownloader/trunk/ressourcen/jd/","jd");
            updateSVN("svn://svn.jdownloader.org/jdownloader/trunk/ressourcen/tools/","tools");
           

        } catch (SVNException e) {
            e.printStackTrace();
        }
    }

}
