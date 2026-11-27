#!/usr/bin/env node
// Runs a Gradle task through the wrapper at the repository root, so the package.json scripts of the
// Java projects work on Windows, Linux and macOS (ADR-011: Turborepo drives the Gradle tasks).
//   node ../../tools/gradle.mjs :libs:sdk-java:build
import { spawnSync } from 'node:child_process';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const wrapper = process.platform === 'win32' ? 'gradlew.bat' : './gradlew';
const result = spawnSync(wrapper, process.argv.slice(2), { cwd: root, stdio: 'inherit', shell: process.platform === 'win32' });
process.exit(result.status ?? 1);
