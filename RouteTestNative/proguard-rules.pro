# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Develop\Tools\android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep all public methods and fields for these classes

-dontnote com.genexus.xml.GXXMLSerializable
-dontnote com.android.vending.licensing.ILicensingService

# Classes for offline apps with encrypt database. keep sqlcipher
-keep class net.sqlcipher.** {
    *;
}

-keep class net.sqlcipher.database.** {
    *;
}
