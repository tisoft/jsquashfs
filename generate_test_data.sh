#!/bin/bash

set -euo pipefail
trap "exit" INT

# Based on https://fedoraproject.org/wiki/QA:Testcase_squashfs-tools_compression

# Define block sizes
blocks=(4K 1M)

# Define fill files
fill=(/dev/zero /dev/urandom)

# Define number of iterations
iter=5

# Define fragment sizes
frags=(0 1 2047 4095)

# Define test directory
testdir=test_data

# Define data directory
datadir=${testdir}/data

# set compression types to test
ucomp=(gzip lzo lzma xz lz4 zstd)

# set options to test
uopts=("-no-fragments" "-always-use-fragments" "-noI -noD -noF -noX" "-comp lz4 -Xhc" "-comp lzo -Xalgorithm lzo1x_1")

# Check if test directory exists and make if not
[ -d ${testdir} ] || mkdir ${testdir}
[ -d ${testdir} ] || (echo "Unable to make '${testdir}', aborting (failed)."; exit 1)

# Check if data directory exists and make if not
if [ -d ${datadir} ]; then
  echo "Using existing data directory."
else
  echo "Building data directory."
  mkdir ${datadir}
  [ -d ${datadir} ] || (echo "Unable to make '${datadir}', aborting (failed)."; exit 1)
  for size in ${frags[*]}; do
    for file in ${fill[*]}; do
      dd if=${file} of=${datadir}/frag-`basename ${file}`-${size} bs=1 count=${size} > /dev/null 2>&1
    done
  done
  for size in ${blocks[*]}; do
    for ((count=1;${count}<=${iter};count++)); do
      for file in ${fill[*]}; do
        dd if=${file} of=${datadir}/file-`basename ${file}`-${size}-${count} bs=${size} count=${count} > /dev/null 2>&1
      done
    done
  done
  for size1 in ${frags[*]}; do
    for file1 in ${fill[*]}; do
      for size2 in ${blocks[*]}; do
        for ((count=1;${count}<=${iter};count++)); do
          for file2 in ${fill[*]}; do
            cat ${datadir}/file-`basename ${file2}`-${size2}-${count} ${datadir}/frag-`basename ${file1}`-${size1} > ${datadir}/combined-`basename ${file2}`-${size2}-${count}-`basename ${file1}`-${size1}
          done
        done
      done
    done
  done
fi

# create file with xattrs
touch ${datadir}/file-xattr
xattr -w user.test test ${datadir}/file-xattr

# create directory with xattrs
mkdir ${datadir}/dir-xattr
xattr -w user.test test ${datadir}/dir-xattr

# create images with default options for all compression formats
for comp in ${ucomp[*]}; do
  echo "Building squashfs image using ${comp} compression."
  mksquashfs ${datadir} ${testdir}/sq.img.${comp} -comp ${comp} || (echo "mksquashfs failed for ${comp} compression."; continue)
done

# create images with default compression for all options
for opts in "${uopts[@]}"; do
  echo "Building squashfs image using ${opts} option."
  mksquashfs ${datadir} ${testdir}/sq.img.${opts// /_} ${opts} || (echo "mksquashfs failed for ${opts} option."; continue)
done