/**
 *  ISY Controller
 *
 *  Copyright 2014 Richard L. Lynch <rich@richlynch.com>
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

metadata {
    definition (name: "ISY Thermostat", namespace: "isy", author: "Alex Palmer") {
        capability "Thermostat"
        capability "Polling"
        capability "Refresh"
        capability "relativeHumidityMeasurement"
        
        
        command "fanOn"
        command "fanAuto"
        command "setpointUp"
        command "setpointDown"
        //command "setThermostatMode"
    }

    simulator {
    }
	
   tiles {
        valueTile("frontTile", "device.temperature", width: 1, height: 1) {
            state("temperature", label:'${currentValue}°', backgroundColor:"#e8e3d8")
        }
    
        valueTile("temperature", "device.temperature", width: 1, height: 1) {
            state("temperature", label:'${currentValue}°', backgroundColor:"#0A1E2C")
        }
        valueTile("humidity", "device.humidity", width: 1, height: 1) {
            state("humidity", label:'${currentValue}%', backgroundColor:"#0A1E2C")
        }
        
        standardTile("fanMode", "device.thermostatFanMode", decoration: "flat") {
            state "Auto", action:"fanOn", backgroundColor:"#e8e3d8", icon:"st.thermostat.fan-auto"
            state "On", action:"fanAuto", backgroundColor:"#e8e3d8", icon:"st.thermostat.fan-on"
        }
        
        
        standardTile("thermostatMode", "device.thermostatMode", decoration: "flat") {
            state "off", action:"thermostat.heat", backgroundColor:"#e8e3d8", icon:"st.thermostat.heating-cooling-off", nextState:"heat"
            state "heat", action:"thermostat.cool", backgroundColor:"#ff6e7e", icon:"st.thermostat.heat", nextState:"cool"
            state "cool", action:"thermostat.auto", backgroundColor:"#90d0e8", icon:"st.thermostat.cool", nextState:"auto"
            state "auto", action:"thermostat.off", backgroundColor:"#e8e3d8", icon:"st.thermostat.auto", nextState:"off"
            
        }
        
        valueTile("thermostatSetpoint", "device.thermostatSetpoint", width: 2, height: 2) {
            state "off", label:'${currentValue}°', unit: "C", backgroundColor:"#e8e3d8"
            state "heat", label:'${currentValue}°', unit: "C", backgroundColor:"#e8e3d8"
            state "cool", label:'${currentValue}°', unit: "C", backgroundColor:"#e8e3d8"
        }
        valueTile("heatingSetpoint", "device.heatingSetpoint", inactiveLabel: false) {
			state "heat", label:'${currentValue}° heat', unit:"F", backgroundColor:"#ffffff"
		}
        valueTile("coolingSetpoint", "device.coolingSetpoint", inactiveLabel: false) {
			state "cool", label:'${currentValue}° cool', unit:"F", backgroundColor:"#ffffff"
		}
        standardTile("thermostatOperatingState", "device.thermostatOperatingState", inactiveLabel: false) {
            state "heating", backgroundColor:"#ff6e7e"
            state "cooling", backgroundColor:"#90d0e8"
            state "fan only", backgroundColor:"#e8e3d8"
		}
        standardTile("setpointUp", "device.thermostatSetpoint", decoration: "flat") {
            state "setpointUp", action:"setpointUp", icon:"st.thermostat.thermostat-up"
        }
        
        standardTile("setpointDown", "device.thermostatSetpoint", decoration: "flat") {
            state "setpointDown", action:"setpointDown", icon:"st.thermostat.thermostat-down"
        }

        standardTile("refresh", "device.temperature", decoration: "flat") {
            state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        
        
      main "frontTile"
      details(["temperature","humidity", "fanMode", "thermostatMode", "heatingSetpoint", "coolingSetpoint", "setpointUp", "setpointDown","refresh"])
  }
}



// parse events into attributes
def parse(String description) {
    log.debug "Parsing Dev ${device.deviceNetworkId} '${description}'"

}

private Integer convertHexToInt(hex) {
    Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
    [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private getHostAddress() {
    def ip = getDataValue("ip")
    def port = getDataValue("port")

    //convert IP/port
    ip = convertHexToIP(ip)
    port = convertHexToInt(port)
    //log.debug "Using ip: ${ip} and port: ${port} for device: ${device.id}"
    return ip + ":" + port
}

private getAuthorization() {
    //log.debug("Using " + getDataValue("username") + ":" + getDataValue("password"))
    def userpassascii = getDataValue("username") + ":" + getDataValue("password")
    "Basic " + userpassascii.encodeAsBase64().toString()
}

def getRequest(path) {
    log.debug "Sending request for ${path} from ${device.deviceNetworkId}"
    new physicalgraph.device.HubAction(
        'method': 'GET',
        'path': path,
        'headers': [
            'HOST': getHostAddress(),
            'Authorization': getAuthorization()
        ], device.deviceNetworkId)
}

// handle commands

def setpointUp() {
    log.debug "Executing setpointUP"
    def newHsp = ''
    def oldHsp = ''
    
    oldHsp = device.currentValue("heatingSetpoint")
    
    newHsp = oldHsp + 1
    sendEvent(name: 'heatingSetpoint', value: newHsp)
    newHsp = newHsp*2

    
    def node = getDataValue("nodeAddr").replaceAll(" ", "%20")
    def path = "/rest/nodes/${node}/cmd/CLISPH/${newHsp}"
    getRequest(path)
    
    def newCsp = ''
    def oldCsp = ''
    
    oldCsp = device.currentValue("coolingSetpoint")
    
    newCsp = oldCsp + 1
    sendEvent(name: 'coolingSetpoint', value: newCsp)
    newCsp = newCsp*2

    
    node = getDataValue("nodeAddr").replaceAll(" ", "%20")
    path = "/rest/nodes/${node}/cmd/CLISPC/${newCsp}"
    getRequest(path)
    
    
}

def setpointDown() {
    log.debug "Executing setpointDown"
    def newHsp = ''
    def oldHsp = ''
    
    oldHsp = device.currentValue("heatingSetpoint")
    
    newHsp = oldHsp - 1
    sendEvent(name: 'heatingSetpoint', value: newHsp)
    newHsp = newHsp*2

    
    def node = getDataValue("nodeAddr").replaceAll(" ", "%20")
    def path = "/rest/nodes/${node}/cmd/CLISPH/${newHsp}"
    getRequest(path)
    
    def newCsp = ''
    def oldCsp = ''
    
    oldCsp = device.currentValue("coolingSetpoint")
    
    newCsp = oldCsp - 1
    sendEvent(name: 'coolingSetpoint', value: newCsp)
    newCsp = newCsp*2

    
    node = getDataValue("nodeAddr").replaceAll(" ", "%20")
    path = "/rest/nodes/${node}/cmd/CLISPC/${newCsp}"
    getRequest(path)
     
}

def fanAuto() {
    log.debug "Executing Fan-Auto"
    
    sendEvent(name: 'thermostatFanMode', value: 'Auto')
    
    def node = getDataValue("nodeAddr").replaceAll(" ", "%20")
    def path = "/rest/nodes/${node}/cmd/CLIFS/8"
    getRequest(path)
   }
   
def fanOn() {
    log.debug "Executing Fan-On"
    
    sendEvent(name: 'thermostatFanMode', value: 'On')
    
    def node = getDataValue("nodeAddr").replaceAll(" ", "%20")
    def path = "/rest/nodes/${node}/cmd/CLIFS/7"
    getRequest(path)
   }

def heat(){
	log.debug "Changing to heat"
    sendEvent(name: 'thermostatMode', value: 'heat')
    
    def node = getDataValue("nodeAddr").replaceAll(" ", "%20")
    def path = "/rest/nodes/${node}/cmd/CLIMD/1"
    getRequest(path)
}
def cool(){
	log.debug "Changing to cool"
    sendEvent(name: 'thermostatMode', value: 'cool')
    
    def node = getDataValue("nodeAddr").replaceAll(" ", "%20")
    def path = "/rest/nodes/${node}/cmd/CLIMD/2"
    getRequest(path)
}

def auto(){
	log.debug "Changing to auto"
    sendEvent(name: 'thermostatMode', value: 'auto')
    
    def node = getDataValue("nodeAddr").replaceAll(" ", "%20")
    def path = "/rest/nodes/${node}/cmd/CLIMD/3"
    getRequest(path)
}

def off(){
	log.debug "Changing to off"
    sendEvent(name: 'thermostatMode', value: 'off')
    
    def node = getDataValue("nodeAddr").replaceAll(" ", "%20")
    def path = "/rest/nodes/${node}/cmd/CLIMD/0"
    getRequest(path)
}


def poll() {
    if (!device.deviceNetworkId.contains(':')) {
        log.debug "Executing 'poll' from ${device.deviceNetworkId}"

        def path = "/rest/status"
        getRequest(path)
    }
    else {
        log.debug "Ignoring poll request for ${device.deviceNetworkId}"
    }
}

def refresh() {
    log.debug "Executing 'refresh'"

    def path = "/rest/status/"
    def result = new physicalgraph.device.HubAction(
        'method': 'GET',
        'path': path,
        'headers': [
            'HOST': getHostAddress(),
            'Authorization': getAuthorization()
        ], device.deviceNetworkId)
     log.debug result
}

private def parseDiscoveryMessage(String description) {
    def device = [:]
    def parts = description.split(',')
    parts.each { part ->
        part = part.trim()
        if (part.startsWith('devicetype:')) {
            def valueString = part.split(":")[1].trim()
            device.devicetype = valueString
        } else if (part.startsWith('mac:')) {
            def valueString = part.split(":")[1].trim()
            if (valueString) {
                device.mac = valueString
            }
        } else if (part.startsWith('networkAddress:')) {
            def valueString = part.split(":")[1].trim()
            if (valueString) {
                device.ip = valueString
            }
        } else if (part.startsWith('deviceAddress:')) {
            def valueString = part.split(":")[1].trim()
            if (valueString) {
                device.port = valueString
            }
        } else if (part.startsWith('ssdpPath:')) {
            def valueString = part.split(":")[1].trim()
            if (valueString) {
                device.ssdpPath = valueString
            }
        } else if (part.startsWith('ssdpUSN:')) {
            part -= "ssdpUSN:"
            def valueString = part.trim()
            if (valueString) {
                device.ssdpUSN = valueString
            }
        } else if (part.startsWith('ssdpTerm:')) {
            part -= "ssdpTerm:"
            def valueString = part.trim()
            if (valueString) {
                device.ssdpTerm = valueString
            }
        } else if (part.startsWith('headers')) {
            part -= "headers:"
            def valueString = part.trim()
            if (valueString) {
                device.headers = valueString
            }
        } else if (part.startsWith('body')) {
            part -= "body:"
            def valueString = part.trim()
            if (valueString) {
                device.body = valueString
            }
        }
    }

    device
}