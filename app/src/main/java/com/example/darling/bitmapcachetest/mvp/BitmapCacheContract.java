package com.example.darling.bitmapcachetest.mvp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;

import com.example.darling.bitmapcachetest.mvp.model.entity.User;
import com.jess.arms.base.DefaultAdapter;
import com.jess.arms.mvp.IModel;
import com.jess.arms.mvp.IView;

import java.io.File;
import java.util.List;

import io.reactivex.Observable;

public interface  BitmapCacheContract {

    interface View extends IView {
        void setAdapter(DefaultAdapter adapter);
        void startLoadMore();
        void endLoadMore(List<User> mUsers);
        Activity getActivity();
    }

    //Model层定义接口,外部只需关心Model返回的数据,无需关心内部细节,即是否使用缓存
    interface Model extends IModel {

        void onStart();

        Observable<List<User>> getUsers(int lastIdQueried, boolean update);

        Observable downloadBitmap(String iPath);

        Observable<Boolean> initDiskCache(Context context);

        File getDiskCacheDir(Context context, String uniqueName);

        Bitmap getBitmap(String key);

        void addBitmapToMemoryCache(String key,Bitmap bitmap);
    }
}
