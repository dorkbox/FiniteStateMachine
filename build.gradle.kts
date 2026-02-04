/*
 * Copyright 2026 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS   // always show the stacktrace!
gradle.startParameter.warningMode = WarningMode.All

plugins {
    id("com.dorkbox.GradleUtils") version "4.8"
    id("com.dorkbox.Licensing") version "3.1"
    id("com.dorkbox.VersionUpdate") version "3.2"
    id("com.dorkbox.GradlePublish") version "2.2"

    kotlin("jvm") version "2.3.0"
}


GradleUtils.load {
    group = "com.dorkbox"
    id = "FSM" // this is the maven ID!

    description = "Finite State Machine using the AhoCorasick implementation using a Double Array Trie"
    name = "FSM"
    version = "1.3"

    vendor = "Dorkbox LLC"
    vendorUrl = "https://dorkbox.com"

    url = "https://git.dorkbox.com/dorkbox/FSM"

    issueManagement {
        url = "${url}/issues"
        nickname = "Gitea Issues"
    }

    developer {
        id = "dorkbox"
        name = vendor
        email = "email@dorkbox.com"
    }
}
GradleUtils.defaults()
GradleUtils.compileConfiguration(JavaVersion.VERSION_25)


licensing {
    license(License.APACHE_2) {
        description(Extras.description)
        author(Extras.vendor)
        url(Extras.url)

        extra("AhoCorasickDoubleArrayTrie", License.APACHE_2) {
            description("An extremely fast implementation of Aho Corasick algorithm based on Double Array Trie structure.")
            copyright(2018)
            author("hankcs <me@hankcs.com>")
            url("https://github.com/hankcs/AhoCorasickDoubleArrayTrie")
        }
    }
}

tasks.jar.get().apply {
    manifest {
        // https://docs.oracle.com/javase/tutorial/deployment/jar/packageman.html
        attributes["Name"] = Extras.name

        attributes["Specification-Title"] = Extras.name
        attributes["Specification-Version"] = Extras.version
        attributes["Specification-Vendor"] = Extras.vendor

        attributes["Implementation-Title"] = "${Extras.group}.${Extras.id}"
        attributes["Implementation-Version"] = GradleUtils.now()
        attributes["Implementation-Vendor"] = Extras.vendor
    }
}


dependencies {
    api("com.dorkbox:Updates:1.3")

    testImplementation("junit:junit:4.13.2")
}
