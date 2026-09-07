package com.kotomichi.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotomichi.app.R
import com.kotomichi.di.get
import com.kotomichi.model.Deck
import com.kotomichi.model.UserProfile
import com.kotomichi.usecase.AuthUseCase
import com.kotomichi.usecase.DeckProgressUseCase
import com.kotomichi.usecase.GamificationUseCase
import com.kotomichi.usecase.ReviewCardUseCase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToLearn: (Long) -> Unit,
    onNavigateToReview: () -> Unit,
    onLogout: () -> Unit
) {
    val authUseCase: AuthUseCase = get()
    val deckProgressUseCase: DeckProgressUseCase = get()
    val gamificationUseCase: GamificationUseCase = get()
    val reviewUseCase: ReviewCardUseCase = get()
    val syncRepository: com.kotomichi.repository.SyncRepository = get()
    val deckRepository: com.kotomichi.repository.DeckRepository = get()

    val user by authUseCase.currentUser.collectAsStateWithLifecycle(null)
    var decks by remember { mutableStateOf<List<Deck>>(emptyList()) }
    var deckProgressMap by remember { mutableStateOf<Map<Long, com.kotomichi.model.DeckProgress>>(emptyMap()) }
    val dueCount by reviewUseCase.observeDueCount(user?.id ?: "").collectAsStateWithLifecycle(0)
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val userId = user?.id
    LaunchedEffect(userId) {
        if (userId == null) {
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        loadError = null
        runCatching { syncRepository.pullMasterData() }
        val progressResult = runCatching { syncRepository.pullUserData() }
        progressResult.exceptionOrNull()?.let { loadError = it.message }
        decks = deckRepository.getPublishedDecks()
        deckProgressMap = deckProgressUseCase.getAllDeckProgress(userId).associateBy { it.deckId }
        isLoading = false
    }
    
    val currentLevel = user?.currentLevel ?: 1
    val totalExp = user?.totalExp ?: 0L
    val nextLevelExp = gamificationUseCase.calculateExpForLevel(currentLevel + 1)
    val currentLevelExp = gamificationUseCase.calculateExpForLevel(currentLevel)
    val expProgress = gamificationUseCase.calculateExpProgress(currentLevel, totalExp)
    val currentStreak = user?.currentStreak ?: 0
    
    androidx.compose.material3.Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                actions = {
                    IconButton(onClick = { /* Open profile */ }) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profil",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = { /* Open settings */ }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Pengaturan",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Filled.Logout,
                            contentDescription = "Keluar",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (loadError != null) {
                    androidx.compose.material3.Surface(
                        color = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tidak dapat memuat progress dari server: $loadError",
                            modifier = Modifier.padding(12.dp),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp
                        )
                    }
                }
                // User Profile Card
                ProfileCard(
                    user = user ?: com.kotomichi.model.UserProfile(id = ""),
                    currentLevel = if (user != null) currentLevel else 1,
                    totalExp = if (user != null) totalExp else 0L,
                    nextLevelExp = nextLevelExp,
                    currentLevelExp = currentLevelExp,
                    expProgress = expProgress,
                    currentStreak = if (user != null) currentStreak else 0
                )
                
                // Action Buttons
                ActionButtonsRow(
                    dueCount = dueCount,
                    onLearnClick = { 
                        if (decks.isNotEmpty()) {
                            onNavigateToLearn(decks.first().id)
                        }
                    },
                    onReviewClick = onNavigateToReview
                )
                
                // Deck List
                DeckListSection(
                    decks = decks,
                    deckProgressMap = deckProgressMap,
                    onDeckClick = onNavigateToLearn
                )
                
                // Activity Calendar (placeholder)
                ActivityCalendarSection()
            }
        }
    }
}

@Composable
fun ProfileCard(
    user: UserProfile,
    currentLevel: Int,
    totalExp: Long,
    nextLevelExp: Long,
    currentLevelExp: Long,
    expProgress: Double,
    currentStreak: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Selamat datang kembali, ${user.name}!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Level $currentLevel • ${totalExp.toString()} EXP",
                        fontSize = 14.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Level Badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(28.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = "$currentLevel",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            // EXP Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Progress ke Level ${currentLevel + 1}", fontSize = 12.sp)
                    Text("${(totalExp - currentLevelExp)} / ${(nextLevelExp - currentLevelExp)} EXP", fontSize = 12.sp)
                }
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { (expProgress / 100).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    trackColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                )
            }
            
            // Streak
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StreakItem(
                    icon = Icons.Filled.LocalFireDepartment,
                    label = "Streak",
                    value = "$currentStreak hari",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary
                )
                StreakItem(
                    icon = Icons.Filled.Psychology,
                    label = "Kuasai",
                    value = "${user.totalExp / 100} kata",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun StreakItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = "", tint = color, modifier = Modifier.size(24.dp))
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 12.sp, color = color.copy(alpha = 0.8f))
    }
}

@Composable
fun ActionButtonsRow(
    dueCount: Int,
    onLearnClick: () -> Unit,
    onReviewClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onLearnClick,
            modifier = Modifier.weight(1f).height(56.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Belajar Baru", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        
        Button(
            onClick = onReviewClick,
            modifier = Modifier.weight(1f).height(56.dp),
            enabled = dueCount > 0,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                disabledContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Review", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                if (dueCount > 0) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        Text(
                            text = dueCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onError,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeckListSection(
    decks: List<Deck>,
    deckProgressMap: Map<Long, com.kotomichi.model.DeckProgress>,
    onDeckClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Daftar Bab", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("${decks.size} bab tersedia", fontSize = 14.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(decks) { deck ->
                val progress = deckProgressMap[deck.id]
                DeckItem(deck = deck, progress = progress, onClick = { onDeckClick(deck.id) })
            }
        }
    }
}

@Composable
fun DeckItem(
    deck: Deck,
    progress: com.kotomichi.model.DeckProgress?,
    onClick: () -> Unit
) {
    val mastery = progress?.masteryPercent ?: 0.0
    val isUnlocked = progress?.averageRetrievability ?: 0.0 >= 0.90 || deck.orderIndex == 1
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxWidth(),
        onClick = onClick,
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isUnlocked) androidx.compose.material3.MaterialTheme.colorScheme.surface else androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = deck.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = "JLPT ${deck.jlptLevel?.name ?: ""}", fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                if (!isUnlocked) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Terkunci",
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Kemahiran", fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { (mastery / 100).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    )
                    Text("%.0f%%".format(mastery), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                
                if (progress != null) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Kartu", fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${progress.learnedVocab} / ${progress.totalVocab}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityCalendarSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Kalender Aktivitas", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
            Text("Heatmap aktivitas belajar akan ditampilkan di sini", 
                fontSize = 14.sp, 
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
            )
        }
    }
}