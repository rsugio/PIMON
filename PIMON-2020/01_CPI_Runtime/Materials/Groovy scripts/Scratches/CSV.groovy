import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter

import java.time.Month
import java.time.format.TextStyle

def months = [Month.values()*.value, Month.values()*.getDisplayName(TextStyle.FULL, Locale.ENGLISH)].transpose()

StringBuilder builder = new StringBuilder()
CSVPrinter csv = new CSVPrinter(builder, CSVFormat.DEFAULT)
csv.printRecords(months)

//#region CSVPrinter close() with auto-flush
csv.close(true)
//#endregion

//#region CSVPrinter with explicit flush() and close()
//csv.flush()
//csv.close()
//#endregion

println(builder.toString())