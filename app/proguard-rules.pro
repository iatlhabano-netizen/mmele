# Mmele R8 rules.
#
# The engine, online protocol and UI use no reflection. Firebase Auth/Database, DataStore,
# AboutLibraries and Compose ship their own consumer rules. Firebase's reflective POJO mapping
# (getValue(MyClass::class.java)) is NOT used: rooms are read as Map/String/Boolean only, see
# FirebaseOnlineGameRepository and Wire. If you ever add a data class read via getValue(Class),
# keep it here, e.g.  -keepclassmembers class bw.mmele.online.SomeDto { *; }

# Readable crash stack traces in Play Console (upload mapping.txt with each release).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# AboutLibraries reads its generated JSON from res/raw/aboutlibraries.json by name; keep it
# from resource shrinking (see res/raw/keep.xml).
