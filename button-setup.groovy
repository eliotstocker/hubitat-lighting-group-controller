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

import hubitat.helper.ColorUtils
import hubitat.helper.HexUtils

definition(
        name: "Lighting Group Controller",
        namespace: "piratemedia",
        author: "Eliot Stocker",
        description: "Application to enable direction of lights based on physical/virtual Buttons",
        category: "Convenience",
        iconUrl: "https://raw.githubusercontent.com/eliotstocker/SmartThings-LightPhysicalControl/master/logo-small.png",
        iconX2Url: "https://raw.githubusercontent.com/eliotstocker/SmartThings-LightPhysicalControl/master/logo.png",
        parent: "piratemedia:Lighting Group Manager",
)


preferences {
    page(name: "prefInit")
    page(name: "prefMainMenu")

    page(name: "prefButtonSetup")

    page(name: "prefLightSetup")

    page(name: "prefTopLevelSettings")
    page(name: "prefAdvancedTopLevel")
    page(name: "prefIndividualLightSetting")

    page(name: "prefTimedSettings")
    page(name: "prefTimedSettingSetup")

    page(name: "prefButtonOverrides")

    page(name: "prefAdvancedSettings")
}

// Top Level Settings ///////////////////////
/////////////////////////////////////////////

def prefInit() {
    if (app.getInstallationState() == "COMPLETE") {
        upgradeIfRequired()
        return prefMainMenu()
    }

    return dynamicPage(name: "prefInit", title: "Group Setup | Installation", nextPage: "mainMenu", uninstall: false, install: true) {
        section() {
            paragraph("Welcome to Lighting Group Controller, Lets get started with some basic setup:")
        }

        section("Group Name") {
            label title: "Name", required: true, defaultValue: app.label
        }

        section("Contol Button") {
            input "button", "capability.pushableButton", title: "Button", multiple: false, required: true
        }

        section("Group Lights") {
            input "lights", "capability.switch", title: "Lights", multiple: true, required: true
        }

        section() {
            paragraph("<sub>More settings will be available once initial setup is complete...</sub>")
        }
    }
}

def prefMainMenu() {
    clearTempPrefs()
    return dynamicPage(name: "prefMainMenu", title: "Group: ${app.label}", uninstall: true, install: true) {
        section {
            href(name: "buttonSetup", title: "Edit Remote", required: false, page: "prefButtonSetup", description: "Remote, Button Mapping etc")
            href(name: "lightSetup", title: "Edit Controlled Lights", required: false, page: "prefLightSetup", description: "Edit Lights which are in the group")
            href(name: "topLevelSettings", title: "Top Level Settings", required: false, page: "prefTopLevelSettings", description: "Default settings for lights when no timed settings apply")
            href(name: "timedSettings", title: "Timed Settings", required: false, page: "prefTimedSettings", description: "Setup different scenes to trigger based on time of day")
            if(dimmingEnabled()) {
                href(name: "buttonOverrides", title: "Button Overrides", required: false, page: "prefButtonOverrides", description: "Set buttons to run specific settings when quick pressed any time")
            }
            href(name: "advancedSettings", title: "Advanced Settings", required: false, page: "prefAdvancedSettings", description: "These settings are for more advanced control and debugging")
        }
    }
}

def prefButtonSetup() {
    clearTempPrefs()
    return dynamicPage(name: "prefButtonSetup", title: "Group: ${app.label} | Remote", nextPage: "prefMainMenu") {
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
    }
}

def prefLightSetup() {
    clearTempPrefs()
    return dynamicPage(name: "prefLightSetup", title: "Group: ${app.label} | Lights", nextPage: "prefMainMenu") {
        section("Select Light(s) to control") {
            input "lights", "capability.switch", title: "Lights", multiple: true, submitOnChange: true
        }
    }
}

def prefTopLevelSettings(params) {
    clearTempPrefs()

    if(params && params.snapshot) {
        def snapshot = snapshotConfiguration()
        state.individualSettings = snapshot.individualSettings
        this.level = snapshot.level
        app.updateSetting("level", [type: "number", value: snapshot.level])
        this.temp = snapshot.temp
        app.updateSetting("temp", [type: "number", value: snapshot.temp])
        this.color = snapshot.color
        app.updateSetting("color", [type: "color", value: snapshot.color])
    }

    return dynamicPage(name: "prefTopLevelSettings", title: "Group: ${app.label} | Top Level Settings", nextPage: "prefMainMenu") {
        section() {
            paragraph("these settings are used when no timed settings are active");
            if(checkForCapability("ChangeLevel")) {
                input "level", "number", title: "Light Brightness", range: "1..100", required: false
            }
            if(checkForCapability("ColorTemperature")) {
                input "temp", "number", title: "Light Color Temperature", range: "2200..6500", required: false
            }
            if(checkForCapability("ColorControl")) {
                input "color", "color", title: "Color", required: false
            }
        }
        section("Individual Light Settings") {
            href(name: "advancedTopLevel", title: "Top level per light settings (${state.individualSettings.size()}/${lights.size()})", required: false, page: "prefAdvancedTopLevel", description: "Set values for individual lights")
        }

        section("Snapshot | <b>BETA</b>") {
            paragraph("<sub>The snapshot functionality allows you to set values for this Lighting group based on the current settings of each light in the group, get the lights setup how you like them, and then press the \"Set From Snapshot\" button bellow<br><b>Warning: this will override all of your existing top level settings!</b></sub>")
            href(name: "timedSettingSetup-Snapshot", title: "Set From Snapshot", description: "<b>BETA</b> | Set To level settings based on the current phisical light settings", required: false, page: "prefTopLevelSettings", params: [snapshot: true])
        }
    }
}

def prefTimedSettings(params) {
    if(params && params.remove && params.index != null) {
        state.timedSettings.remove(params.index)
    }

    clearTempPrefs()

    return dynamicPage(name: "prefTimedSettings", title: "Group: ${app.label} | Timed Settings", nextPage: "prefMainMenu") {
        section() {
            paragraph("Bellow you can create timed settings these will override all top level settings between the start and end times")
            state.timedSettings.eachWithIndex { timed, i ->
                renderSingleTimedSetting(i, timed)
            }
            href(name: "timedSettingSetup-New", title: "Add a new Timed Setting", description: "set specific settings for a specific time frame", required: false, page: "prefTimedSettingSetup")
        }
        section("Snapshot | <b>BETA</b>") {
            paragraph("<sub>The snapshot functionality allows you to set values for this Lighting group based on the current settings of each light in the group, get the lights setup how you like them, and then press the \"Set From Snapshot\" button bellow</sub>")
            href(name: "timedSettingSetup-Snapshot", title: "New from Snapshot", description: "<b>BETA</b> | New time specific setting based on the current phisical light settings", required: false, page: "prefTimedSettingSetup", params: [snapshot: true])
        }
    }
}

def prefAdvancedSettings() {
    clearTempPrefs()
    return dynamicPage(name: "prefAdvancedSettings", title: "Group: ${app.label} | Advanced Settings", nextPage: "prefMainMenu") {
        section("Debug and Fun") {
            input "doubleSendActions", "bool", title: "Send actions twice (all at once followed by each command in order)", defaultValue: true
            input "orderedactions", "bool", title: "Turn lights on/off in order", defaultValue: false
            input "interlightdelay", "number", title: "Inter Light Delay (ms)", range: "10..1000", defaultValue: 100
            input "sendOnCommand", "bool", title: "Send On Command (When Level and/or color temp set)", defaultValue: false
        }
        section("Operation") {
            input "disable", "bool", title: "Pause Button Opperations (Use this to temperarity disable button opperation)", defaultValue: false
            input "onlyLog", "bool", title: "Log actions but do not perform (For Debug Use Only)", defaultValue: false
            paragraph("<sub>View Logs <a href=\"/logs\" target=\"_blank\">Here</a>")
        }
    }
}

def prefButtonOverrides() {
    clearTempPrefs()
    return dynamicPage(name: "prefButtonOverrides", title: "Group: ${app.label} | Button Override Settings", nextPage: "prefMainMenu") {
        section() {
            paragraph "Here you may select some settings to run at any time when dim and/or brighten buttons are pressed"
            input "onBrightenPress", "enum", title: "On Brighten Quick Press", options: getActionOptions()
            input "onDimPress", "enum", title: "On Dim Quick Press", options: getActionOptions()
        }
    }
}

// Top Level Advanced Settings //////////////
/////////////////////////////////////////////

def prefAdvancedTopLevel(params) {
    if(params && params.remove && params.id) {
        state.individualSettings.remove(params.id)
    }

    clearTempPrefs()

    return dynamicPage(name: "prefAdvancedTopLevel", title: "Group: ${app.label} | Top Level Settings | Advanced", nextPage: "prefTopLevelSettings") {
        section() {
            paragraph("Bellow you can create specific settings for individual lights, these will override your top level settings...")
            state.individualSettings.entrySet().each { light ->
                renderSingleLightSetting(light.key, light.value)
            }
            href(name: "individualLightSetting-New", title: "Add a new Individual Light Setting", description: "set specific settings for a particular light...", required: false, page: "prefIndividualLightSetting")
        }
    }
}

def prefIndividualLightSetting(params) {
    def installed = false
    def title = "Group: ${app.label} | New Individual Light Setting"
    if(params && params.id) {
        installed = true
        title = "Group: ${app.label} | Setting for: ${getDeviceNameFromId(params.id)}"
    }

    def base = params && params.base ? state[params.base] : state

    if(params && params.selector != null) {
        base = base[params.selector]
        if(installed) {
            title = "Group: ${app.label} | ${base.label} | ${getDeviceNameFromId(params.id)}"
        } else {
            title = "Group: ${app.label} | ${base.label} | New Individual Light Setting"
        }
    }

    if(params && params.back && params.back.params) {
        state.__editingTimed = params.back.params
    }

    return dynamicPage(name: "prefIndividualLightSetting", title: title, nextPage: (params && params.back ? params.back.page : "prefAdvancedTopLevel")) {
        section() {
            customCSS()

            if(!installed) {
                input "__i_light", "enum", title: "Light", multiple: false, required: false, options: getGroupLightOptions(), submitOnChange: true
            }

            if(__i_light || installed) {
                def l = installed ? params.id : __i_light
                if(!installed) {
                    base.individualSettings[l] = [:]
                }

                input "__i_on", "bool", title: "Light On", required: false, submitOnChange: true, defaultValue: installed ? base.individualSettings[l].on : true
                paragraph("<sub>you may wish to default a light to off, but turn on for specific timed modes...</sub>")
                base.individualSettings[l].on = __i_on == null ? (installed ? base.individualSettings[l].on : true) : __i_on

                if(checkForCapability("ChangeLevel", l)) {
                    input "__i_level", "number", title: "Light Brightness", range: "1..100", required: false, submitOnChange: true, defaultValue: installed ? base.individualSettings[l].level : null
                    if(l) {
                        base.individualSettings[l].level = __i_level
                    }
                }
                if(checkForCapability("ColorTemperature", l)) {
                    input "__i_temp", "number", title: "Light Color Temperature", range: "2200..6500", required: false, submitOnChange: true, defaultValue: installed ? base.individualSettings[l].temp : null
                    if(__i_temp) {
                        base.individualSettings[l].temp = __i_temp
                    }
                }
                if(checkForCapability("ColorControl", l)) {
                    input "__i_color", "color", title: "Color", required: false, submitOnChange: true, defaultValue: installed ? base.individualSettings[l].color : null
                    if(__i_color) {
                        base.individualSettings[l].color = __i_color
                    }
                }
            }

            if(installed) {
                def removeParams = [id: params.id, remove: true]
                if(params.back && params.back.params) {
                    removeParams.putAll(params.back.params)
                }

                href(name: "remove", title: "Remove Light Setting", required: false, page: (params && params.back ? params.back.page : "prefAdvancedTopLevel"), description: "remove...", params: removeParams)
                paragraph """<script>
\$('button[name^="_action_href_remove|"]').addClass('remove-btn-custom');
</script>"""
            }
        }
    }
}

// Timed Settings ///////////////////////////
/////////////////////////////////////////////

def prefTimedSettingSetup(params) {
    clearTempPrefs(true)

    def installed = false
    def snapshot
    def title = "Group: ${app.label} | New Timed Setting"
    def vals = [
            id: generateID()
    ]

    def showIndividual = false
    if(state.__editingTimed) {
        showIndividual = true
        params = state.__editingTimed
    }

    if(params && params.index != null) {
        installed = true
        vals = state.timedSettings[params.index]
        title = "Group: ${app.label} | Timed Setting: ${vals.label}"

        this.__t_index = params.index
        app.updateSetting("__t_index", [type: "number", value: params.index])
    }

    if(installed && params && params.remove && params.id) {
        state.timedSettings[params.index].individualSettings.remove(params.id)
    }

    if(params && params.snapshot) {
        snapshot = snapshotConfiguration()
        snapshot.id = generateID()

        showIndividual = true
        vals = snapshot
    }

    return dynamicPage(name: "prefTimedSettingSetup", title: title, nextPage: "prefTimedSettings") {
        section() {
            customCSS()
            input "__t_label", "text", title: "Setup Name", required: true, submitOnChange: true, defaultValue: installed ? vals.label : null
            if(__t_label) {
                vals.label = __t_label
            }
        }

        section("Time Range") {
            input "__t_startType", "enum", title:"Start Type", options: timedEnums, required: true, submitOnChange: true, defaultValue: installed ? vals.startType : null
            if(__t_startType) {
                vals.startType = __t_startType
            }
            if (isSpecific(vals.startType)) {
                input "__t_start", "time", title:"Start Time", required: true, submitOnChange: true, defaultValue: installed ? vals.start : null
                if(__t_start) {
                    vals.start = __t_start
                }
            } else {
                vals.remove("start")
            }

            input "__t_endType", "enum", title:"End Type", options: timedEnums, required: true, submitOnChange: true, defaultValue: installed ? vals.endType : null
            if(__t_endType) {
                vals.endType = __t_endType
            }
            if (isSpecific(vals.endType)) {
                input "__t_end", "time", title:"End Time", required: true, submitOnChange: true, defaultValue: installed ? vals.end : null
                if(__t_end) {
                    vals.end = __t_end
                }
            } else {
                vals.remove("end")
            }
        }

        section(hideable: true, "Select Lights Settings when turned on within selected time range") {
            if(checkForCapability("ChangeLevel")) {
                input "__t_level", "number", title: "Light Brightness", range: "1..100", required: false, submitOnChange: true, defaultValue: installed || snapshot ? vals.level : null
                if(__t_level) {
                    vals.level = __t_level
                }
            }
            if(checkForCapability("ColorTemperature")) {
                input "__t_temp", "number", title: "Light Color Temperature", range: "2200..6500", submitOnChange: true, required: false, defaultValue: installed || snapshot ? vals.temp : null
                if(__t_temp) {
                    vals.temp = __t_temp
                }
            }
            if(checkForCapability("ColorControl")) {
                input "__t_color", "color", title: "Color", required: false, submitOnChange: true, defaultValue: installed || snapshot ? vals.color : null
                if(__t_color) {
                    vals.color = __t_color
                }
            }
        }

        if(!vals.individualSettings) {
            vals.individualSettings = [:]
        }

        def individualLightParams = [base: "timedSettings", selector: __t_index, back: [page: "prefTimedSettingSetup", params: params]]

        section(hideable: true, hidden: !showIndividual, "Individual Light Setting") {
            vals.individualSettings.entrySet().each { light ->
                renderSingleLightSetting(light.key, light.value, individualLightParams)
            }
            href(name: "individualLightSetting-New", title: "Add a new Individual Light Setting", description: "set specific settings for a particular light...", required: false, page: "prefIndividualLightSetting", params: individualLightParams)
        }

        if(vals.size() > 0 && validateTimed(vals)) {
            if(__t_index != null) {
                state.timedSettings[(int)__t_index] = vals
            } else {
                state.timedSettings.push(vals)
                app.updateSetting("__t_index", [type: "number", value: state.timedSettings.size() - 1])
            }
        }

        section("Snapshot | <b>BETA</b>") {
            def snapshotParams = [snapshot: true]
            if(params) {
                snapshotParams.putAll(params)
            }
            paragraph("<sub>The snapshot functionality allows you to set values for this Lighting group based on the current settings of each light in the group, get the lights setup how you like them, and then press the \"Replace with Snapshot\" button bellow<b>Warning: this will override all values for this timed setting!</b></sub>")
            href(name: "snapshotCurrent", title: "Replace with Snapshot", description: "<b>BETA</b> | take a snapshot of lighting configuration as it is right now", required: false, page: "prefTimedSettingSetup", params: snapshotParams)
        }

        if(installed) {
            section() {
                href(name: "remove", title: "Remove Light Setting", required: false, page: "prefTimedSettings", description: "remove...", params: [index: params.index, remove: true])

                paragraph """<script>
\$('button[name^="_action_href_remove|"]').addClass('remove-btn-custom');
</script>"""
            }
        }
    }
}

// Preference Helpers ///////////////////////////////////
/////////////////////////////////////////////////////////

def generateID() {
    def generator = { String alphabet, int n ->
        new Random().with {
            (1..n).collect { alphabet[ nextInt( alphabet.length() ) ] }.join()
        }
    }

    return generator((('A'..'Z')+('a'..'z')+('0'..'9')).join(), 24)
}

def validateTimed(vals) {
    if(!vals.label || !vals.startType || !vals.endType) {
        return false
    }

    if(isSpecific(vals.startType) && !vals.start) {
        return false
    }

    if(isSpecific(vals.endType) && !vals.end) {
        return false
    }

    return true
}

def renderSingleLightSetting(id, singleLight) {
    renderSingleLightSetting(id, singleLight, [])
}

def renderSingleLightSetting(id, singleLight, params) {
    singleLight.id = id;

    def attributes = singleLight.on ? "On" : "Off"
    if(singleLight.on) {
        if(singleLight.level) {
            attributes += " | ${singleLight.level}%"
        }
        if(singleLight.color && singleLight.color != "#000000") {
            attributes += "<span style=\"margin-left: 5px; width: 25px; height: 25px; display: inline-block; background-color=\"${singleLight.color}\"></span>"
        } else if(singleLight.temp) {
            attributes += " | ${singleLight.temp}K"
        }
    }

    def loadParams = [id: id]
    loadParams.putAll(params)

    href(name: "individualLightSetting-${id}", title: getDeviceNameFromId(id), description: attributes, required: false, page: "prefIndividualLightSetting", params: loadParams)
    setLightButtonStyle("individualLightSetting-${id}", getColorForSetting(singleLight))
}

def renderSingleTimedSetting(index, timed) {
    timed.index = index
    timed.on = true

    href(name: "timedLightSetting-${index}", title: timed.label, description: displayTime(timed), required: false, page: "prefTimedSettingSetup", params: [index: index])
    setLightButtonStyle("timedLightSetting-${index}", getColorForSetting(timed))
}

def getColorForSetting(setting) {
    if(setting.on) {
        if(setting.color && setting.color != "#000000") {
            return setting.color
        }
        if(setting.temp) {
            return ctToRGB(setting.temp, setting.level ? setting.level : 100)
        }
        if(setting.level) {
            val = HexUtils.integerToHexString((int) Math.round(setting.level * 2.55), 1)
            return "#${val}${val}${val}"
        }
        return "#FFFFFF"
    }

    return "#000000"
}

def setLightButtonStyle(name, color) {
    paragraph """<script>
		\$('button[name^="_action_href_${name}|"]').css({'border-left-color': '${color}', 'border-left-width': '70px'}); 
	</script>"""
}

def isSpecific(selector) {
    return selector == "specific"
}

timedEnums = ['specific': 'Specify', 'sunrise': 'Sunrise', 'sunset': 'Sunset']

def displayTime(timed) {
    def display = "From: ";
    if(!timed.startType || !timed.endType) {
        return "Time Error!"
    }

    if(isSpecific(timed.startType)) {
        try {
            display += Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", timed.start).format('HH:mm')
        } catch(e) {
            display += "Time Error"
        }
    } else {
        display += timedEnums[timed.startType]
    }

    display += ", Until: "

    if(isSpecific(timed.endType)) {
        try {
            display += Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", timed.end).format('HH:mm')
        } catch(e) {
            display += "Time Error"
        }
    } else {
        display += timedEnums[timed.endType]
    }

    return display;
}

def clearTempPrefs(individual) {
    app.updateSetting("__i_light", [type: "enum", value: ""])
    app.updateSetting("__i_on", [type: "bool", value: ""])
    app.updateSetting("__i_level", [type: "number", value: ""])
    app.updateSetting("__i_temp", [type: "number", value: ""])
    app.updateSetting("__i_color", [type: "color", value: ""])
    if(individual) {
        return
    }

    app.updateSetting("__t_label", [type: "text", value: ""])
    app.updateSetting("__t_index", [type: "number", value: ""])
    app.updateSetting("__t_startType", [type: "enum", value: ""])
    app.updateSetting("__t_start", [type: "time", value: ""])
    app.updateSetting("__t_endType", [type: "enum", value: ""])
    app.updateSetting("__t_end", [type: "time", value: ""])
    app.updateSetting("__t_level", [type: "number", value: ""])
    app.updateSetting("__t_temp", [type: "number", value: ""])
    app.updateSetting("__t_color", [type: "color", value: ""])
    state.__editingTimed = null
}

def customCSS() {
    paragraph """<style>
        .remove-btn-custom { background-color: #cc2d3b; color: #FFFFFF; border-left: none; width: auto !important; float: right;}
		.remove-btn-custom::before { content: ''; font-family: 'Material Icons'; transform: none !important }
        .remove-btn-custom .state-incomplete-text { display: none; }
	</style>"""
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

def getGroupLightOptions(exclude) {
    def opts = getGroupLightOptions()
    for(id in exclude) {
        opts.remove(id)
    }

    return opts
}

def getGroupLightOptions() {
    def opts = [:]
    for (light in lights) {
        opts[light.id] = light.label
    }

    return opts
}

def getActionOptions() {
    def opts = [
            main: "Top Level Settings"
    ]

    for(timed in state.timedSettings) {
        opts[timed.id] = timed.label
    }

    return opts
}

//pretty much coppied verbatim from: https://github.com/mattdesl/kelvin-to-rgb
def ctToRGB(ct, level) {
    //add 2000 for visual adjustment
    temp = (2000 + ct) / 100
    def red
    def blue
    def green

    if (temp <= 66) {
        red = 255
    } else {
        red = temp - 60
        red = 329.698727466 * Math.pow(red, -0.1332047592)
        if (red < 0) {
            red = 0
        }
        if (red > 255) {
            red = 255
        }
    }

    if (temp <= 66) {
        green = temp
        green = 99.4708025861 * Math.log(green) - 161.1195681661
        if (green < 0) {
            green = 0
        }
        if (green > 255) {
            green = 255
        }
    } else {
        green = temp - 60
        green = 288.1221695283 * Math.pow(green, -0.0755148492)
        if (green < 0) {
            green = 0
        }
        if (green > 255) {
            green = 255
        }
    }

    if (temp >= 66) {
        blue = 255
    } else {
        if (temp <= 19) {
            blue = 0
        } else {
            blue = temp - 10
            blue = 138.5177312231 * Math.log(blue) - 305.0447927307
            if (blue < 0) {
                blue = 0
            }
            if (blue > 255) {
                blue = 255
            }
        }
    }


    def modifier = ((float)level / 200.0) + 0.5

    def rHex = HexUtils.integerToHexString((int) Math.round(red * modifier), 1)
    def gHex = HexUtils.integerToHexString((int) Math.round(green * modifier), 1)
    def bHex = HexUtils.integerToHexString((int) Math.round(blue * modifier), 1)

    return "#${rHex}${gHex}${bHex}"
}

// Control Code /////////////////////////////////////////
/////////////////////////////////////////////////////////

def installed() {
    app.updateSetting('LCGVersion', [type: 'number', value: 1])
    this.LCGVersion = 1;
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
    unsubscribe()

    if(!state.individualSettings) {
        state.individualSettings = [:]
    }

    if(!state.timedSettings) {
        state.timedSettings = []
    }

    if(enabled == null) {
        app.updateSetting("enabled", [type: "bool", value: "true"])
    }

    subscribe(button, "pushed", buttonPressed)
    subscribe(button, "held", buttonHeld)
    subscribe(button, "released", buttonReleased)
}

def upgradeIfRequired() {
    if (!LCGVersion || LCGVersion < 1) {
        log.debug "Migrating from Version: ${LCGVersion ? LCGVersion : 'Legacy'}"
        migrate()
    }
}

//TODO: we can remove this migration at some point, though we should probably keep the mechanism for any future migartions
def migrate(dryRun = false) {
    log.debug "New Version: 1"

    //this is a hack to update the app.name (change the label and then change back)
    if(!dryRun) {
        def nameCache = app.label
        app.updateLabel("Lighting Group Controller")
        app.updateLabel(nameCache)
    }


    def children = getChildApps()

    //Migrate Timed Settings
    def timed = children.findAll {
        it.getName() == "Light Physical Button Time Settings"
    }

    def timedSettings = []

    timed.forEach {
        def setting = it.getSettings()
        setting.id = generateID()
        setting.startType = it.startType
        setting.endType = it.endType
        setting.start = it.start
        setting.end = it.end
        setting.label = it.label

        if(it.hasSpecificSettings()) {
            setting.individualSettings = [:]

            lights.forEach { l ->
                def ind = it.getSpecificLightSetting(l.id)

                if(ind) {
                    setting.individualSettings[l.id] = ind
                }
            }
        }

        timedSettings.push(setting)
    }

    if(!dryRun) {
        state.timedSettings = timedSettings
    }


    // Migrate Top Level Individual Light Settings
    def individual = children.findAll {
        it.getName() == "Light Physical Single Light Setting"
    }

    def individualSettings = [:]

    individual.forEach {
        def settings = it.getLightSettings()

        individualSettings[settings.id] = settings
    }

    //store individual settings
    if(!dryRun) {
        state.individualSettings = individualSettings
    }

    //set new version
    if(!dryRun) {
        app.updateSetting('LCGVersion', [type: 'number', value: 1])
        this.LCGVersion = 1
    }

    //delete child apps
    children.forEach{ child ->
        def grandChildren = child.getChildApps()
        if(grandChildren.size()) {
            grandChildren.forEach{ gc ->
                log.debug "Deleting: ${gc.label}(${gc.id})"
                if(!dryRun) {
                    child.deleteChildApp(gc.id)
                }
            }
        }

        log.debug "Deleting: ${child.label}(${child.id})"
        if(!dryRun) {
            deleteChildApp(child.id)
        }
    }

    if(dryRun) {
        values = [
                LCGVersion: 1,
                individualSettings: individualSettings,
                timedSettings: timedSettings
        ]

        log.debug "Migrated Values: ${values}"
    }
}

def buttonPressed(evt) {
    upgradeIfRequired()

    if(disable) {
        log.info "Button ${app.label} is Disabled..."
        return
    }

    def buttonNumber = evt.value;
    if (buttonNumber == offBtnNumber) {
        // Off Button Pressed
        return setLightingOff()
    }

    if (buttonNumber == dimButton) {
        //Dimming Button Quick Pressed
        return runDimQuickAction()
    }

    if (buttonNumber == brightenButton) {
        //Brighten Button Quick Pressed
        return runBrightenQuickAction()
    }

    if (!offBtnNumber && lights.any {it.currentValue("switch") == "on"}) {
        //This is a Toggle Button turn off Lights
        return setLightingOff()
    }

    def activeTimedAction = getCurrentActiveTimedAction()

    if (activeTimedAction) {
        return runTimedAction(activeTimedAction.id)
    }

    runTopAction()
}

def buttonHeld(evt) {
    upgradeIfRequired()

    if(disable) {
        log.info "Button ${app.label} is Disabled..."
        return
    }

    def buttonNumber = evt.value;
    def dimmable = getDevicesWithCapability("ChangeLevel");

    switch(buttonNumber) {
        case dimButton:
            if(state.dimming != "down") {
                state.dimming = "down"
                dimmable.each {it.startLevelChange("down")}
            }
            break;
        case brightenButton:
            if(state.dimming != "up") {
                state.dimming = "up"
                dimmable.each {it.startLevelChange("up")};
            }
            break;
    }
}

def buttonReleased(evt) {
    upgradeIfRequired()

    if(disable) {
        log.info "Button ${app.label} is Disabled..."
        return
    }

    def buttonNumber = evt.value;

    def dimmable = getDevicesWithCapability("ChangeLevel");

    dimmable.each {it.stopLevelChange()};
}

def getInterLightDelay() {
    if(interlightdelay != null && interlightdelay instanceof Integer || interlightdelay instanceof Long) {
        return interlightdelay
    }

    return 100
}

def getDeviceNameFromId(id) {
    def selected = lights.find { it.id == id }
    if(selected != null) {
        return selected.label
    }
    return "Unknown Device"
}

def checkDeviceForCapability(dev, capability) {
    def capabilites = dev.getCapabilities()
    return capabilites.any { it.name == capability }
}

def checkForCapability(capability, id) {
    def selected = lights.find { it.id == id }
    if(selected != null) {
        return checkDeviceForCapability(selected, capability)
    }
    return false
}

def checkForCapability(capability) {
    return lights.any { light ->
        light.getCapabilities().any { it.name == capability }
    }
}

def getDevicesWithCapability(capability) {
    return lights.findAll { light ->
        light.getCapabilities().any { it.name == capability }
    }
}

def getTime(id, which) {
    def timed = getTimedActionById(id)

    if(!timed) {
        return null
    }

    if(isSpecific(timed["${which}Type"])) {
        def date = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", timed[which])

        return timeToday(date.format("HH:mm"))
    }

    def loc = getLocation()

    if (timed["${which}Type"] == "sunrise") {
        return loc.sunrise
    }

    if (timed["${which}Type"] == "sunset") {
        return loc.sunset
    }

    return null;
}

def isActive(id) {
    def s = getTime(id, "start")
    def e = getTime(id, "end")

    if(e.before(s)) {
        e = e.plus(1)
    }

    if(timeOfDayIsBetween(s, e, new Date()) || timeOfDayIsBetween(s.minus(1), e.minus(1), new Date())) {
        return true;
    }

    return false;
}

def getCurrentActiveTimedAction() {
    return state.timedSettings.find { timed ->
        return isActive(timed.id)
    }
}

def runDimQuickAction() {
    if(!onDimPress) {
        return
    }

    if(onDimPress == "main") {
        return runTopAction()
    }

    return runTimedAction(onDimPress)
}

def runBrightenQuickAction() {
    if(!onBrightenPress) {
        return
    }

    if(onBrightenPress == "main") {
        return runTopAction()
    }

    return runTimedAction(onBrightenPress)
}

def setLightingState(lState) {
    lightsSorted().eachWithIndex { light, i ->
        setLightStateAsync(lState, light, i)
    }
}

def setLightingOff() {
    lightsSorted().reverse().eachWithIndex { light, i ->
        setLightOffAsync(light, i)
    }
}

def setLightStateAsync(state, light, order) {
    if(!orderedactions) {
        setLightState([state: state, light: light.id])
    }
    runInMillis(order * getInterLightDelay(), 'setLightState', [data: [state: state, light: light.id], overwrite: false])
}

def setLightOffAsync(light, order) {
    if(!orderedactions) {
        setLightOff(light.id)
    }
    runInMillis(order * getInterLightDelay(), 'setLightOff', [data: light.id, overwrite: false])
}

def setLightState(input) {
    def lState = input['state']
    def light = lights.find {it.id == input['light']}


    if(sendOnCommand || (!(lState.color && lState.color != "#000000" && checkDeviceForCapability(light, "ColorControl"))
            && !(lState.level && checkDeviceForCapability(light, "SwitchLevel"))
            && !(lState.temp && checkDeviceForCapability(light, "ColorTemperature")))) {
        if(onlyLog) {
            log.debug "${input['light']} | Turn On"
        } else {
            light.on()
        }
    }

    def setColor = false

    if(lState.color && lState.color != "#000000" && checkDeviceForCapability(light, "ColorControl")) {
        def rgb = ColorUtils.hexToRGB(lState.color)
        def hsv = ColorUtils.rgbToHSV(rgb)
        def colorMap = [hue: hsv[0], saturation: hsv[1], level: lState.level ? lState.level : hsv[2]]

        if(onlyLog) {
            log.debug "${input['light']} | Set Color: ${lState.color}"
        } else {
            light.setColor(colorMap)
        }
        setColor = true
    }

    if(lState.level && checkDeviceForCapability(light, "SwitchLevel")) {
        if(onlyLog) {
            log.debug "${input['light']} | Set Level: ${lState.level}"
        } else {
            light.setLevel(lState.level, 0)
        }
    }

    if(lState.temp && checkDeviceForCapability(light, "ColorTemperature") && !setColor) {
        if(onlyLog) {
            log.debug "${input['light']} | Set Color Temp: ${lState.temp}"
        } else {
            light.setColorTemperature(lState.temp)
        }
    }
}

def setLightOff(id) {
    def light = lights.find { it.id == id }

    if(onlyLog) {
        log.debug "${id} | Turn Off"
    } else {
        light.off()
    }
}

def runTopAction() {
    def mainSettings = [level: level, temp: temp, color: color]

    lightsSorted().eachWithIndex{ light, i ->
        def data = getSpecificLightSetting(light.id)

        if(!data) {
            //No Specific Settings for this light
            return setLightStateAsync(mainSettings, light, i)
        }

        if(!data.on) {
            return setLightOffAsync(light, i)
        }

        setLightStateAsync(computeSettings(data, mainSettings), light, 1)
    }
}

def runTimedAction(id) {
    def settings = getTimedActionById(id)
    if(!settings) {
        log.error("Missing Action: ${id}")
        return
    }

    def topSettings = [level: level, temp: temp, color: color]

    if (settings.individualSettings && settings.individualSettings.size()) {
        //Run though each light and apply specific settings
        return lights.eachWithIndex { light, i ->
            def data = settings.individualSettings[light.id]

            if(!data) {
                //No Specific Settings for this light
                return setLightStateAsync(settings, light, i)
            }

            if(!data.on) {
                return setLightOffAsync(light, i);
            }

            setLightStateAsync(computeSettings(data, settings, topSettings), light, i)
        }
    } else {
        //Apply settings to all lights
        setLightingState(computeSettings(settings, topSettings))
    }
}

def getSpecificLightSetting(id) {
    return state.individualSettings[id]
}

def lightsSorted() {
    return lights.sort { it.displayName }
}

def getTimedActionById(id) {
    def setting = state.timedSettings.find { timed ->
        return timed.id == id
    }

    return setting
}

def computeSettings(Map... settingsArray) {
    def settings = [:]

    settingsArray.reverse().each {
        if(it.level != null) {
            settings.level = it.level
        }
        if(it.color != null && it.color != "#000000") {
            settings.color = it.color
        }
        if(it.temp != null) {
            settings.temp = it.temp
        }
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

def snapshotConfiguration() {
    def vals = [
            level: [],
            temp: [],
            color: []
    ]

    // aggregate All Values to set a reasonable top level
    lights.each { l ->
        if(l.currentValue("switch") == "on") {
            if(checkDeviceForCapability(l, "ChangeLevel")) {
                vals.level.push(l.currentValue("level"))
            }
            if(checkDeviceForCapability(l, "ColorControl") && l.currentValue("colorMode") == "RGB") {
                vals.color.push(getColorFromDevice(l))
            } else if(checkDeviceForCapability(l, "ColorTemperature")) {
                vals.temp.push(l.currentValue("colorTemperature"))
            }
        }
    }

    def top = [
            level: getPopularElement(vals.level),
            temp: getPopularElement(vals.temp)
    ]

    // if not all color lights have a color then ignore at top level
    if(color.size() > 0 && color.size() == getDevicesWithCapability("ColorControl").size()) {
        top.color = getPopularElement(vals.color)
    }

    def individual = [:]

    //get any individual settings that stray from the top level
    lights.each { l ->
        if(l.currentValue("switch") == "off") {
            individual[l.id] = [
                    id: l.id,
                    on: false
            ]
        } else {
            def ind = [:]
            if(checkDeviceForCapability(l, "ChangeLevel") && !valuesClose(l.currentValue("level"), top.level, 3)) {
                ind.level = l.currentValue("level")
            }
            if(checkDeviceForCapability(l, "ColorTemperature") && l.currentValue("colorMode") != "RGB" && !valuesClose(l.currentValue("colorTemperature"), top.temp, 150)) {
                ind.temp = l.currentValue("colorTemperature")
            }
            if(checkDeviceForCapability(l, "ColorControl") && l.currentValue("colorMode") == "RGB" && getColorFromDevice(l) != top.color && l.currentValue("color") != '#000000') {
                ind.color = getColorFromDevice(l)
            }

            if (ind.size() > 0) {
                ind.on = true
                ind.id = l.id
                individual[l.id] = ind
            }
        }
    }

    top.individualSettings = individual

    return top
}

def getColorFromDevice(dev) {
    def hsl = [dev.currentValue("hue"), dev.currentValue("saturation"), dev.currentValue("level")]
    def rgb = ColorUtils.hsvToRGB(hsl)
    return ColorUtils.rgbToHEX(rgb)
}

def valuesClose(v1, v2, range) {
    return v1 > v2 - range && v1 < v2 + range
}

def getPopularElement(array) {
    def count = 1
    def tempCount
    def popular = array[0]
    def temp = 0

    for (int i = 0; i < (array.size() - 1); i++) {
        temp = array[i]
        tempCount = 0
        for (int j = 1; j < array.size(); j++)
        {
            if (temp == array[j])
                tempCount++
        }
        if (tempCount > count)
        {
            popular = temp
            count = tempCount
        }
    }
    return popular;
}