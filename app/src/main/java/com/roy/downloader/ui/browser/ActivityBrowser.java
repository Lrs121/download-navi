package com.roy.downloader.ui.browser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.roy.downloader.R;
import com.roy.downloader.core.model.data.entity.BrowserBookmark;
import com.roy.downloader.core.utils.Utils;
import com.roy.downloader.databinding.ABrowserBottomAppBarBinding;
import com.roy.downloader.databinding.ABrowserTopAppBarBinding;
import com.roy.downloader.ui.FragmentCallback;
import com.roy.downloader.ui.SendTextToClipboard;
import com.roy.downloader.ui.adddownload.ActivityAddDownload;
import com.roy.downloader.ui.adddownload.AddInitParams;
import com.roy.downloader.ui.browser.bookmarks.ActivityBrowserBookmarks;
import com.roy.downloader.ui.browser.bookmarks.ActivityEditBookmark;
import com.roy.downloader.ui.settings.ActivitySettings;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

/*
 * A simple, WebView-based browser
 */

public class ActivityBrowser extends AppCompatActivity implements FragmentCallback {
    @SuppressWarnings("unused")
    private static final String TAG = ActivityBrowser.class.getSimpleName();

    private static final String TAG_DOUBLE_BACK_PRESSED = "double_back_pressed";
    private static final String TAG_IS_CURRENT_PAGE_BOOKMARKED = "is_current_page_bookmarked";
    private static final String TAG_CONTEXT_MENU_DIALOG = "context_menu_dialog";

    private BrowserViewModel viewModel;
    private WebView webView;
    private TextInputLayout addressLayout;
    private TextInputEditText addressInput;
    private CoordinatorLayout coordinatorLayout;
    private boolean doubleBackPressed = false;
    private boolean hideMenuButtons = false;
    private boolean isCurrentPageBookmarked = false;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (!Utils.isWebViewAvailable(this)) {
            Toast.makeText(getApplicationContext(), R.string.webview_is_required, Toast.LENGTH_SHORT).show();
            finish();
        }

        setTheme(Utils.getAppTheme(getApplicationContext()));
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this).get(BrowserViewModel.class);
        viewModel.observeUrlFetchState().observe(this, this::handleUrlFetchState);

        initView();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayShowTitleEnabled(false);
        initAddressBar();
        initWebView();

        if (savedInstanceState != null) {
            doubleBackPressed = savedInstanceState.getBoolean(TAG_DOUBLE_BACK_PRESSED);
            isCurrentPageBookmarked = savedInstanceState.getBoolean(TAG_IS_CURRENT_PAGE_BOOKMARKED);
            webView.restoreState(savedInstanceState);
        } else {
            String url = getUrlFromIntent();
            if (url != null) {
                viewModel.url.set(url);
                viewModel.loadUrl(webView);
            } else {
                viewModel.loadStartPage(webView);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        disposables.clear();
    }

    @Override
    protected void onStart() {
        super.onStart();

        observeDownloadRequests();
    }

    private void observeDownloadRequests() {
        disposables.add(viewModel.observeDownloadRequests().observeOn(AndroidSchedulers.mainThread()).subscribe((request) -> {
            viewModel.stopLoading(webView);
            showAddDownloadDialog(request.getUrl());
        }));
    }

    private void showAddDownloadDialog(String url) {
        if (url == null) return;

        AddInitParams initParams = new AddInitParams();
        initParams.url = url;

        Intent i = new Intent(this, ActivityAddDownload.class);
        i.putExtra(ActivityAddDownload.TAG_INIT_PARAMS, initParams);
        startActivityForResult(i, 0);
    }

    private void handleUrlFetchState(BrowserViewModel.UrlFetchState fetchState) {
        isCurrentPageBookmarked = false;
        invalidateOptionsMenu();
        if (fetchState == BrowserViewModel.UrlFetchState.PAGE_FINISHED) {
            checkIsCurrentPageBookmarked();
        }
    }

    private void checkIsCurrentPageBookmarked() {
        disposables.add(viewModel.isCurrentPageBookmarked().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe((isBookmarked) -> {
            isCurrentPageBookmarked = isBookmarked;
            invalidateOptionsMenu();
        }, (e) -> {
            isCurrentPageBookmarked = false;
            invalidateOptionsMenu();
        }));
    }

    private String getUrlFromIntent() {
        Intent i = getIntent();
        if (i != null) {
            if (i.getData() != null) return i.getData().toString();
            else return i.getStringExtra(Intent.EXTRA_TEXT);
        }

        return null;
    }

    private void initView() {
        if (viewModel.pref.browserBottomAddressBar()) {
            ABrowserBottomAppBarBinding binding = DataBindingUtil.setContentView(this, R.layout.a_browser_bottom_app_bar);
            binding.setLifecycleOwner(this);
            binding.setViewModel(viewModel);
            setSupportActionBar(binding.toolbar); // change this to toolbar instead of bottomBar
            webView = binding.webView;
            addressLayout = binding.addressBar.addressLayout;
            addressInput = binding.addressBar.addressInput;
            coordinatorLayout = binding.coordinatorLayout;

        } else {
            ABrowserTopAppBarBinding binding = DataBindingUtil.setContentView(this, R.layout.a_browser_top_app_bar);
            binding.setLifecycleOwner(this);
            binding.setViewModel(viewModel);
            setSupportActionBar(binding.toolbar);
            webView = binding.webView;
            addressLayout = binding.addressBar.addressLayout;
            addressInput = binding.addressBar.addressInput;
            coordinatorLayout = binding.coordinatorLayout;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(TAG_DOUBLE_BACK_PRESSED, doubleBackPressed);
        outState.putBoolean(TAG_IS_CURRENT_PAGE_BOOKMARKED, isCurrentPageBookmarked);
        webView.saveState(outState);

        super.onSaveInstanceState(outState);
    }

    private void initWebView() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            webView.setAnimationCacheEnabled(false);
            webView.setAlwaysDrawnWithCacheEnabled(false);
        }
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setLongClickable(true);
        webView.setOnLongClickListener((v) -> {
            WebView.HitTestResult result = webView.getHitTestResult();
            switch (result.getType()) {
                case WebView.HitTestResult.SRC_ANCHOR_TYPE:
                case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE:
                case WebView.HitTestResult.IMAGE_TYPE:
                    String url = result.getExtra();
                    if (url != null) showContextMenu(url);
                    return true;
                default:
                    return false;
            }
        });
        viewModel.initWebView(webView);
    }

    private void initAddressBar() {
        KeyboardVisibilityEvent.setEventListener(this, this, (isOpen) -> {
            if (!isOpen) addressLayout.clearFocus();
        });

        addressLayout.setEndIconOnClickListener((v) -> viewModel.url.set(""));
        toggleClearButton(false);

        addressInput.setOnFocusChangeListener((v, hasFocus) -> {
            /* Move to the beginning of the address bar after keyboard hiding */
            if (!hasFocus) addressInput.setSelection(0);
            toggleMenuButtons(hasFocus);
            toggleClearButton(hasFocus && !TextUtils.isEmpty(viewModel.url.get()));
        });

        addressInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                toggleClearButton(s.length() > 0 && addressInput.hasFocus());
            }
        });

        addressInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                addressLayout.clearFocus();
                hideKeyboard();
                viewModel.loadUrl(webView);

                return true;
            }
            return false;
        });
    }

    private void toggleClearButton(boolean show) {
        addressLayout.setEndIconVisible(show);
    }

    private void toggleMenuButtons(boolean hide) {
        hideMenuButtons = hide;
        invalidateOptionsMenu();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(addressInput.getWindowToken(), 0);
    }

    private void showContextMenu(String url) {
        DialogBrowserContextMenu dialog = DialogBrowserContextMenu.newInstance(url);
        dialog.show(getSupportFragmentManager(), TAG_CONTEXT_MENU_DIALOG);
    }

    @Override
    public void fragmentFinished(Intent intent, ResultCode code) {
        if (code != ResultCode.OK) return;

        String action = intent.getAction();
        if (action == null) return;

        String url = intent.getStringExtra(DialogBrowserContextMenu.TAG_URL);
        switch (action) {
            case DialogBrowserContextMenu.TAG_ACTION_SHARE -> makeShareDialog(url);
            case DialogBrowserContextMenu.TAG_ACTION_DOWNLOAD -> showAddDownloadDialog(url);
            case DialogBrowserContextMenu.TAG_ACTION_COPY -> showCopyToClipboardDialog(url);
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (hideMenuButtons) return false;

        getMenuInflater().inflate(R.menu.menu_browser, menu);

        if (menu instanceof MenuBuilder menuBuilder) {
            menuBuilder.setOptionalIconsVisible(true);
        }

        return true;
    }

    // fix the error crashing on searchbar input clicked it crashes 
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        BrowserViewModel.UrlFetchState state = viewModel.observeUrlFetchState().getValue();

        MenuItem refresh = menu.findItem(R.id.refreshMenu);
        MenuItem stop = menu.findItem(R.id.stopMenu);
        if (refresh != null && stop != null) {
            boolean fetching = state == BrowserViewModel.UrlFetchState.FETCHING || state == BrowserViewModel.UrlFetchState.PAGE_STARTED;
            refresh.setVisible(!fetching);
            stop.setVisible(!fetching);
        }

        MenuItem forward = menu.findItem(R.id.forwardMenu);
        if (forward != null) {
            forward.setVisible(webView.canGoForward());
        }

        MenuItem desktopVersion = menu.findItem(R.id.desktopVersionMenu);
        if (desktopVersion != null) {
            desktopVersion.setChecked(viewModel.isDesktopMode());
        }

        MenuItem addBookmark = menu.findItem(R.id.addBookmarkMenu);
        MenuItem editBookmark = menu.findItem(R.id.editBookmarkMenu);
        if (addBookmark != null && editBookmark != null) {
            addBookmark.setVisible(!isCurrentPageBookmarked);
            editBookmark.setVisible(isCurrentPageBookmarked);
        }

        return true;
    }

    // edited for go measure nothing wrong with it 
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.forwardMenu) {
            if (webView.canGoForward()) webView.goForward();
        } else if (itemId == R.id.stopMenu) {
            viewModel.stopLoading(webView);
        } else if (itemId == R.id.refreshMenu) {
            webView.reload();
        } else if (itemId == R.id.shareMenu) {
            makeShareDialog(viewModel.url.get());
        } else if (itemId == R.id.settingsMenu) {
            showSettings();
        } else if (itemId == R.id.desktopVersionMenu) {
            item.setChecked(!item.isChecked());
            viewModel.enableDesktopMode(webView, item.isChecked());
            webView.reload();
        } else if (itemId == R.id.bookmarksMenu) {
            showBookmarks();
        } else if (itemId == R.id.addBookmarkMenu) {
            addBookmark();
        } else if (itemId == R.id.editBookmarkMenu) {
            disposables.add(viewModel.getBookmarkForCurrentPage().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(this::showEditBookmarkDialog, (e) -> Log.e(TAG, Log.getStackTraceString(e))));
        } else if (itemId == R.id.closeMenu) {
            finish();
        } else if (itemId == R.id.startPageMenu) {
            viewModel.loadStartPage(webView);
        }

        return true;
    }

    private void makeShareDialog(String url) {
        if (url == null) return;

        startActivity(Intent.createChooser(Utils.makeShareUrlIntent(url), getString(R.string.share_via)));
    }

    private void showCopyToClipboardDialog(String url) {
        if (url == null) return;

        Intent i = new Intent(this, SendTextToClipboard.class);
        i.putExtra(Intent.EXTRA_TEXT, url);
        startActivityForResult(i, 0);
    }

    private void showSettings() {
        Intent i = new Intent(this, ActivitySettings.class);
        i.putExtra(ActivitySettings.TAG_OPEN_PREFERENCE, ActivitySettings.BrowserSettings);
        settings.launch(i);
    }

    private void showBookmarks() {
        bookmarks.launch(new Intent(this, ActivityBrowserBookmarks.class));
    }

    private void addBookmark() {
        disposables.add(viewModel.addBookmark().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe((bookmark) -> {
            showBookmarkAddedSnackbar(bookmark);
            isCurrentPageBookmarked = bookmark.url.equals(viewModel.url.get());
        }, this::showAddBookmarkFailedSnackbar));
    }

    private void showEditBookmarkDialog(BrowserBookmark bookmark) {
        if (bookmark == null) return;

        Intent i = new Intent(this, ActivityEditBookmark.class);
        i.putExtra(ActivityEditBookmark.TAG_BOOKMARK, bookmark);
        editBookmark.launch(i);
    }

    private void showBookmarkAddedSnackbar(BrowserBookmark bookmark) {
        Snackbar.make(coordinatorLayout, R.string.browser_bookmark_added, Snackbar.LENGTH_SHORT).setAction(R.string.browser_bookmark_edit_menu, (v) -> showEditBookmarkDialog(bookmark)).show();
    }

    private void showAddBookmarkFailedSnackbar(Throwable e) {
        Log.e(TAG, Log.getStackTraceString(e));

        Snackbar.make(coordinatorLayout, R.string.browser_bookmark_add_failed, Snackbar.LENGTH_SHORT).show();
    }

    final ActivityResultLauncher<Intent> settings = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        /*
         * Lazy applying settings
         * TODO: consider other options for applying settings
         */
        recreate();
    });

    final ActivityResultLauncher<Intent> editBookmark = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        Intent data = result.getData();
        handleEditBookmarkRequest(result.getResultCode(), data);
    });

    final ActivityResultLauncher<Intent> bookmarks = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        Intent data = result.getData();
        handleBookmarksRequest(data);
    });

    private void handleEditBookmarkRequest(int resultCode, @Nullable Intent data) {
        if (resultCode != RESULT_OK || data == null) return;

        String action = data.getAction();
        if (action == null) return;

        BrowserBookmark bookmark = data.getParcelableExtra(ActivityEditBookmark.TAG_BOOKMARK);

        String message = null;
        switch (action) {
            case ActivityEditBookmark.TAG_RESULT_ACTION_DELETE_BOOKMARK -> {
                message = getResources().getQuantityString(R.plurals.browser_bookmark_deleted, 1);
                if (bookmark != null && bookmark.url.equals(viewModel.url.get()))
                    isCurrentPageBookmarked = false;
            }
            case ActivityEditBookmark.TAG_RESULT_ACTION_DELETE_BOOKMARK_FAILED ->
                    message = getResources().getQuantityString(R.plurals.browser_bookmark_delete_failed, 1);
            case ActivityEditBookmark.TAG_RESULT_ACTION_APPLY_CHANGES_FAILED ->
                    message = getString(R.string.browser_bookmark_change_failed);
            case ActivityEditBookmark.TAG_RESULT_ACTION_APPLY_CHANGES ->
                    isCurrentPageBookmarked = bookmark != null && bookmark.url.equals(viewModel.url.get());
        }
        if (message != null)
            Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_SHORT).show();
    }

    private void handleBookmarksRequest(@Nullable Intent data) {
        String action = (data == null ? null : data.getAction());
        if (ActivityBrowserBookmarks.TAG_ACTION_OPEN_BOOKMARK.equals(action)) {
            BrowserBookmark bookmark = data.getParcelableExtra(ActivityBrowserBookmarks.TAG_BOOKMARK);
            if (bookmark == null) return;
            viewModel.url.set(bookmark.url);
            viewModel.loadUrl(webView);
        } else {
            checkIsCurrentPageBookmarked();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            doubleBackPressed = false;
            webView.goBack();
        } else {
            if (doubleBackPressed) {
                doubleBackPressed = false;
                super.onBackPressed();
            } else {
                doubleBackPressed = true;
                Toast.makeText(this, R.string.browser_back_pressed, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
