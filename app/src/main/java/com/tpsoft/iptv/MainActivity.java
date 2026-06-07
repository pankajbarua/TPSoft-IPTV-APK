package com.tpsoft.iptv;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
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
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        
        webView = findViewById(R.id.webView);
        fullscreenContainer = findViewById(R.id.fullscreenContainer);
        progressBar = findViewById(R.id.progressBar);
        
        setupWebView();
        loadIPTVPlayer();
        setupImmersiveMode();
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                hideSystemUI();
            }
        });
        
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
                hideSystemUI();
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
                showSystemUI();
            }
        });
    }
    
    private void loadIPTVPlayer() {
        String htmlContent = getIPTVHTML();
        webView.loadDataWithBaseURL("https://tpsoft.iptv/", htmlContent, "text/html", "UTF-8", null);
    }
    
    private void setupImmersiveMode() {
        hideSystemUiRunnable = () -> hideSystemUI();
        webView.setOnTouchListener((v, event) -> {
            showSystemUI();
            handler.removeCallbacks(hideSystemUiRunnable);
            handler.postDelayed(hideSystemUiRunnable, 3000);
            return false;
        });
    }
    
    private void hideSystemUI() {
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                webView.loadUrl("javascript:prevChannel();");
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                webView.loadUrl("javascript:nextChannel();");
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                webView.loadUrl("javascript:togglePlay();");
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                webView.loadUrl("javascript:openMenu();");
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                webView.loadUrl("javascript:toggleMenu();");
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
    
    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
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
        // আপনার আগের দেওয়া সম্পূর্ণ HTML কোড এখানে বসবে
        // আপাতত একটি মিনিমাম ভার্সন দেওয়া হলো
        return "<!DOCTYPE html>\n" +
               "<html>\n" +
               "<head>\n" +
               "<meta charset=\"UTF-8\">\n" +
               "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
               "<title>TPSoft IPTV</title>\n" +
               "<script src=\"https://cdn.jsdelivr.net/npm/hls.js@latest\"></script>\n" +
               "<style>\n" +
               "body{background:#000;color:#fff;font-family:sans-serif;margin:0;padding:0;overflow:hidden}\n" +
               "video{width:100%;height:100%;object-fit:contain}\n" +
               "#controls{position:fixed;bottom:20px;left:0;right:0;text-align:center;background:rgba(0,0,0,0.5);padding:10px}\n" +
               "button{background:#ff6600;border:none;color:#fff;padding:10px 20px;margin:5px;border-radius:5px}\n" +
               "</style>\n" +
               "</head>\n" +
               "<body>\n" +
               "<video id=\"video\" controls autoplay></video>\n" +
               "<div id=\"controls\">\n" +
               "<button onclick=\"prevChannel()\">◀️ Prev</button>\n" +
               "<button onclick=\"togglePlay()\">⏯️ Play/Pause</button>\n" +
               "<button onclick=\"nextChannel()\">Next ▶️</button>\n" +
               "</div>\n" +
               "<script>\n" +
               "var channels = [\n" +
               "{name:'BTV', url:'https://owrcovcrpy.gpcdn.net/bpk-tv/1709/output/index.m3u8'},\n" +
               "{name:'Somoy TV', url:'https://owrcovcrpy.gpcdn.net/bpk-tv/1702/output/index.m3u8'},\n" +
               "{name:'Ekattor TV', url:'https://owrcovcrpy.gpcdn.net/bpk-tv/1705/output/index.m3u8'}\n" +
               "];\n" +
               "var video = document.getElementById('video');\n" +
               "var current = 0;\n" +
               "function playChannel(index) { video.src = channels[index].url; video.play(); current = index; }\n" +
               "function nextChannel() { current = (current+1)%channels.length; playChannel(current); }\n" +
               "function prevChannel() { current = (current-1+channels.length)%channels.length; playChannel(current); }\n" +
               "function togglePlay() { video.paused ? video.play() : video.pause(); }\n" +
               "playChannel(0);\n" +
               "</script>\n" +
               "</body>\n" +
               "</html>";
    }
}
