# Readme

Version 2.0.0-M7-public

## About

This is the seventh public milestone release of version 2.0. For publication, some contents have been removed, for example support of OWL which is still under heavy development.

This repository contains the reference implementation for the subject-oriented modeling language ["Parallel Activity Specification Scheme" (PASS)](https://github.com/I2PM/PASS-Standard-Book-Tex-Project).

See TODO.md for proposed features and known problems.

## Execution

This project is developed using OpenJDK 11 and Scala 2.13. It is required to use Java 11 or newer. You can check your Java version by typing `java -version` in a terminal / Command Prompt. There are no other requirements as everything is provided in the jar files, including the Scala runtime.

The execution is separated into two programs. The interpreter which runs in the background and a console application which loads the processes and controls the execution.

Both applications communicate via a fault-tolerant Akka connection. This allows restarting/stopping them independently.

### Process interpreter

Windows: to start the process interpreter open a Command Prompt, change to this directory and run `start_semantic.cmd`.

Unix: to start the process interpreter open a terminal, change to this directory and run `./start_semantic.sh`. You may need to set the file executable before that via `chmod +x start_semantic.sh`.

The process interpreter will print for each execution step debugging information. As the interpretation happens in an endless loop this will be very noisy - but you can leave this terminal / Command Prompt in the background as you control everything with the console application.


You can terminate the process interpreter with CTRL+C. On Unix-like systems you can pause it with CRTL+Z and resume it with `fg`.


### Console application

Windows: to start the console application open a second Command Prompt, change to this directory and run `start_console.cmd`.

Unix: to start the console application open a second terminal, change to this directory and run `./start_console.sh`. You may need to set the file executable before that via `chmod +x start_console.sh`.

In the beginning you will have no available actions as no processes are running. You can always hit the enter key to trigger a reload of the available actions.

You can exit the Console Application with `quit` and print a short help with `help`.


### Process execution

Processes can be loaded with the Console Application which parses them and sends them to the process interpreter.

To load a process type (for example) `process load ./processes/template.pass` into the Console Application.

To start a new process instance of a loaded process type (for example) `process start Template` into the Console Application.

You can manually force the termination of a process instance with (for example) `process kill 1`.

You can quit the Console Application with `quit`.

All commands support tab-completion, especially file name completion.

You can edit the provided application.conf file, for example to change the ports of the Akka systems if the default ports are already in use, or to adjust the debugging level and logfile.


### Example Process Models

Example processes are given in the `processes` folder.

## Process Model Parsing and File Conversion

It is possible to parse or convert process model files,
without executing them / running the above applications.
This might be useful during modeling,
for example when you just want to make sure that your process model files are valid.
It can also be used to convert your process model files to a different format, e.g.,
when you want to exchange them with a different application.

### Parsing to CLI

To just parse and print process model files
into the case classes provided in the package `de.athalis.pass.processmodel.tudarmstadt`,
you can use the Process Model Parsing Util with the scripts `parse_processfile.cmd` resp. `parse_processfile.sh`.

It takes a single argument: the path(s) to the file(s) that contain the process model(s),
separated by the OS-dependent path separator character.

For example, for a single file, run in a console `parse_processfile.cmd processes\Macro.graphml` on Windows
or `./parse_processfile.sh processes/Macro.graphml` on Unix.

To parse multiple files at once, separate the paths with the OS-dependent path separator character:
- Example on Windows: `parse_processfile.cmd processes\Macro.graphml;processes\echo_server.pass`
- Example on \*nix: `./parse_processfile.sh processes\Macro.graphml:processes\echo_server.pass`

### Converting process model files

With the scripts `convert_processmodel_files.cmd` and `convert_processmodel_files.sh` it is possible to convert process model files.

Currently, these formats / conversions are supported:

| Format        | Extension   | Input | Output | Parameter |
| ------------- | ----------- | ----- | ------ | --------- |
| textual / AST | `*.pass`    | yes   | no     |           |
| GraphML       | `*.graphml` | yes   | no     |           |
| ASM           | `*.coreasm` | no    | yes    | `asm`     |

The input format is automatically detected by the file extension.
The output format has to be given as first parameter,
the target folder as second parameter.

As with the CLI parser, to parse multiple process model files at once, separate the paths with the OS-dependent path separator character:
- Example on Windows: `convert_processmodel_files.cmd asm processes-asm processes\Macro.graphml;processes\echo_server.pass`
- Example on \*nix: `./convert_processmodel_files.sh asm processes-asm processes\Macro.graphml:processes\echo_server.pass`

The output will list the created files per line.

## Literature

* André Wolski. **Spezifikation einer Ausführungssemantik für das Subjektorientierte Prozessmanagement mit CoreASM**. Bachelor-Thesis, TU Darmstadt, October 2018. https://tuprints.ulb.tu-darmstadt.de/id/eprint/8360
* André Wolski, Stephan Borgert, and Lutz Heuser. **An extended Subject-Oriented Business Process Management Execution Semantics**. In _S-BPM ONE 2019, June 26 - 28, 2019, Seville, Spain_. ACM, New York, NY, USA, 2019. https://doi.org/10.1145/3329007.3332706
* André Wolski, Stephan Borgert, and Lutz Heuser. **A CoreASM based Reference Implementation for Subject-Oriented Business Process Management Execution Semantics**. In _S-BPM ONE'19, June 26 - 28, 2019, Seville, Spain_. ACM, New York, NY, USA, 2019. https://doi.org/10.1145/3329007.3329018
* Matthes Elstermann, and André Wolski. **Mapping Execution and Model Semantics for Subject-Oriented Process Models**. In: _Freitag, M., Kinra, A., Kotzab, H., Kreowski, HJ., Thoben, KD. (eds) Subject-Oriented Business Process Management. The Digital Workplace – Nucleus of Transformation._ S-BPM ONE 2020. Communications in Computer and Information Science, vol 1278. Springer, Cham. http://doi.org/10.1007/978-3-030-64351-5_4
* **PASS Standard Book** (work in progress): https://github.com/I2PM/PASS-Standard-Book-Tex-Project


## Development

See [DEVELOPMENT.md](DEVELOPMENT.md).


## Releases

Releases are currently only published as a single zip file, containing fat-jars for the bundled applications, scripts to run them, example process models and other auxiliary files.

As there are currently still no known downstream users, the modules are not yet released separately to Maven. Feel free to get in contact about this.


## Contact

André Wolski <andre.wolski@stud.tu-darmstadt.de>
