
Pismo File Mount Audit Package build 162
Pismo Technic Inc., Copyright 2005-2011 Joe Lowe
2011.10.27
http://www.pismotechnic.com/


Documentation for Pismo File Mount Audit Package is available in
pfmap-doc.html .


Quick Start
-----------

Example using Explorer extension:

1) Browse to a .zip or .iso file in explorer.
2) Right click and select "Quick Mount" from the context menu.
- File will disappear and be replaced with a folder of the same name.
3) Browse into folder, explore, access files, return to parent
folder.
4) Right click on folder and select "Unmount" from the context menu.
- Folder will disappear and original file will reappear.


Example using command line:

From a command prompt in cmd.exe,
>pfm mount website.zip
>start website.zip\index.html
 ...
>pfm unmount website.zip


Example using Mount Control:

1) Find "Pismo File Mount Audit Package" in start menu and execute
"Mount Control" menu item.
2) Run "Mount" command from "File" menu.
- Open file dialog will appear.
3) Browse to a .zip or .iso file and click "Open" button.
- Mount options dialog will appear.
4) Click "Mount" button.
- File will appear in list in window.
5) Double click on file in list.
- Explorer browse window will open.
6) Explore, access files.
7) In Mount Control window, right click on file in list and select
"Unmount" from context menu.
- File will be removed from list. Explorer browse window will close.


Overview
--------

Pismo File Mount Audit Package (PFMAP) gives users the ability to
access files stored in container files, using the same tools used to
access files directly on hard drives and removable media. PFMAP
provides functionality similar to the Compressed Folder shell
extensions built into Windows Explorer, or to CD image mounting
utilities such as Microsoft VirtualCD or Nero ImageDrive.

PFMAP varies from existing utilities in a few key areas.

1) Ease of use.

   PFMAP gives quick and easy access to the contents of the container
   files. The contents of container files become available in the
   same place the container file exists. There is no drive letter or
   mount point. There is no need to browse to other locations or
   launch other programs.

2) Format support.

   PFMAP supports container file formats for which there is no native
   file system support. Image based products are limited to supporting
   built in file system formats such as CDFS and FAT.

3) Application compatibility.

   Shell extension and file manager type products must copy one or all
   of the files from an archive to the hard drive to allow application
   access. PFMAP allows applications to access files in place.

4) Enterprise security environment support.

   PFMAP includes security mechanisms to protect the system and to
   provide data access and visiblity control, even when being used
   from non-privileged user accounts. IT teams can deploy PFMAP for
   use by users who don't have admin rights on their workstations.

5) Extensibility.

   PFMAP is designed to enable efficient development of extensions to
   support additional container file formats and virtual file
   systems.


Features
--------

- Explorer integration.

   The core features of PFMAP are available through an Explorer shell
   extension. Context menu commands and drag-drop operations can be
   used to perform most operations.

- Command line interface.

   All functionality is available from the command line via pfm.exe.

- CD/DVD image file reader.

   ISO, CISO, CFS, ISZ, DAA, and UIF format CD/DVD image files can be
   mounted in place and contents accessed, without first burning to
   media.

- ZIP archive file reader.

   Most .ZIP format archives can be mounted in place and contents
   accessed. Contained documents and executables can be opened without
   being extracted to the hard drive.

- PFO, Private Folder reader/writer.

   Private Folder is a full featured container file system that
   provides encrypted and compressed storage of user data and
   applications. Private Folder utilizes AES encryption and PKCS5v2
   key generation.


Pismo File Mount
----------------

This application utilizes Pismo File Mount, an operating system
extension that enables access and modification to user and program
data through the file system interface of the operating system.

Commercial and non-commercial developers can integrate Pismo File
Mount into their applications using the Pismo File Mount Developer
Kit.

http://www.pismotechnic.com/pfm/


Release History
---------------

-- Build 162 - 2011.10.26

Minor enhancement/fixes to UNC support. New OSX kernel module
implementation.

-- Build 159 - 2010.07.07

Implemented PFO recovery command in pfolder utility. Allows
recovering user data from corrupted PFO file sets.

-- Build 158 - 2010.06.08

Fixed delay and popup dialog for empty floppy drive in mount
options dialog.

Improved uid/gid handling for mounts on mac/linux.

Minor bug fixes and enhancements.

-- Build 157 - 2010.05.18

First public release of development Linux and Mac versions.

Fixed compression handling issue in Private Folder that could
result in unmountable file sets.

Improved Private Folder mount logic to provide automatic recovery
mount of some corrupted file sets.

Improved handling of "abc???" style file name wildcards from
cmd.exe .

Switched SMX to use Igor Pavlovs public domain LZMA
implementation.

Fixed installer to not leave zombie installer process when SMX
driver is unable to load.

-- Build 154 - 2009.10.14

Bumped build number to avoid user confusion caused by leading
zero in previous builds.

Fixed Explorer folder window hotkey issue affecting some users.

Various minor fixes for improved compatiblity with some
applications and with checked(debug) Windows kernels.

-- Build 053 - 2009.08.18

Improved ISO reader compatibility with UDF 2.5 images (Bluray).

Improved driver compatibility with Microsoft DFS, used in
some enterprise networks.

-- Build 051 - 2009.06.04

Work-around for compatibility issue with Dr. Web anti-virus.

Added LZMA and Bzip2 support to zip reader.

Improved UTF8 filename support in zip reader.

Improved compatibility with some installers (SQL 2005).

-- Build 050 - 2009.03.05

Protocol, marshaller, and driver changes to eliminate extra
copying of file data.

Data caching performance improvements.

Support for unbuffered I/O from client applications, for
improved performance with some applications.

-- Build 048 - 2008.11.03

Added SMX creation utility with GUI, ptsmxcreate.

Added ISO/CISO/DAA/CFS conversion utility with GUI, ptisoconvert.

Added CFS creation utility with GUI, ptcfscreate.

-- Build 047 - 2008.08.29

Added support for recent DAA image format extensions.

Changed driver load logic, to allow driver to load on systems
where other drivers have exhausted certain limited system
resources.

Fixed problem preventing some files from being accessible in
large zip files.

Improved compatibility with a few 3rd party file system
security products.

-- Build 046 - 2008.08.08

Added support for ISZ, DAA, and UIF image formats.

Improved mounted drive letter functionality.

Improved handling of require-admin applications on Vista with UAC
enabled.

Fixed unicode case-folding issue causing problems with some european
language characters in file names.

Expose .\.$media\image.iso file in mounted CD/DVD images. Allows
on the fly conversion of image file formats for access by burning
programs.

Support reading pure UDF CD/DVD images.

Various changes to driver loading and error reporting in installer
and installer stub, to address some configuration specific install
issues.

-- Build 045 - 2008.06.20

Updated ptiso tool and self mounting executable stubs to include
support for LZMA compressed Compact ISO images.

-- Build 044 - 2008.05.14

Fixed issue in ptiso tool that would sometimes result in creation
of corrupt images when using the create command. Issue did not
affect the convert command.

-- Build 043 - 2008.05.01

Added support for CISO and CFS to ISO reader.

Added ptiso tool, and related executable stubs.

Fixed some minor file system compatibility issues.

Changed installer to use Pismo SMX technology. Result is elimination
of Winzip dialog and a significantly smaller download package.

-- Build 042 - 2008.03.23

Fixed issue preventing some executables from running inside mounted
files on Vista with UAC enabled.

-- Build 041 - 2008.02.25

Fixed issue preventing certain executables from running inside
mounted files.

-- Build 040 - 2008.02.20

Drive letter support for mounted files, to improve compatibility
with a few installer programs.

Added support for volume labels to the the ISO reader.

Add support for creating new Private Folders from explorer
File/New menu.

Fixed handling of file create times for UDF format ISO images.

Fixed support for 32 bit formatters on x64 versions of Windows.

Resolved deadlock with Trend anti-virus.

Various driver changes to improve compatibility with 3rd party
file system products.

Fixed pfmhost.exe holding up logoff in some configurations.

-- Build 038 - 2007.12.31

First public release.
