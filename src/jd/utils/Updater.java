//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import jd.config.CFGConfig;
import jd.controlling.JDLogger;
import jd.event.MessageEvent;
import jd.event.MessageListener;
import jd.http.Browser;
import jd.nutils.JDHash;
import jd.nutils.SimpleFTP;
import jd.nutils.io.JDIO;
import jd.nutils.io.JDIO.FileSelector;
import jd.nutils.svn.Subversion;
import jd.nutils.zip.Zip;
import jd.parser.Regex;
import jd.update.FileUpdate;
import jd.update.Restarter;
import jd.update.WebUpdater;

import org.tmatesoft.svn.core.SVNException;

public class Updater {
	private File pluginsDir;

	private WebUpdater webupdater;
	private ArrayList<FileUpdate> remoteFileList;
	private File workingDir;
	private JFrame frame;
	private static String UPDATE_SUB_DIR = "exclude_jd_update";

	private File updateDir;
	// private File svn;
	private File jars;

	private ArrayList<File> packedFiles;
	public static final String BRANCH = "Pinky_F";
	

	public static ArrayList<Server> SERVERLIST = new ArrayList<Server>();
	public static Server UPDATE0 = new RSYNCServer(-1,
			"http://update0.jdownloader.org/branches/" + BRANCH + "/",
			"update0.jdownloader.org", 2121, "/home/www/update/http/branches/"
					+ BRANCH + "/", false);
	public static Server UPDATE1 = new RSYNCServer(-1,
			"http://update1.jdownloader.org/branches/" + BRANCH + "/",
			"update1.jdownloader.org", 2121, "/home/www/update/http/branches/"
					+ BRANCH + "/", false);
	public static Server UPDATE2 = new RSYNCServer(-1,
			"http://update2.jdownloader.org/branches/" + BRANCH + "/",
			"update2.jdownloader.org", 2121, "/home/www/update/http/branches/"
					+ BRANCH + "/", false);
	static {

		SERVERLIST.add(UPDATE0);
		SERVERLIST.add(UPDATE2);
		SERVERLIST.add(UPDATE1);

		
		SERVERLIST.add(new RSYNCServer(-1,
				"http://update4ex.jdownloader.org/branches/" + BRANCH + "/",
				"rsync://update4ex.jdownloader.org", 0, "/jd-mirror/branches/"
						+ BRANCH + "/", false));
		// SERVERLIST.append("-1:http://jd.code4everyone.de/%BRANCH%/\r\n");
		// SERVERLIST.append("-1:http://jd.mirrors.cyb0rk.net/%BRANCH%/\r\n");

	}

	public static void main(String[] args) throws Exception {

		Browser.setGlobalConnectTimeout(500000);
		Browser.setGlobalReadTimeout(500000);
		Updater upd = new Updater();

		WebUpdater.getConfig("WEBUPDATE").save();
		System.out.println("STATUS: Webupdate");

		upd.webupdate();
		System.out.println("STATUS: move plugins");

		upd.movePlugins(getCFG("plugins_dir"));
		upd.moveJars(getCFG("dist_dir"));
		upd.removeFileOverhead();
		upd.cleanUp();
		JOptionPane.showConfirmDialog(upd.frame, "Check " + upd.updateDir
				+ " and remove files you do NOT want to update.");
		JOptionPane.showConfirmDialog(upd.frame, "Check " + upd.workingDir
				+ " and remove files that should not be in the hashlist.");
		upd.merge();
		upd.pack();

		ArrayList<File> list = upd.getLocalFileList(upd.workingDir, false);
		list.remove(0);
		for (Server serv : SERVERLIST) {
			if (!serv.isManuelUpload())
				upd.upload(list, serv);
		}

		upd.uploadHashList();
		for (Server serv : SERVERLIST) {
			if (serv.isManuelUpload())
				System.err.println("Upload files to: ftp://" + serv.getUser()
						+ ":" + serv.getPass() + "@" + serv.getIp() + ":"
						+ serv.getPort() + "" + serv.getFTPPath());
		}
		System.exit(0);
	}

	private void pack() {
		this.packedFiles = new ArrayList<File>();
		if (ask("Update Captchamethods ?")) {
			packedFiles.addAll(pack(new File(this.workingDir, "jd/captcha")));
		}

		if (ask("Update Routerscripts ?"))
			packedFiles.addAll(pack(new File(this.workingDir, "jd/router")));

		if (ask("Update Images ?"))
			packedFiles.addAll(pack(new File(this.workingDir, "jd/img")));

		if (ask("Update Licenses ?"))
			packedFiles.addAll(pack(new File(this.workingDir, "licenses")));
		if (ask("Update languages ?"))
			packedFiles.addAll(pack(new File(this.workingDir, "jd/languages")));
		FileSelector fs = new FileSelector() {

			@Override
			public boolean doIt(File file) {

				return !file.getName().endsWith(".extract");
			}

		};
		JDIO.removeRekursive(new File(this.workingDir, "jd/captcha"), fs);
		JDIO.removeRekursive(new File(this.workingDir, "jd/router"), fs);
		JDIO.removeRekursive(new File(this.workingDir, "jd/img"), fs);
		JDIO.removeRekursive(new File(this.workingDir, "licenses"), fs);
		JDIO.removeRekursive(new File(this.workingDir, "jd/languages"), fs);
		// for (File f : packedFiles) {
		// String newpath = f.getAbsolutePath().replace(
		// this.updateDir.getAbsolutePath(),
		// this.workingDir.getAbsolutePath());
		//
		// new File(newpath).delete();
		// }
	}

	private boolean ask(String string) {
		// TODO Auto-generated method stub
		return JOptionPane.showConfirmDialog(frame, string) == JOptionPane.OK_OPTION;
	}

	private void cleanUp() {
		String[] outdated = Regex.getLines(JDIO.getLocalFile(new File(
				this.workingDir, "outdated.dat")));

		for (String path : outdated) {
			if (path.trim().length() == 0)
				continue;
			if (new File(this.workingDir, path).exists()) {

				JDIO.removeDirectoryOrFile(new File(this.workingDir, path));
				System.err.println(" CLEAN UP: "
						+ new File(this.workingDir, path).getAbsolutePath());
			}
			if (new File(this.updateDir, path).exists()) {
				JDIO.removeDirectoryOrFile(new File(this.updateDir, path));
				System.err.println(" CLEAN UP: "
						+ new File(this.updateDir, path).getAbsolutePath());
			}
		}

		String[] rest = new String[] {/* "libs/svnkit.jar" */"info.txt",
				"jd/img/default/flags", "plugins/JDPremium.jar",
				"tools/Windows/recycle.exe", "tools/Windows/recycle.cpp",
				"jd/img/screenshots", "jd/img/synthetica", "updateLog.txt",
				"jdupdate.jar"

		};
		for (String path : rest) {
			if (new File(this.workingDir, path).exists()) {
				JDIO.removeDirectoryOrFile(new File(this.workingDir, path));
				System.err.println(" CLEAN UP: "
						+ new File(this.workingDir, path).getAbsolutePath());
			}
			if (new File(this.updateDir, path).exists()) {
				JDIO.removeDirectoryOrFile(new File(this.updateDir, path));
				System.err.println(" CLEAN UP: "
						+ new File(this.updateDir, path).getAbsolutePath());
			}
		}
	}

	public Updater() throws IOException, SVNException {
		workingDir = new File(".").getCanonicalFile();

		updateDir = new File(workingDir, UPDATE_SUB_DIR);
		updateDir.mkdirs();
		initGUI();
	}

	/**
	 * gets a config entry (string) asks if not available
	 * 
	 * @param key
	 * @return
	 */
	public static String getCFG(String key) {
		CFGConfig cfg = CFGConfig.getConfig("LOCALCONFIG");
		String ret = cfg.getStringProperty(key);
		if (ret == null) {
			JFrame frame = new JFrame();
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
			ret = JOptionPane.showInputDialog(frame, "get Config: " + key);
			cfg.setProperty(key, ret);
			cfg.save();
		}
		return ret;
	}

	private void moveJars(String string) throws IOException {
		jars = new File(string);
		boolean dojars = false;
		if (ask("Update Binaries (jars)?")) {
			dojars = true;
			copyDirectory(new File(jars, "libs"), new File(this.updateDir,
					"libs"));
		}

		if (dojars)
			copyFile(new File(jars, "JDownloader.jar"), new File(updateDir,
					"JDownloader.jar"));
		if (ask("Update Version number?")) {
			Subversion svn;
			try {
				svn = new Subversion("svn://svn.jdownloader.org/jdownloader");
				long head = svn.latestRevision();

				new File(updateDir, "config").mkdirs();
				JDIO.saveToFile(new File(updateDir, "config/version.cfg"),
						(head + "").getBytes());

			} catch (SVNException e) {
				e.printStackTrace();
			}
		}

		if (dojars)
			copyFile(new File(jars, "tinyupdate.jar"), new File(updateDir,
					"tools/tinyupdate.jar"));
		copyFile(new File(jars.getParentFile(), "ressourcen/outdated.dat"),
				new File(updateDir, "outdated.dat"));
		if (ask("Update Addons ?")) {
			for (File f : new File(jars.getParentFile(),
					"ressourcen/pluginressourcen").listFiles()) {
				copyDirectory(f, this.updateDir);
			}

			for (File f : new File(jars, "pluginressourcen").listFiles()) {
				copyDirectory(f, this.updateDir);
			}
		}

		copyDirectory(new File(jars.getParentFile(), "ressourcen/licenses"),
				new File(this.updateDir, "licenses"));
		copyDirectory(new File(jars.getParentFile(), "ressourcen/jd"),
				new File(this.updateDir, "jd"));
		copyDirectory(new File(jars.getParentFile(), "ressourcen/tools"),
				new File(this.updateDir, "tools"));
		if (dojars)
			copyDirectory(new File(jars.getParentFile(), "ressourcen/libs"),
					new File(this.updateDir, "libs"));

	}

	private ArrayList<File> pack(File file) {

		new File(file, file.getName() + ".extract").delete();
		//new File(file, file.getName() + ".extract").deleteOnExit();
		Zip zip = new Zip(file.listFiles(), new File(file, file.getName()
				+ ".extract"));
		zip.setDeleteAfterPack(true);
		zip.setExcludeFilter(Pattern.compile(".+/.extract",
				Pattern.CASE_INSENSITIVE));
		try {
			return zip.zip();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	// private void lockUpdate() throws IOException {
	// Browser br = new Browser();
	// br.forceDebug(true);
	// System.out.println(br.getPage("http://update0.jdownloader.org/lock.php?pass="
	// + getCFG("server_pass")));
	// System.out.println(br.getPage("http://update1.jdownloader.org/lock.php?pass="
	// + getCFG("server_pass")));
	// System.out.println(br.getPage("http://update2.jdownloader.org/lock.php?pass="
	// + getCFG("server_pass")));
	//
	// }

	private void uploadHashList() throws Exception {

		LinkedHashMap<String, String> map = createHashList(this.workingDir);
		Browser br = new Browser();
		br.forceDebug(true);
		String serverlist = createServerList();

		File f = new File(BRANCH + "_server.list");
		JDIO.writeLocalFile(f, serverlist);
		SimpleFTP.upload(UPDATE0.getIp(), UPDATE0.getPort(), UPDATE0.getUser(),
				UPDATE0.getPass(), "/http/", f);
		SimpleFTP.upload(UPDATE1.getIp(), UPDATE1.getPort(), UPDATE1.getUser(),
				UPDATE1.getPass(), "/http/", f);
		SimpleFTP.upload(UPDATE2.getIp(), UPDATE2.getPort(), UPDATE2.getUser(),
				UPDATE2.getPass(), "/http/", f);
		f.delete();

		String list = br.postPage(
				"http://update1.jdownloader.org/update.php?pass="
						+ getCFG("updateHashPW2") + "&branch=" + BRANCH, map);

		f = new File("hashlist.lst");
		JDIO.writeLocalFile(f, list);

		Zip zip = new Zip(f, new File(BRANCH + "_update.zip"));
		zip.setDeleteAfterPack(true);
		try {
			zip.zip();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JDIO.writeLocalFile(new File(BRANCH + "_update.md5"), JDHash
				.getMD5(new File(BRANCH + "_update.zip")));
		SimpleFTP.upload(UPDATE0.getIp(), UPDATE0.getPort(), UPDATE0.getUser(),
				UPDATE0.getPass(), "/http/", new File(BRANCH + "_update.zip"));
		SimpleFTP.upload(UPDATE1.getIp(), UPDATE1.getPort(), UPDATE1.getUser(),
				UPDATE1.getPass(), "/http/", new File(BRANCH + "_update.zip"));
		SimpleFTP.upload(UPDATE2.getIp(), UPDATE2.getPort(), UPDATE2.getUser(),
				UPDATE2.getPass(), "/http/", new File(BRANCH + "_update.zip"));
		SimpleFTP.upload(UPDATE0.getIp(), UPDATE0.getPort(), UPDATE0.getUser(),
				UPDATE0.getPass(), "/http/", new File(BRANCH + "_update.md5"));
		SimpleFTP.upload(UPDATE1.getIp(), UPDATE1.getPort(), UPDATE1.getUser(),
				UPDATE1.getPass(), "/http/", new File(BRANCH + "_update.md5"));
		SimpleFTP.upload(UPDATE2.getIp(), UPDATE2.getPort(), UPDATE2.getUser(),
				UPDATE2.getPass(), "/http/", new File(BRANCH + "_update.md5"));
		new File(BRANCH + "_update.md5").delete();
		new File(BRANCH + "_update.zip").delete();

		// br = br;

	}

	private String createServerList() {
		String ret = "";
		for (Server serv : SERVERLIST) {
			ret += serv.getPriority() + ":" + serv.getHttppath() + "\r\n";
		}
		return ret.trim();
	}

	/**
	 * 
	 * 
	 * @param dir
	 * @return
	 */
	private LinkedHashMap<String, String> createHashList(File dir) {
		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
		ArrayList<File> list = getLocalFileList(dir, false);

		for (File f : list) {
			String path = f.getAbsolutePath()
					.replace(dir.getAbsolutePath(), "");

			path = path.replace("/", "/");
			if (path.trim().length() == 0 || f.isDirectory()
					|| f.getName().startsWith(".") || f.getName().endsWith("~")) {

				continue;
			}

			String hash = JDHash.getMD5(f);
			int i = 1;
			String nH = hash;
			while (map.containsKey(nH)) {
				nH = hash + "_" + i;
				i++;

			}
			map.put(nH, path);
		}

		return map;
	}

	private void merge() throws IOException {
		this.copyDirectory(this.updateDir, this.workingDir);

		JDIO.removeDirectoryOrFile(updateDir);

	}

	/**
	 * Uploads this filelist. Hash is checked after upload.
	 * 
	 * @param list
	 * @param serv
	 * @throws IOException
	 */
	private boolean upload(ArrayList<File> list, Server serv)
			throws IOException {
		if (serv instanceof RSYNCServer) {
			if (serv.getIp().startsWith("rsync://")) {
				System.out.println(JDUtilities.runCommand("rsync",
						new String[] { "-c", "-av",
								this.workingDir.getAbsolutePath() + "/",
								serv.getIp() + serv.getFTPPath() }, null,
						999999));
			} else {

				System.out.println(JDUtilities.runCommand("rsync",
						new String[] {
								"-c",
								"-av",
								this.workingDir.getAbsolutePath() + "/",
								"-e",
								"ssh -p 31261",
								getCFG(this + "_rsync_user3") + "@"
										+ serv.getIp() + ":"
										+ serv.getFTPPath() }, null, 999999));
			}
		} else {
			SimpleFTP.uploadSecure(serv.getIp(), serv.getPort(),
					serv.getUser(), serv.getPass(), serv.getFTPPath(),
					this.workingDir, list.toArray(new File[] {}));
		}
		return true;
	}

	// /**
	// * Gets the diuf between lokal copy and updatelist
	// *
	// * @return
	// */
	// private ArrayList<File> getFileList() {
	//
	// System.out.println("Demerge updatelist");
	// ArrayList<File> listUpdate = this.getLocalFileList(this.workingDir,
	// true);
	// listUpdate.remove(0);
	// main: for (Iterator<File> it = listUpdate.iterator(); it.hasNext();) {
	// File file = it.next();
	//
	// String newHash = JDHash.getMD5(file);
	// String newFile = file.getAbsolutePath().replace(
	// updateDir.getAbsolutePath(), "");
	// newFile = newFile.replace("\\", "/");
	// if (newFile.trim().length() == 0)
	// continue;
	//
	// File localFile = new File(workingDir, newFile);
	// if (file.isDirectory() && localFile.isDirectory()
	// && localFile.exists()) {
	// it.remove();
	// continue main;
	// }
	//
	// String localHash = JDHash.getMD5(localFile);
	// if (localHash != null && localHash.equalsIgnoreCase(newHash)) {
	// it.remove();
	// continue main;
	// }
	//
	// System.out.println("Update: " + localFile);
	// }
	// System.out.println("Demerge updatelist finished: " + listUpdate.size()
	// + " files");
	// return listUpdate;
	// }

	/** Copies host and decryptplugins from svn dir to updatelist */
	private void movePlugins(String cfg) throws IOException {
		if (!ask("Update plugins?"))
			return;
		if (cfg == null)
			return;
		pluginsDir = new File(cfg);
		File file;
		JDIO.removeDirectoryOrFile(file = new File(this.updateDir,
				"jd/plugins/hoster"));
		copyDirectory(new File(pluginsDir, "hoster"), file);
		System.out.println("Updated BIN->" + file);
		JDIO.removeDirectoryOrFile(file = new File(this.updateDir,
				"jd/plugins/decrypter"));
		copyDirectory(new File(pluginsDir, "decrypter"), file);
		System.out.println("Updated BIN->" + file);

		JDIO.removeDirectoryOrFile(file = new File(this.updateDir,
				"jd/dynamics"));
		copyDirectory(new File(pluginsDir.getParentFile(), "dynamics"), file);
		System.out.println("Updated BIN->" + file);

	}

	/**
	 * Copies directory a to b
	 * 
	 * @param srcPath
	 * @param dstPath
	 * @throws IOException
	 */
	public void copyDirectory(File srcPath, File dstPath) throws IOException {
		if (srcPath.getAbsolutePath().contains(".svn"))
			return;

		if (srcPath.isDirectory()) {
			if (!dstPath.exists()) {
				System.out.println("Create Dir" + dstPath);
				dstPath.mkdir();
			}
			String files[] = srcPath.list();
			for (int i = 0; i < files.length; i++) {
				copyDirectory(new File(srcPath, files[i]), new File(dstPath,
						files[i]));
			}
		} else {
			copyFile(srcPath, dstPath);

		}

	}

	private void copyFile(File srcPath, File dstPath) throws IOException {
		String hashd = JDHash.getMD5(dstPath);
		String hashs = JDHash.getMD5(srcPath);

		if (srcPath.getAbsolutePath().contains(".svn"))
			return;

		if (!srcPath.exists()) {
			System.out.println("File or directory does not exist.");
			System.exit(0);
		} else {
			if (hashs.equalsIgnoreCase(hashd))
				return;
			if (dstPath.exists()) {
				dstPath.delete();
			}
			InputStream in = new FileInputStream(srcPath);
			dstPath.getParentFile().mkdirs();
			dstPath.createNewFile();
			OutputStream out = new FileOutputStream(dstPath);

			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
			System.out.println("        Copy File " + srcPath + " -> "
					+ dstPath);
		}

	}

	/**
	 * Removes files that are in working dir, but not in hashlist
	 * 
	 * @throws Exception
	 */
	private void removeFileOverhead() throws Exception {
		ArrayList<File> localFiles = getLocalFileList(workingDir, false);
		StringBuilder sb = new StringBuilder();
		ArrayList<String> remRequested = new ArrayList<String>();
		// clear folders in updaedir
		File tmp = new File(this.updateDir, "/tmp/");
		JDIO.removeDirectoryOrFile(tmp);
		tmp.mkdirs();
		tmp = new File(this.updateDir, "/config/");
		// JDIO.removeDirectoryOrFile(tmp);
		tmp.mkdirs();
		tmp = new File(this.updateDir, "/backup/");
		JDIO.removeDirectoryOrFile(tmp);
		tmp.mkdirs();
		// clear folders in local dir
		tmp = new File(this.workingDir, "/tmp/");
		JDIO.removeDirectoryOrFile(tmp);
		tmp.mkdirs();
		String version = JDIO.getLocalFile(new File(this.workingDir,
				"/config/version.cfg"));
		tmp = new File(this.workingDir, "/config/");
		JDIO.removeDirectoryOrFile(tmp);
		JDIO.writeLocalFile(new File(this.workingDir, "/config/version.cfg"),
				version);
		tmp.mkdirs();
		tmp = new File(this.workingDir, "/backup/");
		JDIO.removeDirectoryOrFile(tmp);
		tmp.mkdirs();
		int i = 0;
		// for (File f : localFiles) {
		// if (!f.isDirectory()
		// && !containsFile(f)
		// && !f.getAbsolutePath().equalsIgnoreCase(
		// workingDir.getAbsolutePath())) {
		// sb.append(f.getAbsolutePath() + "\r\n");
		//
		// remRequested.add(f.getAbsolutePath());
		// i++;
		// }
		//
		// }
		// if (true) {
		// String removeFiles = UserIOGui
		// .getInstance()
		// .requestTextAreaDialog(
		// "Files to remove",
		// "These "
		// + i
		// +
		// " files were found localy, but not in the remotehashlist. The will be removed if you don't delete them.",
		// sb.toString());
		// if (removeFiles != null) {
		// for (String line : Regex.getLines(removeFiles)) {
		// File del = new File(line.trim());
		// if (del.exists()) {
		// System.out.println("Delete " + del.getAbsolutePath());
		//
		// while (!JDIO.removeDirectoryOrFile(del)) {
		// JOptionPane
		// .showConfirmDialog(frame,
		// "COuld not delete "
		// + del.getAbsolutePath());
		//
		// }
		// }
		// }
		// }
		// }
		// /**
		// * rest move
		// *
		// */
		// for (String path : remRequested) {
		// File f = new File(path);
		// if (f.exists()) {
		// String newPath = path.replace(workingDir.getAbsolutePath(),
		// this.updateDir.getAbsolutePath());
		// File newFile = new File(newPath);
		// if (newFile.exists()
		// && newFile.lastModified() >= f.lastModified()) {
		// System.out.println("Removed " + path + "(newer file in "
		// + updateDir.getAbsolutePath());
		// f.delete();
		// } else if (newFile.exists()) {
		// System.out.println("Rename " + path + "-->" + newPath
		// + "(newer file in " + workingDir.getAbsolutePath());
		// newFile.delete();
		// f.renameTo(newFile);
		// } else {
		// System.out.println("Move " + path + "->"
		// + newFile.getAbsolutePath());
		// f.renameTo(newFile);
		// }
		// }
		//
		// }
	}

	private void initGUI() {
		this.frame = new JFrame();
		frame.setTitle("Updater");
		frame.setAlwaysOnTop(true);
		frame.setVisible(true);
	}

	/**
	 * Updates the current working dir. sync with online bin
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private void webupdate() {
		try {
			FileUpdate.WAITTIME_ON_ERROR = 1;
			webupdater = new WebUpdater();
			webupdater.setIgnorePlugins(false);
			webupdater.setWorkingdir(workingDir);
			webupdater.setOSFilter(false);
			remoteFileList = webupdater.getAvailableFiles();

			ArrayList<FileUpdate> update = (ArrayList<FileUpdate>) remoteFileList
					.clone();
			webupdater.filterAvailableUpdates(update);
			System.out.println("UPdate: " + update);
			webupdater.updateFiles(update, null);
			webupdater.getBroadcaster().addListener(new MessageListener() {

				public void onMessage(MessageEvent event) {
					System.out.println(event.getMessage());

				}

			});
			Restarter.main(new String[] { "-nolog" });

		} catch (Exception e) {
			JDLogger.exception(e);
			remoteFileList = new ArrayList<FileUpdate>();
		}

	}

	/** checks if file f is oart of the hashlist */
	private boolean containsFile(File f) {
		if (remoteFileList == null)
			return true;
		for (FileUpdate fu : remoteFileList) {
			String remote = fu.getLocalFile().getAbsolutePath();
			String local = f.getAbsolutePath();
			if (f.isDirectory()) {
				if (remote.startsWith(local)) {
					return true;
				}
			} else {

				if (f.exists()
						&& remote.equals(local)
						&& JDHash.getMD5(f)
								.equalsIgnoreCase(fu.getRemoteHash())) {
					return true;
				}
			}

		}
		return false;
	}

	/**
	 * Scans a folder rec. filters addons.lst, src and update_doif folder.
	 * 
	 * @param dir
	 * @param noFilter
	 * @return
	 */
	private ArrayList<File> getLocalFileList(File dir, boolean noFilter) {
		ArrayList<File> ret = new ArrayList<File>();
		if (noFilter
				|| (!dir.getAbsolutePath().contains("addonlist.lst") && !dir
						.getAbsolutePath().contains(UPDATE_SUB_DIR))) {
			ret.add(dir);
		} else {
			return ret;
		}
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				ret.addAll(getLocalFileList(f, noFilter));
			} else {
				if (noFilter
						|| (!f.getAbsolutePath().contains("addonlist.lst") && !f
								.getAbsolutePath().contains(UPDATE_SUB_DIR))) {
					ret.add(f);
				}
			}
		}
		return ret;
	}

	static class Server {

		private int priority;
		private String ftpPath;

		public int getPriority() {
			return priority;
		}

		public String getPass() {
			// TODO Auto-generated method stub
			return getCFG(this + "ftp_pass3");
		}

		public String getUser() {
			// TODO Auto-generated method stub
			return getCFG(this + "_ftp_user3");
		}

		public String getFTPPath() {
			// TODO Auto-generated method stub
			return ftpPath;
		}

		private String httppath;

		public String getHttppath() {
			return httppath;
		}

		public String getIp() {
			return ip;
		}

		public int getPort() {
			return port;
		}

		private String ip;
		private int port;
		private boolean manuelUpload;

		public boolean isManuelUpload() {
			return manuelUpload;
		}

		public Server(int priority, String httpPath, String ftpIP, int ftpHost,
				String ftpPath, boolean b) {
			this.priority = priority;
			this.httppath = httpPath;
			this.ip = ftpIP;
			this.port = ftpHost;
			this.ftpPath = ftpPath;
			manuelUpload = b;
		}

		public String toString() {
			return ip;
		}

	}

}
