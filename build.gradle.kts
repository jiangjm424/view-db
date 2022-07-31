import db.by
import db.groupId
import db.versionName
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost.DEFAULT
import com.vanniktech.maven.publish.SonatypeHost.S01
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jlleitschuh.gradle.ktlint.KtlintExtension

buildscript {
    repositories {
        google()
        mavenCentral()

    }
    dependencies {
        classpath(libs.gradlePlugin.android)
        classpath(libs.gradlePlugin.kotlin)
        classpath(libs.gradlePlugin.mavenPublish)
    }
}

// https://youtrack.jetbrains.com/issue/KTIJ-19369
@Suppress("DSL_SCOPE_VIOLATION", "UnstableApiUsage")
plugins {
    alias(libs.plugins.binaryCompatibility)
    alias(libs.plugins.dokka)
    alias(libs.plugins.ktlint)
}

tasks.withType<DokkaMultiModuleTask>().configureEach {
    outputDirectory by file("$rootDir/docs/api")
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            setUrl("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        }
    }

    group = project.groupId
    version = project.versionName

    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    plugins.withId("com.vanniktech.maven.publish.base") {
        group = project.groupId
        version = project.versionName

        extensions.configure<MavenPublishBaseExtension> {
            publishToMavenCentral(S01)
            signAllPublications()
            pomFromGradleProperties()
        }
    }

    // Uninstall test APKs after running instrumentation tests.
    tasks.whenTaskAdded {
        if (name == "connectedDebugAndroidTest") {
            finalizedBy("uninstallDebugAndroidTest")
        }
    }
}
