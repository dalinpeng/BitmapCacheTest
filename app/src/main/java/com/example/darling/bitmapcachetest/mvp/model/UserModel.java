package com.example.darling.bitmapcachetest.mvp.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.example.darling.bitmapcachetest.DiskLruCache;
import com.example.darling.bitmapcachetest.mvp.BitmapCacheContract;
import com.example.darling.bitmapcachetest.mvp.model.entity.User;
import com.example.darling.bitmapcachetest.mvp.utils.Utils;
import com.jess.arms.integration.IRepositoryManager;
import com.jess.arms.mvp.BaseModel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Scheduler;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.rx_cache2.DynamicKey;
import io.rx_cache2.EvictDynamicKey;

import static android.os.Environment.isExternalStorageRemovable;

public class UserModel extends BaseModel implements BitmapCacheContract.Model {

    @Inject
    public UserModel(IRepositoryManager repositoryManager) {
        super(repositoryManager);
    }

    /**
     * 它使用一个强引用（strong referenced）的LinkedHashMap保存最近引用的对象，并且在缓存超出设置大小的时候剔除（evict）最近最少使用到的对象
     */
    private LruCache<String, Bitmap> mMemoryCache;

    @Override
    public void onStart() {

        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };

    }

    public static final int USERS_PER_PAGE = 10;

    @Override
    public Observable<List<User>> getUsers(int lastIdQueried, boolean update) {
        //使用rxcache缓存,上拉刷新则不读取缓存,加载更多读取缓存
        return Observable.just(mRepositoryManager.obtainRetrofitService(CommonService.class).getUsers(lastIdQueried, USERS_PER_PAGE))
                .flatMap(new Function<Observable<List<User>>, ObservableSource<List<User>>>() {
                    @Override
                    public ObservableSource<List<User>> apply(@NonNull Observable<List<User>> listObservable) throws Exception {
                        return mRepositoryManager.obtainCacheService(CommonCache.class)
                                .getUsers(listObservable, new DynamicKey(lastIdQueried), new EvictDynamicKey(update))
                                .map(listReply -> listReply.getData());
                    }
                });
    }

    @Override
    public Observable<Bitmap> downloadBitmap(String iPath) {
        try {
            //对资源链接
            URL url = new URL(iPath);
            ;
            Observable<Bitmap> observable = Observable.just(url)
                    .map(new Function<URL, Bitmap>() {
                        @Override
                        public Bitmap apply(URL url) throws Exception {
                            //打开输入流
                            InputStream inputStream = url.openStream();
                            //对网上资源进行下载转换位图图片
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            inputStream.close();
                            return bitmap;
                        }
                    });

            Disposable disposable1 = Observable.just(url)
                    .map(new Function<URL, DiskLruCache.Editor>() {
                @Override
                public DiskLruCache.Editor apply(URL url) throws Exception {
                    //            File file=new File(Environment.getExternalStorageDirectory()+"/haha.gif");
//            FileOutputStream fileOutputStream = new FileOutputStream(file);

                    InputStream inputStream = url.openStream();
                    int hasRead = 0;
                    OutputStream outputStream = null;
                    DiskLruCache.Editor editor = null;
                    String key = Utils.getKeyForDisk(iPath);
                    // Also add to disk cache
                    synchronized (mDiskCacheLock) {
                        if (mDiskLruCache != null && mDiskLruCache.get(key) == null) {
                            //得到DiskLruCache.Editor
                            editor = mDiskLruCache.edit(key);
                            if (editor != null) {
                                outputStream = editor.newOutputStream(0);
                                while ((hasRead = inputStream.read()) != -1) {
                                    outputStream.write(hasRead);
                                }
                                outputStream.close();
                            }
                        }
                    }
                    return editor;
                }
            })
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Consumer<DiskLruCache.Editor>() {
                        @Override
                        public void accept(DiskLruCache.Editor editor) throws Exception {
                            // Add to memory cache as before
                            //写入缓存
                            editor.commit();
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {

                        }
                    });

            return observable;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
//            if (urlConnection != null) {
//                urlConnection.disconnect();
//            }
            try {
                if (mDiskLruCache != null) {
                    mDiskLruCache.flush();
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public Bitmap getBitmap(String iPath) {
        //key name (md5 iPath)
        String imageKey = Utils.getKeyForDisk(iPath);

        Bitmap bitmap = getBitmapFromMemCache(imageKey);
        if (bitmap != null) {
            Log.d(TAG, "Image from MemCache " + iPath);

            return bitmap;
        } else {
            // Check disk cache in background thread
            bitmap = getBitmapFromDiskCache(imageKey);
            if (bitmap != null) {
                Log.d(TAG, "Image from DiskCache: " + iPath);
                return bitmap;
            }
            return null;
        }
    }

    @Override
    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    private static final String TAG = UserModel.class.getSimpleName();

    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }


    private final Object mDiskCacheLock = new Object();
    private boolean mDiskCacheStarting = true;
    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB
    private static final String DISK_CACHE_SUBDIR = "thumbnails";
    private DiskLruCache mDiskLruCache;

    public Observable<Boolean> initDiskCache(Context context) {
        // Initialize disk cache on background thread
        return Observable.just(getDiskCacheDir(context, DISK_CACHE_SUBDIR))
                .map(new Function<File, Boolean>() {
                    @Override
                    public Boolean apply(File file) throws Exception {
                        synchronized (mDiskCacheLock) {
                            File cacheDir = file;
                            //@param valueCount the number of values per cache entry. Must be positive.
                            //每个缓存条目..key
                            mDiskLruCache = DiskLruCache.open(cacheDir, 1, 1, DISK_CACHE_SIZE);
                            mDiskCacheStarting = false; // Finished initialization
                            mDiskCacheLock.notifyAll(); // Wake any waiting threads
                        }
                        return mDiskCacheStarting;
                    }
                });
    }

    Context mContext;

    /**
     * Creates a unique subdirectory of the designated app cache directory. Tries to use external
     * but if not mounted, falls back on internal storage.
     *
     * @param context
     * @param uniqueName
     * @return
     */
    public File getDiskCacheDir(Context context, String uniqueName) {
        mContext = context;
        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir
        final String cachePath =
                Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                        !isExternalStorageRemovable() ? context.getExternalCacheDir().getPath() :
                        context.getCacheDir().getPath();

        return new File(cachePath + File.separator + uniqueName);
    }

    /**
     * Note:因为初始化磁盘缓存涉及到I/O操作，所以它不应该在主线程中进行。但是这也意味着在初始化完成之前缓存可以被访问。
     * 为了解决这个问题，在上面的实现中，有一个锁对象（lock object）来确保在磁盘缓存完成初始化之前，应用无法对它进行读取。
     *
     * @param key
     * @return
     */
    public Bitmap getBitmapFromDiskCache(String key) {
        synchronized (mDiskCacheLock) {
            // Wait while disk cache is started from background thread
            while (mDiskCacheStarting) {
                try {
                    mDiskCacheLock.wait();
                } catch (InterruptedException e) {
                }
            }

//            if (mDiskLruCache != null) {
//                return mDiskLruCache.get(key);
//            }
            if (mDiskLruCache != null) {
                try {

                    DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
                    if (snapshot != null) {
                        InputStream in = snapshot.getInputStream(0);
                        return BitmapFactory.decodeStream(in);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public OutputStream addBitmapToCache(String key, Bitmap bitmap) throws IOException {
        // Add to memory cache as before
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
        // Also add to disk cache
        synchronized (mDiskCacheLock) {
            if (mDiskLruCache != null && mDiskLruCache.get(key) == null) {
                //得到DiskLruCache.Editor
                DiskLruCache.Editor editor = mDiskLruCache.edit(key);
                if (editor != null) {
                    return editor.newOutputStream(0);
                }
            }
            return null;
        }
    }

//    public Bitmap loadBitmapFromDiskCache(String imageKey) {
//        // Check disk cache in background thread
//        DiskLruCache.Value bitmap = getBitmapFromDiskCache(imageKey);
//        if (bitmap == null) { // Not found in disk cache
//            // Process as normal
////            final Bitmap bitmap = decodeSampledBitmapFromResource(
////                    getResources(), params[0], 100, 100));
//        }
//
//        // Add final bitmap to caches
//        addBitmapToCache(imageKey, bitmap);
//        return bitmap;
//    }
}
