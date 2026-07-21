package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.Client
import com.example.data.SettingsManager
import com.example.ui.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: AppRepository
    private lateinit var settingsManager: SettingsManager
    private lateinit var viewModel: MainViewModel

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AppRepository(db.appDao())
        settingsManager = SettingsManager(context)
        viewModel = MainViewModel(repository, settingsManager)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertClientDirectly() = runBlocking {
        val client = Client(
            id = 0,
            name = "Test Client",
            company = "Test Company",
            position = "Manager",
            mobile = "01712345678",
            whatsapp = "01712345678",
            email = "test@example.com",
            address = "Test Address",
            specifications = "Test Specifications"
        )
        db.appDao().insertClient(client)
        val clients = db.appDao().getAllClientsList()
        assertEquals(1, clients.size)
        assertEquals("Test Client", clients[0].name)
    }

    @Test
    fun testAddClientViaViewModel() = runBlocking {
        val client = Client(
            id = 0,
            name = "ViewModel Client",
            company = "Test Company",
            position = "Developer",
            mobile = "01812345678",
            whatsapp = "01812345678",
            email = "vm@example.com",
            address = "VM Address",
            specifications = "VM Specifications"
        )
        viewModel.addClient(client)
        
        // Wait for coroutine to complete and fetch clients
        val dbClients = repository.getAllClients()
        assertEquals(1, dbClients.size)
        assertEquals("ViewModel Client", dbClients[0].name)
    }
}
