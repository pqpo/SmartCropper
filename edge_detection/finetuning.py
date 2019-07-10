#!/usr/bin/python
# coding=utf8

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

from hed_net import *
import numpy as np
import os
import util
from generate_batch_data import generate_batch_data

from tensorflow import flags

flags.DEFINE_string('finetuning_dir', './finetuning_model',
                    'finetuning directory.')
flags.DEFINE_string('checkpoint_dir', './checkpoint',
                    'Checkpoint directory.')
flags.DEFINE_string('image', 'test_image/IMG_20190704_143127.jpg', 'fine tuning image')
flags.DEFINE_string('annotation', 'test_image/annotation_IMG_20190704_143127.png_threshold.jpg', 'fine tuning annotation')
flags.DEFINE_string('csv', '', 'fine tuning csv')
flags.DEFINE_integer('batch_size', 4, 'batch size')
flags.DEFINE_float('lr', 0.0005, 'learning rate')
flags.DEFINE_integer('iterations', 15,
                     'Number of iterations')
flags.DEFINE_float('output_threshold', 0.0, 'output threshold')

FLAGS = flags.FLAGS

hed_ckpt_file_path = os.path.join(FLAGS.checkpoint_dir, 'hed.ckpt')

train_layer = ['block0_1', 'block1_0', 'block2_1', 'block3_2', 'block4_3', 'block5_2']

if not ((os.path.exists(FLAGS.image)) and (os.path.exists(FLAGS.annotation)) or (os.path.exists(FLAGS.csv))):
    print('please add input, --img, --annotation or --csv')
    exit()

images = []
annotations = []

batch_size = FLAGS.batch_size

if os.path.exists(FLAGS.image) and os.path.exists(FLAGS.annotation):
    images.append(FLAGS.image)
    annotations.append(FLAGS.annotation)

if os.path.exists(FLAGS.csv):
    csv_img, csv_ann = util.load_sample_from_csv(FLAGS.csv)
    if csv_img is not None and len(csv_img) > 0:
        images.extend(csv_img)
        annotations.extend(csv_ann)

if len(images) == 0:
    print('Samples is empty, exit()')
    exit()

print('fine tuning images size: {}'.format(len(images)))
print('fine tuning annotations size: {}'.format(len(annotations)))

assert len(images) == len(annotations)

image_tensor, annotation_tensor = generate_batch_data(images, annotations, batch_size=batch_size)

if __name__ == "__main__":

    is_training = tf.placeholder(tf.bool)

    dsn_fuse, dsn1, dsn2, dsn3, dsn4, dsn5 = mobilenet_v2_style_hed(image_tensor, is_training)

    hed_weights = tf.get_collection(tf.GraphKeys.GLOBAL_VARIABLES, scope='hed')

    cost = class_balanced_sigmoid_cross_entropy(dsn_fuse, annotation_tensor)
    # cost = class_balanced_sigmoid_cross_entropy(dsn_fuse, annotation_tensor) \
    #     + class_balanced_sigmoid_cross_entropy(dsn1, annotation_tensor)\
    #     + class_balanced_sigmoid_cross_entropy(dsn2, annotation_tensor)\
    #     + class_balanced_sigmoid_cross_entropy(dsn3, annotation_tensor)\
    #     + class_balanced_sigmoid_cross_entropy(dsn4, annotation_tensor)\
    #     + class_balanced_sigmoid_cross_entropy(dsn5, annotation_tensor)

    var_list = [v for v in tf.trainable_variables() if v.name.split('/')[2] in train_layer]
    gradients = tf.gradients(cost, var_list)
    gradients = list(zip(gradients, var_list))

    with tf.control_dependencies(tf.get_collection(tf.GraphKeys.UPDATE_OPS)):
        train_step = tf.train.AdamOptimizer(learning_rate=FLAGS.lr).apply_gradients(gradients)
        # train_step = tf.train.AdamOptimizer(learning_rate=FLAGS.lr).minimize(cost)

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
                for step in range(len(images)):
                    feed_dict_to_use = {is_training: True}
                    _dsn_fuse, _ = sess.run([dsn_fuse, train_step], feed_dict=feed_dict_to_use)
                    if epoch == FLAGS.iterations - 1:
                        feed_dict_to_use[is_training] = False
                        dsn_fuse_evaluate = sess.run(dsn_fuse, feed_dict=feed_dict_to_use)
                        dsn_fuse_image = np.where(dsn_fuse_evaluate[0] > FLAGS.output_threshold, [255], [0])
                        dsn_fuse_image_path = os.path.join('./test_image', 'fine_tuning_output_img.png')
                        util.save_img(dsn_fuse_image_path, dsn_fuse_image.reshape([256, 256]))
                        saver.save(sess, hed_ckpt_file_path, global_step=0)

        print("Train Finished!")
