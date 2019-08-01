#!/usr/bin/python
#coding=utf8

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

from hed_net import *

from tensorflow import flags
flags.DEFINE_string('checkpoint_dir', './checkpoint',
                    'Checkpoint directory.')
flags.DEFINE_string('output_file', './hed_lite_model_quantize.tflite',
                    'Output file')

FLAGS = flags.FLAGS

if __name__ == "__main__":

    image_input = tf.placeholder(tf.float32, shape=(const.image_height, const.image_width, 3), name='hed_input')
    image_float = image_input / 255.0
    image_float = tf.expand_dims(image_float, axis=0)

    print('###1 input shape is: {}, name is: {}'.format(image_input.get_shape(), image_input.name))
    dsn_fuse, dsn1, dsn2, dsn3, dsn4, dsn5 = mobilenet_v2_style_hed(image_float, False)
    img_output = tf.reshape(dsn_fuse, shape=(const.image_height, const.image_width), name="img_output")
    print('###2 output shape is: {}, name is: {}'.format(img_output.get_shape(), img_output.name))

    # Saver
    hed_weights = tf.get_collection(tf.GraphKeys.GLOBAL_VARIABLES, scope='hed')
    saver = tf.train.Saver(hed_weights)

    global_init = tf.global_variables_initializer()

    with tf.Session() as sess:
        sess.run(global_init)

        latest_ck_file = tf.train.latest_checkpoint(FLAGS.checkpoint_dir)
        if latest_ck_file:
            print('restore from latest checkpoint file : {}'.format(latest_ck_file))
            saver.restore(sess, latest_ck_file)
        else:
            print('no checkpoint file to restore, exit()')
            exit()

        converter = tf.contrib.lite.TFLiteConverter.from_session(sess, [image_input], [img_output])
        converter.post_training_quantize = True
        tflite_model = converter.convert()
        open(FLAGS.output_file, 'wb').write(tflite_model)
        print('finished')

