// SPDX-FileCopyrightText: 2015 - 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version embeddedKotlinVersion
    `java-gradle-plugin`
}

group = "org.mutantcat.lightbookinput.build_logic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    implementation(libs.kotlinx.serialization.json)
}

gradlePlugin {
    plugins {
        register("androidAppConvention") {
            id = "org.mutantcat.lightbookinput.app-convention"
            implementationClass = "AndroidAppConventionPlugin"
        }
        register("dataChecksums") {
            id = "org.mutantcat.lightbookinput.data-checksums"
            implementationClass = "DataChecksumsPlugin"
        }
        register("nativeAppConvention") {
            id = "org.mutantcat.lightbookinput.native-app-convention"
            implementationClass = "NativeAppConventionPlugin"
        }
        register("nativeCacheHash") {
            id = "org.mutantcat.lightbookinput.native-cache-hash"
            implementationClass = "NativeCacheHashPlugin"
        }
        register("openccData") {
            id = "org.mutantcat.lightbookinput.opencc-data"
            implementationClass = "OpenCCDataPlugin"
        }
    }
}
