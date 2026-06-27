package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val consumers by viewModel.consumers.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedConsumerIdForDetail by remember { mutableStateOf<Int?>(null) }
    var consumerToEdit by remember { mutableStateOf<Consumer?>(null) }

    var selectedTab by remember { mutableStateOf(0) }
    var selectedAnalysisConsumerId by remember { mutableStateOf<Int?>(null) }

    val selectedConsumer = remember(consumers, selectedConsumerIdForDetail) {
        consumers.firstOrNull { it.id == selectedConsumerIdForDetail }
    }

    val filteredConsumers = remember(consumers, searchQuery) {
        if (searchQuery.isBlank()) {
            consumers
        } else {
            consumers.filter {
                it.localName.contains(searchQuery, ignoreCase = true) ||
                        it.consumerNumber.contains(searchQuery, ignoreCase = true) ||
                        it.phone.contains(searchQuery, ignoreCase = true) ||
                        it.address.contains(searchQuery, ignoreCase = true) ||
                        it.agencyName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "LPG Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                CircleShape
                            )
                            .padding(6.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1.0f)) {
                        Text(
                            text = "LPG Manager",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Personal Customer Database",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Simple stats badge
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${consumers.size} Total",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Spreadsheet Download / Export CSV button
                    IconButton(
                        onClick = { exportConsumersToCsv(context, consumers) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export Excel Sheet",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Single bar search text field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search customers or agency...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("consumer_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Segmented TabRow to toggle consumer list vs analytics
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Customers List", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.People, contentDescription = "List view", modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Monthly Reports", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Assessment, contentDescription = "Analytics dashboard", modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    consumerToEdit = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .testTag("add_consumer_fab")
                    .navigationBarsPadding()
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Customer")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedTab == 0) {
                if (filteredConsumers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = "Empty",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (consumers.isEmpty()) {
                                    "No customers yet.\nTap '+' below to register custom LPG cards."
                                } else {
                                    "No search results match."
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(filteredConsumers, key = { it.id }) { consumer ->
                            ConsumerCardItem(
                                consumer = consumer,
                                onSelect = { selectedConsumerIdForDetail = consumer.id },
                                mapPredictedDateHighlight = true,
                                onQuickRefill = {
                                    val today = LpgCalculationUtils.getCurrentDateStr()
                                    viewModel.logDateEvent(
                                        consumerId = consumer.id,
                                        eventType = "Customer Delivery/Refill",
                                        eventDate = today,
                                        amountStatus = "Completed"
                                    )
                                    Toast.makeText(context, "Logged refill for ${consumer.localName}.", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            } else {
                MonthlyAnalyticsDashboard(
                    consumers = consumers,
                    selectedAnalysisConsumerId = selectedAnalysisConsumerId,
                    onSelectAnalysisConsumer = { selectedAnalysisConsumerId = it }
                )
            }
        }
    }

    // Detail Dialog Sheet with comprehensive transaction timeline & manual logging
    if (selectedConsumer != null) {
        ConsumerDetailDialog(
            consumer = selectedConsumer,
            viewModel = viewModel,
            onDismiss = { selectedConsumerIdForDetail = null },
            onEdit = {
                // Open add/edit dialog pre-populated
                consumerToEdit = selectedConsumer
                selectedConsumerIdForDetail = null
                showAddEditDialog = true
            },
            onRefillToday = {
                val today = LpgCalculationUtils.getCurrentDateStr()
                viewModel.logDateEvent(
                    consumerId = selectedConsumer.id,
                    eventType = "Customer Delivery/Refill",
                    eventDate = today,
                    amountStatus = "Completed"
                )
                Toast.makeText(context, "Logged refill for ${selectedConsumer.localName}.", Toast.LENGTH_SHORT).show()
                selectedConsumerIdForDetail = null
            },
            onDelete = {
                viewModel.deleteConsumer(selectedConsumer)
                selectedConsumerIdForDetail = null
                Toast.makeText(context, "Deleted ${selectedConsumer.localName}.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Add or Edit Customer registration dialog
    if (showAddEditDialog) {
        AddEditCustomerDialog(
            existingConsumer = consumerToEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { cId, cNum, lName, pName, phoneNum, addr, comp, agencyN, passB, cycleDays, bookingD, deliveryD, custDelD, predictedD ->
                viewModel.addOrUpdateConsumer(
                    id = cId,
                    consumerNumber = cNum,
                    localName = lName,
                    officialPassbookName = pName,
                    phone = phoneNum,
                    address = addr,
                    companyType = comp,
                    agencyName = agencyN,
                    hasPassbook = passB,
                    cycleDays = cycleDays,
                    lastAgencyBookingDate = bookingD,
                    lastAgencyDeliveryDate = deliveryD,
                    lastCustomerDeliveryDate = custDelD,
                    nextPredictedBookingDate = predictedD
                )
                showAddEditDialog = false
                Toast.makeText(context, "Saved customer info successfully.", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun ConsumerCardItem(
    consumer: Consumer,
    onSelect: () -> Unit,
    mapPredictedDateHighlight: Boolean = false,
    onQuickRefill: () -> Unit
) {
    val remainingDays = remember(consumer.nextPredictedBookingDate) {
        getRemainingDays(consumer.nextPredictedBookingDate)
    }

    val isNearDue = remainingDays != null && remainingDays in 0..3
    val isOverdue = remainingDays != null && remainingDays < 0

    val borderStroke = when {
        isOverdue -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
        isNearDue -> BorderStroke(1.5.dp, Color(0xFFF2994A).copy(alpha = 0.8f))
        else -> null
    }

    val statusColor = when {
        remainingDays == null -> MaterialTheme.colorScheme.outline
        remainingDays < 0 -> MaterialTheme.colorScheme.error
        remainingDays <= 5 -> Color(0xFFF2994A) // orange
        else -> MaterialTheme.colorScheme.primary
    }

    val companyLabel = consumer.companyType.lowercase(Locale.ROOT).trim()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = borderStroke
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.0f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = consumer.localName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isNearDue) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Upcoming refill notification",
                                tint = Color(0xFFF2994A),
                                modifier = Modifier.size(16.dp)
                            )
                        } else if (isOverdue) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Overdue refill notification",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "ID: ${consumer.consumerNumber} • Cycle: ${consumer.cycleDays} days",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Refill notification banner badge inside card
                    if (isNearDue) {
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFB74D))
                        ) {
                            Text(
                                text = "Due soon!",
                                color = Color(0xFFE65100),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isOverdue) {
                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF5350))
                        ) {
                            Text(
                                text = "Overdue!",
                                color = Color(0xFFC62828),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Passbook tracking badge
                    if (consumer.hasPassbook) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Passbook ✓",
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Company label badge
                    Surface(
                        color = when {
                            companyLabel.contains("indane") -> Color(0xFFFF5722)
                            companyLabel.contains("hp") -> Color(0xFF1E88E5)
                            companyLabel.contains("bharat") -> Color(0xFFFFB300)
                            else -> MaterialTheme.colorScheme.secondary
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = consumer.companyType,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Agency Name block (if available)
            if (consumer.agencyName.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = "Agency",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = consumer.agencyName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (consumer.phone.isNotBlank() || consumer.address.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                if (consumer.phone.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(consumer.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (consumer.address.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(consumer.address, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Last Refill: ${consumer.lastCustomerDeliveryDate ?: "None"}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (consumer.nextPredictedBookingDate != null) {
                            "Next Estimated: ${consumer.nextPredictedBookingDate}"
                        } else {
                            "Next Estimated: N/A"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                TextButton(
                    onClick = onQuickRefill,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refilled Today", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ConsumerDetailDialog(
    consumer: Consumer,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onRefillToday: () -> Unit,
    onDelete: () -> Unit
) {
    val remainingDays = remember(consumer.nextPredictedBookingDate) {
        getRemainingDays(consumer.nextPredictedBookingDate)
    }

    val statusMessage = remember(remainingDays) {
        when {
            remainingDays == null -> "No refill logged yet"
            remainingDays < 0 -> "Overdue by ${-remainingDays} days"
            remainingDays == 0 -> "Due today"
            else -> "Due in $remainingDays days"
        }
    }

    val statusColor = when {
        remainingDays == null -> MaterialTheme.colorScheme.outline
        remainingDays < 0 -> MaterialTheme.colorScheme.error
        remainingDays <= 5 -> Color(0xFFF2994A)
        else -> MaterialTheme.colorScheme.primary
    }

    // Manual Event input states
    var customLogDate by remember { mutableStateOf(LpgCalculationUtils.getCurrentDateStr()) }
    var selectedEventTypeIndex by remember { mutableStateOf(0) } // 0: Booking Request, 1: Agency Pickup, 2: Customer Refill
    val eventTypes = listOf("Booking Request", "Agency Pickup/Delivery", "Customer Delivery/Refill")

    // Parse transaction logs cleanly using Moshi helper
    val transactionsList = remember(consumer.transactionHistoryJson) {
        parseTransactions(consumer.transactionHistoryJson)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = consumer.localName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("delete_consumer_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete customer")
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { HorizontalDivider() }

                item { DetailItem(label = "Consumer Connection ID", value = consumer.consumerNumber) }
                if (consumer.officialPassbookName.isNotBlank()) {
                    item { DetailItem(label = "Official Passbook Name", value = consumer.officialPassbookName) }
                }

                item { DetailItem(label = "LPG Provider Company", value = consumer.companyType) }
                item { DetailItem(label = "Gas Agency Name", value = consumer.agencyName.ifBlank { "Not specified" }) }
                item {
                    DetailItem(
                        label = "LPG Passbook with Me",
                        value = if (consumer.hasPassbook) "Yes (Securely with me)" else "No / Not with me"
                    )
                }

                item { DetailItem(label = "Prediction Cycle", value = "${consumer.cycleDays} Days") }
                item { DetailItem(label = "Phone Number", value = consumer.phone.ifBlank { "Not provided" }) }
                item { DetailItem(label = "Delivery Address", value = consumer.address.ifBlank { "Not provided" }) }

                item { HorizontalDivider() }

                item {
                    Surface(
                        color = statusColor.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Estimated Next Cycle Prediction",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${consumer.nextPredictedBookingDate ?: "N/A"} ($statusMessage)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }
                        }
                    }
                }

                // Current records summaries
                item { DetailItem(label = "Last Logged Refill", value = consumer.lastCustomerDeliveryDate ?: "Never logged") }
                item { DetailItem(label = "Last Agency Booking Date", value = consumer.lastAgencyBookingDate ?: "N/A") }
                item { DetailItem(label = "Last Agency Pick-up Date", value = consumer.lastAgencyDeliveryDate ?: "N/A") }

                item { HorizontalDivider() }

                // --- NEW SECTION: Log a brand new custom transaction on the fly ---
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Log New Transaction Event Manually",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Event selector segmented buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Book", "Agency", "Retail").forEachIndexed { index, label ->
                                val isSelected = selectedEventTypeIndex == index
                                val containerCol = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                val contentCol = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(containerCol)
                                        .clickable { selectedEventTypeIndex = index }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = contentCol
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // Date field for manual log
                        OutlinedTextField(
                            value = customLogDate,
                            onValueChange = { customLogDate = it },
                            label = { Text("Log Date", fontSize = 11.sp) },
                            placeholder = { Text("YYYY-MM-DD") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (customLogDate.trim().isNotBlank()) {
                                    val mappedType = eventTypes[selectedEventTypeIndex]
                                    viewModel.logDateEvent(
                                        consumerId = consumer.id,
                                        eventType = mappedType,
                                        eventDate = customLogDate.trim()
                                    )
                                    customLogDate = LpgCalculationUtils.getCurrentDateStr()
                                    Toast.makeText(viewModel.getApplication(), "Successfully logged $mappedType!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log Record", fontSize = 11.sp)
                        }
                    }
                }

                item { HorizontalDivider() }

                // --- NEW SECTION: Display complete Transaction History Timeline ---
                item {
                    Text(
                        text = "Complete Transaction History Logs (${transactionsList.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (transactionsList.isEmpty()) {
                    item {
                        Text(
                            text = "No history log found. Add events manually above or quick-refill.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(transactionsList.asReversed().mapIndexed { i, t -> IndexedValue(transactionsList.size - 1 - i, t) }) { iv ->
                        val index = iv.index
                        val trans = iv.value

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Match appropriate icon to history item
                                val transIcon = when {
                                    trans.eventType.contains("Book", ignoreCase = true) -> Icons.Default.Bookmark
                                    trans.eventType.contains("Agency", ignoreCase = true) -> Icons.Default.LocalShipping
                                    trans.eventType.contains("Customer", ignoreCase = true) -> Icons.Default.LocalFireDepartment
                                    else -> Icons.Default.CheckCircle
                                }
                                val iconColor = when {
                                    trans.eventType.contains("Book", ignoreCase = true) -> MaterialTheme.colorScheme.secondary
                                    trans.eventType.contains("Agency", ignoreCase = true) -> MaterialTheme.colorScheme.tertiary
                                    trans.eventType.contains("Customer", ignoreCase = true) -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outline
                                }

                                Icon(
                                    imageVector = transIcon,
                                    contentDescription = null,
                                    tint = iconColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = trans.eventType,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${trans.date} • ${trans.status}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Let users delete incorrect history items directly!
                            IconButton(
                                onClick = {
                                    viewModel.deleteTransactionEvent(consumer.id, index)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete item",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit All Details")
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Close")
                }
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun DetailItem(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun AddEditCustomerDialog(
    existingConsumer: Consumer?,
    onDismiss: () -> Unit,
    onSave: (
        id: Int,
        cNumber: String,
        localName: String,
        officialPassbookName: String,
        phone: String,
        address: String,
        provider: String,
        agencyName: String,
        hasPassbook: Boolean,
        cycleDays: Int,
        bookingDate: String?,
        deliveryDate: String?,
        custDelDate: String?,
        predictedDate: String?
    ) -> Unit
) {
    var cNumber by remember { mutableStateOf(existingConsumer?.consumerNumber ?: "") }
    var localName by remember { mutableStateOf(existingConsumer?.localName ?: "") }
    var officialPassbookName by remember { mutableStateOf(existingConsumer?.officialPassbookName ?: "") }
    var phone by remember { mutableStateOf(existingConsumer?.phone ?: "") }
    var address by remember { mutableStateOf(existingConsumer?.address ?: "") }
    var provider by remember { mutableStateOf(existingConsumer?.companyType ?: "Indane") }
    var agencyName by remember { mutableStateOf(existingConsumer?.agencyName ?: "") }
    var hasPassbook by remember { mutableStateOf(existingConsumer?.hasPassbook ?: false) }

    // Prediction configurations cycle (Defaults to 25 days)
    var cycleDaysStr by remember { mutableStateOf(existingConsumer?.cycleDays?.toString() ?: "25") }

    // Editable dates
    var lastBookingDate by remember { mutableStateOf(existingConsumer?.lastAgencyBookingDate ?: "") }
    var lastAgencyDeliveryDate by remember { mutableStateOf(existingConsumer?.lastAgencyDeliveryDate ?: "") }
    var lastCustomerDeliveryDate by remember { mutableStateOf(existingConsumer?.lastCustomerDeliveryDate ?: "") }
    var nextPredictedBookingDate by remember { mutableStateOf(existingConsumer?.nextPredictedBookingDate ?: "") }

    val isEdit = existingConsumer != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Configure Customer Info" else "Register LPG Customer",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = cNumber,
                        onValueChange = { cNumber = it },
                        label = { Text("Consumer Connection ID *") },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_dialog_consumer_number")
                    )
                }
                item {
                    OutlinedTextField(
                        value = localName,
                        onValueChange = { localName = it },
                        label = { Text("Customer Display Name *") },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_dialog_local_name")
                    )
                }
                item {
                    OutlinedTextField(
                        value = officialPassbookName,
                        onValueChange = { officialPassbookName = it },
                        label = { Text("Official Passbook Name (Optional)") },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Delivery Address") },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Gas Agency Name
                item {
                    OutlinedTextField(
                        value = agencyName,
                        onValueChange = { agencyName = it },
                        label = { Text("Gas Agency Name (e.g. Sri Ram HP Agency)") },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // NEW: Customizable prediction cycle days
                item {
                    OutlinedTextField(
                        value = cycleDaysStr,
                        onValueChange = { cycleDaysStr = it },
                        label = { Text("Prediction Cycle Difference (Days) *") },
                        placeholder = { Text("Defaults to 25") },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Passbook possession check
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .clickable { hasPassbook = !hasPassbook }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = if (hasPassbook) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "LPG Passbook physically with me",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (hasPassbook) "Yes (Passbook is cataloged with me)" else "No / Not with me",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = hasPassbook,
                            onCheckedChange = { hasPassbook = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                // Provider Select Buttons
                item {
                    Text("LPG Provider Company", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Indane", "HP Gas", "Bharat Gas").forEach { comp ->
                            val isSel = provider.lowercase().trim() == comp.lowercase().trim()
                            val borderCol = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            val textCol = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                            Box(
                                modifier = Modifier
                                    .weight(1.0f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.5.dp, borderCol, RoundedCornerShape(8.dp))
                                    .clickable { provider = comp }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(comp, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textCol)
                            }
                        }
                    }
                }

                // Section header for dates
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Customize Transaction History Dates",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Editable dates
                item {
                    OutlinedTextField(
                        value = lastBookingDate,
                        onValueChange = { lastBookingDate = it },
                        label = { Text("Last Agency Booking Date") },
                        shape = RoundedCornerShape(8.dp),
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = lastAgencyDeliveryDate,
                        onValueChange = { lastAgencyDeliveryDate = it },
                        label = { Text("Last Agency Pick-up Date") },
                        shape = RoundedCornerShape(8.dp),
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = lastCustomerDeliveryDate,
                        onValueChange = { lastCustomerDeliveryDate = it },
                        label = { Text("Last Customer Refill Date") },
                        shape = RoundedCornerShape(8.dp),
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = nextPredictedBookingDate,
                        onValueChange = { nextPredictedBookingDate = it },
                        label = { Text("Next Predicted Refill Date") },
                        shape = RoundedCornerShape(8.dp),
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (cNumber.isNotBlank() && localName.isNotBlank()) {
                            val computedCycle = cycleDaysStr.trim().toIntOrNull() ?: 25
                            onSave(
                                existingConsumer?.id ?: 0,
                                cNumber.trim(),
                                localName.trim(),
                                officialPassbookName.trim(),
                                phone.trim(),
                                address.trim(),
                                provider,
                                agencyName.trim(),
                                hasPassbook,
                                computedCycle,
                                lastBookingDate.trim().takeIf { it.isNotBlank() },
                                lastAgencyDeliveryDate.trim().takeIf { it.isNotBlank() },
                                lastCustomerDeliveryDate.trim().takeIf { it.isNotBlank() },
                                nextPredictedBookingDate.trim().takeIf { it.isNotBlank() }
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_dialog_save_button")
                ) {
                    Text("Save")
                }
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

// Simple Helper to calculate remaining days
private fun getRemainingDays(nextDateStr: String?): Int? {
    if (nextDateStr.isNullOrBlank()) return null
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val targetDate = sdf.parse(nextDateStr) ?: return null
        val today = sdf.parse(sdf.format(Date())) ?: return null
        val diffMs = targetDate.time - today.time
        (diffMs / (1000 * 60 * 60 * 24)).toInt()
    } catch (e: Exception) {
        null
    }
}

// Safe robust parser to parse transactions in MainScreen
private fun parseTransactions(jsonStr: String?): List<LpgTransaction> {
    if (jsonStr.isNullOrBlank()) return emptyList()
    return try {
        val moshi = com.squareup.moshi.Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, LpgTransaction::class.java)
        val adapter = moshi.adapter<List<LpgTransaction>>(type)
        adapter.fromJson(jsonStr) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

// CSV systematic spreadsheet exporter details
fun exportConsumersToCsv(context: android.content.Context, list: List<Consumer>) {
    if (list.isEmpty()) {
        Toast.makeText(context, "No consumer data available to export.", Toast.LENGTH_SHORT).show()
        return
    }

    // CSV header structure
    val csvHeader = "ID,Consumer Name,Official Passbook Name,Phone Number,Delivery Address,LPG Provider,Gas Agency Name,Passbook is with Me,Prediction Cycle (Days),Last Booking Date,Last Agency Delivery Date,Last Customer Delivery,Next Predicted Refill Date\n"
    
    val csvBody = StringBuilder()
    csvBody.append(csvHeader)
    
    for (consumer in list) {
        val row = listOf(
            escapeCsv(consumer.consumerNumber),
            escapeCsv(consumer.localName),
            escapeCsv(consumer.officialPassbookName),
            escapeCsv(consumer.phone),
            escapeCsv(consumer.address),
            escapeCsv(consumer.companyType),
            escapeCsv(consumer.agencyName),
            if (consumer.hasPassbook) "Yes" else "No",
            consumer.cycleDays.toString(),
            escapeCsv(consumer.lastAgencyBookingDate),
            escapeCsv(consumer.lastAgencyDeliveryDate),
            escapeCsv(consumer.lastCustomerDeliveryDate),
            escapeCsv(consumer.nextPredictedBookingDate)
        ).joinToString(",")
        csvBody.append(row).append("\n")
    }

    try {
        // Write file inside caches directory
        val cacheFile = java.io.File(context.cacheDir, "lpg_consumers_record.csv")
        cacheFile.writeText(csvBody.toString())
        
        // Expose provider Uri securely
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )
        
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "LPG Consumers Data Sheet")
            putExtra(android.content.Intent.EXTRA_TEXT, "Exposing complete spreadsheet export of personal customer database cards.")
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(android.content.Intent.createChooser(intent, "Share LPG Database Spreadsheet"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to share/export CSV data: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun escapeCsv(value: String?): String {
    if (value == null) return ""
    var s = value.replace("\"", "\"\"")
    if (s.contains(",") || s.contains("\n") || s.contains("\"")) {
        s = "\"$s\""
    }
    return s
}

// Extract a readable month key ("MMM YYYY") for grouping analytics data
private fun getMonthKey(dateStr: String?): String? {
    if (dateStr.isNullOrBlank()) return null
    return try {
        val sdfSource = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdfSource.parse(dateStr) ?: return null
        val sdfDest = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        sdfDest.format(date)
    } catch (e: Exception) {
        null
    }
}

@Composable
fun MonthlyAnalyticsDashboard(
    consumers: List<Consumer>,
    selectedAnalysisConsumerId: Int?,
    onSelectAnalysisConsumer: (Int?) -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val activeConsumer = consumers.firstOrNull { it.id == selectedAnalysisConsumerId }
    val dropdownLabel = activeConsumer?.localName ?: "All Registered Customers"

    val targetConsumers = remember(consumers, selectedAnalysisConsumerId) {
        if (selectedAnalysisConsumerId == null) consumers else consumers.filter { it.id == selectedAnalysisConsumerId }
    }

    // Compute delivered refills (Completed deliveries)
    val totalDelivered = remember(targetConsumers) {
        targetConsumers.sumOf { c ->
            parseTransactions(c.transactionHistoryJson).count {
                it.eventType.contains("Customer", ignoreCase = true) || it.eventType.contains("Refill", ignoreCase = true)
            }
        }
    }

    // Compute pending refills (Booking Requests not completed or next predictions with no delivery logged yet)
    val totalPending = remember(targetConsumers) {
        targetConsumers.sumOf { c ->
            var count = parseTransactions(c.transactionHistoryJson).count {
                it.status.contains("Pending", ignoreCase = true) || 
                (it.eventType.contains("Book", ignoreCase = true) && !it.status.contains("Complete", ignoreCase = true))
            }
            // Count predicted date if upcoming and not refilled in its month
            val pred = c.nextPredictedBookingDate
            if (!pred.isNullOrBlank()) {
                val pMonth = getMonthKey(pred)
                val lastDel = c.lastCustomerDeliveryDate
                val isFulfilled = if (pMonth != null && !lastDel.isNullOrBlank()) {
                    getMonthKey(lastDel) == pMonth && lastDel >= pred
                } else false
                if (pMonth != null && !isFulfilled) count++
            }
            count
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Consumer selection dropdown container
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dropdownExpanded = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter Selection",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Database Analytics Scope", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(dropdownLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Icon(
                        imageVector = if (dropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Toggle Dropdown",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    DropdownMenuItem(
                        text = { Text("All Customers Combined", fontWeight = FontWeight.Bold) },
                        onClick = {
                            onSelectAnalysisConsumer(null)
                            dropdownExpanded = false
                        }
                    )
                    consumers.forEach { consumer ->
                        DropdownMenuItem(
                            text = { Text(consumer.localName) },
                            onClick = {
                                onSelectAnalysisConsumer(consumer.id)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // High Level Analytics Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Delivered Refills counter card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Refilled", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$totalDelivered Cylinders",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Pending refills counter card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Refills Pending", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color(0xFFF2994A),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$totalPending Cylinder${if (totalPending == 1) "" else "s"}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Render visual charting component!
        val deliveredMap = remember(targetConsumers) {
            val map = mutableMapOf<String, Int>()
            targetConsumers.forEach { c ->
                parseTransactions(c.transactionHistoryJson).forEach { t ->
                    if (t.eventType.contains("Customer", ignoreCase = true) || t.eventType.contains("Refill", ignoreCase = true)) {
                        val mKey = getMonthKey(t.date)
                        if (mKey != null) {
                            map[mKey] = map.getOrDefault(mKey, 0) + 1
                        }
                    }
                }
            }
            map
        }

        val pendingMap = remember(targetConsumers) {
            val map = mutableMapOf<String, Int>()
            targetConsumers.forEach { c ->
                parseTransactions(c.transactionHistoryJson).forEach { t ->
                    if (t.status.contains("Pending", ignoreCase = true) || 
                        (t.eventType.contains("Book", ignoreCase = true) && !t.status.contains("Complete", ignoreCase = true))) {
                        val mKey = getMonthKey(t.date)
                        if (mKey != null) {
                            map[mKey] = map.getOrDefault(mKey, 0) + 1
                        }
                    }
                }
                val pred = c.nextPredictedBookingDate
                if (!pred.isNullOrBlank()) {
                    val pMonth = getMonthKey(pred)
                    val lastDel = c.lastCustomerDeliveryDate
                    val isFulfilled = if (pMonth != null && !lastDel.isNullOrBlank()) {
                        getMonthKey(lastDel) == pMonth && lastDel >= pred
                    } else false
                    if (pMonth != null && !isFulfilled) {
                        map[pMonth] = map.getOrDefault(pMonth, 0) + 1
                    }
                }
            }
            map
        }

        val sortedMonths = remember(deliveredMap, pendingMap) {
            (deliveredMap.keys + pendingMap.keys).toList().sortedWith { m1, m2 ->
                try {
                    val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                    val d1 = sdf.parse(m1) ?: Date(0)
                    val d2 = sdf.parse(m2) ?: Date(0)
                    d1.compareTo(d2)
                } catch (e: Exception) {
                    m1.compareTo(m2)
                }
            }
        }

        MonthlySummaryChart(
            deliveredData = deliveredMap,
            pendingData = pendingMap,
            sortedMonths = sortedMonths
        )
    }
}

@Composable
fun MonthlySummaryChart(
    deliveredData: Map<String, Int>,
    pendingData: Map<String, Int>,
    sortedMonths: List<String>
) {
    val maxVal = remember(deliveredData, pendingData) {
        val maxFromData = (deliveredData.values + pendingData.values).maxOfOrNull { it } ?: 0
        maxOf(maxFromData, 3) 
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Chart Title & Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Refills Breakdown",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF2E7D32)) 
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delivered", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFF2994A)) 
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pending", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (sortedMonths.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No refill transactions logged with dates yet.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                var activeMonthIndex by remember { mutableStateOf<Int?>(null) }

                // Live dynamic summary display when clicking month bars
                AnimatedVisibility(
                    visible = activeMonthIndex != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    activeMonthIndex?.let { index ->
                        if (index in sortedMonths.indices) {
                            val activeMonth = sortedMonths[index]
                            val deliveredCount = deliveredData[activeMonth] ?: 0
                            val pendingCount = pendingData[activeMonth] ?: 0
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(activeMonth, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                        Text("Refill Activity Report", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Text("Delivered: $deliveredCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                        Text("Pending: $pendingCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD84315))
                                    }
                                }
                            }
                        }
                    }
                }

                // Main Bar Graph columns rendering
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    // Draw horizontal lines representing scale guidelines
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 24.dp), 
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(1f, 0.66f, 0.33f, 0f).forEach { fraction ->
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )
                        }
                    }

                    // Columns container (Horizontal Scrollable)
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        sortedMonths.forEachIndexed { idx, month ->
                            val deliveredCount = deliveredData[month] ?: 0
                            val pendingCount = pendingData[month] ?: 0
                            val isSelected = activeMonthIndex == idx
                            
                            val deliveredHeightFraction = if (maxVal > 0) deliveredCount.toFloat() / maxVal else 0f
                            val pendingHeightFraction = if (maxVal > 0) pendingCount.toFloat() / maxVal else 0f

                            Column(
                                modifier = Modifier
                                    .width(70.dp)
                                    .fillMaxHeight()
                                    .clickable {
                                        activeMonthIndex = if (activeMonthIndex == idx) null else idx
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                // Side-by-Side Clustered Bars
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    // Delivered bar
                                    val delBg = if (isSelected) Color(0xFF2E7D32) else Color(0xFF4CAF50).copy(alpha = 0.85f)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(deliveredHeightFraction.coerceIn(0.04f, 1f))
                                            .shadow(if (isSelected) 4.dp else 0.dp, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(delBg)
                                    )

                                    // Pending bar
                                    val penBg = if (isSelected) Color(0xFFD84315) else Color(0xFFF2994A).copy(alpha = 0.85f)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(pendingHeightFraction.coerceIn(0.04f, 1f))
                                            .shadow(if (isSelected) 4.dp else 0.dp, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(penBg)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Month Label text
                                Text(
                                    text = month,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                
                // Interactive tip instruction
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tip: Tap on any month column above for detailed Refills Report",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
