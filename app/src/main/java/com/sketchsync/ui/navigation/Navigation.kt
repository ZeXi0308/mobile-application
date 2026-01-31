package com.sketchsync.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sketchsync.data.model.AuthState
import com.sketchsync.ui.auth.AuthViewModel
import com.sketchsync.ui.auth.LoginScreen
import com.sketchsync.ui.auth.RegisterScreen
import com.sketchsync.ui.canvas.CanvasScreen
import com.sketchsync.ui.gallery.GalleryScreen
import com.sketchsync.ui.profile.ProfileScreen
import com.sketchsync.ui.room.RoomListScreen
import com.sketchsync.ui.splash.SplashScreen

/**
 * 应用导航
 */
@Composable
fun SketchSyncNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()
    
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // 启动页
        composable(Screen.Splash.route) {
            SplashScreen(
                authState = authState,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToRooms = {
                    navController.navigate(Screen.RoomList.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        // 登录页
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.RoomList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        // 注册页
        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.RoomList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        // 房间列表页
        composable(Screen.RoomList.route) {
            RoomListScreen(
                onNavigateToCanvas = { roomId ->
                    navController.navigate(Screen.Canvas.createRoute(roomId))
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.RoomList.route) { inclusive = true }
                    }
                }
            )
        }
        
        // 画布页
        composable(
            route = Screen.Canvas.route,
            arguments = listOf(navArgument("roomId") { type = NavType.StringType })
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            CanvasScreen(
                roomId = roomId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // 个人中心页
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToGallery = {
                    navController.navigate(Screen.Gallery.route)
                },
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.RoomList.route) { inclusive = true }
                    }
                }
            )
        }
        
        // 画廊页
        composable(Screen.Gallery.route) {
            GalleryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * 路由定义
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object RoomList : Screen("rooms")
    object Profile : Screen("profile")
    object Gallery : Screen("gallery")
    object Canvas : Screen("canvas/{roomId}") {
        fun createRoute(roomId: String) = "canvas/$roomId"
    }
}
