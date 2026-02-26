############################################
# 🔥 MINIMAL SAFE RULES (MOSHI CODEGEN)
############################################

# Keep annotations metadata (required by Room + Moshi codegen)
-keepattributes *Annotation*

############################################
# 🔥 ENUM SAFETY (CefrLevel)
############################################

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

############################################
# 🔥 FIREBASE CRASHLYTICS FIX
############################################

-dontwarn com.google.firebase.crashlytics.buildtools.reloc.afu.org.checkerframework.checker.formatter.qual.ConversionCategory
-dontwarn com.google.firebase.crashlytics.buildtools.reloc.org.checkerframework.checker.formatter.qual.ConversionCategory

############################################
# 🔥 OPTIONAL (Only if warnings appear)
############################################

-dontwarn org.checkerframework.**