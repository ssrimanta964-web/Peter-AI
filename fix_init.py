with open('app/src/main/java/com/example/ui/PeterViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace('        initWakeWordDetector(application)\n', '')

with open('app/src/main/java/com/example/ui/PeterViewModel.kt', 'w') as f:
    f.write(content)
