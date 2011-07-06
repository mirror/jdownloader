//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.a;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import jd.nutils.encoding.Base64;


public class ContainerUtilites {

    private boolean checkDigest(InputStream inputStream, String orginalHash) throws IOException {
        byte[] buffer = new byte[2048];
        int read = 0;
        MessageDigest digest = null;
        String hashComputed;
        try {
            digest = MessageDigest.getInstance("SHA");
            while ((read = inputStream.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            inputStream.close();
            hashComputed = new String(Base64.encodeToByte(digest.digest(),false));
            if (hashComputed.equals(orginalHash)) { return true; }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean checkJarEntry(String entry, String hash) {
        return true;
    }

    boolean jarValid(String jarFilename) {
        try {
            boolean integrityOK = true;
            JarFile jarFile = new JarFile(jarFilename);
            JarInputStream jis = new JarInputStream(new FileInputStream(jarFilename));
            Manifest manifest = jis.getManifest();
            Map<String, Attributes> entries = manifest.getEntries();
            String entry;
            Attributes attributes;
            String hash;
            Iterator<String> iterator = entries.keySet().iterator();
            /**
             * Hier wird jeder Eintrag des Manifestes abgearbeitet. Der
             * Klassenname und der SHA Hash wird ausgelesen
             */
            while (iterator.hasNext() && integrityOK) {
                entry = iterator.next();
                attributes = entries.get(entry);
                hash = attributes.getValue("SHA1-Digest");
                integrityOK = integrityOK & checkDigest(jarFile.getInputStream(jarFile.getEntry(entry)), hash);
                checkJarEntry(entry, hash);
                System.out.println(entry);
            }
            return integrityOK;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
