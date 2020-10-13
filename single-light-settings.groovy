/**
 *  Light Physical Single Light Setting
 *
 *  Copyright 2018 Eliot Stocker
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
        name: "Physical Light Control Single Light Settings",
        namespace: "piratemedia",
        author: "Eliot Stocker",
        description: "child app to set a single lights settings",
        category: "Convenience",
        iconUrl: "https://raw.githubusercontent.com/eliotstocker/SmartThings-LightPhysicalControl/master/logo-small.png",
        iconX2Url: "https://raw.githubusercontent.com/eliotstocker/SmartThings-LightPhysicalControl/master/logo.png",
        parent: "piratemedia:Light Physical Button Time Settings"){
    appSetting "devices"
}


preferences {
    page(name: "LightSettingsPage")
}

def LightSettingsPage() {
    dynamicPage(name: "LightSettingsPage", install: true, uninstall: true) {
        section("Select Light(s) For settings") {
            input "selected", "enum", title: "Light", submitOnChange: true, options: lightChoices()
        }

        if(selected) {
            section("Select Light Settings") {
                input "io", "bool", title: "On/Off", required: true, defaultValue: true
                if(checkDeviceForCapability("SwitchLevel")) {
                    input "level", "number", title: "Light Brightness", range: "(1..100)", required: false
                }
                if(checkDeviceForCapability("ColorTemperature")) {
                    input "temp", "number", title: "Light Color Temperature", range: "(2200..6500)", required: false
                }
                if(checkDeviceForCapability("ColorControl")) {
                    input "color", "color", title: "Color", required: false
                }
            }
        }
    }
}

def checkDeviceForCapability(capability) {
    app.updateLabel(getName(selected))
    return parent.checkDeviceForCapabilityById(selected, capability)
}

def getName(id) {
    def dev = parent.getLightDevices().find {it.id == id}
    if(dev) {
        dev.label
    }
}

def lightChoices() {
    def names = [:]
    parent.getLightDevices().each{ light ->
        names[light.id] = light.label
    }
    return names
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

def initialize() {
    // TODO: subscribe to attributes, devices, locations, etc.
}

def getLightSettings() {
    return [
            light: selected,
            on: io,
            level: level,
            temp: temp,
            color: color
    ]
}

// TODO: implement event handlers