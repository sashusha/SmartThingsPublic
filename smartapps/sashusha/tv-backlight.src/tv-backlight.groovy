/**
 *  End TV
 *
 *  Copyright 2016 Alexander Vladimirov
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
    name: "TV Backlight",
    namespace: "sashusha",
    author: "Alexander Vladimirov",
    description: "Turns TV backlights on/off when TV is used/not used",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    singleInstance: true)


preferences {
	section("Settings") {
    	icon(title: "Pick an icon")
//    	input "harmony", "capability.mediaController", title: "Harmony Hub", multiple: false
        input "tvLights", "device.hueBulb", title: "TV Backlight", multiple: false
        input "delay", "number", title:"Turn Off Delay (seconds)", range:"0..*", defaultValue:"300", required: false
        input "temperature", "number", title:"Backlight color temperature (Kelvin)", range:"1500..*", defaultValue:"4000", required: false
        input "level", "number", title:"Backlight level (%)", range:"0..100", defaultValue:"75", required: false
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def uninstalled() {
  	getChildDevices().each {
		deleteChildDevice(it.deviceNetworkId)
  	}
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
//    subscribe(harmony, "currentActivity", activityChangeHandler)    
    try {
        def existingDevice = getChildDevice("foo")
        if (!existingDevice) {
            addVirtualSwitch()
        }
    } catch (e) {
        log.error "Error creating device: ${e}"
    }
}

def on(device) {
	state.activity = true
    turnOnBacklight()
}

def off(device) {
	state.activity = false
    runIn(delay, conditionallyTurnOffBacklight)
}

private addVirtualSwitch() {
	addChildDevice("glsapps", "Virtual Switch", "foo", location.hubs[0].id, [label:"Virtual TV Backlight", name:"Virtual Switch"])
}

def activityChangeHandler(evt) {
    log.trace "activityChangeHandler: ${evt.value}"
    if (evt.value == "--") {
      state.activity = false
      runIn(delay, conditionallyTurnOffBacklight)
    } else {
      state.activity = true
      turnOnBacklight()
    }
}

def turnOnBacklight() {	
	tvLights.setColorTemperature(temperature)
    tvLights.setLevel(level)
}

def conditionallyTurnOffBacklight() {
	if (!state.activity) {
    	tvLights.off()
    }
}