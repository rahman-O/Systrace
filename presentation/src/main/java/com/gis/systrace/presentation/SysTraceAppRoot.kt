package com.gis.systrace.presentation

import android.Manifest
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gis.systrace.core.common.DeviceSnapshot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SysTraceAppRoot(
    viewModel: DeviceSnapshotViewModel = viewModel(),
) {
    val snapshot by viewModel.snapshot.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Device", "Location")
    val icons = listOf(Icons.Default.Info, Icons.Default.LocationOn)

    val permissionsToRequest = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_PHONE_NUMBERS,
    )

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.refresh()
    }

    LaunchedEffect(Unit) {
        launcher.launch(permissionsToRequest)
        viewModel.refresh()
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text("SysTrace") },
                        actions = {
                            IconButton(onClick = { viewModel.refresh() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        },
                    )
                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title) },
                                icon = { Icon(icons[index], contentDescription = null) },
                            )
                        }
                    }
                }
            },
        ) { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                color = MaterialTheme.colorScheme.background,
            ) {
                snapshot?.let { data ->
                    when (selectedTab) {
                        0 -> DeviceSnapshotContent(data)
                        1 -> LocationTab(data)
                    }
                } ?: run {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(text = "Collecting device data...")
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
            .padding(16.dp),
    ) {
        item {
            InfoSection(title = "About Phone") {
                InfoRow("Device name", snapshot.userDeviceName ?: "N/A")
                InfoRow("Phone number", maskSensitive(snapshot.phoneNumber))
                InfoRow("SIM status", snapshot.simStatusSummary ?: "N/A")
                InfoRow("Model", "${snapshot.deviceManufacturer} ${snapshot.deviceModel}")
                InfoRow("Android version", snapshot.androidVersionDisplay ?: snapshot.androidVersion)
                InfoRow("Android security update", snapshot.securityPatch.ifBlank { "N/A" })
                InfoRow(
                    "Google Play system update",
                    snapshot.googlePlaySystemUpdate ?: "N/A",
                )
                InfoRow("Baseband version", snapshot.basebandVersion ?: snapshot.radioVersion ?: "N/A")
                InfoRow("Kernel version", snapshot.kernelVersion.ifBlank { "N/A" })
                InfoRow("Build number", snapshot.buildNumber)
            }
        }

        item {
            InfoSection(title = "Software Information") {
                InfoRow("Fingerprint", maskSensitive(snapshot.fingerprint, visibleTail = 8))
                InfoRow("Incremental", snapshot.buildIncremental ?: "N/A")
                InfoRow("Build type", snapshot.buildType ?: "N/A")
                InfoRow("Build tags", snapshot.buildTags ?: "N/A")
                InfoRow("SDK version", snapshot.sdkVersion.toString())
                InfoRow("Bootloader", snapshot.bootloader.ifBlank { "N/A" })
                InfoRow("Hardware", snapshot.hardware.ifBlank { "N/A" })
                InfoRow("Board", snapshot.board.ifBlank { "N/A" })
                InfoRow("Device", snapshot.deviceName.ifBlank { "N/A" })
                InfoRow("Product", snapshot.deviceProduct.ifBlank { "N/A" })
                InfoRow("Brand", snapshot.deviceBrand)
                InfoRow("SoC manufacturer", snapshot.socManufacturer ?: "N/A")
                InfoRow("SoC model", snapshot.socModel ?: "N/A")
                InfoRow("SKU", snapshot.sku ?: "N/A")
                snapshot.buildTimeEpochMillis?.let {
                    InfoRow("Build time", java.util.Date(it).toString())
                }
                InfoRow("Codename", snapshot.buildCodename ?: "N/A")
                InfoRow("Base OS", snapshot.baseOs ?: "N/A")
                InfoRow("Preview SDK", snapshot.previewSdkInt?.toString() ?: "N/A")
                if (snapshot.cpuAbi.isNotEmpty()) {
                    InfoRow("Supported ABIs", snapshot.cpuAbi.joinToString(", "))
                }
                if (snapshot.supported32BitAbis.isNotEmpty()) {
                    InfoRow("32-bit ABIs", snapshot.supported32BitAbis.joinToString(", "))
                }
                if (snapshot.supported64BitAbis.isNotEmpty()) {
                    InfoRow("64-bit ABIs", snapshot.supported64BitAbis.joinToString(", "))
                }
            }
        }

        item {
            InfoSection(title = "Hardware Information") {
                val totalRam = snapshot.totalRamBytes / (1024 * 1024)
                InfoRow("RAM", "$totalRam MB total")

                val intTotal = snapshot.internalStorageBytes / (1024 * 1024 * 1024)
                InfoRow("Internal storage", "$intTotal GB total")

                snapshot.externalStorageBytes?.let {
                    InfoRow("External storage", "${it / (1024 * 1024 * 1024)} GB total")
                }
                InfoRow("CPU model", snapshot.cpuModel ?: "N/A")
                InfoRow("CPU cores", snapshot.cpuCores.toString())
                InfoRow("GPU", snapshot.gpuRenderer ?: "N/A")
                InfoRow("Cameras", snapshot.cameraCount.toString())
                InfoRow("NFC", snapshot.nfcSupported.toString())
            }
        }

        item {
            InfoSection(title = "Device Identity") {
                InfoRow("Serial", snapshot.serialNumber ?: "N/A")
                InfoRow("Android ID", maskSensitive(snapshot.androidId))
                InfoRow("IMEI (slot 1)", maskSensitive(snapshot.imeiPrimary))
                InfoRow("IMEI (slot 2)", maskSensitive(snapshot.imeiSecondary))
                InfoRow("MEID", maskSensitive(snapshot.meid))
                InfoRow("ICCID", maskSensitive(snapshot.iccid))
                InfoRow("IMSI", maskSensitive(snapshot.imsi))
                snapshot.identifierAvailability?.let {
                    InfoRow("Restrictions", it)
                }
            }
        }

        item {
            InfoSection(title = "SIM Details") {
                if (snapshot.simSlots.isEmpty()) {
                    InfoRow("SIM", "No SIM data")
                } else {
                    snapshot.simSlots.forEach { slot ->
                        InfoRow("Slot ${slot.slotIndex}", slot.simState ?: "N/A")
                        InfoRow("  Operator", slot.simOperator ?: "N/A")
                        InfoRow("  Carrier", slot.carrierName ?: "N/A")
                        InfoRow("  MCC/MNC", "${slot.mcc ?: "?"}/${slot.mnc ?: "?"}")
                        InfoRow("  Country", slot.countryIso ?: "N/A")
                        InfoRow("  Network", slot.networkType ?: "N/A")
                        InfoRow("  Roaming", slot.isRoaming.toString())
                        InfoRow("  eSIM", slot.isEmbedded.toString())
                        slot.phoneNumber?.let { InfoRow("  Number", maskSensitive(it)) }
                    }
                }
                if (snapshot.hasTelephonyData) {
                    InfoRow("Carrier", snapshot.carrierName ?: "N/A")
                    InfoRow("SIM operator", snapshot.simOperatorName ?: "N/A")
                    InfoRow("MCC/MNC", "${snapshot.mcc ?: "?"}/${snapshot.mnc ?: "?"}")
                    InfoRow("Country ISO", snapshot.countryIso ?: "N/A")
                }
            }
        }

        item {
            InfoSection(title = "Display") {
                InfoRow("Resolution", "${snapshot.displayWidthPixels}x${snapshot.displayHeightPixels}")
                InfoRow("Refresh rate", "${snapshot.refreshRateHz} Hz")
                InfoRow("Screen size", "%.2f inches".format(snapshot.screenSizeInches))
                InfoRow("Density", "${snapshot.densityDpi} DPI")
            }
        }

        if (snapshot.systemProperties.isNotEmpty()) {
            item {
                InfoSection(title = "System Properties") {
                    snapshot.systemProperties.forEach { (key, value) ->
                        InfoRow(key, maskSensitive(value, visibleTail = 8))
                    }
                }
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
fun LocationTab(snapshot: DeviceSnapshot) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val lat = snapshot.lastKnownLatitude
        val lon = snapshot.lastKnownLongitude

        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier
                .height(80.dp)
                .width(80.dp),
            tint = if (lat != null) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Precise Coordinates",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
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
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Location Unavailable", color = MaterialTheme.colorScheme.error)
                    }
                    Text(
                        "Please grant location permissions and ensure GPS is enabled.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun InfoSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            value,
            modifier = Modifier.weight(1.5f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
