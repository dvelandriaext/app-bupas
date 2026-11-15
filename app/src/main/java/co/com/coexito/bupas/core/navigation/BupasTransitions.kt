package co.com.coexito.bupas.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation.NavBackStackEntry

/**
 * Transiciones estándar de navegación y cambio de pasos para la app móvil BUPAS.
 * Sigue la cadencia de movimiento suave de Material 3 (eje horizontal compartido + fade).
 */
object BupasTransitions {
    private const val ENTER_DURATION_MS = 320
    private const val EXIT_DURATION_MS = 240

    val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(
            animationSpec = tween(durationMillis = ENTER_DURATION_MS, easing = FastOutSlowInEasing)
        ) + slideInHorizontally(
            animationSpec = tween(durationMillis = ENTER_DURATION_MS, easing = FastOutSlowInEasing),
            initialOffsetX = { fullWidth -> fullWidth / 5 }
        )
    }

    val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(
            animationSpec = tween(durationMillis = EXIT_DURATION_MS, easing = FastOutSlowInEasing)
        ) + slideOutHorizontally(
            animationSpec = tween(durationMillis = ENTER_DURATION_MS, easing = FastOutSlowInEasing),
            targetOffsetX = { fullWidth -> -fullWidth / 8 }
        )
    }

    val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(
            animationSpec = tween(durationMillis = ENTER_DURATION_MS, easing = FastOutSlowInEasing)
        ) + slideInHorizontally(
            animationSpec = tween(durationMillis = ENTER_DURATION_MS, easing = FastOutSlowInEasing),
            initialOffsetX = { fullWidth -> -fullWidth / 5 }
        )
    }

    val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(
            animationSpec = tween(durationMillis = EXIT_DURATION_MS, easing = FastOutSlowInEasing)
        ) + slideOutHorizontally(
            animationSpec = tween(durationMillis = ENTER_DURATION_MS, easing = FastOutSlowInEasing),
            targetOffsetX = { fullWidth -> fullWidth / 5 }
        )
    }

    /**
     * Transición direccional reutilizable para cambios de paso dentro de una misma pantalla
     * (por ejemplo, Login -> Doble Autenticación 2FA).
     */
    fun stepTransition(isForward: Boolean): ContentTransform {
        val enterOffset: (Int) -> Int = { width -> if (isForward) width / 4 else -width / 4 }
        val exitOffset: (Int) -> Int = { width -> if (isForward) -width / 6 else width / 6 }

        val enter = fadeIn(
            animationSpec = tween(durationMillis = ENTER_DURATION_MS, easing = FastOutSlowInEasing)
        ) + slideInHorizontally(
            animationSpec = tween(durationMillis = ENTER_DURATION_MS, easing = FastOutSlowInEasing),
            initialOffsetX = enterOffset
        )

        val exit = fadeOut(
            animationSpec = tween(durationMillis = EXIT_DURATION_MS, easing = FastOutSlowInEasing)
        ) + slideOutHorizontally(
            animationSpec = tween(durationMillis = ENTER_DURATION_MS, easing = FastOutSlowInEasing),
            targetOffsetX = exitOffset
        )

        return enter togetherWith exit
    }
}
