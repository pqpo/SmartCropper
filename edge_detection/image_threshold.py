#!/usr/bin/python
#coding=utf8

# 图像二值化工具

from tensorflow import flags
from skimage import color
from skimage import io
from skimage import transform
import numpy as np
from skimage import filters
import os

flags.DEFINE_string('input_img', '',
                    'input image.')
flags.DEFINE_string('output_img', '',
                    'output image.')

FLAGS = flags.FLAGS

if not os.path.exists(FLAGS.input_img):
    print('--input_img invalid')
    exit()

if FLAGS.output_img == '':
    print('--output_img invalid')
    exit()


def threshold(ann_path, save_path):
    img = io.imread(ann_path, mode='RGB')
    img = color.rgb2gray(img)
    thresh = filters.threshold_otsu(img)
    img = np.where(img > thresh, [255], [0])
    img = np.clip(img, 0, 255).astype(np.uint8)
    io.imsave(save_path, img)


if __name__ == '__main__':
    threshold(FLAGS.input_img, FLAGS.output_img)
