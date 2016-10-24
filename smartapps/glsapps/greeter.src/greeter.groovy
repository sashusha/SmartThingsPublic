/**
 *  Greeter
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
    name: "Greeter",
    namespace: "glsapps",
    author: "Alexander Vladimirov",
    description: "Does things when somebody comes home",
    category: "Family",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Settings") {
    	input "light", "capability.switch", title:"Light to turn on/off", multiple: false, required: true
        input "presenceSensors", "capability.presenceSensor", title:"Presence sensors", multiple: true, required: true
        input "absenceDuration", "number", title:"Absence duration (minutes)", range:"0..*", required: true, default:"30"
        input "sunsetOffset", "number", title:"Sunset offset (minutes)", range:"0..*", required: true, default: 15
        input "turnOffDelay", "number", title:"Turn off after (minutes)", range:"1..30", required: true, default: 5
	}
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
	subscribe(presenceSensors, "presence.present", presentHandler);
}

def presentHandler(event) {
	log.trace "presentHandler"
	if (!event.isStateChange()) {
    	log.trace "not a state change. returning"
    	return
    }
    
    if (wasAbsenceTooShort(event) || isBeforeSunset()) {
    	log.trace "Absence was too short or before sunset. returning"
        return
    }
    light.on()
    runIn(turnOffDelay * 60, turnLightOff)
}

def turnLightOff() {
	light.off()
}

private wasAbsenceTooShort(event) {
	def lastThree = event.device.events(max:3)
    if (lastThree.size() < 2) {
    	log.trace "wasAbsenceTooShort: less than 2 events"
    	return false
    }
    if (lastThree[0].date.equals(event.date)) {
    	log.trace "wasAbsenceTooShort event.date: ${event.date} previous: ${lastThree[1].date}"
    	def previousTs = lastThree[1].date.getTime();
        return (event.date.getTime() - previousTs) < (absenceDuration * 60000)
    }
    
    return false
}

private isBeforeSunset() {
    def ts = now()
    log.trace "Now: ${new Date(ts)}"
    def sunsetWithOffset = getSunriseAndSunset(sunsetOffset: -sunsetOffset).sunset
    log.trace "Sunset with offset: ${sunsetWithOffset}"
	return ts < sunsetWithOffset.getTime()
}

