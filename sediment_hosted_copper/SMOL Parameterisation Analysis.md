# SMOL Language Parameterisation: Available Features and Limitations

## 1. Introduction

This document provides a technical analysis of parameterisation capabilities in the SMOL language, distinguishing between supported parameterisation features and those not available in the current language specification. Examples are drawn from the `simulate_onto.smol` file, located in the `SemanticObjects/examples/Geological` directory of the GitHub repository referenced in Qu et al.'s 2024 paper "Semantically Triggered Qualitative Simulation of a Geological Process."

The SMOL Language Manual is available at: https://smolang.org/language.html

## 2. Available Parameterisation Features in SMOL

### 2.1 Class Constructor Parameters

SMOL supports parameterisation through class constructors, allowing values to be passed during object instantiation and stored as instance fields.

**Documentation**: https://smolang.org/language/classes.html

#### Example from simulate_onto.smol:

```smol
abstract class GeoObject (hidden GeoObject above,
                          hidden GeoObject below,
                          hidden GeoObject left,
                          hidden GeoObject right,
                          hidden GeoObject behind,
                          hidden GeoObject front,
                          hidden Double size)
```

```smol
class ShaleUnit extends GeoUnit (hidden Double temperature, 
                                 hidden Boolean hasKerogenSource, 
                                 hidden Int maturedUnits)
```

#### Usage:
```smol
ShaleUnit mandal = new ShaleUnit(null, null, null, null, null, null, 
                                 20.0, 1, 0.0, True, 0);
```

**Characteristics:**
- Parameters are passed at object creation time
- Values are stored as instance fields
- Type checking is enforced at compile time
- Parameters are scoped to individual object instances

### 2.2 Method Parameters

Methods in SMOL classes can accept parameters for computation and logic flow.

**Documentation**: https://smolang.org/language/classes.html

#### Example from simulate_onto.smol:

```smol
class GeoUnit extends GeoObject (hidden Int mergeId)
    Boolean canMerge(GeoUnit other)
        return other.mergeId == this.mergeId;
    end
    
    Unit mergeWith(GeoUnit other)
        this.size = this.size + other.size;
    end
end
```

```smol
class Driver(GeoUnit top, GeoUnit bottom)
    Unit sim(List<DepositionGenerator> actions, 
             Double startPast, 
             GeoUnit init, 
             Double checkStart)
        // Implementation
    end
end
```

**Characteristics:**
- Parameters are passed at method invocation
- Scope is limited to method execution
- Support for multiple parameter types including objects and primitives

### 2.3 FMU Simulation Parameters

SMOL provides specialised parameterisation for Functional Mock-up Units (FMUs). FMUs are standardised simulation models following the Functional Mock-up Interface (FMI) standard, which enables model exchange and co-simulation between different simulation tools. In SMOL, FMUs are used to integrate numerical simulators (such as physical system models) into the semantic simulation framework.

**Documentation**: https://smolang.org/language/fmos.html

#### When to Use FMUs:
- Integrating existing simulation models (e.g., from Modelica, Simulink)
- Simulating continuous physical systems (thermal, mechanical, electrical)
- Coupling multiple domain-specific simulators
- Creating digital twins that combine discrete logic with continuous dynamics

#### General Syntax:
```smol
FMO[in Type1 param1, out Type2 param2] fmu = simulate("path/to/fmu.fmu", 
                                                        param1=initialValue1, 
                                                        param2=initialValue2);
```

#### Example from SMOL documentation:
```smol
main
    FMO[in Boolean v, out Double l] tank = simulate("Tank.fmu", 
                                                     v = False,  // valve closed
                                                     d = 0.5,    // drain rate
                                                     f = 1.0);   // fill rate
    tank.doStep(1.0);  // advance simulation by 1 time unit
end
```

**Note**: The geological simulation in simulate_onto.smol does not utilise FMUs, as it implements discrete event simulation rather than continuous system dynamics.

### 2.4 SPARQL Query Parameters

SMOL supports parameterised SPARQL queries using the `%i` placeholder notation, where `i` is a positive integer.

**Documentation**: https://smolang.org/language/semantic-access.html

#### Example from SMOL documentation:
```smol
List<C> l = access("SELECT ?obj WHERE {?obj prog:C_b ?b. ?b prog:D_c %1 }", this.x);
```

#### From simulate_onto.smol (non-parameterised query):
```smol
List<ShaleUnit> fs = member("<domain:models> some (<obo:RO_0000056> 
    some <domain:oil_window_maturation_trigger>)");
```

#### Parameterised Query Example (following SMOL syntax):
```smol
// If the geological simulation needed parameterized queries:
String propertyName = "temperature";
Double threshold = 100.0;

List<ShaleUnit> results = access(
    "SELECT ?obj WHERE { 
        ?obj prog:ShaleUnit_%1 ?value . 
        FILTER(?value > %2) 
    }", 
    propertyName, 
    threshold
);
```

**Characteristics:**
- Parameters are substituted using `%1`, `%2`, etc.
- Parameters are passed after the query string
- Supports multiple parameter types
- Used for dynamic query construction

## 3. Parameterisation Features Not Available in SMOL

### 3.1 File-Level Parameter Declaration

SMOL does not support declaring parameters at the file level that can be referenced throughout the program.

#### Not Supported - Hypothetical Example:
```smol
// THIS SYNTAX IS NOT VALID IN SMOL
param Double MIGRATION_INCREMENT = 100.0;
param Double BASE_TEMPERATURE = 2.5;
param Double TEMP_GRADIENT = 30.0;

class ShaleUnit extends GeoUnit (...)
    override Unit update()
        // Cannot reference file-level parameters
        this.temperature = BASE_TEMPERATURE + (depth * TEMP_GRADIENT);
    end
end
```

#### Current Implementation Requirement from simulate_onto.smol:
```smol
class ShaleUnit extends GeoUnit (...)
    override Unit update()
        Double under = this.getSizeAbove();
        // Values must be hardcoded
        this.temperature = 2.5 + ((under/1000) * 30);
    end
end
```

### 3.2 Global Constants

SMOL does not provide a mechanism for defining global constants accessible across classes and methods.

#### Not Supported - Hypothetical Example:
```smol
// THIS SYNTAX IS NOT VALID IN SMOL
const Int DEFAULT_LAYER_COUNT = 31;
const Double DEFAULT_DURATION = 2.0;
```

#### Current Implementation from simulate_onto.smol:
```smol
// Values must be specified at each usage point
DepositionGenerator dep = new DepositionGenerator(div, 2.0, 31);
DepositionGenerator depTor = new DepositionGenerator(tor, 2.0, 5);
DepositionGenerator depEko = new DepositionGenerator(ekofisk, 2.0, 1);
```

### 3.3 Configuration Import Mechanisms

SMOL does not support importing configuration from external files or sources at compile time.

#### Not Available:
- No `import` or `include` statements for configuration files
- No preprocessor directives for compile-time substitution
- No environment variable access for configuration

### 3.4 Default Parameter Values

SMOL does not support default values for class constructor or method parameters.

#### Not Supported - Hypothetical Example:
```smol
// THIS SYNTAX IS NOT VALID IN SMOL
class ChalkUnit extends GeoUnit (List<Double> kerogenUnits = null)
```

## 4. Practical Implications

### 4.1 Hardcoded Values in simulate_onto.smol

The absence of file-level parameterisation results in hardcoded values throughout the implementation:

```smol
// Temperature calculation in ShaleUnit
this.temperature = 2.5 + ((under/1000) * 30);

// Migration increment in ChalkUnit
if next + 100.0 < this.size then
    l.content = next + 100;
end

// Simulation parameters in main
driver.sim(dl, 136.0, mandal, (-66.0));
```

### 4.2 Configuration Distribution

Configuration values must be distributed across multiple locations:

```smol
// In object creation
ShaleUnit mandal = new ShaleUnit(null, null, null, null, null, null, 
                                 20.0, 1, 0.0, True, 0);

// In deposition generators
DepositionGenerator dep = new DepositionGenerator(div, 2.0, 31);

// In simulation execution
driver.sim(dl, 136.0, mandal, (-66.0));
```

## 5. Comparison Summary

| Feature | Available in SMOL | Example from Code | Documentation |
|---------|------------------|-------------------|---------------|
| Class constructor parameters | Yes | `new ShaleUnit(..., 20.0, 1, 0.0, True, 0)` | [Classes](https://smolang.org/language/classes.html) |
| Method parameters | Yes | `sim(actions, startPast, init, checkStart)` | [Classes](https://smolang.org/language/classes.html) |
| FMU parameters | Yes | Not used in geological example | [FMOs](https://smolang.org/language/fmos.html) |
| SPARQL query parameters | Yes | `access("... %1", value)` syntax | [Semantic Access](https://smolang.org/language/semantic-access.html) |
| File-level parameters | No | Would replace hardcoded `2.5`, `100.0`, etc. | N/A |
| Global constants | No | Would replace repeated `2.0` duration values | N/A |
| Configuration imports | No | Would centralise all numeric values | N/A |
| Default parameter values | No | Would simplify object construction | N/A |

## 6. Implications for Sensitivity Analysis

### 6.1 Intended Use Case

Sensitivity analysis of the geological simulation requires systematic variation of 19 key parameters spanning layer dimensions, temperature model coefficients, migration rates, timing parameters, and layer counts. These parameters are currently hardcoded throughout the simulate_onto.smol file.

### 6.2 Recommended Approach Given SMOL's Limitations

Since SMOL lacks file-level parameters, the most practical approach is:

1. **Restructure the main block** to group all parameter values at the beginning:
   ```smol
   main
       // === SENSITIVITY ANALYSIS PARAMETERS ===
       // Layer dimensions
       Double shaleSize = 20.0;
       Double sandstoneSize = 26.5;
       Double torSize = 67.5;
       // ... all other parameters
       
       // Use variables instead of literals
       ShaleUnit mandal = new ShaleUnit(null, null, null, null, null, null, 
                                        shaleSize, 1, 0.0, True, 0);
   ```

2. **Create a parameter-passing structure** for values used inside classes:
   ```smol
   class SimConfig(Double baseTemp, Double tempFactor, Double hydroIncrement)
   end
   ```

3. **Modify classes** to accept configuration:
   ```smol
   class ShaleUnit extends GeoUnit (..., hidden SimConfig config)
       override Unit update()
           Double under = this.getSizeAbove();
           this.temperature = this.config.baseTemp + 
                             ((under/1000) * this.config.tempFactor);
       end
   end
   ```

### 6.3 External Workflow Requirements

Since SMOL does not support configuration files or command-line parameters for passing runtime values to programs, conducting sensitivity analysis requires creating a new SMOL file for each parameter combination. This cumbersome workflow involves:

1. **File Modification for Each Run**: Unlike languages that allow parameter passing via configuration files or command-line arguments, SMOL requires physically modifying the source file for each simulation run. For example, testing 5 temperature gradients × 4 migration rates × 3 layer thicknesses would require creating 60 different SMOL files.

2. **Parameter Replacement Script**: An external pre-processor must:
   - Read the original SMOL file as a template
   - Search and replace hardcoded values with new parameter values
   - Write out a new SMOL file for each parameter combination
   - Manage file naming to track which parameters were used
   
   Example workflow:
   ```python
   # For each parameter combination in sensitivity analysis
   for temp_gradient in [25.0, 30.0, 35.0]:
       # Read original file
       with open('simulate_onto.smol', 'r') as f:
           content = f.read()
       
       # Replace hardcoded value
       content = content.replace('((under/1000) * 30)', 
                                f'((under/1000) * {temp_gradient})')
       
       # Write new file
       with open(f'simulate_onto_temp{temp_gradient}.smol', 'w') as f:
           f.write(content)
       
       # Execute simulation on modified file
       run_smol(f'simulate_onto_temp{temp_gradient}.smol')
   ```

3. **Result Management**: Since each run produces output from a different file, external tooling must also track which outputs correspond to which parameter values.

This approach is significantly more complex than typical sensitivity analysis workflows where parameters can be passed externally without modifying source code.

## 7. Conclusion

SMOL provides parameterisation mechanisms suitable for object-oriented programming patterns, including class constructors, method parameters, and specialised support for FMU and SPARQL operations. However, the language does not support file-level parameter declarations, global constants, or external configuration mechanisms. This limitation requires us to embed configuration values directly in code and employ design patterns to manage configuration complexity in larger applications such as the geological simulation demonstrated in simulate_onto.smol.