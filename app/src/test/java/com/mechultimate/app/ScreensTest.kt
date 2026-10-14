package com.mechultimate.app

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mechultimate.app.data.Settings
import com.mechultimate.app.ui.AppViewModel
import com.mechultimate.app.ui.CalculatorScreen
import com.mechultimate.app.ui.ConverterScreen
import com.mechultimate.app.ui.HomeScreen
import com.mechultimate.app.ui.MechTheme
import com.mechultimate.core.units.UnitSystem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the main screens on the JVM (Robolectric native graphics), checks key content and
 * writes screenshots to build/screenshots for visual review.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h1600dp-xhdpi")
class ScreensTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val settings = Settings(unitSystem = UnitSystem.SI, disclaimerAccepted = true, recents = listOf("PIP-001", "VES-001"))

    private fun shot(name: String) {
        val dir = File("build/screenshots").apply { mkdirs() }
        rule.waitForIdle()
        val view = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bmp))
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun home() {
        rule.setContent { MechTheme { HomeScreen(settings, {}, {}, {}, {}) } }
        rule.onNodeWithText("Pressure Vessels").assertIsDisplayed()
        shot("home")
    }

    @Test fun b313Calculator() {
        rule.setContent { MechTheme { CalculatorScreen("PIP-001", null, viewModel<AppViewModel>(), settings) {} } }
        rule.onNodeWithText("Selected schedule").assertExists()
        // Change design pressure to 100 bar(g): a heavier schedule must be selected.
        rule.onNode(hasSetTextAction() and hasText("Internal design pressure")).performTextReplacement("100")
        rule.onNodeWithText("Sch 80 / XS", substring = true).assertExists()
        rule.onNodeWithText("calculation steps", substring = true).performClick()
        shot("calc_b313")
    }

    @Test fun vesselCalculatorUsUnits() {
        val us = settings.copy(unitSystem = UnitSystem.US)
        rule.setContent { MechTheme { CalculatorScreen("VES-001", null, viewModel<AppViewModel>(), us) {} } }
        rule.onAllNodesWithText("psig", substring = true).onFirst().assertExists()
        shot("calc_ves001_us")
    }

    @Test fun psychrometrics() {
        rule.setContent { MechTheme { CalculatorScreen("HVC-001", null, viewModel<AppViewModel>(), settings) {} } }
        rule.onNodeWithText("Humidity ratio W").assertExists()
        shot("calc_psychro")
    }

    @Test fun converter() {
        rule.setContent { MechTheme { ConverterScreen(UnitSystem.SI, 4) } }
        rule.onNodeWithText("Unit converter").assertIsDisplayed()
        shot("converter")
    }
}
