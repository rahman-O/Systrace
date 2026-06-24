package com.gis.systrace.presentation

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext

@Composable
fun ApplicationsScreen() {
    val context = LocalContext.current
    val pm = context.packageManager
    val packages = remember { pm.getInstalledApplications(PackageManager.GET_META_DATA).sortedBy { it.packageName } }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(packages) { app ->
            AppRow(app, pm)
        }
    }
}

@Composable
fun AppRow(app: ApplicationInfo, pm: PackageManager) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = app.loadLabel(pm).toString())
            Text(text = app.packageName)
            Text(text = if (app.flags and ApplicationInfo.FLAG_SYSTEM != 0) "System App" else "User App")
        }
    }
}
