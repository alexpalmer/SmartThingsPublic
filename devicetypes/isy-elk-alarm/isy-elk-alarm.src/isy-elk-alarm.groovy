/**
 *  ISY Controller
 *
 * 
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
 /*
 1, Alarm State
	0, No Alarm Active
	1, Entrance Delay
	2, Alarm Abort Delay
	3, Fire Alarm
	4, Medical
	5, Police
	6, Burglar
2, Arm Up State
	0, Not Ready
	1, Ready to Arm
	2, Ready to Arm w/ Zone Voilated
	3, Armed with Exit Timer
	4, Armed Fully
	5, Forced Armed w/Zone Voilated
	6, Armed with Bypass
3, Armed State
	0, Disarmed
	1, Armed Away
	2, Armed Stay
	3, Armed Stay Instant
	4, Armed Night
	5, Armed Night Instant
	6, Armed Vacation
 */

metadata {
    definition (name: "ISY ELK Alarm", namespace: "isy-elk-alarm", author: "Alex Palmer") {
        //capability "Contact Sensor"
        capability "Polling"
        capability "Refresh"
        //capability "Actuator"
        attribute "AlarmState", "string"
        attribute "ArmUpState", "string"
        attribute "ArmedState", "string"
        command "updateNA"
        command "Disarm"
        command "ArmAway"
        command "ArmNight"
        
    }

    simulator {
    }

    tiles {
        standardTile("Mode", "device.ArmedState", decoration: "flat", canChangeBackground: true, width: 2, height: 2) {
            state "Disarmed", label:'Disarmed', icon:"st.Home.home2", backgroundColor:"#44b621"
            state "Armed Stay", label:'Armed Stay', icon:"st.Home.home3", backgroundColor:"#ffffff"
            state "Armed Away", label:'Armed Away', icon:"st.Home.home", backgroundColor:"#ffffff"
             state "Armed Night", label:'Armed Night', icon:"st.Home.home", backgroundColor:"#ffffff"
        }
        valueTile("AlarmState", "device.AlarmState", canChangeBackground: true, width: 3, height: 1) {
            state "No Alarm Active", label:'No Alarm Active', backgroundColor:"#44b621"
            state "Burlgar Alarm", label:'Burglar Alarm', backgroundColor:"#ffffff"
         }
         standardTile("Arm", "device.ArmUpState", width: 1, height: 1){
         	state "default", label:'Arm Away', action: "ArmAway"
          }
          standardTile("Disarm", "device.ArmUpState", width: 1, height: 1){
         	state "default", label:'Disarm', action: "Disarm", backgroundColor:"#44b621"
          }
        
        
        standardTile("refresh", "device.contact", inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        
        standardTile("update", "device.contact", inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"updateNA", icon:"st.Electronics.electronics6"
        }
        
        
        main "Mode"
        details (["Mode", "AlarmState", "update", "refresh", "Arm", "Disarm"])
    }
}

// parse events into attributes
def parse(String description) {
    //Handled in ISY Switch
}

def updateNA() {
	log.debug "Running Update NA"
    def str = device.deviceNetworkId.split(':')
    log.debug str[1]
    device.updateDataValue("nodeAddr", str[1])
    device.updateDataValue("port",'0050')
    device.updateDataValue("ip",'C0A81203')
}

private Integer convertHexToInt(hex) {
    Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
    [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private getHostAddress() {
    def ip = 'C0A81003'
    def port = '0050'

    //convert IP/port
    ip = convertHexToIP(ip)
    port = convertHexToInt(port)
    log.debug "Using ip: ${ip} and port: ${port} for device: ${device.id}"
    return ip + ":" + port
}

private getAuthorization() {
    def userpassascii = 'admin' + ":" + 'admin'
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



def poll() {
    if (!device.deviceNetworkId.contains(':')) {
        log.debug "Executing 'poll' from ${device.deviceNetworkId}"

        def path = "/rest/elk/area/1/get/status"
        getRequest(path)
    }
    else {
        log.debug "Ignoring poll request for ${device.deviceNetworkId}"
    }
}

def refresh() {
    log.debug "Executing 'refresh'"

    def path = "/rest/elk/area/1/get/status"
    getRequest(path)
}

def Disarm() {
    log.debug "Executing 'Disarm'"

    def path = "/rest/elk/area/1/cmd/disarm?code=0927"
    getRequest(path)
}

def ArmAway() {
    log.debug "Executing 'Disarm'"

    def path = "/rest/elk/area/1/cmd/arm?armType=1&code=0927"
    getRequest(path)
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