#!/usr/bin/env python3
"""Fails when median cold start regresses more than the allowed share against the committed baseline.

Usage: check_startup_regression.py <benchmarkData.json> <baseline.json> [max_regression=0.15]
"""
import json
import sys

METRIC = "timeToInitialDisplayMs"


def medians(path):
    data = json.load(open(path))
    return {
        b["name"]: b["metrics"][METRIC]["median"]
        for b in data.get("benchmarks", [])
        if METRIC in b.get("metrics", {})
    }


def main(argv):
    if len(argv) < 3:
        print(__doc__)
        return 2
    current = medians(argv[1])
    baseline = json.load(open(argv[2]))
    limit = float(argv[3]) if len(argv) > 3 else 0.15
    if not baseline:
        print("Baseline has no startup benchmarks; refusing to pass an empty check.")
        return 2
    failed = False
    for name, base in baseline.items():
        if name not in current:
            print(f"MISSING  {name}: not in this run")
            failed = True
            continue
        now = current[name]
        change = (now - base) / base
        status = "FAIL" if change > limit else "ok"
        failed |= status == "FAIL"
        print(f"{status:7}  {name}: {now:.1f} ms vs baseline {base:.1f} ms ({change:+.1%}, limit +{limit:.0%})")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
