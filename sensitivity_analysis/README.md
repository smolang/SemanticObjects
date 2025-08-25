# SMOL Sensitivity Analysis Framework

Complete sensitivity analysis toolkit for SMOL geological simulations with adaptive analysis capabilities that automatically adjust to single-parameter, few-parameter, or many-parameter scenarios.

## Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Installation](#installation)
4. [Complete Workflow](#complete-workflow)
5. [Analysis Tools](#analysis-tools)
6. [Usage Examples](#usage-examples)
7. [Understanding Results](#understanding-results)
8. [Advanced Features](#advanced-features)
9. [File Structure](#file-structure)
10. [Troubleshooting](#troubleshooting)

---

### Key Features

- **Adaptive Analysis**: Automatically adjusts analysis methods based on parameter count
- **Complete Workflow**: From simulation generation to final reports
- **Multiple Analysis Modes**:
  - Single parameter: Detailed response curves, optimal values
  - Few parameters (2-5): Interaction analysis, pairwise effects
  - Many parameters (6+): Sensitivity indices, correlation matrices, PCA
- **Rich Visualisations**: Tornado plots, response curves, correlation heatmaps
- **Professional Reports**: Automatically generated HTML reports with embedded plots

### Analysis Capabilities

| Analysis Type | Single Parameter | Several Parameters | Many Parameters |
|---------------|------------------|--------------------|------------------|
| Response curves | ✓ | ✓                  | ✓ |
| Correlation analysis | ✓ | ✓                  | ✓ |
| Optimal values | ✓ | ✓                  | ✓ |
| Interaction effects | - | ✓                  | ✓ |
| Sensitivity indices | - | -                  | ✓ |
| PCA analysis | - | ✓                  | ✓ |
| Parameter clustering | - | ✓                  | ✓ |

---

## Quick Start

### 1. Set Up Environment
```bash
cd sensitivity_analysis
source venv/bin/activate
pip install -r requirements.txt
```

### 2. Run Complete Analysis (Auto-detect mode)
```bash
cd scripts
python run_analysis.py --generate-report
```

### 3. Quick Single Parameter Analysis
```bash
cd scripts
python quick_analysis.py SHALE_SIZE
```

---

## Installation

### Prerequisites
- Python 3.8 or higher
- SMOL project built (`./gradlew build` from project root)
- Virtual environment (recommended)

### Setup Steps

1. **Navigate to sensitivity analysis directory**
   ```bash
   cd sensitivity_analysis
   ```

2. **Create virtual environment** (if not exists)
   ```bash
   python -m venv venv
   source venv/bin/activate
   ```

3. **Install dependencies**
   ```bash
   pip install -r requirements.txt
   ```

4. **Verify SMOL is built**
   ```bash
   ls -la ../build/libs/smol.jar
   ```

5. **Test the framework**
   ```bash
   cd scripts
   python test_template_parameterisation.py
   ```

---

## Complete Workflow

### Phase 1: Generate Simulations

#### 1.1 Create Baseline Simulation
```bash
cd scripts
python parameter_modifier_template.py -t ../templates/simulate_onto_template.smol --baseline
```

**Output Files Created:**
- **Location:** `../data/input/simulate_onto_baseline_YYYYMMDD_HHMMSS.smol`
- **Content:** Complete SMOL simulation file with all parameters set to baseline values from `config/baseline.json`
- **Purpose:** Reference simulation for comparison with parameter variations
- **Size:** ~1-5 KB (geological simulation script)

#### 1.2 Generate Parameter Sensitivity Data

**Single Parameter Analysis** (One-at-a-Time):
```bash
# Vary only SHALE_SIZE, keeping all others at baseline values
# Creates: 1 baseline + 4 SHALE_SIZE variations = 5 total simulations
python batch_runner.py ../templates/simulate_onto_template.smol --parameters SHALE_SIZE --samples 5
```

**Output Files Created:**
- **SMOL Input Files (5 files):** `../data/input/`
  - `simulate_onto_baseline_YYYYMMDD_HHMMSS.smol`
  - `simulate_onto_SHALE_SIZE_14.0_hash.smol`
  - `simulate_onto_SHALE_SIZE_17.0_hash.smol`
  - `simulate_onto_SHALE_SIZE_23.0_hash.smol` 
  - `simulate_onto_SHALE_SIZE_26.0_hash.smol`
- **Simulation Results (5 files):** `../data/output/`
  - `*_output_YYYYMMDD_HHMMSS.json` (contains trap_count, leak_count, execution_time)
- **Summary Files:** `../data/output/`
  - `sensitivity_results_YYYYMMDD_HHMMSS.csv` (all results in table format)
  - `sensitivity_results_YYYYMMDD_HHMMSS.json` (results + metadata)
  - `debug_log_YYYYMMDD_HHMMSS.txt` (complete verbose output for debugging)

**Multiple Individual Parameter Analysis (OAT - One-At-a-Time)**:
```bash
# Vary SHALE_SIZE and TEMP_FACTOR using OAT design
# Creates: 1 baseline + 4 SHALE_SIZE variations + 4 TEMP_FACTOR variations = 9 total simulations
# NOTE: This is NOT factorial design (which would be 5×5=25 simulations)
python batch_runner.py ../templates/simulate_onto_template.smol --parameters SHALE_SIZE TEMP_FACTOR --samples 5 --parallel
```

**Output Files Created:**
- **SMOL Input Files (9 files):** `../data/input/`
  - 1 baseline + 4 SHALE_SIZE variations + 4 TEMP_FACTOR variations
- **Simulation Results (9 files):** `../data/output/` 
  - JSON files with simulation metrics for each parameter variation
- **Summary Files:** `../data/output/`
  - `sensitivity_results_YYYYMMDD_HHMMSS.csv` (9 rows: parameter values + trap_count results)
  - `sensitivity_results_YYYYMMDD_HHMMSS.json` (includes metadata: parameters analysed, total simulations)
  - `debug_log_YYYYMMDD_HHMMSS.txt` (complete verbose output for debugging)

**Comprehensive OAT Analysis** (All parameters):
```bash
# Vary all 19 geological parameters using OAT design
# Creates: 1 baseline + ~4×19 parameter variations = ~77 total simulations
python batch_runner.py ../templates/simulate_onto_template.smol --samples 5 --parallel
```

**Output Files Created:**
- **SMOL Input Files (~77 files):** `../data/input/`
  - 1 baseline + ~76 parameter variations across all 19 parameters
- **Simulation Results (~77 files):** `../data/output/`
  - Individual JSON results for each simulation run
- **Summary Files:** `../data/output/`
  - `sensitivity_results_YYYYMMDD_HHMMSS.csv` (~77 rows with all parameter-metric combinations)
  - `sensitivity_results_YYYYMMDD_HHMMSS.json` (complete dataset + metadata)
  - `debug_log_YYYYMMDD_HHMMSS.txt` (complete verbose output for debugging)
- **Total Size:** ~1-2 MB (depending on simulation complexity)

**Key Differences:**
- **Single**: Studies one parameter's effect in isolation
- **Multiple OAT**: Studies several parameters individually (one-at-a-time, no interactions)  
- **Comprehensive OAT**: Studies all parameters individually for complete sensitivity ranking

### Phase 2: Analysis and Visualisation

#### 2.1 Quick Analysis (Single Parameter Only)
```bash
# Fast analysis for one parameter with immediate visualisation
python quick_analysis.py PARAMETER_NAME [--metric trap_count]
```

**When to use:** Need rapid insights on a single parameter's effect
**Time:** ~30 seconds per parameter

**Output Files Created:**
- **Analysis Results:** `../data/output/quick_analysis_PARAMETER_YYYYMMDD_HHMMSS.json`
  - Contains: optimal value, sensitivity metrics, correlation coefficients, curve fitting results
- **Visualisation:** `../figures/quick_analysis/PARAMETER_YYYYMMDD_HHMMSS.png`
  - Contains: response curve, rate of change, distribution plots, summary statistics
- **Console Output:** Formatted analysis summary with key findings and recommendations

#### 2.2 Comprehensive Analysis (Adaptive Multi-Mode)

**Auto-Detection Mode** (Recommended):
```bash
# Automatically detects analysis mode based on available data
python run_analysis.py
```

**When to use:** Let the framework choose the appropriate analysis method
**Detects:** Single (1 param) → Few (2-5 params) → Many (6+ params) modes

**Output Files Created:**
- **Analysis Results:** `../data/output/complete_analysis_YYYYMMDD_HHMMSS.json`
  - Contains: mode detection, core analysis, correlation results, recommendations
- **Summary Statistics:** `../data/output/analysis_summary_YYYYMMDD_HHMMSS.csv`
  - Contains: parameter statistics, metric ranges, execution times
- **Visualisations:** `../figures/` (adaptive based on detected mode)
  - Single: response curves, derivatives
  - Few: interaction plots, parameter ranking  
  - Many: tornado plots, correlation heatmaps, PCA plots

**Manual Mode Selection**:
```bash
# Force single parameter analysis (detailed response curves)
python run_analysis.py --mode single --parameters SHALE_SIZE

# Force few parameter analysis (interaction focus)
python run_analysis.py --mode few --parameters SHALE_SIZE TEMP_FACTOR

# Force comprehensive analysis (full sensitivity ranking)
python run_analysis.py --mode many
```

**Report Generation**:
```bash
# Add professional HTML report with embedded plots
python run_analysis.py --generate-report
```

**When to use:** Need publication-ready results or detailed documentation

**Additional Output Files Created:**
- **HTML Report:** `../data/output/reports/sensitivity_report_MODE_YYYYMMDD_HHMMSS.html`
  - Contains: executive summary, embedded plots, statistical analysis, recommendations
  - Self-contained file with all visualisations embedded as base64 images
  - Professional formatting suitable for presentations or publications

**Key Differences Between Modes:**
- **Single mode**: Optimal values, curve fitting, critical points, derivatives
- **Few mode**: Parameter interactions, pairwise effects, response surfaces  
- **Many mode**: Sensitivity indices, PCA, correlation matrices, tornado plots

#### 2.3 Component Analysis (Individual Tools)

**Core Statistical Analysis Only:**
```bash
python results_analyzer.py
```
**Output:** `../data/output/analysis_results_YYYYMMDD_HHMMSS.json` (statistical analysis without plots)

**Visualisations Only:**
```bash
python sensitivity_plots.py  
```
**Output:** Multiple PNG files in `../figures/` (plots without statistical analysis)
- Response curves, tornado plots, correlation heatmaps (depending on data)

**Correlation Analysis Only:**
```bash
python correlation_analysis.py
```
**Output:** 
- `../data/output/correlation_analysis_YYYYMMDD_HHMMSS.json` (correlation matrices, PCA results)
- `../figures/pca_analysis_YYYYMMDD_HHMMSS.png` (PCA plots)  
- `../figures/parameter_dendrogram_YYYYMMDD_HHMMSS.png` (clustering plots)

**When to use:** Need specific analysis components or custom workflows
**Note:** Component analysis requires existing simulation data

#### Debug Output Files

All sensitivity analysis commands create a comprehensive debug log file:

**Debug Log:** `../data/output/debug_log_YYYYMMDD_HHMMSS.txt`
- **Content:** Complete verbose output from all simulation runs
- **Includes:** Parameter variations, simulation status, trap/leak counts, execution times, any errors
- **Format:** Human-readable text with clear sections for each simulation
- **Purpose:** Single consolidated file for troubleshooting and debugging issues
- **When Created:** Automatically generated alongside CSV and JSON results

**Example Debug Log Content:**
```
SMOL SENSITIVITY ANALYSIS DEBUG LOG
============================================================
Timestamp: 20241201_143022
Source file: ../templates/simulate_onto_template.smol
Parameters analysed: 2
Total simulations: 9
============================================================

SIMULATION: baseline = baseline
Status: completed
Trap Count: 123
Leak Count: 45
Execution Time: 12.3s
----------------------------------------
SIMULATION: SHALE_SIZE = 14.0
Status: completed
Trap Count: 98
Leak Count: 52
Execution Time: 11.8s
----------------------------------------
...
```

This debug file makes it easier to identify which specific simulations failed, review parameter-by-parameter results, and troubleshoot any issues with the sensitivity analysis process.

### Phase 3: Generate Reports

**Integrated Report Generation** (Recommended):
```bash
# Complete analysis with professional HTML report
python run_analysis.py --generate-report
```

**When to use:** Need complete analysis + publication-ready report in one step

**Output Files Created:**
- All outputs from Phase 2 analysis PLUS:
- **HTML Report:** `../data/output/reports/sensitivity_report_MODE_YYYYMMDD_HHMMSS.html`
  - Executive summary with key findings
  - All visualisations embedded as high-resolution images
  - Statistical analysis results in formatted tables
  - Actionable recommendations based on analysis
  - Self-contained file (no external dependencies)

**Manual Report Generation**:
```bash
# Generate report from existing analysis results  
python report_generator.py
```

**When to use:** Already have analysis results and need custom report formatting
**Requires:** Previous analysis outputs in `data/output/` directory

**Output Files Created:**
- **HTML Report:** `../data/output/reports/sensitivity_report_YYYYMMDD_HHMMSS.html`
  - Generated from existing JSON analysis files
  - May have limited embedded visualisations if plots are missing

---

## Analysis Tools

### 1. Parameter Modification Tools

| Script | Purpose | Usage |
|--------|---------|-------|
| `parameter_modifier_template.py` | Generate parameter variations | Single modifications |
| `batch_runner.py` | Generate multiple simulations | Batch parameter sweeps |

### 2. Analysis Tools

| Script | Purpose | Best For |
|--------|---------|----------|
| `quick_analysis.py` | Fast single parameter analysis | Quick insights |
| `results_analyzer.py` | Core adaptive analysis engine | All analysis modes |
| `correlation_analysis.py` | Statistical correlations & PCA | Multi-parameter studies |
| `sensitivity_plots.py` | Adaptive visualisations | All analysis modes |

### 3. Orchestration Tools

| Script | Purpose | Usage |
|--------|---------|-------|
| `run_analysis.py` | Master analysis orchestrator | Complete workflows |
| `report_generator.py` | HTML report generation | Professional reports |

---

## Usage Examples

### Example 1: Single Parameter Deep Dive
```bash
# Generate data
python batch_runner.py ../templates/simulate_onto_template.smol --parameters SHALE_SIZE --samples 10

# Quick analysis
python quick_analysis.py SHALE_SIZE

# Detailed analysis with report
python run_analysis.py --mode single --parameters SHALE_SIZE --generate-report
```

**Expected Output:**
- Response curve with optimal value
- Correlation analysis (r = 0.85, p < 0.001)
- Sensitivity metrics
- HTML report with recommendations

### Example 2: Two-Parameter Interaction Study
```bash
# Generate data
python batch_runner.py ../templates/simulate_onto_template.smol --parameters SHALE_SIZE TEMP_FACTOR --samples 8 --parallel

# Analyse interactions
python run_analysis.py --mode few --parameters SHALE_SIZE TEMP_FACTOR --generate-report
```

**Expected Output:**
- Individual parameter effects
- Interaction strength analysis
- 2D response surface plots
- Parameter ranking

### Example 3: Comprehensive Sensitivity Analysis
```bash
# Generate full parameter space
python batch_runner.py ../templates/simulate_onto_template.smol --samples 5 --parallel --workers 4

# Comprehensive analysis
python run_analysis.py --mode many --generate-report
```

**Expected Output:**
- Sensitivity indices for all parameters
- Correlation matrix and PCA
- Parameter clustering
- Tornado plot ranking
- Complete HTML report

### Example 4: Custom Analysis Pipeline
```bash
# Step 1: Generate specific parameter combinations
python parameter_modifier_template.py -t ../templates/simulate_onto_template.smol SHALE_SIZE=25.0 TEMP_FACTOR=35.0

# Step 2: Run simulation
python simulation_runner.py ../data/input/simulate_onto_SHALE_SIZE_25.0_*.smol

# Step 3: Analyse results
python results_analyzer.py

# Step 4: Generate visualisations
python sensitivity_plots.py

# Step 5: Create report
python report_generator.py
```

---

## Understanding Results

### Single Parameter Analysis Results

**Key Metrics:**
- **Optimal Value**: Parameter setting that maximizes the target metric
- **Sensitivity Range**: Total variation in target metric across parameter range
- **Correlation**: Strength and direction of relationship
- **Response Curve Fit**: Best mathematical model (linear, quadratic, cubic)

**Interpretation:**
```
Optimal SHALE_SIZE: 23.5 → trap_count: 145
Sensitivity: 85 trap_count units (42% relative variation)
Correlation: r = 0.78 (strong positive relationship)
Best fit: Quadratic (R² = 0.91)
```

### Multi-Parameter Analysis Results

**Parameter Ranking Example:**
1. SHALE_SIZE (|r| = 0.78) - Strongest effect
2. TEMP_FACTOR (|r| = 0.65) - Secondary effect  
3. CAP_SIZE (|r| = 0.43) - Moderate effect

**Interaction Analysis:**
- SHALE_SIZE-TEMP_FACTOR: Significant interaction (strength = 0.12)
- Indicates parameters should be optimised jointly

### Statistical Significance

**P-value Interpretation:**
- p < 0.001: Highly significant relationship
- p < 0.05: Statistically significant
- p ≥ 0.05: No significant relationship

**R² Values:**
- R² > 0.8: Strong model fit
- R² 0.5-0.8: Moderate fit
- R² < 0.5: Weak fit, consider other models

---

## Advanced Features

### 1. Custom Analysis Modes

**Force specific analysis mode:**
```bash
# Force many-parameter analysis on few parameters
python run_analysis.py --mode many --parameters SHALE_SIZE TEMP_FACTOR

# Force single-parameter analysis
python run_analysis.py --mode single --parameters SHALE_SIZE
```

### 2. Sensitivity Indices (Many Parameters)

The framework calculates various sensitivity indices:

- **First-order Sobol indices**: Direct parameter effects
- **Total-order Sobol indices**: Including interaction effects
- **Morris indices**: Screening method for large parameter spaces

**Requires:** SALib library and sufficient sample size

### 3. Correlation Analysis Features

**Pearson vs Spearman:**
- Pearson: Linear relationships
- Spearman: Monotonic (including nonlinear) relationships

**Principal Component Analysis:**
- Identifies parameter combinations that explain most variance
- Reduces dimensionality for large parameter sets

**Hierarchical Clustering:**
- Groups similar parameters
- Identifies redundant parameters

### 4. Advanced Plotting Options

**Matplotlib Style Customisation:**
Edit `sensitivity_plots.py` to customise:
```python
plt.style.use('seaborn-darkgrid')  # Change style
sns.set_palette("viridis")         # Change colour palette
```

**Plot Resolution:**
```python
plotter = SensitivityPlotter()
plotter.dpi = 300  # High resolution for publications
```

### 5. Custom Report Templates

Modify `report_generator.py` to customise HTML reports:
- Add company branding
- Include additional sections
- Modify CSS styling

---

## File Structure

```
sensitivity_analysis/
├── README.md                    # This file
├── requirements.txt             # Python dependencies
├── config/
│   ├── parameters.json          # Parameter definitions
│   └── baseline.json           # Baseline parameter values
├── scripts/                    # Analysis scripts
│   ├── __init__.py
│   ├── parameter_modifier_template.py    # Generate parameter variations
│   ├── simulation_runner.py             # Run individual simulations
│   ├── batch_runner.py                  # Batch simulation orchestrator
│   ├── results_analyzer.py              # Core analysis engine
│   ├── sensitivity_plots.py             # Visualization suite
│   ├── correlation_analysis.py          # Statistical analysis
│   ├── quick_analysis.py               # Single parameter tool
│   ├── run_analysis.py                 # Master orchestrator
│   ├── report_generator.py             # HTML report generator
│   └── test_template_parameterisation.py
├── templates/
│   └── simulate_onto_template.smol     # SMOL template with {{PARAM}} placeholders
├── data/
│   ├── input/                  # Generated SMOL files
│   └── output/                 # Simulation results and analysis
│       └── reports/            # HTML reports
├── figures/                    # Generated plots and visualisations
└── venv/                      # Virtual environment (if using venv)
```

### Key File Types

| Extension | Purpose | Location |
|-----------|---------|----------|
| `*.smol` | SMOL simulation files | `data/input/` |
| `*_output_*.json` | Simulation results | `data/output/` |
| `analysis_*.json` | Analysis results | `data/output/` |
| `*.png` | Visualisations | `figures/` |
| `*.html` | Analysis reports | `data/output/reports/` |
| `*.csv` | Summary statistics | `data/output/` |

---