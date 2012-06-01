
ISO CISO (Compact ISO) and CFS (Compact File Set) Tool build 162
Pismo Technic Inc., Copyright 2008-2011 Joe Lowe
2011.10.27
http://www.pismotechnic.com/


License
-------

Ptiso is distributed free of charge, in binary and source form. Refer
to the included license.txt and ptiso-license.txt files.


Installation on Windows
-----------------------

No installation or uninstallation is required or provided with ptiso.

Ptiso.exe is a command line utility and is designed for use from the
cmd.exe prompt. It is recommended that users place the ptiso.exe file
in the c:\windows folder so it can more easily be used.


Installation on OSX and Linux
-----------------------------

You must compile ptiso from source. The included make script should be
used. If the configure or make fails, you should insure you have the
following components installed on your system.

   GNU C++ compiler (g++)
   curses/ncurses development files

The following shell commands will compile and install ptiso.

   tar xvzf ptiso-*.tar.gz
   cd ptiso-*
   make release
   sudo make install
   ...
   sudo make uninstall

Please contact Pismo Technic Inc. support if you encounter problems
compiling ptiso.


Installation on other operating systems
---------------------------------------

It is possible to use ptiso on other unix-like operating systems by
compiling it from source. Other operating systems may require changes
to the ptiso source or make scripts. Users of other operating systems
are encouraged to contact Pismo Technic Inc. for support and to
provide feedback or submit source change recommendations.


Getting Started
---------------

Some ptiso help is available at the command line:

   ptiso
   ptiso -h
   ptiso create -h
   ptiso convert -h

Converting a standard ISO file to Compact ISO and back.

   ptiso convert someimage.iso someimage.ciso
   ptiso convert someimage.ciso someimage.iso

The conversion mechanism can read ISO data from standard input,
allowing direct creation of Compact ISO files from the output of
mkisofs and similar image creation utilities.

   mkisofs somefolder | ptiso convert - someimage.ciso

Compact File Set compliant images can be created directly from
files and folders in the file system. Compact File Sets are
written using a restricted ISO-9660 file system and are
compatible with many existing systems and applications that
support ISO image files.

   ptiso create someimage.cfs somefolder

When creating a Compact File Set, a script can be used to
facilitate direct control over the layout of the files in the
file set.

>>> start of script.txt
   file $windir/system.ini
   label {important files}
   folder drivers {
      glob $windir/system32/drivers m*.sys
      file $windir/system32/drivers/ntfs.sys
   }
   folder programs {
      folder docs {
         tree {c:/program files} -** **.doc **.txt **.rtf **.htm*
      }
   }
>>> end of script.txt

   ptiso create someimage.cfs script.txt

Small scripts can be specified as a quoted argument on the
command line.

   ptiso create -z zip memorydump.cfs "file c:/windows/memory.dmp"


Release History
---------------

-- Build 162 - 2011.10.26

Improved OSX and Linux support in source package.

-- Build 158 - 2010.06.08

Minor fixes.

-- Build 157 - 2010.05.18

Added binary package for Mac and Linux.

Source package build updates.

-- Build 154 - 2009.10.14

Bumped build number to avoid user confusion caused by leading
zero in previous builds.

Improved OSX and Linux support in source package.

-- Build 051 - 2009.06.04

OSX support in source package.

-- Build 050 - 2009.03.05

Minor fixes.

-- Build 048 - 2008.11.03

Added optional non GPL implementation of the LZMA compression
algorithm to source package.

Added ISO/CISO/DAA/CFS conversion utility with GUI, ptisoconvert.

Added CFS creation utility with GUI, ptcfscreate.

-- Build 047 - 2008.08.29

Fixed incorrect password header initialization for encrypted
CISO/CFS images.

-- Build 046 - 2008.08.08

Fixed unicode file name comparison issue affecting some European
language characters.

-- Build 045 - 2008.06.20

Added support for LZMA compessed Compact ISO files.

-- Build 044 - 2008.05.14

Fixed issue in ptiso tool that would sometimes result in creation
of corrupt images when using the create command. Issue did not
affect the convert command.

-- Build 043 - 2008.05.01

First release.
