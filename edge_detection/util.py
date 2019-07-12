#!/usr/bin/python
#coding=utf8

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import scipy.misc
import numpy as np


def save_img(out_path, img):
    img = np.clip(img, 0, 255).astype(np.uint8)
    scipy.misc.imsave(out_path, img)


def load_sample_from_csv(csv_file):
    images = []
    annotations = []
    with open(csv_file, 'r') as f:
        for line in f.readlines():
            a_list = line.split(', ')
            image_l = a_list[0]
            annotation_l = a_list[1].replace('\n', '')
            images.append(image_l)
            annotations.append(annotation_l)
    return images, annotations


