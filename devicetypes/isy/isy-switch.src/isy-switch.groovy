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
    definition (name: "ISY Switch", namespace: "isy", author: "Richard L. Lynch") {
        capability "Switch"
        capability "Polling"
        capability "Refresh"
        capability "Actuator"
        command "updateNA"
    }

    simulator {
    }

    tiles {
        standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
            state "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"off"
            state "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"on"
        }
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        standardTile("update", "device.contact", inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"updateNA", icon:"st.Electronics.electronics6"
        }

        main "switch"
        details (["switch", "refresh","update"])
    }
}

def updateNA() {
	log.debug "Running Update NA"
    def str = device.deviceNetworkId.split(':')
    log.debug str[1]
    device.updateDataValue("nodeAddr", str[1])
    device.updateDataValue("port",'0050')
    device.updateDataValue("ip",'C0A81003')
}

// parse events into attributes
def parse(String description) {
    log.debug "Parsing Dev ${device.deviceNetworkId} '${description}'"

    def parsedEvent = parseDiscoveryMessage(description)
    //log.debug "Parsed event: " + parsedEvent
    //log.debug "Body: " + parsedEvent['body']
    if (parsedEvent['body'] != null) {
        def xmlText = new String(parsedEvent.body.decodeBase64())
        log.debug 'Device Type Decoded body: ' + xmlText

        def xmlTop = new XmlSlurper().parseText(xmlText)
        def nodes = xmlTop.node
        
              
        //log.debug 'Nodes: ' + nodes.name()
        if (xmlText.contains('node')) {
        log.debug 'Node Running'
        def childMap = [:]
        parent.getChildDevices(false).each { child ->
            def childNodeAddr = child.getDataValue("nodeAddr")
            childMap[childNodeAddr] = child
            log.debug "Adding Child " + child + "ID: " + childNodeAddr
        }
        nodes.each { node ->
            def nodeAddr = node.attributes().id
            def status = ''
            def formatted = ''
            def humidity = ''
            def heatSpt = ''
            def coolSpt = ''
            def tstatMd= ''

            node.property.each { prop ->
                if (prop.attributes().id == 'ST') {
                    status = prop.attributes().value
                    formatted = prop.attributes().formatted
                }
            }
            node.property.each { prop ->
                if (prop.attributes().id == 'CLIHUM') {
                    humidity = prop.attributes().formatted
                }
            }
            node.property.each { prop ->
                if (prop.attributes().id == 'CLISPH') {
                    heatSpt = prop.attributes().formatted
                }
            }
            node.property.each { prop ->
                if (prop.attributes().id == 'CLISPC') {
                    coolSpt = prop.attributes().formatted
                }
            }
            node.property.each { prop ->
                if (prop.attributes().id == 'CLIMD') {
                    tstatMd = prop.attributes().formatted
                    if (tstatMd == 'Heat') {tstatMd = 'heat'}
                    if (tstatMd == 'Off') {tstatMd = 'off'}
                    if (tstatMd == 'Cool') {tstatMd = 'cool'}
                    if (tstatMd == 'Auto') {tstatMd = 'auto'}
                    if (tstatMd == 'Program Auto') {tstatMd = 'pauto'}
                 }
            }
            

            if (status != '' && childMap[nodeAddr]) {
                def child = childMap[nodeAddr]

                if (child.getDataValue("nodeAddr") == nodeAddr) {
                    def value = 'on'
                    if (status == '0') {
                        value = 'off'
                    }
                    status = status.toFloat() * 99.0 / 255.0
                    status = status.toInteger()
                    log.debug "Updating ${child.label} ${nodeAddr} to ${value}/${status}"
                    child.sendEvent(name: 'switch', value: value)

                    if (status != 0) {
                        child.sendEvent(name: 'level', value: status)
                    }
                    if (child.label == "Thermostat") {
                    	log.debug "Thermostat Found: "+ formatted + "df " + humidity + " %RH "+ tstatMd
                        child.sendEvent (name: 'temperature', value: formatted)
                        child.sendEvent (name: 'humidity', value: humidity)
                        child.sendEvent (name: 'heatingSetpoint', value: heatSpt)
                        child.sendEvent (name: 'coolingSetpoint', value: coolSpt)
                        child.sendEvent (name: 'thermostatMode', value: tstatMd)
                    }
                }
            
            }
        }
        }
        
        def stnode = xmlTop
        //log.debug 'stnode: ' + stnode.name()
        
        if (xmlText.contains('status')) {
        log.debug 'Running Status'	
            def childMap = [:]
        parent.getChildDevices(false).each { child ->
            def childNodeAddr = child.getDataValue("nodeAddr")
            childMap[childNodeAddr] = child
            //log.debug "Adding Child " + child + "ID: " + childNodeAddr
        }
            stnode.each { node ->
            	def type = ''
                def area = ''
                def areatype = ''
                def alarmref = 'ELK-M1'
                def zone = ''
                def zoneref = ''
                def val = ''
                
                stnode.ze.each { ze ->
                if (ze.attributes().type == '51') {
                    zone = ze.attributes().zone
                    zoneref = 'ZE ' + zone
                    val = ze.attributes().val
                    log.debug 'Zone: ' + zone + ' Value: ' + val + ' ZoneRef: ' + zoneref
                }
                if (zone != '' && val != '' && childMap[zoneref]) {
                def child = childMap[zoneref]
				if (child.getDataValue("nodeAddr") == zoneref) {
                def value = 'open'
                if (val == '0') {
                      value = 'closed'
                	}
                log.debug "Found: " + zoneref + ' Child: ' + child.name
                log.debug "Updating ${child.name} ${zoneref} to ${value}/${val}"
                child.sendEvent(name: 'contact', value: value)
                }
                }
                }
                
                stnode.ae.each { ae ->
                if (ae.attributes().type == '1' || ae.attributes().type == '2' || ae.attributes().type == '3') {
                    areatype = ae.attributes().type
                    val = ae.attributes().val
                    //alarmref = "ELK-M1"
                    log.debug 'AreaType: ' + areatype + ' Value: ' + val
                }
                if (areatype != '' && val != '' && childMap[alarmref]) {
                	def child = childMap[alarmref]
                    def value = ''
                    log.debug "Looking for child"
				if (child.getDataValue("nodeAddr") == alarmref) {
                	log.debug "Found Child " + child.name
                    if (areatype == '1') {
                	if (val == '0') {
                      value = 'No Alarm Active'
                	}
                	if (val == '6') {
                      value = 'Burglar Alarm'
                	}
                	log.debug "Found: " + alarmref + ' Child: ' + child.name
                	log.debug "Updating ${child.name} ${alarmref} to ${value}/${val}"
                	child.sendEvent(name: 'AlarmState', value: value)
                }
                	if (areatype == '2') {
                		if (val == '0') {
                      	value = 'Not Ready'
                		}
                		if (val == '1') {
                      	value = 'Ready to Arm'
                		}
                		if (val == '3') {
                	  	value = 'Armed with Exit Timer'
                      	}
                		if (val == '4') {
                	  	value = 'Armed Fully'
                      	}      
                		log.debug "Found: " + alarmref + ' Child: ' + child.name
                		log.debug "Updating ${child.name} ${alarmref} to ${value}/${val}"
                		child.sendEvent(name: 'ArmUpState', value: value)
                	}
                    if (areatype == '3') {
                		if (val == '0') {
                      	value = 'Disarmed'
                		}
                		if (val == '1') {
                      	value = 'Armed Away'
                		}
                		if (val == '2') {
                	  	value = 'Armed Stay'
                      	}
                		if (val == '3') {
                	  	value = 'Armed Stay Instant'
                      	}   
                        if (val == '4') {
                	  	value = 'Armed Night'
                      	}
                        if (val == '5') {
                	  	value = 'Armed Night Instant'
                      	}
                        if (val == '6') {
                	  	value = 'Armed Vacation'
                      	}
                        log.debug "Found: " + alarmref + ' Child: ' + child.name
                		log.debug "Updating ${child.name} ${alarmref} to ${value}/${val}"
                		child.sendEvent(name: 'ArmedState', value: value)
                	}
                	}
                	}
                }
                
         }   
        }
        
        
    }
    
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
    log.debug "Using ip: ${ip} and port: ${port} for device: ${device.id}"
    return ip + ":" + port
}

private getAuthorization() {
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
def on() {
    log.debug "Executing 'on'"

    sendEvent(name: 'switch', value: 'on')
    def node = getDataValue("nodeAddr").replaceAll(" ", "%20")
    def path = "/rest/nodes/${node}/cmd/DON"
    getRequest(path)
}

def off() {
    log.debug "Executing 'off'"

    sendEvent(name: 'switch', value: 'off')
    def node = getDataValue("nodeAddr").replaceAll(" ", "%20")
    def path = "/rest/nodes/${node}/cmd/DOF"
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

    def path = "/rest/status"
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