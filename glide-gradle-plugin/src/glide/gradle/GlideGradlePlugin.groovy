package glide.gradle

import com.google.appengine.AppEnginePlugin
import com.google.appengine.task.ExplodeAppTask
import com.google.appengine.task.RunTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.gaelyk.tasks.GaelykSynchronizeResourcesTask

class GlideGradlePlugin implements Plugin<Project> {

    public static final String GLIDE_MAVEN_REPO = 'http://dl.bintray.com/kdabir/glide'
    def javaVersion = 1.7

    void apply(Project project) {
        project.apply(plugin: 'war')
        project.apply(plugin: 'groovy')
        project.apply(plugin: 'org.gaelyk')

        project.repositories {
            jcenter()
            maven { url GLIDE_MAVEN_REPO }
            mavenCentral()
            mavenLocal()
        }

        project.sourceCompatibility = javaVersion
        project.targetCompatibility = javaVersion

        project.extensions.create('glide', GlideExtension, project, getVersions())

        project.afterEvaluate {
            // We need after evaluate to let user configure the glide {} block in buildscript and
            // then we add the dependencies to the project

            GlideExtension glideExtension = project.glide
            Versions versions = glideExtension.versions

            project.dependencies {
                compile "com.google.appengine:appengine-api-1.0-sdk:${versions.appengineVersion}"
                compile "com.google.appengine:appengine-api-labs:${versions.appengineVersion}"
                compile "org.codehaus.groovy:groovy-all:${versions.groovyVersion}"
                compile "org.gaelyk:gaelyk:${versions.gaelykVersion}"
                compile "io.github.kdabir.glide:glide-filters:${versions.glideFiltersVersion}"

                if (glideExtension.useSitemesh) {
                    compile "org.sitemesh:sitemesh:${versions.sitemeshVersion}"
                }

                appengineSdk "com.google.appengine:appengine-java-sdk:${versions.appengineVersion}"
            }
        }

        project.webAppDirName = "app"

        project.sourceSets {
            main.groovy.srcDirs = ['src']
            main.java.srcDirs = ['src']

            test.groovy.srcDirs = ['test']
            test.java.srcDirs = ['test']

            functionalTests.groovy.srcDir 'functionalTests'
        }

        project.tasks.withType(GaelykSynchronizeResourcesTask){
            enabled = false
        }

        project.task("glideVersion") << { println("${versions.selfVersion}") }

        def glideSyncTask = project.tasks.create("glideSync", glide.gradle.GlideSyncTask)

        ExplodeAppTask explode = project.tasks.findByName(AppEnginePlugin.APPENGINE_EXPLODE_WAR)
        RunTask runTask = project.tasks.findByName(AppEnginePlugin.APPENGINE_RUN)

        glideSyncTask.dependsOn explode
        runTask.dependsOn glideSyncTask
    }

    private Properties getVersions() {
        final Properties versions = new Properties();
        versions.load(this.getClass().getResourceAsStream("/versions.properties"));
        return versions
    }
}

