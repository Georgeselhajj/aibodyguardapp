package com.aibodyguard.app.dashboard.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.aibodyguard.app.dashboard.DashboardViewModel
import com.aibodyguard.app.dashboard.model.Alert
import com.aibodyguard.app.dashboard.model.CarouselItem
import com.aibodyguard.app.dashboard.model.Member
import com.aibodyguard.app.dashboard.model.SecurityMode
import com.aibodyguard.app.dashboard.model.ThreatPerson
import com.aibodyguard.app.ui.theme.AIBodyguardTheme

@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel = viewModel(),
    onAddTrustedMember: () -> Unit = {},
    onAddAlert: () -> Unit = {},
    onAddThreat: () -> Unit = {}
) {
    val robotConnected by viewModel.robotConnected.collectAsStateWithLifecycle()
    val robotPowered by viewModel.robotPowered.collectAsStateWithLifecycle()
    val securityMode by viewModel.securityMode.collectAsStateWithLifecycle()
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val trustedMembers by viewModel.trustedMembers.collectAsStateWithLifecycle()
    val threats by viewModel.threats.collectAsStateWithLifecycle()

    DashboardScreen(
        robotConnected = robotConnected,
        robotPowered = robotPowered,
        securityMode = securityMode,
        alerts = alerts,
        trustedMembers = trustedMembers,
        threats = threats,
        carouselItems = viewModel.carouselItems,
        onSecurityModeSelected = viewModel::onSecurityModeSelected,
        onToggleRobotPower = viewModel::onToggleRobotPower,
        onAddTrustedMember = {
            viewModel.onAddTrustedMember()
            onAddTrustedMember()
        },
        onRemoveTrustedMember = viewModel::onRemoveMember,
        onAddAlert = onAddAlert,
        onAddThreat = onAddThreat,
        onRemoveThreat = viewModel::onRemoveThreat
    )
}

@Composable
fun DashboardScreen(
    robotConnected: Boolean,
    robotPowered: Boolean,
    securityMode: SecurityMode,
    alerts: List<Alert>,
    trustedMembers: List<Member>,
    threats: List<ThreatPerson>,
    carouselItems: List<CarouselItem>,
    onSecurityModeSelected: (SecurityMode) -> Unit,
    onToggleRobotPower: () -> Unit,
    onAddTrustedMember: () -> Unit,
    onRemoveTrustedMember: (String) -> Unit,
    onAddAlert: () -> Unit,
    onAddThreat: () -> Unit,
    onRemoveThreat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DashboardTopBar(
                robotConnected = robotConnected,
                robotPowered = robotPowered,
                onToggleRobotPower = onToggleRobotPower
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                RobotInfoCarouselSection(carouselItems = carouselItems)
            }
            item {
                AlertsHeader(onAddAlert = onAddAlert)
            }
            items(alerts) { alert ->
                AlertCard(alert = alert)
            }
            item {
                SecurityModeSection(
                    selectedMode = securityMode,
                    onModeSelected = onSecurityModeSelected
                )
            }
            item {
                TrustedMembersSection(
                    members = trustedMembers,
                    onAddTrustedMember = onAddTrustedMember,
                    onRemoveTrustedMember = onRemoveTrustedMember
                )
            }
            item {
                ThreatsSection(
                    threats = threats,
                    onAddThreat = onAddThreat,
                    onRemoveThreat = onRemoveThreat
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    robotConnected: Boolean,
    robotPowered: Boolean,
    onToggleRobotPower: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "AI Bodyguard",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        actions = {
            RobotPowerButton(
                robotPowered = robotPowered,
                onToggleRobotPower = onToggleRobotPower
            )
            Spacer(modifier = Modifier.width(8.dp))
            RobotStatusIndicator(robotConnected = robotConnected)
            Spacer(modifier = Modifier.width(16.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
private fun RobotPowerButton(
    robotPowered: Boolean,
    onToggleRobotPower: () -> Unit
) {
    val tint = if (robotPowered) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.error
    }
    IconButton(onClick = onToggleRobotPower) {
        Icon(
            imageVector = Icons.Default.PowerSettingsNew,
            contentDescription = if (robotPowered) "Turn robot off" else "Turn robot on",
            tint = tint
        )
    }
}

@Composable
private fun RobotStatusIndicator(robotConnected: Boolean) {
    val accent = if (robotConnected) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.error
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Text(
                text = if (robotConnected) "Connected" else "Disconnected",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RobotInfoCarouselSection(carouselItems: List<CarouselItem>) {
    val pagerState = rememberPagerState(pageCount = { carouselItems.size })

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader(
            title = "Robot Intelligence",
            subtitle = "Core AI Bodyguard capabilities ready on the home dashboard."
        )

        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp
        ) { page ->
            val item = carouselItems[page]
            val gradients = listOf(
                listOf(Color(0xFFF4B544), Color(0xFF8C5A00)),
                listOf(Color(0xFF5AD9A8), Color(0xFF135A4D)),
                listOf(Color(0xFF7CCBFF), Color(0xFF1B4D80))
            )
            FeatureCard(
                item = item,
                gradientColors = gradients[page % gradients.size]
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(carouselItems.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 22.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(
    item: CarouselItem,
    gradientColors: List<Color>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = item.imageRes),
                    contentDescription = item.title,
                    modifier = Modifier.size(78.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AlertsHeader(onAddAlert: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SectionHeader(
            title = "Recent Alerts",
            subtitle = "Latest robot notifications and incident summaries.",
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onAddAlert) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add alert",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AlertCard(alert: Alert) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = alert.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = alert.timestamp,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SecurityModeSection(
    selectedMode: SecurityMode,
    onModeSelected: (SecurityMode) -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader(
                title = "Security Mode",
                subtitle = if (selectedMode == SecurityMode.HOME) {
                    "Home mode keeps watch with a lighter response profile."
                } else {
                    "Away mode enables strict monitoring and faster escalation."
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModeButton(
                    title = "Owner is Home",
                    description = "Less aggressive",
                    selected = selectedMode == SecurityMode.HOME,
                    onClick = { onModeSelected(SecurityMode.HOME) }
                )
                ModeButton(
                    title = "Owner is Away",
                    description = "Strict monitoring",
                    selected = selectedMode == SecurityMode.AWAY,
                    onClick = { onModeSelected(SecurityMode.AWAY) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.ModeButton(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                textAlign = TextAlign.Center,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun TrustedMembersSection(
    members: List<Member>,
    onAddTrustedMember: () -> Unit,
    onRemoveTrustedMember: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SectionHeader(
                title = "Trusted Members",
                subtitle = "People recognized by the robot as safe occupants."
            )
            IconButton(onClick = onAddTrustedMember) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add trusted member",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(members, key = { it.id }) { member ->
                MemberCard(
                    member = member,
                    onRemove = { onRemoveTrustedMember(member.id) }
                )
            }
        }
    }
}

@Composable
private fun MemberCard(
    member: Member,
    onRemove: () -> Unit
) {
    Box(modifier = Modifier.width(128.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = member.imageRes),
                        contentDescription = member.name,
                        modifier = Modifier.size(34.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = member.role.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        RemoveBadge(
            contentDescription = "Remove ${member.name}",
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

@Composable
private fun ThreatsSection(
    threats: List<ThreatPerson>,
    onAddThreat: () -> Unit,
    onRemoveThreat: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SectionHeader(
                title = "Known Threats",
                subtitle = "People the robot should always flag — added from gallery photos.",
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onAddThreat) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add threat person",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        if (threats.isEmpty()) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Report,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "No threats registered. Tap + to upload pictures of a person to flag.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(threats, key = { it.id }) { threat ->
                    ThreatCard(
                        threat = threat,
                        onRemove = { onRemoveThreat(threat.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThreatCard(
    threat: ThreatPerson,
    onRemove: () -> Unit
) {
    Box(modifier = Modifier.width(140.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    val firstUri = threat.photoUris.firstOrNull()
                    if (firstUri != null) {
                        AsyncImage(
                            model = firstUri,
                            contentDescription = threat.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Report,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Text(
                    text = threat.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${threat.photoUris.size} photo${if (threat.photoUris.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        RemoveBadge(
            contentDescription = "Remove ${threat.name}",
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

@Composable
private fun RemoveBadge(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(4.dp)
            .size(24.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.error
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF09131B, showSystemUi = true)
@Composable
private fun DashboardScreenPreview() {
    AIBodyguardTheme {
        DashboardScreen(
            robotConnected = true,
            robotPowered = true,
            securityMode = SecurityMode.AWAY,
            alerts = listOf(
                Alert(
                    title = "Suspicious Person Detected",
                    description = "The robot identified an unknown visitor near the side gate.",
                    timestamp = "Now"
                ),
                Alert(
                    title = "Patrol Route Interrupted",
                    description = "Obstacle avoidance protocol was triggered in the driveway.",
                    timestamp = "7 min ago"
                )
            ),
            trustedMembers = listOf(
                Member(name = "Maya", imageRes = android.R.drawable.ic_menu_myplaces),
                Member(name = "Omar", imageRes = android.R.drawable.ic_menu_camera),
                Member(name = "Lea", imageRes = android.R.drawable.ic_menu_info_details)
            ),
            threats = emptyList(),
            carouselItems = listOf(
                CarouselItem(
                    title = "AI Threat Detection",
                    description = "Scans live surroundings and highlights unusual behavior.",
                    imageRes = android.R.drawable.ic_dialog_alert
                ),
                CarouselItem(
                    title = "Smart Patrol",
                    description = "Navigates your home perimeter autonomously.",
                    imageRes = android.R.drawable.ic_menu_compass
                ),
                CarouselItem(
                    title = "Trusted Face Recognition",
                    description = "Verifies recognized members before escalating.",
                    imageRes = android.R.drawable.ic_menu_camera
                )
            ),
            onSecurityModeSelected = {},
            onToggleRobotPower = {},
            onAddTrustedMember = {},
            onRemoveTrustedMember = {},
            onAddAlert = {},
            onAddThreat = {},
            onRemoveThreat = {}
        )
    }
}
