# SMOL File Parameterization Changes

## Overview
This document summarizes the changes made to create `simulate_onto_parameterized.smol` from the original `simulate_onto.smol` to support sensitivity analysis.

## Changes Made

### 1. Added Parameter Definitions (Lines 6-24)
Added parameter definitions at the top of the file:

```smol
SHALE_SIZE = 20.0              // Thickness of initial shale source rock unit (meters)
SANDSTONE_SIZE = 26.5          // Thickness of Div sandstone units (meters)
TOR_SIZE = 67.5                // Thickness of Tor chalk units (meters) 
EKOFISK_SIZE = 99.0            // Thickness of Ekofisk chalk units (meters)
CAP_SIZE = 30.0                // Thickness of cap/seal shale units (meters)
AB1_SIZE = 186.0               // Thickness of AB1 sandstone units (meters)
AB2_SIZE = 98.05               // Thickness of AB2 sandstone units (meters)
BASE_TEMPERATURE = 2.5         // Base temperature for geothermal gradient (celsius)
TEMP_FACTOR = 30.0             // Temperature increase factor with depth (celsius/km)
HYDROCARBON_INCREMENT = 100.0  // Rate of hydrocarbon generation and migration
START_PAST = 136.0             // Starting time in geological past (million years)
CHECK_START = -66.0            // Time to begin checking for maturation triggers (million years)
DEPOSITION_DURATION = 2.0      // Time span for each deposition event (million years)
DIV_LAYERS = 31                // Number of Div sandstone layers
TOR_LAYERS = 5                 // Number of Tor chalk layers
EKOFISK_LAYERS = 1             // Number of Ekofisk chalk layers
CAP_LAYERS = 1                 // Number of cap rock (seal) layers
AB1_LAYERS = 5                 // Number of AB1 sandstone layers
AB2_LAYERS = 26                // Number of AB2 sandstone layers
```

### 2. Parameterized Temperature Calculation (Line 214)
**Before:**
```smol
this.temperature = 2.5 + ((under/1000) * 30);
```

**After:**
```smol
this.temperature = BASE_TEMPERATURE + ((under/1000) * TEMP_FACTOR);
```

### 3. Parameterized Unit Constructors

#### Initial Shale Unit (Line 381)
**Before:**
```smol
ShaleUnit mandal = new ShaleUnit(null, null, null, null, null, null, 20.0, 1, 0.0, True, 0);
```

**After:**
```smol
ShaleUnit mandal = new ShaleUnit(null, null, null, null, null, null, SHALE_SIZE, 1, 0.0, True, 0);
```

#### Div Sandstone Units (Lines 385-386)
**Before:**
```smol
SandstoneUnit div = new SandstoneUnit(null, null, null, null, null, null, 26.5, 2, null);
DepositionGenerator dep = new DepositionGenerator(div, 2.0, 31);
```

**After:**
```smol
SandstoneUnit div = new SandstoneUnit(null, null, null, null, null, null, SANDSTONE_SIZE, 2, null);
DepositionGenerator dep = new DepositionGenerator(div, DEPOSITION_DURATION, DIV_LAYERS);
```

#### Tor Chalk Units (Lines 393-394)
**Before:**
```smol
SandstoneUnit tor = new ChalkUnit(null, null, null, null, null, null, 67.5, 2, null);
DepositionGenerator depTor = new DepositionGenerator(tor, 2.0, 5);
```

**After:**
```smol
SandstoneUnit tor = new ChalkUnit(null, null, null, null, null, null, TOR_SIZE, 2, null);
DepositionGenerator depTor = new DepositionGenerator(tor, DEPOSITION_DURATION, TOR_LAYERS);
```

#### Ekofisk Chalk Units (Lines 397-398)
**Before:**
```smol
SandstoneUnit ekofisk = new ChalkUnit(null, null, null, null, null, null, 99.0, 2, null);
DepositionGenerator depEko = new DepositionGenerator(ekofisk, 2.0, 1);
```

**After:**
```smol
SandstoneUnit ekofisk = new ChalkUnit(null, null, null, null, null, null, EKOFISK_SIZE, 2, null);
DepositionGenerator depEko = new DepositionGenerator(ekofisk, DEPOSITION_DURATION, EKOFISK_LAYERS);
```

#### Cap Shale Units (Lines 401-402)
**Before:**
```smol
ShaleUnit cap = new ShaleUnit(null, null, null, null, null, null, 30.0, 1, 0.0, False, 0);
DepositionGenerator depCap = new DepositionGenerator(cap, 2.0, 1);
```

**After:**
```smol
ShaleUnit cap = new ShaleUnit(null, null, null, null, null, null, CAP_SIZE, 1, 0.0, False, 0);
DepositionGenerator depCap = new DepositionGenerator(cap, DEPOSITION_DURATION, CAP_LAYERS);
```

#### AB1 Sandstone Units (Lines 405-406)
**Before:**
```smol
SandstoneUnit ab1 = new SandstoneUnit(null, null, null, null, null, null, 186.0, 2, null);
DepositionGenerator depAb1 = new DepositionGenerator(ab1, 2.0, 5);
```

**After:**
```smol
SandstoneUnit ab1 = new SandstoneUnit(null, null, null, null, null, null, AB1_SIZE, 2, null);
DepositionGenerator depAb1 = new DepositionGenerator(ab1, DEPOSITION_DURATION, AB1_LAYERS);
```

#### AB2 Sandstone Units (Lines 409-410)
**Before:**
```smol
SandstoneUnit ab2 = new SandstoneUnit(null, null, null, null, null, null, 98.05, 2, null);
DepositionGenerator depAb2 = new DepositionGenerator(ab2, 2.0, 26);
```

**After:**
```smol
SandstoneUnit ab2 = new SandstoneUnit(null, null, null, null, null, null, AB2_SIZE, 2, null);
DepositionGenerator depAb2 = new DepositionGenerator(ab2, DEPOSITION_DURATION, AB2_LAYERS);
```

### 4. Parameterized Simulation Call (Line 417)
**Before:**
```smol
driver.sim(dl, 136.0, mandal, (-66.0));
```

**After:**
```smol
driver.sim(dl, START_PAST, mandal, CHECK_START);
```

## Parameter Mapping

| Parameter | Original Value | Description |
|-----------|----------------|-------------|
| SHALE_SIZE | 20.0 | Initial shale unit thickness |
| SANDSTONE_SIZE | 26.5 | Div sandstone unit thickness |
| TOR_SIZE | 67.5 | Tor chalk unit thickness |
| EKOFISK_SIZE | 99.0 | Ekofisk chalk unit thickness |
| CAP_SIZE | 30.0 | Cap/seal shale unit thickness |
| AB1_SIZE | 186.0 | AB1 sandstone unit thickness |
| AB2_SIZE | 98.05 | AB2 sandstone unit thickness |
| BASE_TEMPERATURE | 2.5 | Base temperature for geothermal gradient |
| TEMP_FACTOR | 30.0 | Temperature increase factor with depth |
| START_PAST | 136.0 | Simulation start time (million years ago) |
| CHECK_START | -66.0 | Maturation trigger check time |
| DEPOSITION_DURATION | 2.0 | Time span for each deposition event |
| DIV_LAYERS | 31 | Number of Div sandstone layers |
| TOR_LAYERS | 5 | Number of Tor chalk layers |
| EKOFISK_LAYERS | 1 | Number of Ekofisk chalk layers |
| CAP_LAYERS | 1 | Number of cap rock layers |
| AB1_LAYERS | 5 | Number of AB1 sandstone layers |
| AB2_LAYERS | 26 | Number of AB2 sandstone layers |

## Validation

To validate these changes:

1. **Syntax Check**: Ensure the parameterized file compiles successfully
2. **Baseline Comparison**: Run both files and compare outputs to ensure identical results
3. **Parameter Modification**: Test changing parameter values to confirm sensitivity analysis works

## Next Steps

1. Test the parameterized file with the SMOL interpreter
2. Update the sensitivity analysis configuration to use the new parameter names
3. Run a test sensitivity analysis to verify functionality
4. If validation passes, consider replacing the original file