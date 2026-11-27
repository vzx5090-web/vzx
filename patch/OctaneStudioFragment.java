package com.winlator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.core.FileUtils;
import com.winlator.core.StringUtils;
import com.winlator.xenvironment.RootFS;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Installs and launches the actual Octane Windows Studio build through Wine/Box64. */
public class OctaneStudioFragment extends Fragment {
    private static final String VERSION_URL = "https://octane.wtf/setup/studio/2021/version.txt";
    private static final String PACKAGE_URL = "https://octane.wtf/setup/studio/2021/%s-studio.zip";
    private static final String INSTALL_DIR = "OctaneStudio";
    private static final long MAX_UNPACKED = 8L * 1024L * 1024L * 1024L;

    private final ArrayList<Container> containers = new ArrayList<>();
    private ContainerManager manager;
    private Spinner spinner;
    private Button createButton, installButton, launchButton;
    private ProgressBar progress;
    private TextView status;
    private boolean busy;

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                             @Nullable ViewGroup parent, @Nullable Bundle state) {
        Context c = requireContext();
        manager = new ContainerManager(c);
        ScrollView scroll = new ScrollView(c);
        scroll.setFillViewport(true);
        LinearLayout page = new LinearLayout(c);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(24), dp(20), dp(30));
        page.setBackgroundColor(Color.rgb(18, 18, 24));
        scroll.addView(page);

        addText(page, "Octane Studio", 26, true, Color.WHITE, 0, 0, 0, 8);
        addText(page, "Install the official Octane Studio Windows package and run its RobloxStudioBeta.exe on Android using Wine + Box64.", 14, false, 0xffc4c4d0, 0, 0, 0, 20);
        addText(page, "Wine container", 15, true, Color.WHITE, 0, 0, 0, 4);

        spinner = new Spinner(c);
        page.addView(spinner, match());
        createButton = makeButton(c, "Create Octane container");
        page.addView(createButton, buttonParams());
        createButton.setOnClickListener(v -> createContainer());

        installButton = makeButton(c, "Install / update official Octane Studio");
        page.addView(installButton, buttonParams());
        installButton.setOnClickListener(v -> installStudio());

        launchButton = makeButton(c, "Launch Octane Studio");
        page.addView(launchButton, buttonParams());
        launchButton.setOnClickListener(v -> launchStudio());

        progress = new ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(1000);
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams pp = match();
        pp.topMargin = dp(14);
        page.addView(progress, pp);

        status = addText(page, "Ready. Select or create a Wine container.", 13, false, 0xffddddE6, 0, 14, 0, 4);
        addText(page, "This is the real Windows Studio package, not a native Android editor. Compatibility and rendering depend on Wine/Box64 and the phone GPU; successful APK installation does not guarantee Studio will run on every device.", 12, false, 0xffaaaaB8, 0, 18, 0, 0);
        refreshContainers(0);
        return scroll;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (manager != null && !busy) refreshContainers(selectedId());
    }

    private void refreshContainers(int preferredId) {
        if (spinner == null || manager == null) return;
        containers.clear();
        containers.addAll(manager.getContainers());
        ArrayList<String> names = new ArrayList<>();
        int selected = 0;
        for (int i = 0; i < containers.size(); i++) {
            names.add(containers.get(i).getName());
            if (containers.get(i).id == preferredId) selected = i;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setVisibility(containers.isEmpty() ? View.GONE : View.VISIBLE);
        if (!containers.isEmpty()) spinner.setSelection(selected);
        createButton.setVisibility(containers.isEmpty() ? View.VISIBLE : View.GONE);
        installButton.setEnabled(!containers.isEmpty() && !busy);
        launchButton.setEnabled(!containers.isEmpty() && !busy);
    }

    private int selectedId() {
        int p = spinner == null ? -1 : spinner.getSelectedItemPosition();
        return p >= 0 && p < containers.size() ? containers.get(p).id : 0;
    }

    private Container selectedContainer() {
        int p = spinner == null ? -1 : spinner.getSelectedItemPosition();
        return p >= 0 && p < containers.size() ? containers.get(p) : null;
    }

    private void createContainer() {
        if (!RootFS.find(requireContext()).isValid()) {
            setStatus("Complete Winlator's first-time system setup from Containers, then return here.");
            return;
        }
        setBusy(true);
        setStatus("Creating a dedicated Octane Wine container...");
        manager.createContainerAsync(new JSONObject(), c -> {
            if (!isAdded()) return;
            if (c == null) {
                setBusy(false);
                setStatus("Container creation failed. Check free storage.");
                return;
            }
            c.setName("Octane Studio");
            c.saveData();
            setBusy(false);
            refreshContainers(c.id);
            setStatus("Container ready. Install the official Studio package.");
        });
    }

    private void installStudio() {
        final Container container = selectedContainer();
        if (container == null) {
            setStatus("Create or select a Wine container first.");
            return;
        }
        if (!RootFS.find(requireContext()).isValid()) {
            setStatus("Winlator system files are not ready. Finish first-time setup in Containers.");
            return;
        }
        final Context app = requireContext().getApplicationContext();
        setBusy(true);
        setProgress(0, 0);
        setStatus("Checking Octane's current Studio package version...");
        new Thread(() -> {
            File zip = new File(app.getCacheDir(), "octane-studio.zip");
            File stage = null;
            try {
                String version = getVersion();
                if (!version.matches("[A-Za-z0-9._-]{1,80}")) throw new IOException("Invalid version returned by Octane.");
                setStatus("Downloading official Octane Studio " + version + "...");
                download(new URL(String.format(Locale.US, PACKAGE_URL, version)), zip);

                File driveC = new File(container.getRootDir(), ".wine/drive_c");
                if (!driveC.isDirectory() && !driveC.mkdirs()) throw new IOException("Cannot access Wine C: drive.");
                stage = new File(driveC, ".OctaneStudio-new");
                File target = new File(driveC, INSTALL_DIR);
                File backup = new File(driveC, ".OctaneStudio-old");
                deleteTree(stage);
                if (!stage.mkdirs()) throw new IOException("Cannot create temporary install folder.");

                setStatus("Extracting and checking RobloxStudioBeta.exe...");
                unzip(zip, stage);
                File exe = findExe(stage);
                if (exe == null || !isPE(exe)) throw new IOException("The official ZIP did not contain a valid RobloxStudioBeta.exe.");
                String relative = stage.getCanonicalFile().toPath().relativize(exe.getCanonicalFile().toPath()).toString();
                FileUtils.writeString(new File(stage, ".octane-version"), version);

                deleteTree(backup);
                if (target.exists() && !target.renameTo(backup)) throw new IOException("Could not safely replace the old install.");
                boolean moved = stage.renameTo(target);
                if (!moved) {
                    moved = copyTree(stage, target);
                    if (moved) deleteTree(stage);
                }
                if (!moved) {
                    if (backup.exists()) backup.renameTo(target);
                    throw new IOException("Could not move Studio into the Wine C: drive.");
                }
                deleteTree(backup);

                File installedExe = new File(target, relative);
                if (!installedExe.isFile()) throw new IOException("Studio executable missing after extraction.");
                writeShortcut(container, driveC, installedExe);
                zip.delete();
                setBusy(false);
                ui(() -> refreshContainers(container.id));
                setStatus("Octane Studio " + version + " installed. Tap Launch Octane Studio.");
            } catch (Exception e) {
                if (stage != null) deleteTree(stage);
                zip.delete();
                setBusy(false);
                setStatus("Install failed: " + message(e));
            }
        }, "Octane-Studio-Installer").start();
    }

    private String getVersion() throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(VERSION_URL).openConnection();
        c.setConnectTimeout(20000);
        c.setReadTimeout(20000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "OctaneStudioAndroid/1.0");
        try {
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) throw new IOException("Octane version server HTTP " + code);
            StringBuilder value = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) value.append(line);
            }
            return value.toString().trim();
        } finally { c.disconnect(); }
    }

    private void download(URL url, File target) throws Exception {
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setConnectTimeout(25000);
        c.setReadTimeout(60000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "OctaneStudioAndroid/1.0");
        try {
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) throw new IOException("Octane package server HTTP " + code);
            long total = c.getContentLengthLong();
            try (InputStream in = new BufferedInputStream(c.getInputStream());
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(target))) {
                byte[] buffer = new byte[65536];
                long done = 0;
                int n;
                while ((n = in.read(buffer)) != -1) {
                    out.write(buffer, 0, n);
                    done += n;
                    setProgress(done, total);
                }
            }
        } finally { c.disconnect(); }
    }

    private void unzip(File zip, File folder) throws Exception {
        String root = folder.getCanonicalPath();
        String prefix = root + File.separator;
        long total = 0;
        try (ZipInputStream in = new ZipInputStream(new BufferedInputStream(new FileInputStream(zip)))) {
            ZipEntry entry;
            byte[] buffer = new byte[65536];
            while ((entry = in.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (name.isEmpty() || name.startsWith("/") || name.matches("^[A-Za-z]:.*")) throw new IOException("Unsafe ZIP entry.");
                File outFile = new File(folder, name);
                String canonical = outFile.getCanonicalPath();
                if (!canonical.equals(root) && !canonical.startsWith(prefix)) throw new IOException("ZIP path escapes install folder.");
                if (entry.isDirectory()) {
                    if (!outFile.isDirectory() && !outFile.mkdirs()) throw new IOException("Cannot create ZIP directory.");
                } else {
                    File parent = outFile.getParentFile();
                    if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new IOException("Cannot create output directory.");
                    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile))) {
                        int n;
                        while ((n = in.read(buffer)) != -1) {
                            total += n;
                            if (total > MAX_UNPACKED) throw new IOException("Package exceeds safe extraction size.");
                            out.write(buffer, 0, n);
                        }
                    }
                }
                in.closeEntry();
            }
        }
    }

    private static File findExe(File dir) {
        ArrayDeque<File> todo = new ArrayDeque<>();
        todo.add(dir);
        while (!todo.isEmpty()) {
            File f = todo.removeFirst();
            File[] children = f.listFiles();
            if (children == null) continue;
            for (File child : children) {
                if (child.isDirectory()) todo.add(child);
                else if (child.isFile() && child.getName().equalsIgnoreCase("RobloxStudioBeta.exe")) return child;
            }
        }
        return null;
    }

    private static boolean isPE(File file) {
        if (!file.isFile() || file.length() < 2) return false;
        try (InputStream in = new FileInputStream(file)) { return in.read() == 'M' && in.read() == 'Z'; }
        catch (Exception e) { return false; }
    }

    private void writeShortcut(Container c, File driveC, File exe) throws Exception {
        File desktop = new File(c.getUserDir(), "Desktop");
        if (!desktop.isDirectory() && !desktop.mkdirs()) throw new IOException("Cannot create Wine Desktop.");
        String relative = driveC.getCanonicalFile().toPath().relativize(exe.getCanonicalFile().toPath()).toString();
        String dos = "C:\\" + relative.replace(File.separatorChar, '\\');
        StringBuilder body = new StringBuilder();
        body.append("[Desktop Entry]\nName=Octane Studio\n");
        body.append("Exec=wine ").append(StringUtils.escapeDOSPath(dos)).append("\n");
        body.append("Type=Application\nStartupWMClass=RobloxStudioBeta\n");
        String args = octaneLinkArgs();
        if (!args.isEmpty()) body.append("\n[Extra Data]\nexecArgs=").append(args).append("\n");
        if (!FileUtils.writeString(new File(desktop, "Octane Studio.desktop"), body.toString())) {
            throw new IOException("Could not create Studio shortcut.");
        }
    }

    private String octaneLinkArgs() {
        Activity a = getActivity();
        if (a == null) return "";
        Uri uri = a.getIntent().getData();
        if (uri == null || !"octane-studio".equalsIgnoreCase(uri.getScheme())) return "";
        Map<String, String> fields = new HashMap<>();
        String raw = uri.getEncodedSchemeSpecificPart();
        if (raw == null) return "";
        for (String pair : raw.split("\\+")) {
            int i = pair.indexOf(':');
            if (i > 0) fields.put(pair.substring(0, i).toLowerCase(Locale.ROOT), Uri.decode(pair.substring(i + 1)));
        }
        String ticket = fields.get("gameinfo");
        if (ticket == null || !ticket.matches("[A-Za-z0-9._~+/=-]{1,4096}")) return "";
        StringBuilder args = new StringBuilder("-url \"https://octane.wtf/Login/Negotiate.ashx\" -ticket \"").append(ticket).append("\"");
        String task = fields.get("task");
        if (task == null || task.isEmpty()) return args.toString();
        if (!task.matches("[A-Za-z0-9_-]{1,40}")) return "";
        args.append(" -task ").append(task);
        if ("TryAsset".equals(task)) {
            String asset = fields.get("assetid");
            if (asset == null || !asset.matches("[0-9]{1,32}")) return "";
            args.append(" -assetId ").append(asset);
        } else {
            String place = fields.get("placeid");
            String universe = fields.get("universeid");
            if (place == null || universe == null || !place.matches("[0-9]{1,32}") || !universe.matches("[0-9]{1,32}")) return "";
            args.append(" -placeId ").append(place).append(" -universeId ").append(universe);
        }
        return args.toString();
    }

    private void launchStudio() {
        Container c = selectedContainer();
        if (c == null) { setStatus("Create or select a Wine container first."); return; }
        File driveC = new File(c.getRootDir(), ".wine/drive_c");
        File exe = findExe(new File(driveC, INSTALL_DIR));
        if (exe == null || !isPE(exe)) { setStatus("Studio is not installed in this container yet."); return; }
        try {
            writeShortcut(c, driveC, exe);
            File shortcut = new File(new File(c.getUserDir(), "Desktop"), "Octane Studio.desktop");
            Intent intent = new Intent(requireActivity(), XServerDisplayActivity.class);
            intent.putExtra("container_id", c.id);
            intent.putExtra("shortcut_path", shortcut.getAbsolutePath());
            startActivity(intent);
        } catch (Exception e) { setStatus("Launch failed: " + message(e)); }
    }

    private static boolean copyTree(File src, File dst) {
        if (src.isDirectory()) {
            if (!dst.exists() && !dst.mkdirs()) return false;
            File[] files = src.listFiles();
            if (files == null) return false;
            for (File f : files) if (!copyTree(f, new File(dst, f.getName()))) return false;
            return true;
        }
        File parent = dst.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) return false;
        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
            byte[] b = new byte[65536]; int n;
            while ((n = in.read(b)) != -1) out.write(b, 0, n);
            return true;
        } catch (Exception e) { return false; }
    }

    private static void deleteTree(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteTree(child);
        }
        file.delete();
    }

    private void setBusy(boolean value) {
        ui(() -> {
            busy = value;
            if (installButton != null) installButton.setEnabled(!value && !containers.isEmpty());
            if (launchButton != null) launchButton.setEnabled(!value && !containers.isEmpty());
            if (createButton != null) createButton.setEnabled(!value);
            if (spinner != null) spinner.setEnabled(!value);
            if (progress != null) progress.setVisibility(value ? View.VISIBLE : View.GONE);
        });
    }

    private void setProgress(long done, long total) {
        ui(() -> {
            if (progress == null) return;
            if (total <= 0) progress.setIndeterminate(true);
            else { progress.setIndeterminate(false); progress.setProgress((int)Math.min(1000, done * 1000L / total)); }
        });
    }

    private void setStatus(String value) { ui(() -> { if (status != null) status.setText(value); }); }

    private void ui(Runnable action) {
        Activity a = getActivity();
        if (a != null) a.runOnUiThread(() -> { if (isAdded()) action.run(); });
    }

    private static String message(Exception e) {
        String m = e.getMessage();
        return m == null || m.trim().isEmpty() ? e.getClass().getSimpleName() : m;
    }

    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }

    private static TextView addText(LinearLayout page, String value, int size, boolean bold,
                                    int color, int left, int top, int right, int bottom) {
        TextView v = new TextView(page.getContext());
        v.setText(value); v.setTextSize(size); v.setTextColor(color);
        if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD);
        v.setPadding(left, top, right, bottom);
        page.addView(v, new LinearLayout.LayoutParams(-1, -2));
        return v;
    }

    private static Button makeButton(Context c, String value) {
        Button b = new Button(c); b.setText(value); b.setAllCaps(false); return b;
    }

    private LinearLayout.LayoutParams match() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams p = match(); p.topMargin = dp(10); return p;
    }
}
