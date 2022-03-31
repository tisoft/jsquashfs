#!/bin/bash

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

# Run unmounted tests
for comp in ${ucomp[*]}; do
  echo "Building squashfs image using ${comp} compression."
  if [ "${comp}" == gzip ]; then
    mksquashfs ${datadir} ${testdir}/sq.img.${comp} || (echo "mksquashfs failed for ${comp} compression."; continue)
  else
    mksquashfs ${datadir} ${testdir}/sq.img.${comp} -comp ${comp} || (echo "mksquashfs failed for ${comp} compression."; continue)
  fi
done
