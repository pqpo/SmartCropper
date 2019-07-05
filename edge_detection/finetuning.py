#!/usr/bin/python
# coding=utf8

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

from hed_net import *
import numpy as np
import os
import util

from tensorflow import flags

flags.DEFINE_string('finetuning_dir', './checkpoint',
                    'finetuning directory.')
flags.DEFINE_string('checkpoint_dir', './checkpoint',
                    'Checkpoint directory.')
flags.DEFINE_string('image', 'test_image/IMG_20190705_161350.jpg', 'fine tuning image')
flags.DEFINE_string('annotation', 'test_image/annotation.png_threshold.jpg', 'fine tuning annotation')
flags.DEFINE_string('csv', '', 'fine tuning csv')
flags.DEFINE_float('lr', 0.0004, 'learning rate')
flags.DEFINE_integer('iterations', 10,
                     'Number of iterations, default 100.')
flags.DEFINE_float('output_threshold', 0.0, 'output threshold, default: 0.0')

FLAGS = flags.FLAGS

hed_ckpt_file_path = os.path.join(FLAGS.checkpoint_dir, 'hed.ckpt')

train_layer = ['dsn1', 'dsn2', 'dsn3', 'dsn4', 'dsn5', 'dsn_fuse']

if not ((os.path.exists(FLAGS.image)) and (os.path.exists(FLAGS.annotation)) or (os.path.exists(FLAGS.csv))):
    print('please add input, --img, --annotation or --csv')
    exit()

samples = []

if os.path.exists(FLAGS.image) and os.path.exists(FLAGS.annotation):
    samples.append((FLAGS.image, FLAGS.annotation))

if os.path.exists(FLAGS.csv):
    csv_samples = util.load_sample_from_csv(FLAGS.csv)
    if csv_samples is not None and len(csv_samples) > 0:
        samples.extend(csv_samples)

if len(samples) == 0:
    print('Samples is empty, exit()')
    exit()

print('fine tuning samples size: {}'.format(len(samples)))

if __name__ == "__main__":

    image_path_placeholder = tf.placeholder(tf.string)
    annotation_path_placeholder = tf.placeholder(tf.string)

    annotation_content = tf.read_file(annotation_path_placeholder)
    annotation_tensor = tf.image.decode_png(annotation_content, channels=1)
    annotation_tensor = tf.reshape(annotation_tensor, [const.image_height, const.image_width, 1])
    annotation_float = tf.to_float(annotation_tensor)
    annotation_float = annotation_float / 255.0
    annotation_float = tf.expand_dims(annotation_float, axis=0)

    image_tensor = tf.read_file(image_path_placeholder)
    image_tensor = tf.image.decode_jpeg(image_tensor, channels=3)
    origin_tensor = tf.image.resize_images(image_tensor, [const.image_height, const.image_width])
    image_float = tf.to_float(origin_tensor)
    image_float = image_float / 255.0
    image_float = tf.expand_dims(image_float, axis=0)

    dsn_fuse, dsn1, dsn2, dsn3, dsn4, dsn5 = mobilenet_v2_style_hed(image_float, True)

    hed_weights = tf.get_collection(tf.GraphKeys.GLOBAL_VARIABLES, scope='hed')

    cost = class_balanced_sigmoid_cross_entropy(dsn_fuse, annotation_float)

    # var_list = [v for v in tf.trainable_variables() if v.name.split('/')[1] not in train_layer]
    # gradients = tf.gradients(cost, var_list)
    # gradients = list(zip(gradients, var_list))

    with tf.control_dependencies(tf.get_collection(tf.GraphKeys.UPDATE_OPS)):
        # train_step = tf.train.AdamOptimizer(learning_rate=FLAGS.lr).apply_gradients(gradients)
        train_step = tf.train.AdamOptimizer(learning_rate=FLAGS.lr).minimize(cost)

    global_init = tf.global_variables_initializer()

    # Saver
    saver = tf.train.Saver(hed_weights)

    with tf.Session() as sess:
        sess.run(global_init)

        latest_ck_file = tf.train.latest_checkpoint(FLAGS.finetuning_dir)
        if latest_ck_file:
            print('restore from latest checkpoint file : {}'.format(latest_ck_file))
            saver.restore(sess, latest_ck_file)
        else:
            print('no checkpoint file to restore, exit()')
            exit()

        for epoch in range(FLAGS.iterations):
            for step in range(len(samples)):
                sample = samples[step]
                feed_dict_to_use = {image_path_placeholder: sample[0],
                                    annotation_path_placeholder: sample[1]}
                _dsn_fuse, _ = sess.run([dsn_fuse, train_step], feed_dict=feed_dict_to_use)
                if epoch == FLAGS.iterations - 1:
                    dsn_fuse_image = np.where(_dsn_fuse[0] > FLAGS.output_threshold, [255], [0])
                    dsn_fuse_image_path = os.path.join('./test_image', 'fine_tuning_output_img.png')
                    util.save_img(dsn_fuse_image_path, dsn_fuse_image.reshape([256, 256]))
                    saver.save(sess, hed_ckpt_file_path, global_step=0)

        print('done')
