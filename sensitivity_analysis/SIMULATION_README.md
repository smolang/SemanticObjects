# SMOL Geological Simulation - Sensitivity Analysis Guide

This guide provides complete instructions for performing sensitivity analysis on SMOL geological simulations using the enhanced parameterization system.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Parameter Configuration](#parameter-configuration)
3. [Running Individual Simulations](#running-individual-simulations)
4. [Running Batch Sensitivity Analysis](#running-batch-sensitivity-analysis)
5. [Understanding Outputs](#understanding-outputs)
6. [Data Analysis](#data-analysis) *(Coming Soon)*
7. [Visualization](#visualization) *(Coming Soon)*
8. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Environment Setup
```bash
# Navigate to sensitivity analysis directory
cd sensitivity_analysis

# Activate virtual environment (if using venv)
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install required packages
pip install -r requirements.txt

# Ensure SMOL is built
cd ../
./gradlew build
cd sensitivity_analysis
```

### Verify Setup
```bash
# Test that SMOL JAR exists and is accessible
ls -la ../build/libs/smol.jar

# Test parameter modification system
cd scripts
python test_template_parameterisation.py
```

---

## Parameter Configuration

### Understanding Parameters

The system analyzes **19 geological parameters** defined in `config/parameters.json`:

#### **Geological Sizes** (meters)
- `SHALE_SIZE`: Thickness of initial shale source rock unit
- `SANDSTONE_SIZE`: Thickness of Div sandstone units  
- `TOR_SIZE`: Thickness of Tor chalk units
- `EKOFISK_SIZE`: Thickness of Ekofisk chalk units
- `CAP_SIZE`: Thickness of cap/seal shale units
- `AB1_SIZE`: Thickness of AB1 sandstone units
- `AB2_SIZE`: Thickness of AB2 sandstone units

#### **Temperature Model**
- `BASE_TEMPERATURE`: Base temperature for geothermal gradient (°C)
- `TEMP_FACTOR`: Temperature increase factor with depth (°C/km)

#### **Simulation Parameters**
- `HYDROCARBON_INCREMENT`: Rate of hydrocarbon generation/migration
- `START_PAST`: Starting time in geological past (million years)
- `CHECK_START`: Time to begin maturation checks (million years)  
- `DEPOSITION_DURATION`: Time span for each deposition event (million years)

#### **Layer Counts**
- `DIV_LAYERS`: Number of Div sandstone layers
- `TOR_LAYERS`: Number of Tor chalk layers
- `EKOFISK_LAYERS`: Number of Ekofisk chalk layers
- `CAP_LAYERS`: Number of cap rock layers
- `AB1_LAYERS`: Number of AB1 sandstone layers
- `AB2_LAYERS`: Number of AB2 sandstone layers

### Configuring Parameter Ranges

Edit `config/parameters.json` to customize analysis ranges:

```json
{
  "parameters": {
    "SHALE_SIZE": {
      "min": 14.0,           // Minimum value to test
      "max": 26.0,           // Maximum value to test  
      "base": 20.0,          // Baseline/default value
      "type": "float",
      "category": "geological_size",
      "units": "meters",
      "description": "Thickness of initial shale source rock unit"
    }
  }
}
```

**Key Fields:**
- `min`/`max`: Define the analysis range for parameter sweeps
- `base`: Baseline value used for comparison and default simulations
- `type`: "float" or "int" for proper value formatting
- `category`: Groups related parameters for analysis

---

## Running Individual Simulations

### 1. Generate Baseline Simulation

Create baseline configuration with default parameter values:

```bash
cd scripts

# Generate baseline SMOL file
python parameter_modifier_template.py -t ../templates/simulate_onto_template.smol --baseline

# This creates: ../data/input/simulate_onto_baseline_[timestamp].smol
```

### 2. Run Baseline Simulation

```bash
# Run the baseline simulation
python simulation_runner.py ../data/input/simulate_onto_baseline_[timestamp].smol

# With verbose output for debugging
python simulation_runner.py ../data/input/simulate_onto_baseline_[timestamp].smol --verbose
```

**Output Location:** `data/output/simulate_onto_baseline_output_[timestamp].json`

### 3. Generate Parameter Variations

Create simulations with specific parameter changes:

```bash
# Single parameter change
python parameter_modifier_template.py -t ../templates/simulate_onto_template.smol SHALE_SIZE=25.0

# Multiple parameter changes  
python parameter_modifier_template.py -t ../templates/simulate_onto_template.smol SHALE_SIZE=25.0 TEMP_FACTOR=35.0

# Generated files appear in: ../data/input/
```

### 4. Run Parameter Variation

```bash
# Run the generated parameter variation
python simulation_runner.py ../data/input/simulate_onto_SHALE_SIZE_25.0_[hash].smol
```

---

## Running Batch Sensitivity Analysis

### 1. Single Parameter Analysis

Test one parameter across its defined range:

```bash
# Analyze SHALE_SIZE with 5 samples across its range (14.0-26.0)
python batch_runner.py ../templates/simulate_onto_template.smol --parameters SHALE_SIZE --samples 5

# Analyze TEMP_FACTOR with 10 samples  
python batch_runner.py ../templates/simulate_onto_template.smol --parameters TEMP_FACTOR --samples 10 --parallel
```

### 2. Multiple Parameter Analysis

Test several parameters simultaneously:

```bash
# Test geological sizes only
python batch_runner.py ../templates/simulate_onto_template.smol --parameters SHALE_SIZE SANDSTONE_SIZE TOR_SIZE --samples 5

# Test temperature model parameters
python batch_runner.py ../templates/simulate_onto_template.smol --parameters BASE_TEMPERATURE TEMP_FACTOR --samples 8 --parallel
```

### 3. Full Sensitivity Analysis

Test all 19 parameters (comprehensive analysis):

```bash
# Complete analysis with default settings
python batch_runner.py ../templates/simulate_onto_template.smol --samples 5 --parallel

# High-resolution analysis (warning: ~100+ simulations)
python batch_runner.py ../templates/simulate_onto_template.smol --samples 10 --parallel --workers 4
```

### Command Options

- `--samples N`: Number of test points per parameter (default: 5)
- `--parameters LIST`: Specific parameters to test (default: all)
- `--parallel`: Run simulations in parallel for speed
- `--workers N`: Maximum parallel processes (default: CPU cores - 1)

### Understanding Parameter Sampling

When you specify `--samples N`, the system creates N evenly-spaced test points across each parameter's range:

#### **Example: SHALE_SIZE with --samples 5**
- **Range**: 14.0 to 26.0 (from parameters.json)
- **Sample points**: [14.0, 17.0, 20.0, 23.0, 26.0]
- **Baseline filtering**: Removes 20.0 (within 1% of base value)
- **Final tests**: [14.0, 17.0, 23.0, 26.0] + baseline run

#### **Example: DIV_LAYERS with --samples 5** (integer parameter)
- **Range**: 18 to 43
- **Sample points**: [18.0, 24.25, 30.5, 36.75, 43.0]
- **Integer rounding**: [18, 24, 31, 37, 43]
- **Duplicate removal**: [18, 24, 31, 37, 43] (no duplicates)
- **Baseline filtering**: Removes 31 (closest to base=31)
- **Final tests**: [18, 24, 37, 43] + baseline run

#### **Sampling Method:**
1. **Linear spacing**: Uses `numpy.linspace(min, max, samples)`
2. **Integer handling**: Rounds to nearest integer, removes duplicates
3. **Baseline exclusion**: Skips values within 1% of baseline to avoid redundancy
4. **Baseline inclusion**: Always runs baseline as separate simulation

#### **Total Simulations Calculator:**
```bash
# Single parameter: --samples 5
# Creates: 1 baseline + ~4 parameter tests = ~5 simulations

# Multiple parameters: --parameters SHALE_SIZE TEMP_FACTOR --samples 5  
# Creates: 1 baseline + ~4 SHALE tests + ~4 TEMP tests = ~9 simulations

# All parameters: --samples 5 (19 parameters)
# Creates: 1 baseline + ~4×19 parameter tests = ~77 simulations
```

---

## Understanding Outputs

### Simulation Results Structure

Each simulation produces a JSON file in `data/output/` containing:

```json
{
  "simulation": {
    "smol_file": "path/to/input/file.smol",
    "timestamp": "2025-07-22T10:30:45.123456",
    "execution_time": 145.67
  },
  "metrics": {
    "trap_count": 120,           // Number of hydrocarbon traps
    "leak_count": 0,             // Number of hydrocarbon leaks  
    "maturation_events": 19,     // Shale maturation occurrences
    "migration_events": 19,      // Hydrocarbon migration events
    "final_simulation_time": 2.0,
    "status": "completed",
    "warnings": [],
    "errors": []
  },
  "raw_output": {
    "stdout": "Complete simulation output...",
    "stderr": ""
  }
}
```

### Batch Analysis Results

Batch runs produce comprehensive CSV and JSON files in `data/output/`:

**CSV Format:** `sensitivity_results_[timestamp].csv`
- Each row = one simulation run
- Columns include parameter values and all metrics
- Easy to import into Excel/R/Python for analysis

**JSON Format:** `sensitivity_results_[timestamp].json`
- Complete metadata and results
- Preserves all simulation details
- Machine-readable for automated analysis

### Key Metrics for Analysis

1. **Trap Count**: Primary outcome - hydrocarbon accumulation
2. **Leak Count**: Hydrocarbon loss - should generally be 0
3. **Maturation Events**: Source rock activation frequency
4. **Migration Events**: Hydrocarbon movement frequency
5. **Execution Time**: Computational performance indicator

---

## Data Analysis

*Section reserved for analysis tools and methods - Coming Soon*

### Statistical Analysis Tools
- Parameter correlation analysis
- Sensitivity index calculations  
- Response surface modeling
- Uncertainty quantification

### Analysis Scripts
- `results_analyzer.py` - Statistical analysis of simulation results
- `correlation_analysis.py` - Parameter interaction effects
- `uncertainty_analysis.py` - Model uncertainty assessment

---

## Visualization  

*Section reserved for plotting and visualization tools - Coming Soon*

### Visualization Tools
- Sensitivity index plots
- Parameter correlation heatmaps
- Response surface visualization
- Uncertainty distribution plots

### Plotting Scripts
- `sensitivity_plots.py` - Standard sensitivity analysis charts
- `correlation_plots.py` - Parameter interaction visualization
- `response_plots.py` - Model response visualization

---

## Troubleshooting

### Common Issues

**1. SMOL JAR Not Found**
```bash
# Error: Could not find smol.jar
# Solution: Build SMOL first
cd ../
./gradlew build
cd sensitivity_analysis
```

**2. Template File Missing**
```bash
# Error: Template file not found
# Solution: Verify template exists
ls -la templates/simulate_onto_template.smol
```

**3. Parameter Validation Errors**
```bash
# Error: Parameter X value Y out of range [min, max]
# Solution: Check config/parameters.json ranges
```

**4. Simulation Timeout**
```bash
# Error: Simulation timeout after 1800 seconds
# Solution: Increase timeout
python simulation_runner.py file.smol --timeout 3600
```

### Performance Tips

1. **Use Parallel Processing**: Always add `--parallel` for batch runs
2. **Optimize Sample Size**: Start with 5 samples, increase if needed
3. **Monitor Disk Space**: Each simulation ~10KB output
4. **Check Memory Usage**: Large batch runs use significant RAM

### Getting Help

1. **Test System**: Run `python test_template_parameterisation.py`
2. **Check Logs**: Review generated JSON files for error details
3. **Validate Config**: Ensure parameters.json has correct syntax
4. **Check Examples**: See existing output files for expected format

---

## File Structure Reference

```
sensitivity_analysis/
├── config/
│   ├── parameters.json       # Parameter definitions and ranges
│   └── baseline.json         # Baseline parameter values
├── templates/
│   └── simulate_onto_template.smol  # Template with {{PARAM}} placeholders
├── scripts/
│   ├── parameter_modifier_template.py   # Generate parameter variations
│   ├── simulation_runner.py             # Run individual simulations  
│   ├── batch_runner.py                  # Run batch sensitivity analysis
│   └── test_template_parameterisation.py  # Test system
├── data/
│   ├── input/               # Generated SMOL files
│   └── output/              # Simulation results (JSON/CSV)
├── requirements.txt         # Python dependencies
└── SIMULATION_README.md     # This guide
```