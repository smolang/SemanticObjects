#!/usr/bin/env python3
"""
Parse SMOL debug output and reformat multi-line print statements into readable single lines.
Usage: python parse_debug_output.py debug_output_1.txt > parsed_output.txt
"""

import sys
import re

def merge_lines(lines):
    """
    Merge fragmented print statements into logical single lines.

    Strategy:
    - Look ahead to detect if next lines are continuations
    - Merge based on patterns like "Label: VALUE unit" split across 3 lines
    """
    merged = []
    i = 0

    while i < len(lines):
        current = lines[i].rstrip()

        # Skip DEBUG OwlStmt and DEBUG Interpreter.owlQuery lines
        if current.startswith('[DEBUG OwlStmt]') or current.startswith('[DEBUG Interpreter.owlQuery]'):
            i += 1
            continue

        # Skip empty lines
        if not current:
            merged.append("")
            i += 1
            continue

        # Check if this is a section header (all === or -----)
        if re.match(r'^[=\-]{40,}$', current):
            merged.append(current)
            i += 1
            continue

        # Pattern: "TIMESTEP #" followed by number, colon, value, unit
        if current.startswith('TIMESTEP #'):
            line_parts = [current]
            i += 1
            # Collect: number, ":", time value, "Ma ago"
            while i < len(lines) and len(line_parts) < 5:
                part = lines[i].strip()
                if not part or part.startswith('='):
                    break
                line_parts.append(part)
                i += 1
                if 'Ma ago' in part or 'ago' in part:
                    break

            # Reconstruct: "TIMESTEP #1: 600.0 Ma ago"
            if len(line_parts) >= 4:
                timestep_num = line_parts[1]
                time_val = line_parts[2].lstrip(':').strip() if len(line_parts) > 2 else ""
                time_unit = line_parts[3] if len(line_parts) > 3 else ""
                if len(line_parts) > 4:
                    time_unit += ' ' + line_parts[4]
                merged.append(f"TIMESTEP #{timestep_num}: {time_val} {time_unit}")
            else:
                merged.append(current)
            continue

        # Pattern: Unit state lines (Reduced shale, Oxidised sediment, Evaporite, Basement)
        if (current.startswith('Reduced shale:') or
            current.startswith('Oxidised sediment:') or
            current.startswith('Evaporite:') or
            current.startswith('Basement:')):
            line_parts = [current]
            i += 1
            # Collect parts until we hit a new unit type or empty line
            while i < len(lines):
                part = lines[i].strip()
                # Stop conditions: empty line, new unit, or section marker
                if (not part or
                    part.startswith('Reduced shale:') or
                    part.startswith('Oxidised sediment:') or
                    part.startswith('Evaporite:') or
                    part.startswith('Basement:') or
                    part.startswith('***') or
                    part.startswith('=') or
                    part.startswith('Brine#')):
                    break
                line_parts.append(part)
                i += 1
                # Stop after we have a complete unit line (ends with kg/m³)
                if 'kg/m³' in part or 'kg/m3' in part:
                    break

            # Reconstruct the unit line
            reconstructed = line_parts[0]
            j = 1
            while j < len(line_parts):
                part = line_parts[j]
                # Add space before units
                if part in ['m', 'ppm', '%', '°C', 'wt%', 'kg/m³', 'kg/m3']:
                    if not reconstructed.endswith(' '):
                        reconstructed += ' '
                    reconstructed += part
                elif part.startswith(','):
                    reconstructed += part
                else:
                    if reconstructed and not reconstructed.endswith(' ') and not reconstructed.endswith(','):
                        reconstructed += ' '
                    reconstructed += part
                j += 1

            merged.append(reconstructed)
            continue

        # Pattern: "Depth: VALUE m, T=VALUE °C" (standalone depth lines)
        if current.startswith('Depth:'):
            line_parts = [current]
            i += 1
            # Collect parts for depth and temperature
            while i < len(lines) and len(line_parts) < 6:
                part = lines[i].strip()
                if not part or part.startswith('***') or part.startswith('BEFORE') or part.startswith('AFTER'):
                    break
                line_parts.append(part)
                i += 1
                if '°C' in part:
                    break

            # Reconstruct
            reconstructed = line_parts[0]
            j = 1
            while j < len(line_parts):
                part = line_parts[j]
                if part in ['m', '°C']:
                    reconstructed += ' ' + part
                elif part.startswith(','):
                    reconstructed += part
                else:
                    reconstructed += ' ' + part
                j += 1

            merged.append(reconstructed)
            continue

        # Pattern: "Injected Brine#" followed by number on next line
        if current.startswith('Injected Brine#'):
            i += 1
            # Get the number on next line
            if i < len(lines) and is_number(lines[i].strip()):
                merged.append(f"{current}{lines[i].strip()}")
                i += 1
            else:
                merged.append(current)
            continue

        # Pattern: "Brine#" followed by number and properties
        if current.startswith('Brine#') or current.strip().startswith('Brine#'):
            line_parts = [current]
            i += 1
            # Collect all parts until we hit empty line or new section
            while i < len(lines):
                part = lines[i].strip()
                if (not part or
                    part.startswith('***') or
                    part.startswith('=') or
                    part.startswith('Entered') or
                    part.startswith('Exit') or
                    part.startswith('Reduced shale:') or
                    part.startswith('Oxidised sediment:') or
                    part.startswith('Evaporite:') or
                    part.startswith('Basement:')):
                    break
                line_parts.append(part)
                i += 1
                # Stop after we have enough parts for a complete line
                if 'dir=' in part or '°C' in part:
                    # Check if next line completes it (like "up" or "down")
                    if i < len(lines):
                        next_part = lines[i].strip()
                        if next_part in ['up', 'down']:
                            line_parts.append(next_part)
                            i += 1
                    break

            # Reconstruct the brine line
            reconstructed = line_parts[0]
            j = 1
            while j < len(line_parts):
                part = line_parts[j]
                # Handle different fragments
                if part.startswith(':') or part.startswith(','):
                    reconstructed += part
                elif part in ['m', 'ppm', '%', '°C', 'wt%', 'up', 'down']:
                    # Special case: 'up'/'down' after 'dir=' should not have space
                    if part in ['up', 'down'] and reconstructed.endswith('='):
                        reconstructed += part
                    else:
                        if not reconstructed.endswith(' '):
                            reconstructed += ' '
                        reconstructed += part
                elif part.startswith('at unit depth') or part.startswith('depth='):
                    reconstructed += ' ' + part
                else:
                    # Check if previous doesn't end with space
                    if reconstructed and not reconstructed.endswith(' ') and not reconstructed.endswith(',') and not reconstructed.endswith('='):
                        reconstructed += ' '
                    reconstructed += part
                j += 1

            merged.append(reconstructed)
            continue

        # Pattern: Compact brine summary for visualisation
        # Format: VIZ|Brine#ID(dir)|Cu=X|Sal=Y|Ox=Z|depth=W
        if current.startswith('VIZ|Brine#'):
            line_parts = [current]
            i += 1

            # Collect all parts (usually split across multiple lines)
            while i < len(lines) and len(line_parts) < 15:
                part = lines[i].strip()
                if not part or part.startswith('---') or part.startswith('VIZ|'):
                    break
                line_parts.append(part)
                i += 1
                # Stop after we have depth value
                if is_number(part) and len(line_parts) > 10:
                    break

            # Reconstruct: VIZ|Brine#1(up)|Cu=100.0|Sal=25.0|Ox=0.5|depth=3450.0
            reconstructed = ""
            for part in line_parts:
                reconstructed += part

            merged.append(reconstructed)
            continue

        # Pattern: Compact redox precipitation summary
        # Format: VIZ|RedoxPpt|depth=X|Cu=Y|Ox=Z
        if current.startswith('VIZ|RedoxPpt'):
            line_parts = [current]
            i += 1

            # Collect all parts
            while i < len(lines) and len(line_parts) < 10:
                part = lines[i].strip()
                if not part or part.startswith('---') or part.startswith('VIZ|'):
                    break
                line_parts.append(part)
                i += 1
                # Stop after oxidation value
                if is_number(part) and len(line_parts) > 5:
                    break

            # Reconstruct
            reconstructed = ""
            for part in line_parts:
                reconstructed += part

            merged.append(reconstructed)
            continue

        # Pattern: Compact vein formation summary
        # Format: VIZ|VeinPpt|type=X|depth=Y|Cu=Z
        if current.startswith('VIZ|VeinPpt'):
            line_parts = [current]
            i += 1

            # Collect all parts
            while i < len(lines) and len(line_parts) < 10:
                part = lines[i].strip()
                if not part or part.startswith('---') or part.startswith('VIZ|') or part.startswith('Cumulative'):
                    break
                line_parts.append(part)
                i += 1
                # Stop after Cu value
                if is_number(part) and len(line_parts) > 5:
                    break

            # Reconstruct
            reconstructed = ""
            for part in line_parts:
                reconstructed += part

            merged.append(reconstructed)
            continue

        # Pattern: Compact evaporite summary
        # Format: VIZ|Evaporite|depth=X|SalBefore=Y|SalAfter=Z
        if current.startswith('VIZ|Evaporite'):
            line_parts = [current]
            i += 1

            # Collect all parts
            while i < len(lines) and len(line_parts) < 10:
                part = lines[i].strip()
                if not part or part.startswith('---') or part.startswith('VIZ|'):
                    break
                line_parts.append(part)
                i += 1
                # Stop after SalAfter value
                if is_number(part) and len(line_parts) > 5:
                    break

            # Reconstruct
            reconstructed = ""
            for part in line_parts:
                reconstructed += part

            merged.append(reconstructed)
            continue

        # Pattern: Parameter lines ending with value and unit on next line
        if (':' in current and
            (current.startswith('GEOTHERMAL_GRADIENT:') or
             current.startswith('VEIN_SALINITY_THRESHOLD:') or
             current.startswith('INITIAL_COPPER_PPM:') or
             current.startswith('BRINE_OXIDATION:') or
             current.startswith('PRECIP_EFFICIENCY:'))):
            line_parts = [current]
            i += 1
            # Collect next line (unit)
            if i < len(lines):
                next_line = lines[i].strip()
                if next_line and not next_line.startswith('='):
                    line_parts.append(next_line)
                    i += 1

            # Reconstruct
            if len(line_parts) == 2:
                merged.append(f"{line_parts[0]}{line_parts[1]}")
            else:
                merged.append(current)
            continue

        # Special pattern: "Cu DEPOSITED: VALUE kg/m³ (VALUE% efficiency)"
        # or "VEIN Cu DEPOSITED: VALUE kg/m³ (concentration factor=VALUE)"
        if 'DEPOSITED:' in current:
            line_parts = [current]
            i += 1

            # Collect the fragmented parts
            while i < len(lines):
                part = lines[i].strip()
                if not part or part.startswith('***') or part.startswith('CUMULATIVE'):
                    break
                line_parts.append(part)
                i += 1
                # Stop after closing parenthesis
                if ')' in part:
                    break

            # Reconstruct the line
            reconstructed = line_parts[0]
            j = 1
            while j < len(line_parts):
                part = line_parts[j]
                # Add space before units and opening parenthesis
                if part in ['kg/m³', 'kg/m3'] or part.startswith('('):
                    reconstructed += ' ' + part
                elif part == ')':
                    reconstructed += part
                elif part.startswith('%'):
                    reconstructed += part
                else:
                    reconstructed += ' ' + part
                j += 1

            merged.append(reconstructed)
            continue

        # Pattern: "  Triggered at depth VALUE m: salinity VALUE -> VALUE wt%"
        if 'Triggered at depth' in current:
            line_parts = [current]
            i += 1

            # Collect fragmented parts (depth, salinity values)
            while i < len(lines) and lines[i].strip():
                part = lines[i].strip()
                if part.startswith('Exit') or part.startswith('Entered'):
                    break
                line_parts.append(part)
                i += 1
                if 'wt%' in part:
                    break

            # Reconstruct
            reconstructed = line_parts[0]
            j = 1
            while j < len(line_parts):
                part = line_parts[j]
                if part in ['m:', 'wt%', '->'] or part.startswith('->'):
                    reconstructed += ' ' + part
                else:
                    reconstructed += part if reconstructed.endswith(' ') else ' ' + part
                j += 1

            merged.append(reconstructed)
            continue

        # Pattern: "  >> Evaporite: salinity increased to VALUE wt%"
        if '>> Evaporite: salinity increased to' in current:
            line_parts = [current]
            i += 1

            # Collect the value and unit (typically split across 2 lines)
            while i < len(lines) and len(line_parts) < 3:
                part = lines[i].strip()
                if not part or part.startswith('Entered') or part.startswith('Exit'):
                    break
                line_parts.append(part)
                i += 1
                if 'wt%' in part:
                    break

            # Reconstruct: "  >> Evaporite: salinity increased to 30.0 wt%"
            if len(line_parts) >= 3:
                value = line_parts[1]
                unit = line_parts[2]
                merged.append(f"{line_parts[0]}{value} {unit}")
            elif len(line_parts) == 2:
                # Value but no unit
                merged.append(f"{line_parts[0]}{line_parts[1]}")
            else:
                merged.append(current)
            continue

        # Check if next line is a numeric value (likely continuation)
        if i + 1 < len(lines):
            next_line = lines[i + 1].strip()

            # Pattern: "Label: " followed by number on next line
            if current.endswith(':') and is_number(next_line):
                value = next_line
                i += 2

                # Check if there's a unit on the following line
                if i < len(lines):
                    unit = lines[i].strip()
                    # Common units
                    if unit in ['m', 'ppm', 'wt%', '%', '°C', 'kg/m³', 'kg/m3', 'Ma', '°C/km']:
                        merged.append(f"{current} {value} {unit}")
                        i += 1
                    elif unit.startswith(',') or unit.startswith('('):
                        # Handle cases like ", T=" or "(efficiency)"
                        merged.append(f"{current} {value}{unit}")
                        i += 1
                    else:
                        merged.append(f"{current} {value}")
                else:
                    merged.append(f"{current} {value}")
                continue

            # Pattern: "Label=VALUE" split as "Label=" on one line, value on next
            if current.endswith('=') and is_number(next_line):
                value = next_line
                i += 2

                # Check for unit or continuation
                if i < len(lines):
                    continuation = lines[i].strip()

                    # Unit indicators
                    if continuation in ['m', 'ppm', 'wt%', '%', '°C', 'kg/m³', 'kg/m3', 'Ma']:
                        merged.append(f"{current}{value} {continuation}")
                        i += 1
                    # Continuation with comma or other punctuation
                    elif continuation.startswith(',') or continuation.startswith('%'):
                        # Look for more fields on same logical line
                        rest = continuation
                        i += 1

                        # Keep merging until we hit a complete break
                        while i < len(lines) and not lines[i].strip().endswith(':'):
                            part = lines[i].strip()
                            if not part:
                                break
                            # Check if it's a new field like "Cu="
                            if '=' in part and part.endswith('='):
                                rest += ' ' + part
                                i += 1
                                # Get the value
                                if i < len(lines):
                                    rest += lines[i].strip()
                                    i += 1
                                    # Get unit if exists
                                    if i < len(lines) and lines[i].strip() in ['m', 'ppm', 'wt%', '%', '°C', 'kg/m³']:
                                        rest += ' ' + lines[i].strip()
                                        i += 1
                            else:
                                rest += part
                                i += 1

                        merged.append(f"{current}{value}{rest}")
                    else:
                        merged.append(f"{current}{value}")
                else:
                    merged.append(f"{current}{value}")
                continue

        # Pattern: "BEFORE:" or "AFTER:" with multiple fields
        if current.startswith('BEFORE:') or current.startswith('AFTER:'):
            merged.append(current)
            i += 1

            # Merge the Ox=, Cu=, Sal= fields that follow
            field_line = ""
            while i < len(lines):
                line = lines[i].strip()
                if not line:
                    break
                if line.startswith('***') or line.endswith(':'):
                    break

                # If it's a field label ending with =
                if line.endswith('='):
                    if field_line:
                        field_line += ' '
                    field_line += line
                    i += 1
                    # Get value
                    if i < len(lines):
                        field_line += lines[i].strip()
                        i += 1
                        # Get unit
                        if i < len(lines) and lines[i].strip() in ['ppm', 'wt%', '%']:
                            field_line += ' ' + lines[i].strip()
                            i += 1
                elif line.startswith(','):
                    field_line += line
                    i += 1
                else:
                    break

            if field_line:
                merged.append(f"  {field_line}")
            continue

        # Default: keep line as is
        merged.append(current)
        i += 1

    return merged

def is_number(s):
    """Check if string is a number (int or float)."""
    try:
        float(s)
        return True
    except ValueError:
        return False

def clean_debug_markers(lines):
    """Optionally remove DEBUG markers for cleaner output."""
    cleaned = []
    for line in lines:
        # Keep DEBUG lines but make them less verbose
        if line.startswith('[DEBUG'):
            # Optionally skip these or keep them
            cleaned.append(line)  # Keep for now
        else:
            cleaned.append(line)
    return cleaned

def main():
    if len(sys.argv) < 2:
        print("Usage: python parse_debug_output.py <debug_output_file>")
        print("Example: python parse_debug_output.py sediment_hosted_copper/debug_output.txt")
        sys.exit(1)

    input_file = sys.argv[1]

    # Read all lines
    with open(input_file, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    # Strip newlines but keep original structure
    lines = [line.rstrip('\n') for line in lines]

    # Merge fragmented lines
    merged_lines = merge_lines(lines)

    # Output
    for line in merged_lines:
        print(line)

if __name__ == '__main__':
    main()
