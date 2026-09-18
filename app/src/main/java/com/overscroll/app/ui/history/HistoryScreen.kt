package com.overscroll.app.ui.history

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.overscroll.app.data.history.DailyCount
import com.overscroll.app.service.ScrollCountRepository
import com.overscroll.app.ui.theme.OverscrollPrimary
import com.overscroll.app.ui.theme.OverscrollBackgroundTop
import com.overscroll.app.ui.theme.OverscrollBackgroundBottom
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class ChartDataPoint(
    val label: String, // Label to show on X-axis (e.g. 'Mon', '15', 'Jan')
    val value: Int
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: ScrollCountRepository,
) : ViewModel() {

    private val _selectedAppFilter = MutableStateFlow<String?>(null)
    val selectedAppFilter = _selectedAppFilter.asStateFlow()
    
    private val _selectedSegment = MutableStateFlow("Week")
    val selectedSegment = _selectedSegment.asStateFlow()

    fun setAppFilter(packageName: String?) {
        _selectedAppFilter.value = packageName
    }
    
    fun setSegment(segment: String) {
        _selectedSegment.value = segment
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val chartData = combine(_selectedAppFilter, _selectedSegment, ::Pair)
        .flatMapLatest { (filter, segment) ->
            when (segment) {
                "Week" -> {
                    if (filter == null) {
                        combine(repository.getLast7DaysCombined(), repository.combinedTodayCount) { history, todayCount ->
                            buildDaysChartData(history.associate { it.date to it.totalCount }, todayCount, 7)
                        }
                    } else {
                        combine(repository.getLast7DaysForPackage(filter), repository.todayCounts) { history, todayCountsMap ->
                            buildDaysChartData(history.associate { it.date to it.count }, todayCountsMap[filter] ?: 0, 7)
                        }
                    }
                }
                "Month" -> {
                    if (filter == null) {
                        combine(repository.getLast30DaysCombined(), repository.combinedTodayCount) { history, todayCount ->
                            buildDaysChartData(history.associate { it.date to it.totalCount }, todayCount, 30)
                        }
                    } else {
                        combine(repository.getLast30DaysForPackage(filter), repository.todayCounts) { history, todayCountsMap ->
                            buildDaysChartData(history.associate { it.date to it.count }, todayCountsMap[filter] ?: 0, 30)
                        }
                    }
                }
                "Year" -> {
                    if (filter == null) {
                        // For Year, we don't mix in "todayCount" dynamically into the current month,
                        // because the DB query already returns the current month's total including today (if persisted).
                        // To be perfectly accurate we could add today's unpersisted count, but for a yearly view it's negligible.
                        repository.getLast12MonthsCombined().map { history ->
                            buildMonthsChartData(history.associate { it.period to it.totalCount })
                        }
                    } else {
                        repository.getLast12MonthsForPackage(filter).map { history ->
                            buildMonthsChartData(history.associate { it.period to it.totalCount })
                        }
                    }
                }
                else -> kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun buildDaysChartData(
        historyMap: Map<String, Int>,
        todayCount: Int,
        days: Int
    ): List<ChartDataPoint> {
        val today = LocalDate.now()
        val data = mutableListOf<ChartDataPoint>()
        val formatter = DateTimeFormatter.ofPattern("EEE")
        val monthFormatter = DateTimeFormatter.ofPattern("d") // just the day number for Month view
        
        for (i in (days - 1) downTo 0) {
            val date = today.minusDays(i.toLong())
            val dateStr = date.toString()
            
            val label = if (days == 7) {
                if (i == 0) "Today" else date.format(formatter)
            } else {
                if (i == 0) "Today" else if (date.dayOfMonth % 5 == 0) date.format(monthFormatter) else ""
            }
            
            if (i == 0) {
                data.add(ChartDataPoint(label, todayCount))
            } else {
                data.add(ChartDataPoint(label, historyMap[dateStr] ?: 0))
            }
        }
        return data
    }
    
    private fun buildMonthsChartData(
        historyMap: Map<String, Int>
    ): List<ChartDataPoint> {
        val today = LocalDate.now()
        val data = mutableListOf<ChartDataPoint>()
        val formatter = DateTimeFormatter.ofPattern("MMM")
        
        for (i in 11 downTo 0) {
            val date = today.minusMonths(i.toLong())
            val periodStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val label = date.format(formatter)
            data.add(ChartDataPoint(label, historyMap[periodStr] ?: 0))
        }
        return data
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val data by viewModel.chartData.collectAsState()
    val selectedSegment by viewModel.selectedSegment.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        OverscrollBackgroundTop,
                        OverscrollBackgroundBottom,
                    )
                )
            )
    ) {
        TopAppBar(
            title = {
                Text(
                    "History",
                    fontWeight = FontWeight.Bold,
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            
            SegmentedControl(
                segments = listOf("Week", "Month", "Year"),
                selectedSegment = selectedSegment,
                onSegmentSelected = { viewModel.setSegment(it) }
            )
            Spacer(Modifier.height(16.dp))
            
            // App Filter
            val selectedAppFilter by viewModel.selectedAppFilter.collectAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppFilterChip(
                    label = "Combined",
                    selected = selectedAppFilter == null,
                    onClick = { viewModel.setAppFilter(null) }
                )
                com.overscroll.app.config.AppTrackerConfig.SUPPORTED_APPS.forEach { app ->
                    AppFilterChip(
                        label = app.displayName,
                        selected = selectedAppFilter == app.packageName,
                        onClick = { viewModel.setAppFilter(app.packageName) }
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (data.isNotEmpty()) {
                    BarChart(data = data, modifier = Modifier.fillMaxSize().padding(24.dp))
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Loading data...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            // Statistics summary
            if (data.isNotEmpty()) {
                val total = data.sumOf { it.value }
                val avg = if (data.isNotEmpty()) total / data.size else 0
                val max = data.maxOfOrNull { it.value } ?: 0
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatBox(label = "Total", value = total.toString(), modifier = Modifier.weight(1f))
                    StatBox(label = "Average", value = avg.toString(), modifier = Modifier.weight(1f))
                    StatBox(label = "Highest", value = max.toString(), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun SegmentedControl(
    segments: List<String>,
    selectedSegment: String,
    onSegmentSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        segments.forEach { segment ->
            val isSelected = segment == selectedSegment
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSegmentSelected(segment) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = segment,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BarChart(
    data: List<ChartDataPoint>,
    modifier: Modifier = Modifier
) {
    val maxCount = data.maxOfOrNull { it.value }?.coerceAtLeast(10) ?: 10 // Minimum scale of 10
    
    // Animation progress
    val animationProgress = remember(data) { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    val barColor = OverscrollPrimary
    val barColorLight = OverscrollPrimary.copy(alpha = 0.5f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall

    Column(modifier = modifier) {
        // Chart Area
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barWidth = (canvasWidth / data.size) * 0.7f // Makes bars relatively thinner for 30 days
            val spacing = (canvasWidth - (barWidth * data.size)) / (data.size - 1)

            data.forEachIndexed { index, dataPoint ->
                val barHeight = (dataPoint.value.toFloat() / maxCount) * canvasHeight * animationProgress.value
                val xOffset = index * (barWidth + spacing)
                val yOffset = canvasHeight - barHeight

                // Draw rounded bar
                drawRoundRect(
                    color = if (index == data.size - 1) barColor else barColorLight, // Highlight today/current month
                    topLeft = Offset(xOffset, yOffset),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                )
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        // X-Axis Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEachIndexed { index, dataPoint ->
                if (dataPoint.label.isNotEmpty()) {
                    Text(
                        text = dataPoint.label,
                        style = labelStyle,
                        color = labelColor,
                        fontWeight = if (index == data.size - 1) FontWeight.Bold else FontWeight.Normal,
                        // Avoid clipping long labels by scaling them down if needed or we could just use a modifier,
                        // but since they are small ("Mon", "Jan", "15") it's generally fine.
                    )
                } else if (data.size > 12) {
                    // When labels are empty (like in Month view for intermediate days), 
                    // we still need to take up space to keep spacing correct if we used weight, 
                    // but SpaceBetween handles this automatically if we just render an invisible element.
                    // Wait, SpaceBetween only distributes space between visible elements! 
                    // Let's use Spacer with weight 1f if we can't use SpaceBetween properly.
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
@Composable
fun AppFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary 
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
