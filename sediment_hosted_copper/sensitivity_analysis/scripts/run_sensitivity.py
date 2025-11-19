#!/usr/bin/env python3
"""
Sensitivity Analysis Script for SMOL Copper Simulation

Runs multiple simulations with controlled parameter variations to assess sensitivity.
Uses one-at-a-time (OAT) approach: vary each parameter while keeping others at baseline.

Usage:
    python run_sensitivity.py [--samples N] [--base-dir PATH]

Options:
    --samples N     Number of samples per parameter (default: 6)
    --base-dir PATH Base directory (default: ../)
"""

import argparse
import json
import os
import re
import subprocess
import sys
import time
from datetime import datetime
from pathlib import Path

import numpy as np
import pandas as pd


class CopperSensitivityAnalysis:
    """Orchestrates sensitivity analysis for copper deposit simulation."""

    def __init__(self, base_dir='.', n_samples=6):
        """
        Initialize sensitivity analysis.

        Args:
            base_dir: Base directory containing sensitivity_analysis/
            n_samples: Number of samples per parameter for OAT analysis
        """
        self.base_dir = Path(base_dir).resolve()
        self.n_samples = n_samples

        # Paths
        self.sensitivity_dir = self.base_dir / 'sensitivity_analysis'
        self.config_dir = self.sensitivity_dir / 'config'
        self.results_dir = self.sensitivity_dir / 'results'
        self.smol_file = self.sensitivity_dir / 'copper_simulation_parameterized.smol'
        self.ontology_file = self.base_dir / 'copper_ontology.ttl'
        self.jar_path = self.base_dir.parent / 'build' / 'libs' / 'smol.jar'

        # Load configuration
        with open(self.config_dir / 'parameters.json', 'r') as f:
            self.param_config = json.load(f)

        with open(self.config_dir / 'baseline.json', 'r') as f:
            self.baseline = json.load(f)

        # Results storage
        self.results = []

    def generate_parameter_variations(self):
        """
        Generate parameter sets using one-at-a-time (OAT) sampling.

        Returns:
            List of dicts, each representing a scenario with all parameter values
        """
        variations = []

        # Baseline scenario
        baseline_scenario = {
            'scenario': 'baseline',
            'parameter': 'baseline',
            'value': 'baseline',
            **{k: v for k, v in self.baseline.items()}
        }
        variations.append(baseline_scenario)

        # Vary each parameter
        for param_name, config in self.param_config.items():
            min_val = config['min']
            max_val = config['max']
            baseline_val = config['baseline']

            # Generate evenly-spaced samples
            samples = np.linspace(min_val, max_val, self.n_samples)

            for sample_val in samples:
                # Skip values within 1% of baseline
                if abs(sample_val - baseline_val) / baseline_val < 0.01:
                    continue

                # Create scenario with this parameter varied
                scenario = {
                    'scenario': f'{param_name}_{sample_val:.2f}',
                    'parameter': param_name,
                    'value': sample_val,
                    **{k: v for k, v in self.baseline.items()}  # Start with baseline
                }
                scenario[param_name] = sample_val  # Override with varied value
                variations.append(scenario)

        return variations

    def modify_smol_file(self, params, output_path):
        """
        Create modified SMOL file with specific parameter values.

        Args:
            params: Dict of parameter values (keys match variable names)
            output_path: Where to save modified file
        """
        with open(self.smol_file, 'r') as f:
            content = f.read()

        # Replace each parameter value using regex
        for param_name, param_value in params.items():
            if param_name in ['scenario', 'parameter', 'value']:
                continue  # Skip metadata fields

            # Pattern matches: Double PARAM_NAME = 123.45;
            pattern = rf'(Double {param_name}\s*=\s*)[0-9.]+(\s*;)'
            replacement = rf'\g<1>{param_value}\g<2>'
            content = re.sub(pattern, replacement, content)

        # Write modified file
        with open(output_path, 'w') as f:
            f.write(content)

    def run_simulation(self, smol_path):
        """
        Execute SMOL simulation using JAR.

        Args:
            smol_path: Path to SMOL file to run

        Returns:
            (stdout_output, stderr_output, return_code)
        """
        cmd = [
            'java', '-jar', str(self.jar_path),
            '-i', str(smol_path),
            '-e',  # Execute mode
            '-b', str(self.ontology_file),
            '-p', 'UFRGS1=https://www.inf.ufrgs.br/bdi/ontologies/geocoreontology#UFRGS',
            '-p', 'obo=http://purl.obolibrary.org/obo/',
            '-d', 'http://www.semanticweb.org/quy/ontologies/2023/2/untitled-ontology-38#'
        ]

        try:
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=600,  # 10 minute timeout
                cwd=str(self.base_dir)
            )
            return result.stdout, result.stderr, result.returncode
        except subprocess.TimeoutExpired:
            return '', 'Timeout after 600 seconds', -1
        except Exception as e:
            return '', str(e), -1

    def extract_total_copper(self, output_text):
        """
        Extract 'Total copper deposited' value from simulation output.

        Args:
            output_text: Combined stdout/stderr from simulation

        Returns:
            Float value or None if not found
        """
        pattern = r'Total copper deposited:\s*([\d.]+)\s*kg/m3'
        match = re.search(pattern, output_text)
        if match:
            return float(match.group(1))
        return None

    def extract_max_copper(self, output_text):
        """
        Extract 'Maximum unit copper' value from simulation output.

        Args:
            output_text: Combined stdout/stderr from simulation

        Returns:
            Float value or None if not found
        """
        pattern = r'Maximum unit copper:\s*([\d.]+)\s*kg/m3'
        match = re.search(pattern, output_text)
        if match:
            return float(match.group(1))
        return None

    def run_all_simulations(self):
        """Run simulations for all parameter variations."""
        variations = self.generate_parameter_variations()

        print(f"{'='*70}")
        print(f"COPPER SENSITIVITY ANALYSIS")
        print(f"{'='*70}")
        print(f"Total scenarios: {len(variations)}")
        print(f"  Baseline: 1")
        print(f"  Parameter variations: {len(variations) - 1}")
        print(f"  Samples per parameter: {self.n_samples}")
        print(f"{'='*70}\n")

        for i, variation in enumerate(variations, 1):
            scenario_name = variation['scenario']
            print(f"[{i}/{len(variations)}] Running: {scenario_name}...", end=' ', flush=True)

            # Create temporary SMOL file with modified parameters
            temp_smol = self.results_dir / f"temp_{scenario_name}.smol"

            try:
                start_time = time.time()

                # Modify SMOL file
                param_dict = {k: v for k, v in variation.items()
                              if k not in ['scenario', 'parameter', 'value']}
                self.modify_smol_file(param_dict, temp_smol)

                # Run simulation
                stdout, stderr, returncode = self.run_simulation(temp_smol)
                elapsed = time.time() - start_time

                # Parse results
                combined_output = stdout + stderr
                total_copper = self.extract_total_copper(combined_output)
                max_copper = self.extract_max_copper(combined_output)

                # Determine status
                if returncode != 0:
                    status = 'error'
                elif total_copper is None or max_copper is None:
                    status = 'parse_failed'
                else:
                    status = 'success'

                # Store results
                result = {
                    'scenario': scenario_name,
                    'parameter': variation['parameter'],
                    'value': variation['value'],
                    'total_copper': total_copper,
                    'max_copper': max_copper,
                    'status': status,
                    'elapsed_time': elapsed,
                    **{f'param_{k}': v for k, v in param_dict.items()}
                }
                self.results.append(result)

                print(f"{status.upper()} ({elapsed:.1f}s)", end='')
                if total_copper is not None:
                    print(f" | Total Cu: {total_copper:.1f} kg/m³")
                else:
                    print()

            except Exception as e:
                print(f"ERROR: {e}")
                result = {
                    'scenario': scenario_name,
                    'parameter': variation['parameter'],
                    'value': variation['value'],
                    'total_copper': None,
                    'max_copper': None,
                    'status': 'exception',
                    'elapsed_time': 0,
                    **{f'param_{k}': v for k, v in param_dict.items()}
                }
                self.results.append(result)

            finally:
                # Clean up temporary file
                if temp_smol.exists():
                    temp_smol.unlink()

    def save_results(self):
        """Save results to CSV and JSON files."""
        df = pd.DataFrame(self.results)

        # Generate timestamp
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')

        # Save CSV
        csv_path = self.results_dir / f'sensitivity_results_{timestamp}.csv'
        df.to_csv(csv_path, index=False)
        print(f"\n✓ Results saved to: {csv_path}")

        # Save JSON (more detailed)
        json_path = self.results_dir / f'sensitivity_results_{timestamp}.json'
        with open(json_path, 'w') as f:
            json.dump(self.results, f, indent=2)
        print(f"✓ Results saved to: {json_path}")

        # Print summary
        print(f"\n{'='*70}")
        print("SUMMARY")
        print(f"{'='*70}")
        success_count = sum(1 for r in self.results if r['status'] == 'success')
        print(f"Successful runs: {success_count}/{len(self.results)}")

        if success_count > 0:
            successful_results = [r for r in self.results if r['status'] == 'success']
            total_cu_values = [r['total_copper'] for r in successful_results]
            print(f"Total copper range: {min(total_cu_values):.1f} - {max(total_cu_values):.1f} kg/m³")

        return csv_path


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(
        description='Run sensitivity analysis for SMOL copper simulation'
    )
    parser.add_argument(
        '--samples',
        type=int,
        default=6,
        help='Number of samples per parameter (default: 6)'
    )
    parser.add_argument(
        '--base-dir',
        type=str,
        default='..',
        help='Base directory containing sensitivity_analysis/ (default: ../)'
    )

    args = parser.parse_args()

    # Create analyzer
    analyzer = CopperSensitivityAnalysis(
        base_dir=args.base_dir,
        n_samples=args.samples
    )

    # Run analysis
    analyzer.run_all_simulations()

    # Save results
    csv_path = analyzer.save_results()

    # Print next steps
    print(f"\n{'='*70}")
    print("NEXT STEPS")
    print(f"{'='*70}")
    print("To generate visualizations, run:")
    print(f"  python scripts/visualize_results.py {csv_path}")
    print(f"{'='*70}\n")


if __name__ == '__main__':
    main()
