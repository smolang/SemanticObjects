1. How could SHACL constraints improve the simulation? If there are more constraints that are not been defined in the ontology, then SHACL constraints could be a great approach. 
2. For the copper use case - what can we do to improve/modify the ontology for it?
3. SHACL constraints - sensitivity analysis?
4. Concentrate on the copper case.

## Question 1: Why and How Ontology Was Used in Qu's Paper

### Can this question be answered?
**Yes, this question can be fully answered from the paper.**

### Why Ontology Was Used:

1. **Bridge Knowledge Gap**: Traditional numerical simulations require complete data, but geological data is inherently incomplete. Ontologies allow encoding expert knowledge to fill gaps.
    - **Paper quote** (Introduction): "Due to the insufficient amount of data, it is difficult for geologists to verify the accuracy, manage the uncertainty, and recognise the incompleteness"
    - **Example**: The Ekofisk field has well data at specific points, but conditions between wells must be inferred
    - **Ontology solution**: Encode rules like "shale at 60°C with kerogen will mature" to reason about unsampled locations
    - **Contrast with numerical**: Numerical models need temperature data at every grid point; ontology needs only the rule

2. **Enable Qualitative Reasoning**: Support geologists in validating interpretations when quantitative data is insufficient (million-year timescales, indirect observations).
    - **Paper context**: "many geological processes took place on a million-year time scale with insufficient data and non-direct observations"
    - **Example**: Cannot directly observe 50-million-year-old maturation, only current oil presence
    - **Ontology benefit**: Captures qualitative knowledge like "oil indicates past maturation" without needing continuous historical data
    - **Real application**: Geologists use presence of oil in Ekofisk to infer Mandal shale reached maturation temperature

3. **Separate Concerns**:
    - **Geologists define domain knowledge** (triggers, processes) without programming
        - Example: Figure 8 shows trigger axioms in geological terms (temperature, rock type)
        - No code required: Geologists write "chalk fractures at 0.36 MPa" as OWL axiom
    - **Programmers implement simulations** without deep geological expertise
        - Example: `simulate_onto.smol` implements generic heat flow (line 195) without knowing petroleum geology
        - Code handles object updates, not geological interpretation
    - **Process triggers serve as the interface** between both worlds
        - Bridge example: Line 330 query connects program objects to geological concepts
        - Neither party needs to understand the other's domain deeply

4. **Formalise Expert Knowledge**: Convert informal geological understanding into machine-readable format for automated reasoning.
    - **Informal knowledge**: "Source rocks mature when buried deep enough to get hot"
    - **Formal representation**: OWL axiom linking temperature property to maturation trigger
    - **Machine reasoning**: SPARQL queries can find all objects meeting formal conditions
    - **Benefit shown**: Paper Table 3 results match expert expectations, validating formalization

### How Ontology Was Used:

1. **Process Trigger Mechanism**:
    - Extended BFO's `process_boundary` to create trigger concept
    - Defined trigger conditions as OWL axioms (Figure 8 in paper)
    - Triggers specify when geological processes start

2. **Semantic Lifting**:
    - SMOL objects are "lifted" to RDF representation
    - Modeling bridge connects program objects to domain concepts
    - Example: `ShaleUnit` maps to geological shale with temperature property

3. **Runtime Integration**:
    - Semantic queries find objects meeting trigger conditions
    - When triggers activate, corresponding methods execute
    - State changes reflect back to knowledge graph

4. **Specific Implementation**:
   ```smol
   // Semantic query for oil maturation trigger
   List<ShaleUnit> fs = member("<domain:models> some (<obo:RO_0000056> some <domain:oil_window_maturation_trigger>)");
   
   // Execute maturation on matching objects
   fs.content.mature();
   ```

## Question 2: What Are the Specific Parts of the Ontology Used in the Paper's Simulation

### Answer:
The paper utilises specific parts of the ontology `total_mini.ttl` for the Ekofisk petroleum system simulation:

1. **Process Triggers** (the key innovation):
   - `oil_window_maturation_trigger` - Triggers thermal maturation when temperature reaches 60-120°C
   - `gas_window_maturation_trigger` - Triggers gas maturation when temperature exceeds 120°C  
   - `chalk_fracturing_trigger` - Triggers when tensile strength reaches 0.36 MPa
   - `shale_fracturing_trigger` - Triggers when tensile strength reaches 0.5 MPa

2. **Geological Processes**:
   - `thermal_maturation_process` - The process of kerogen converting to oil/gas
   - `fluid_migration` - The upward movement of hydrocarbons
   - `fracturing` - The process that changes rock permeability

3. **Domain Entities**:
   - Rock types: `shale`, `chalk`, `sandstone`, `mudstone`
   - Fluids: `oil`, `gas`, `kerogen`, `amount_of_organic_matter`
   - Properties: `temperature`, `tensile_strength`, `permeability`
   - Roles: `source_rock`, `reservoir`, `cap_rock`

4. **BFO/GeoCore Framework**:
   - `BFO:process_boundary` - Extended to create process triggers
   - `GeoCore:geological_process` - Parent class for all geological processes
   - `GeoCore:geological_object` - For rock formations
   - Quality and measurement entities for properties

### Key Finding:
The simulation specifically uses the **oil window maturation trigger** as shown in line 330 of simulate_onto.smol:
```smol
List<ShaleUnit> fs = member("<domain:models> some (<obo:RO_0000056> some <domain:oil_window_maturation_trigger>)");
```

**Important observations about this implementation:**

1. **Single Trigger Usage**: This is the ONLY semantic query for process triggers in the entire `simulate_onto.smol` file. Despite the paper discussing multiple trigger types (gas window, fracturing triggers), the simulation only implements the oil window maturation trigger.

2. **Query Timing**: The trigger query only executes after a specific time threshold (`checkStart = -66.0`), which corresponds to 66 million years ago in the simulation timeline.

3. **Query Mechanism**: The `member()` function performs a semantic query using the pattern:
   - `domain:models` - connects SMOL objects to their ontological representation
   - `obo:RO_0000056` - the "participates in" relation from the Relations Ontology
   - `domain:oil_window_maturation_trigger` - the specific trigger type

4. **Missing Implementations**: 
   - No queries for `gas_window_maturation_trigger` (temperature > 120°C)
   - No queries for fracturing triggers (chalk or shale)
   - No queries for fluid migration triggers

This demonstrates the semantic bridge between the ontology's process triggers and the computational simulation, but also reveals that the paper's implementation is limited to demonstrating only one type of trigger.

### Relationship between simulate_onto.smol and total_mini.ttl:

The relationship is a **runtime semantic connection** where:

1. **total_mini.ttl** provides:
   - The ontological schema (classes, properties, relationships)
   - Domain knowledge definitions (what is a trigger, process, geological object)
   - The semantic framework for reasoning

2. **simulate_onto.smol** provides:
   - The computational implementation of geological processes
   - The simulation logic and state changes
   - Semantic annotations on classes (via `models` statements)

3. **Connection mechanism**:
   - SMOL classes use `models` statements to link to ontology concepts
   - Example from ShaleUnit:
   ```smol
   models "a <http://purl.obolibrary.org/obo/bfo.owl#UFRGS:GeoReservoirOntology_sedimentary_geological_object>; 
           obo:RO_0001015 _:fr3; 
           <http://purl.obolibrary.org/obo/bfo.owl#UFRGS:GeoCoreOntology_constituted_by> _:fr1; 
           obo:RO_0000086 _:fr2. 
           _:fr1 a domain:shale. 
           _:fr2 domain:datavalue %temperature; a domain:temperature. 
           _:fr3 a domain:amount_of_organic_matter."
   ```
   - At runtime, SMOL creates RDF triples connecting program objects to ontology concepts
- The semantic query `member()` in `simulate_onto.smol` searches these runtime triples against patterns defined in the ontology.

4. **Execution flow**:

   **Command Line Invocation:**
   ```bash
   java -jar build/libs/smol.jar --load --jenaReasoner owl --domain "https://github.com/smolang/SemanticObjects/examples/geological#" --execute examples/Geological/simulate_onto.smol --back examples/Geological/total_mini.ttl
   ```
   - `--execute examples/Geological/simulate_onto.smol` - The SMOL program to run
   - `--back examples/Geological/total_mini.ttl` - The background ontology to load

   **Detailed Execution Timeline:**

   a. **Initial Loading Phase**:
      - `total_mini.ttl` is loaded into a Jena RDF model with OWL reasoning enabled
      - This creates the knowledge base with all trigger definitions, geological concepts, and relationships

   b. **Program Compilation**:
      - `simulate_onto.smol` is parsed and compiled
      - The `models` statements in classes are noted but not yet processed

   c. **Runtime - Object Creation**:
      - When `new ShaleUnit(...)` is called (line 362 in simulate_onto.smol)
      - The SMOL runtime creates the object
      - **Semantic Lifting occurs**: The `models` statement is processed
      - RDF triples are generated and added to the runtime RDF graph
      - These triples connect to concepts defined in `total_mini.ttl`

   d. **Runtime - Semantic Query**:
      - When line 330 executes: `member("<domain:models> some (<obo:RO_0000056> some <domain:oil_window_maturation_trigger>)")`
      - The query searches the **combined** RDF graph containing:
        - Static knowledge from `total_mini.ttl` (what is a trigger, what are the relationships)
        - Dynamic knowledge from lifted SMOL objects (which objects exist, their current temperature)
      - The reasoner uses axioms from `total_mini.ttl` to infer which objects participate in triggers

   e. **Throughout Execution**:
      - `simulate_onto.smol` drives the simulation logic
      - `total_mini.ttl` provides the semantic framework for reasoning
      - Both work together through the runtime RDF graph

   **Key Point**: The files work together (not sequentially) throughout execution:
   - The TTL file provides the **static knowledge framework**
   - The SMOL file provides the **dynamic computational behavior**
   - They meet in the **runtime RDF graph** where semantic lifting creates instances that the ontology can reason about

## Question 3: What Works vs. What Doesn't Work in the Paper

**Partially. The paper only shows successful scenarios, making it impossible to fully verify failure cases without running experiments.**

### What Works (Demonstrated in Paper):
1. **Thermal Maturation Process**:
   - **Temperature-based triggering at 60°C for oil window**
     - Demonstrated in: Paper Table 3 showing maturation starts at 52 Ma
     - Code evidence: `simulate_onto.smol` line 195 calculates temperature as `2.5 + ((depth/1000) * 30)`
     - At 52 Ma, the Mandal shale reaches ~60°C based on burial depth
     - The `member()` query at line 330 successfully identifies ShaleUnits meeting this temperature
   
   - **Depth-temperature correlation (30°C/km gradient)**
     - Implemented in: `simulate_onto.smol` line 195
     - Paper Section 4.2 states: "geothermal gradient of 30 °C/km"
     - Formula: temperature = 2.5°C (surface) + (depth_in_km × 30°C/km)
   
   - **Migration of matured hydrocarbons upward**
     - Code implementation: Lines 197-209 in `simulate_onto.smol`
     - When `maturedUnits > 0`, the code calls `this.above.addUnit()` (line 200)
     - Output shows: "migrate from shale" messages in simulation results
   
   - **Trapping beneath seal rocks (mudstone/shale)**
     - Seal behavior: `ShaleUnit.caps()` returns `True` (line 232)
     - Trap logic: Lines 203-205 print "trap in shale" when seal is encountered
     - Paper mentions Vaale Formation (mudstone) acts as seal

2. **Integration Features**:
   - **Semantic queries successfully identify objects meeting trigger conditions**
     - Demonstrated: `simulate_onto.smol` line 330 query returns ShaleUnit objects
     - Evidence: Paper states simulation found objects at correct time (52 Ma)
     - Implementation: `member()` function bridges SMOL objects with ontology patterns
     - Success shown by: "reasoning finished" message followed by maturation execution
   
   - **Process execution follows trigger activation**
     - Code flow: Lines 333-339 iterate through query results and call `mature()`
     - Paper Table 3: Shows maturation period 52-14 Ma matches code execution
     - Each matched object executes its process method immediately after identification
   
   - **State changes (temperature, maturation) tracked correctly**
     - Temperature updates: Line 195 recalculates based on burial depth each timestep
     - Maturation counter: `maturedUnits` increments (line 222) when triggered
     - Output verification: Print statements show depth, temperature, and maturation status
     - Paper Figure 20: Graph shows consistent state evolution over simulation time

### What is NOT Shown/Tested:
1. **Failed Trigger Scenarios**:
   - **What happens if temperature never reaches 60°C?**
     - Expected behavior: No maturation should occur, `maturedUnits` should remain 0
     - The `member()` query should return an empty list
     - No "maturation on-going!" messages should appear
     - Why not tested: Paper only shows deep burial scenarios where 60°C is always reached
   
   - **Behavior when tensile strength conditions aren't met**
     - Expected: Rocks should not fracture, permeability should remain unchanged
     - No fracturing process should trigger
     - Why not tested: Fracturing triggers are defined but never queried in the code
   
   - **Gas window trigger (>120°C) - not demonstrated**
     - Expected: Different maturation products (gas instead of oil)
     - Should trigger at deeper depths/later times
     - Why not tested: No `member()` query for `gas_window_maturation_trigger`

2. **Edge Cases**:
   - **Multiple simultaneous triggers**
     - Not tested: Only one `member()` query at line 330
     - Missing: No code checking for both maturation AND fracturing at same time
     - Potential issue: Order of execution could matter but isn't specified
     - Implementation gap: Would need multiple queries or combined query patterns
   
   - **Conflicting process interactions**
     - Example not tested: What if fracturing changes permeability during migration?
     - Current code: Processes are independent (maturation doesn't affect migration path)
     - Missing: No mechanism for one process to modify conditions for another
     - Real-world issue: Fracturing should create new migration pathways
   
   - **Incomplete data scenarios**
     - Not tested: What if temperature data is missing for some units?
     - Current assumption: All properties always have values
     - Missing: No null checks or default value handling in semantic queries
     - Implementation gap: Ontology doesn't specify behavior for missing data

3. **Alternative Parameter Values**:
   - **Different temperature gradients**
     - Current: Fixed at 30°C/km (line 195: `((under/1000) * 30)`)
     - Not tested: Low gradient (15°C/km) or high gradient (45°C/km) scenarios
     - Impact: Would change depth at which 60°C is reached
     - Implementation: Gradient is hardcoded, not parameterised
   
   - **Various rock property combinations**
     - Current: Only tests chalk/sandstone as reservoirs, shale as source
     - Not tested: What if sandstone had organic matter?
     - Missing: Different porosity, permeability values
     - Gap: Rock properties are simplified (only `caps()` method differs)
   
   - **Non-standard geological sequences**
     - Current: Fixed sequence - Mandal(shale) → Tor(chalk) → Ekofisk(chalk) → Vaale(mudstone)
     - Not tested: Inverted sequences, missing layers, lateral variations
     - Implementation limitation: Deposition is purely vertical (1D column)
     - Real-world gap: No faults, unconformities, or complex geometries

### Gap in Knowledge:
The paper lacks a systematic exploration of failure modes. To fully understand what doesn't work, specific test cases are needed:

**1. Temperature Threshold Tests:**
- **Test Case**: Shallow burial scenario (max depth < 2000m)
- **Setup**: Modify deposition to stop early or use thinner layers
- **Expected Result**: Temperature stays below 60°C, no maturation occurs
- **Implementation**: Change line 366-388 deposit fewer/thinner units

**2. No Organic Matter Test:**
- **Test Case**: ShaleUnit with `hasKerogenSource = False`
- **Setup**: Create shale units without kerogen (line 362: `new ShaleUnit(..., False, 0)`)
- **Expected Result**: Even at high temperature, no maturation should occur
- **Current Gap**: The axioms in ontology don't check for organic matter presence

**3. Gas Window Test (>120°C):**
- **Test Case**: Deep burial scenario (>4000m depth)
- **Setup**: Add more deposition layers to increase burial depth
- **Implementation Needed**: Add query for `gas_window_maturation_trigger` after line 330
- **Expected Result**: Different maturation behavior at higher temperatures

**4. Fracturing Trigger Tests:**
- **Test Case**: Apply stress to chalk/shale units
- **Current Gap**: No stress/strain properties or calculations in the code
- **Implementation Needed**: 
  - Add tensile strength property to rock units
  - Add stress calculation based on overburden
  - Add queries for fracturing triggers
  - Implement permeability changes when fracturing occurs

**5. Multiple Trigger Interaction:**
- **Test Case**: Simultaneous maturation and fracturing
- **Setup**: Deep, high-stress environment
- **Implementation Needed**: Multiple `member()` queries in sequence
- **Expected Behavior**: Both processes should execute without interference

**Test Goals:**
- Validates the ontology's trigger conditions actually work as designed
- Ensures the semantic approach handles negative cases correctly
- Tests the robustness of the trigger mechanism
- Verifies that the ontology-program bridge works for all defined triggers, not just one

### Key Innovation:
The paper's main contribution is the **"semantically triggered" approach** where:
- Ontology defines WHEN processes occur (declarative knowledge)
- Program defines HOW processes execute (procedural knowledge)
- Process triggers serve as the bridge between static knowledge and dynamic simulation

This separation allows domain experts and programmers to work independently while maintaining semantic consistency.

## Conclusions