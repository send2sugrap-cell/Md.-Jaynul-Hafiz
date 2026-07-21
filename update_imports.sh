awk '
/^import / && !imports_done {
    print "import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis"
    print "import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis"
    print "import com.patrykandpatrick.vico.compose.chart.Chart"
    print "import com.patrykandpatrick.vico.compose.chart.column.columnChart"
    print "import com.patrykandpatrick.vico.compose.component.lineComponent"
    print "import com.patrykandpatrick.vico.core.entry.FloatEntry"
    print "import com.patrykandpatrick.vico.core.entry.entryModelOf"
    imports_done = 1
}
{print}
' ./app/src/main/java/com/example/ui/VendorScreens.kt > tmp.kt && mv tmp.kt ./app/src/main/java/com/example/ui/VendorScreens.kt
