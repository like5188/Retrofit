#### 最新版本

模块|Retrofit
---|---
最新版本|[![Download](https://jitpack.io/v/like5188/Retrofit.svg)](https://jitpack.io/#like5188/Retrofit)

## 功能介绍

1、用 Retrofit2 + Kotlin + coroutines + RxJava2 + LiveData 封装的库。

2、提供了普通网络请求、下载、上传功能。具体功能请查看相关模块。

## 使用方法：

1、引用

在Project的gradle中加入：
```groovy
    allprojects {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }
```
在Module的gradle中加入：
```groovy
    dependencies {
        // coroutines
        implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:版本号'
        implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:版本号'
        // 核心代码（必须）
        implementation 'com.github.like5188.Retrofit:core:版本号'
        // 普通网络请求
        implementation 'com.github.like5188.Retrofit:common:版本号'
        // 下载
        implementation 'com.github.like5188.Retrofit:download:版本号'
        // 上传
        implementation 'com.github.like5188.Retrofit:upload:版本号'
    }
```