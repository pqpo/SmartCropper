#!/usr/bin/python
# coding=utf8

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

from util import *
from hed_net import *
import os

from tensorflow import flags

flags.DEFINE_string('input_img', 'test_image/test2.png',
                    'Image path to run hed, must be jpg image.')
flags.DEFINE_string('checkpoint_dir', './finetuning_model',
                    'Checkpoint directory.')
flags.DEFINE_string('output_img', 'test_image/test2_o.jpg',
                    'Output image path.')
flags.DEFINE_float('output_threshold', 0.0, 'output threshold, default: 0.0')

FLAGS = flags.FLAGS

if not os.path.exists(FLAGS.input_img):
    print('--input_img invalid')
    exit()

if FLAGS.output_img == '':
    print('--output_img invalid')
    exit()

if __name__ == "__main__":
    image_path_placeholder = tf.placeholder(tf.string)

    feed_dict_to_use = {image_path_placeholder: FLAGS.input_img}

    image_tensor = tf.read_file(image_path_placeholder)
    image_tensor = tf.image.decode_jpeg(image_tensor, channels=3)
    origin_tensor = tf.image.resize_images(image_tensor, [const.image_height, const.image_width])
    image_float = tf.to_float(origin_tensor)
    image_float = image_float / 255.0
    image_float = tf.expand_dims(image_float, axis=0)

    dsn_fuse, dsn1, dsn2, dsn3, dsn4, dsn5 = mobilenet_v2_style_hed(image_float, False)
    # dsn_fuse = tf.reshape(dsn_fuse, shape=(const.image_height, const.image_width))

    global_init = tf.global_variables_initializer()

    # Saver
    hed_weights = tf.get_collection(tf.GraphKeys.GLOBAL_VARIABLES, scope='hed')
    saver = tf.train.Saver(hed_weights)

    with tf.Session() as sess:
        sess.run(global_init)

        latest_ck_file = tf.train.latest_checkpoint(FLAGS.checkpoint_dir)
        if latest_ck_file:
            print('restore from latest checkpoint file : {}'.format(latest_ck_file))
            saver.restore(sess, latest_ck_file)
        else:
            print('no checkpoint file to restore, exit()')
            exit()

        _dsn_fuse = sess.run(dsn_fuse, feed_dict=feed_dict_to_use)

        dsn_fuse_image = np.where(_dsn_fuse[0] > FLAGS.output_threshold, [255], [0])
        save_img(FLAGS.output_img, dsn_fuse_image.reshape([256, 256]))
        print('done! output image: {}'.format(FLAGS.output_img))
