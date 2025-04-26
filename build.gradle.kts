plugins {
    kotlin("jvm") version "1.9.23"
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("edu.sc.seis.launch4j") version "2.5.4"
}

group = "ru.phantom2097"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17)) // Явно указываем Java 17
    }
}

application {
    mainClass.set("ru.phantom2097.MainKt")
}

launch4j {
    mainClassName = application.mainClass.get()
//    icon = "${projectDir}/src/main/resources/icon.ico" // Конвертируйте PNG в ICO
    outfile = "PaymentApp.exe"
    windowTitle = "Платежное приложение"
    productName = "PaymentApp"
}

javafx {
    version = "21"  // Обновлено до LTS версии (или можно оставить 17)
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.graphics")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("no.tornado:tornadofx:1.7.20")  // Обновленная версия TornadoFX
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.23")  // Добавлено для работы TornadoFX
    implementation("com.squareup.retrofit2:retrofit:2.9.0") // Сетевые запросы
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // JSON конвертер
    implementation("com.google.code.gson:gson:2.10.1") // JSON обработка
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.7.3")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17" // Устанавливаем JVM target для Kotlin
    }
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
        attributes["Implementation-Version"] = archiveVersion.get()
    }
    archiveFileName.set("PaymentApp.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Включаем все зависимости
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

    // Указываем main класс для запуска
    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
    manifest.attributes["Main-Class"] = "ru.phantom2097.MainKt"
}

tasks.register<Copy>("copyRuntimeLibs") {
    into("build/libs/libs")
    from(configurations.runtimeClasspath)
}

tasks.register<CreateStartScripts>("createExeScripts") {
    outputDir = file("build/launcher")
    mainClass.set(application.mainClass.get())
    applicationName = "PaymentApp"
    classpath = files("PaymentApp.jar") + files(tasks.named("copyRuntimeLibs").get().outputs.files)
}