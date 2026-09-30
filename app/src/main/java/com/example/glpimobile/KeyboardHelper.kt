package com.example.glpimobile

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AbsListView
import android.widget.EditText
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView

object KeyboardHelper {

    /**
     * Aplica insets dinâmicos nos botões do fundo para evitar que fiquem sobrepostos
     * pela barra de navegação do sistema Android (gestos ou 3 botões).
     */
    fun applyDynamicBottomInsets(view: View) {
        val isTarget = when (view.id) {
            R.id.container_btn_abrir_ticket,
            R.id.container_btn_adicionar,
            R.id.container_btn_enviar,
            R.id.btn_guardar_settings_container,
            R.id.container_btn_confirmar_res,
            R.id.pagination_edit -> true
            else -> false
        }
        if (isTarget) {
            // Remove sombras/elevação default dos botões dentro do container
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    val child = view.getChildAt(i)
                    child.elevation = 0f
                    child.stateListAnimator = null
                }
            }
            ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val layoutParams = v.layoutParams as? ViewGroup.MarginLayoutParams
                if (layoutParams != null) {
                    val baseMargin = if (v.id == R.id.pagination_edit) {
                        (v.context.resources.displayMetrics.density * 32).toInt()
                    } else {
                        (v.context.resources.displayMetrics.density * 8).toInt()
                    }
                    layoutParams.bottomMargin = systemBars.bottom + baseMargin
                    v.layoutParams = layoutParams
                }
                insets
            }
            if (view.isAttachedToWindow) {
                view.requestApplyInsets()
            } else {
                view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        v.requestApplyInsets()
                        v.removeOnAttachStateChangeListener(this)
                    }
                    override fun onViewDetachedFromWindow(v: View) {}
                })
            }
        }
    }

    /**
     * Verifica se o toque foi fora do EditText focado e, se sim, retira o foco e esconde o teclado.
     * Deve ser chamado no override do dispatchTouchEvent da Activity.
     */
    fun handleTouchOutside(activity: Activity, ev: MotionEvent?) {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val v = activity.currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    v.clearFocus()
                    val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
    }

    /**
     * Esconde o teclado se houver um EditText focado e retira o foco do mesmo.
     */
    fun hideKeyboard(activity: Activity) {
        val focusedView = activity.currentFocus
        if (focusedView is EditText) {
            focusedView.clearFocus()
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(focusedView.windowToken, 0)
        }
    }

    /**
     * Percorre a hierarquia de views e adiciona listeners de scroll / touch
     * para esconder o teclado ao fazer scroll. Usa isolamento por tag para evitar duplicados.
     */
    fun setupDismissKeyboardOnScroll(view: View, activity: Activity) {
        applyDynamicBottomInsets(view)
        when (view) {
            is NestedScrollView -> {
                if (view.tag != "keyboard_dismiss_scroll_setup") {
                    view.tag = "keyboard_dismiss_scroll_setup"
                    view.setOnTouchListener(object : View.OnTouchListener {
                        private var startY = 0f
                        private var startX = 0f
                        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                            if (event != null) {
                                when (event.action) {
                                    MotionEvent.ACTION_DOWN -> {
                                        startY = event.y
                                        startX = event.x
                                    }
                                    MotionEvent.ACTION_MOVE -> {
                                        val distY = Math.abs(event.y - startY)
                                        val distX = Math.abs(event.x - startX)
                                        if (distY > 15 || distX > 15) {
                                            hideKeyboard(activity)
                                        }
                                    }
                                }
                            }
                            return false
                        }
                    })
                }
            }
            is ScrollView -> {
                if (view.tag != "keyboard_dismiss_scroll_setup") {
                    view.tag = "keyboard_dismiss_scroll_setup"
                    view.setOnTouchListener(object : View.OnTouchListener {
                        private var startY = 0f
                        private var startX = 0f
                        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                            if (event != null) {
                                when (event.action) {
                                    MotionEvent.ACTION_DOWN -> {
                                        startY = event.y
                                        startX = event.x
                                    }
                                    MotionEvent.ACTION_MOVE -> {
                                        val distY = Math.abs(event.y - startY)
                                        val distX = Math.abs(event.x - startX)
                                        if (distY > 15 || distX > 15) {
                                            hideKeyboard(activity)
                                        }
                                    }
                                }
                            }
                            return false
                        }
                    })
                }
            }
            is RecyclerView -> {
                if (view.tag != "keyboard_dismiss_scroll_setup") {
                    view.tag = "keyboard_dismiss_scroll_setup"
                    view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                            super.onScrollStateChanged(recyclerView, newState)
                            if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                                hideKeyboard(activity)
                            }
                        }
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            super.onScrolled(recyclerView, dx, dy)
                            if (Math.abs(dy) > 5 || Math.abs(dx) > 5) {
                                hideKeyboard(activity)
                            }
                        }
                    })
                }
            }
            is AbsListView -> {
                if (view.tag != "keyboard_dismiss_scroll_setup") {
                    view.tag = "keyboard_dismiss_scroll_setup"
                    view.setOnScrollListener(object : AbsListView.OnScrollListener {
                        override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {
                            if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                                hideKeyboard(activity)
                            }
                        }
                        override fun onScroll(view: AbsListView?, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {}
                    })
                }
            }
        }
        
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setupDismissKeyboardOnScroll(view.getChildAt(i), activity)
            }
        }
    }

    /**
     * Remove acentos e caracteres especiais (diacríticos) de uma string para permitir
     * pesquisas insensíveis a acentos.
     */
    fun removeAccents(str: String): String {
        val temp = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(temp, "")
    }
}
