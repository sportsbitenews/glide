package glide.gradle

import com.google.appengine.AppEnginePlugin
import directree.DirTree
import glide.testing.IntgTestHelpers
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.TimeUnit

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class GlideAppRunIntgTests extends Specification {

    public static final File testProjectDir = new File("build", "test-run-project")

    @Shared def runResult

    def setup() {

    }

    def cleanup() { // teardown


    }

    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    def setupSpec() {  // before-class

        DirTree.create(testProjectDir.absolutePath) {
            dir "app", {
                file "index.groovy", "println 'home'"
                file "home.html", "<h1>hello world</h1>"
                file "_routes.groovy", "get '/', forward:'index.groovy'"
            }
            file 'glide.groovy', " app { }"
            file "build.gradle", """\
                   plugins {
                    id 'com.appspot.glide-gae'
                   }
                   repositories { mavenLocal() }
                   appengine {
                        daemon = true
                   }
                """.stripIndent()
        }

        runResult = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withTestKitDir(IntgTestHelpers.testKitGradleHome)
                .withPluginClasspath()
                .forwardOutput()
                .withArguments(AppEnginePlugin.APPENGINE_RUN, '--info' ,"--stacktrace")
                .build()

        println runResult.output


    }

    def cleanupSpec() {  // after-class
        def stopResult = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withTestKitDir(IntgTestHelpers.testKitGradleHome)
                .withPluginClasspath()
                .withArguments(AppEnginePlugin.APPENGINE_STOP, '--info' ,"--stacktrace")
                .build()
    }


    def "starts the development server"() {
        expect:
        runResult.task(":" + GlideGradlePlugin.GLIDE_START_SYNC_TASK_NAME).outcome == SUCCESS
        runResult.task(":" + AppEnginePlugin.APPENGINE_RUN).outcome == SUCCESS
        runResult.output.contains('Dev App Server is now running')
    }

    @Ignore('cant get the latest server logs')
    def "output contains logs"() {
        new URL("http://localhost:8080/").text

        expect:
        runResult.output.contains "uri=/"
    }


    def "serves groovy scripts"() {
        expect:
        new URL("http://localhost:8080/index.groovy").text.contains 'home'
    }

    def "routes work"() {
        def resp = new URL("http://localhost:8080/").text

        expect:
        resp.contains 'home'
    }

}

