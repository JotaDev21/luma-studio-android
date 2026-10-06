plugins { kotlin("jvm"); kotlin("plugin.serialization") }
kotlin { jvmToolchain(17) }
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    testImplementation(kotlin("test"))
}
tasks.test {
    inputs.files(listOf("filters/presets.json","effects/presets.json","templates/stories.json","styles/presets.json").map {rootProject.file("app/src/main/assets/$it")})
        .withPropertyName("bundledCatalogs")
    systemProperty("luma.presets.file", rootProject.file("app/src/main/assets/filters/presets.json").absolutePath)
    systemProperty("luma.effects.file", rootProject.file("app/src/main/assets/effects/presets.json").absolutePath)
    systemProperty("luma.templates.file", rootProject.file("app/src/main/assets/templates/stories.json").absolutePath)
    systemProperty("luma.styles.file",rootProject.file("app/src/main/assets/styles/presets.json").absolutePath)
}
