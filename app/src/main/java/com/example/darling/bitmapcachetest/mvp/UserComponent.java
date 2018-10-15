package com.example.darling.bitmapcachetest.mvp;

import com.example.darling.bitmapcachetest.MainActivity;
import com.example.darling.bitmapcachetest.UserModule;
import com.jess.arms.di.component.AppComponent;
import com.jess.arms.di.scope.ActivityScope;

import dagger.Component;

@ActivityScope
@Component(modules = UserModule.class,dependencies = AppComponent.class)
public interface UserComponent {
    void inject(MainActivity activity);
}
