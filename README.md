# WiiM Universal Hubitat Driver

A comprehensive Hubitat driver for WiiM devices including WiiM Mini, WiiM Pro, WiiM Pro Plus, WiiM Ultra, WiiM Amp, and WiiM Amp Pro.

## Features

- **Universal Compatibility**: Works with all WiiM devices (Mini, Pro, Pro Plus, Ultra, Amp, Amp Pro)
- **Full Media Control**: Play, pause, stop, next/previous track, volume control, mute
- **Source Management**: Switch between WiFi, Bluetooth, Line-in, Optical, Coaxial, USB, and more
- **Preset Support**: Control up to 10 device presets
- **Multiroom Audio**: Join/leave multiroom groups, control master/slave relationships
- **Album Art Display**: Visual album art with track information in device states
- **USB Playback**: Play local USB files and manage USB playlists
- **URL/M3U Support**: Play direct URLs and M3U playlists
- **Advanced Features**: Seeking, loop modes, prompt sounds, device reboot
- **Real-time Status**: Automatic polling with configurable intervals
- **SSL Support**: Handles self-signed certificates with HTTPS/HTTP fallback

## Installation

### Method 1: Hubitat Package Manager (Recommended)
1. In Hubitat, go to **Apps** → **Add Built-In App**
2. Search for "WiiM Universal" and install

### Method 2: Manual Installation
1. In Hubitat, go to **Drivers Code**
2. Click **+ New Driver**
3. Copy and paste the contents of `WiiM-Universal.groovy`
4. Click **Save**
5. Go to **Devices** → **+ Add Device**
6. Select **WiiM Universal** as the driver
7. Configure the device IP address and other settings

## Configuration

### Required Settings
- **Device IP Address**: The IP address of your WiiM device
- **Use HTTPS**: Enable if your device uses HTTPS (recommended)

### Optional Settings
- **Status Polling Interval**: How often to check device status (30s to 5min, or disabled)
- **Volume Step**: Amount to change volume with volume up/down commands (1-25)
- **Album Art Display Size**: Size of album art in device states (100-300px)
- **Debug/Info Logging**: Enable detailed logging for troubleshooting

## Usage

### Basic Controls
- **Play/Pause/Stop**: Standard media transport controls
- **Volume Control**: Set volume (0-100) or use volume up/down
- **Mute**: Toggle mute state
- **Next/Previous**: Navigate tracks

### Source Switching
Use the `switchToSource` command with any of these sources:
- `wifi` - WiFi streaming
- `bluetooth` or `bt` - Bluetooth audio
- `linein` or `aux` - Line-in input
- `optical` - Optical input
- `coaxial` - Coaxial input
- `usb` or `udisk` - USB storage
- `usbdac` - USB DAC

### Presets
Control device presets (1-10) using the `playPreset` command.

### Multiroom Audio
- `joinMultiroomGroup(hostIP)` - Join a multiroom group
- `leaveMultiroomGroup()` - Leave current group
- `getMultiroomStatus()` - Check multiroom status

### Advanced Features
- `playURL(url)` - Play direct URL
- `playM3U(url)` - Play M3U playlist
- `seekTo(seconds)` - Seek to position in track
- `setLoopMode(mode)` - Set loop mode (0-5)
- `playPromptSound(url)` - Play notification sound
- `reboot()` - Reboot device

## Device Attributes

The driver provides comprehensive device state information:

### Playback
- `playbackStatus` - Current playback state (play/pause/stop/buffering)
- `currentTrack` - Current track title
- `currentArtist` - Current artist
- `currentAlbum` - Current album
- `trackPosition` - Current position (MM:SS format)
- `trackDuration` - Total duration (MM:SS format)

### Device Info
- `deviceName` - Device name
- `deviceModel` - Device model
- `deviceVersion` - Firmware version
- `wifiStrength` - WiFi signal strength
- `connectionStatus` - Connection status

### Media
- `albumArt` - Album art URL
- `albumArtHtml` - Formatted album art with track info
- `currentSource` - Current audio source
- `availableSources` - List of available sources
- `loopMode` - Current loop mode

### Multiroom
- `multiroomRole` - Device role (standalone/master/slave)
- `multiroomSlaves` - Number of slave devices

## Troubleshooting

### Connection Issues
1. Verify the device IP address is correct
2. Check if HTTPS is enabled/disabled appropriately
3. Ensure the device is on the same network as Hubitat
4. Try rebooting the WiiM device

### SSL Certificate Issues
The driver automatically handles self-signed certificates and will fall back to HTTP if HTTPS fails.

### Debug Logging
Enable debug logging in device preferences to see detailed command/response information.

## Changelog

### Version 1.09 (2025-06-25)
- Removed TuneIn, iHeartRadio, and Spotify functionality to rely on presets
- Improved stability and error handling

### Version 1.08 (2025-06-24)
- Fixed BigDecimal modulo error in formatTime function
- Added better error handling for track position/duration
- Handle live streams with totlen=0

### Version 1.07 (2025-06-24)
- Fixed album art retrieval (getArt command not supported)
- Improved error handling and null checking
- Album art now retrieved via getPlayerStatus

### Version 1.06 (2025-06-24)
- Added albumArtHtml attribute for visual album art display
- Configurable image size
- Enhanced now playing card with track info

### Version 1.05 (2025-06-24)
- Added album art support with albumArt attribute
- Enhanced now playing information parsing

### Version 1.04 (2025-06-24)
- Added playIHeartRadio and playSpotify commands
- Support for station IDs, URIs, URLs, and intelligent format detection

### Version 1.03 (2025-06-24)
- Added playTuneInStation command
- Support for station IDs, URLs, and station name searches

### Version 1.02 (2025-06-24)
- Updated for universal WiiM device compatibility
- Added source availability detection
- Improved error handling for unsupported features

### Version 1.01 (2025-06-24)
- Added comprehensive API support
- URL/M3U playback, prompt sounds, seeking, loop modes
- Multiroom control, USB playback, hex string conversion
- Enhanced status parsing and source switching

### Version 1.00 (2025-06-24)
- Initial release with basic playback controls
- Volume control, preset selection, status polling
- SSL certificate handling

## License

Copyright 2025 Simon Mason

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at:

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues and questions:
1. Check the troubleshooting section above
2. Enable debug logging and check the logs
3. Open an issue on GitHub with detailed information

## Acknowledgments

Based on WiiM/LinkPlay HTTP API documentation and Home Assistant integration. 