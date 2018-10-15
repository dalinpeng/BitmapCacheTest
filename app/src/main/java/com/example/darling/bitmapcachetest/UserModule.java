package com.example.darling.bitmapcachetest;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.example.darling.bitmapcachetest.adapter.UserAdapter;
import com.example.darling.bitmapcachetest.mvp.BitmapCacheContract;
import com.example.darling.bitmapcachetest.mvp.model.UserModel;
import com.example.darling.bitmapcachetest.mvp.model.entity.User;
import com.jess.arms.di.scope.ActivityScope;

import java.util.ArrayList;
import java.util.List;

import dagger.Module;
import dagger.Provides;

@Module
public class UserModule {
    private BitmapCacheContract.View view;

    //构建UserModule时,将View的实现类传进来,这样就可以提供View的实现类给presenter
    public UserModule(BitmapCacheContract.View view) {
        this.view = view;
    }
    @ActivityScope
    @Provides
    BitmapCacheContract.View provideUserView(){
        return this.view;
    }

    @ActivityScope
    @Provides
    BitmapCacheContract.Model provideUserModel(UserModel model){
        return model;
    }

    @ActivityScope
    @Provides
    RecyclerView.LayoutManager provideLayoutManager() {
        return new GridLayoutManager(view.getActivity(), 2);
    }

    @ActivityScope
    @Provides
    List<User> provideUserList() {
        return new ArrayList<>();
    }

    @ActivityScope
    @Provides
    RecyclerView.Adapter provideUserAdapter(List<User> list){
        return new UserAdapter(list);
    }
}
