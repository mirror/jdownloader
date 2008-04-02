package jd;

//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.

import java.awt.Graphics;
import java.awt.Image;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JWindow;

import jd.captcha.JACController;
import jd.config.Configuration;
import jd.controlling.JDController;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.PackageManager;
import jd.event.UIEvent;
import jd.gui.skins.simple.SimpleGUI;
import jd.plugins.Plugin;
import jd.unrar.JUnrar;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

/**
 * @author astaldo/JD-Team
 */

public class Main {

    private static Logger logger = JDUtilities.getLogger();

    public static void main(String args[]) {
        
    	Boolean newInstance = false;
    	
    	for ( String currentArg : args ) {
    		
    		// useful for developer version
    		if ( currentArg.equals("--new-instance") || currentArg.equals("-n") ) {
    			
    			if ( Runtime.getRuntime().maxMemory()<100000000 ){
                    JDUtilities.restartJD(args);
                }
                
    			newInstance = true;
            	startSocketServer();
            	break;
            	
    		}
    		
    	}
    	
    	// listen for command line arguments from new jD instances
    	if ( !newInstance && tryConnectSocketClient(JDUtilities.arrayToString(args,";")) ) {

    		// show help also in new instance before exit
    		if ( JDUtilities.arrayToString(args,";").contains("--help") ||
    				JDUtilities.arrayToString(args,";").contains("-h") ) {
    			showCmdHelp();
    		}
    		
    		logger.info("Send parameters to existing jD instance and exit");
    		System.exit(0);
    		
    	} else if ( !newInstance ) {

            if ( Runtime.getRuntime().maxMemory()<100000000 ){
                JDUtilities.restartJD(args);
            }
            
        	startSocketServer();
        	
    	}
        
        if( System.getProperty("os.name").toLowerCase().indexOf("mac")>=0){
            logger.info("apple.laf.useScreenMenuBar=true");
            logger.info("com.apple.mrj.application.growbox.intrudes=false");
            logger.info("com.apple.mrj.application.apple.menu.about.name=jDownloader");
           
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "jDownloader");
            System.setProperty("com.apple.mrj.application.growbox.intrudes","false");
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }
      
        JDLocale.setLocale("english");
        JDTheme.setTheme("default");
        logger.info(System.getProperty("java.class.path"));
        Boolean stop = false;
        
        // pre start parameters
        for ( String currentArg : args ) {
        	
        	if ( currentArg.equals("--help") || currentArg.equals("-h") ) {
        		showCmdHelp();
        		System.exit(0);
        	} else if ( currentArg.equals("--show") || currentArg.equals("-s") ) {
        		JACController.showDialog(false);
        		stop = true;
        	} else if ( currentArg.equals("--train") || currentArg.equals("-t") ) {
        		JACController.showDialog(false);
        		stop = true;
        	}
        	
        }
        
        if ( !stop ) {
        	
        	Main main = new Main();
        	main.go();
        	
            // post start parameters
            processParameters(args, true);
            
        }
        
    }
  
    @SuppressWarnings("unchecked")
    private void go() {

        JDInit init = new JDInit();
   
        logger.info("Registriere Plugins");
        init.init();
        init.loadImages();
        JWindow window = new JWindow() {
			private static final long serialVersionUID = 1L;

			public void paint(Graphics g) {
                Image splashImage = JDUtilities.getImage("jd_logo_large");
                g.drawImage(splashImage, 0, 0, this);
            }
        };

        window.setSize(450, 100);
        window.setLocationRelativeTo(null);
        
        if(JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getBooleanProperty(SimpleGUI.PARAM_SHOW_SPLASH, true)){
        	window.setVisible(true);
        }
     
        init.loadConfiguration();
        /*
         * Übergangsfix. Die Interactiosn wurden in eine subconfig verlegt. dieser teil kopiert bestehende events in die neue configfile       
         */
  
        if(JDUtilities.getConfiguration().getInteractions().size()>0&& JDUtilities.getSubConfig(Configuration.CONFIG_INTERACTIONS).getProperty(Configuration.PARAM_INTERACTIONS,null)==null){
            JDUtilities.getSubConfig(Configuration.CONFIG_INTERACTIONS).setProperty(Configuration.PARAM_INTERACTIONS,JDUtilities.getConfiguration().getInteractions());
            JDUtilities.getConfiguration().setInteractions(new Vector<Interaction>());
            JDUtilities.saveConfig();
        }
        final JDController controller = init.initController();
        if (init.installerWasVisible()) {
            init.doWebupdate(JDUtilities.getConfiguration().getIntegerProperty(Configuration.CID, -1),true);
        }
        else {
            init.initGUI(controller);
            init.initPlugins();
            init.loadDownloadQueue();
            init.loadModules();
            init.checkUpdate();
            
            if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL_JARED) {
                init.doWebupdate(JDUtilities.getConfiguration().getIntegerProperty(Configuration.CID, -1),false);
            }
        }
        controller.setInitStatus(JDController.INIT_STATUS_COMPLETE);
        
        //init.createQueueBackup();
        
        window.dispose();
        controller.getUiInterface().onJDInitComplete();
        Properties pr = System.getProperties();
        TreeSet propKeys = new TreeSet(pr.keySet());  
        for (Iterator it = propKeys.iterator(); it.hasNext(); ) {
            String key = (String)it.next();
           logger.finer("" + key + "=" + pr.get(key));
        }
        logger.info("jd.revision="+JDUtilities.getJDTitle());
        logger.info("jd.run="+JDUtilities.getRunType());
        logger.info("jd.lastAuthor="+JDUtilities.getLastChangeAuthor());   
        logger.info("jd.appDir="+JDUtilities.getCurrentWorkingDirectory(null));
     
        new PackageManager().interact(this);
       try {
        logger.info( Plugin.headRequest(new URL("http://share.gulli.com/files/989404514/Indi.part11.rar.html"), null, null, false).getHtmlCode());
    }
    catch (MalformedURLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    };
    
    

        
//        org.apache.log4j.Logger lg = org.apache.log4j.Logger.getLogger("jd");
//        BasicConfigurator.configure();
//   
//        lg.error("hallo Welt");
//        lg.setLevel(org.apache.log4j.Level.ALL);
        
    }
    
    private static Boolean tryConnectSocketClient(String args) {
    	
    	Socket socket = null;
        PrintWriter out = null;
        BufferedReader in = null;
        
    	int port = 9000;
    	short tries = 0;
    	Boolean success = false;
    	long timer = System.currentTimeMillis();
    	while ( success == false && tries < 10 ) {
    		
    		try {
    			
    			socket = new Socket();
    			socket.bind(null);
    			socket.connect(new InetSocketAddress("localhost", port), 50 ); 
    		
    			logger.info("Found running jD server");
    			success = true;
    			
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
    		} catch (Exception e) {
    		    
    	    	port++;
    	    	tries++;
    	    }
    		
    	}
    	
    	logger.info("Socket search lasts "+(System.currentTimeMillis()-timer)+" ms");
    	if ( success == false ) {
    		logger.warning("No running jD server found");
    		return false;
    	}
    	
        String fromServer;

        try {
		    
		    out.println(args);
        	fromServer = in.readLine();
			logger.info("jD server response: " + fromServer);
			
			out.close();
        	in.close();
        	socket.close();
        	
		} catch (IOException e) {
			e.printStackTrace();
		}
        
    	return true;
    	
    }
    
    private static Boolean startSocketServer() {
    	
    	new Thread(new Runnable(){

            public void run() {
            	
            	ServerSocket serverSocket = null;
            	int port = 9000;
            	short tries = 0;
            	Boolean success = false;
            	
            	while ( success == false && tries < 10 ) {
            		
            		try {
            			serverSocket = new ServerSocket();
            			serverSocket.bind(new InetSocketAddress("localhost", port));
            		
            			logger.info("Listen for parameters on port "+port);
            			success = true;
            		} catch (IOException e) {
            			port++;
            			tries++;
            		}
            		
            	}
            	
            	if ( success == false ) {
            		logger.severe("Could not listen to port [9000-9009]");
            	}
            	
            	Socket clientSocket = null;
            	PrintWriter out = null;
            	BufferedReader in = null;
            	Boolean acceptSuccess = true;
            	
            	try {
            		
            		while ( acceptSuccess == true ) {
            			
            			try {
            				clientSocket = serverSocket.accept();
            				
            				logger.info(clientSocket.getLocalAddress()+" - "+clientSocket.getInetAddress());
                            if(!clientSocket.getLocalAddress().equals(clientSocket.getInetAddress())){
                                clientSocket.close();
                                break;
                                
                            }
            				
            			} catch (IOException e) {
            				logger.severe("Accept failed");
            				acceptSuccess = false;
            			}
            			
            			out = new PrintWriter(clientSocket.getOutputStream(), true);
            			in = new BufferedReader(new InputStreamReader(
            					clientSocket.getInputStream()));
            			String inputLine;
            			
            			while ( (inputLine = in.readLine()) != null ) {
            				
            				String inputArray[] = inputLine.split(";");
            				processParameters(inputArray, false);
            				out.println("received");
            				
            			}
            			
            		}
        			
        			out.close();
        			in.close();
        			clientSocket.close();
        			serverSocket.close();
            		
            	} catch (IOException e) {
            		e.printStackTrace();
            	}
            	
            }
            
        }).start();
    	
    	return true;
    	
    }
    
    /**
     * 
     * TODO
     * 
     * - beim adden im hintergrund bleiben
     * 
     */
    
public static void processParameters(String[] input, Boolean isServer) {
        
    	Boolean addLinksSwitch = false;
    	Boolean addContainersSwitch = false;
    	Boolean addPasswordsSwitch = false;
    	
    	Vector<String> linksToAdd = new Vector<String>();
    	Vector<String> containersToAdd = new Vector<String>();
    	
    	for ( String currentArg : input ) {
    		
    		if ( currentArg.equals("--help") || currentArg.equals("-h") ) {
    			
            	addLinksSwitch = false;
                addContainersSwitch = false;
            	addPasswordsSwitch = false;
                
    			showCmdHelp();
    			
            } else if ( currentArg.equals("--add-links") || currentArg.equals("--add-link")
            		|| currentArg.equals("-a") ) {
            	
            	addLinksSwitch = true;
                addContainersSwitch = false;
            	addPasswordsSwitch = false;
            	logger.info(currentArg + " parameter");
            	
            } else if ( currentArg.equals("--add-containers") || currentArg.equals("--add-container")
            		|| currentArg.equals("-c") ) {
            	
            	addContainersSwitch = true;
            	addLinksSwitch = false;
            	addPasswordsSwitch = false;
            	logger.info(currentArg + " parameter");
            	
            } else if ( currentArg.equals("--add-passwords") || currentArg.equals("--add-password")
            		|| currentArg.equals("-p") ) {
            	
            	addContainersSwitch = false;
            	addLinksSwitch = false;
            	addPasswordsSwitch = true;
            	logger.info(currentArg + " parameter");
            	
            } else if ( currentArg.equals("--start-download") || currentArg.equals("-d") ) {
    			
            	addLinksSwitch = false;
                addContainersSwitch = false;
            	addPasswordsSwitch = false;
                
            	logger.info(currentArg + " parameter");
            	JDUtilities.getGUI().setStartStopButtonState(true);
            	JDUtilities.getController().startDownloads();
    			
            } else if ( currentArg.equals("--stop-download") || currentArg.equals("-D") ) {
    			
            	addLinksSwitch = false;
                addContainersSwitch = false;
            	addPasswordsSwitch = false;
                
            	logger.info(currentArg + " parameter");
            	JDUtilities.getGUI().setStartStopButtonState(false);
            	JDUtilities.getController().stopDownloads();
    			
            } else if ( currentArg.equals("--show") || currentArg.equals("-s") ) {
    			
            	addLinksSwitch = false;
                addContainersSwitch = false;
            	addPasswordsSwitch = false;
                
            	JACController.showDialog(false);
            	
            } else if ( currentArg.equals("--train") || currentArg.equals("-t") ) {
    			
            	addLinksSwitch = false;
                addContainersSwitch = false;
            	addPasswordsSwitch = false;
                
            	JACController.showDialog(true);
            	
            } else if ( currentArg.equals("--minimize") || currentArg.equals("-m") ) {
    			
            	addLinksSwitch = false;
                addContainersSwitch = false;
            	addPasswordsSwitch = false;
                
            	JDUtilities.getGUI().setMinimized(true);
            	logger.info(currentArg + " parameter");
    			
            } else if ( addLinksSwitch && currentArg.charAt(0) != '-' ) {
            	
    			linksToAdd.add(currentArg);
    			
    		} else if ( addContainersSwitch && currentArg.charAt(0) != '-' ) {
    			
    			if ( new File(currentArg).exists() ) {
    				containersToAdd.add(currentArg);
    			} else {
    				logger.warning("Container does not exist");
    			}
    			
    		} else if ( addPasswordsSwitch && !(currentArg.charAt(0) == '-') ) {
    			
    			JUnrar unrar = new JUnrar(true);
    	        unrar.addToPasswordlist(currentArg);
    	        logger.info("Add password: " + currentArg);
    			
    		} else if ( currentArg.contains("http://") && !(currentArg.charAt(0) == '-') ) {

                addContainersSwitch = false;
            	addLinksSwitch = false;
            	addPasswordsSwitch = false;
    			linksToAdd.add(currentArg);
    			
    		} else if ( new File(currentArg).exists() && !(currentArg.charAt(0) == '-') ) {

                addContainersSwitch = false;
            	addLinksSwitch = false;
            	addPasswordsSwitch = false;
    			containersToAdd.add(currentArg);
    			
    		} else {
    			
    			addContainersSwitch = false;
            	addLinksSwitch = false;
            	addPasswordsSwitch = false;
            	
    		}
            
        }
        
        if ( linksToAdd.size() > 0 ) logger.info("Links to add: " + linksToAdd.toString());
        if ( containersToAdd.size() > 0 ) logger.info("Containers to add: " + containersToAdd.toString());
        
        for ( int i = 0; i < containersToAdd.size(); i++ ) {
        	JDUtilities.getController().loadContainerFile(new File(containersToAdd.get(i)));
        }
        
        String linksToAddString = "";
        
        for ( int i = 0; i < linksToAdd.size(); i++ ) {
        	linksToAddString += linksToAdd.get(i)+"\n";
        }
        
        if ( !linksToAddString.equals("") ) {
        	JDUtilities.getGUI().fireUIEvent(new UIEvent(JDUtilities.getGUI(),
        			UIEvent.UI_LINKS_TO_PROCESS, linksToAddString));
        }
        
    }
    
    private static void showCmdHelp() {
        
    	String[][] help = new String[][] { 
				{ JDUtilities.getJDTitle(), "Coalado|Astaldo|DwD|Botzi|eXecuTe GPLv3" },
				{ "http://jdownloader.ath.cx/\t\t", "http://www.the-lounge.org/viewforum.php?f=217" + System.getProperty("line.separator") },
				{ "-h --help\t", "Show this help message" },
				{ "-a --add-link(s)", "Add links" },
				{ "-c --add-container(s)", "Add containers" },
				{ "-p --add-password(s)", "Add passwords" },
				{ "-d --start-download", "Start download" },
				{ "-D --stop-download", "Stop download" },
				{ "-m --minimize\t", "Minimize download window" },
				{ "-s --show\t", "Show JAC prepared captchas" },
				{ "-t --train\t", "Train a JAC method" },
				{ "-n --new-instance", "Force new instance if another jD is running" }
				};
        
		for ( String helpLine[] : help) {
            System.out.println(helpLine[0] + "\t" + helpLine[1]);
        }
        
    }
    
}
