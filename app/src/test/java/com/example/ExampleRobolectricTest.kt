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

    @Test
    fun `offline engine parses Hindi flashlight and battery command`() = runBlocking {
        val offlineEngine = OfflineCommandProvider()
        val resFlash = offlineEngine.analyzeCommand("फ्लैशलाइट ऑन करो")
        assertEquals(IntentType.FLASHLIGHT, resFlash.intent.type)
        assertEquals("ON", resFlash.intent.action)

        val resBattery = offlineEngine.analyzeCommand("बैटरी कितनी है?")
        assertEquals(IntentType.BATTERY_STATUS, resBattery.intent.type)
    }

    @Test
    fun `offline engine parses Bengali flashlight and battery command`() = runBlocking {
        val offlineEngine = OfflineCommandProvider()
        val resFlash = offlineEngine.analyzeCommand("টর্চ জ্বালাও")
        assertEquals(IntentType.FLASHLIGHT, resFlash.intent.type)
        assertEquals("ON", resFlash.intent.action)

        val resBattery = offlineEngine.analyzeCommand("ব্যাটারি কত?")
        assertEquals(IntentType.BATTERY_STATUS, resBattery.intent.type)
    }

    @Test
    fun `offline engine parses Google search command`() = runBlocking {
        val offlineEngine = OfflineCommandProvider()
        val res = offlineEngine.analyzeCommand("search on google for spider-man no way home")
        assertEquals(IntentType.AI_QUERY, res.intent.type)
        assertEquals("spider-man no way home", res.intent.query)
    }

    @Test
    fun `offline engine parses joke command in English, Hindi, and Bengali`() = runBlocking {
        val offlineEngine = OfflineCommandProvider()
        val resEn = offlineEngine.analyzeCommand("tell me a joke")
        assertEquals(IntentType.AI_QUERY, resEn.intent.type)
        assertNotNull(resEn.directAnswer)

        val resHi = offlineEngine.analyzeCommand("एक जोक सुनाओ")
        assertEquals(IntentType.AI_QUERY, resHi.intent.type)
        assertNotNull(resHi.directAnswer)

        val resBn = offlineEngine.analyzeCommand("একটি কৌতুক বলো")
        assertEquals(IntentType.AI_QUERY, resBn.intent.type)
        assertNotNull(resBn.directAnswer)
    }

    @Test
    fun `offline engine answers boss queries with configured details`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.data.local.PeterPreferences(context)
        prefs.updateBossProfile(
            name = "Bruce Wayne",
            title = "Supreme Commander",
            details = "Billionaire genius who built awesome gadgets."
        )

        val offlineEngine = OfflineCommandProvider(prefs)
        
        // English query
        val resEn = offlineEngine.analyzeCommand("who is your boss?")
        assertEquals(IntentType.AI_QUERY, resEn.intent.type)
        assertNotNull(resEn.directAnswer)
        org.junit.Assert.assertTrue(resEn.directAnswer!!.contains("Bruce Wayne"))
        org.junit.Assert.assertTrue(resEn.directAnswer!!.contains("Supreme Commander"))

        // Hindi query
        val resHi = offlineEngine.analyzeCommand("तुम्हारा बॉस कौन है?")
        assertEquals(IntentType.AI_QUERY, resHi.intent.type)
        assertNotNull(resHi.directAnswer)
        org.junit.Assert.assertTrue(resHi.directAnswer!!.contains("Bruce Wayne"))

        // Bengali query
        val resBn = offlineEngine.analyzeCommand("তোমার বস কে?")
        assertEquals(IntentType.AI_QUERY, resBn.intent.type)
        assertNotNull(resBn.directAnswer)
        org.junit.Assert.assertTrue(resBn.directAnswer!!.contains("Bruce Wayne"))
    }

    @Test
    fun `offline engine detects emergency code red commands in multiple languages`() = runBlocking {
        val offlineEngine = OfflineCommandProvider()

        val res1 = offlineEngine.analyzeCommand("Hey Peter code red")
        assertEquals(IntentType.EMERGENCY_LOCKDOWN, res1.intent.type)
        assertNotNull(res1.directAnswer)

        val res2 = offlineEngine.analyzeCommand("Hello Peter code red")
        assertEquals(IntentType.EMERGENCY_LOCKDOWN, res2.intent.type)

        val resHi = offlineEngine.analyzeCommand("कोड रेड")
        assertEquals(IntentType.EMERGENCY_LOCKDOWN, resHi.intent.type)

        val resBn = offlineEngine.analyzeCommand("কোড রেড")
        assertEquals(IntentType.EMERGENCY_LOCKDOWN, resBn.intent.type)
    }

    @Test
    fun `viewModel locks down on code red and deactivates only with Daddy is home`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.PeterViewModel(app)

        // Trigger lockdown
        vm.activateLockdown()
        org.junit.Assert.assertTrue(vm.settings.value.isLockdownActive)

        // Attempt wrong password
        val wrongAttempt = vm.deactivateLockdown("wrong_pass_123")
        org.junit.Assert.assertFalse(wrongAttempt)
        org.junit.Assert.assertTrue(vm.settings.value.isLockdownActive)

        // Attempt correct password "Daddy is home"
        val correctAttempt = vm.deactivateLockdown("Daddy is home")
        org.junit.Assert.assertTrue(correctAttempt)
        org.junit.Assert.assertFalse(vm.settings.value.isLockdownActive)
    }

    @Test
    fun `offline engine parses search as background search and proof as show proof`() = runBlocking {
        val offlineEngine = OfflineCommandProvider()

        // Background search
        val resSearch = offlineEngine.analyzeCommand("search quantum computing")
        assertEquals(IntentType.AI_QUERY, resSearch.intent.type)
        assertEquals("quantum computing", resSearch.intent.query)
        assertNotNull(resSearch.directAnswer)

        // Show proof English
        val resProofEn = offlineEngine.analyzeCommand("show proof")
        assertEquals(IntentType.SHOW_PROOF, resProofEn.intent.type)
        assertNotNull(resProofEn.directAnswer)

        // Show proof Hindi
        val resProofHi = offlineEngine.analyzeCommand("प्रमाण दिखाओ")
        assertEquals(IntentType.SHOW_PROOF, resProofHi.intent.type)

        // Show proof Bengali
        val resProofBn = offlineEngine.analyzeCommand("প্রমাণ দেখাও")
        assertEquals(IntentType.SHOW_PROOF, resProofBn.intent.type)

        // Screen Search English
        val resScreenEn = offlineEngine.analyzeCommand("share my screen with Peter")
        assertEquals(IntentType.SCREEN_SEARCH, resScreenEn.intent.type)
        assertNotNull(resScreenEn.directAnswer)

        // Screen Search Hindi
        val resScreenHi = offlineEngine.analyzeCommand("मेरी स्क्रीन देखो")
        assertEquals(IntentType.SCREEN_SEARCH, resScreenHi.intent.type)

        // Screen Search Bengali
        val resScreenBn = offlineEngine.analyzeCommand("আমার স্ক্রিন দেখো")
        assertEquals(IntentType.SCREEN_SEARCH, resScreenBn.intent.type)
    }
}
