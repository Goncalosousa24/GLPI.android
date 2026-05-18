---
description: How to create a new list-based page following the GLPI Mobile Design System
---

When creating a new Activity or Fragment that displays a list of items (`RecyclerView`), you MUST follow these standards to maintain UI/UX consistency.

### 1. RecyclerView Animation
Every `RecyclerView` must use the "slide up" animation for item entry.

**XML Layout:**
Add `android:layoutAnimation="@anim/layout_animation_slide_up"` to the `RecyclerView` definition.
```xml
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/rv_items"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layoutAnimation="@anim/layout_animation_slide_up" />
```

**Kotlin Code:**
You MUST programmatically trigger the animation after updating the adapter's data (e.g., in `applyPagination` or data load callbacks).
```kotlin
rvItems.scheduleLayoutAnimation()
```

### 2. Pagination Standard
To ensure high performance and premium feel, all lists are limited to **10 items per page**.

- Use `subList(start, end)` on your main data list.
- If the current page changes, always reset the scroll to the top:
```kotlin
nestedScrollView.smoothScrollTo(0, 0)
```

### 3. Navigation Controls
Use Lottie-based arrows for "Anterior" and "Próxima" buttons.
- Animation: `@raw/seta_direita`
- Color: Apply `azul_glpi` using `PorterDuff.Mode.SRC_ATOP`.

### 4. Filter UI
If the page includes filters (e.g., status, priority), indicate the **active state** clearly.
- Active Background: `R.drawable.bg_filtro_ativo` (which includes a 2dp `azul_glpi` border).
- Default Background: `R.drawable.bg_cartao_brilhante`.

### 5. Lottie Branding
- Header/Back Arrow: Should be White if on a colored header background.
- Other Lotties (Search, Filters, Pagination): Use `azul_glpi` unless specified otherwise.
