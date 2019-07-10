#!/usr/bin/python
#coding=utf8

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import multiprocessing as mt

import tensorflow as tf
import const


def generate_batch_data(images, annotations, batch_size):

    def __map_fun(image_path, annotation_path):
        image_tensor = tf.read_file(image_path)
        image_tensor = tf.image.decode_jpeg(image_tensor, channels=3)
        image_tensor = tf.image.resize_images(image_tensor, [const.image_height, const.image_width])
        image_float = tf.to_float(image_tensor)
        image_float = image_float / 255.0

        annotation_content = tf.read_file(annotation_path)
        annotation_tensor = tf.image.decode_png(annotation_content, channels=1)
        annotation_tensor = tf.image.resize_images(annotation_tensor, [const.image_height, const.image_width])
        annotation_float = tf.to_float(annotation_tensor)
        annotation_float = annotation_float / 255.0

        return image_float, annotation_float

    data_set = tf.data.Dataset.from_tensor_slices((images, annotations))\
        .shuffle(100).repeat().map(__map_fun, num_parallel_calls=mt.cpu_count()).batch(batch_size)
    iterator = data_set.make_one_shot_iterator()
    return iterator.get_next()



