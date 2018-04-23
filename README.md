`MyMap`集成如下功能：
========================================
##1. 卫星定位
当启动手机APP时，APP会回`申请定位权限`的授权(在文件`AndroidManifest.xml`中定义)。
当定位开始，小米手机会有通知栏，显示`正在定位`，等定位完成，小米手机上通知栏上，会显示`定位完成`。
此时，我们的APP会自动`飞到`定位的位置。

-------------------------
技术来源： `mapbox-android-demo`-`plugins`-`LocalizationPluginActivity.java`
代码文件：
`com/mapbox/mapboxandroiddemo/examples/plugins/LocalizationPluginActivity.java`
-------------------------
但是，这个demo有个缺憾，无法手动`启用GPS定位`的图标，一开始就自动去启动了，非常不人性化啊：）
我尝试了使用项目：
`mapbox-plugins-android/`，使用开发版本的`0.5.0`
在文件`app/build.gradle`中，我们不用稳定版本：
`implementation 'com.mapbox.mapboxsdk:mapbox-android-plugin-locationlayer:0.4.0'`
而采用`开发版本`：
`implementation 'com.mapbox.mapboxsdk:mapbox-android-plugin-locationlayer:0.5.0-SNAPSHOT'`
结果，`开发版本有严重bug`，导致我们系统崩溃。网上也有反馈。
所以，我们最终采用的是稳定版本。
源自：
`mapbox-android-demo/`中的Demo: `LocationPluginActivity.java`.
-------------------------
========================================
##2. 地图风格选择
我们提供了两种地图：
++ 街道地图：中文支持
++ 卫星+街道地图： 不支持中文？
---------------------------
通过app的右上角`菜单`项，实现选择。

-------------------------
技术来源： `mapbox-android-demo`-`plugins`-`DefaultStyleActivity.java`
代码文件：
`com/mapbox/mapboxandroiddemo/examples/styles/DefaultStyleActivity.java`
-------------------------
========================================
##3. GeoJson格式文件的加载和显示
利用`MapBox`的`GeoJSON Plugin`，我们可以做到如下功能：
++ 手动选择用户手机里的`GeoJson`文件，然后进行加载；
++ 直接加载手机APP里存储的`GeoJson`文件；
++ 从网页上抓取GeoJson文件，并进行加载。
该功能为后续从服务器下载`定位位置``附件2公里`（距离自定义）的`光纤箱`做基础。
---------------------------
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
---------------------------
`访问外部存储空间的权限`：WRITE_EXTERNAL_STORAGE， GeoJSON插件例子中用到。

通过，点击右侧的三个`浮动按钮`，分别是：
---------------------------
++ `从本地文件夹选择`
++ `直接加载固定的文件名`
++ `从固定的网址上下载相应的GeoJson文件`
---------------------------

-------------------------
技术来源： `mapbox-android-demo`-`plugins`-`GeoJsonPluginActivity.java`
代码文件：
`com/mapbox/mapboxandroiddemo/examples/plugins/GeoJsonPluginActivity.java`
-------------------------
========================================
##4. 限制区域为烟台市

通过设定一个矩形区域：烟台市，
用来控制整个地图的使用不超过该区域。
调试时，可通过`showBoundsArea`函数，可以显示出该区域。
默认，禁用了该功能。

技术来源：`mapbox-android-demo`-`camera`--`RestrictCameraActivity.java`
Demo名称： Restrict map panning.
说明： Prevent a map from being panned to a different place.
代码文件：
`com/mapbox/mapboxandroiddemo/examples/camera/RestrictCameraActivity.java`
========================================

========================================
##5 添加十字花
为了显示地图的中心位置，我们添加了十字花功能。
详见：`showCrosshair()`
========================================


========================================
##6 权限请求
我们添加了两种请求：
一种是请求`访问存储器`（源自GeoJsonPlugin）
一种是请求`访问位置信息`（源自LocationPlugin）,
详见：`showCrosshair()`
当集成起来的时候很难处理，存在冲突的情况：
我们在函数`onRequestPermissionsResult`中，巧妙的进行了分类处理：
-------------------------------
@Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case 1: {
                // from GeoJsonPluginActivity.java
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.v("Permission: " + permissions[0] + "was " + grantResults[0]);
                    showFileChooserDialog();
                }
                return;
            }

            case 0:
            {
                // from LocationPluginActivity.java
                permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
                return;
            }
        }
    }
-------------------------------
其中`requestCode`，是通过阅读代码，发现`PermissionsManager`调用`requestLocationPermissions`时，
-------------------------------
permissionsManager = new PermissionsManager(this);
permissionsManager.requestLocationPermissions(this);
-------------------------------
其请求码为`0`。
而GeoJsonPlugin申请`WRITE_EXTERNAL_STORAGE`权限时，其请求码为`1`：
-------------------------------
ActivityCompat.requestPermissions(MainActivity.this,
                                new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
-------------------------------
从而进行有效区分。
========================================
