#!/bin/bash

cp TTT_class_schema.smol TTT_class_$1.smol
cp TTT_rule_schema.smol TTT_rule_$1.smol
sed -i 's/VAR/'$1'/g' TTT_class_$1.smol
sed -i 's/VAR/'$1'/g' TTT_rule_$1.smol
