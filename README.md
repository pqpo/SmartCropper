# SmartCropper

## [English](README_EN.md) | 中文

简单易用的智能图片裁剪库，适用于身份证，名片，文档等照片的裁剪。 如果觉得还不错，欢迎 start，fork。

## 支持特性

- 使用智能算法(基于opencv)识别图片中的边框  
- 支持拖动锚点，手动调节选区，放大镜效果提升定位体验
- 使用透视变换裁剪并矫正选区，还原正面图片
- 支持丰富的UI设置，如辅助线，蒙版，锚点，放大镜等

## 例子（[传送门](art/SmartCropperSampleV5.apk)）

![](art/download_qr.png)

### 1. 选择图片后智能选区，使用透视变换裁剪并矫正选区：

![](art/smart_crop_1.png)
![](art/cropped_1.png)

### 2. 拖动锚点，手动调节选区，右上角放大镜效果方便拖拽定位：

![](art/advance_crop_2.png)

### gif 动画：

![](art/smartcropper_photo.gif)
![](art/smartcropper_album_1.gif)

## 接入

可以直接依赖 aar 文件夹下的 aar 文件，也可以 clone 项目，将 smartcropperlib 作为 Android 模块导入。
另外 libs 目录下是编译好的 native library，如果引入项目不想编译，可以直接使用。（JCenter 仓库地址之后提供）

注意：由于使用了 JNI， 请**不要混淆**  

```
-keep class me.pqpo.smartcropperlib.**{*;}
```  

## 使用  

### 1. 裁剪布局：  
```xml
<me.pqpo.smartcropperlib.view.CropImageView   
        android:id="@+id/iv_crop"  
        android:layout_width="match_parent" 
        android:layout_height="match_parent" />  
```  

注意： CropImageView 继承至 ImageView，但是 ScaleType 必须为居中类型，如果手动设置成 fit_end,fit_start,matrix 将会报错。  

### 2. 设置待裁剪图片：    
```java
ivCrop.setImageToCrop(selectedBitmap); 
```

该方法内部会使用 native 代码智能识别边框，并绘制图片与选区。在 native 层实现，大大的提高了运行效率，运行时间与图片大小成正比，在大图片的情况下，可以考虑在子线程执行，或者压缩传入的图片。

### 3. 裁剪选区内的图片：

```java  
Bitmap crop = ivCrop.crop();  
```  

根据选区裁剪出选区内的图片，并使用透视变换矫正成正面图片。  

注意：改方法主要逻辑也是位于 native 层，运行时间与图片大小成正比，在大图片的情况下，可以考虑在子线程执行，或者压缩传入的图片。

## Attributes

|name|format|description|
|:---:|:---:|:---:|
|civMaskAlpha|integer|选区外蒙版的透明度，取值范围 0-255|
|civShowGuideLine|boolean|是否显示辅助线，默认 true|
|civLineColor|color|选区线的颜色|
|civLineWidth|dimension|选区线的宽度|
|civShowMagnifier|boolean|在拖动的时候是否显示放大镜，默认 true|
|civMagnifierCrossColor|color|放大镜十字准心的颜色|
|civGuideLineWidth|dimension|辅助线宽度|
|civGuideLineColor|color|辅助线颜色|
|civPointFillColor|color|锚点内部区域填充颜色|
|civPointFillAlpha|integer|锚点内部区域填充颜色透明度|

## Features

- [x] 优化点排序算法
- [x] CropImageView 选区放大镜效果
- [x] CropImageView xml属性配置
- [ ] 优化智能选区算法
- [ ] 欢迎提 ISSUE

---

## 关于我：

- 邮箱：    pqponet@gmail.com
- GitHub：  [pqpo](https://github.com/pqpo)
- 博客：    [pqpo's notes](https://pqpo.me)
- Twitter: [Pqponet](https://twitter.com/Pqponet)
- 微信公众号: pqpo_me(扫下方二维码) 

<img src="art/qrcode_for_gh.jpg" width="200">

License
-------

    Copyright 2017 pqpo

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.




