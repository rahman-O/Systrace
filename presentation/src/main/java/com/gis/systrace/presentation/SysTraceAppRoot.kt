package com.gis.systrace.presentation

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gis.systrace.core.common.DeviceSnapshot
import com.gis.systrace.service.BatteryOptimizationHelper
import com.gis.systrace.service.ServiceStarter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SysTraceAppRoot(
    viewModel: DeviceSnapshotViewModel = viewModel()
) {
    val snapshot by viewModel.snapshot.collectAsState()
    val serviceRunning by viewModel.serviceRunning.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Apps", "Location")
    val icons = listOf(Icons.Default.Info, Icons.Default.List, Icons.Default.LocationOn)

    // Permission Handling
    val permissionsToRequest = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_PHONE_NUMBERS,
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }.toTypedArray()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        ServiceStarter.start(context)
        (context as? Activity)?.let { activity ->
            BatteryOptimizationHelper.requestExemption(activity)
        }
        viewModel.refresh()
        viewModel.updateServiceStatus()
    }

    LaunchedEffect(Unit) {
        ServiceStarter.start(context)
        launcher.launch(permissionsToRequest)
        viewModel.refresh()
        viewModel.updateServiceStatus()
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text("SysTrace - Pro")
                                Text(
                                    text = if (serviceRunning) {
                                        "Background: Running"
                                    } else {
                                        "Background: Stopped"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (serviceRunning) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                viewModel.refresh()
                                viewModel.updateServiceStatus()
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }
                    )
                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title) },
                                icon = { Icon(icons[index], contentDescription = null) }
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                color = MaterialTheme.colorScheme.background
            ) {
                snapshot?.let { data ->
                    when (selectedTab) {
                        0 -> DeviceSnapshotContent(data)
                        1 -> ApplicationsTab(data.installedApplications)
                        2 -> LocationTab(data)
                    }
                } ?: run {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "Extracting Full Device Report...")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { launcher.launch(permissionsToRequest) }) {
                            Text("Grant Permissions")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceSnapshotContent(snapshot: DeviceSnapshot) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            InfoSection(title = "Device Identity") {
                InfoRow("Serial", snapshot.serialNumber ?: "N/A")
                InfoRow("Android ID", maskSensitive(snapshot.androidId))
                InfoRow("IMEI (Slot 1)", maskSensitive(snapshot.imeiPrimary))
                InfoRow("IMEI (Slot 2)", maskSensitive(snapshot.imeiSecondary))
                InfoRow("MEID", maskSensitive(snapshot.meid))
                InfoRow("ICCID", maskSensitive(snapshot.iccid))
                InfoRow("IMSI", maskSensitive(snapshot.imsi))
                InfoRow("Phone Number", maskSensitive(snapshot.phoneNumber))
                snapshot.identifierAvailability?.let {
                    InfoRow("Restrictions", it)
                }
            }
        }

        item {
            InfoSection(title = "About Phone") {
                InfoRow("Device Name", snapshot.userDeviceName ?: "N/A")
                InfoRow("Brand", snapshot.deviceBrand)
                InfoRow("Product", snapshot.deviceProduct)
                InfoRow("Build Type", snapshot.buildType ?: "N/A")
                InfoRow("Build Tags", snapshot.buildTags ?: "N/A")
                InfoRow("Incremental", snapshot.buildIncremental ?: "N/A")
                InfoRow("Build Number", snapshot.buildNumber)
                InfoRow("Base OS", snapshot.baseOs ?: "N/A")
                InfoRow("Radio", snapshot.radioVersion ?: "N/A")
                InfoRow("Fingerprint", maskSensitive(snapshot.fingerprint, visibleTail = 8))
            }
        }

        item {
            InfoSection(title = "Device Specs") {
                InfoRow("Manufacturer", snapshot.deviceManufacturer)
                InfoRow("Model", snapshot.deviceModel)
                InfoRow("Android", snapshot.androidVersion)
                InfoRow("SDK", snapshot.sdkVersion.toString())
                InfoRow("Security Patch", snapshot.securityPatch)
                InfoRow("Kernel", snapshot.kernelVersion)
            }
        }

        item {
            InfoSection(title = "SIM & Cellular") {
                if (snapshot.simSlots.isEmpty()) {
                    InfoRow("SIM", "No SIM data")
                } else {
                    snapshot.simSlots.forEach { slot ->
                        InfoRow("Slot ${slot.slotIndex}", slot.simState ?: "N/A")
                        InfoRow("  Operator", slot.simOperator ?: "N/A")
                        InfoRow("  Carrier", slot.carrierName ?: "N/A")
                        InfoRow("  MCC/MNC", "${slot.mcc ?: "?"}/${slot.mnc ?: "?"}")
                        InfoRow("  Network", slot.networkType ?: "N/A")
                        InfoRow("  Roaming", slot.isRoaming.toString())
                        InfoRow("  eSIM", slot.isEmbedded.toString())
                    }
                }
            }
        }

        item {
            InfoSection(title = "Hardware Status") {
                val totalRam = snapshot.totalRamBytes / (1024 * 1024)
                val availRam = snapshot.availableRamBytes / (1024 * 1024)
                InfoRow("RAM", "$availRam MB free / $totalRam MB")
                
                val intFree = snapshot.freeStorageBytes / (1024 * 1024 * 1024)
                val intTotal = snapshot.internalStorageBytes / (1024 * 1024 * 1024)
                InfoRow("Internal Storage", "$intFree GB free / $intTotal GB")
                
                snapshot.externalStorageBytes?.let {
                    InfoRow("External Storage", "${it / (1024 * 1024 * 1024)} GB total")
                }
                InfoRow("CPU Model", snapshot.cpuModel ?: "N/A")
                InfoRow("CPU Cores", snapshot.cpuCores.toString())
                InfoRow("GPU", snapshot.gpuRenderer ?: "N/A")
                InfoRow("Cameras", snapshot.cameraCount.toString())
                InfoRow("NFC", snapshot.nfcSupported.toString())
            }
        }

        item {
            InfoSection(title = "System Info") {
                snapshot.uptimeMillis?.let {
                    val hours = it / (1000 * 60 * 60)
                    InfoRow("Uptime", "$hours hours")
                }
                InfoRow("Timezone", snapshot.timezone ?: "N/A")
                InfoRow("Locale", snapshot.locale ?: "N/A")
                InfoRow("Bluetooth", snapshot.bluetoothName ?: "N/A")
                InfoRow("BT Enabled", snapshot.bluetoothEnabled?.toString() ?: "N/A")
                InfoRow("GMS Version", snapshot.googlePlayServicesVersion ?: "N/A")
            }
        }

        item {
            InfoSection(title = "Battery & Power") {
                InfoRow("Level", "${snapshot.batteryPercentage}%")
                InfoRow("Status", snapshot.chargingState)
                InfoRow("Health", snapshot.batteryHealth)
                InfoRow("Temp", "${snapshot.batteryTemperatureCelsius}°C")
                InfoRow("Voltage", "${snapshot.batteryVoltageMillivolts} mV")
                InfoRow("Tech", snapshot.batteryTechnology)
            }
        }

        item {
            InfoSection(title = "Network Connectivity") {
                InfoRow("Presence", snapshot.onlineStatus)
                InfoRow("Screen Locked", snapshot.isScreenLocked.toString())
                InfoRow("Internet", if (snapshot.isInternetConnected) "Connected" else "Disconnected")
                snapshot.offlineReason?.let { InfoRow("Offline Reason", it) }
                InfoRow("Network", snapshot.networkType)
                InfoRow("Status", snapshot.connectionStatus)
                InfoRow("WiFi SSID", snapshot.wifiSsid ?: "N/A")
                InfoRow("IP Address", snapshot.ipAddresses.firstOrNull { !it.contains(":") } ?: "N/A")
                InfoRow("DNS", snapshot.dnsServers.take(2).joinToString(", "))
                InfoRow("Carrier", snapshot.carrierName ?: "N/A")
                InfoRow("VPN Active", snapshot.vpnDetected.toString())
            }
        }

        item {
            InfoSection(title = "Display Details") {
                InfoRow("Resolution", "${snapshot.displayWidthPixels}x${snapshot.displayHeightPixels}")
                InfoRow("Refresh Rate", "${snapshot.refreshRateHz} Hz")
                InfoRow("Screen Size", "%.2f inches".format(snapshot.screenSizeInches))
                InfoRow("Density", "${snapshot.densityDpi} DPI")
            }
        }

        item {
            InfoSection(title = "Security & MDM") {
                InfoRow("Device Owner", snapshot.deviceOwnerStatus)
                InfoRow("Is Device Owner", snapshot.isDeviceOwner.toString())
                InfoRow("Is Profile Owner", snapshot.isProfileOwner.toString())
                InfoRow("Owner Component", snapshot.deviceOwnerComponent ?: "N/A")
                InfoRow("Work Profile", snapshot.isWorkProfile.toString())
                InfoRow("Encrypted", snapshot.isEncrypted?.toString() ?: "N/A")
                InfoRow("Encryption", snapshot.encryptionStatus ?: "N/A")
                InfoRow("Screen Lock", snapshot.screenLockType ?: "N/A")
                InfoRow("OEM Unlocked", snapshot.isOemUnlocked?.toString() ?: "N/A")
                InfoRow("Rooted", snapshot.rootDetected.toString())
                InfoRow("Emulator", snapshot.emulatorDetected.toString())
                InfoRow("ADB Enabled", snapshot.usbDebuggingEnabled.toString())
                InfoRow("Dev Options", snapshot.developerOptionsEnabled.toString())
                InfoRow("Widevine", snapshot.widevineLevel ?: "N/A")
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun maskSensitive(value: String?, visibleTail: Int = 4): String {
    if (value.isNullOrBlank()) return "N/A"
    if (value.length <= visibleTail) return value
    return "****${value.takeLast(visibleTail)}"
}

@Composable
fun ApplicationsTab(apps: List<DeviceSnapshot.AppDetail>) {
    Column {
        Text(
            "Installed Apps (${apps.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold
        )
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            items(apps.sortedBy { it.packageName }) { app ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(app.packageName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Ver: ${app.versionName ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LocationTab(snapshot: DeviceSnapshot) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val lat = snapshot.lastKnownLatitude
        val lon = snapshot.lastKnownLongitude

        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.height(80.dp).width(80.dp),
            tint = if (lat != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Precise Coordinates", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                if (lat != null && lon != null) {
                    InfoRow("Latitude", "%.6f".format(lat))
                    InfoRow("Longitude", "%.6f".format(lon))
                    InfoRow("Accuracy", "${snapshot.locationAccuracyMeters} meters")
                    InfoRow("Provider", snapshot.locationProvider?.uppercase() ?: "N/A")
                    val date = java.util.Date(snapshot.locationTimestampEpochMillis ?: 0L)
                    InfoRow("Timestamp", date.toString())
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Location Unavailable", color = MaterialTheme.colorScheme.error)
                    }
                    Text(
                        "Please grant location permissions and ensure GPS is enabled.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InfoSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(value, modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
