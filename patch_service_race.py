with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

old_resume = """    override fun onResume() {
        super.onResume()"""

new_resume = """    companion object {
        var isForeground = false
    }
    
    override fun onResume() {
        super.onResume()
        isForeground = true"""

old_pause = """    override fun onPause() {
        super.onPause()"""

new_pause = """    override fun onPause() {
        super.onPause()
        isForeground = false"""

content = content.replace(old_resume, new_resume).replace(old_pause, new_pause)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
