import re

with open('app/src/main/java/com/example/ui/components/LockdownSecurityScreen.kt', 'r') as f:
    content = f.read()

# Add showDeviceAdminControls: Boolean = true
content = content.replace('onHardwareLock: () -> Boolean = { false },', 'onHardwareLock: () -> Boolean = { false },\n    showDeviceAdminControls: Boolean = true,')

# Wrap the System Device Admin Status / Action section
old_admin_block = """            // System Device Admin Status / Action
            if (isDeviceAdminActive) {
                Button(
                    onClick = { onHardwareLock() },"""

new_admin_block = """            // System Device Admin Status / Action
            if (showDeviceAdminControls) {
                if (isDeviceAdminActive) {
                    Button(
                        onClick = { onHardwareLock() },"""

content = content.replace(old_admin_block, new_admin_block)

old_admin_end = """                }
            }
        }

        // BOTTOM SECURITY STATUS BAR"""

new_admin_end = """                }
                }
            }
        }

        // BOTTOM SECURITY STATUS BAR"""

# We have to be careful with the end replacement, let's just do a string replacement for the exact end part.
# There is a better way: Just find the block and replace it.
