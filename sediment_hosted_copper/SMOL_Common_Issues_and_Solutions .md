# SMOL Language Common Issues and Solutions

## 1. Mathematical Expression Syntax Errors

### Problem
SMOL does not support inline arithmetic operations or complex expressions. You cannot perform calculations directly in return statements, assignments, or method calls.

### Examples

**Incorrect:**
```smol
// Direct arithmetic in return
return a + b;

// Arithmetic in method parameters
print(depth + 100);
```

**Correct:**
```smol
// Step-by-step arithmetic
Double sum = a + b;
return sum;

// Calculate before using
Double newDepth = depth + 100;
print(newDepth);
```

## 2. Unicode Character Issues

### Problem
SMOL only accepts standard ASCII characters. Unicode characters like full-width semicolons (；) or other special characters will cause syntax errors.

### Examples

**Incorrect:**
```smol
abstract Unit update(SimulationParameters params)；  // Unicode semicolon
abstract Unit printState()；                         // Unicode semicolon
```

**Correct:**
```smol
abstract Unit update(SimulationParameters params);  // ASCII semicolon
abstract Unit printState();                         // ASCII semicolon
```

## 3. Logical Operators

### Problem
SMOL uses word-based logical operators (`and`, `or`) instead of symbol-based operators (`&&`, `||`).

### Examples

**Incorrect:**
```smol
if temperature > 150 && salinity > 31 then
    // code
end

if x < 0 || x > 100 then
    // code
end
```

**Correct:**
```smol
if temperature > 150 and salinity > 31 then
    // code
end

if x < 0 or x > 100 then
    // code
end
```

## 4. List Creation and Manipulation

### Problem
SMOL does not support nested list creation or complex list initialization. Lists must be created empty and populated using `add()` method.

### Examples

**Incorrect:**
```smol
// Nested list creation
return new List<Double>(value1, new List<Double>(value2, new List<Double>(value3, null)));

// Direct initialization with values
List<Double> myList = new List<Double>(1.0, 2.0, 3.0);
```

**Correct:**
```smol
// Create and populate step by step
List<Double> result = new List<Double>();
result.add(value1);
result.add(value2);
result.add(value3);
return result;

// Proper list initialization
List<Double> myList = new List<Double>();
myList.add(1.0);
myList.add(2.0);
myList.add(3.0);
```

## 5. Method Override Syntax

### Problem
When implementing abstract methods in subclasses, you must use the `override` keyword.

### Examples

**Incorrect:**
```smol
class BasementUnit extends CopperGeoUnit ()
    // Missing override keyword
    List<Double> processBrine(Double brineCopper, Double brineSalinity, Double brineOxidation)
        // implementation
    end
end
```

**Correct:**
```smol
class BasementUnit extends CopperGeoUnit ()
    // Proper override declaration
    override List<Double> processBrine(Double brineCopper, Double brineSalinity, Double brineOxidation)
        // implementation
    end
end
```

## 6. For Loop Syntax

### Problem
SMOL has a specific for loop syntax and does not support `++` or `--` operators.

### Examples

**Incorrect:**
```smol
// Using ++ operator
for Int i = 0; i < 5; i++ do
    // code
end

// Using -- operator
for Int j = 10; j > 0; j-- do
    // code
end
```

**Correct:**
```smol
// Proper increment
for Int i = 0; i < 5; i = i + 1 do
    // code
end

// Proper decrement
for Int j = 10; j > 0; j = j - 1 do
    // code
end
```

## 7. Method Calls in Print Statements

### Problem
SMOL does not allow direct method calls within print statements. You must call the method first and store the result.

### Examples

**Incorrect:**
```smol
print("Depth: ");
print(this.getDepth());

print("Temperature: ");
print(unit.getTemperature(params));
```

**Correct:**
```smol
print("Depth: ");
Double depth = this.getDepth();
print(depth);

print("Temperature: ");
Double temp = unit.getTemperature(params);
print(temp);
```

## 8. Conditional Expressions

### Problem
SMOL requires simple conditions and does not support complex boolean expressions in one line.

### Examples

**Incorrect:**
```smol
// Complex condition with multiple operations
if (temp >= params.vein_temp) and (salinity > params.vein_salinity) then
    // code
end

// Negation with complex expression
if !(x > 5 and y < 10) then
    // code
end
```

**Correct:**
```smol
// Simple conditions without parentheses
if temp >= params.vein_temp and salinity > params.vein_salinity then
    // code
end

// Break down complex negations
Boolean condition = x > 5 and y < 10;
if !condition then
    // code
end
```

## 9. String Concatenation

### Problem
SMOL uses the `++` operator for string concatenation, not the `+` operator.

### Examples

**Incorrect:**
```smol
String message = "Temperature: " + temp + " degrees";
```

**Correct:**
```smol
String message = "Temperature: " ++ doubleToString(temp) ++ " degrees";
```

## 10. Type Conversions

### Problem
SMOL requires explicit type conversions using built-in functions.

### Examples

**Incorrect:**
```smol
Int x = 5;
Double y = x;  // Implicit conversion

String s = x;  // Attempting to convert int to string implicitly
```

**Correct:**
```smol
Int x = 5;
Double y = intToDouble(x);  // Explicit conversion

String s = intToString(x);   // Explicit conversion to string
```

## 11. Variable Scope and Declaration

### Problem
Variables must be declared with their type, and scope rules must be followed carefully.

### Examples

**Incorrect:**
```smol
// Using variable before declaration
x = 5;
Int x;

// Redeclaring variables
Int count = 0;
Int count = 1;  // Error: already declared
```

**Correct:**
```smol
// Declare before use
Int x;
x = 5;

// Use different names or reuse existing variable
Int count = 0;
count = 1;  // Reusing existing variable
```

## 12. Return Statements

### Problem
Methods with return types must have explicit return statements, and void methods should not have return types.

### Examples

**Incorrect:**
```smol
// Method with return type but no return
Double calculate()
    Double result = 5.0;
    // Missing return statement
end

// Void method with Unit return type
Unit printMessage()
    print("Hello");
    // Should not have Unit return type
end
```

**Correct:**
```smol
// Proper return statement
Double calculate()
    Double result = 5.0;
    return result;
end

// Void method without return type
printMessage()
    print("Hello");
end
```

## Summary of Best Practices

1. **Always break down complex expressions** into simple step-by-step operations
2. **Use only ASCII characters** in your code
3. **Remember SMOL's unique syntax** for loops, conditionals, and operators
4. **Declare variables before use** with explicit types
5. **Use explicit type conversions** when needed
6. **Follow inheritance rules** with proper `override` keywords
7. **Keep conditions simple** without unnecessary parentheses
8. **Store method results** before using them in other operations

## Common Error Messages and Their Meanings

### "mismatched input"
This usually means you are using syntax from another language that SMOL does not recognise.

### "extraneous input"
You have extra characters or tokens that SMOL does not expect at that position.

### "missing NAME at"
SMOL expects a variable or identifier name at this position.

### "no viable alternative at input"
The parser cannot understand the syntax you are using - often caused by using operators or constructs from other languages.

### "token recognition error"
Usually caused by Unicode or special characters that SMOL does not recognise.

By following these guidelines, you can avoid the most common syntax errors in SMOL and write code that compiles successfully.