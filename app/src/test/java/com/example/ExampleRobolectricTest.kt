package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.IntentType
import com.example.domain.ai.OfflineCommandProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PETER", appName)
    }

    @Test
    fun `offline engine parses flashlight command`() = runBlocking {
        val offlineEngine = OfflineCommandProvider()
        val res = offlineEngine.analyzeCommand("turn on flashlight")
        assertEquals(IntentType.FLASHLIGHT, res.intent.type)
        assertEquals("ON", res.intent.action)
    }

    @Test
    fun `offline engine parses battery command`() = runBlocking {
        val offlineEngine = OfflineCommandProvider()
        val res = offlineEngine.analyzeCommand("check battery percentage")
        assertEquals(IntentType.BATTERY_STATUS, res.intent.type)
    }

    @Test
    fun `offline engine parses volume command`() = runBlocking {
        val offlineEngine = OfflineCommandProvider()
        val res = offlineEngine.analyzeCommand("set volume to 80%")
        assertEquals(IntentType.VOLUME_CONTROL, res.intent.type)
        assertEquals(80, res.intent.value)
    }
}
