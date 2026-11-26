#!/usr/bin/env python3
"""Fails when R8 renamed or removed a field of the SDK's Gson models (SFY-28792).

The SDK's request and response models are (de)serialized by Gson through
reflection. In a host app built with minify, R8 renames fields it may rename
and removes fields it only sees written, unless the SDK ships keep rules.
Either way the JSON keys change and the backend no longer understands the
events, or responses parse to empty objects.

Usage: check_minified_models.py <R8 output directory with mapping.txt and usage.txt>
"""
import os
import sys

MODEL_PACKAGE = "com.segmentify.segmentifyandroidsdk.model."


def is_field(member):
    # Methods have a parameter list; fields do not.
    return "(" not in member


def renamed_fields(mapping_path):
    problems = []
    current = None
    with open(mapping_path, encoding="utf-8") as mapping:
        for line in mapping:
            if line.startswith("#") or not line.strip():
                continue
            if not line.startswith(" "):
                original = line.split(" -> ")[0].strip()
                current = original if original.startswith(MODEL_PACKAGE) else None
                continue
            if current is None or " -> " not in line:
                continue
            member, new_name = line.strip().split(" -> ")
            if not is_field(member):
                continue
            name = member.split()[-1]
            if name != new_name:
                problems.append(f"renamed: {current}.{name} -> {new_name}")
    return problems


def removed_fields(usage_path):
    problems = []
    current = None
    with open(usage_path, encoding="utf-8") as usage:
        for line in usage:
            if not line.strip():
                continue
            if not line.startswith(" "):
                name = line.strip().rstrip(":")
                current = name if name.startswith(MODEL_PACKAGE) else None
                continue
            member = line.strip()
            if current is not None and is_field(member):
                problems.append(f"removed: {current}.{member.split()[-1]}")
    return problems


def main(output_dir):
    mapping = os.path.join(output_dir, "mapping.txt")
    usage = os.path.join(output_dir, "usage.txt")
    for path in (mapping, usage):
        if not os.path.isfile(path):
            print(f"missing R8 output: {path}")
            return 2
    problems = renamed_fields(mapping) + removed_fields(usage)
    if problems:
        print(f"{len(problems)} model field(s) would change the JSON in a minified host app:")
        for problem in problems[:200]:
            print("  " + problem)
        return 1
    print("Model fields survive R8 unchanged.")
    return 0


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print(__doc__)
        sys.exit(2)
    sys.exit(main(sys.argv[1]))
