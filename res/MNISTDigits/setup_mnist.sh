#!/bin/bash

echo "downloading train-images-idx3-ubyte.gz..."
wget -N http://yann.lecun.com/exdb/mnist/train-images-idx3-ubyte.gz

echo "downloading train-labels-idx1-ubyte.gz..."
wget -N http://yann.lecun.com/exdb/mnist/train-labels-idx1-ubyte.gz

echo "downloading t10k-images-idx3-ubyte.gz..."
wget -N http://yann.lecun.com/exdb/mnist/t10k-images-idx3-ubyte.gz

echo "downloading t10k-labels-idx1-ubyte.gz..."
wget -N http://yann.lecun.com/exdb/mnist/t10k-labels-idx1-ubyte.gz

echo "gunzipping train-images-idx3-ubyte.gz..."
gunzip -f train-images-idx3-ubyte.gz

echo "gunzipping train-labels-idx1-ubyte.gz..."
gunzip -f train-labels-idx1-ubyte.gz

echo "gunzipping t10k-images-idx3-ubyte.gz..."
gunzip -f t10k-images-idx3-ubyte.gz

echo "gunzipping t10k-labels-idx1-ubyte.gz..."
gunzip -f t10k-labels-idx1-ubyte.gz
