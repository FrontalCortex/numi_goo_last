
import re

with open("app/src/main/java/com/example/app/BlindingLessonFragment.kt", "r", encoding="utf-8") as f:
    content = f.read()

# Fix formatting
old_format = """                    is MathOperation -> {
                        currentOperation.firstNumber?.let { number ->
                            numberText.text = number.toString()
                        }
                    }"""
new_format = """                    is MathOperation -> {
                        val n1 = currentOperation.firstNumber?.toString() ?: ""
                        val op = currentOperation.operator ?: "x"
                        val n2 = currentOperation.secondNumber?.toString() ?: ""
                        numberText.text = f"{n1} {op} {n2}".trim()
                    }""".replace("f\"", "\"$") # Python trick

content = content.replace(old_format, new_format)

# Fix logic part 1
old_logic1 = """            is MathOperation -> {
                if (lessonItem.blindingMultiplication == true || lessonItem.isMultiplication == true) {
                    answerNumber = currentOperation.firstNumber?.times(currentOperation.secondNumber!!) ?: 0"""

new_logic1 = """            is MathOperation -> {
                if (lessonItem.blindingMultiplication == true || lessonItem.isMultiplication == true || isDailyQuestionMode) {
                    val first = currentOperation.firstNumber ?: 0
                    val second = currentOperation.secondNumber ?: 0
                    val op = currentOperation.operator ?: "x"
                    if (op == "x" || op == "*") {
                        answerNumber = first * second
                    } else if (op == "+") {
                        answerNumber = first + second
                    } else if (op == "-") {
                        answerNumber = first - second
                    } else {
                        answerNumber = first * second
                    }"""

content = content.replace(old_logic1, new_logic1)

# Fix logic part 2
old_logic2 = """                    if (lessonItem.isMultiplication == true && lessonItem.isBlinding != true) {"""
new_logic2 = """                    if (lessonItem.isMultiplication == true && lessonItem.isBlinding != true && !isDailyQuestionMode) {"""

# Only replace the one right after answerNumber logic
# We can just replace all occurrences of old_logic2, there should be only one or a few, but we only care about this one.
content = content.replace(old_logic2, new_logic2)

with open("app/src/main/java/com/example/app/BlindingLessonFragment.kt", "w", encoding="utf-8") as f:
    f.write(content)
print("Replaced successfully!")

