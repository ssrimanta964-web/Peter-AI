with open('app/src/main/java/com/example/ui/PeterViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace('kotlinx.coroutines.delay(800)', 'kotlinx.coroutines.delay(1200)')

with open('app/src/main/java/com/example/ui/PeterViewModel.kt', 'w') as f:
    f.write(content)
