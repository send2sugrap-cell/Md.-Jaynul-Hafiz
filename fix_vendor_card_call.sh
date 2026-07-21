sed -i -e '/val vendorDue = vendorTotalPurchases - vendorTotalPaid/a\
                                            val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)\
                                            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)\
                                            val monthlyExpenditure = vendorBills.filter { bill ->\
                                                if (bill.vendorId == vendor.id) {\
                                                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = bill.timestamp }\
                                                    cal.get(java.util.Calendar.MONTH) == currentMonth && cal.get(java.util.Calendar.YEAR) == currentYear\
                                                } else false\
                                            }.sumOf { it.totalAmount }' \
-e 's/onClick = { onNavigateToLedger(vendor.id) }/monthlyExpenditure = monthlyExpenditure, onClick = { onNavigateToLedger(vendor.id) }/g' \
./app/src/main/java/com/example/ui/VendorScreens.kt
