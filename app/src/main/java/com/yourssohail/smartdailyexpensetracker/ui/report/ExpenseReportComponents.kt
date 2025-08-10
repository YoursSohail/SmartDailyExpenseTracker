package com.yourssohail.smartdailyexpensetracker.ui.report

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.TableView
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourssohail.smartdailyexpensetracker.ui.theme.SmartDailyExpenseTrackerTheme
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.IndicatorCount
import ir.ehsannarmani.compose_charts.models.IndicatorPosition
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties

@Composable
fun ReportSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun DailyExpenseChartCard(
    columnChartData: List<Bars>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(16.dp)
        ) {
            Text(
                text = "Daily Expenses",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ColumnChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                data = columnChartData,
                labelProperties = LabelProperties(
                    enabled = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp
                    )
                ),
                indicatorProperties = HorizontalIndicatorProperties(
                    textStyle = TextStyle(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    count = IndicatorCount.CountBased(count = 4),
                    position = IndicatorPosition.Horizontal.Start,
                ),
                barProperties = BarProperties(
                    cornerRadius = Bars.Data.Radius.Rectangle(topLeft = 4.dp, topRight = 4.dp),
                ),
                labelHelperProperties = LabelHelperProperties(
                    textStyle = TextStyle(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                ),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportOptionsBottomSheet(
    sheetState: SheetState,
    // scope: CoroutineScope, // Removed
    onDismiss: () -> Unit,
    onExportCsvClicked: () -> Unit,
    onExportPdfClicked: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            ListItem(
                headlineContent = { Text("Export as CSV") },
                leadingContent = { Icon(Icons.Outlined.TableView, "Export as CSV") },
                modifier = Modifier.clickable {
                    onExportCsvClicked()
                }
            )
            ListItem(
                headlineContent = { Text("Export as PDF") },
                leadingContent = { Icon(Icons.Outlined.PictureAsPdf, "Export as PDF") },
                modifier = Modifier.clickable {
                    onExportPdfClicked()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareReportOptionsBottomSheet(
    sheetState: SheetState,
    // scope: CoroutineScope, // Removed
    onDismiss: () -> Unit,
    onSharePdfRequested: () -> Unit,
    onShareCsvRequested: () -> Unit,
    onShareTextRequested: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            ListItem(
                headlineContent = { Text("Share as PDF") },
                leadingContent = { Icon(Icons.Outlined.PictureAsPdf, "Share as PDF") },
                modifier = Modifier.clickable { onSharePdfRequested() }
            )
            ListItem(
                headlineContent = { Text("Share as CSV") },
                leadingContent = { Icon(Icons.Outlined.TableView, "Share as CSV") },
                modifier = Modifier.clickable { onShareCsvRequested() }
            )
            ListItem(
                headlineContent = { Text("Share as Text") },
                leadingContent = {
                    Icon(
                        Icons.AutoMirrored.Outlined.TextSnippet,
                        "Share as Text"
                    )
                }, // Updated Icon
                modifier = Modifier.clickable { onShareTextRequested() }
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ReportSectionCardPreview() {
    SmartDailyExpenseTrackerTheme {
        ReportSectionCard(title = "Sample Section") {
            Text("This is some sample content for the preview.")
            Text("More content can go here.")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DailyExpenseChartCardPreview() {
    SmartDailyExpenseTrackerTheme {
        val dummyChartData = remember {
            listOf(
                Bars(
                    label = "Mon",
                    values = listOf(
                        Bars.Data(
                            value = 150.0,
                            label = "Expense",
                            color = Brush.verticalGradient(listOf(Color.Blue, Color.Cyan))
                        )
                    )
                ),
                Bars(
                    label = "Tue",
                    values = listOf(
                        Bars.Data(
                            value = 250.0,
                            label = "Expense",
                            color = Brush.verticalGradient(listOf(Color.Blue, Color.Cyan))
                        )
                    )
                ),
                Bars(
                    label = "Wed",
                    values = listOf(
                        Bars.Data(
                            value = 100.0,
                            label = "Expense",
                            color = Brush.verticalGradient(listOf(Color.Blue, Color.Cyan))
                        )
                    )
                )
            )
        }
        DailyExpenseChartCard(columnChartData = dummyChartData)
    }
}
