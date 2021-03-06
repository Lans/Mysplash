package com.wangdaye.mysplash.common.provider;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.google.android.apps.muzei.api.provider.Artwork;
import com.google.android.apps.muzei.api.provider.MuzeiArtProvider;
import com.google.android.apps.muzei.api.provider.ProviderContract;
import com.wangdaye.mysplash.R;
import com.wangdaye.mysplash.common.data.entity.unsplash.Photo;
import com.wangdaye.mysplash.common.utils.helper.MuzeiUpdateHelper;

import java.util.ArrayList;
import java.util.List;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class MysplashMuzeiArtProvider extends MuzeiArtProvider {

    @Override
    protected void onLoadRequested(boolean initial) {
        MysplashMuzeiWorker.enqueue();
    }
}

class MysplashMuzeiWorker extends Worker
        implements MuzeiUpdateHelper.OnUpdateCallback {

    public MysplashMuzeiWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    static void enqueue() {
        WorkManager.getInstance()
                .enqueue(new OneTimeWorkRequest.Builder(MysplashMuzeiWorker.class)
                        .setConstraints(new Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build())
                        .build());
    }

    @NonNull
    @Override
    public Result doWork() {
        if (MuzeiUpdateHelper.update(getApplicationContext(), this)) {
            return Result.success();
        } else {
            return Result.failure();
        }
    }

    @Override
    public void onUpdateSucceed(@NonNull List<Photo> photoList) {
        List<Artwork> artworkList = new ArrayList<>(photoList.size());
        for (Photo p : photoList) {
            artworkList.add(new Artwork.Builder()
                    .title(getApplicationContext().getString(R.string.by) + " " + p.user.name)
                    .byline(getApplicationContext().getString(R.string.on) + " " + p.created_at.split("T")[0])
                    .persistentUri(Uri.parse(p.getWallpaperSizeUrl(getApplicationContext())))
                    .token(p.id)
                    .webUri(Uri.parse(p.links.html))
                    .build());
        }
        ProviderContract.getProviderClient(getApplicationContext(), MysplashMuzeiArtProvider.class)
                .addArtwork(artworkList);
    }

    @Override
    public void onUpdateFailed() {
        // do nothing.
    }
}