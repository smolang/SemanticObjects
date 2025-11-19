# SMOL Interactive Debugging Guide

Quick reference for debugging SMOL simulations using the interactive REPL.

## Starting Interactive Mode

**With ontology and prefixes:**
```bash
java -jar build/libs/smol.jar -i copper_simulation.smol -v -l -m \
  -b copper_ontology.ttl \
  -p UFRGS1=https://www.inf.ufrgs.br/bdi/ontologies/geocoreontology#UFRGS \
  -p obo=http://purl.obolibrary.org/obo/ \
  -d http://www.semanticweb.org/quy/ontologies/2023/2/untitled-ontology-38#
```

**Flags:**
- `-i` = input file
- `-v` = verbose output
- `-l` = load REPL (interactive mode)
- `-e` = execute file before REPL (use with `-l` to auto-run then inspect)
- `-m` = materialize RDF output
- `-b` = background ontology
- `-p` = prefix definitions
- `-d` = domain namespace

## Essential REPL Commands

### Execution Control
| Command | Description |
|---------|-------------|
| `read <file>` | Load and parse a SMOL file |
| `step` or `s` | Execute one statement |
| `auto` | Run until next breakpoint or completion |
| `exit` | Exit REPL |

### Inspection
| Command | Description |
|---------|-------------|
| `examine` or `e` | Print runtime state (stack, heap, memory) |
| `eval <expr>` | Evaluate SMOL expression (e.g., `eval unit.copperContent`) |
| `query <SPARQL>` or `q <SPARQL>` | Run SPARQL query on runtime graph |
| `dump [file]` | Export RDF to file (default: `output.ttl`) |

### Configuration
| Command | Description |
|---------|-------------|
| `verbose on\|off` | Toggle verbose output |
| `reasoner off\|rdfs\|owl` | Set reasoning mode |
| `source <name> true\|false` | Enable/disable data sources (heap, staticTable, vocabularyFile) |

## Reasoner Control

SMOL supports three reasoning modes for SPARQL queries:

### Reasoner Modes

| Mode | Description | Use Case |
|------|-------------|----------|
| `off` | No inference | Fast queries, explicit triples only |
| `rdfs` | RDFS reasoning | Class hierarchies, property inheritance |
| `owl` | OWL reasoning (HermiT) | Full ontology inference, complex reasoning |

### Commands

**Check current reasoner:**
```
reasoner
```

**Switch reasoner mode:**
```
reasoner off
reasoner rdfs
reasoner owl
```

### Examples

**Start without reasoning for faster queries:**
```bash
java -jar build/libs/smol.jar -i copper_simulation.smol -l --jenaReasoner off
```

**Enable OWL reasoning in REPL:**
```
> reasoner owl
> query SELECT ?unit WHERE { ?unit a UFRGS:GeoCoreOntology_sedimentary_geological_object }
```

**Disable reasoning for performance:**
```
> reasoner off
> query SELECT * WHERE { ?unit prog:CopperGeoUnit_copperContent ?copper }
```

### Performance Tips

- Use `reasoner off` for debugging program logic (fastest)
- Use `reasoner rdfs` for basic ontology queries (moderate speed)
- Use `reasoner owl` only when you need full inference (slower but most complete)
- Switch modes dynamically in REPL to balance speed vs. inference needs

## Adding Breakpoints

### Syntax
```smol
breakpoint;
```

### Examples

**Break at specific simulation phase:**
```smol
main
    // Phase 1: Basin formation
    while time > 600.0 do
        time = time - 10.0;
    end

    breakpoint;  // Pause after basin formation

    // Phase 2: Mineralization
    while time > 0.0 do
        // ... brine processing
        time = time - 10.0;
    end
end
```

**Conditional breakpoint:**
```smol
while time > 0.0 do
    // Process brine...

    if time <= 200.0 then
        breakpoint;  // Only break in late-stage mineralization
    end

    time = time - 10.0;
end
```

**Break inside methods:**
```smol
class CopperGeoUnit {
    List<Double> processBrine(Double copper, Double salinity, Double oxidation)
        breakpoint;  // Inspect parameters on each call

        // Process brine logic...
        return result;
    end
}
```

## SPARQL Queries in REPL

### Basic Syntax
```
query <SPARQL query>
q <SPARQL query>
```

## Example Queries

**Total copper across all units:**
```sparql
query SELECT (SUM(?copper) as ?total) WHERE {
    ?unit prog:CopperGeoUnit_copperContent ?copper
}
```

**Units with copper enrichment:**
```sparql
query SELECT ?unit ?copper ?depth WHERE {
    ?unit prog:CopperGeoUnit_copperContent ?copper;
          prog:CopperGeoUnit_depth ?depth.
    FILTER(?copper > 1.0)
}
ORDER BY DESC(?copper)
```

**Reduced units (organic-rich):**
```sparql
query SELECT ?unit ?orgC WHERE {
    ?unit prog:ReducedSedimentUnit_organicContent ?orgC.
    FILTER(?orgC > 0.5)
}
```

**Temperature profile:**
```sparql
query SELECT ?unit ?depth ?temp WHERE {
    ?unit prog:CopperGeoUnit_depth ?depth;
          prog:CopperGeoUnit_temperature ?temp
}
ORDER BY ?depth
```

**Stratigraphy with ontology:**
```sparql
query SELECT ?unit ?rockType WHERE {
    ?unit a ?unitClass;
          untitled-ontology-38:constituted_by ?rock.
    ?rock a ?rockType
}
```