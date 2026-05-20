package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.Station
import com.example.ui.theme.*
import com.example.viewmodel.RadioViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: RadioViewModel by viewModels()

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Дозвольте сповіщення для керування радіо з екрану сповіщень",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Init media controller connection
        viewModel.initController(this)

        // Request notification permissions for API 33+ devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RadioAppScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioAppScreen(viewModel: RadioViewModel) {
    val context = LocalContext.current
    
    // Core state flow collecting
    val stations by viewModel.stations.collectAsState()
    val genres by viewModel.genres.collectAsState()
    val currentStation by viewModel.currentPlayingStation.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val sleepTimerSeconds by viewModel.sleepTimerRemainingSeconds.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val isFavoriteOnly by viewModel.isFavoriteOnly.collectAsState()

    // Dialog flags
    var showAddStationDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = "Плеєр",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("app_logo")
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "UA Радіо",
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Трансляція України",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                actions = {
                    // Sleep Countdown widget
                    IconButton(
                        onClick = { showSleepTimerDialog = true },
                        modifier = Modifier.testTag("sleep_timer_top_btn")
                    ) {
                        val isActive = sleepTimerSeconds > 0
                        BadgedBox(
                            badge = {
                                if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.secondary,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = formatShortSeconds(sleepTimerSeconds),
                                            color = SpaceObsidian,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isActive) Icons.Default.HourglassFull else Icons.Outlined.HourglassEmpty,
                                contentDescription = "Таймер сну",
                                tint = if (isActive) MaterialTheme.colorScheme.secondary else TextPrimary
                            )
                        }
                    }

                    // Sync action
                    IconButton(
                        onClick = {
                            viewModel.syncWithLatestStations {
                                Toast.makeText(
                                    context,
                                    "Список радіостанцій успішно оновлено з OnlineRadioBox!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.testTag("sync_stations_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Оновити список з OnlineRadioBox",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Plus action
                    IconButton(
                        onClick = { showAddStationDialog = true },
                        modifier = Modifier.testTag("add_station_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Додати радіо",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = SpaceObsidian,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddStationDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = SpaceObsidian,
                modifier = Modifier
                    .padding(bottom = if (currentStation != null) 90.dp else 16.dp)
                    .testTag("add_custom_station_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Додати свою станцію"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceObsidian)
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                
                // Search field OutlinedTextField
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Пошук радіостанцій...", color = TextSecondary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Пошук",
                            tint = TextSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Очистити",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = SpaceObsidianLight,
                        unfocusedContainerColor = SpaceObsidianLight
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("search_bar")
                )

                // Navigation Filter Category Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Жанрові категорії",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )

                    // Custom ultra-stable Favorites selector
                    val isFav = isFavoriteOnly
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isFav) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f) else SpaceObsidianLight)
                            .border(
                                1.dp,
                                if (isFav) MaterialTheme.colorScheme.tertiary else GlassBorder,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.isFavoriteOnly.value = !isFav }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("favorites_filter_chip"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFav) MaterialTheme.colorScheme.tertiary else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Улюблені",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isFav) MaterialTheme.colorScheme.tertiary else TextPrimary
                        )
                    }
                }

                // Custom ultra-stable responsive list of Genre selector
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(genres) { genre ->
                        val isSelected = genre == selectedGenre
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else SpaceObsidianLight)
                                .border(
                                    1.dp,
                                    if (isSelected) Color.Transparent else GlassBorder,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { viewModel.selectedGenre.value = genre }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("genre_chip_$genre")
                        ) {
                            Text(
                                text = genre,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) SpaceObsidian else TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Render stations or empty state safely
                if (stations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radio,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Не знайдено жодної станції",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Спробуйте змінити фільтри або додайте свою власну інтернет-станцію!",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(
                            start = 16.dp, 
                            end = 16.dp, 
                            top = 8.dp, 
                            bottom = if (currentStation != null) 110.dp else 32.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(stations, key = { it.id }) { station ->
                            val isPlayingThis = currentStation?.id == station.id && isPlaying
                            val isLoadingThis = currentStation?.id == station.id && isLoading
                            StationCardItem(
                                station = station,
                                isPlayingThis = isPlayingThis,
                                isLoadingThis = isLoadingThis,
                                onPlayClick = { viewModel.playStation(station) },
                                onFavoriteToggle = { viewModel.toggleFavorite(station) },
                                onDeleteClick = {
                                    viewModel.deleteStation(station)
                                    Toast.makeText(context, "${station.name} видалено", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // Expandable bottom glass card Now Playing Bar
            AnimatedVisibility(
                visible = currentStation != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                currentStation?.let { station ->
                    NowPlayingBar(
                        station = station,
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        sleepTimerSeconds = sleepTimerSeconds,
                        onPlayPauseToggle = { viewModel.togglePlayPause() },
                        onSleepTimerClick = { showSleepTimerDialog = true }
                    )
                }
            }
        }
    }

    // Interactive custom Dialogs
    if (showAddStationDialog) {
        AddCustomStationDialog(
            onDismiss = { showAddStationDialog = false },
            onAddStation = { name, url, genre, icon ->
                viewModel.addCustomStation(name, url, genre, icon)
                showAddStationDialog = false
            }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            currentRemainingSeconds = sleepTimerSeconds,
            onDismiss = { showSleepTimerDialog = false },
            onSelectTimer = { minutes ->
                viewModel.setSleepTimer(minutes)
                showSleepTimerDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StationCardItem(
    station: Station,
    isPlayingThis: Boolean,
    isLoadingThis: Boolean,
    onPlayClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val cardBg = if (isPlayingThis) {
        Brush.linearGradient(
            colors = listOf(SpaceObsidianLight, SpaceObsidianLight.copy(alpha = 0.8f))
        )
    } else {
        Brush.linearGradient(
            colors = listOf(SpaceObsidianLight.copy(alpha = 0.6f), SpaceObsidianLight.copy(alpha = 0.4f))
        )
    }
    
    val isPlayingSpinAngle by animateFloatAsState(
        targetValue = if (isPlayingThis) 360f else 0f,
        animationSpec = if (isPlayingThis) {
            infiniteRepeatable(
                animation = tween(12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        } else {
            snap()
        },
        label = "vinylDisk"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isPlayingThis) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else GlassBorder,
                RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                onClick = onPlayClick,
                onLongClick = { }
            )
            .testTag("station_card_${station.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBg)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rounded adaptive vinyl icon representer with fallback underlay loading text
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                getGenreColor(station.genre).copy(alpha = 0.9f),
                                getGenreColor(station.genre).copy(alpha = 0.3f)
                            )
                        )
                    )
                    .border(1.dp, GlassBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = station.name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )

                AsyncImage(
                    model = station.iconUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .clip(CircleShape)
                        .rotate(isPlayingSpinAngle)
                )

                if (isLoadingThis) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else if (isPlayingThis) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    )
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Metadata Detail
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = station.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (station.isCustom) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "користувач",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = station.genre,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }

            // Interactive Actions row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Garbage deletes for custom additions
                if (station.isCustom) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.testTag("delete_btn_${station.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Видалити",
                            tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                        )
                    }
                }

                // Star bookmark button
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.testTag("bookmark_btn_${station.id}")
                ) {
                    Icon(
                        imageVector = if (station.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Улюблене",
                        tint = if (station.isFavorite) MaterialTheme.colorScheme.tertiary else TextSecondary
                    )
                }

                // Embedded Player action triggers
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isPlayingThis) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else SpaceObsidian,
                            CircleShape
                        )
                        .testTag("play_btn_${station.id}")
                ) {
                    Icon(
                        imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Грати",
                        tint = if (isPlayingThis) MaterialTheme.colorScheme.primary else TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun NowPlayingBar(
    station: Station,
    isPlaying: Boolean,
    isLoading: Boolean,
    sleepTimerSeconds: Int,
    onPlayPauseToggle: () -> Unit,
    onSleepTimerClick: () -> Unit
) {
    // Elegant bouncing visualizer waveform frequencies
    val infiniteTransition = rememberInfiniteTransition(label = "musicVisualizer")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "leftwave"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "midwave"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rightwave"
    )

    Surface(
        color = SpaceObsidianLight.copy(alpha = 0.92f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .testTag("now_playing_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding() // Safely clears system gestural pill
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            
            // Continuous indicator progress stripe
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(GlassBorder)
            ) {
                if (isLoading) {
                    val progressAnim by rememberInfiniteTransition("bufferLines").animateFloat(
                        initialValue = -1f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "bufferingStripe"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .fillMaxHeight()
                            .align(Alignment.CenterStart)
                            .graphicsLayer {
                                translationX = (progressAnim * 180).dp.toPx()
                            }
                            .background(MaterialTheme.colorScheme.primary)
                    )
                } else if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Micro logo with fallback underlay
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    getGenreColor(station.genre).copy(alpha = 0.9f),
                                    getGenreColor(station.genre).copy(alpha = 0.3f)
                                )
                            )
                        )
                        .border(1.dp, GlassBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = station.name.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )

                    AsyncImage(
                        model = station.iconUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Detail display metadata
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = station.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isLoading) {
                            Text(
                                text = "Буферизація...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = if (isPlaying) "Зараз грає" else "На паузі",
                                fontSize = 11.sp,
                                color = if (isPlaying) MaterialTheme.colorScheme.secondary else TextSecondary
                            )
                            if (isPlaying) {
                                Spacer(modifier = Modifier.width(6.dp))
                                // Bouncing EQ micro animation
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.Bottom,
                                    modifier = Modifier.height(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .fillMaxHeight(h1)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .fillMaxHeight(h2)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .fillMaxHeight(h3)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                    }
                }

                // Timer Action Button
                IconButton(
                    onClick = onSleepTimerClick,
                    modifier = Modifier.testTag("sleep_timer_btn")
                ) {
                    val isActive = sleepTimerSeconds > 0
                    Icon(
                        imageVector = if (isActive) Icons.Default.HourglassFull else Icons.Outlined.HourglassEmpty,
                        contentDescription = "Таймер сну",
                        tint = if (isActive) MaterialTheme.colorScheme.secondary else TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Major controller action
                FilledIconButton(
                    onClick = onPlayPauseToggle,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isPlaying) MaterialTheme.colorScheme.primary else TextPrimary,
                        contentColor = SpaceObsidian
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("onplay_pause_bottom_btn")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Керування плеєром",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SleepTimerDialog(
    currentRemainingSeconds: Int,
    onDismiss: () -> Unit,
    onSelectTimer: (minutes: Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceObsidianLight),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .testTag("sleep_timer_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassEmpty,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Таймер сну",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (currentRemainingSeconds > 0) {
                        "Залишилося: ${formatLongSeconds(currentRemainingSeconds)}"
                    } else {
                        "Виберіть час, через який автоматично зупинити радіо"
                    },
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Preset options
                val presets = listOf(
                    0 to "Вимкнути таймер",
                    5 to "5 хвилин",
                    15 to "15 хвилин",
                    30 to "30 хвилин",
                    60 to "60 хвилин",
                    90 to "90 хвилин"
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { (minutes, label) ->
                        Button(
                            onClick = { onSelectTimer(minutes) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (minutes == 0) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                                contentColor = if (minutes == 0) MaterialTheme.colorScheme.tertiary else TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("timer_btn_$minutes")
                        ) {
                            Text(text = label, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("timer_close_btn")
                ) {
                    Text("Закрити")
                }
            }
        }
    }
}

@Composable
fun AddCustomStationDialog(
    onDismiss: () -> Unit,
    onAddStation: (name: String, url: String, genre: String, icon: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var streamUrl by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("Моє радіо") }
    var iconUrl by remember { mutableStateOf("") }

    var isErrorUrl by remember { mutableStateOf(false) }
    var isErrorName by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceObsidianLight),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .testTag("add_station_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Додати свою станцію",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Name input
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isErrorName = false
                    },
                    label = { Text("Назва станції", color = TextSecondary) },
                    isError = isErrorName,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_form_name")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Stream URL input
                OutlinedTextField(
                    value = streamUrl,
                    onValueChange = {
                        streamUrl = it
                        isErrorUrl = false
                    },
                    label = { Text("URL потоку (http/https)", color = TextSecondary) },
                    isError = isErrorUrl,
                    placeholder = { Text("https://example.com/stream.mp3", fontSize = 12.sp, color = TextSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_form_url")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Genre input
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text("Категорія / Жанр", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_form_genre")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Optional artwork icon URL input
                OutlinedTextField(
                    value = iconUrl,
                    onValueChange = { iconUrl = it },
                    label = { Text("Посилання на логотип (необов'язково)", color = TextSecondary) },
                    placeholder = { Text("порожньо для випадкового логотипу", fontSize = 11.sp, color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_form_logo")
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("form_cancel")
                    ) {
                        Text("Скасувати")
                    }

                    Button(
                        onClick = {
                            var hasError = false
                            if (name.isBlank()) {
                                isErrorName = true
                                hasError = true
                            }
                            if (streamUrl.isBlank() || (!streamUrl.startsWith("http://") && !streamUrl.startsWith("https://"))) {
                                isErrorUrl = true
                                hasError = true
                            }
                            if (!hasError) {
                                onAddStation(name, streamUrl, genre, iconUrl.ifBlank { null })
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = SpaceObsidian
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("form_submit")
                    ) {
                        Text("Додати")
                    }
                }
            }
        }
    }
}

private fun formatShortSeconds(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return if (m > 0) "${m}м" else "${s}с"
}

private fun formatLongSeconds(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return String.format("%02d:%02d", m, s)
}

private fun getGenreColor(genre: String): Color {
    return when (genre) {
        "Поп" -> Color(0xFF38BDF8) // Neon Blue
        "Рок" -> Color(0xFFF43F5E) // Crimson Glow
        "Танцювальна" -> Color(0xFFA855F7) // Purple
        "Релакс" -> Color(0xFF10B981) // Emerald Green
        "Суспільне" -> Color(0xFFFBBF24) // Soft Yellow
        "Класика" -> Color(0xFFE2E8F0) // Platinum White
        "Українська" -> Color(0xFFF59E0B) // Amber Gold
        "Новини/Розмови" -> Color(0xFF3B82F6) // Deep Blue
        "Ретро" -> Color(0xFFEC4899) // Retro Pink
        "Джаз" -> Color(0xFFF472B6) // Light Pink
        "Шансон" -> Color(0xFF84CC16) // Lime Green
        else -> Color(0xFF38BDF8) // Neon Blue primary fallback
    }
}
