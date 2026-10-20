# Wheelhouse ProGuard / R8 rules.
# Room, Media3 and coroutines ship their own consumer rules; keep this file small.

# Keep database entities readable in crash reports.
-keep class io.github.pronzzz.wheelhouse.data.db.** { *; }
