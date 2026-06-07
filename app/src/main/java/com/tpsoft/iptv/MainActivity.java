package com.tpsoft.iptv;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    
    private WebView webView;
    private FrameLayout fullscreenContainer;
    private ProgressBar progressBar;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private boolean isFullscreen = false;
    private Handler handler = new Handler();
    private Runnable hideSystemUiRunnable;
    
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Force landscape mode for better TV experience
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        
        webView = findViewById(R.id.webView);
        fullscreenContainer = findViewById(R.id.fullscreenContainer);
        progressBar = findViewById(R.id.progressBar);
        
        // Setup WebView settings
        setupWebView();
        
        // Load the IPTV Player HTML
        loadIPTVPlayer();
        
        // Setup auto-hide system UI for immersive mode
        setupImmersiveMode();
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        
        // Essential settings for video playback
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        // Set user agent for better compatibility
        String userAgent = webSettings.getUserAgentString();
        webSettings.setUserAgentString(userAgent + " TPSoftIPTV/1.0");
        
        // WebViewClient to handle page navigation
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                hideSystemUI();
            }
            
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                progressBar.setVisibility(View.GONE);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    if (error.getErrorCode() == ERROR_HOST_LOOKUP || error.getErrorCode() == ERROR_CONNECT) {
                        Toast.makeText(MainActivity.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
                    }
                }
            }
            
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // Allow all URLs to load in WebView
                view.loadUrl(url);
                return true;
            }
        });
        
        // WebChromeClient for video fullscreen support
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                
                customView = view;
                customViewCallback = callback;
                fullscreenContainer.addView(customView);
                fullscreenContainer.setVisibility(View.VISIBLE);
                isFullscreen = true;
                
                // Hide system UI when entering fullscreen
                hideSystemUI();
                
                // Set orientation to landscape for fullscreen video
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }
            
            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                
                fullscreenContainer.removeView(customView);
                customView = null;
                fullscreenContainer.setVisibility(View.GONE);
                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                }
                isFullscreen = false;
                
                // Restore orientation
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                
                // Show controls again
                showSystemUI();
            }
            
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }
    
    private void loadIPTVPlayer() {
        // Full HTML content from the IPTV Player
        String htmlContent = getIPTVHTML();
        webView.loadDataWithBaseURL("https://tpsoft.iptv/", htmlContent, "text/html", "UTF-8", null);
    }
    
    private void setupImmersiveMode() {
        hideSystemUiRunnable = new Runnable() {
            @Override
            public void run() {
                hideSystemUI();
            }
        };
        
        // Set on touch listener to show/hide system UI
        webView.setOnTouchListener((v, event) -> {
            showSystemUI();
            handler.removeCallbacks(hideSystemUiRunnable);
            handler.postDelayed(hideSystemUiRunnable, 3000);
            return false;
        });
    }
    
    private void hideSystemUI() {
        // Immersive mode for fullscreen experience
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }
    
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Adjust layout on orientation change
        handler.postDelayed(() -> webView.loadUrl("javascript:window.dispatchEvent(new Event('resize'));"), 100);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Handle remote control / keyboard navigation
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                webView.loadUrl("javascript:prevChannel();");
                showSystemUI();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                webView.loadUrl("javascript:nextChannel();");
                showSystemUI();
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                webView.loadUrl("javascript:togglePlay();");
                showSystemUI();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                webView.loadUrl("javascript:openMenu();");
                showSystemUI();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                webView.loadUrl("javascript:toggleMenu();");
                showSystemUI();
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (isFullscreen) {
                    webView.loadUrl("javascript:document.exitFullscreen();");
                    return true;
                }
                if (webView.canGoBack()) {
                    webView.goBack();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                webView.loadUrl("javascript:togglePlay();");
                return true;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                webView.loadUrl("javascript:nextChannel();");
                return true;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                webView.loadUrl("javascript:prevChannel();");
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Please check your network.", Toast.LENGTH_LONG).show();
        }
        webView.onResume();
        webView.resumeTimers();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
        webView.pauseTimers();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.destroy();
        }
        handler.removeCallbacks(hideSystemUiRunnable);
    }
    
    private String getIPTVHTML() {
        // ============================================================
        // YOUR COMPLETE IPTV HTML CODE FROM PREVIOUS RESPONSE GOES HERE
        // ============================================================
        // Due to length, paste the entire HTML code you received earlier
        // Make sure to include all the HTML, CSS, and JavaScript
        // ============================================================
        
        return "<!DOCTYPE html>\n" +
               "<html lang=\"en\">\n" +
               "<head>\n" +
               "<meta charset=\"UTF-8\">\n" +
               "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\">\n" +
               "<title>TPSoft IPTV</title>\n" +
               "<script src=\"https://cdn.jsdelivr.net/npm/hls.js@latest\"></script>\n" +
               "<link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css\">\n" +
               "<style>\n" +
               "  * { box-sizing: border-box; user-select: none; margin: 0; padding: 0; }\n" +
               "  body { background: #000; font-family: 'Segoe UI', Roboto, sans-serif; overflow: hidden; color: white; }\n" +
               "  #leftPanel { position: fixed; left: 0; top: 0; width: 280px; height: 100vh; background: rgba(10,10,12,0.95); backdrop-filter: blur(12px); padding: 18px 14px; overflow-y: auto; z-index: 1000; border-right: 1px solid rgba(255,255,255,0.1); transform: translateX(-100%); transition: transform 0.3s ease; }\n" +
               "  #leftPanel.open { transform: translateX(0); }\n" +
               "  #leftPanel h3 { margin: 18px 0 8px 0; font-size: 14px; color: #ffaa66; border-left: 3px solid #ff6600; padding-left: 10px; }\n" +
               "  input, button { width: 100%; padding: 12px; margin: 6px 0; border: none; border-radius: 12px; font-size: 14px; }\n" +
               "  input { background: #1f1f2a; color: white; border: 1px solid #2c2c38; }\n" +
               "  button { background: linear-gradient(95deg, #ff4d00, #ff7a2f); color: white; font-weight: bold; cursor: pointer; }\n" +
               "  .signature { margin-top: 28px; font-size: 12px; text-align: center; background: rgba(0,0,0,0.5); border-radius: 16px; padding: 12px; }\n" +
               "  .signature .dev-name { color: #ff4d00; font-weight: bold; }\n" +
               "  .controls-wrapper { position: fixed; top: 0; left: 0; width: 100%; height: 100%; pointer-events: none; z-index: 100; }\n" +
               "  .top-bar { position: absolute; top: 0; left: 0; right: 0; height: 56px; background: linear-gradient(to bottom, rgba(0,0,0,0.75), transparent); display: flex; align-items: center; justify-content: space-between; padding: 0 16px 0 20px; pointer-events: auto; opacity: 0; transition: opacity 0.3s ease; }\n" +
               "  #menuToggleBtn, #channelBtn { background: rgba(0,0,0,0.6); backdrop-filter: blur(8px); border: none; border-radius: 40px; color: white; cursor: pointer; }\n" +
               "  #menuToggleBtn { width: 40px; height: 40px; font-size: 18px; }\n" +
               "  #channelBtn { padding: 6px 14px; font-size: 13px; border: 1px solid rgba(255,255,255,0.2); }\n" +
               "  #countryTabs { position: absolute; top: 68px; left: 20px; right: 20px; display: flex; gap: 8px; overflow-x: auto; white-space: nowrap; opacity: 0; transition: opacity 0.3s ease; pointer-events: auto; }\n" +
               "  #countryTabs button { background: rgba(0,0,0,0.65); backdrop-filter: blur(8px); padding: 6px 16px; border-radius: 40px; font-size: 12px; color: #eee; border: 1px solid rgba(255,255,255,0.2); flex-shrink: 0; }\n" +
               "  #countryTabs button.active-tab { background: #ff6600; color: black; }\n" +
               "  #bottomControls { position: absolute; bottom: 20px; left: 50%; transform: translateX(-50%); display: flex; gap: 12px; background: rgba(0,0,0,0.65); backdrop-filter: blur(20px); padding: 8px 20px; border-radius: 60px; opacity: 0; transition: opacity 0.3s ease; pointer-events: auto; }\n" +
               "  #bottomControls.visible { opacity: 1; }\n" +
               "  .ctrlBtn { width: 42px; height: 42px; border-radius: 50%; background: rgba(20,20,30,0.9); color: white; font-size: 18px; display: flex; align-items: center; justify-content: center; cursor: pointer; border: none; }\n" +
               "  .vol-slider { display: flex; align-items: center; gap: 6px; background: rgba(0,0,0,0.5); padding: 4px 10px; border-radius: 40px; }\n" +
               "  #volumeRange { width: 70px; height: 3px; background: #ff6600; }\n" +
               "  #overlay { position: fixed; top: 0; right: 0; width: 380px; max-width: 88vw; height: 100vh; background: rgba(12,12,18,0.96); backdrop-filter: blur(16px); display: none; flex-direction: column; z-index: 10000; border-left: 1px solid rgba(255,255,255,0.15); }\n" +
               "  #overlay.show { display: flex; }\n" +
               "  .overlay-header { padding: 20px 16px 8px; display: flex; justify-content: space-between; border-bottom: 1px solid #2c2c38; }\n" +
               "  #closeOverlay { background: none; width: auto; font-size: 24px; }\n" +
               "  #searchBox { margin: 12px 16px; width: calc(100% - 32px); background: #1e1e2a; border-radius: 40px; padding: 12px; }\n" +
               "  #popup { flex: 1; overflow-y: auto; padding: 8px 12px; }\n" +
               "  .channel { display: flex; align-items: center; gap: 14px; padding: 12px 14px; margin-bottom: 8px; border-radius: 16px; background: rgba(255,255,255,0.06); cursor: pointer; }\n" +
               "  .channel.active { background: #ff6600; color: #111; }\n" +
               "  .channel img { width: 45px; height: 45px; border-radius: 10px; object-fit: cover; }\n" +
               "  #playerContainer { position: fixed; left: 0; top: 0; width: 100%; height: 100%; background: #000; z-index: 1; }\n" +
               "  video { width: 100%; height: 100%; object-fit: contain; }\n" +
               "  .toast-msg { position: fixed; bottom: 90px; left: 50%; transform: translateX(-50%); background: #1f1f2fcc; backdrop-filter: blur(16px); padding: 6px 16px; border-radius: 40px; font-size: 12px; z-index: 10000; color: #ffcc99; white-space: nowrap; opacity: 0; transition: 0.2s; }\n" +
               "</style>\n" +
               "</head>\n" +
               "<body>\n" +
               "<div id=\"leftPanel\">\n" +
               "  <div style=\"display: flex; justify-content: space-between;\">\n" +
               "    <h2 style=\"color:#ff6600;\"><i class=\"fas fa-tv\"></i> TPSoft IPTV</h2>\n" +
               "    <button id=\"closeDrawerBtn\" style=\"width:40px; background:none; font-size:22px;\"><i class=\"fas fa-times\"></i></button>\n" +
               "  </div>\n" +
               "  <h3><i class=\"fas fa-link\"></i> M3U URL</h3>\n" +
               "  <input id=\"m3uUrl\" placeholder=\"Playlist URL\">\n" +
               "  <button onclick=\"loadM3UUrl()\"><i class=\"fas fa-play-circle\"></i> Load</button>\n" +
               "  <h3><i class=\"fas fa-upload\"></i> M3U File</h3>\n" +
               "  <input type=\"file\" id=\"fileInput\" accept=\".m3u,.m3u8\">\n" +
               "  <div class=\"signature\">\n" +
               "    <div>Developed by-</div>\n" +
               "    <div class=\"dev-name\">PANKAJ BARUA</div>\n" +
               "    <div class=\"company\">TP Soft, Chattogram</div>\n" +
               "  </div>\n" +
               "</div>\n" +
               "<div class=\"controls-wrapper\">\n" +
               "  <div class=\"top-bar\" id=\"topBar\">\n" +
               "    <button id=\"menuToggleBtn\"><i class=\"fas fa-bars\"></i></button>\n" +
               "    <div id=\"channelBtn\"><i class=\"fas fa-list\"></i> Channels</div>\n" +
               "  </div>\n" +
               "  <div id=\"countryTabs\"></div>\n" +
               "  <div id=\"bottomControls\">\n" +
               "    <button class=\"ctrlBtn\" onclick=\"prevChannel()\"><i class=\"fas fa-step-backward\"></i></button>\n" +
               "    <button class=\"ctrlBtn\" onclick=\"togglePlay()\"><i class=\"fas fa-play\" id=\"playPauseIcon\"></i></button>\n" +
               "    <button class=\"ctrlBtn\" onclick=\"nextChannel()\"><i class=\"fas fa-step-forward\"></i></button>\n" +
               "    <div class=\"vol-slider\"><i class=\"fas fa-volume-up\"></i><input type=\"range\" id=\"volumeRange\" min=\"0\" max=\"100\" value=\"70\"></div>\n" +
               "  </div>\n" +
               "</div>\n" +
               "<div id=\"playerContainer\">\n" +
               "  <video id=\"video\" autoplay controls></video>\n" +
               "</div>\n" +
               "<div id=\"overlay\">\n" +
               "  <div class=\"overlay-header\"><h3>Channel List</h3><button id=\"closeOverlay\"><i class=\"fas fa-times\"></i></button></div>\n" +
               "  <input id=\"searchBox\" placeholder=\"Search...\" onkeyup=\"searchChannel()\">\n" +
               "  <div id=\"popup\"></div>\n" +
               "</div>\n" +
               "<div id=\"toastMsg\" class=\"toast-msg\"></div>\n" +
               "<script>\n" +
               "  const video = document.getElementById('video');\n" +
               "  let hls = null, filteredChannels = [], currentIndex = 0, groupsMap = {}, currentGroup = 'All';\n" +
               "  function showToast(msg) { let t = document.getElementById('toastMsg'); t.innerText = msg; t.style.opacity = '1'; setTimeout(() => t.style.opacity = '0', 2000); }\n" +
               "  function saveData() { localStorage.setItem('iptv_data', JSON.stringify({ groupsMap, currentGroup, currentIndex })); }\n" +
               "  function loadData() { let d = localStorage.getItem('iptv_data'); if(d) { let data = JSON.parse(d); if(data.groupsMap) { groupsMap = data.groupsMap; currentGroup = data.currentGroup; currentIndex = data.currentIndex; updateTabs(); filteredChannels = groupsMap[currentGroup] || []; renderList(); if(filteredChannels.length) playChannel(currentIndex); } } }\n" +
               "  function updateTabs() { let tabs = document.getElementById('countryTabs'); tabs.innerHTML = ''; Object.keys(groupsMap).forEach(g => { let btn = document.createElement('button'); btn.innerText = g; btn.onclick = () => { currentGroup = g; filteredChannels = groupsMap[g]; currentIndex = 0; playChannel(0); renderList(); document.querySelectorAll('#countryTabs button').forEach(b => b.classList.remove('active-tab')); btn.classList.add('active-tab'); saveData(); }; tabs.appendChild(btn); }); if(tabs.firstChild) tabs.firstChild.click(); }\n" +
               "  function renderList() { let pop = document.getElementById('popup'); pop.innerHTML = ''; filteredChannels.forEach((c, i) => { let div = document.createElement('div'); div.className = 'channel' + (i === currentIndex ? ' active' : ''); div.innerHTML = '<img src=\"' + (c.logo || 'https://via.placeholder.com/45') + '\" onerror=\"this.src='https://via.placeholder.com/45'\"><div>' + c.name + '</div>'; div.onclick = () => { playChannel(i); closeMenu(); }; pop.appendChild(div); }); }\n" +
               "  function playChannel(i) { if(i<0) i=0; if(i>=filteredChannels.length) i=0; currentIndex = i; let ch = filteredChannels[currentIndex]; if(!ch) return; if(hls) hls.destroy(); if(Hls.isSupported() && ch.url.includes('.m3u8')) { hls = new Hls(); hls.loadSource(ch.url); hls.attachMedia(video); } else { video.src = ch.url; } video.play(); renderList(); showToast(ch.name); saveData(); }\n" +
               "  function parseM3U(text) { let lines = text.split('\\n'); let arr = [], cur = null; for(let l of lines) { l = l.trim(); if(l.startsWith('#EXTINF')) { let name = l.split(',').pop() || 'Channel'; let logo = (l.match(/tvg-logo=\"([^\"])\"/) || [])[1] || ''; let group = (l.match(/group-title=\"([^\"])\"/) || [])[1] || 'General'; cur = { name, logo, group }; } else if(l.startsWith('http') && cur) { arr.push({ ...cur, url: l }); cur = null; } } let grp = {}; arr.forEach(c => { if(!grp[c.group]) grp[c.group] = []; grp[c.group].push(c); }); groupsMap = grp; if(!groupsMap['All']) groupsMap['All'] = arr; updateTabs(); filteredChannels = groupsMap[Object.keys(groupsMap)[0]]; currentIndex = 0; playChannel(0); saveData(); showToast(arr.length + ' channels loaded'); }\n" +
               "  async function loadM3UUrl() { let url = document.getElementById('m3uUrl').value; if(!url) return showToast('Enter URL'); let r = await fetch(url); let t = await r.text(); parseM3U(t); }\n" +
               "  document.getElementById('fileInput').onchange = e => { let f = e.target.files[0]; let r = new FileReader(); r.onload = ev => parseM3U(ev.target.result); r.readAsText(f); };\n" +
               "  window.nextChannel = () => { if(filteredChannels.length) playChannel((currentIndex+1)%filteredChannels.length); };\n" +
               "  window.prevChannel = () => { if(filteredChannels.length) playChannel((currentIndex-1+filteredChannels.length)%filteredChannels.length); };\n" +
               "  window.togglePlay = () => { video.paused ? video.play() : video.pause(); };\n" +
               "  window.searchChannel = () => { let q = document.getElementById('searchBox').value.toLowerCase(); let pop = document.getElementById('popup'); if(!q) renderList(); else { let filtered = filteredChannels.filter(c => c.name.toLowerCase().includes(q)); pop.innerHTML = ''; filtered.forEach((c,i) => { let div = document.createElement('div'); div.className = 'channel'; div.innerHTML = '<img src=\"' + (c.logo || 'https://via.placeholder.com/45') + '\"><div>' + c.name + '</div>'; div.onclick = () => { playChannel(filteredChannels.indexOf(c)); closeMenu(); }; pop.appendChild(div); }); } };\n" +
               "  function openMenu() { document.getElementById('leftPanel').classList.add('open'); }\n" +
               "  function closeMenuDrawer() { document.getElementById('leftPanel').classList.remove('open'); }\n" +
               "  window.toggleMenu = function() { let ov = document.getElementById('overlay'); ov.classList.toggle('show'); if(ov.classList.contains('show')) renderList(); };\n" +
               "  function closeMenu() { document.getElementById('overlay').classList.remove('show'); }\n" +
               "  document.getElementById('menuToggleBtn').onclick = () => { let lp = document.getElementById('leftPanel'); lp.classList.contains('open') ? closeMenuDrawer() : openMenu(); };\n" +
               "  document.getElementById('closeDrawerBtn').onclick = closeMenuDrawer;\n" +
               "  document.getElementById('channelBtn').onclick = () => toggleMenu();\n" +
               "  document.getElementById('closeOverlay').onclick = closeMenu;\n" +
               "  video.addEventListener('play', () => document.getElementById('playPauseIcon').className = 'fas fa-pause');\n" +
               "  video.addEventListener('pause', () => document.getElementById('playPauseIcon').className = 'fas fa-play');\n" +
               "  let vol = document.getElementById('volumeRange'); vol.addEventListener('input', () => video.volume = vol.value/100);\n" +
               "  video.volume = 0.7; vol.value = 70;\n" +
               "  loadData();\n" +
               "  if(!localStorage.getItem('iptv_data')) showToast('Welcome! Load M3U playlist');\n" +
               "</script>\n" +
               "</body>\n" +
               "</html>";
    }
}
