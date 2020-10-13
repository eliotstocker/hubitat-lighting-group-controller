/**
 *  Light Physical Button Setup
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

import hubitat.helper.ColorUtils

definition(
        name: "Physical Light Control Button Setup",
        namespace: "piratemedia",
        author: "Eliot Stocker",
        description: "Application to enable direction of lights based on physical/virtual Buttons",
        category: "Convenience",
        iconUrl: "https://raw.githubusercontent.com/eliotstocker/SmartThings-LightPhysicalControl/master/logo-small.png",
        iconX2Url: "https://raw.githubusercontent.com/eliotstocker/SmartThings-LightPhysicalControl/master/logo.png",
        parent: "piratemedia:Light Physical Control",
)


preferences {
    page(name: "LightSettingsPage")
}

def LightSettingsPage() {
    dynamicPage(name: "LightSettingsPage", install: true, uninstall: true) {
        section("Select the button you wish to use") {
            input "button", "capability.pushableButton", title: "Button", submitOnChange: true, multiple: false
            if(hasMoreThanButtons(1)) {
                input "offBtn", "bool", title: "Separate On/Off buttons", submitOnChange: true
                if(hasOffButton()) {
                    input "offBtnNumber", "enum", title: "Which Button should turn the lights off", options: getButtonOptions(), required: true
                }
                if(hasMoreThanButtons(3)) {
                    input "dimmingBtns", "bool", title: "Enable Dimming Buttons", submitOnChange: true
                    if(dimmingEnabled()) {
                        input "brightenButton", "enum", title: "Which Button should Brighten the Lights", options: getButtonOptions(), required: true
                        input "dimButton", "enum", title: "Which Button should Dim the Lights", options: getButtonOptions(), required: true
                    }
                }
            }
        }
        section("Select Light(s) to turn on/off") {
            input "lights", "capability.switch", title: "Lights", multiple: true, submitOnChange: true
        }

        section("Select Lights initial Settings when turned on") {
            if(checkForCapability("ChangeLevel")) {
                input "level", "number", title: "Light Brightness", range: "(1..100)", required: false
            }
            if(checkForCapability("ColorTemperature")) {
                input "temp", "number", title: "Light Color Temperature", range: "(2200..6500)", required: false
            }
            if(checkForCapability("ColorControl")) {
                input "color", "color", title: "Color", required: false
            }
        }

        section("Time specific settings (Overrides above values within selected time frames)") {
        }
        section() {
            app(name: "timeSetup", appName: "Light Physical Button Time Settings", namespace: "piratemedia", title: "New Time Specific Setting", multiple: true)
        }

        section("Lighting Setup Name") {
            label title: "Setup Name", required: true, defaultValue: app.label
        }
    }
}

def hasMoreThanButtons(count) {
    return button && button.currentValue("numberOfButtons") > count
}

def hasOffButton() {
    if(offBtn) {
        return true
    }
    return false
}

def dimmingEnabled() {
    if(dimmingBtns) {
        return true
    }
    return false
}

def getButtonOptions() {
    def buttonCount = button.currentValue("numberOfButtons")
    def opts = [:];
    for (i = 0; i < buttonCount; i++) {
        btnNum = i + 1
        opts[btnNum] = "Button $btnNum"
    }

    return opts;
}

def checkForCapability(capability) {
    def found = false
    lights.each { light ->
        def capabilites = light.getCapabilities()
        capabilites.each {cap ->
            if(cap.name == capability) {
                found = true
            }
        }
    }
    return found
}

def getDevicesWithCapability(capability) {
    def devices = [];
    lights.each { light ->
        def capabilites = light.getCapabilities()
        capabilites.each {cap ->
            if(cap.name == capability) {
                devices.push(light)
            }
        }
    }

    return devices;
}

def checkDeviceForCapabilityById(id, capability) {
    def selected;
    lights.each { light ->
        if(light.id == id) {
            selected = light
        }
    }
    if(selected != null) {
        return checkDeviceForCapability(selected, capability)
    }
    return false
}

def checkDeviceForCapability(dev, capability) {
    def found = false
    def capabilites = dev.getCapabilities()
    capabilites.each {cap ->
        if(cap.name == capability) {
            found = true
        }
    }
    return found
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(button, "pushed", buttonPressed)
    subscribe(button, "held", buttonHeld)
    subscribe(button, "released", buttonReleased)
}

def buttonPressed(evt) {
    def buttonNumber = evt.value;
    if (buttonNumber == offBtnNumber) {
        // Off Button Pressed
        return lights.each {it.off()}
    }

    if (buttonNumber == dimButton || buttonNumber == brightenButton) {
        //Dimming Buttons Pressed (Do Noithing Here)
        return
    }

    if (!offBtnNumber && lights.find {it.currentValue("switch") == "on"}) {
        //This is a Toggle Button turn off Lights
        return lights.each {it.off()}
    }

    def children = getChildApps()
    def activeChild = children.find {it.isActive()}

    if (activeChild) {
        return runChildAction(activeChild);
    }

    setLightingState([level: level, temp: temp, color: color]);
}

def buttonHeld(evt) {
    def buttonNumber = evt.value;
    def dimmable = getDevicesWithCapability("ChangeLevel");

    switch(buttonNumber) {
        case dimButton:
            log.debug "Dimming";
            if(state.dimming != "down") {
                state.dimming = "down"
                dimmable.each {it.startLevelChange("down")}
            }
            break;
        case brightenButton:
            log.debug "Brightening";
            if(state.dimming != "up") {
                state.dimming = "up"
                dimmable.each {it.startLevelChange("up")};
            }
            break;
    }
}

def buttonReleased(evt) {
    def buttonNumber = evt.value;

    log.debug "Stop Dimming";

    def dimmable = getDevicesWithCapability("ChangeLevel");

    dimmable.each {it.stopLevelChange()};
}

def setLightingState(lState) {
    lights.each {setLightingState(lState, it)}
}

def setLightingState(lState, light) {
    light.on();

    if(lState.level && checkDeviceForCapability(light, "SwitchLevel")) {
        light.setLevel(lState.level, 0.25)
    }

    if(lState.temp && checkDeviceForCapability(light, "ColorTemperature")) {
        light.setColorTemperature(lState.temp)
    }

    if(lState.color != "#000000" && checkDeviceForCapability(light, "ColorControl")) {
        def rgb = ColorUtils.hexToRGB(lState.color)
        def hsv = ColorUtils.rgbToHSV(rgb)
        def colorMap = [hue: hsv[0], saturation: hsv[1], level: lState.level || hsv[2]]

        light.setColor(colorMap)
    }
}

def runChildAction(child) {
    def settings = child.getSettings()
    def topSettings = [level: level, temp: temp, color: color]

    if (child.hasSpecificSettings()) {
        //Run though each light and apply specific settings
        return lights.each{ light ->
            def data = child.getSpecificLightSetting(light.id)

            if(!data) {
                //No Specific Settings for this light
                return setLightingState(settings, light)
            }

            if(!data.on) {
                return light.off()
            }

            setLightingState(computeSettings(data, settings, topSettings), light)
        }
    } else {
        //Apply settings to all lights
        setLightingState(computeSettings(settings, topSettings))
    }
}

def computeSettings(Map... settingsArray) {
    def settings = [:]

    settingsArray.reverse().each {
        if(it.level != null) {
            settings.level = it.level
        }
        if(it.temp != null) {
            settings.temp = it.temp
        }
        if(it.color != null && it.color != "#000000") {
            settings.color = it.color
        }
        log.debug "Settings: $settings"
    }

    return settings;
}

def getLightDevices() {
    def vals = []
    lights.each{ light ->
        def l = [
                id: light.id,
                label: light.label
        ]
        vals.add(l)
    }
    return vals
}