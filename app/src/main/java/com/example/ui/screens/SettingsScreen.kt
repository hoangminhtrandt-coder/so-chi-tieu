package com.example.ui.screens

import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.export.ExcelReportExporter
import com.example.export.PdfReportExporter
import com.example.reminder.DailyReminderReceiver
import com.example.reminder.ReminderManager
import com.example.security.BiometricAuthManager
import com.example.sync.SyncState
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.viewmodel.FinanceUiState
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.TimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    uiState: FinanceUiState,
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var isReminderOn by remember { mutableStateOf(ReminderManager.isReminderEnabled(context)) }
    var reminderTime by remember { mutableStateOf(ReminderManager.getReminderTime(context)) }

    var isLockOn by remember { mutableStateOf(uiState.isSecurityLockEnabled) }
    var isBioOn by remember { mutableStateOf(uiState.isBiometricEnabled) }
    var showPinDialog by remember { mutableStateOf(false) }

    var showSyncKeyDialog by remember { mutableStateOf(false) }
    var newSyncKeyInput by remember { mutableStateOf(uiState.syncId) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        item {
            Column {
                Text(
                    text = "Cài Đặt & Tiện Ích",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Bảo mật sinh trắc, nhắc nhở, đồng bộ đa thiết bị",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. BIOMETRIC SECURITY SECTION
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("security_settings_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = EmeraldPrimary.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Bảo Mật & Khóa Ứng Dụng",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Switch 1: Enable App Lock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bảo vệ ứng dụng bằng khóa",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Yêu cầu xác thực khi mở Sổ Chi Tiêu",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isLockOn,
                            onCheckedChange = { checked ->
                                if (checked && !uiState.hasPin) {
                                    // Must set PIN first
                                    showPinDialog = true
                                } else {
                                    isLockOn = checked
                                    viewModel.setSecurityLock(checked)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary),
                            modifier = Modifier.testTag("app_lock_switch")
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Switch 2: Biometric (Fingerprint / Face)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Vân tay / Khuôn mặt",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "Xác thực sinh trắc học nhanh chóng và an toàn",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isBioOn,
                            onCheckedChange = { checked ->
                                isBioOn = checked
                                viewModel.setBiometric(checked)
                            },
                            enabled = isLockOn,
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary),
                            modifier = Modifier.testTag("biometric_switch")
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // PIN Code Setup/Change Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (uiState.hasPin) "Mã PIN 4 số (Đã thiết lập)" else "Mã PIN dự phòng (Chưa đặt)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Dùng để mở khóa khi sinh trắc học không khả dụng",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = { showPinDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("set_pin_button")
                        ) {
                            Text(if (uiState.hasPin) "Đổi PIN" else "Đặt PIN")
                        }
                    }
                }
            }
        }

        // 2. DAILY REMINDER SECTION
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reminder_settings_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = EmeraldPrimary.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Nhắc Nhở Chi Tiêu Hàng Ngày",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Switch: Daily Reminder
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bật thông báo nhắc nhở",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Nhắc ghi chép chi tiêu mỗi tối để không bỏ sót",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isReminderOn,
                            onCheckedChange = { checked ->
                                isReminderOn = checked
                                ReminderManager.setReminder(context, checked, reminderTime.first, reminderTime.second)
                                Toast.makeText(
                                    context,
                                    if (checked) "Đã bật nhắc nhở hàng ngày lúc ${String.format(Locale.US, "%02d:%02d", reminderTime.first, reminderTime.second)}" else "Đã tắt nhắc nhở",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary),
                            modifier = Modifier.testTag("reminder_switch")
                        )
                    }

                    if (isReminderOn) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // Time Picker Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Giờ nhắc nhở:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = String.format(Locale.US, "%02d:%02d mỗi ngày", reminderTime.first, reminderTime.second),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }

                            Button(
                                onClick = {
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            reminderTime = Pair(hourOfDay, minute)
                                            ReminderManager.setReminder(context, true, hourOfDay, minute)
                                            Toast.makeText(
                                                context,
                                                "Đã đổi giờ nhắc thành ${String.format(Locale.US, "%02d:%02d", hourOfDay, minute)}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        reminderTime.first,
                                        reminderTime.second,
                                        true
                                    ).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.testTag("change_reminder_time_button")
                            ) {
                                Text("Đổi giờ")
                            }
                        }
                    }
                }
            }
        }

        // 3. MULTI-DEVICE CLOUD SYNC SECTION
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sync_settings_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = EmeraldPrimary.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Đồng Bộ Đa Thiết Bị",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Nhập cùng Mã Đồng Bộ trên các điện thoại/máy tính bảng khác nhau để dùng chung dữ liệu sổ chi tiêu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sync Code Display & Copy Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Mã đồng bộ hiện tại:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = uiState.syncId,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("SyncID", uiState.syncId))
                                        Toast.makeText(context, "Đã sao chép mã đồng bộ!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("copy_sync_id_button")
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Sao chép")
                                }

                                TextButton(
                                    onClick = {
                                        newSyncKeyInput = uiState.syncId
                                        showSyncKeyDialog = true
                                    }
                                ) {
                                    Text("Thay đổi")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Status and Sync Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val lastSyncStr = if (uiState.lastSyncTime > 0)
                                SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(uiState.lastSyncTime))
                            else "Chưa đồng bộ"
                            Text(
                                text = "Lần cuối: $lastSyncStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            when (val state = uiState.syncState) {
                                is SyncState.Syncing -> {
                                    Text("Đang đồng bộ...", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                                }
                                is SyncState.Success -> {
                                    Text("Đồng bộ thành công", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                                }
                                is SyncState.Error -> {
                                    Text(state.message, style = MaterialTheme.typography.labelSmall, color = ExpenseRed, maxLines = 1)
                                }
                                else -> {}
                            }
                        }

                        Button(
                            onClick = { viewModel.triggerCloudSync() },
                            enabled = uiState.syncState !is SyncState.Syncing,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.testTag("sync_now_button")
                        ) {
                            if (uiState.syncState is SyncState.Syncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Đồng bộ ngay")
                        }
                    }
                }
            }
        }

        // 4. EXPORT CENTER
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_center_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Xuất Dữ Liệu Báo Cáo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Xuất file PDF hoặc Excel để in ấn, gửi email hoặc phân tích chi tiết.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val file = viewModel.exportPdfReport()
                                if (file != null) PdfReportExporter.sharePdf(context, file)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Báo cáo PDF")
                        }

                        Button(
                            onClick = {
                                val file = viewModel.exportExcelReport()
                                if (file != null) ExcelReportExporter.shareExcel(context, file)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("File Excel (.csv)")
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    // Set/Change PIN Dialog
    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        var pinConfirm by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Thiết Lập Mã PIN 4 Số", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Nhập mã số gồm 4 chữ số để khóa và bảo vệ dữ liệu sổ chi tiêu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinInput = it },
                        label = { Text("Nhập mã PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("pin_input_field")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinConfirm,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinConfirm = it },
                        label = { Text("Xác nhận mã PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("pin_confirm_field")
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = errorMessage!!, color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length != 4) {
                            errorMessage = "Mã PIN phải gồm đúng 4 chữ số."
                        } else if (pinInput != pinConfirm) {
                            errorMessage = "Mã xác nhận không khớp."
                        } else {
                            viewModel.setPinCode(pinInput)
                            viewModel.setSecurityLock(true)
                            isLockOn = true
                            showPinDialog = false
                            Toast.makeText(context, "Đã thiết lập mã PIN bảo mật thành công!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Lưu PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Hủy") }
            }
        )
    }

    // Change Sync Key Dialog
    if (showSyncKeyDialog) {
        AlertDialog(
            onDismissRequest = { showSyncKeyDialog = false },
            title = { Text("Nhập Mã Đồng Bộ Mới", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Nhập mã đồng bộ từ thiết bị khác để liên kết và tải dữ liệu thu chi về máy này.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newSyncKeyInput,
                        onValueChange = { newSyncKeyInput = it.uppercase() },
                        label = { Text("Mã đồng bộ (VD: VN-4921-AZ)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("sync_id_input_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSyncKeyInput.isNotBlank()) {
                            viewModel.updateSyncId(newSyncKeyInput.trim())
                            showSyncKeyDialog = false
                            Toast.makeText(context, "Đã cập nhật mã đồng bộ!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Kết Nối")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyncKeyDialog = false }) { Text("Hủy") }
            }
        )
    }
}
