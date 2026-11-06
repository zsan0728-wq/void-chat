/*
 * Copyright (c) 2026 Element Creations Ltd.
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 */

plugins {
    id("io.element.android-compose-application")
}

android {
    namespace = "io.element.android.glasslab"
    defaultConfig {
        applicationId = "io.element.android.glasslab"
        minSdk = 34
        targetSdk = Versions.TARGET_SDK
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        create("benchmark") {
            initWith(getByName("release"))
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }
}

dependencies {
    implementation(projects.libraries.designsystem)
}
