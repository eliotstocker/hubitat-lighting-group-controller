/**
 *  Light Physical Button Time Settings
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
        name: "Physical Light Control Button Time Settings",
        namespace: "piratemedia",
        author: "Eliot Stocker",
        description: "Time specific setting for light setup",
        category: "Convenience",
        iconUrl: "https://raw.githubusercontent.com/eliotstocker/SmartThings-LightPhysicalControl/master/logo-small.png",
        iconX2Url: "https://raw.githubusercontent.com/eliotstocker/SmartThings-LightPhysicalControl/master/logo.png",
        parent: "piratemedia:Light Physical Button Setup"
)


preferences {
    page(name: "LightTimeSettingsPage")
}

def LightTimeSettingsPage() {
    dynamicPage(name: "LightTimeSettingsPage", install: true, uninstall: true) {
        section("Time Range") {
            input "startType", "enum", title:"Start Type", options: ['specific': 'Specify', 'sunrise': 'Sunrise', 'sunset': 'Sunset'], require: true, submitOnChange: true
            if (isSpecific(startType)) {
                input "start", "time", title:"Start Time", required: true
            }
            input "endType", "enum", title:"End Type", options: ['specific': 'Specify', 'sunrise': 'Sunrise', 'sunset': 'Sunset'], require: true, submitOnChange: true
            if (isSpecific(endType)) {
                input "end", "time", title:"End Time", required: true
            }
        }
        section("Select Lights Settings when turned on within selected time range") {
            if(parent.checkForCapability("ChangeLevel")) {
                input "level", "number", title: "Light Brightness", range: "(1..100)", required: false
            }
            if(parent.checkForCapability("ColorTemperature")) {
                input "temp", "number", title: "Light Color Temperature", range: "(2200..6500)", required: false
            }
            if(parent.checkForCapability("ColorControl")) {
                input "color", "color", title: "Color", required: false
            }
        }
        section("Advanced: Per Light Settings") {
        }
        section() {
            app(name: "singleLight", appName: "Light Physical Single Light Setting", namespace: "piratemedia", title: "New Single Light Setting", multiple: true)
        }
        section("Time Range Name") {
            label title: "Name", required: true, defaultValue: app.label
        }
    }

}

def isSpecific(selector) {
    return selector == "specific"
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

def getTime(which) {
    def type
    def specific
    if(which == "start") {
        type = startType
        specific = start
    } else {
        type = endType
        specific = end
    }


    if(isSpecific(type)) {
        return Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", specific)
    }

    def loc = getLocation()

    if (type == "sunrise") {
        log.debug "sunrise"
        return loc.sunrise
    }

    if (type == "sunset") {
        log.debug "sunrise"
        return loc.sunset
    }

    return null;
}

def isActive() {
    def s = getTime("start")
    def e = getTime("end")

    if(e.before(s)) {
        e = e.plus(1)
    }

    log.debug "Times | Start: $s, End: $e"

    if(timeOfDayIsBetween(s, e, new Date()) || timeOfDayIsBetween(s.minus(1), e.minus(1), new Date())) {
        return true;
    }
    return false;
}

def getSettings() {
    return [
            level: level,
            temp: temp,
            color: color
    ]
}

def getSpecificLightSetting(id) {
    def children = getChildApps()
    def data = null;
    children.each { child ->
        def settings = child.getLightSettings();
        if(settings.light == id) {
            data = settings
        }
    }
    return data
}

def checkDeviceForCapabilityById(id, capability) {
    return parent.checkDeviceForCapabilityById(id, capability)
}

def hasSpecificSettings() {
    def children = getChildApps()
    return children.size > 0
}

def getLightDevices() {
    return parent.getLightDevices()
}