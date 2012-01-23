#!/bin/sh
input=$1
output=$2
exiftool -b -ThumbnailImage "$input" > "$output"
