import re

filepath = "C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/java/com/example/app/NewChestFragment.kt"
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace the hallucinated spawnStar block
content = re.sub(r'override fun onDestroyView\(\) \{\s*starHandler\.removeCallbacks\(starSpawner\)\s*super\.onDestroyView\(\)\s*\}\s*private fun spawnStar\(\) \{.*?\}\s*private fun onScreenTapped', 'private fun onScreenTapped', content, flags=re.DOTALL)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
