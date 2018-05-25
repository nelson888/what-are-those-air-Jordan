# What are those air J
This is the code of an Android app that predicts which model are your Air Jordan shoes, with the TensorFlow API.

![alt text](https://raw.githubusercontent.com/nelson888/what-are-those-air-Jordan/master/screenshots.png)

## TensorFlow Models
There are two models for predicting: one with 10 classes (Air Jordan 1 to 10) in the model_10/ directory and the other for 32 classes (Air Jordan 1 to 32) in the model_32/ directory. I followed the image retraining tutorial (https://www.tensorflow.org/tutorials/image_retraining) to make them.
These models have a final test accuracy of respectively 76.5% (N=426) and 59.1% (N=1097).

You can switch models in the app by replacing the model.pb and labels.txt by the one desired
