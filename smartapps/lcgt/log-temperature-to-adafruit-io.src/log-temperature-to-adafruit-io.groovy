/**
 *  Copyright 2015 SmartThings
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
 *  Log Temp To Adafruit IO
 *
 *  Author: Ben Miller
 */
definition(
    name: "Log Temperature To Adafruit IO",
    namespace: "lcgt",
    author: "Ben Miller",
    description: "Monitor the temperature and write changes to an Adafruit IO stream.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/its-too-hot.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/its-too-hot@2x.png"
)

preferences {
	section("Thermostat to Monitor:") {
		input "thermostat1", "capability.thermostat", required: true, title: "Sensor?"
	}
section("Humidity Sensor to Monitor:") {
		input "humiditySensor1", "capability.relativeHumidityMeasurement", required: true, title: "Sensor?"
	}
	section("Adafruit IO Information") {
		input "Username", "string", required: true, title: "AdafruitIO Username?"
        input "TFeedName", "string", required: true, title: "T-FeedName?"
        input "HFeedName", "string", required: true, title: "H-FeedName?"
        input "AIOKey", "string", required: true, title: "AIOKey?"
	}
}

def installed() {
	subscribe(thermostat1, "temperature", temperatureHandler)
        subscribe(humiditySensor1, "humidity", humidityHandler)
}

def updated() {
	unsubscribe()
    log.debug "Updated Settings"
	subscribe(thermostat1, "temperature", temperatureHandler)
        subscribe(humiditySensor1, "humidity", humidityHandler)
}

def temperatureHandler(evt) {
	log.trace "temperature: $evt.value, $evt"

	def user = Username
    def key = AIOKey
    def feed = TFeedName
    def temp = evt.value
	
	if (evt.doubleValue ) {
		log.debug "Received temperature update: $evt.value"
        
        //Build the request
        def params = [
		    uri: "https://io.adafruit.com/api/feeds/${feed}/data.json",
            headers: ['X-AIO-Key': key, 'Content-Type': "application/json"],
    		body: 
            	[
        			value: temp
				]
			]
		try {
    		httpPostJson(params) { resp ->
        		resp.headers.each {
        		    log.debug "${it.name} : ${it.value}"
     		   }
        		log.debug "response contentType: ${resp.contentType}"
    		}
		} catch (e) {
    		log.debug "something went wrong: $e"
		}
	}

}
def humidityHandler(evt) {
	log.trace "humidity: $evt.value, $evt"

	def user = Username
    def key = AIOKey
    def feed = HFeedName
    def temp = evt.value
	
	if (evt.doubleValue ) {
		log.debug "Received humidity update: $evt.value"
        
        //Build the request
        def params = [
		    uri: "https://io.adafruit.com/api/feeds/${feed}/data.json",
            headers: ['X-AIO-Key': key, 'Content-Type': "application/json"],
    		body: 
            	[
        			value: temp
				]
			]
		try {
    		httpPostJson(params) { resp ->
        		resp.headers.each {
        		    log.debug "${it.name} : ${it.value}"
     		   }
        		log.debug "response contentType: ${resp.contentType}"
    		}
		} catch (e) {
    		log.debug "something went wrong: $e"
		}
	}

}
