# Sediment-Hosted Copper Deposit Simulation

This directory contains a SMOL simulation of copper mineralisation in the Zambian Copperbelt, modelling sediment-hosted copper deposit formation through oxidised brine circulation and redox-controlled precipitation.

## Quick Start

Complete workflow from simulation to visualisation:

```bash
# 1. Build SMOL interpreter (from repository root)
./gradlew shadowJar

# 2. Return to copper simulation directory
cd sediment_hosted_copper

# 3. Run simulation and save output
java -jar ../build/libs/smol.jar \
    -i copper_simulation_new.smol \
    -v -e -m \
    -b copper_ontology.ttl \
    -p UFRGS1=https://www.inf.ufrgs.br/bdi/ontologies/geocoreontology#UFRGS \
    -p obo=http://purl.obolibrary.org/obo/ \
    -d http://www.semanticweb.org/quy/ontologies/2023/2/untitled-ontology-38# \
    > debug_output.txt 2>&1

# 4. Install Python dependencies
pip install -r requirements.txt

# 5. Parse the output
python3 parse_debug_output.py debug_output.txt > parsed_output.txt

# 6. Generate visualisation
python3 visualise_simulation_new.py parsed_output.txt
```

---

## Running the Simulation

### Prerequisites

1. **SMOL interpreter compiled**: Run `./gradlew shadowJar` from repository root
2. **JAR location**: `build/libs/smol.jar` in repository root
3. **Java 11+**: SMOL requires Java 11 or later

### Non-Interactive Mode

Run the simulation and save output to a file:

```bash
java -jar ../build/libs/smol.jar \
    -i copper_simulation_new.smol \
    -v -e -m \
    -b copper_ontology.ttl \
    -p UFRGS1=https://www.inf.ufrgs.br/bdi/ontologies/geocoreontology#UFRGS \
    -p obo=http://purl.obolibrary.org/obo/ \
    -d http://www.semanticweb.org/quy/ontologies/2023/2/untitled-ontology-38# \
    > debug_output.txt 2>&1
```

**Command explanation:**
- `-i copper_simulation_new.smol`: Input SMOL file
- `-v`: Verbose output
- `-e`: Execute mode (non-interactive)
- `-m`: Materialise RDF triples to `output.ttl`
- `-b copper_ontology.ttl`: Background ontology for semantic reasoning
- `-p UFRGS1=...`: Prefix for geoscience ontology
- `-p obo=...`: Prefix for OBO Foundry ontologies
- `-d http://...`: Domain prefix for simulation entities
- `> debug_output.txt 2>&1`: Redirect all output (stdout + stderr) to file

### Interactive Mode (For Debugging)

Run with REPL enabled to inspect state during execution:

```bash
java -jar ../build/libs/smol.jar \
    -i copper_simulation_new.smol \
    -v -l -m \
    -b copper_ontology.ttl \
    -p UFRGS1=https://www.inf.ufrgs.br/bdi/ontologies/geocoreontology#UFRGS \
    -p obo=http://purl.obolibrary.org/obo/ \
    -d http://www.semanticweb.org/quy/ontologies/2023/2/untitled-ontology-38#
```

**Difference from non-interactive:**
- `-l`: Load REPL (interactive shell) instead of `-e`
- Output appears in terminal, not redirected to file

**REPL commands:**
- Type commands interactively after simulation completes
- `exit`: Quit REPL
- See `../../CLAUDE.md` for full REPL command reference

### Running Without Output File

To see output in terminal without saving:

```bash
java -jar ../build/libs/smol.jar \
    -i copper_simulation_new.smol \
    -v -e \
    -b copper_ontology.ttl \
    -p UFRGS1=https://www.inf.ufrgs.br/bdi/ontologies/geocoreontology#UFRGS \
    -p obo=http://purl.obolibrary.org/obo/ \
    -d http://www.semanticweb.org/quy/ontologies/2023/2/untitled-ontology-38#
```

(Omit the `> debug_output.txt 2>&1` redirection)

---

## Parsing the Output

### Why Parsing is Needed

SMOL print statements are fragmented across multiple lines. The parser merges these into readable single lines and extracts structured data for visualisation.

### Install Python Dependencies

```bash
# Install required packages
pip install -r requirements.txt
```

**Contents of requirements.txt:**
- `numpy`: Numerical operations
- `matplotlib`: Plotting
- `pandas`: Data handling

### Run Parser

```bash
python3 parse_debug_output.py debug_output.txt > parsed_output.txt
```

**What the parser does:**
1. Merges multi-line SMOL print statements into single lines
2. Extracts compact `VIZ|` format summaries for visualisation
3. Cleans up formatting (e.g., "dir= up" → "dir=up")
4. Handles special patterns (brine states, precipitation events, etc.)

**Input:** `debug_output.txt` (raw SMOL output)
**Output:** `parsed_output.txt` (cleaned, structured output)

---

## Visualising Results

```bash
python3 visualise_simulation_new.py parsed_output.txt
```

**What we get:**
1. redox_layer_2250m_timeseries.png
2. redox_layer_2950m_timeseries.png
3. combined_layer_comparison.png