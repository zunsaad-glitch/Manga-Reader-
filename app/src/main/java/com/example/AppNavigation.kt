package com.example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val isAgeVerified by viewModel.isAgeVerified.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(isAgeVerified) {
        if (isAgeVerified && navController.currentDestination?.route == "age_verification") {
            navController.navigate("manga_list") {
                popUpTo("age_verification") { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = if (isAgeVerified) "manga_list" else "age_verification") {
        composable("age_verification") {
            AgeVerificationScreen(
                onVerified = {
                    scope.launch {
                        viewModel.settingsRepository.setAgeVerified(true)
                    }
                    navController.navigate("manga_list") {
                        popUpTo("age_verification") { inclusive = true }
                    }
                }
            )
        }
        
        composable("manga_list") {
            MangaListScreen(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToChapters = { mangaId ->
                    if (mangaId.all { it.isDigit() }) {
                        navController.navigate("nh_detail/$mangaId")
                    } else if (mangaId.startsWith("pururin_") || mangaId.startsWith("hfox_") || mangaId.startsWith("3h_") || mangaId.startsWith("nh_")) {
                        val prov = mangaId.substringBefore("_")
                        val idOnly = mangaId.substringAfter("_")
                        navController.navigate("janda_detail/$prov/$idOnly")
                    } else {
                        navController.navigate("chapters/$mangaId")
                    }
                },
                onNavigateToAuthor = { authorId -> navController.navigate("author/$authorId") },
                onNavigateToNhDetail = { id -> navController.navigate("nh_detail/$id") },
                onNavigateToNhArtist = { artistName -> navController.navigate("nh_artist/$artistName") },
                onNavigateToJandaDetail = { provider, id -> navController.navigate("janda_detail/$provider/$id") },
                onNavigateToReader = { chapterId -> navController.navigate("reader/$chapterId") },
                onNavigateToMillionDollar = { navController.navigate("million_dollar") }
            )
        }
        
        composable("million_dollar") {
            MillionDollarScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            "author/{authorId}",
            arguments = listOf(navArgument("authorId") { type = NavType.StringType })
        ) { backStackEntry ->
            val authorId = backStackEntry.arguments?.getString("authorId") ?: return@composable
            AuthorProfileScreen(
                authorId = authorId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToChapters = { mangaId ->
                    if (mangaId.all { it.isDigit() }) {
                        navController.navigate("nh_detail/$mangaId")
                    } else if (mangaId.startsWith("pururin_") || mangaId.startsWith("hfox_") || mangaId.startsWith("3h_") || mangaId.startsWith("nh_")) {
                        val prov = mangaId.substringBefore("_")
                        val idOnly = mangaId.substringAfter("_")
                        navController.navigate("janda_detail/$prov/$idOnly")
                    } else {
                        navController.navigate("chapters/$mangaId")
                    }
                }
            )
        }
        
        composable(
            "chapters/{mangaId}",
            arguments = listOf(navArgument("mangaId") { type = NavType.StringType })
        ) { backStackEntry ->
            val mangaId = backStackEntry.arguments?.getString("mangaId") ?: return@composable
            ChapterListScreen(
                mangaId = mangaId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToReader = { chapterId -> navController.navigate("reader/$chapterId") },
                onNavigateToAuthor = { authorId -> navController.navigate("author/$authorId") },
                onNavigateToManga = { targetMangaId ->
                    if (targetMangaId.all { it.isDigit() }) {
                        navController.navigate("nh_detail/$targetMangaId")
                    } else if (targetMangaId.startsWith("pururin_") || targetMangaId.startsWith("hfox_") || targetMangaId.startsWith("3h_") || targetMangaId.startsWith("nh_")) {
                        val prov = targetMangaId.substringBefore("_")
                        val idOnly = targetMangaId.substringAfter("_")
                        navController.navigate("janda_detail/$prov/$idOnly")
                    } else {
                        navController.navigate("chapters/$targetMangaId")
                    }
                }
            )
        }
        
        composable(
            "reader/{chapterId}",
            arguments = listOf(navArgument("chapterId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: return@composable
            ChapterReaderScreen(
                chapterId = chapterId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // ✅ nHentai Detail Screen
        composable(
            "nh_detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            NhDetailScreen(
                id = id,
                onBack = { navController.popBackStack() },
                onArtistClick = { artistName ->
                    navController.navigate("nh_artist/$artistName")
                },
                onStartReading = { pageIndex ->
                    navController.navigate("nh_reader/$id?page=$pageIndex")
                }
            )
        }

        // ✅ nHentai Artist Screen (e.g. replicating https://nhentai.com/fr/artist/urakan)
        composable(
            "nh_artist/{artistName}",
            arguments = listOf(navArgument("artistName") { type = NavType.StringType })
        ) { backStackEntry ->
            val artistName = backStackEntry.arguments?.getString("artistName") ?: return@composable
            NhArtistScreen(
                artistName = artistName,
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { id ->
                    navController.navigate("nh_detail/$id")
                }
            )
        }

        // ✅ nHentai Reader Screen
        composable(
            "nh_reader/{id}?page={page}",
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("page") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            val page = backStackEntry.arguments?.getInt("page") ?: 0
            NhReaderScreen(
                id = id,
                initialPage = page,
                onBack = { navController.popBackStack() }
            )
        }

        // ✅ JandaPress Multi-Provider Explorer Screen
        composable("jandapress") {
            JandaPressScreen(
                onNavigateToDetail = { provider, id ->
                    navController.navigate("janda_detail/$provider/$id")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ✅ JandaPress Detail Screen
        composable(
            "janda_detail/{provider}/{id}",
            arguments = listOf(
                navArgument("provider") { type = NavType.StringType },
                navArgument("id") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val provider = backStackEntry.arguments?.getString("provider") ?: "pururin"
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            JandaDetailScreen(
                provider = provider,
                id = id,
                onBack = { navController.popBackStack() },
                onArtistClick = { artistName ->
                    navController.navigate("nh_artist/$artistName")
                },
                onStartReading = { pageIndex ->
                    navController.navigate("janda_reader/$provider/$id?page=$pageIndex")
                }
            )
        }

        // ✅ JandaPress Reader Screen
        composable(
            "janda_reader/{provider}/{id}?page={page}",
            arguments = listOf(
                navArgument("provider") { type = NavType.StringType },
                navArgument("id") { type = NavType.StringType },
                navArgument("page") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val provider = backStackEntry.arguments?.getString("provider") ?: "pururin"
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            val page = backStackEntry.arguments?.getInt("page") ?: 0
            JandaReaderScreen(
                provider = provider,
                id = id,
                initialPage = page,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
