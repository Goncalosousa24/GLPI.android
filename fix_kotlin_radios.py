import os
import re

BASE_DIR = "app/src/main/java/com/example/glpimobile"

files_to_fix = [
    "OpenTicketsActivity.kt",
    "InventoryActivity.kt",
    "ReservationsActivity.kt",
    "MyTicketsHistoryActivity.kt",
    "ProgressTicketsActivity.kt",
    "PriorityTicketsActivity.kt",
    "LogsActivity.kt",
    "MyCreatedTicketsActivity.kt",
    "ResolvedTicketsActivity.kt",
    "EditTicketsActivity.kt",
    "ProfileActivity.kt",
]

for filename in files_to_fix:
    filepath = os.path.join(BASE_DIR, filename)
    if not os.path.exists(filepath):
        print(f"SKIP: {filename} not found")
        continue
    
    with open(filepath, 'r') as f:
        content = f.read()
    
    original = content
    
    # 1. Replace setButtonDrawable with buttonTintList
    content = content.replace(
        'setButtonDrawable(R.drawable.selector_radio_custom)',
        'buttonTintList = android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.azul_glpi))'
    )
    
    # 2. Replace textSize = 16f with 14f + bold + uppercase + font
    # Pattern: textSize = 16f followed by gravity = ...
    content = content.replace(
        'textSize = 16f',
        'textSize = 14f\n                isAllCaps = true\n                typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.amiko_bold)\n                setTypeface(typeface, android.graphics.Typeface.BOLD)'
    )
    
    if content != original:
        with open(filepath, 'w') as f:
            f.write(content)
        print(f"FIXED: {filename}")
    else:
        print(f"NO CHANGE: {filename}")
        
print("Done!")
