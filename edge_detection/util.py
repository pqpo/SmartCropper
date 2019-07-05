#!/usr/bin/python
#coding=utf8

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import scipy.misc
import numpy as np
import shutil
from skimage import color


def save_img(out_path, img):
    img = np.clip(img, 0, 255).astype(np.uint8)
    scipy.misc.imsave(out_path, img)


def load_sample_from_csv(csv_file):
    samples = []
    with open(csv_file, 'r') as f:
        for line in f.readlines():
            a_list = line.split(', ')
            image_l = a_list[0]
            annotation_l = a_list[1].replace('\n', '')
            samples.append((image_l, annotation_l))
    return samples


def threshold(ann_path, hold):
    # shutil.copy(ann_path, '{}.backup'.format(ann_path))
    img = scipy.misc.imread(ann_path, mode='RGB')
    img = color.rgb2gray(img)
    img = np.where(img > hold, [255], [0])
    save_img(ann_path+"_threshold.jpg", img)


if __name__ == '__main__':
    ann_file = './test_image/annotation.png'
    threshold(ann_file, 0.5)

