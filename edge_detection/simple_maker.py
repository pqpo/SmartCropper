from tensorflow import flags
from skimage import color
from skimage import io
from skimage import transform
import numpy as np
from skimage import filters
import os
import math

# todo

if __name__ == '__main__':
    background = io.imread('make_test/background.jpg')
    background_shape = background.shape
    background_width = background_shape[0]
    background_height = background_shape[1]

    rect = io.imread('make_test/rect.png')
    rect_shape = rect.shape
    rect_width = rect_shape[0]
    rect_height = rect_shape[1]

    src = np.array([[0, 0], [400, 400], [800, 400], [400, 0]])
    dst = np.array([[0, 0], [0, rect_height], [rect_width, rect_height], [rect_width, 0]])

    tform3 = transform.ProjectiveTransform()
    tform3.estimate(src, dst)

    warped = transform.warp(rect, tform3, output_shape=(background_width, background_height))

    io.imsave('make_test/rect_tr.png', warped)