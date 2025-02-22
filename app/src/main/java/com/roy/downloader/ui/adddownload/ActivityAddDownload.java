package com.roy.downloader.ui.adddownload;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.roy.downloader.R;
import com.roy.downloader.core.RepositoryHelper;
import com.roy.downloader.core.model.data.entity.DownloadInfo;
import com.roy.downloader.core.settings.SettingsRepository;
import com.roy.downloader.core.utils.Utils;
import com.roy.downloader.ui.BaseAlertDialog;
import com.roy.downloader.ui.BatteryOptimizationDialog;
import com.roy.downloader.ui.FragmentCallback;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class ActivityAddDownload extends AppCompatActivity
        implements FragmentCallback {
    public static final String TAG_INIT_PARAMS = "init_params";

    private static final String TAG_DOWNLOAD_DIALOG = "add_download_dialog";
    private static final String TAG_BATTERY_DIALOG = "battery_dialog";

    private DialogAddDownload dialogAddDownload;
    private BatteryOptimizationDialog batteryDialog;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private BaseAlertDialog.SharedViewModel dialogViewModel;
    private SettingsRepository pref;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(Utils.getTranslucentAppTheme(getApplicationContext()));
        super.onCreate(savedInstanceState);

        pref = RepositoryHelper.getSettingsRepository(getApplicationContext());
        ViewModelProvider provider = new ViewModelProvider(this);
        dialogViewModel = provider.get(BaseAlertDialog.SharedViewModel.class);

        FragmentManager fm = getSupportFragmentManager();
        dialogAddDownload = (DialogAddDownload) fm.findFragmentByTag(TAG_DOWNLOAD_DIALOG);
        if (dialogAddDownload == null) {
            AddInitParams initParams = null;
            Intent i = getIntent();
            if (i != null)
                initParams = i.getParcelableExtra(TAG_INIT_PARAMS);
            if (initParams == null) {
                initParams = new AddInitParams();
            }
            fillInitParams(initParams);
            dialogAddDownload = DialogAddDownload.newInstance(initParams);
            dialogAddDownload.show(fm, TAG_DOWNLOAD_DIALOG);
        }
        batteryDialog = (BatteryOptimizationDialog) fm.findFragmentByTag(TAG_BATTERY_DIALOG);
        if (Utils.shouldShowBatteryOptimizationDialog(this)) {
            showBatteryOptimizationDialog();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        subscribeAlertDialog();
    }

    @Override
    protected void onStop() {
        super.onStop();

        disposables.clear();
    }

    private void subscribeAlertDialog() {
        Disposable d = dialogViewModel.observeEvents()
                .subscribe((event) -> {
                    if (event.dialogTag == null) {
                        return;
                    }
                    if (event.dialogTag.equals(TAG_BATTERY_DIALOG)) {
                        if (event.type != BaseAlertDialog.EventType.DIALOG_SHOWN) {
                            batteryDialog.dismiss();
                            pref.askDisableBatteryOptimization(false);
                        }
                        if (event.type == BaseAlertDialog.EventType.POSITIVE_BUTTON_CLICKED) {
                            Utils.requestDisableBatteryOptimization(this);
                        }
                    }
                });
        disposables.add(d);
    }

    private void fillInitParams(AddInitParams params) {
        SettingsRepository pref = RepositoryHelper.getSettingsRepository(getApplicationContext());
        SharedPreferences localPref = PreferenceManager.getDefaultSharedPreferences(this);

        if (params.url == null) {
            params.url = getUrlFromIntent();
        }
        if (params.dirPath == null) {
            if (pref != null) {
                params.dirPath = Uri.parse(pref.saveDownloadsIn());
            }
        }
        if (params.retry == null) {
            params.retry = localPref.getBoolean(
                    getString(R.string.add_download_retry_flag),
                    true
            );
        }
        if (params.replaceFile == null) {
            params.replaceFile = localPref.getBoolean(
                    getString(R.string.add_download_replace_file_flag),
                    false
            );
        }
        if (params.unmeteredConnectionsOnly == null) {
            params.unmeteredConnectionsOnly = localPref.getBoolean(
                    getString(R.string.add_download_unmetered_only_flag),
                    false
            );
        }
        if (params.numPieces == null) {
            params.numPieces = localPref.getInt(
                    getString(R.string.add_download_num_pieces),
                    DownloadInfo.MIN_PIECES
            );
        }
        if (params.uncompressArchive == null) {
            params.uncompressArchive = localPref.getBoolean(
                    getString(R.string.add_download_uncompress_archive_flag),
                    false
            );
        }
    }

    private String getUrlFromIntent() {
        Intent i = getIntent();
        if (i != null) {
            if (i.getData() != null)
                return i.getData().toString();
            else
                return i.getStringExtra(Intent.EXTRA_TEXT);
        }

        return null;
    }

    private void showBatteryOptimizationDialog() {
        var fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(TAG_BATTERY_DIALOG) == null) {
            batteryDialog = BatteryOptimizationDialog.newInstance();
            var ft = fm.beginTransaction();
            ft.add(batteryDialog, TAG_BATTERY_DIALOG);
            ft.commitAllowingStateLoss();
        }
    }

    @Override
    public void fragmentFinished(Intent intent, ResultCode code) {
        finish();
    }

    @Override
    public void onBackPressed() {
        dialogAddDownload.onBackPressed();
    }
}
