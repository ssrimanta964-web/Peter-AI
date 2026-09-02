with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

old_main = """                if (stripped.isBlank() || stripped.length <= 2) {
                    viewModel.startListening()
                } else {"""

new_main = """                if (stripped.isBlank() || stripped.length <= 2) {
                    viewModel.startListening("Yes boss?")
                } else {"""

content = content.replace(old_main, new_main)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
