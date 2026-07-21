package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Client::class, Order::class, User::class, InvoiceRecord::class, RegisteredCustomer::class, ManagementUser::class, SystemActivityLog::class, InventoryItem::class, PriceEstimate::class, PaperType::class, Vendor::class, VendorBill::class, VendorPayment::class, RecurringBill::class, VendorDocument::class], version = 19, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}
