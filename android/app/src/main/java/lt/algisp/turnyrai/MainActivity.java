package lt.algisp.turnyrai;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Locale;

/**
 * Turnyrai – programėlė, kuri pagal grupės kodą atidaro tos grupės
 * Google Apps Script puslapį (taurių eilę).
 */
public class MainActivity extends Activity {

    /** Viešas grupių sąrašas. Pridėjus grupę čia, programėlės iš naujo diegti nereikia. */
    static final String REGISTRY_URL =
            "https://raw.githubusercontent.com/algisp911-debug/turnyrai-app/main/grupes.json";

    static final String SETUP_PAGE = "file:///android_asset/setup.html";
    static final String OFFLINE_PAGE = "file:///android_asset/offline.html";

    private WebView web;
    private SharedPreferences prefs;
    private volatile boolean onLocalPage = true;
    /** Po prisijungimo išvalyti istoriją, kad „Atgal“ negrąžintų į kodo įvedimą. */
    private boolean clearOnFinish = false;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("turnyrai", MODE_PRIVATE);

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(web, true);

        web.addJavascriptInterface(new Bridge(), "Turnyrai");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new Client());

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            openStart();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    private void openStart() {
        String url = prefs.getString("url", null);
        if (url != null) { clearOnFinish = true; web.loadUrl(url); }
        else web.loadUrl(SETUP_PAGE);
    }

    private void changeGroup() {
        prefs.edit().remove("url").remove("name").commit();
        web.clearHistory();
        web.loadUrl(SETUP_PAGE);
    }

    @Override
    public void onBackPressed() {
        String cur = web.getUrl();
        if (cur != null && cur.startsWith(SETUP_PAGE)) {
            if (prefs.getString("url", null) != null) openStart();
            else super.onBackPressed();
            return;
        }
        if (web.canGoBack() && cur != null && !cur.startsWith(OFFLINE_PAGE)) {
            web.goBack();
            return;
        }
        String name = prefs.getString("name", "");
        new AlertDialog.Builder(this)
                .setTitle(name.isEmpty() ? "Turnyrai" : name)
                .setItems(new CharSequence[]{"🔄  Atnaujinti", "👥  Keisti grupę", "🚪  Išeiti"}, (d, which) -> {
                    if (which == 0) openStart();
                    else if (which == 1) changeGroup();
                    else finish();
                })
                .setNegativeButton("Atšaukti", null)
                .show();
    }

    // ------------------------------------------------------------------

    private class Client extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            onLocalPage = url != null && url.startsWith("file:///android_asset/");
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (clearOnFinish && url != null && !url.startsWith("file:///android_asset/")) {
                clearOnFinish = false;
                view.clearHistory();
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
            Uri u = req.getUrl();
            String scheme = u.getScheme() == null ? "" : u.getScheme();
            String host = u.getHost() == null ? "" : u.getHost();
            if (scheme.equals("file")) return false;
            if (host.endsWith("google.com") || host.endsWith("googleusercontent.com")) return false;
            // kitos nuorodos – į naršyklę
            try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) { }
            return true;
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
            if (req.isForMainFrame()) view.loadUrl(OFFLINE_PAGE);
        }
    }

    // ------------------------------------------------------------------

    /** Metodai, kuriuos kviečia vietiniai puslapiai (setup.html, offline.html). */
    private class Bridge {

        /** Grąžina JSON: {"ok":true,"name":"…","url":"…"} arba {"ok":false,"err":"…"} */
        @JavascriptInterface
        public String lookup(String input) {
            if (!onLocalPage) return "{}";
            try {
                String v = input == null ? "" : input.trim();
                if (v.isEmpty()) return err("Įveskite grupės kodą");
                if (v.startsWith("https://script.google.com/macros/s/") && v.contains("/exec")) {
                    String url = v.substring(0, v.indexOf("/exec") + 5);
                    return ok("Mano grupė", url);
                }
                JSONObject reg = registry();
                if (reg == null) return err("Nepavyko gauti grupių sąrašo. Patikrinkite internetą.");
                JSONObject g = reg.optJSONObject("grupes");
                if (g != null) {
                    String want = v.toUpperCase(Locale.ROOT).replace(" ", "");
                    Iterator<String> it = g.keys();
                    while (it.hasNext()) {
                        String k = it.next();
                        if (k.toUpperCase(Locale.ROOT).replace(" ", "").equals(want)) {
                            JSONObject o = g.getJSONObject(k);
                            return ok(o.optString("pavadinimas", k), o.getString("url"));
                        }
                    }
                }
                return err("Tokio grupės kodo nėra. Pasitikslinkite pas trenerį.");
            } catch (Exception e) {
                return err("Klaida: " + e.getMessage());
            }
        }

        @JavascriptInterface
        public void connect(String name, String url) {
            if (!onLocalPage) return;
            if (url == null || !url.startsWith("https://script.google.com/")) return;
            prefs.edit().putString("url", url).putString("name", name == null ? "" : name).commit();
            runOnUiThread(() -> {
                clearOnFinish = true;
                web.loadUrl(url);
            });
        }

        @JavascriptInterface
        public void retry() {
            if (!onLocalPage) return;
            runOnUiThread(MainActivity.this::openStart);
        }

        @JavascriptInterface
        public void change() {
            if (!onLocalPage) return;
            runOnUiThread(MainActivity.this::changeGroup);
        }

        @JavascriptInterface
        public boolean hasGroup() {
            return prefs.getString("url", null) != null;
        }
    }

    private static String ok(String name, String url) throws Exception {
        return new JSONObject().put("ok", true).put("name", name).put("url", url).toString();
    }

    private static String err(String msg) {
        try { return new JSONObject().put("ok", false).put("err", msg).toString(); }
        catch (Exception e) { return "{\"ok\":false}"; }
    }

    /** Pirmiausia bando internetinį sąrašą, nepavykus – programėlėje įdėtą. */
    private JSONObject registry() {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(REGISTRY_URL + "?t=" + System.currentTimeMillis()).openConnection();
            c.setConnectTimeout(6000);
            c.setReadTimeout(6000);
            c.setUseCaches(false);
            if (c.getResponseCode() == 200) {
                JSONObject j = new JSONObject(read(c.getInputStream()));
                prefs.edit().putString("registry", j.toString()).apply();
                return j;
            }
        } catch (Exception ignored) { }
        try {
            String cached = prefs.getString("registry", null);
            if (cached != null) return new JSONObject(cached);
        } catch (Exception ignored) { }
        try {
            return new JSONObject(read(getAssets().open("grupes.json")));
        } catch (Exception e) {
            return null;
        }
    }

    private static String read(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toString("UTF-8");
    }
}
