ANS Client Service for Android
==============================

This package contains the ANS client service for Android. This service acts as
intermediary between the ANS server and multiple applications on an Android
device. This README is organized in three sections. The first describes how to
build ANS, both from the command-line and using Eclipse, the second describes
how to configure the ANS client service to prepare it to talk with ANS servers, 
and the third describes how an application interacts with the ANS client service.


Building the ANS Client Service for Android
-------------------------------------------

There are two ways to build the ANS client service -- using Eclipse, and from
the command line. In either case, the latest Android SDK must be installed
and configured per the instructions on the Android SDK Web page. If Eclipse
is being used, the Android Eclipse plugin must also be installed per the 
instructions on the Android tools Web page.

Eclipse

This ANS directory is configured as an Eclipse project with all of the
associated files. To import this project directory into Eclipse, simply 
select the File->Import... menu option and expand the 'General' topic and
select 'Existing Project Into Workspace'. Follow the import wizard to complete
the import.

Command Line

Command line builds are supported using the standard Android ant build process
described on the Android project management and build Web page. All of the
standard build targets are supported, in addition to a 'compile' target, which
compiles the Java without building an APK file. In addition to that, the
'ans-constants-jar' target is provided for building the library JAR file that
applications can use to provide constants that are helpful when interacting
with the ANS client service via its Intent interface. The file that results
from building the 'ans-constants-jar' file is ANSConstants.jar and is built in
the ANS directory. Whenever an ANS APK file is built, the ANSConstants.jar
file is rebuilt, and whenever an 'ant clean' command is issued, the 
ANSConstants.jar file is deleted.

To build from the command line, you must modify the local.properties file
to point to the location of your Android SDK. This is accomplished using the
'android' tool from the Android SDK as follows:

android update project --name ANS --target 7 --path .

Issuing this command from the ANS directory will update your environment to 
build from the command-line using your SDK install.

###Ant Commands and Signing:

	:::term
	$ ant clean
	$ ant debug // If you want the debug version

	$ant release

For release version of rhoconnect-push-service we need to sign the apk using jarsigner.

	:::term
	jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore rhoconnect_push_key.keystore -signedjar rhoconnect-push-service-unaligned.apk -storepass rhopush@Zebra -keypass rhopushkey@Zebra ANS-release-unsigned.apk alias_name

To generate a keystore file use below commands

	:::term
	keytool -genkey -v -keystore my-release-key.keystore -alias alias_name -keyalg RSA -keysize 2048 -validity 10000

For more information about Keytool, see the documentation at: [http://docs.oracle.com/javase/6/docs/technotes/tools/windows/keytool.html](http://docs.oracle.com/javase/6/docs/technotes/tools/windows/keytool.html)

To find alias name of your key store use below commands.

	:::term
	keytool -keystore rhoconnect_push_key.keystore -list -v
	
ANS Client Service Configuration
--------------------------------

The ANS client service for Android requires parameters to be set to operate
properly, and this is handled via settings in the AndroidManifest.xml file for
ANS. The following blocks of ANS functionality are configurable:

**WiFi Lock**

This 'wifi_lock' parameter is optional, and if enabled keeps WiFi on when the
device is sleeping/idle. Allowable values are "true" and "false".

**HTTPS Security**

Whether or not to use secure HTTPS for ANS messages is specified in the URL
sent to ANS from the app. If HTTPS security is "on" (https:// prefix), two additional 
settings are possible. These settings are 'secure_ignore_cert_checks' and 
'secure_ignore_hostname'. These settings instruct the client service to ignore
certificate checking or to ignore the hostname portion of the certificate 
check respectively. These options reduce security, but make the use of 
self-signed security certificates simpler. If either of these ignore parameters
is set to true, this setting will be applied to ALL server connections.

**Notification Message Customization**

Notification messages fall into three classes -- error, warning, and info (debug).
Each class of message may have a custom suffix specified, which gets displayed
in the dialog box that is displayed when the notification is selected. This
allows deployment-customizable information to be added to notifications, such
as the contact information for device/system administrators. Finally, since
the vibration is a bit annoying, there is a way to disable it for each of the
different notification types ("false" = disabled, "true" = enabled).

There are also 'warning_disabled' and 'info_disabled' settings, which override the
display of warning and info notifications respectively if set to true.
