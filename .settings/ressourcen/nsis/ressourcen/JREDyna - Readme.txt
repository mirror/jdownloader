Copyright © 2009 Kevin Day nsis@trumpetinc.com

 Permission to use, copy, modify, distribute and sell this software or any part
 thereof and/or its documentation for any purpose is granted without fee provided
 that the above copyright notice and this permission notice appear in all
 copies.

 This software is provided "as is" without express or implied warranty of any kind.
 The author shall have no liability with respect to the infringement of copyrights 
 or patents that any modification to the content of this file or this file itself 
 may incur.


JREDyna.nsh

This is a library to simplify detection, downloading and installing the Java Runtime Environment. The end
result is a user experience that is similar to the dynamic .NET installation that many .NET installers perform.


This library was inspired by a couple of existing libraries:

http://nsis.sourceforge.net/New_installer_with_JRE_check_%28includes_fixes_from_%27Simple_installer_with_JRE_check%27_and_missing_jre.ini%29
http://nsis.sourceforge.net/Simple_Java_Runtime_Download_Script
http://nsis.sourceforge.net/Simple_installer_with_JRE_check


This library has the following dependencies:

InetLoad (http://nsis.sourceforge.net/InetLoad_plug-in)


To use:


Define two constants and include the header in your .nsi file as follows (note that you must do the defines before you include the header):

!define JRE_VERSION "1.6"
; The following will download the latest Windows 32 bit JRE.  You can get other bundle IDs from the Java download site.  Note that this will,
; in most cases, use the Kernel installer, which results in faster download and first time startup.  See http://java.sun.com/javase/6/6u10faq.jsp#JKernel for details
!define JRE_URL "http://javadl.sun.com/webapps/download/AutoDL?BundleId=33787"
!include "JREDyna.nsh"


1.  (Optional) JRE detection and custom NSIS page based on results.  If you wish to have a page displayed to the user if the JRE is going
to be installed, follow the steps in this section.  I find this to be a better experience for the user than putting up a modal dialog, or
just launching the Java download/install without warning them that you are going to.  Different messages are presented based on whether there is
no JRE available, or if the available JRE is too old.



When declaring your pages, use the following macro to insert the JRE notification page:

  !insertmacro CUSTOM_PAGE_JREINFO


For example:

  !insertmacro MUI_PAGE_DIRECTORY
  !insertmacro CUSTOM_PAGE_JREINFO
  !insertmacro MUI_PAGE_INSTFILES
  !insertmacro MUI_PAGE_FINISH


2.  (Required) Detection, download and installation of desired JRE.  If IfSilent flag is set, this is performed silently.  If IfSilent is not
    set, user interaction is required with the JRE installer itself (just two mouseclicks).

In one of your installation sections, make the following call:

  call DownloadAndInstallJREIfNecessary


For example:

;--------------------------------
;Section Definitions

Section "Installation of Application" SecAppFiles

  call DownloadAndInstallJREIfNecessary

  SetOutPath $INSTDIR

  MessageBox MB_OK "App would have been installed here" 
SectionEnd




Future Improvements:

TODO:  The text strings used in this page should be configurable, and should be i18n aware


TODO:  Add option to control whether the JRE install is silent (no dialogs or progress bars displayed). Personally, I don't like having it be
       completely hidden as this introduces large delays with no visible user feedback - but other users may feel differently.  Having the user 
       click a couple of buttons in Sun's installer is acceptable, I think (the same thing occurs with apps that include download of .NET, for 
       example).

TODO:  Maybe make it so JRE_URL define is optional
