The DJ project - NativeSwing
http://djproject.sourceforge.net
Christopher Deckers (chrriis@nextencia.net)
Licence terms: LGPL (see licence.txt)


1. What is the DJ Project - NativeSwing?

The DJ Project is a set of tools and libraries to enhance the user experience of
Java on the Desktop.

The NativeSwing library allows an easy integration of some native components
into Swing applications, and provides some native utilities to enhance Swing's
APIs.


2. How to use it?

Simply place the NativeSwing.jar library in your classpath.
Java 5.0 or later is required.

Then, you need to add the following to your main method:

public static void main(String[] args) {
  NativeSwing.initialize();
  // Here goes the rest of the program initialization
}

Then, when you need to add the native component, simply use the wrapper:

NativeComponentWrapper ncw = new NativeComponentWrapper(nativeComponent);
container.add(ncw.createEmbeddableComponent(<options>)); 

If you want to use the shaping mode for native components (cf the demo), you
need "jna.jar" and "jna_WindowUtils.jar" in your classpath.

3. Any tutorial or demo?

The DJ NativeSwing SWT-based implementation along with its Demo are good
examples on how to use the NativeSwing framework.


4. What is the development status?

The library is tested on Windows and Linux, and it may be possible that it also
works on other platforms.

The library solves those common integration issues:
- Lightweight and heavyweight components produce visual glitches, like Swing
  popup menus, tooltips and combo drop menu to appear behind the native
  components.
- Hidden heavyweight components added to the user interface steal the focus, or
  mess it up.
- Swing modality works for Swing components, but the embedded native component
  are not blocked.

For information about the current implementation status, visit the DJ Project's
website.


5. Sources?

The sources are part of the distribution.
There is of course some access to the CVS tree, from the Sourceforge website.


6. Troubleshooting?

In case of a problem with the framework or some components, it is possible to
activate certain system properties in order to get more information.

The list of properties can be found in SystemProperties.txt.


7. How to contribute?

If you are interested in helping the project, simply send me an e-mail. Friendly
e-mails are always welcome too!
