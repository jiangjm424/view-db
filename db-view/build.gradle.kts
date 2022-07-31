import db.setupLibraryModule

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

setupLibraryModule(publish = true, document = false)

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.paging)

    implementation(libs.androidx.sqlite.ktx)
    implementation(libs.gson)
    implementation(libs.sqlcipher)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso)
}
