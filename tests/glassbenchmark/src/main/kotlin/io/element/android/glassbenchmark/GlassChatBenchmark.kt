/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.glassbenchmark

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Rendering evidence for the production glass component; not Matrix synchronization coverage. */
@RunWith(AndroidJUnit4::class)
class GlassChatBenchmark {
    @get:Rule
    val benchmark = MacrobenchmarkRule()

    @Before
    fun requirePhysicalDevice() {
        check(Build.VERSION.SDK_INT >= 34) { "Glass acceptance requires Android 14 or later." }
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        check(device.executeShellCommand("getprop ro.kernel.qemu").trim() != "1") {
            "Emulator results cannot establish physical-device glass performance."
        }
        check(device.isScreenOn) { "Keep the physical device awake and unlocked throughout the run." }
    }

    @Test
    fun scrollPhotoLight() = measure(theme = "light") { scrollMessages() }

    @Test
    fun scrollPhotoDark() = measure(theme = "dark") { scrollMessages() }

    @Test
    fun twentyVisibleMovingWallpaper() = measure(compact = true, animated = true) {
        check(device.findObjects(By.res("glass_bubble")).size >= 20) { "The viewport must contain 20 independent bubbles." }
        scrollMessages()
    }

    @Test
    fun outgoingBurst() = measure {
        val composer = device.required("composer")
        composer.click()
        repeat(12) { index ->
            composer.text = "Outgoing fixture $index"
            device.required("send").click()
            SystemClock.sleep(90)
        }
        SystemClock.sleep(600)
    }

    @Test
    fun incomingBurst() = measure {
        repeat(12) {
            device.required("receive").click()
            SystemClock.sleep(90)
        }
        SystemClock.sleep(600)
    }

    @Test
    fun keyboardRoundTrips() = measure {
        repeat(5) {
            device.required("composer").click()
            SystemClock.sleep(600)
            device.pressBack()
            SystemClock.sleep(600)
        }
    }

    @Test
    fun longPressAndDismiss() = measure {
        repeat(5) {
            device.required("glass_bubble").longClick()
            check(device.wait(Until.hasObject(By.text("Copy")), 3000)) { "The real contextual menu did not open." }
            SystemClock.sleep(350)
            device.pressBack()
            SystemClock.sleep(350)
        }
    }

    @OptIn(ExperimentalMetricApi::class)
    private fun measure(
        theme: String = "light",
        compact: Boolean = false,
        animated: Boolean = false,
        actions: MacrobenchmarkScope.() -> Unit,
    ) {
        val configurator = Configurator.getInstance()
        val originalIdleTimeout = configurator.waitForIdleTimeout
        // A continuously animating source never becomes idle; don't add 10s to every lookup.
        if (animated) configurator.waitForIdleTimeout = 0
        try {
            val requestedRate = InstrumentationRegistry.getArguments().getString("refreshRate")?.toFloat() ?: 0f
            benchmark.measureRepeated(
                packageName = TARGET_PACKAGE,
                metrics = listOf(
                    FrameTimingMetric(),
                    MemoryUsageMetric(
                        mode = MemoryUsageMetric.Mode.Max,
                        subMetrics = listOf(MemoryUsageMetric.SubMetric.Gpu, MemoryUsageMetric.SubMetric.RssAnon),
                    ),
                ),
                iterations = 8,
                compilationMode = CompilationMode.Full(),
                startupMode = StartupMode.COLD,
                setupBlock = {
                    pressHome()
                    startActivityAndWait(
                        Intent(Intent.ACTION_MAIN).apply {
                            component = ComponentName(TARGET_PACKAGE, "$TARGET_PACKAGE.GlassLabActivity")
                            putExtra("theme", theme)
                            putExtra("compact", compact)
                            putExtra("animated", animated)
                            putExtra("requested_refresh_rate", requestedRate)
                        }
                    )
                    check(device.wait(Until.hasObject(By.res("messages")), 5000)) { "Glass Lab failed to launch." }
                    SystemClock.sleep(800)
                },
                measureBlock = actions,
            )
        } finally {
            configurator.waitForIdleTimeout = originalIdleTimeout
        }
    }

    private fun MacrobenchmarkScope.scrollMessages() {
        val list = device.required("messages")
        list.setGestureMargin(device.displayWidth / 8)
        // reverseLayout puts the newest message at the bottom: first move up to older messages.
        traverseToMessage(list, Direction.UP, id = 0)
        traverseToMessage(list, Direction.DOWN, id = 59)
    }

    private fun MacrobenchmarkScope.traverseToMessage(list: UiObject2, direction: Direction, id: Int) {
        val target = By.res("message_$id")
        for (attempt in 0 until 12) {
            if (device.hasObject(target)) return
            val canContinue = list.fling(direction)
            if (!canContinue) break
        }
        // A boundary scroll event can precede its final semantics update by a frame.
        check(device.wait(Until.hasObject(target), 1000)) {
            "The measured traversal did not reach message $id of the 60-message fixture."
        }
    }

    private fun UiDevice.required(tag: String): UiObject2 =
        checkNotNull(wait(Until.findObject(By.res(tag)), 5000)) { "Missing fixture control: $tag" }

    private companion object {
        const val TARGET_PACKAGE = "io.element.android.glasslab"
    }
}
