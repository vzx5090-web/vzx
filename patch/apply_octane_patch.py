#!/usr/bin/env python3
from pathlib import Path

UPSTREAM = Path("upstream")

def replace_once(relative, old, new):
    path = UPSTREAM / relative
    source = path.read_text(encoding="utf-8")
    count = source.count(old)
    if count != 1:
        raise SystemExit("Patch anchor mismatch in %s: expected 1, found %d" % (relative, count))
    path.write_text(source.replace(old, new, 1), encoding="utf-8")

replace_once("app/build.gradle", "applicationId 'com.winlator'",
             "applicationId 'com.octane.studio.runtime'")
replace_once("app/build.gradle", 'versionName "11.2"',
             'versionName "1.0.0-octane-runtime"')

replace_once("app/src/main/AndroidManifest.xml",
             '            </intent-filter>\n        </activity>',
             '''            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="octane-studio" />
            </intent-filter>
        </activity>''')
replace_once("app/src/main/AndroidManifest.xml",
             'android:authorities="com.winlator.FileProvider"',
             'android:authorities="' + "$" + '{applicationId}.FileProvider"')

replace_once("app/src/main/java/com/winlator/core/FileUtils.java",
             'FileProvider.getUriForFile(activity, "com.winlator.FileProvider", file)',
             'FileProvider.getUriForFile(activity, activity.getPackageName()+".FileProvider", file)')

replace_once("app/src/main/res/menu/main_menu.xml",
             '        <item android:icon="@drawable/icon_container" android:id="@+id/menu_item_containers" android:title="@string/containers" />',
             '''        <item android:icon="@drawable/icon_container" android:id="@+id/menu_item_containers" android:title="@string/containers" />
        <item android:icon="@drawable/icon_container" android:id="@+id/menu_item_octane_studio" android:title="@string/octane_studio" />''')

replace_once("app/src/main/res/values/strings.xml",
             '<string name="app_name">Winlator</string>',
             '<string name="app_name">Octane Studio Android</string>\n    <string name="octane_studio">Octane Studio</string>')

replace_once("app/src/main/java/com/winlator/MainActivity.java",
             'int menuItemId = selectedMenuItemId > 0 ? selectedMenuItemId : (showShortcutsFirst ? R.id.menu_item_shortcuts : R.id.menu_item_containers);',
             '''boolean openOctaneStudio = intent.getData() != null && "octane-studio".equalsIgnoreCase(intent.getData().getScheme());
            int menuItemId = selectedMenuItemId > 0 ? selectedMenuItemId : (openOctaneStudio ? R.id.menu_item_octane_studio : (showShortcutsFirst ? R.id.menu_item_shortcuts : R.id.menu_item_containers));''')

replace_once("app/src/main/java/com/winlator/MainActivity.java",
             '''            case R.id.menu_item_input_controls:
                showFragment(new InputControlsFragment(selectedProfileId));''',
             '''            case R.id.menu_item_octane_studio:
                showFragment(new OctaneStudioFragment());
                break;
            case R.id.menu_item_input_controls:
                showFragment(new InputControlsFragment(selectedProfileId));''')

target = UPSTREAM / "app/src/main/java/com/winlator/OctaneStudioFragment.java"
target.parent.mkdir(parents=True, exist_ok=True)
target.write_bytes(Path("overlay/patch/OctaneStudioFragment.java").read_bytes())

checks = {
    "app/build.gradle": ["com.octane.studio.runtime", "1.0.0-octane-runtime"],
    "app/src/main/AndroidManifest.xml": ["octane-studio", "$" + "{applicationId}.FileProvider"],
    "app/src/main/res/menu/main_menu.xml": ["menu_item_octane_studio"],
    "app/src/main/res/values/strings.xml": ["Octane Studio Android", "octane_studio"],
    "app/src/main/java/com/winlator/MainActivity.java": ["new OctaneStudioFragment()"],
    "app/src/main/java/com/winlator/OctaneStudioFragment.java": [
        "https://octane.wtf/setup/studio/2021/version.txt",
        "RobloxStudioBeta.exe",
        "XServerDisplayActivity"
    ]
}
for relative, needles in checks.items():
    data = (UPSTREAM / relative).read_text(encoding="utf-8")
    for needle in needles:
        if needle not in data:
            raise SystemExit("Post-patch check failed: %s does not contain %s" % (relative, needle))

print("Octane Studio integration patch applied; all structural checks passed.")
