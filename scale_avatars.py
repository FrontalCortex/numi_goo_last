import os
import re

directory = r"C:\Users\ASUS\AndroidStudioProjects\numi_goo_last\app\src\main\res\drawable"

for i in range(7, 13):
    filename = f"avatar_ic{i}.xml"
    filepath = os.path.join(directory, filename)
    
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
        
    # Replace width and height
    content = re.sub(r'android:width="213\.33dp"', 'android:width="200dp"', content)
    content = re.sub(r'android:height="213\.33dp"', 'android:height="200dp"', content)
    
    # Replace viewportWidth and viewportHeight
    content = re.sub(r'android:viewportWidth="213\.33"', 'android:viewportWidth="200"', content)
    content = re.sub(r'android:viewportHeight="213\.33"', 'android:viewportHeight="200"', content)
    
    # Add scaleX and scaleY to the first group
    # We find the first <group and insert scale properties
    content = re.sub(
        r'<group\s+android:translateX="([^"]+)"\s+android:translateY="([^"]+)">',
        r'<group\n        android:scaleX="0.9375"\n        android:scaleY="0.9375"\n        android:translateX="\1"\n        android:translateY="\2">',
        content,
        count=1
    )
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
        
    print(f"Updated {filename}")
