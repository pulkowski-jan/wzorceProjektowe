plugins {
	java
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "pl.edu.uj.student"
version = "1.0"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-jdbc")
	implementation("org.xerial:sqlite-jdbc:3.50.3.0")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.register<Exec>("buildDocs") {
	group = "documentation"
	description = "Kompiluje dokumentację z plików LaTeX (.tex) do formatu PDF przy użyciu latexmk"

	val outputDir = layout.buildDirectory.dir("docs").get().asFile

	doFirst {
		outputDir.mkdirs()
	}

	workingDir = file("docs")

	commandLine(
		"latexmk",
		"-pdf",                                        // Generuj plik PDF
		"-interaction=nonstopmode",                    // Nie zatrzymuj procesu w przypadku ostrzeżeń/drobnych błędów
		"-output-directory=${outputDir.absolutePath}", // Zapisz wynik w build/docs/
		"main.tex"                                     // Główny plik wejściowy
	)


}