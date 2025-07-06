/**
 *  WiiM Universal Device Driver for Hubitat
 *  Compatible with: WiiM Mini, WiiM Pro, WiiM Pro Plus, WiiM Ultra, WiiM Amp, WiiM Amp Pro
 *
 *  Copyright 2025 Simon Mason
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
 *  Based on WiiM/LinkPlay HTTP API documentation and Home Assistant integration
 *
 *  Change Log:
 *  Version 1.09 - 2025-06-25 - Removed TuneIn, iHeartRadio, and Spotify functionality to rely on presets
 *  Version 1.08 - 2025-06-24 - Fixed BigDecimal modulo error in formatTime function, added better error handling
 *                               for track position/duration, handle live streams with totlen=0
 *  Version 1.07 - 2025-06-24 - Fixed album art retrieval (getArt command not supported), improved error handling,
 *                               album art now retrieved via getPlayerStatus, better null checking
 *  Version 1.06 - 2025-06-24 - Added albumArtHtml attribute for visual album art display in device states,
 *                               configurable image size, and enhanced now playing card with track info
 *  Version 1.05 - 2025-06-24 - Added album art support with albumArt attribute and getAlbumArt command,
 *                               enhanced now playing information parsing
 *  Version 1.04 - 2025-06-24 - Added playIHeartRadio and playSpotify commands with support for 
 *                               station IDs, URIs, URLs, and intelligent format detection
 *  Version 1.03 - 2025-06-24 - Added playTuneInStation command with support for station IDs, URLs, 
 *                               and station name searches via TuneIn API
 *  Version 1.02 - 2025-06-24 - Updated to reflect universal WiiM device compatibility, added source availability
 *                               detection based on device capabilities, improved error handling for unsupported features
 *  Version 1.01 - 2025-06-24 - Added comprehensive API support including URL/M3U playback, prompt sounds,
 *                               seeking, loop modes, multiroom control, USB playback, hex string conversion,
 *                               enhanced status parsing, and improved source switching
 *  Version 1.00 - 2025-06-24 - Initial release with basic playback controls, volume control, 
 *                               preset selection, status polling, and SSL certificate handling
 */

metadata {
    definition (
        name: "WiiM Universal", 
        namespace: "simonmason", 
        author: "Simon Mason",
        importUrl: "https://raw.githubusercontent.com/simonmason/wiim-hubitat-driver/main/wiim-universal-driver.groovy"
    ) {
        capability "AudioVolume"
        capability "MediaTransport" 
        capability "MusicPlayer"
        capability "Refresh"
        capability "Initialize"
        
        attribute "deviceName", "string"
        attribute "deviceModel", "string"
        attribute "currentSource", "string"
        attribute "playbackStatus", "enum", ["play", "pause", "stop", "buffering", "load"]
        attribute "currentTrack", "string"
        attribute "currentArtist", "string"
        attribute "currentAlbum", "string"
        attribute "albumArt", "string"
        attribute "albumArtHtml", "string"
        attribute "deviceVersion", "string"
        attribute "wifiStrength", "number"
        attribute "connectionStatus", "enum", ["connected", "disconnected"]
        attribute "loopMode", "enum", ["0", "1", "2", "3", "4", "5"]
        attribute "totalTracks", "number"
        attribute "currentTrackIndex", "number"
        attribute "trackPosition", "string"
        attribute "trackDuration", "string"
        attribute "multiroomRole", "enum", ["standalone", "master", "slave"]
        attribute "multiroomSlaves", "number"
        attribute "availableSources", "string"
        
        command "playPreset", ["number"]
        command "switchToSource", ["string"]
        command "getStatus"
        command "getDeviceInfo"
        command "toggleMute"
        command "volumeUp"
        command "volumeDown"
        command "reboot"
        command "playURL", ["string"]
        command "playM3U", ["string"]
        command "playPromptSound", ["string"]
        command "seekTo", ["number"]
        command "setLoopMode", ["number"]
        command "joinMultiroomGroup", ["string"]
        command "leaveMultiroomGroup"
        command "getMultiroomStatus"
        command "playLocalFile", ["number"]
        command "getUSBPlaylist"
        command "getAlbumArt"
    }

    preferences {
        input "deviceIP", "text", title: "Device IP Address", description: "IP address of your WiiM device", required: true
        input "useHTTPS", "bool", title: "Use HTTPS", description: "Enable if your WiiM device uses HTTPS (most do)", defaultValue: true
        input "pollingInterval", "enum", title: "Status Polling Interval", 
              options: ["30": "30 seconds", "60": "1 minute", "300": "5 minutes", "0": "Disabled"], 
              defaultValue: "60", required: true
        input "volumeStep", "number", title: "Volume Step", description: "Volume change amount (1-25)", range: "1..25", defaultValue: 5
        input "albumArtSize", "enum", title: "Album Art Display Size", 
              options: ["100": "Small (100px)", "150": "Medium (150px)", "200": "Large (200px)", "300": "Extra Large (300px)"], 
              defaultValue: "150", required: true
        input "enableDebug", "bool", title: "Enable Debug Logging", defaultValue: false
        input "enableInfo", "bool", title: "Enable Info Logging", defaultValue: true
    }
}

def installed() {
    logInfo "WiiM Universal driver installed"
    initialize()
}

def updated() {
    logInfo "WiiM Universal driver updated"
    unschedule()
    initialize()
}

def initialize() {
    logInfo "Initializing WiiM Universal device"
    
    if (!deviceIP) {
        logWarn "Device IP address not configured"
        return
    }
    
    // Set initial state
    sendEvent(name: "connectionStatus", value: "disconnected")
    
    // Get initial device info and status
    runIn(2, "getDeviceInfo")
    runIn(5, "getStatus")
    
    // Setup polling if enabled
    if (pollingInterval && pollingInterval != "0") {
        def interval = pollingInterval.toInteger()
        runIn(interval, "pollDevice")
    }
}

def refresh() {
    logInfo "Refreshing device status"
    getStatus()
    getDeviceInfo()
}

def getDeviceInfo() {
    logDebug "Getting device information"
    sendCommand("getStatusEx")
}

def getStatus() {
    logDebug "Getting playback status"
    sendCommand("getPlayerStatus")
}

def play() {
    logInfo "Play command"
    sendCommand("setPlayerCmd:play")
    sendEvent(name: "playbackStatus", value: "play")
}

def pause() {
    logInfo "Pause command"
    sendCommand("setPlayerCmd:pause")
    sendEvent(name: "playbackStatus", value: "pause")
}

def stop() {
    logInfo "Stop command"
    sendCommand("setPlayerCmd:stop")
    sendEvent(name: "playbackStatus", value: "stop")
}

def nextTrack() {
    logInfo "Next track command"
    sendCommand("setPlayerCmd:next")
}

def previousTrack() {
    logInfo "Previous track command"
    sendCommand("setPlayerCmd:prev")
}

def setVolume(volume) {
    if (volume < 0) volume = 0
    if (volume > 100) volume = 100
    
    logInfo "Setting volume to ${volume}"
    sendCommand("setPlayerCmd:vol:${volume}")
    sendEvent(name: "volume", value: volume)
}

def volumeUp() {
    def currentVol = device.currentValue("volume") ?: 50
    def newVol = currentVol + (volumeStep ?: 5)
    setVolume(newVol)
}

def volumeDown() {
    def currentVol = device.currentValue("volume") ?: 50
    def newVol = currentVol - (volumeStep ?: 5)
    setVolume(newVol)
}

def mute() {
    logInfo "Mute command"
    sendCommand("setPlayerCmd:mute:1")
    sendEvent(name: "mute", value: "muted")
}

def unmute() {
    logInfo "Unmute command"
    sendCommand("setPlayerCmd:mute:0")
    sendEvent(name: "mute", value: "unmuted")
}

def toggleMute() {
    def currentMute = device.currentValue("mute")
    if (currentMute == "muted") {
        unmute()
    } else {
        mute()
    }
}

def playPreset(presetNumber) {
    if (presetNumber < 1 || presetNumber > 10) {
        logWarn "Preset number must be between 1 and 10"
        return
    }
    logInfo "Playing preset ${presetNumber}"
    sendCommand("MCUKeyShortClick:${presetNumber}")
}

def switchToSource(source) {
    logInfo "Switching to source: ${source}"
    // Map common source names to API values
    def sourceMap = [
        "wifi": "wifi",
        "bluetooth": "bluetooth", 
        "bt": "bluetooth",
        "linein": "line-in",
        "line-in": "line-in",
        "aux": "line-in",
        "optical": "optical",
        "coaxial": "co-axial",
        "usb": "udisk",
        "udisk": "udisk",
        "usbdac": "PCUSB",
        "airplay": "wifi",
        "dlna": "wifi"
    ]
    
    def apiSource = sourceMap[source.toLowerCase()] ?: source
    sendCommand("setPlayerCmd:switchmode:${apiSource}")
    sendEvent(name: "currentSource", value: source)
}

def playURL(url) {
    logInfo "Playing URL: ${url}"
    sendCommand("setPlayerCmd:play:${url}")
}

def playM3U(url) {
    logInfo "Playing M3U playlist: ${url}"
    sendCommand("setPlayerCmd:m3u:play:${url}")
}

def playPromptSound(url) {
    logInfo "Playing prompt sound: ${url}"
    sendCommand("playPromptUrl:${url}")
}

def seekTo(position) {
    logInfo "Seeking to position: ${position} seconds"
    sendCommand("setPlayerCmd:seek:${position}")
}

def setLoopMode(mode) {
    if (mode < 0 || mode > 5) {
        logWarn "Loop mode must be between 0 and 5"
        return
    }
    logInfo "Setting loop mode to: ${mode}"
    sendCommand("setPlayerCmd:loopmode:${mode}")
    sendEvent(name: "loopMode", value: mode.toString())
}

def joinMultiroomGroup(hostIP) {
    logInfo "Joining multiroom group with host: ${hostIP}"
    sendCommand("ConnectMasterAp:JoinGroupMaster:eth${hostIP}:wifi0.0.0.0")
}

def leaveMultiroomGroup() {
    logInfo "Leaving multiroom group"
    sendCommand("multiroom:Ungroup")
}

def getMultiroomStatus() {
    logInfo "Getting multiroom status"
    sendCommand("multiroom:getSlaveList")
}

def playLocalFile(index) {
    logInfo "Playing local USB file at index: ${index}"
    sendCommand("setPlayerCmd:playLocalList:${index}")
}

def getUSBPlaylist() {
    logInfo "Getting USB playlist"
    sendCommand("getLocalPlayList")
}

def getAlbumArt() {
    logInfo "Getting album art via player status"
    // The getArt command isn't supported, so we'll get it via player status
    sendCommand("getPlayerStatus")
}

def reboot() {
    logInfo "Rebooting device"
    sendCommand("reboot")
}

def pollDevice() {
    getStatus()
    
    // Schedule next poll
    if (pollingInterval && pollingInterval != "0") {
        def interval = pollingInterval.toInteger()
        runIn(interval, "pollDevice")
    }
}

def sendCommand(command) {
    if (!deviceIP) {
        logWarn "Device IP address not configured"
        return
    }
    
    def protocol = useHTTPS ? "https" : "http"
    def uri = "${protocol}://${deviceIP}/httpapi.asp?command=${command}"
    
    logDebug "Sending command: ${command} to ${uri}"
    
    def params = [
        uri: uri,
        timeout: 10,
        ignoreSSLIssues: true,  // This handles the self-signed certificate issue
        headers: [
            "User-Agent": "Hubitat-WiiM-Driver/1.09"
        ]
    ]
    
    try {
        httpGet(params) { resp ->
            logDebug "Response: ${resp.status} - ${resp.data}"
            
            if (resp.status == 200) {
                sendEvent(name: "connectionStatus", value: "connected")
                parseResponse(command, resp.data)
            } else {
                logWarn "HTTP Error: ${resp.status}"
                sendEvent(name: "connectionStatus", value: "disconnected")
            }
        }
    } catch (Exception e) {
        logError "Command failed: ${e.message}"
        sendEvent(name: "connectionStatus", value: "disconnected")
        
        // If HTTPS fails, try HTTP as fallback
        if (useHTTPS && e.message.contains("SSL") || e.message.contains("certificate")) {
            logInfo "HTTPS failed, trying HTTP as fallback"
            def httpUri = "http://${deviceIP}/httpapi.asp?command=${command}"
            try {
                httpGet([uri: httpUri, timeout: 10]) { resp ->
                    logDebug "HTTP fallback successful: ${resp.status}"
                    if (resp.status == 200) {
                        sendEvent(name: "connectionStatus", value: "connected")
                        parseResponse(command, resp.data)
                    }
                }
            } catch (Exception e2) {
                logError "HTTP fallback also failed: ${e2.message}"
            }
        }
    }
}

def parseResponse(command, data) {
    logDebug "Parsing response for command: ${command}, data: ${data}"
    
    if (data == "OK" || data == "ok") {
        logDebug "Command executed successfully"
        return
    }
    
    if (data == "unknown command") {
        logWarn "Command '${command}' not supported by this device"
        return
    }
    
    try {
        if (data.toString().startsWith("{")) {
            def json = new groovy.json.JsonSlurper().parseText(data.toString())
            
            if (command == "getPlayerStatus") {
                parsePlayerStatus(json)
            } else if (command == "getStatusEx") {
                parseDeviceStatus(json)
            } else if (command.startsWith("multiroom")) {
                parseMultiroomStatus(json)
            } else if (command == "getLocalPlayList") {
                parseUSBPlaylist(json)
            }
        } else if (data.toString().isNumber()) {
            // Handle simple numeric responses like GetTrackNumber
            if (command == "GetTrackNumber") {
                sendEvent(name: "totalTracks", value: data.toInteger())
            }
        }
    } catch (Exception e) {
        logError "Failed to parse JSON response: ${e.message}"
    }
}

def parsePlayerStatus(json) {
    logDebug "Parsing player status: ${json}"
    
    if (json.status != null) {
        def status = "stop"
        switch(json.status) {
            case "play":
                status = "play"
                break
            case "pause":
                status = "pause"
                break
            case "stop":
                status = "stop"
                break
            case "loading":
            case "load":
                status = "buffering"
                break
        }
        sendEvent(name: "playbackStatus", value: status)
    }
    
    if (json.vol != null) {
        sendEvent(name: "volume", value: json.vol.toInteger())
    }
    
    if (json.mute != null) {
        def muteStatus = json.mute == "1" ? "muted" : "unmuted"
        sendEvent(name: "mute", value: muteStatus)
    }
    
    if (json.Title != null && json.Title != "") {
        def title = hexToAscii(json.Title) ?: json.Title
        sendEvent(name: "currentTrack", value: title)
        // Update HTML display when track changes
        def artUrl = device.currentValue("albumArt")
        if (artUrl) updateAlbumArtHtml(artUrl)
    }
    
    if (json.Artist != null && json.Artist != "") {
        def artist = hexToAscii(json.Artist) ?: json.Artist
        sendEvent(name: "currentArtist", value: artist)
        // Update HTML display when artist changes
        def artUrl = device.currentValue("albumArt")
        if (artUrl) updateAlbumArtHtml(artUrl)
    }
    
    if (json.Album != null && json.Album != "") {
        def album = hexToAscii(json.Album) ?: json.Album
        sendEvent(name: "currentAlbum", value: album)
    }
    
    // Handle album art URL if present
    if (json.art != null && json.art != "") {
        def artUrl = hexToAscii(json.art) ?: json.art
        // Ensure it's a proper URL
        if (artUrl.startsWith("http")) {
            sendEvent(name: "albumArt", value: artUrl)
            updateAlbumArtHtml(artUrl)
        } else {
            // Some devices return relative paths, construct full URL
            def deviceUrl = useHTTPS ? "https://${deviceIP}" : "http://${deviceIP}"
            def fullArtUrl = "${deviceUrl}${artUrl}"
            sendEvent(name: "albumArt", value: fullArtUrl)
            updateAlbumArtHtml(fullArtUrl)
        }
    }
    
    if (json.mode != null) {
        def modeNames = [
            "0": "idle",
            "1": "airplay", 
            "2": "dlna",
            "10": "wifi",
            "11": "usb",
            "20": "http",
            "31": "spotify",
            "40": "line-in",
            "41": "bluetooth",
            "43": "optical",
            "47": "line-in2",
            "51": "usbdac",
            "99": "slave"
        ]
        def sourceName = modeNames[json.mode.toString()] ?: "unknown"
        sendEvent(name: "currentSource", value: sourceName)
    }
    
    if (json.loop != null) {
        sendEvent(name: "loopMode", value: json.loop.toString())
    }
    
    if (json.plicount != null) {
        sendEvent(name: "totalTracks", value: json.plicount.toInteger())
    }
    
    if (json.plicurr != null) {
        sendEvent(name: "currentTrackIndex", value: json.plicurr.toInteger())
    }
    
    if (json.curpos != null && json.totlen != null) {
        try {
            def currentPos = formatTime(json.curpos.toInteger())
            def totalLen = json.totlen.toInteger() > 0 ? formatTime(json.totlen.toInteger()) : "Live"
            sendEvent(name: "trackPosition", value: currentPos)
            sendEvent(name: "trackDuration", value: totalLen)
        } catch (Exception e) {
            logError "Error processing track position/duration: ${e.message}"
            sendEvent(name: "trackPosition", value: "0:00")
            sendEvent(name: "trackDuration", value: "Unknown")
        }
    }
    
    if (json.type != null) {
        def role = "standalone"
        switch(json.type) {
            case "0":
                role = "standalone" 
                break
            case "1":
                role = "slave"
                break
        }
        sendEvent(name: "multiroomRole", value: role)
    }
}

def parseDeviceStatus(json) {
    logDebug "Parsing device status: ${json}"
    
    if (json.DeviceName != null) {
        sendEvent(name: "deviceName", value: json.DeviceName)
    }
    
    if (json.project != null) {
        sendEvent(name: "deviceModel", value: json.project)
    }
    
    if (json.firmware != null) {
        sendEvent(name: "deviceVersion", value: json.firmware)
    }
    
    if (json.RSSI != null && json.RSSI != "0") {
        sendEvent(name: "wifiStrength", value: json.RSSI.toInteger())
    }
    
    // Parse available sources based on device capabilities
    if (json.plm_support != null) {
        def capabilities = json.plm_support
        def sources = ["wifi"] // All WiiM devices support WiFi
        
        try {
            def capValue = capabilities.startsWith("0x") ? 
                Long.parseLong(capabilities.substring(2), 16) : 
                Long.parseLong(capabilities)
            
            if (capValue & 0x2) sources.add("line-in")      // bit 1: LineIn
            if (capValue & 0x4) sources.add("bluetooth")    // bit 2: Bluetooth  
            if (capValue & 0x8) sources.add("usb")          // bit 3: USB
            if (capValue & 0x10) sources.add("optical")     // bit 4: Optical
            if (capValue & 0x40) sources.add("coaxial")     // bit 6: Coaxial
            if (capValue & 0x100) sources.add("line-in2")   // bit 8: LineIn 2
            if (capValue & 0x8000) sources.add("usbdac")    // bit 15: USBDAC
            
            sendEvent(name: "availableSources", value: sources.join(", "))
        } catch (Exception e) {
            logWarn "Could not parse device capabilities: ${e.message}"
        }
    }
}

def parseMultiroomStatus(json) {
    logDebug "Parsing multiroom status: ${json}"
    
    if (json.slaves != null) {
        sendEvent(name: "multiroomSlaves", value: json.slaves.toInteger())
        
        if (json.slaves.toInteger() > 0) {
            sendEvent(name: "multiroomRole", value: "master")
        } else {
            sendEvent(name: "multiroomRole", value: "standalone")
        }
    }
}

def parseUSBPlaylist(json) {
    logDebug "Parsing USB playlist: ${json}"
    
    if (json.num != null) {
        sendEvent(name: "totalTracks", value: json.num.toInteger())
    }
}

def updateAlbumArtHtml(artUrl) {
    if (!artUrl || artUrl == "") {
        sendEvent(name: "albumArtHtml", value: "<div style='text-align: center; color: #666;'>No album art available</div>")
        return
    }
    
    def size = albumArtSize ?: "150"
    def currentTrack = device.currentValue("currentTrack") ?: "Unknown Track"
    def currentArtist = device.currentValue("currentArtist") ?: "Unknown Artist"
    
    def htmlContent = """
    <div style='text-align: center; padding: 10px; background: #f8f9fa; border-radius: 8px; margin: 5px;'>
        <img src='${artUrl}' 
             style='width: ${size}px; height: ${size}px; object-fit: cover; border-radius: 6px; box-shadow: 0 2px 8px rgba(0,0,0,0.15);' 
             onerror='this.style.display="none"' 
             alt='Album Art'/>
        <div style='margin-top: 8px; font-size: 14px; font-weight: bold; color: #333;'>${currentTrack}</div>
        <div style='font-size: 12px; color: #666; margin-top: 2px;'>${currentArtist}</div>
    </div>
    """
    
    sendEvent(name: "albumArtHtml", value: htmlContent)
}

// Logging functions
def logInfo(msg) {
    if (enableInfo) log.info "[WiiM Universal] ${msg}"
}

def logDebug(msg) {
    if (enableDebug) log.debug "[WiiM Universal] ${msg}"
}

def logWarn(msg) {
    log.warn "[WiiM Universal] ${msg}"
}

def logError(msg) {
    log.error "[WiiM Universal] ${msg}"
}

// Utility functions
def hexToAscii(hexString) {
    if (!hexString || hexString.length() % 2 != 0) {
        return null
    }
    
    try {
        def ascii = ""
        for (int i = 0; i < hexString.length(); i += 2) {
            def hex = hexString.substring(i, i + 2)
            ascii += (char) Integer.parseInt(hex, 16)
        }
        return ascii
    } catch (Exception e) {
        logError "Failed to convert hex to ASCII: ${e.message}"
        return null
    }
}

def formatTime(milliseconds) {
    try {
        def totalSeconds = Math.floor(milliseconds / 1000) as Integer
        def minutes = Math.floor(totalSeconds / 60) as Integer
        def seconds = totalSeconds % 60
        def hours = Math.floor(minutes / 60) as Integer
        
        if (hours >= 1) {
            return String.format("%d:%02d:%02d", hours, minutes % 60, seconds)
        } else {
            return String.format("%d:%02d", minutes, seconds)
        }
    } catch (Exception e) {
        logError "Error formatting time for ${milliseconds}ms: ${e.message}"
        return "0:00"
    }
}