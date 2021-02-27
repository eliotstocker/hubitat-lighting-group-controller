/**
 *  Lighting Group Controller
 *
 *  Copyright 2021 Eliot Stocker
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
        name: "Lighting Group Manager",
        namespace: "piratemedia",
        author: "Eliot Stocker",
        description: "Application to enable control of a group of lights based on physical/virtual Buttons",
        category: "Convenience",
        iconUrl: "https://raw.githubusercontent.com/eliotstocker/SmartThings-LightPhysicalControl/master/logo-small.png",
        iconX2Url: "https://raw.githubusercontent.com/eliotstocker/SmartThings-LightPhysicalControl/master/logo.png",
        singleInstance: true
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    migrate()

    return dynamicPage(name: "mainPage", title: "Lighting Group Controller", install: true, uninstall: true) {
        section {
            app(name: "lightingGroupSetup", appName: "Lighting Group Controller", namespace: "piratemedia", title: "New Lighting Group Setup", multiple: true)
        }
    }
}

def installed() {
    initialize()
}

def updated() {
    log.debug "App: ${app.name}"
}

def migrate() {
    if(app.name != "Lighting Group Manager") {
        app.updateName("Lighting Group Manager")
        if(app.label == "Light Physical Control") {
            app.updateLabel("Lighting Group Manager")
        }
    }
}