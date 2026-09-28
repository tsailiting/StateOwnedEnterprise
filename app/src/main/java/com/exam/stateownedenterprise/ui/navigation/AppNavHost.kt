package com.exam.stateownedenterprise.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.exam.stateownedenterprise.di.AppContainer
import com.exam.stateownedenterprise.ui.home.HomeScreen
import com.exam.stateownedenterprise.ui.home.HomeViewModel
import com.exam.stateownedenterprise.ui.home.HomeViewModelFactory
import com.exam.stateownedenterprise.ui.practice.PracticeScreen
import com.exam.stateownedenterprise.ui.practice.PracticeViewModel
import com.exam.stateownedenterprise.ui.practice.PracticeViewModelFactory
import com.exam.stateownedenterprise.ui.result.ResultScreen
import com.exam.stateownedenterprise.ui.result.ResultViewModel
import com.exam.stateownedenterprise.ui.result.ResultViewModelFactory
import com.exam.stateownedenterprise.ui.review.ReviewScreen
import com.exam.stateownedenterprise.ui.review.ReviewViewModel
import com.exam.stateownedenterprise.ui.review.ReviewViewModelFactory

object Destinations {
    const val HOME = "home"
    const val PRACTICE = "practice/{examSubjectId}"
    const val RESULT = "result/{attemptId}"
    const val REVIEW = "review/{attemptId}"

    fun practiceRoute(examSubjectId: Long): String = "practice/$examSubjectId"
    fun resultRoute(attemptId: Long): String = "result/$attemptId"
    fun reviewRoute(attemptId: Long): String = "review/$attemptId"
}

@Composable
fun AppNavHost(
    appContainer: AppContainer,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.HOME,
        modifier = modifier
    ) {
        composable(Destinations.HOME) {
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(appContainer.questionRepository)
            )
            HomeScreen(
                viewModel = viewModel,
                onStartPractice = { examSubjectId ->
                    navController.navigate(Destinations.practiceRoute(examSubjectId))
                }
            )
        }

        composable(
            route = Destinations.PRACTICE,
            arguments = listOf(
                navArgument("examSubjectId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val examSubjectId = backStackEntry.arguments?.getLong("examSubjectId") ?: return@composable
            val viewModel: PracticeViewModel = viewModel(
                factory = PracticeViewModelFactory(appContainer.questionRepository, examSubjectId)
            )
            PracticeScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onPracticeFinished = { attemptId ->
                    navController.navigate(Destinations.resultRoute(attemptId))
                }
            )
        }

        composable(
            route = Destinations.RESULT,
            arguments = listOf(
                navArgument("attemptId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val attemptId = backStackEntry.arguments?.getLong("attemptId") ?: return@composable
            val viewModel: ResultViewModel = viewModel(
                factory = ResultViewModelFactory(appContainer.questionRepository, attemptId)
            )
            ResultScreen(
                viewModel = viewModel,
                onGoHome = {
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.HOME) { inclusive = true }
                    }
                },
                onRePractice = { examSubjectId ->
                    navController.navigate(Destinations.practiceRoute(examSubjectId)) {
                        popUpTo(Destinations.HOME) { inclusive = false }
                    }
                },
                onViewReview = { id ->
                    navController.navigate(Destinations.reviewRoute(id))
                }
            )
        }

        composable(
            route = Destinations.REVIEW,
            arguments = listOf(
                navArgument("attemptId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val attemptId = backStackEntry.arguments?.getLong("attemptId") ?: return@composable
            val viewModel: ReviewViewModel = viewModel(
                factory = ReviewViewModelFactory(appContainer.questionRepository, attemptId)
            )
            ReviewScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
