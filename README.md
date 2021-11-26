# Style Transfer
The ability to create a new image,known as a pastiche, based on two input images:one representing the artistic style and one representing the content using deep learning.


# Source
This library has been inspired by [TensorflowHub\\Style Transfer](https://tfhub.dev/google/lite-model/magenta/arbitrary-image-stylization-v1-256/int8/prediction/1).

## Integration
 1. For using StyleTransfer module in sample app, include the source code and add the below dependencies in entry/build.gradle to generate hap/support.har.

```
	implementation project(path: ':stylize')
```

 2. For using StyleTransfer module in separate application using har file, add the har file in the entry/libs folder and add the dependencies in entry/build.gradle file.

```
	implementation fileTree(dir: 'libs', include: ['*.har'])
```
 3. For using StyleTransfer module from a remote repository in separate application, add the below dependencies in entry/build.gradle file.

```
	implementation 'dev.applibgroup:stylize:1.0.0'
```

## Usage
 1. Initialise the constructor of Stylize with the image paths, image names, getResourceManager() and getCacheDir() arguments.
 
 2. Use get_output() to get the stylized image in float matrix.
Example:

```slice
    	Stylize stylizer = new Stylize(contentImgPath, contentImgName, styleImgPath, styleImgName,
    			getResourceManager(), getCacheDir());
       outputImage = stylizer.getOutput();
```
Check the example app for more information.

## License

	Copyright (c) TensorFlow Hub

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.

