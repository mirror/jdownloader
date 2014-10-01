The DJ project - NativeSwing
http://djproject.sourceforge.net
Christopher Deckers (chrriis@nextencia.net)
Licence terms: LGPL (see licence.txt)


1. What is the DJ Project - NativeSwing, SWT-based implementation?

The DJ Project is a set of tools and libraries to enhance the user experience of
Java on the Desktop.

The SWT-based NativeSwing library leverages the integration capabilities of the
NativeSwing framework to provide rich components and utilities.

The key components are a rich native web browser and a flash player.


2. How to use it?

Simply place the NativeSwing.jar and NativeSwing-SWT.jar libraries in your
classpath, as well as the SWT library corresponding to your platform (visit
http://www.eclipse.org/swt).
You may need to refer to the SWT FAQ (http://www.eclipse.org/swt/faq.php) to get
it working on certain platforms (like the need to install XULRunner on Linux).
Java 5.0 or later is required.

Then, you need to add the following to your main method:

public static void main(String[] args) {
  NativeInterface.open();
  // Here goes the rest of the program initialization
  NativeInterface.runEventPump();
}

On Mac, you may need to add the "-XstartOnFirstThread" VM parameter.

If you want to use the shaping mode for native components (cf the demo), you
need "jna.jar" and "jna_WindowUtils.jar" in your classpath.

If you want to use the HTML editor, you need the zip of the FCK editor, the
CKEditor editor or the Tiny MCE editor in your classpath depending on which
implementation is configured.

If you want to use the syntax highlighter, you need zip of the SyntaxHighlighter
in your classpath.


3. Any tutorial or demo?

The DJ NativeSwing Demo presents all the features of the NativeSwing library,
along with the corresponding code. Simply launch DJNativeSwing-SWTDemo.jar.

By default, the Windows version of SWT is provided and in the demo's classpath.
If you wish to try on a different platform, simply place the corresponding SWT
library, (re-)named swt.jar alongside DJNativeSwing-SWTDemo.jar.


4. What is the development status?

The library is tested on Windows, Linux and Mac but may work on other platforms
SWT supports.

For information about the current implementation status, visit the DJ Project's
website.


5. Sources?

The sources are part of the distribution.
There is of course some access to the CVS tree, from the Sourceforge website.

For the sources of the SWT libraries, check the eclipse repositories. 


6. Troubleshooting?

In case of a problem with the framework or some components, it is possible to
activate certain system properties in order to get more information.

The list of properties can be found in SystemProperties.txt (framework
properties) and SystemProperties-SWT.txt.


7. How to contribute?

If you are interested in helping the project, simply send me an e-mail. Friendly
e-mails are always welcome too!
