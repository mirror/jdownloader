/*
 * Copyright (C) 2012 Shashank Tulsyan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jdownloader.extensions.neembuu;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import jpfm.SystemUtils;

import org.appwork.utils.Application;
import org.appwork.utils.processes.ProcessBuilderFactory;

/**
 *
 * @author Shashank Tulsyan
 */
final class PismoInstaller {    
    public static void tryInstallingPismoFileMount(Logger l,boolean uninstall)throws Exception{
        char s = File.separatorChar;
        String pismoInstallerDirectory = Application.getHome()+s+"libs"+s+"neembuu"+s+"pfmap"+s;
        
        if(SystemUtils.IS_OS_WINDOWS)pismoInstallerDirectory += "win"+s;
        if(SystemUtils.IS_OS_MAC)pismoInstallerDirectory += "mac"+s;
        if(SystemUtils.IS_OS_LINUX)pismoInstallerDirectory += "lin"+s;
        
        File installerDirectory = new File(pismoInstallerDirectory);
        if(!installerDirectory.exists() || !installerDirectory.isDirectory()){
            throw new IllegalStateException("Installation directory not found at "+installerDirectory.getAbsolutePath());
        }
        String pismoInstaller = "pfminst";
        if(SystemUtils.IS_OS_WINDOWS) pismoInstaller += ".exe";
        File installer = new File(installerDirectory,pismoInstaller);
        if(!installer.exists() || !installer.isFile()){
            throw new IllegalStateException("Installer binary not found at "+installer.getAbsolutePath());
        }
        String parameter = "install";
        if(uninstall) parameter = "uninstall";
        
        
        ProcessBuilder pb = ProcessBuilderFactory.create(installer.getAbsolutePath(),parameter);
        Process p = pb.start();
        boolean success = analyzeOutput(l, p, uninstall);
        if(!success)
            throw new IllegalStateException("Installation failed, check log for details");
    }
    
    /*
    --------------
    Install Output
    --------------
    Adding install reference for file "J:\ProgramsAndSoftware\PismoFileMount\pfmap-162-win_extracted_exe\x64\pfminst.exe".
    Copying file "J:\ProgramsAndSoftware\PismoFileMount\pfmap-162-win_extracted_exe\x64\ptdllrun1.exe" to "C:\Windows\system32\ptdllrun1.exe".
    NOTE: Keeping existing file "C:\Windows\system32\ptdllrun1.exe", duplicate of "J:\ProgramsAndSoftware\PismoFileMount\pfmap-162-win_extracted_exe\x64\ptdllrun1.exe".
    Copying file "J:\ProgramsAndSoftware\PismoFileMount\pfmap-162-win_extracted_exe\ptdllrun1.exe" to "C:\Windows\SysWOW64\ptdllrun1.exe".
    NOTE: Keeping existing file "C:\Windows\SysWOW64\ptdllrun1.exe", duplicate of "J:\ProgramsAndSoftware\PismoFileMount\pfmap-162-win_extracted_exe\ptdllrun1.exe".
    
    Copying file "J:\ProgramsAndSoftware\PismoFileMount\pfmap-162-win_extracted_exe\x64\pfm.exe" to "C:\Windows\pfm.exe".
    Copying file "J:\ProgramsAndSoftware\PismoFileMount\pfmap-162-win_extracted_exe\x64\pfmfs.sys" to "C:\Windows\system32\Drivers\pfmfs_640.sys".
    Configuring driver "pfmfs_640".
    Loading driver "pfmfs_640" via Service Manager.
    Copying file "J:\ProgramsAndSoftware\PismoFileMount\pfmap-162-win_extracted_exe\x64\pfmstat.exe" to "C:\Windows\pfmstat.exe".
    Copying file "J:\ProgramsAndSoftware\PismoFileMount\pfmap-162-win_extracted_exe\x64\pfmhost.exe" to "C:\Windows\pfmhost.exe".
    Copying file "J:\ProgramsAndSoftware\PismoFileMount\pfmap-162-win_extracted_exe\x64\pfmsyshost.exe" to "C:\Windows\pfmsyshost.exe".
    Copying file "J:\ProgramsAndSoftware\PismoFileMount\pfmap-162-win_extracted_exe\x64\pfmapi.dll" to "C:\Windows\system32\pfmapi_640.dll".
    Copying file "J:\ProgramsAndSoftware\PismoFileMount\pfmap-162-win_extracted_exe\pfmapi.dll" to "C:\Windows\SysWOW64\pfmapi_640.dll".
    Registering DLL "pfmapi_640.dll".
    Running command ""C:\Windows\SysWOW64\ptdllrun1.exe" -i pfmapi_640.dll".
    Registering DLL "pfmapi_640.dll".
    Running command ""C:\Windows\system32\ptdllrun1.exe" -i pfmapi_640.dll".
    Copying file "J:\ProgramsAndSoftware\PismoFileMount\pfmap-162-win_extracted_exe\x64\pfmisofs.dll" to "C:\Windows\system32\pfmisofs.dll".
    Registering formatter "C:\Windows\system32\pfmisofs.dll".
    Copying file "J:\ProgramsAndSoftware\PismoFileMount\pfmap-162-win_extracted_exe\x64\pfmpfolderfs.dll" to "C:\Windows\system32\pfmpfolderfs.dll".
    Registering formatter "C:\Windows\system32\pfmpfolderfs.dll".
    Copying file "J:\ProgramsAndSoftware\PismoFileMount\pfmap-162-win_extracted_exe\x64\pfmramfs.dll" to "C:\Windows\system32\pfmramfs.dll".
    Registering formatter "C:\Windows\system32\pfmramfs.dll".
    Copying file "J:\ProgramsAndSoftware\PismoFileMount\pfmap-162-win_extracted_exe\x64\pfmzipfs.dll" to "C:\Windows\system32\pfmzipfs.dll".
    Registering formatter "C:\Windows\system32\pfmzipfs.dll".
    Copying file "J:\ProgramsAndSoftware\PismoFileMount\pfmap-162-win_extracted_exe\pfmhost.exe" to "C:\Windows\SysWOW64\pfmhost.exe".
    Copying file "J:\ProgramsAndSoftware\PismoFileMount\pfmap-162-win_extracted_exe\pfmsyshost.exe" to "C:\Windows\SysWOW64\pfmsyshost.exe".
    Core install complete.
    
    
    -----------------------------------
    UnInstall Output for a full install
    -----------------------------------    
    Removing install reference for file "J:\ProgramsAndSoftware\PismoFileMount\pfmap-162-win_extracted_exe\x64\pfminst.exe".
    NOTE: Starting installed uninstaller "C:\Windows\pfm.exe".
    Running command ""C:\Windows\pfm.exe" uninstall -f".
    Removing install reference for file "C:\Windows\pfm.exe".
    Unconfiguring driver "pfmfs_640".
    Deleting file "C:\Windows\system32\Drivers\pfmfs_640.sys".
    Deleting file "C:\Windows\pfm.exe".
    Deleting file "C:\Windows\system32\pfmhost.exe".
    Deleting file "C:\Windows\pfmhost.exe".
    Deleting file "C:\Windows\system32\pfmsyshost.exe".
    Deleting file "C:\Windows\pfmsyshost.exe".
    Deleting file "C:\Windows\pfmstat.exe".
    Unregistering system DLL "pfmapi_640.dll".
    Running command ""C:\Windows\system32\ptdllrun1.exe" -u pfmapi_640.dll".
    Deleting file "C:\Windows\system32\pfmapi_640.dll".
    Unregistering system DLL "pfmapi_640.dll".
    Running command ""C:\Windows\SysWOW64\ptdllrun1.exe" -u pfmapi_640.dll".
    Deleting file "C:\Windows\SysWOW64\pfmapi_640.dll".
    Unregistering formatter "C:\Windows\system32\pfmisofs.dll".
    Deleting file "C:\Windows\system32\pfmisofs.dll".
    Unregistering formatter "C:\Windows\system32\pfmpfolderfs.dll".
    Deleting file "C:\Windows\system32\pfmpfolderfs.dll".
    Unregistering formatter "C:\Windows\system32\pfmramfs.dll".
    Deleting file "C:\Windows\system32\pfmramfs.dll".
    Unregistering formatter "C:\Windows\system32\pfmzipfs.dll".
    Deleting file "C:\Windows\system32\pfmzipfs.dll".
    Deleting file "C:\Windows\SysWOW64\pfmhost.exe".
    Deleting file "C:\Windows\SysWOW64\pfmsyshost.exe".
    Uninstall complete.
    NOTE: Installed uninstaller "C:\Windows\pfm.exe" finished.
    Deleting file "C:\Windows\pfm.exe".
    Deleting file "C:\Windows\system32\pfmhost.exe".
    Deleting file "C:\Windows\pfmhost.exe".
    Deleting file "C:\Windows\system32\pfmsyshost.exe".
    Deleting file "C:\Windows\pfmsyshost.exe".
    Deleting file "C:\Windows\pfmstat.exe".
    Deleting file "C:\Windows\SysWOW64\pfmhost.exe".
    Deleting file "C:\Windows\SysWOW64\pfmsyshost.exe".
    Uninstall complete.
    
    --------------------------------------
    UnInstall Output for a partial install
    --------------------------------------
    Removing install reference for file "J:\ProgramsAndSoftware\PismoFileMount\pfmap-162-win_extracted_exe\x64\pfminst.exe".
    NOTE: Reference removed but other references remain. Not removing shared files.
    C:\Users\Shashank Tulsyan\.jd_home\libs\neembuu\pfmap\win\x64\pfminst.exe
    Uninstall complete.
    */
    private static boolean analyzeOutput(Logger l, Process p,boolean uninstall)throws IOException  {
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String s = null; 
        READLINE_LOOP: while( (s=br.readLine())!=null ){
            l.info(s);
            if(s.trim().toLowerCase().contains("complete")){
                if(uninstall){
                    if(s.trim().toLowerCase().contains("uninstall"))return true;
                    else continue READLINE_LOOP;
                }
                else return true;
            }
        }
        
        br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        s = null; 
        while( (s=br.readLine())!=null ){
            l.severe(s);
        }
        
        return false;
    }
    
    
}
