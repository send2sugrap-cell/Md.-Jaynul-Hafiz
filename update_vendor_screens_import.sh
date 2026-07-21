awk '
/^import / && !imports_done {
    print "import kotlinx.coroutines.launch"
    imports_done = 1
}
{print}
' ./app/src/main/java/com/example/ui/VendorScreens.kt > tmp.kt && mv tmp.kt ./app/src/main/java/com/example/ui/VendorScreens.kt
