import re
with open("app/src/main/java/com/example/app/GlobalLessonData.kt", "r", encoding="utf-8") as f:
    content = f.read()
# Replace LessonItem containing TYPE_PART or TYPE_BACK_PART. 
# Matches "LessonItem(" followed by anything up to TYPE_PART/TYPE_BACK_PART, followed by anything up to "),"
pattern = re.compile(r"^[ \t]*LessonItem\([^()]*?TYPE_(BACK_)?PART.*?^\s*\),\s*$", re.MULTILINE | re.DOTALL)
new_content = pattern.sub("", content)
with open("app/src/main/java/com/example/app/GlobalLessonData.kt", "w", encoding="utf-8") as f:
    f.write(new_content)
print("Done")
