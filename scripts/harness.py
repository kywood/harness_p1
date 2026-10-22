#!/usr/bin/env python3

import argparse
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BUILD = ROOT / "scripts/build.sh"
LOG_DIR = ROOT / "logs"
MODEL = "ollama_chat/qwen2.5-coder:7b"

MAX_FIXES = 3
AI_TIMEOUT = 900
BUILD_TIMEOUT = 300


def run(command, timeout, log_name):
    LOG_DIR.mkdir(exist_ok=True)
    log_path = LOG_DIR / log_name

    print(f"\n[RUN] {' '.join(command[:3])}", flush=True)

    try:
        with log_path.open("w", encoding="utf-8") as log:
            result = subprocess.run(
                command,
                cwd=ROOT,
                stdout=log,
                stderr=subprocess.STDOUT,
                timeout=timeout,
                env=os.environ.copy(),
            )

        output = log_path.read_text(
            encoding="utf-8", errors="replace"
        )

        print(output[-5000:], flush=True)
        return result.returncode, output

    except subprocess.TimeoutExpired:
        print(f"[TIMEOUT] {log_name}", flush=True)
        return 124, f"Process timed out after {timeout}s"


def extract_build_errors(output):
    errors = []

    for line in output.splitlines():
        line = line.strip()

        if (
            line.startswith("e:")
            or "error:" in line.lower()
            or "Unresolved reference" in line
            or "Compilation error" in line
        ):
            errors.append(line)

    return "\n".join(errors) if errors else output[-4000:]

def ask_ai(instruction, name):
    command = [
        "aider",
        "--model", MODEL,
        "--no-auto-commits",
        "--no-auto-test",
        "--yes",
        "--file",
        "android/app/src/main/java/com/example/torrentstreamplayer/MainActivity.kt",
        "--message", instruction,
    ]

    return run(command, AI_TIMEOUT, name)

def build(attempt):
    return run(
        ["bash", str(BUILD)],
        BUILD_TIMEOUT,
        f"build-{attempt}.log",
    )


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--task", required=True)
    args = parser.parse_args()

    task_path = Path(args.task).resolve()

    if not task_path.is_file():
        print(f"Task not found: {task_path}")
        return 1

    task = task_path.read_text(encoding="utf-8")

    if not task.strip():
        print("Task is empty.")
        return 1

    rules = (
        "Read AGENTS.md, SPEC.md and PLAN.md before coding.\n"
        "Do not modify scripts/build.sh, scripts/harness.py, "
        "Gradle wrapper files, or local.properties.\n"
        "Modify only source and test files needed for this task.\n"
        "Do not execute /test or run Gradle yourself.\n"
        "The harness will run the build separately.\n"
    )

    instruction = f"{rules}\nTASK:\n{task}"

    print("\n=== AI DEVELOPMENT ===", flush=True)

    code, _ = ask_ai(instruction, "aider-initial.log")

    if code != 0:
        print("AI execution failed.")
        return 1

    # AI가 Android 소스를 실제로 수정했는지 확인
    result = subprocess.run(
        ["git", "status", "--porcelain", "--", "android/app/src"],
        cwd=ROOT,
        capture_output=True,
        text=True,
    )

    if result.returncode != 0:
        print("Git status check failed.")
        return 1

    if not result.stdout.strip():
        print("\n=== AI FAILED: NO SOURCE CHANGES ===")
        print("Aider did not modify Android source files.")
        return 1

    for attempt in range(MAX_FIXES + 1):
        print(
            f"\n=== BUILD ATTEMPT {attempt + 1} ===",
            flush=True,
        )

        code, output = build(attempt)

        if code == 0:
            print("\n=== BUILD SUCCESS ===")
            return 0

        if attempt == MAX_FIXES:
            break

        print("\n=== AI AUTO FIX ===", flush=True)

        fix_instruction = (
            f"{rules}\n"
            f"Original task:\n{task}\n\n"
            "The Android build failed. "
            "Inspect and fix the code causing the error.\n"
            "Do not change build infrastructure.\n\n"
            f"Build errors:\n{extract_build_errors(output)}"
            # f"Build output:\n{output[-12000:]}"
        )

        code, _ = ask_ai(
            fix_instruction,
            f"aider-fix-{attempt + 1}.log",
        )

        if code != 0:
            print("AI fix execution failed.")
            return 1

    print("\n=== BUILD FAILED AFTER RETRIES ===")
    return 1


if __name__ == "__main__":
    sys.exit(main())
