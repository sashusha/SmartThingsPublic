metadata {
	// Automatically generated. Make future change here.
	definition (name: "My Thermostat", namespace: "glsapps", author: "Alexander Vladimirov") {
		capability "Actuator"
		capability "Temperature Measurement"
		capability "Relative Humidity Measurement"
		capability "Thermostat"
		capability "Battery"
		capability "Configuration"
		capability "Refresh"
		capability "Sensor"
		
		attribute "thermostatFanState", "string"

		command "switchMode"
		command "switchFanMode"
		command "quickSetCool"
		command "quickSetHeat"
        command "tempUp"
        command "tempDown"
        command "levelUp"
        command "levelDown"

		fingerprint deviceId: "0x08", inClusters: "0x43,0x40,0x44,0x31,0x80,0x85,0x60"
	}

	// simulator metadata
	simulator {
		status "off"            : "command: 4003, payload: 00"
		status "heat"           : "command: 4003, payload: 01"
		status "cool"           : "command: 4003, payload: 02"
		status "auto"           : "command: 4003, payload: 03"
		status "emergencyHeat"  : "command: 4003, payload: 04"

		status "fanAuto"        : "command: 4403, payload: 00"
		status "fanOn"          : "command: 4403, payload: 01"
		status "fanCirculate"   : "command: 4403, payload: 06"

		status "heat 60"        : "command: 4303, payload: 01 09 3C"
		status "heat 72"        : "command: 4303, payload: 01 09 48"

		status "cool 76"        : "command: 4303, payload: 02 09 4C"
		status "cool 80"        : "command: 4303, payload: 02 09 50"

		status "temp 58"        : "command: 3105, payload: 01 2A 02 44"
		status "temp 62"        : "command: 3105, payload: 01 2A 02 6C"
		status "temp 78"        : "command: 3105, payload: 01 2A 03 0C"
		status "temp 86"        : "command: 3105, payload: 01 2A 03 34"

		status "idle"           : "command: 4203, payload: 00"
		status "heating"        : "command: 4203, payload: 01"
		status "cooling"        : "command: 4203, payload: 02"

		// reply messages
		reply "2502": "command: 2503, payload: FF"
	}

	tiles(scale: 2) {
    
    	multiAttributeTile(name:"thermostatFull", type:"thermostat", width:6, height:3) {
            tileAttribute("device.heatingSetpoint", key: "PRIMARY_CONTROL") {
                attributeState "heatingSetpoint", label:'${currentValue}°', defaultState: true, unit: "dF",
                backgroundColors:[
					[value: 31, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
					[value: 95, color: "#d04e00"],
					[value: 96, color: "#bc2323"]
				]
            }

			tileAttribute("device.humidity", key: "SECONDARY_CONTROL", height:2) {
                attributeState "humidity", label:'${currentValue}', unit:"%", defaultState: true
            }
            
            tileAttribute("device.heatingSetpoint", key: "VALUE_CONTROL") {
                attributeState "VALUE_UP", action: "tempUp"
                attributeState "VALUE_DOWN", action: "tempDown"
            }
            
      		// This one is required to display set temperature
			tileAttribute("device.thermostatOperatingState", key: "OPERATING_STATE") {
                attributeState("idle", backgroundColor:"#ffffff")
                attributeState("heating", backgroundColor:"#ffa81e")
                attributeState("cooling", backgroundColor:"#269bd2")
            }
        }
		
        valueTile("modeLabel", "device.thermostatMode", width: 2, height: 2, decoration: "flat") {
            state("thermostateMode", label: "Mode:", defaultState: true)
        }
        valueTile("tempLabel", "device.temperatureLabel", width: 2, height: 2) {
            state("tempLabel", label: 'House Temp', defaultState: true)
        }
        standardTile("refreshLabel", "device.thermostatMode", width: 2, height: 2, decoration: "flat") {
            state("default", label: "refresh")
        }

        standardTile("mode", "device.thermostatMode", width: 2, height: 2, decoration: "flat") {
			state "off", label:'Off', action:"thermostat.heat", backgroundColor:"#ffffff", icon:"st.Home.home1", nextState:"turning on"
			state "heat", label:'On', action:"thermostat.off", backgroundColor:"#44b621", icon:"st.Home.home1", nextState:"turning off"
			state "turning off", label:'${name}', backgroundColor:"#ffffff", icon:"st.Home.home1", nextState:"off"
			state "turning on", label:'${name}', backgroundColor:"#ffa81e", icon:"st.Home.home1", nextState:"heat"
		}
        
		valueTile("battery", "device.battery", decoration: "flat", width: 2, height: 2) {
			state "battery", label:'${currentValue}% battery', unit:""
		}
        
        valueTile("actual", "device.temperature", width:2, height:2, decoration: "flat") {
        	state("default", label:'Temp ${currentValue}°', backgroundColors:[
					[value: 31, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
					[value: 95, color: "#d04e00"],
					[value: 96, color: "#bc2323"]
				])
        }
        
		standardTile("refresh", "device.thermostatMode", width: 2, height: 2, decoration: "flat") {
			state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
                
        controlTile("setpointSlider", "device.heatingSetpoint", "slider", width: 6, height: 1, range: "(55..80)") {
        	state "heatingSetpoint", action: "quickSetHeat"
        }

		main "thermostatFull"
		details(["thermostatFull", "setpointSlider", "mode", "actual", "refresh"])
	}
}

def parse(String description)
{
	def result = []
	if (description == "updated") {
	} else {
		def zwcmd = zwave.parse(description, [0x42:2, 0x43:2, 0x31: 2, 0x60: 3])
		if (zwcmd) {
			result += zwaveEvent(zwcmd)
		} else {
			log.debug "$device.displayName couldn't parse $description"
		}
	}
	if (!result) {
		return null
	}
	if (result.size() == 1 && (!state.lastbatt || now() - state.lastbatt > 48*60*60*1000)) {
		result << response(zwave.batteryV1.batteryGet().format())
	}
	log.debug "$device.displayName parsed '$description' to $result"
	result
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
	def result = null
	def encapsulatedCommand = cmd.encapsulatedCommand([0x42:2, 0x43:2, 0x31: 2])
	log.debug ("Command from endpoint ${cmd.sourceEndPoint}: ${encapsulatedCommand}")
	if (encapsulatedCommand) {
		result = zwaveEvent(encapsulatedCommand)
		if (cmd.sourceEndPoint == 1) {    // indicates a response to refresh() vs an unrequested update
			def event = ([] + result)[0]  // in case zwaveEvent returns a list
			def resp = nextRefreshQuery(event?.name)
			if (resp) {
				log.debug("sending next refresh query: $resp")
				result = [] + result + response(["delay 200", resp])
			}
		}
	}
	result
}

def zwaveEvent(physicalgraph.zwave.commands.thermostatsetpointv2.ThermostatSetpointReport cmd)
{
	def cmdScale = cmd.scale == 1 ? "F" : "C"
	def temp = convertTemperatureIfNeeded(cmd.scaledValue, cmdScale, cmd.precision)
	def unit = getTemperatureScale()
	def map1 = [ value: temp, unit: unit, displayed: false ]
	switch (cmd.setpointType) {
		case 1:
			map1.name = "heatingSetpoint"
			break;
		case 2:
			map1.name = "coolingSetpoint"
			break;
		default:
			log.debug "unknown setpointType $cmd.setpointType"
			return
	}

	// So we can respond with same format
	state.size = cmd.size
	state.scale = cmd.scale
	state.precision = cmd.precision

	def mode = device.latestValue("thermostatMode")
	if (mode && map1.name.startsWith(mode) || (mode == "emergency heat" && map1.name == "heatingSetpoint")) {
		def map2 = [ name: "thermostatSetpoint", value: temp, unit: unit ]
		[ createEvent(map1), createEvent(map2) ]
	} else {
		createEvent(map1)
	}
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv2.SensorMultilevelReport cmd)
{
	def map = [:]
	if (cmd.sensorType == 1) {
		map.name = "temperature"
		map.unit = getTemperatureScale()
		map.value = convertTemperatureIfNeeded(cmd.scaledSensorValue, cmd.scale == 1 ? "F" : "C", cmd.precision)
	} else if (cmd.sensorType == 5) {
		map.name = "humidity"
		map.unit = "%"
		map.value = cmd.scaledSensorValue
	}
	createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.thermostatoperatingstatev2.ThermostatOperatingStateReport cmd)
{
	def map = [name: "thermostatOperatingState" ]
	switch (cmd.operatingState) {
		case physicalgraph.zwave.commands.thermostatoperatingstatev2.ThermostatOperatingStateReport.OPERATING_STATE_IDLE:
			map.value = "idle"
			break
		case physicalgraph.zwave.commands.thermostatoperatingstatev2.ThermostatOperatingStateReport.OPERATING_STATE_HEATING:
			map.value = "heating"
			break
		case physicalgraph.zwave.commands.thermostatoperatingstatev2.ThermostatOperatingStateReport.OPERATING_STATE_COOLING:
			map.value = "cooling"
			break
		case physicalgraph.zwave.commands.thermostatoperatingstatev2.ThermostatOperatingStateReport.OPERATING_STATE_FAN_ONLY:
			map.value = "fan only"
			break
		case physicalgraph.zwave.commands.thermostatoperatingstatev2.ThermostatOperatingStateReport.OPERATING_STATE_PENDING_HEAT:
			map.value = "pending heat"
			break
		case physicalgraph.zwave.commands.thermostatoperatingstatev2.ThermostatOperatingStateReport.OPERATING_STATE_PENDING_COOL:
			map.value = "pending cool"
			break
		case physicalgraph.zwave.commands.thermostatoperatingstatev2.ThermostatOperatingStateReport.OPERATING_STATE_VENT_ECONOMIZER:
			map.value = "vent economizer"
			break
	}
	def result = createEvent(map)
	if (result.isStateChange && device.latestValue("thermostatMode") == "auto" && (result.value == "heating" || result.value == "cooling")) {
		def thermostatSetpoint = device.latestValue("${result.value}Setpoint")
		result = [result, createEvent(name: "thermostatSetpoint", value: thermostatSetpoint, unit: getTemperatureScale())]
	}
	result
}

def zwaveEvent(physicalgraph.zwave.commands.thermostatfanstatev1.ThermostatFanStateReport cmd) {
	def map = [name: "thermostatFanState", unit: ""]
	switch (cmd.fanOperatingState) {
		case 0:
			map.value = "idle"
			break
		case 1:
			map.value = "running"
			break
		case 2:
			map.value = "running high"
			break
	}
	createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.thermostatmodev2.ThermostatModeReport cmd) {
	def map = [name: "thermostatMode"]
	def thermostatSetpoint = null
	switch (cmd.mode) {
		case physicalgraph.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_OFF:
			map.value = "off"
			break
		case physicalgraph.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_HEAT:
			map.value = "heat"
			thermostatSetpoint = device.latestValue("heatingSetpoint")
			break
		case physicalgraph.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_AUXILIARY_HEAT:
			map.value = "emergency heat"
			thermostatSetpoint = device.latestValue("heatingSetpoint")
			break
		case physicalgraph.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_COOL:
			map.value = "cool"
			thermostatSetpoint = device.latestValue("coolingSetpoint")
			break
		case physicalgraph.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_AUTO:
			map.value = "auto"
			def temp = device.latestValue("temperature")
			def heatingSetpoint = device.latestValue("heatingSetpoint")
			def coolingSetpoint = device.latestValue("coolingSetpoint")
			if (temp && heatingSetpoint && coolingSetpoint) {
				if (temp < (heatingSetpoint + coolingSetpoint) / 2.0) {
					thermostatSetpoint = heatingSetpoint
				} else {
					thermostatSetpoint = coolingSetpoint
				}
			}
			break
	}
	state.lastTriedMode = map.value
	if (thermostatSetpoint) {
		[ createEvent(map), createEvent(name: "thermostatSetpoint", value: thermostatSetpoint, unit: getTemperatureScale()) ]
	} else {
		createEvent(map)
	}
}

def zwaveEvent(physicalgraph.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport cmd) {
	def map = [name: "thermostatFanMode", displayed: false]
	switch (cmd.fanMode) {
		case physicalgraph.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport.FAN_MODE_AUTO_LOW:
			map.value = "fanAuto"
			break
		case physicalgraph.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport.FAN_MODE_LOW:
			map.value = "fanOn"
			break
		case physicalgraph.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport.FAN_MODE_CIRCULATION:
			map.value = "fanCirculate"
			break
	}
	state.lastTriedFanMode = map.value
	createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.thermostatmodev2.ThermostatModeSupportedReport cmd) {
	def supportedModes = ""
	if(cmd.off) { supportedModes += "off " }
	if(cmd.heat) { supportedModes += "heat " }
	if(cmd.auxiliaryemergencyHeat) { supportedModes += "emergency heat " }
	if(cmd.cool) { supportedModes += "cool " }
	if(cmd.auto) { supportedModes += "auto " }

	state.supportedModes = supportedModes
	[ createEvent(name:"supportedModes", value: supportedModes, displayed: false),
	  response(zwave.thermostatFanModeV3.thermostatFanModeSupportedGet()) ]
}

def zwaveEvent(physicalgraph.zwave.commands.thermostatfanmodev3.ThermostatFanModeSupportedReport cmd) {
	def supportedFanModes = ""
	if(cmd.auto) { supportedFanModes += "fanAuto " }
	if(cmd.low) { supportedFanModes += "fanOn " }
	if(cmd.circulation) { supportedFanModes += "fanCirculate " }

	state.supportedFanModes = supportedFanModes
	[ createEvent(name:"supportedFanModes", value: supportedModes, displayed: false),
	  response(refresh()) ]
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
	log.debug "Zwave event received: $cmd"
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
	def map = [ name: "battery", unit: "%" ]
	if (cmd.batteryLevel == 0xFF) {
		map.value = 1
		map.descriptionText = "${device.displayName} battery is low"
		map.isStateChange = true
	} else {
		map.value = cmd.batteryLevel
	}
	state.lastbatt = now()
	createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	log.warn "Unexpected zwave command $cmd"
}

def refresh() {
	// Use encapsulation to differentiate refresh cmds from what the thermostat sends proactively on change
	def cmd = zwave.sensorMultilevelV2.sensorMultilevelGet()
	zwave.multiChannelV3.multiChannelCmdEncap(destinationEndPoint:1).encapsulate(cmd).format()
}

def nextRefreshQuery(name) {
	def cmd = null
	switch (name) {
		case "temperature":
			cmd = zwave.thermostatModeV2.thermostatModeGet()
			break
		case "thermostatMode":
			cmd = zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 1)
			break
		case "heatingSetpoint":
			cmd = zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 2)
			break
		case "coolingSetpoint":
			cmd = zwave.thermostatFanModeV3.thermostatFanModeGet()
			break
		case "thermostatFanMode":
			cmd = zwave.thermostatOperatingStateV2.thermostatOperatingStateGet()
			break
		case "thermostatOperatingState":
			// get humidity, multilevel sensor get to endpoint 2
			cmd = zwave.sensorMultilevelV2.sensorMultilevelGet()
			return zwave.multiChannelV3.multiChannelCmdEncap(destinationEndPoint:2).encapsulate(cmd).format()
		default: return null
	}
	zwave.multiChannelV3.multiChannelCmdEncap(destinationEndPoint:1).encapsulate(cmd).format()
}

def quickSetHeat(degrees) {
	setHeatingSetpoint(degrees, 300)
}

def tempUp() {
	quickSetHeat(device.latestValue("heatingSetpoint") + 1)
}

def tempDown() {
	quickSetHeat(device.latestValue("heatingSetpoint") - 1)
}

def setHeatingSetpoint(degrees, delay = 30000) {
	setHeatingSetpoint(degrees.toDouble(), delay)
}

def setHeatingSetpoint(Double degrees, Integer delay = 30000) {
	log.trace "setHeatingSetpoint($degrees, $delay)"
	def deviceScale = state.scale ?: 1
	def deviceScaleString = deviceScale == 2 ? "C" : "F"
	def locationScale = getTemperatureScale()
	def p = (state.precision == null) ? 1 : state.precision

	def convertedDegrees
	if (locationScale == "C" && deviceScaleString == "F") {
		convertedDegrees = celsiusToFahrenheit(degrees)
	} else if (locationScale == "F" && deviceScaleString == "C") {
		convertedDegrees = fahrenheitToCelsius(degrees)
	} else {
		convertedDegrees = degrees
	}
	
    sendEvent(name: "heatingSetpoint", value: convertedDegrees)

	delayBetween([
		zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 1, scale: deviceScale, precision: p, scaledValue: convertedDegrees).format(),
		zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 1).format()
	], delay)
}

def quickSetCool(degrees) {
	setCoolingSetpoint(degrees, 1000)
}

def setCoolingSetpoint(degrees, delay = 30000) {
	setCoolingSetpoint(degrees.toDouble(), delay)
}

def setCoolingSetpoint(Double degrees, Integer delay = 30000) {
	log.trace "setCoolingSetpoint($degrees, $delay)"
	def deviceScale = state.scale ?: 1
	def deviceScaleString = deviceScale == 2 ? "C" : "F"
	def locationScale = getTemperatureScale()
	def p = (state.precision == null) ? 1 : state.precision

	def convertedDegrees
	if (locationScale == "C" && deviceScaleString == "F") {
		convertedDegrees = celsiusToFahrenheit(degrees)
	} else if (locationScale == "F" && deviceScaleString == "C") {
		convertedDegrees = fahrenheitToCelsius(degrees)
	} else {
		convertedDegrees = degrees
	}

	delayBetween([
		zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 2, scale: deviceScale, precision: p,	 scaledValue: convertedDegrees).format(),
		zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 2).format()
	], delay)
}

def configure() {
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSupportedGet().format(),
	], 2300)
}

def modes() {
	["off", "heat", "cool", "auto", "emergency heat"]
}

def switchMode() {
	def currentMode = device.currentState("thermostatMode")?.value
	def lastTriedMode = state.lastTriedMode ?: currentMode ?: "off"
	def supportedModes = getDataByName("supportedModes")
	def modeOrder = modes()
	def next = { modeOrder[modeOrder.indexOf(it) + 1] ?: modeOrder[0] }
	def nextMode = next(lastTriedMode)
	if (supportedModes?.contains(currentMode)) {
		while (!supportedModes.contains(nextMode) && nextMode != "off") {
			nextMode = next(nextMode)
		}
	}
	state.lastTriedMode = nextMode
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: modeMap[nextMode]).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], 1000)
}

def switchToMode(nextMode) {
	def supportedModes = getDataByName("supportedModes")
	if(supportedModes && !supportedModes.contains(nextMode)) log.warn "thermostat mode '$nextMode' is not supported"
	if (nextMode in modes()) {
		state.lastTriedMode = nextMode
		"$nextMode"()
	} else {
		log.debug("no mode method '$nextMode'")
	}
}

def switchFanMode() {
	def currentMode = device.currentState("thermostatFanMode")?.value
	def lastTriedMode = state.lastTriedFanMode ?: currentMode ?: "off"
	def supportedModes = getDataByName("supportedFanModes") ?: "fanAuto fanOn"
	def modeOrder = ["fanAuto", "fanCirculate", "fanOn"]
	def next = { modeOrder[modeOrder.indexOf(it) + 1] ?: modeOrder[0] }
	def nextMode = next(lastTriedMode)
	while (!supportedModes?.contains(nextMode) && nextMode != "fanAuto") {
		nextMode = next(nextMode)
	}
	switchToFanMode(nextMode)
}

def switchToFanMode(nextMode) {
	def supportedFanModes = getDataByName("supportedFanModes")
	if(supportedFanModes && !supportedFanModes.contains(nextMode)) log.warn "thermostat mode '$nextMode' is not supported"

	def returnCommand
	if (nextMode == "fanAuto") {
		returnCommand = fanAuto()
	} else if (nextMode == "fanOn") {
		returnCommand = fanOn()
	} else if (nextMode == "fanCirculate") {
		returnCommand = fanCirculate()
	} else {
		log.debug("no fan mode '$nextMode'")
	}
	if(returnCommand) state.lastTriedFanMode = nextMode
	returnCommand
}

def getDataByName(String name) {
	state[name] ?: device.getDataValue(name)
}

def getModeMap() { [
	"off": 0,
	"heat": 1,
	"cool": 2,
	"auto": 3,
	"emergency heat": 4
]}

def setThermostatMode(String value) {
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: modeMap[value]).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], standardDelay)
}

def getFanModeMap() { [
	"auto": 0,
	"on": 1,
	"circulate": 6
]}

def setThermostatFanMode(String value) {
	delayBetween([
		zwave.thermostatFanModeV3.thermostatFanModeSet(fanMode: fanModeMap[value]).format(),
		zwave.thermostatFanModeV3.thermostatFanModeGet().format()
	], standardDelay)
}

def off() {
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 0).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], standardDelay)
}

def heat() {
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 1).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], standardDelay)
}

def emergencyHeat() {
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 4).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], standardDelay)
}

def cool() {
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 2).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], standardDelay)
}

def auto() {
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 3).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], standardDelay)
}

def fanOn() {
	delayBetween([
		zwave.thermostatFanModeV3.thermostatFanModeSet(fanMode: 1).format(),
		zwave.thermostatFanModeV3.thermostatFanModeGet().format()
	], standardDelay)
}

def fanAuto() {
	delayBetween([
		zwave.thermostatFanModeV3.thermostatFanModeSet(fanMode: 0).format(),
		zwave.thermostatFanModeV3.thermostatFanModeGet().format()
	], standardDelay)
}

def fanCirculate() {
	delayBetween([
		zwave.thermostatFanModeV3.thermostatFanModeSet(fanMode: 6).format(),
		zwave.thermostatFanModeV3.thermostatFanModeGet().format()
	], standardDelay)
}

private getStandardDelay() {
	1000
}