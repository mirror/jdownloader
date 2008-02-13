package jd.unrar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Zip {
	private File[] srcFiles;
	private File destinationFile;
	/**
	 * Füllt die zipDatei auf die gewünschte größe auf
	 */
	public int fillSize = 0;
	/**
	 * Dateien/Ordner die nicht hinzugefügt werden sollen
	 */
	public LinkedList<File> excludeFiles = new LinkedList<File>();

	/**
	 * 
	 * @param srcFiles
	 *            Datei oder Ordner die dem Ziparchiv hinzugefügt werden sollen.
	 * @param destinationFile
	 *            die Zieldatei
	 */
	public Zip(File srcFile, File destinationFile) {
		this(new File[] { srcFile }, destinationFile);
	}

	/**
	 * 
	 * @param srcFiles
	 *            Dateien oder Ordner die dem Ziparchiv hinzugefügt werden
	 *            sollen.
	 * @param destinationFile
	 *            die Zieldatei
	 */
	public Zip(File[] srcFiles, File destinationFile) {
		this.srcFiles = srcFiles;
		this.destinationFile = destinationFile;
	}

	/**
	 * wird aufgerufen um die Zip zu erstellen
	 * 
	 * @throws Exception
	 */
	public void zip() throws Exception {
		ZipOutputStream zip = null;
		FileOutputStream fileWriter = null;

		fileWriter = new FileOutputStream(destinationFile);
		zip = new ZipOutputStream(fileWriter);
		for (int i = 0; i < srcFiles.length; i++) {
			if (srcFiles[i].isDirectory())
				addFolderToZip("", srcFiles[i].getAbsolutePath(), zip);
			else if (srcFiles[i].isFile())
				addFileToZip("", srcFiles[i].getAbsolutePath(), zip);
		}

		zip.flush();
		zip.close();
		int toFill = (int) (fillSize - destinationFile.length());

		if (toFill > 0) {
			byte[] sig = new byte[] {80,75,3,4,20,0,8,0,8,0};
			toFill-=sig.length;
			FileInputStream in = new FileInputStream(destinationFile);
			File newTarget = new File(
					destinationFile.getAbsolutePath() + ".jd");
			FileOutputStream out = new FileOutputStream(newTarget);
			out.write(sig);
			out.write(new byte[toFill]);
			int c;
			while ((c = in.read()) != -1) {
				out.write(c);
			}
			in.close();
			out.close();
			destinationFile.delete();
			newTarget.renameTo(destinationFile);
		}

	}

	private void addFileToZip(String path, String srcFile, ZipOutputStream zip)
			throws Exception {
		File folder = new File(srcFile);
		if (excludeFiles.contains(folder))
			return;
		if (folder.isDirectory()) {
			addFolderToZip(path, srcFile, zip);
		} else {
			byte[] buf = new byte[1024];
			int len;
			FileInputStream in = new FileInputStream(srcFile);
			zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
			while ((len = in.read(buf)) > 0) {
				zip.write(buf, 0, len);
			}
		}
	}

	private void addFolderToZip(String path, String srcFolder,
			ZipOutputStream zip) throws Exception {
		File folder = new File(srcFolder);
		if (excludeFiles.contains(folder))
			return;
		for (String fileName : folder.list()) {
			if (path.equals("")) {
				addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip);
			} else {
				addFileToZip(path + "/" + folder.getName(), srcFolder + "/"
						+ fileName, zip);
			}
		}
	}
}
