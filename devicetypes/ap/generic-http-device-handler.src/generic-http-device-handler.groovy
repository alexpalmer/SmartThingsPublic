/**
 *  Generic ESP8266 Device Handler v1
 *  
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */



metadata {
	definition (name: "Generic HTTP Device Handler", author: "AP", namespace:"AP") {
		capability "Temperature Measurement"
		capability "Polling"
		capability "Refresh"
        
		attribute "temperature0", "number"
		attribute "temperature1", "number"
		attribute "temperature2", "number"
		attribute "temperature3", "number"
		attribute "temperature4", "number"
        
        attribute "defaulttemp", "string"
        
        attribute "effect", "string"
        attribute "efficiency", "number"

	}
    preferences {
		input("DeviceIP", "string", title:"Device IP Address", description: "Please enter your device's IP Address", required: true, displayDuringSetup: true)
		input("DevicePort", "string", title:"Device Port", description: "Empty assumes port 80.", required: false, displayDuringSetup: true)
		input("DevicePath", "string", title:"URL Path", description: "Rest of the URL, include forward slash.", displayDuringSetup: true)
		input(name: "DevicePostGet", type: "enum", title: "POST or GET", options: ["POST","GET"], defaultValue: "POST", required: false, displayDuringSetup: true)
        input("defaulttemp", "string", title: "Default Reported Temperature", type: "enum", options: ["temperature0","temperature1","temperature2","temperature3","temperature4"], defaultValue: "temperature0", required: false, displayDuringSetup: true)
		
	}
	
	simulator {
	}
    
    valueTile("temperature0", "device.temperature0", width: 1, height: 1) {
			state("default", label:'In Sup\n${currentValue}°', Unit:"F",
				backgroundColors:[
					[value: 32, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
					[value: 92, color: "#d04e00"],
					[value: 98, color: "#bc2323"]
				]
			)
		}
     valueTile("temperature1", "device.temperature1", width: 1, height: 1) {
			state("default", label:'Out Sup\n${currentValue}°', Unit:'F',
				backgroundColors:[
					[value: 32, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
					[value: 92, color: "#d04e00"],
					[value: 98, color: "#bc2323"]
				]
			)
		}
        
    valueTile("temperature2", "device.temperature2", width: 1, height: 1) {
			state("default", label:'In Ret\n${currentValue}°', Unit:'F',
				backgroundColors:[
					[value: 32, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
					[value: 92, color: "#d04e00"],
					[value: 98, color: "#bc2323"]
				]
			)
		}
     valueTile("temperature3", "device.temperature3", width: 1, height: 1) {
			state("default", label:'Furnace\n ${currentValue}°', Unit:'F',
				backgroundColors:[
					[value: 32, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
					[value: 92, color: "#d04e00"],
					[value: 98, color: "#bc2323"]
				]
			)
		}
     valueTile("temperature4", "device.temperature4", width: 1, height: 1) {
			state("default", label:'Out Ret\n${currentValue}°', Unit:'F',
				backgroundColors:[
					[value: 32, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
					[value: 92, color: "#d04e00"],
					[value: 98, color: "#bc2323"]
				]
			)
		}
        
        valueTile("effect", 'device.effect', width: 1, height: 1) {
        	state("default", label:'${currentValue}')
        }
        valueTile("efficiency", 'device.efficiency', width: 1, height: 1) {
        	state("default", label:'${currentValue}%')
        }
        
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
        }
     main "efficiency"
		//details(["refresh","temperature0","temperature1","temperature2","temperature3","temperature4","effect","efficiency"])
        details(["temperature0","refresh","temperature1","temperature2","efficiency","temperature4","effect","temperature3"])
        }

def runCmd(String varCommand) {
	def host = DeviceIP
	def hosthex = convertIPtoHex(host).toUpperCase()
	def LocalDevicePort = ''
	if (DevicePort==null) { LocalDevicePort = "80" } else { LocalDevicePort = DevicePort }
	def porthex = convertPortToHex(LocalDevicePort).toUpperCase()
	device.deviceNetworkId = "$hosthex:$porthex"
	

	log.debug "The device id configured is: $device.deviceNetworkId"

	def headers = [:] 
	headers.put("HOST", "$host:$LocalDevicePort")
	headers.put("Content-Type", "application/x-www-form-urlencoded")
	
	log.debug "The Header is $headers"

	def path = ''
	def body = ''
	log.debug "Uses which method: $DevicePostGet"
	def method = "POST"
	try {
		if (DevicePostGet.toUpperCase() == "GET") {
			method = "GET"
			path = varCommand
			if (path.substring(0,1) != "/") { path = "/" + path }
			log.debug "GET path is: $path"
		} else {
			path = DevicePath
			body = varCommand 
			log.debug "POST body is: $body"
		}
		log.debug "The method is $method"
	}
	catch (Exception e) {
		settings.DevicePostGet = "POST"
		log.debug e
		log.debug "You must not have set the preference for the DevicePOSTGET option"
	}

	try {
		def hubAction = new physicalgraph.device.HubAction(
			method: method,
			path: path,
			body: body,
			headers: headers
			)
		hubAction.options = [outputMsgToS3:false]
		log.debug hubAction
		hubAction
	}
	catch (Exception e) {
		log.debug "Hit Exception $e on $hubAction"
	}
}

def refresh() {
	def FullCommand = '/xmlstatus'
	runCmd(FullCommand)
}
def poll() {
	refresh()
}

def parse(String description) {
	//log.debug "Parsing Dev ${device.deviceNetworkId} '${description}'"

    def parsedEvent = parseDiscoveryMessage(description)
    //log.debug "Parsed event: " + parsedEvent
    //log.debug "Body: " + parsedEvent['body']
    if (parsedEvent['body'] != null) {
        def xmlText = new String(parsedEvent.body.decodeBase64())
        //log.debug 'Device Type Decoded body: ' + xmlText

        def xmlTop = new XmlSlurper().parseText(xmlText)
      
        
        xmlTop.temp.each { temp ->
        	def nodeAddr = temp.attributes().id
            def device = temp.attributes().device
            def val = temp.attributes().value
            //log.debug "Found Device="+device+" Temperature="+val
            switch (device) {
            case "0":
            	sendEvent (name: 'temperature0', value: val)
                //log.debug "Sending temperature0"
                break
            case "1":
            	sendEvent (name: 'temperature1', value: val)
                break
            case "2":
            	sendEvent (name: 'temperature2', value: val)
                break
            case "3":
            	sendEvent (name: 'temperature3', value: val)
                break
            case "4":
            	sendEvent (name: 'temperature4', value: val)
                break
            default:
            	log.debug "Unknown Device="+device
            	break     
            
            }
            
                        
        }
        //log.debug (device.currentValue('temperature0')-device.currentValue('temperature2'))
        if ((device.currentValue('temperature0')-device.currentValue('temperature2'))>0)
        sendEvent (name: 'effect', value: 'cooling')
        
        if ((device.currentValue('temperature0')-device.currentValue('temperature2'))<0)
        sendEvent (name: 'effect', value: 'heating')
        
        if ((device.currentValue('temperature0')-device.currentValue('temperature2'))==0)
        sendEvent (name: 'effect', value: 'nothing')
        
        def eff = ((device.currentValue('temperature4')-device.currentValue('temperature1')))/((device.currentValue('temperature2')-device.currentValue('temperature1')))*100.0
        sendEvent (name: 'efficiency', value: eff)
        
        def deftemp = device.currentValue(defaulttemp)
        sendEvent (name: 'temperature', value: deftemp)
        
        
    }
    
}

private String convertIPtoHex(ipAddress) { 
	String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
	//log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
	return hex
}
private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
	//log.debug hexport
	return hexport
}
private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}
private String convertHexToIP(hex) {
	//log.debug("Convert hex to ip: $hex") 
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}
private getHostAddress() {
	def parts = device.deviceNetworkId.split(":")
	//log.debug device.deviceNetworkId
	def ip = convertHexToIP(parts[0])
	def port = convertHexToInt(parts[1])
	return ip + ":" + port
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