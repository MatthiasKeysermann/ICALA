#!/bin/bash

echo "downloading att_faces.zip..."
wget -N http://www.cl.cam.ac.uk/Research/DTG/attarchive/pub/data/att_faces.zip

echo "unzipping att_faces.zip..."
unzip -o att_faces.zip

echo "converting .pgm files to .png files..."
DIRS="*"
for i in $DIRS ; do
	if [ -d "$i" ]; then		
		cd "$i"
		FILES="*.pgm"
		for j in $FILES ; do
			#echo "converting $i/$j..."
			convert "$j" "${j%.pgm}.png"
		done
		cd ".."
	fi
done

echo "removing .pgm files..."
DIRS="*"
for i in $DIRS ; do
	if [ -d "$i" ]; then		
		cd "$i"
		FILES="*.pgm"
		for j in $FILES ; do
			#echo "removing $i/$j..."
			rm "$j"
		done
		cd ".."
	fi
done
