# Swiss-AS Dev-Tools
An IntelliJ Plugin used for developers at Swiss-AS.

Please note that this plugin will NOT work outside of the company as it uses integration tools and specific internal URLS.

**The Plugin adds following features to IntelliJ :**

1. a Traffic Light will be shown in the bottom of IntelliJ to indicate the status of the build server with different color code.
Bulbs can be on/off/blinking and multiple bulbs can be turned on on the same time.

2. Some pre-commit check are added to avoid committing without description and to warn developers if they try to commit when the build server indicates an issue (ex compile error or failing tests)

3. It will add a Warning Zone where all warnings detected by the Server are displayed .

4. It will add some automatic code correction to comply with the company standards (ex : "this" before a local variable), based on the source code of intellij-plugin-save-actions

Even if this plugin is no use for people outside of Swiss-AS,  this project is public as it may help other doing the same kind of plugin

![ScreenShot](/doc/screenshot.png)
