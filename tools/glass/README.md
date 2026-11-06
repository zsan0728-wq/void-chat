# Glass rendering acceptance

`tests/glasslab` is a deterministic, offline Android app using the same `RefractiveBackdrop`,
`RefractiveMessageBubble`, and motion tokens as the real timeline. It has 60 fixtures, including
incoming/outgoing text, long text, and photo messages. It is **not** a Matrix transport test.
The module declares no network permission. Build results and measurements must be recorded separately.

## Build and interactive verification

Use the repository's Java 21, Android SDK, and Gradle environment:

```powershell
.\gradlew.bat :tests:glasslab:assembleBenchmark :tests:glassbenchmark:assembleBenchmark --no-scan
```

The profileable target is not debuggable and uses release variants of dependencies. It is debug
signed for local installation; it is a benchmark fixture, not a shipping APK.

```powershell
adb install -r tests/glasslab/build/outputs/apk/benchmark/glasslab-benchmark.apk
adb shell am start -n io.element.android.glasslab/.GlassLabActivity --es theme dark --ez compact false --ez animated false --ef requested_refresh_rate 120
```

Omit `theme` to follow Android. `theme=light|dark` overrides the lab's Compose host appearance as well
as its theme, so Haze sees the same appearance. `requested_refresh_rate` asks the window for that
rate; it does **not** establish the actual presented rate. Defaults leave refresh selection to Android.

- **20 visible** uses 20 viewport slots with compact text to expose 20 independently refracting
  surfaces. This is an effect-count stress fixture, not a proposed message/font layout.
- **Moving wall** adds a moving stripe to the live photo source. Watch its refracted edge through
  different bubbles during scrolling, sending, and long press. It defaults off and stops when system
  animations are disabled. It is diagnostic source content, not a prerendered glass bitmap.
- **Send** appends the typed fixture; **Receive** appends an incoming fixture. Only new IDs animate.
- Long press opens working local **Copy**, **Reply**, and **Delete** actions. The separate production
  timeline retains Matrix actions and needs its own logged-in functional regression.

Inspect light and dark modes, both directions, long text, photo containers, rapid flings, changing
wallpaper, long press/release, keyboard opening/closing, and device rotation. Verify letters are never
refracted, rounded optical edges move with their bubble, and recycled rows do not replay entrance.
Repeat with Android's Remove animations preference enabled; no entrance/press scaling should run.
Capture a screen recording for visual correctness independently of the timing run.

## Physical-device Macrobenchmark

Keep the phone plugged in, awake, unlocked, cool, and free of unrelated foreground activity. Disable
screen recording and debugger during timing runs. Record battery/thermal state and any device power
mode; do not silently alter system refresh or performance settings. Run the same matrix at requested
60 and 120 Hz on a device supporting those modes, then inspect actual display cadence in each trace.
Also test a genuinely 60 Hz device before claiming 60 Hz device coverage.

```powershell
./tools/glass/Run-GlassBenchmarks.ps1 -Serial DEVICE_SERIAL -AdbPath adb -RefreshRate 60
./tools/glass/Run-GlassBenchmarks.ps1 -Serial DEVICE_SERIAL -AdbPath adb -RefreshRate 120
```

Each method is invoked separately and must produce eight measured iterations and eight Perfetto
traces. The runner preserves per-method logs, JSON, XML, traces, commit/dirty-tree status, display
state, thermal state, and `dumpsys meminfo` under `build/glass-perf/`. Failure preserves partial
evidence and stops the matrix. It changes no system display or power settings. It only requests a
window refresh rate and installs/runs the two test packages through Gradle.

Scenarios: photo scrolling in light/dark, 20 visible surfaces over a moving photo source, twelve
outgoing messages, twelve incoming messages, five keyboard round trips, and five menu open/close
cycles. Scroll traversal asserts that the oldest of the sixty fixtures was reached. Dense mode
asserts at least twenty glass surfaces in the UI hierarchy before measuring the flings.

For one scenario during development:

```powershell
./tools/glass/Run-GlassBenchmarks.ps1 -Serial DEVICE_SERIAL -AdbPath adb -RefreshRate 120 -Methods scrollPhotoDark
```

Do not suppress Macrobenchmark's debuggable/emulator/thermal warnings to obtain a passing result.
An automation smoke test or preview does not count as performance evidence.

## Interpret and retain evidence

Record device/model/SoC/API, APK SHA-256, commit and dirty diff, requested **and observed** refresh rate,
test scenario, iterations, thermal conditions, CPU frame duration P50/P90/P95/P99, frame overrun
P50/P90/P95/P99, and available peak GPU/RSS metrics. Positive frame overrun means a missed deadline.
The nominal budgets are 16.67 ms at 60 Hz and 8.33 ms at 120 Hz.

`FrameTimingMetric` CPU duration is not GPU shader time. Its inverse is not FPS. Obtain presented FPS
and missed presentation deadlines from Perfetto FrameTimeline in the measured active interaction
window, documenting the query and excluding intentional idle intervals. If the driver does not
expose GPU memory/timing counters, label those unavailable; do not substitute CPU or total PSS values.
Retain every iteration, including slow ones, and compare repeat passes in alternating order.

For lifecycle/memory regression, keep a single process alive for at least five minutes, repeat
scroll/send/keyboard/theme cycles, and take periodic GPU/process memory snapshots. Cold-started
Macrobenchmark iterations alone cannot prove absence of a persistent GPU memory leak. Inspect
Perfetto's RenderThread, UI thread, RenderNode/layer work, and GPU tracks before choosing an optimization.

The production default currently uses `HazeInputSharing.Never`, one wallpaper source and independent
glass modifiers. Haze 2.1's experimental `Auto` reduces source replay on Android with two consumers
but adds offscreen memory and clipping. A future A/B must keep the same wallpaper, optics, visible
count, refresh cadence, and device conditions; report both memory and frame overrun. Do not enable
sharing purely from upstream sample timings and do not remove any visible bubble's refraction.

Finally repeat send/receive, encrypted rooms, pagination, image/media handling, local-echo replacement,
copy/reply/delete, TalkBack, and navigation in Element X itself. Lab rendering measurements do not
establish these application-level behaviors.

## Result status

No FPS, GPU duration, or memory growth result is recorded by this harness's creation. Populate a
separate dated result document only after running it and retaining its raw artifacts. Build success,
shader correctness, Matrix behavior, and performance acceptance are distinct outcomes.
