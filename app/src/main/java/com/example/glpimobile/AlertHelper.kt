package com.example.glpimobile

import android.app.Activity
import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import android.view.animation.AccelerateDecelerateInterpolator

object AlertHelper {

    /**
     * Exibe o alerta premium (premium alert) de forma consistente em qualquer Activity.
     * Requer que o layout da Activity inclua o layout_custom_alert.xml.
     */
    fun exibirAlertaPremium(activity: Activity, mensagem: String, isError: Boolean = false) {
        val alertCard = activity.findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_custom_alert)
        val alertText = activity.findViewById<TextView>(R.id.tv_alert_message)

        if (alertCard == null || alertText == null) return

        alertText.text = mensagem
        
        // Cor do alerta (Azul para sucesso/info, Vermelho para erro)
        val backgroundColor = if (isError) {
            androidx.core.content.ContextCompat.getColor(activity, R.color.vermelho_forte)
        } else {
            androidx.core.content.ContextCompat.getColor(activity, R.color.azul_glpi)
        }
        alertCard.setCardBackgroundColor(ColorStateList.valueOf(backgroundColor))

        alertCard.animate().cancel()
        alertCard.visibility = View.VISIBLE
        alertCard.alpha = 0f
        alertCard.translationY = 50f

        alertCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                alertCard.animate()
                    .alpha(0f)
                    .translationY(50f)
                    .setDuration(300)
                    .setStartDelay(2500)
                    .withEndAction { alertCard.visibility = View.GONE }
                    .start()
            }
            .start()
    }

    /**
     * Exibe o alerta premium (premium alert) de forma consistente usando um View contentor
     * (útil para Diálogos e layouts flutuantes).
     */
    fun exibirAlertaPremium(rootView: View, mensagem: String, isError: Boolean = false) {
        val alertCard = rootView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_custom_alert)
        val alertText = rootView.findViewById<TextView>(R.id.tv_alert_message)

        if (alertCard == null || alertText == null) return

        alertText.text = mensagem
        val context = rootView.context
        
        // Cor do alerta (Azul para sucesso/info, Vermelho para erro)
        val backgroundColor = if (isError) {
            androidx.core.content.ContextCompat.getColor(context, R.color.vermelho_forte)
        } else {
            androidx.core.content.ContextCompat.getColor(context, R.color.azul_glpi)
        }
        alertCard.setCardBackgroundColor(ColorStateList.valueOf(backgroundColor))

        alertCard.animate().cancel()
        alertCard.visibility = View.VISIBLE
        alertCard.alpha = 0f
        alertCard.translationY = 50f

        alertCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                alertCard.animate()
                    .alpha(0f)
                    .translationY(50f)
                    .setDuration(300)
                    .setStartDelay(2500)
                    .withEndAction { alertCard.visibility = View.GONE }
                    .start()
            }
            .start()
    }
}
