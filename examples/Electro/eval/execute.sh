#!/bin/bash
sed -i'.bak1' -e '/^\(RECORD\|-t\(h\|o\)\)/!d' output
sed -i'.bak2' -r 's/^.{7}//' output
awk 'NR > 1 { print $0 - prev } { prev = $0 }' < output >timeseries.dat
