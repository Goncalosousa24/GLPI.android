import os
import re

LAYOUT_DIR = "app/src/main/res/layout"

files_to_fix = [
    "dialog_filter_assigned.xml",
    "dialog_filter_status.xml",
    "dialog_filter_tickets_criados.xml",
    "dialog_filter_profile.xml",
    "dialog_inventory_filter.xml"
]

for filename in files_to_fix:
    filepath = os.path.join(LAYOUT_DIR, filename)
    if not os.path.exists(filepath):
        continue
    
    with open(filepath, 'r') as f:
        content = f.read()
    
    # Replace radio button selector with standard buttonTint
    content = content.replace('android:button="@drawable/selector_radio_custom"', 'android:buttonTint="@color/azul_glpi"')
    
    # Try to find all TextViews inside LinearLayers that are right next to RadioButtons
    # But essentially, all option TextViews seem to have:
    # android:layout_width="0dp" / wrap_content
    # android:layout_weight="1"
    # android:textSize="16sp"
    
    # Let's replace the properties of those TextViews dynamically:
    # We want to replace textSize="16sp" with the premium setup for these option textviews
    # We'll use lookarounds or just a simple regex to add the attributes
    
    def replacer(match):
        return match.group(1) + 'android:textAllCaps="true"\n                android:fontFamily="@font/amiko_bold"\n                android:textStyle="bold"\n                android:textSize="14sp"' + match.group(2)
        
    content = re.sub(r'(<TextView[^>]*?)android:textSize="16sp"([^>]*?>)', replacer, content, flags=re.DOTALL)
    
    with open(filepath, 'w') as f:
        f.write(content)
        
print("Done")
