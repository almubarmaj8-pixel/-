package com.example.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Alarm
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.random.Random

// Astrological Color Palette
val AstralDarkBg = Color(0xFF09071A)
val AstralSurface = Color(0xFF131030)
val AstralCard = Color(0xFF1B1740)
val AstralPrimary = Color(0xFF8B5CF6)
val AstralSecondary = Color(0xFF10B981)
val AstralAccent = Color(0xFFF59E0B)
val AstralMuted = Color(0xFF9CA3AF)
val TextLight = Color(0xFFF9FAFB)

@Composable
fun AlarmScreen(viewModel: AlarmViewModel) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val ringingAlarm by viewModel.ringingAlarm.collectAsStateWithLifecycle()
    val nextAlarmText by viewModel.nextAlarmText.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Alarms, 1 = Practice, 2 = Stats
    var showAddEditDialog by remember { mutableStateOf(false) }
    var alarmToEdit by remember { mutableStateOf<Alarm?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(AstralDarkBg, Color(0xFF120E2C))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Main Top Bar
            HeaderSection()

            // Next Alarm Countdown Summary Banner
            nextAlarmText?.let { infoText ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = AstralSurface.copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "المنبه القادم",
                                    tint = AstralPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = infoText,
                                    color = TextLight,
                                    fontSize = 13.sp,
                                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
                                )
                            }
                        }

                        // Instant test button
                        Button(
                            onClick = { viewModel.testAlarmInFiveSeconds() },
                            colors = ButtonDefaults.buttonColors(containerColor = AstralPrimary.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("test_5s_button")
                        ) {
                            Text("جرب ٥ ثوانِ", color = AstralPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Main Tab Screens
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    0 -> AlarmsTab(
                        alarms = alarms,
                        onToggle = { viewModel.toggleAlarm(it) },
                        onDelete = { viewModel.deleteAlarm(it) },
                        onEdit = {
                            alarmToEdit = it
                            showAddEditDialog = true
                        }
                    )
                    1 -> ChallengesTab(viewModel)
                    2 -> StatsTab(viewModel)
                }
            }

            // Custom M3 RTL Navigation Bar
            CustomBottomBar(
                activeTab = activeTab,
                onTabSelected = { activeTab = it }
            )
        }

        // Floating Action Button for adding alarm
        if (activeTab == 0) {
            FloatingActionButton(
                onClick = {
                    alarmToEdit = null
                    showAddEditDialog = true
                },
                containerColor = AstralPrimary,
                contentColor = TextLight,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = 76.dp) // Offset navigation bar
                    .size(56.dp)
                    .testTag("add_alarm_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة منبه")
            }
        }

        // Alarm Edit/Add Dialog Screen
        if (showAddEditDialog) {
            AddEditAlarmDialog(
                alarm = alarmToEdit,
                onDismiss = { showAddEditDialog = false },
                onSave = { h, m, days, label, vib, gentle, puzzle, puzzleDiff, snooze ->
                    if (alarmToEdit == null) {
                        viewModel.addAlarm(h, m, days, label, vib, gentle, puzzle, puzzleDiff, snooze)
                    } else {
                        viewModel.updateAlarm(
                            alarmToEdit!!.copy(
                                hour = h,
                                minute = m,
                                daysToRepeat = days,
                                label = label,
                                isVibrate = vib,
                                isGentleWakeUp = gentle,
                                puzzleType = puzzle,
                                puzzleDifficulty = puzzleDiff,
                                snoozeTimeMinutes = snooze,
                                isEnabled = true // Auto enable on edit
                            )
                        )
                    }
                    showAddEditDialog = false
                }
            )
        }

        // Active Ringing Alarm Overriding Dialog
        ringingAlarm?.let { alarm ->
            ActiveRingingOverlay(
                alarm = alarm,
                onDismiss = { viewModel.dismissRinging(alarm.id) },
                onSnooze = { viewModel.snoozeRinging(alarm.id) }
            )
        }
    }
}

// Global RTL Safe Header
@Composable
fun HeaderSection() {
    var currentTimeString by remember { mutableStateOf("") }
    var currentDateString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            currentTimeString = SimpleDateFormat("hh:mm a", Locale("ar")).format(now.time)
            currentDateString = SimpleDateFormat("EEEE، d MMMM", Locale("ar")).format(now.time)
            delay(1000)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Identity Logo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(AstralPrimary.copy(alpha = 0.2f), CircleShape)
                    .border(1.dp, AstralPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = AstralPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "المنبه الذكي",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextLight
                )
                Text(
                    text = "صحة ويقظة تامة",
                    fontSize = 11.sp,
                    color = AstralMuted
                )
            }
        }

        // Live clock digital view
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = currentTimeString,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextLight
            )
            Text(
                text = currentDateString,
                fontSize = 11.sp,
                color = AstralMuted
            )
        }
    }
}

@Composable
fun AlarmsTab(
    alarms: List<Alarm>,
    onToggle: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit,
    onEdit: (Alarm) -> Unit
) {
    if (alarms.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(AstralCard, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = AstralMuted,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "لا توجد منبهات مفعّلة حالياً",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "سجل منبهاتك الصباحية وقم بحل الألغاز والمسائل الذكية الرياضية لتضمن استيقاظك بهمة ونشاط!",
                fontSize = 12.sp,
                color = AstralMuted,
                textAlign = TextAlign.Center
            )
        }
    } else {
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 84.dp)
            ) {
                items(alarms) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onToggle = { onToggle(alarm) },
                        onDelete = { onDelete(alarm) },
                        onEdit = { onEdit(alarm) }
                    )
                }
            }
        }
    }
}

@Composable
fun AlarmCard(
    alarm: Alarm,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }
            .testTag("alarm_card_${alarm.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.isEnabled) AstralCard else AstralCard.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Clock time and toggle row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = alarm.getFormattedTime(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = if (alarm.isEnabled) TextLight else AstralMuted
                        )
                    }
                    if (alarm.label.isNotEmpty()) {
                        Text(
                            text = alarm.label,
                            color = if (alarm.isEnabled) AstralPrimary else AstralMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextLight,
                        checkedTrackColor = AstralPrimary,
                        uncheckedThumbColor = AstralMuted,
                        uncheckedTrackColor = AstralSurface
                    ),
                    modifier = Modifier.testTag("alarm_switch_${alarm.id}")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "التكرار",
                        tint = AstralMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = alarm.getRepeatDaysFormatted(),
                        fontSize = 12.sp,
                        color = AstralMuted
                    )
                }

                // Show active puzzle tag
                if (alarm.puzzleType != "NONE") {
                    val puzzleLabel = if (alarm.puzzleType == "MATH") "تحدي الحساب 🧮" else "تحدي الحركة 📱"
                    val difficultyLabel = when (alarm.puzzleDifficulty) {
                        "EASY" -> "سهل"
                        "MEDIUM" -> "متوسط"
                        else -> "صعب"
                    }
                    Box(
                        modifier = Modifier
                            .background(AstralPrimary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .border(0.5.dp, AstralPrimary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "$puzzleLabel ($difficultyLabel)",
                            color = AstralPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Quick action editing layout when expanded
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = AstralSurface, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        // Edit button
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .background(AstralSurface, CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تعديل",
                                tint = AstralPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Delete button
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChallengesTab(viewModel: AlarmViewModel) {
    // A laboratory tab where users can practice solving wake up challenges
    val context = LocalContext.current
    var practiceType by remember { mutableStateOf("MATH") } // MATH or SHAKE
    var practiceDifficulty by remember { mutableStateOf("EASY") }
    var practiceRunning by remember { mutableStateOf(false) }

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 84.dp)
        ) {
            item {
                Text(
                    text = "تحديات الاستيقاظ واليقظة 🤯",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextLight
                )
                Text(
                    text = "العقل النامي لا يقع في فخ 'الغفوة المستمرة'. اختر التحدي لتعود لوعيك وقدراتك العقلية فور إرسال المنبه.",
                    fontSize = 12.sp,
                    color = AstralMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AstralSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "تخصيص اللغز للتدريب الصباحي",
                            color = TextLight,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Selection Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (practiceType == "MATH") AstralPrimary else AstralCard,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { practiceType = "MATH" }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = TextLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("مسألة رياضيات", color = TextLight, fontSize = 12.sp)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (practiceType == "SHAKE") AstralPrimary else AstralCard,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { practiceType = "SHAKE" }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = TextLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("هز الهاتف بقوة", color = TextLight, fontSize = 12.sp)
                                }
                            }
                        }

                        if (practiceType == "MATH") {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("درجة صعوبة المسائل الرياضية:", color = AstralMuted, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("EASY" to "سهل جداً", "MEDIUM" to "متوسط", "HARD" to "عبقري 🧠").forEach { (v, name) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (practiceDifficulty == v) AstralPrimary.copy(alpha = 0.2f) else AstralCard,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (practiceDifficulty == v) AstralPrimary else Color.Transparent,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable { practiceDifficulty = v }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(name, color = TextLight, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { practiceRunning = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AstralSecondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ابدأ تجربة ذكية للغز الآن 🚀", color = TextLight, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AstralSurface.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "هل تود تجربة النظام الحقيقي؟",
                            color = TextLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "انقر على زر 'منبه ٥ ثوانِ' في الأعلى، ثم أغلق التطبيق أو ابقه مفتوحاً. سينطلق منبه حقيقي ويطلق صوتاً لتجربة الحسم بالتفصيل!",
                            color = AstralMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    if (practiceRunning) {
        Dialog(
            onDismissRequest = { practiceRunning = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AstralDarkBg)
            ) {
                ActiveChallengeView(
                    label = "تدريب على المنبه",
                    puzzleType = practiceType,
                    difficulty = practiceDifficulty,
                    onSolved = { practiceRunning = false },
                    onSnoozeEnabled = false,
                    onSnooze = {}
                )
            }
        }
    }
}

@Composable
fun StatsTab(viewModel: AlarmViewModel) {
    // Gamified early awakening statistics and analytics report
    val totalWakeups by viewModel.statsWakeups.collectAsStateWithLifecycle()
    val totalSnoozes by viewModel.statsSnoozes.collectAsStateWithLifecycle()
    val streak by viewModel.statsStreak.collectAsStateWithLifecycle()

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 84.dp)
        ) {
            item {
                Text(
                    text = "سجل الإيقاظ والنشاط الحالي 📈",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextLight
                )
                Text(
                    text = "يقيس التطبيق كفاءة استيقاظك بدقة لمنع الخمول والنوم المفرط.",
                    fontSize = 12.sp,
                    color = AstralMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Streak card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AstralPrimary.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, AstralPrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("سلسلة حسم الاستيقاظ المتتالية", color = AstralMuted, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$streak أيام متواصلة 🔥", color = TextLight, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("رائع! أنت متقيد بوعيك وتحل المسائل فوراً.", color = AstralSecondary, fontSize = 11.sp)
                        }

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(AstralPrimary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔥", fontSize = 28.sp)
                        }
                    }
                }
            }

            // Stats row cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = AstralCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AstralSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("مرّات الاستيقاظ دون غفو", color = AstralMuted, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$totalWakeups مرّة", color = TextLight, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = AstralCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = AstralAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("مرّات طلب الغفو (متراكم)", color = AstralMuted, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$totalSnoozes مرّات", color = TextLight, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Quality of Awake Ring Circle
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AstralSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "معدل الاستيقاظ بدون استخدام الغفوة",
                            color = TextLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(140.dp)
                        ) {
                            val ratio = if (totalWakeups + totalSnoozes == 0) 1.0f else totalWakeups.toFloat() / (totalWakeups + totalSnoozes)
                            val percent = (ratio * 100).toInt()
                            
                            Canvas(modifier = Modifier.size(120.dp)) {
                                drawCircle(
                                    color = AstralCard,
                                    style = Stroke(width = 12.dp.toPx())
                                )
                                drawArc(
                                    color = AstralPrimary,
                                    startAngle = -90f,
                                    sweepAngle = ratio * 360f,
                                    useCenter = false,
                                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$percent%", color = TextLight, fontSize = 24.sp, fontWeight = FontWeight.Black)
                                Text("معدل نجاح", color = AstralMuted, fontSize = 10.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "كلما ارتفع هذا المعدل، زادت صحة دورتك البيولوجية الصباحية اليومية.",
                            color = AstralMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// Dialog to add or edit precise alarms
@Composable
fun AddEditAlarmDialog(
    alarm: Alarm? = null,
    onDismiss: () -> Unit,
    onSave: (hour: Int, minute: Int, daysToRepeat: String, label: String, vibrate: Boolean, gentle: Boolean, puzzleType: String, puzzleDiff: String, snoozeMins: Int) -> Unit
) {
    var selectedHour by remember { mutableStateOf(alarm?.hour ?: 7) }
    var selectedMinute by remember { mutableStateOf(alarm?.minute ?: 0) }
    var selectedLabel by remember { mutableStateOf(alarm?.label ?: "") }
    var selectedVibrate by remember { mutableStateOf(alarm?.isVibrate ?: true) }
    var selectedGentle by remember { mutableStateOf(alarm?.isGentleWakeUp ?: true) }
    var selectedPuzzleType by remember { mutableStateOf(alarm?.puzzleType ?: "NONE") } // "NONE", "MATH", "SHAKE"
    var selectedPuzzleDiff by remember { mutableStateOf(alarm?.puzzleDifficulty ?: "MEDIUM") }
    var selectedSnooze by remember { mutableStateOf(alarm?.snoozeTimeMinutes ?: 5) }

    // Represent repeat days (0 = Saturday, 1 = Sunday, etc.)
    val initialDaysSet = remember {
        alarm?.daysToRepeat?.split(",")?.filter { it.isNotEmpty() }?.mapNotNull { it.toIntOrNull() }?.toMutableStateList()
            ?: mutableStateListOf()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = AstralSurface
        ) {
            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = if (alarm == null) "إضافة منبه ذكي جديد" else "تعديل المنبه الذكي",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Gorgeous custom styled spinners for hour and minute selecting
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AstralSurface, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hour Picker
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = { selectedHour = (selectedHour + 1) % 24 },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "زيادة الساعة", tint = TextLight)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(AstralCard, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = String.format("%02d", selectedHour),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        color = AstralPrimary
                                    )
                                }
                                IconButton(
                                    onClick = { selectedHour = if (selectedHour == 0) 23 else selectedHour - 1 },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "تقليل الساعة", tint = TextLight)
                                }
                                Text("الساعة", color = AstralMuted, fontSize = 10.sp)
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = ":",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = TextLight,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Minute Picker
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = { selectedMinute = (selectedMinute + 1) % 60 },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "زيادة الدقيقة", tint = TextLight)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(AstralCard, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = String.format("%02d", selectedMinute),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        color = AstralPrimary
                                    )
                                }
                                IconButton(
                                    onClick = { selectedMinute = if (selectedMinute == 0) 59 else selectedMinute - 1 },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "تقليل الدقيقة", tint = TextLight)
                                }
                                Text("الدقيقة", color = AstralMuted, fontSize = 10.sp)
                            }

                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // AM/PM calculation hint indicator
                            val amPmStr = if (selectedHour < 12) "صباحاً" else "مساءً"
                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .background(AstralPrimary.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(amPmStr, color = AstralPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Label tag selector
                    item {
                        OutlinedTextField(
                            value = selectedLabel,
                            onValueChange = { selectedLabel = it },
                            placeholder = { Text("المسمى، مثلاً: الاستيقاظ للعمل", color = AstralMuted) },
                            label = { Text("اسم المنبه", color = AstralPrimary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = AstralCard,
                                unfocusedContainerColor = AstralCard,
                                focusedBorderColor = AstralPrimary,
                                unfocusedBorderColor = AstralSurface,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Days of weeks selecting buttons
                    item {
                        Text("أيام تكرار المنبه:", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        val dayNames = listOf("س", "ح", "ن", "ث", "ر", "خ", "ج") // Sat, Sun, Mon, Tue, Wed, Thu, Fri
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            dayNames.forEachIndexed { idx, char ->
                                val active = initialDaysSet.contains(idx)
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            if (active) AstralPrimary else AstralCard,
                                            CircleShape
                                        )
                                        .border(
                                            1.dp,
                                            if (active) AstralPrimary else AstralSurface,
                                            CircleShape
                                        )
                                        .clickable {
                                            if (active) initialDaysSet.remove(idx) else initialDaysSet.add(idx)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        char,
                                        color = if (active) TextLight else AstralMuted,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Puzzle challenges choice
                    item {
                        Text("لغز حتمية الاستيقاظ (يضمن عدم الغفوة):", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("NONE" to "بدون ⚠️", "MATH" to "حساب 🧮", "SHAKE" to "هز الهاتف 📱").forEach { (type, text) ->
                                val active = selectedPuzzleType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (active) AstralPrimary else AstralCard,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedPuzzleType = type }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text, color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (selectedPuzzleType == "MATH") {
                        item {
                            Text("صعوبة لغز الحساب:", color = AstralMuted, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("EASY" to "سهل جداً", "MEDIUM" to "متوسط", "HARD" to "عبقري 🤯").forEach { (diff, name) ->
                                    val active = selectedPuzzleDiff == diff
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (active) AstralPrimary.copy(alpha = 0.2f) else AstralCard,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (active) AstralPrimary else Color.Transparent,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable { selectedPuzzleDiff = diff }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(name, color = TextLight, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Snooze selection row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("مدة غفوة التأجيل:", color = TextLight, fontSize = 12.sp)
                            Row(
                                modifier = Modifier.background(AstralCard, RoundedCornerShape(8.dp)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(1, 3, 5, 10).forEach { mins ->
                                    val active = selectedSnooze == mins
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (active) AstralPrimary else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedSnooze = mins }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("$mins د", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Switch switches
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تشغيل الاهتزاز", color = TextLight, fontSize = 12.sp)
                            Switch(
                                checked = selectedVibrate,
                                onCheckedChange = { selectedVibrate = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = AstralPrimary)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("صوت متصاعد تدريجياً", color = TextLight, fontSize = 12.sp)
                            Switch(
                                checked = selectedGentle,
                                onCheckedChange = { selectedGentle = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = AstralPrimary)
                            )
                        }
                    }

                    // Footer Buttons
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val daysStr = initialDaysSet.sorted().joinToString(",")
                                    onSave(selectedHour, selectedMinute, daysStr, selectedLabel, selectedVibrate, selectedGentle, selectedPuzzleType, selectedPuzzleDiff, selectedSnooze)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AstralPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_alarm_button")
                            ) {
                                Text("حفظ الُمَنبّه ✅", color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = AstralCard),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("إلغاء", color = AstralMuted, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Custom bottom tab layout
@Composable
fun CustomBottomBar(activeTab: Int, onTabSelected: (Int) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        color = AstralSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Triple("المنبهات", Icons.Default.Notifications, Icons.Outlined.Notifications),
                Triple("التحدي", Icons.Default.Edit, Icons.Outlined.Edit),
                Triple("الإحصائيات", Icons.Default.CheckCircle, Icons.Outlined.CheckCircle)
            )

            tabs.forEachIndexed { idx, pair ->
                val active = activeTab == idx
                val scale by animateFloatAsState(if (active) 1.15f else 1.0f)

                Column(
                    modifier = Modifier
                        .clickable { onTabSelected(idx) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (active) pair.second else pair.third,
                        contentDescription = pair.first,
                        tint = if (active) AstralPrimary else AstralMuted,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = pair.first,
                        color = if (active) TextLight else AstralMuted,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// Full Screen active ringing screen when alarm goes off
@Composable
fun ActiveRingingOverlay(
    alarm: Alarm,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AstralDarkBg
    ) {
        ActiveChallengeView(
            label = alarm.label,
            puzzleType = alarm.puzzleType,
            difficulty = alarm.puzzleDifficulty,
            onSolved = onDismiss,
            onSnoozeEnabled = true,
            onSnooze = onSnooze
        )
    }
}

// Subordinate view that coordinates the puzzle UI
@Composable
fun ActiveChallengeView(
    label: String,
    puzzleType: String,
    difficulty: String,
    onSolved: () -> Unit,
    onSnoozeEnabled: Boolean = true,
    onSnooze: () -> Unit
) {
    val context = LocalContext.current
    var isSuccess by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0C20), Color(0xFF1E1740))
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Clock logo with pulsed state
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(AstralPrimary.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, AstralPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = AstralPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (label.isNotEmpty()) label else "حان وقت الاستيقاظ!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = TextLight,
                textAlign = TextAlign.Center
            )

            Text(
                text = "صباح الخير! الاستيقاظ ليس بالجسد بل بالعقل والذكاء ☀️",
                color = AstralMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Solve content depending on type
            when (puzzleType) {
                "NONE" -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "منبه بدون تحدي مفعّل. قم بالاستقاظ سريعاً!",
                            color = AstralMuted,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { onSolved() },
                            colors = ButtonDefaults.buttonColors(containerColor = AstralSecondary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("dismiss_no_puzzle_button")
                        ) {
                            Text("إيقاف المنبه الآن 🛑", color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                "MATH" -> {
                    MathPuzzleSection(
                        difficulty = difficulty,
                        onCompleted = { onSolved() }
                    )
                }
                "SHAKE" -> {
                    ShakePuzzleSection(
                        onCompleted = { onSolved() }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (onSnoozeEnabled) {
                TextButton(
                    onClick = onSnooze,
                    modifier = Modifier.testTag("snooze_overlay_button")
                ) {
                    Text(
                        "تأجيل بغفوة قصيرة (اضغط كغفوة مسموحة)",
                        color = AstralAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Math equations section solver
@Composable
fun MathPuzzleSection(
    difficulty: String,
    onCompleted: () -> Unit
) {
    var firstNum by remember { mutableStateOf(0) }
    var secondNum by remember { mutableStateOf(0) }
    var operation by remember { mutableStateOf("+") }
    var correctAnswer by remember { mutableStateOf(0) }
    
    var uAnswer by remember { mutableStateOf("") }
    var equationsCompleted by remember { mutableStateOf(0) }
    val equationsRequired = 3

    // Generate equations functional solver
    fun generateEquation() {
        uAnswer = ""
        val (min, max) = when (difficulty) {
            "EASY" -> 4 to 15
            "MEDIUM" -> 11 to 29
            else -> 25 to 79
        }
        
        firstNum = Random.nextInt(min, max)
        secondNum = Random.nextInt(2, 12)
        
        val ops = if (difficulty == "HARD") listOf("+", "-", "*") else listOf("+", "-")
        operation = ops.random()
        
        correctAnswer = when (operation) {
            "+" -> firstNum + secondNum
            "-" -> firstNum - secondNum
            else -> firstNum * secondNum
        }
    }

    LaunchedEffect(equationsCompleted) {
        if (equationsCompleted < equationsRequired) {
            generateEquation()
        } else {
            onCompleted()
        }
    }

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "لغز الحساب: أجب عن المسائل لتفعيل الإيقاف ($equationsCompleted / $equationsRequired)",
                color = AstralAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(14.dp))

            // The equation card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(containerColor = AstralSurface)
            ) {
                Text(
                    text = "$firstNum $operation $secondNum = ؟",
                    color = TextLight,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Input display row
            Text(
                text = if (uAnswer.isEmpty()) "?" else uAnswer,
                color = AstralPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AstralCard, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Numerical Keypad styling
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("-", "0", "مسح")
                ).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { char ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .background(AstralCard, RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (char == "مسح") {
                                            uAnswer = ""
                                        } else {
                                            uAnswer += char
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char,
                                    color = TextLight,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val ans = uAnswer.toIntOrNull()
                    if (ans == correctAnswer) {
                        equationsCompleted += 1
                    } else {
                        uAnswer = ""
                        generateEquation() // reset or randomize if wrong to preserve wake up!
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AstralSecondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_math_answer")
            ) {
                Text(
                    text = "تأكيد الإجابة والتحقق الحسابي ✔️",
                    color = TextLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Shake action solver using accelerometer hardware sensor
@Composable
fun ShakePuzzleSection(
    onCompleted: () -> Unit
) {
    val context = LocalContext.current
    var shakeCount by remember { mutableStateOf(0) }
    val targetShakes = 15

    // Physical sensor setup bound safely to view lifecycle
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            private var lastUpdate: Long = 0
            private var lastX = 0f
            private var lastY = 0f
            private var lastZ = 0f
            private val shakeThreshold = 10.0f // custom gravity speed offset

            override fun onSensorChanged(event: SensorEvent) {
                val curTime = System.currentTimeMillis()
                if ((curTime - lastUpdate) > 100) {
                    val diffTime = curTime - lastUpdate
                    lastUpdate = curTime

                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    val speed = abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000

                    if (speed > 750) {
                        shakeCount = (shakeCount + 1).coerceAtMost(targetShakes)
                        if (shakeCount >= targetShakes) {
                            onCompleted()
                        }
                    }
                    lastX = x
                    lastY = y
                    lastZ = z
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "تحدي الحركة النشطة: قم بهز الهاتف بقوة!",
                color = AstralAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "يحسب حساس التسارع حركات يدك لضخ الدم واسترداد يقظتك بالكامل.",
                color = AstralMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            Text(
                text = "$shakeCount / $targetShakes",
                color = AstralPrimary,
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Pulse progress bar
            val progress = shakeCount.toFloat() / targetShakes.toFloat()
            LinearProgressIndicator(
                progress = progress,
                color = AstralPrimary,
                trackColor = AstralCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "يرجى الهز بشكل متتالي ونشط لحساب الخطوات الإجمالية.",
                color = AstralMuted,
                fontSize = 11.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Fallback unlock button (for stream preview emulator or if accelerometer is missing)
            Button(
                onClick = {
                    shakeCount = (shakeCount + 2).coerceAtMost(targetShakes)
                    if (shakeCount >= targetShakes) {
                        onCompleted()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AstralCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("تخاكٍ بالنقر اليدوي البديل (للمعاينة والمحاكاة) 🔔", color = TextLight, fontSize = 11.sp)
            }
        }
    }
}
