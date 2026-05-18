package com.example.glpimobile

import android.app.Activity
import com.airbnb.lottie.LottieAnimationView

/**
 * Helper para padronizar as animações de filtros e ordenação em toda a aplicação.
 */
object AnimationHelper {

    /**
     * Procura e executa a animação de Lottie associada a filtros ou ordenação na Activity atual.
     * Suporta diversos IDs usados no projeto para garantir uma abordagem "universal".
     */
    fun playFilterAnimation(activity: Activity) {
        val filterIds = listOf(
            R.id.lottie_filtro,
            R.id.lottie_filtro_history,
            R.id.lottie_filtro_profile
        )
        playAnimationForIds(activity, filterIds)
    }

    fun playOrderAnimation(activity: Activity) {
        val orderIds = listOf(
            R.id.lottie_ordem,
            R.id.lottie_ordem_progress,
            R.id.lottie_ordem_priority,
            R.id.lottie_ordem_open,
            R.id.lottie_ordem_res
        )
        playAnimationForIds(activity, orderIds)
    }

    private fun playAnimationForIds(activity: Activity, ids: List<Int>) {
        for (id in ids) {
            try {
                activity.findViewById<LottieAnimationView>(id)?.let { view ->
                    view.progress = 0f
                    view.playAnimation()
                }
            } catch (e: Exception) {
            }
        }
    }
}

fun Activity.playFilterAnimation() {
    AnimationHelper.playFilterAnimation(this)
}

fun Activity.playOrderAnimation() {
    AnimationHelper.playOrderAnimation(this)
}
